import logging
from netfilterqueue import NetfilterQueue
import socket
import thread
import time
import traceback

import dpkt

import iptables
import shutdown_hook
import full_proxy_service
import dns_server


LOGGER = logging.getLogger('fqrouter.%s' % __name__)
raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
shutdown_hook.add(raw_socket.close)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)
SO_MARK = 36
raw_socket.setsockopt(socket.SOL_SOCKET, SO_MARK, 0xface)


def run():
    try:
        insert_iptables_rules()
        thread.start_new(handle_nfqueue, ())
    except:
        LOGGER.exception('failed to start dns service')
        dns_service_status.error = traceback.format_exc()


def status():
    return dns_service_status.get_status_description()


def clean():
    delete_iptables_rules()

# === private ===

class DnsServiceStatus(object):
    def __init__(self):
        self.last_activity_at = None
        self.error = None

    def get_status_description(self):
        if self.error:
            return 'ERROR'
        if not self.last_activity_at:
            return 'NOT STARTED'
        if time.time() - self.last_activity_at > 30:
            return 'NO ACTIVITY'
        return 'WORKING'


dns_service_status = DnsServiceStatus()
domains = {} # ip => domain

RULES = [
    (
        {'target': 'NFQUEUE', 'extra': 'udp dpt:53 NFQUEUE num 1'},
        ('filter', 'OUTPUT', '-p udp --dport 53 -j NFQUEUE --queue-num 1')
    ), (
        {'target': 'NFQUEUE', 'extra': 'udp dpt:53 NFQUEUE num 1'},
        ('filter', 'FORWARD', '-p udp --dport 53 -j NFQUEUE --queue-num 1')
    ), (
        {'target': 'NFQUEUE', 'extra': 'udp spt:53 NFQUEUE num 1'},
        ('filter', 'INPUT', '-p udp --sport 53 -j NFQUEUE --queue-num 1')
    ), (
        {'target': 'NFQUEUE', 'extra': 'udp spt:53 NFQUEUE num 1'},
        ('filter', 'FORWARD', '-p udp --sport 53 -j NFQUEUE --queue-num 1')
    )
]
for dns_ip, dns_port in dns_server.list_dns_servers():
    if dns_port != 53:
        RULES.append((
            {'target': 'NFQUEUE', 'source': dns_ip, 'extra': 'udp spt:53 NFQUEUE num 1'},
            ('filter', 'INPUT', '-p udp -s %s --sport %s -j NFQUEUE --queue-num 1' % (dns_ip, dns_port))
        ))
        RULES.append((
            {'target': 'NFQUEUE', 'source': dns_ip, 'extra': 'udp spt:53 NFQUEUE num 1'},
            ('filter', 'FORWARD', '-p udp -s %s --sport %s -j NFQUEUE --queue-num 1' % (dns_ip, dns_port))
        ))

# source http://zh.wikipedia.org/wiki/%E5%9F%9F%E5%90%8D%E6%9C%8D%E5%8A%A1%E5%99%A8%E7%BC%93%E5%AD%98%E6%B1%A1%E6%9F%93
WRONG_ANSWERS = {
    '4.36.66.178',
    '8.7.198.45',
    '37.61.54.158',
    '46.82.174.68',
    '59.24.3.173',
    '64.33.88.161',
    '64.33.99.47',
    '64.66.163.251',
    '65.104.202.252',
    '65.160.219.113',
    '66.45.252.237',
    '72.14.205.99',
    '72.14.205.104',
    '78.16.49.15',
    '93.46.8.89',
    '128.121.126.139',
    '159.106.121.75',
    '169.132.13.103',
    '192.67.198.6',
    '202.106.1.2',
    '202.181.7.85',
    '203.161.230.171',
    '203.98.7.65',
    '207.12.88.98',
    '208.56.31.43',
    '209.36.73.33',
    '209.145.54.50',
    '209.220.30.174',
    '211.94.66.147',
    '213.169.251.35',
    '216.221.188.182',
    '216.234.179.13',
    '243.185.187.39'
}

GOOGLE_PLUS_WRONG_ANSWERS = {
    '74.125.127.102',
    '74.125.155.102',
    '74.125.39.113',
    '74.125.39.102',
    '209.85.229.138'
}

dns_transactions = {} # id => (started_at, orig_dst, orig_dport)


def insert_iptables_rules():
    shutdown_hook.add(delete_iptables_rules)
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_nfqueue_rules(1)
    iptables.delete_rules(RULES)


def handle_nfqueue():
    try:
        nfqueue = NetfilterQueue()
        nfqueue.bind(1, handle_packet)
        nfqueue.run()
    except:
        LOGGER.exception('stopped handling nfqueue')
        dns_service_status.error = traceback.format_exc()


def handle_packet(nfqueue_element):
    try:
        ip_packet = dpkt.ip.IP(nfqueue_element.get_payload())
        dns_packet = dpkt.dns.DNS(ip_packet.udp.data)
        questions = [question for question in dns_packet.qd if question.type == dpkt.dns.DNS_A]
        dns_packet.domain = questions[0].name if questions else None
        if 53 == ip_packet.udp.dport:
            is_primary = 0xface != nfqueue_element.get_mark()
            if is_primary:
                raw_socket.sendto(nfqueue_element.get_payload(), (socket.inet_ntoa(ip_packet.dst), 0))
            dns_server_name, dns_ip, dns_port = dns_server.select_dns_server(dns_packet.domain, is_primary)
            if is_primary and dns_packet.domain:
                LOGGER.info('[%s] resolve %s using: %s' % (dns_packet.id, dns_packet.domain, dns_server_name))
            dns_transactions[dns_packet.id] = (time.time(), socket.inet_ntoa(ip_packet.dst), ip_packet.udp.dport)
            ip_packet.dst = socket.inet_aton(dns_ip)
            ip_packet.sum = 0
            ip_packet.udp.dport = dns_port
            ip_packet.udp.sum = 0
            nfqueue_element.set_payload(str(ip_packet))
            nfqueue_element.accept()
        else:
            if contains_wrong_answer(dns_packet):
            # after the fake packet dropped, the real answer can be accepted by the client
                LOGGER.debug('drop fake dns packet: %s' % dns_packet.domain)
                nfqueue_element.drop()
                return
            if dns_packet.id in dns_transactions:
                orig_dst = dns_transactions[dns_packet.id][1]
                orig_dport = dns_transactions[dns_packet.id][2]
                del dns_transactions[dns_packet.id]
                ip_packet.src = socket.inet_aton(orig_dst)
                ip_packet.sum = 0
                ip_packet.udp.sport = orig_dport
                ip_packet.udp.sum = 0
                nfqueue_element.set_payload(str(ip_packet))
                nfqueue_element.accept()
            else:
                nfqueue_element.drop()
            expired_dns_transaction_ids = []
            for dns_transaction_id, dns_transaction in dns_transactions.items():
                if time.time() - dns_transaction[0] > 10:
                    LOGGER.error('[%s] timeout' % dns_transaction_id)
                    expired_dns_transaction_ids.append(dns_transaction_id)
            for dns_transaction_id in expired_dns_transaction_ids:
                del dns_transactions[dns_transaction_id]
            dns_service_status.last_activity_at = time.time()
    except:
        LOGGER.exception('failed to handle packet')
        nfqueue_element.accept()


def contains_wrong_answer(dns_packet):
    if dpkt.dns.DNS_A not in [question.type for question in dns_packet.qd]:
        return False # not answer to A question, might be PTR
    for answer in dns_packet.an:
        if dpkt.dns.DNS_A == answer.type:
            resolved_ip = socket.inet_ntoa(answer['rdata'])
            if resolved_ip in WRONG_ANSWERS:
                return True
            if 'plus.google.com' in dns_packet.domain and resolved_ip in GOOGLE_PLUS_WRONG_ANSWERS:
                return True
            else:
                domains[resolved_ip] = dns_packet.domain
                LOGGER.info('[%s] resolved %s => %s' % (dns_packet.id, dns_packet.domain, resolved_ip))
                if 'twitter.com' in dns_packet.domain:
                    full_proxy_service.add_to_black_list(resolved_ip)
                return False # if the blacklist is incomplete, we will think it is right answer
    return True # to find empty answer


def get_domain(ip):
    return domains.get(ip)
import logging
from netfilterqueue import NetfilterQueue
import socket
import thread
import time
import traceback

import dpkt

import iptables
import shutdown_hook
import dns_resolver
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
            handle_outbound_packet(nfqueue_element, ip_packet, dns_packet)
        else:
            handle_inbound_packet(nfqueue_element, ip_packet, dns_packet)
        dns_service_status.last_activity_at = time.time()
    except:
        LOGGER.exception('failed to handle packet')
        nfqueue_element.accept()


def handle_outbound_packet(nfqueue_element, ip_packet, dns_packet):
    is_primary = not nfqueue_element.get_mark()
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
    hosted_form = dns_server.transform_domain_to_hosted_form(dns_packet.domain)
    if hosted_form:
        dns_packet.qd[0].name = hosted_form
        ip_packet.udp.data = str(dns_packet)
        ip_packet.udp.ulen = len(ip_packet.udp)
        ip_packet.len = len(ip_packet)
    nfqueue_element.set_payload(str(ip_packet))
    nfqueue_element.accept()


def handle_inbound_packet(nfqueue_element, ip_packet, dns_packet):
    if dns_resolver.contains_wrong_answer(dns_packet):
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
        original_form = dns_server.transform_domain_from_hosted_form(dns_packet.domain)
        if original_form:
            dns_packet.qd[0].name = original_form
            for an in dns_packet.an:
                an.name = original_form
            ip_packet.udp.data = str(dns_packet)
            ip_packet.udp.ulen = len(ip_packet.udp)
            ip_packet.len = len(ip_packet)
        nfqueue_element.set_payload(str(ip_packet))
        nfqueue_element.accept()
    else:
        nfqueue_element.drop()
    expired_dns_transaction_ids = []
    for dns_transaction_id, dns_transaction in dns_transactions.items():
        if time.time() - dns_transaction[0] > 5:
            LOGGER.error('[%s] timeout' % dns_transaction_id)
            expired_dns_transaction_ids.append(dns_transaction_id)
    for dns_transaction_id in expired_dns_transaction_ids:
        del dns_transactions[dns_transaction_id]
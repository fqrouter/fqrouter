import logging
from netfilterqueue import NetfilterQueue
import socket
import thread
import time
import traceback

import dpkt

import iptables
import shutdown_hook
import network_interface
import jamming_event
import full_proxy_service


LOGGER = logging.getLogger(__name__)


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

PRIMARY_DNS = '8.8.8.8'
PRIMARY_DNS_MARK = 0x1feed
FOR_STUPID_GOOGLE_DNS = '199.91.73.222' # v2ex dns resolves google.com and youtube.com better
FOR_STUPID_GOOGLE_DNS_MARK = 0x2feed

RULES = []
for iface in network_interface.list_data_network_interfaces():
    RULES.append((
        {'target': 'NFQUEUE', 'iface_out': iface, 'extra': 'udp dpt:53 mark match ! 0xfeed/0xffff NFQUEUE num 1'},
        ('nat', 'OUTPUT', '-o %s -p udp --dport 53 -m mark ! --mark 0xfeed/0xffff -j NFQUEUE --queue-num 1' % iface)
    ))
    RULE_REDIRECT_TO_PRIMARY_DNS = (
        {'target': 'DNAT', 'iface_out': iface, 'extra':
            'udp dpt:53 mark match 0x1feed to:%s:53' % PRIMARY_DNS},
        ('nat', 'OUTPUT', '-o %s -p udp --dport 53 -m mark --mark 0x1feed '
                          '-j DNAT --to-destination %s:53' % (iface, PRIMARY_DNS))
    )
    RULES.append(RULE_REDIRECT_TO_PRIMARY_DNS)
    RULE_REDIRECT_TO_FOR_STUPID_GOOGLE_DNS = (
        {'target': 'DNAT', 'iface_out': iface, 'extra':
            'udp dpt:53 mark match 0x2feed to:%s:53' % FOR_STUPID_GOOGLE_DNS},
        ('nat', 'OUTPUT', '-o %s -p udp --dport 53 -m mark --mark 0x2feed '
                          '-j DNAT --to-destination %s:53' % (iface, FOR_STUPID_GOOGLE_DNS))
    )
    RULES.append(RULE_REDIRECT_TO_FOR_STUPID_GOOGLE_DNS)
    RULE_DROP_PACKET = (
        {'target': 'NFQUEUE', 'iface_in': iface, 'extra': 'udp spt:53 NFQUEUE num 1'},
        ('filter', 'INPUT', '-i %s -p udp --sport 53 -j NFQUEUE --queue-num 1' % iface)
    )
    RULES.append(RULE_DROP_PACKET)

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
            if dns_packet.domain and ('google.com' in dns_packet.domain or 'youtube.com' in dns_packet.domain
                                      or 'googleusercontent.com' in dns_packet.domain
                                      or 'appspot.com' in dns_packet.domain):
                LOGGER.info('resolve stupid %s using: %s' % (dns_packet.domain, FOR_STUPID_GOOGLE_DNS))
                nfqueue_element.set_mark(FOR_STUPID_GOOGLE_DNS_MARK)
                nfqueue_element.repeat()
            else:
                nfqueue_element.set_mark(PRIMARY_DNS_MARK)
                nfqueue_element.repeat()
        else:
            if contains_wrong_answer(dns_packet):
            # after the fake packet dropped, the real answer can be accepted by the client
                LOGGER.debug('drop fake dns packet: %s' % repr(dns_packet))
                jamming_event.record('%s: dns hijacking' % dns_packet.domain)
                nfqueue_element.drop()
                return
            nfqueue_element.accept()
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
                LOGGER.info('dns resolve: %s => %s' % (dns_packet.domain, resolved_ip))
                if 'twitter.com' in dns_packet.domain:
                    full_proxy_service.add_to_black_list(resolved_ip)
                return False # if the blacklist is incomplete, we will think it is right answer
    return True # to find empty answer


def get_domain(ip):
    return domains.get(ip)
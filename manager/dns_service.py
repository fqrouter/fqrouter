import logging
from netfilterqueue import NetfilterQueue
import socket
import thread

import dpkt

import iptables
import shutdown_hook
import network_interface

LOGGER = logging.getLogger(__name__)


def run():
    insert_iptables_rules()
    thread.start_new(handle_nfqueue, ())


def status():
    return 'N/A'


def clean():
    delete_iptables_rules()

# === private ===

CLEAN_DNS = '8.8.8.8'

RULES = []
for iface in network_interface.list_data_network_interfaces():
    # this rule make sure we always query from the "CLEAN" dns
    RULE_REDIRECT_TO_CLEAN_DNS = (
        {'target': 'DNAT', 'iface_out': iface, 'extra': 'udp dpt:53 to:%s:53' % CLEAN_DNS},
        ('nat', 'OUTPUT', '-o %s -p udp --dport 53 -j DNAT --to-destination %s:53' % (iface, CLEAN_DNS))
    )
    RULES.append(RULE_REDIRECT_TO_CLEAN_DNS)
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


def handle_packet(nfqueue_element):
    try:
        ip_packet = dpkt.ip.IP(nfqueue_element.get_payload())
        dns_packet = dpkt.dns.DNS(ip_packet.udp.data)
        if contains_wrong_answer(dns_packet):
        # after the fake packet dropped, the real answer can be accepted by the client
            LOGGER.debug('drop fake dns packet: %s' % repr(dns_packet))
            nfqueue_element.drop()
            return
        nfqueue_element.accept()
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
                return True # to find wrong answer
            else:
                LOGGER.info('dns resolve: %s => %s' % (dns_packet.qd[0].name, resolved_ip))
                return False # if the blacklist is incomplete, we will think it is right answer
    return True # to find empty answer
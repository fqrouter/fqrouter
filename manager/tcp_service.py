import logging
import thread
from netfilterqueue import NetfilterQueue
import socket

import dpkt

import iptables
import shutdown_hook


LOGGER = logging.getLogger(__name__)


def run():
    insert_iptables_rules()
    thread.start_new(handle_nfqueue, ())


def status():
    pass


def clean():
    delete_iptables_rules()

#=== private ===

RULE_SERVER_SYN_ACK = (
    {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x12 NFQUEUE num 2'},
    ('filter', 'INPUT', '-p tcp --tcp-flags ALL SYN,ACK -j NFQUEUE --queue-num 2')
)

RULES = (
    RULE_SERVER_SYN_ACK,
)

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)


def insert_iptables_rules():
    shutdown_hook.add(delete_iptables_rules)
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)


def handle_nfqueue():
    try:
        nfqueue = NetfilterQueue()
        nfqueue.bind(2, handle_packet)
        nfqueue.run()
    except:
        LOGGER.exception('stopped handling nfqueue')


def handle_packet(nfqueue_element):
    try:
        ip_packet = dpkt.ip.IP(nfqueue_element.get_payload())
        handle_syn_ack(ip_packet)
        nfqueue_element.accept()
    except:
        LOGGER.exception('failed to handle packet')
        nfqueue_element.accept()


def handle_syn_ack(syn_ack):
    tcp_packet = dpkt.tcp.TCP(
        sport=syn_ack.tcp.dport, dport=syn_ack.tcp.sport,
        flags=dpkt.tcp.TH_ACK, seq=syn_ack.tcp.ack, ack=syn_ack.tcp.seq + 1,
        data='', opts='')
    ip_packet = dpkt.ip.IP(dst=syn_ack.src, src=syn_ack.dst, p=dpkt.ip.IP_PROTO_TCP, ttl=12)
    ip_packet.data = ip_packet.tcp = tcp_packet
    raw_socket.sendto(str(ip_packet), (socket.inet_ntoa(ip_packet.dst), 0))
    LOGGER.debug('inject empty ACK: %s' % repr(ip_packet))
    tcp_packet = dpkt.tcp.TCP(
        sport=syn_ack.tcp.dport, dport=syn_ack.tcp.sport,
        flags=dpkt.tcp.TH_ACK, seq=syn_ack.tcp.ack, ack=syn_ack.tcp.seq + 1,
        data=5 * '0', opts='')
    ip_packet = dpkt.ip.IP(dst=syn_ack.src, src=syn_ack.dst, p=dpkt.ip.IP_PROTO_TCP, ttl=12)
    ip_packet.data = ip_packet.tcp = tcp_packet
    raw_socket.sendto(str(ip_packet), (socket.inet_ntoa(ip_packet.dst), 0))
    LOGGER.debug('inject wrong ACK: %s' % repr(ip_packet))
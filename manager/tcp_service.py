import logging
import thread
from netfilterqueue import NetfilterQueue
import socket
import time
import collections

import dpkt

import iptables
import shutdown_hook
import dns_service


LOGGER = logging.getLogger(__name__)


def run():
    insert_iptables_rules()
    dns_service.add_wrong_dns_answer_handler(handle_dns_wrong_answer)
    thread.start_new(handle_nfqueue, ())


def status():
    pass


def clean():
    delete_iptables_rules()

#=== private ===

TTL_TO_GFW = 11 # based on black magic

RULE_INPUT_SYN_ACK = (
    {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x12 NFQUEUE num 2'},
    ('filter', 'INPUT', '-i wlan0 -p tcp --tcp-flags ALL SYN,ACK -j NFQUEUE --queue-num 2')
)

RULE_INPUT_RST = (
    {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x04 NFQUEUE num 2'},
    ('filter', 'INPUT', '-i wlan0 -p tcp --tcp-flags ALL RST -j NFQUEUE --queue-num 2')
)

RULE_FORWARD_SYN_ACK = (
    {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x12 NFQUEUE num 2'},
    ('filter', 'FORWARD', '-i wlan0 -p tcp --tcp-flags ALL SYN,ACK -j NFQUEUE --queue-num 2')
)

RULE_FORWARD_RST = (
    {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x04 NFQUEUE num 2'},
    ('filter', 'FORWARD', '-i wlan0 -p tcp --tcp-flags ALL RST -j NFQUEUE --queue-num 2')
)

RULES = (
    RULE_INPUT_SYN_ACK,
    RULE_INPUT_RST,
    RULE_FORWARD_SYN_ACK,
    RULE_FORWARD_RST
)

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
shutdown_hook.add(raw_socket.close)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)

udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
shutdown_hook.add(udp_socket.close)
udp_socket.settimeout(0)
udp_socket.setsockopt(socket.SOL_IP, socket.IP_TTL, TTL_TO_GFW)
udp_socket.bind(('', dns_service.DNS_PROBE_SPORT))

SYN_ACK_TIMEOUT = 2 # seconds

international_zone = set() # found by fake DNS packet sent back by GFW caused by our stimulation
domestic_zone = set() # found by RST sent back by server caused by our stimulation
pending_syn_ack = {} # ip => (time, sport => packet)
recent_rst_packets = collections.deque(maxlen=30)


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
        if dpkt.tcp.TH_RST & ip_packet.tcp.flags:
            should_accept = handle_rst(ip_packet)
        else:
            should_accept = handle_syn_ack(ip_packet)
        if should_accept:
            nfqueue_element.accept()
        else:
            nfqueue_element.drop()
    except:
        LOGGER.exception('failed to handle packet')
        nfqueue_element.accept()


def handle_rst(rst):
    LOGGER.debug('received RST: %s' % format_ip_packet(rst))
    rst.src_ip = socket.inet_ntoa(rst.src)
    rst.dst_ip = socket.inet_ntoa(rst.dst)
    recent_rst_packets.append(rst)
    return True # receiving RST we already screwed, dropping it will not help


def handle_syn_ack(syn_ack):
    if 255 == syn_ack.ttl:
        # syn_ack injected back
        return True
    else:
        # syn_ack intercepted
        uncertain_ip = socket.inet_ntoa(syn_ack.src)
        if uncertain_ip in international_zone:
            inject_poison_ack_to_fill_gfw_buffer_with_garbage(syn_ack)
            return True
        elif uncertain_ip in domestic_zone:
            return True
        else:
            if uncertain_ip in pending_syn_ack:
                pending_syn_ack.setdefault(uncertain_ip, (time.time(), {}))[1][syn_ack.tcp.dport] = syn_ack
                if time.time() - pending_syn_ack[uncertain_ip][0] > SYN_ACK_TIMEOUT:
                    domestic_ip = uncertain_ip
                    LOGGER.info('treat the ip as domestic: %s' % domestic_ip)
                    domestic_zone.add(domestic_ip)
                    _, packets = pending_syn_ack.pop(domestic_ip)
                    for syn_ack in packets.values():
                        inject_back_syn_ack(syn_ack)
            else:
                pending_syn_ack.setdefault(uncertain_ip, (time.time(), {}))[1][syn_ack.tcp.dport] = syn_ack
                inject_dns_question_to_stimulate_gfw(uncertain_ip)
            return False


def inject_dns_question_to_stimulate_gfw(uncertain_ip):
# the router deployed with http rst is very likely to have dns hijacking deployed
# if the ttl can reach a router with dns hijacking deployed
# then we assume there is gfw device between us and the server
# the uncertain ip will be recognized as "international"
    dns_question = dpkt.dns.DNS(qd=[dpkt.dns.DNS.Q(name='%s.twitter.com' % uncertain_ip.replace('.', '-'))])
    LOGGER.debug('inject DNS question: %s' % repr(dns_question))
    udp_socket.sendto(str(dns_question), (uncertain_ip, 53))


def handle_dns_wrong_answer(question):
    if not question.endswith('.twitter.com'):
        return
    possible_ip = question.replace('.twitter.com', '')
    if 4 == len(possible_ip.split('-')):
        international_ip = possible_ip.replace('-', '.')
        LOGGER.info('found international ip: %s' % international_ip)
        international_zone.add(international_ip)
        if international_ip in pending_syn_ack:
            _, packets = pending_syn_ack.pop(international_ip)
            for syn_ack in packets.values():
                inject_poison_ack_to_fill_gfw_buffer_with_garbage(syn_ack)
            for syn_ack in packets.values():
                inject_back_syn_ack(syn_ack)


def inject_back_syn_ack(syn_ack):
    LOGGER.debug('inject back syn ack: %s' % format_ip_packet(syn_ack))
    syn_ack.ttl = 255
    syn_ack.sum = 0
    syn_ack.tcp.sum = 0
    raw_socket.sendto(str(syn_ack), (socket.inet_ntoa(syn_ack.dst), 0))


def inject_poison_ack_to_fill_gfw_buffer_with_garbage(syn_ack):
    tcp_packet = dpkt.tcp.TCP(
        sport=syn_ack.tcp.dport, dport=syn_ack.tcp.sport,
        flags=dpkt.tcp.TH_ACK, seq=syn_ack.tcp.ack, ack=syn_ack.tcp.seq + 1,
        data='', opts='')
    ip_packet = dpkt.ip.IP(dst=syn_ack.src, src=syn_ack.dst, p=dpkt.ip.IP_PROTO_TCP, ttl=TTL_TO_GFW)
    ip_packet.data = ip_packet.tcp = tcp_packet
    raw_socket.sendto(str(ip_packet), (socket.inet_ntoa(ip_packet.dst), 0))
    LOGGER.debug('inject empty ACK: %s' % format_ip_packet(ip_packet))
    tcp_packet = dpkt.tcp.TCP(
        sport=syn_ack.tcp.dport, dport=syn_ack.tcp.sport,
        flags=dpkt.tcp.TH_ACK, seq=syn_ack.tcp.ack, ack=syn_ack.tcp.seq + 1,
        data=5 * '0', opts='')
    ip_packet = dpkt.ip.IP(dst=syn_ack.src, src=syn_ack.dst, p=dpkt.ip.IP_PROTO_TCP, ttl=TTL_TO_GFW)
    ip_packet.data = ip_packet.tcp = tcp_packet
    raw_socket.sendto(str(ip_packet), (socket.inet_ntoa(ip_packet.dst), 0))
    LOGGER.debug('inject poison ACK: %s' % format_ip_packet(ip_packet))


def format_ip_packet(ip_packet):
    return '%s=>%s %s' % (socket.inet_ntoa(ip_packet.src), socket.inet_ntoa(ip_packet.dst), repr(ip_packet))
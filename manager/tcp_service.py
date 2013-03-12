import logging
import thread
from netfilterqueue import NetfilterQueue
import socket
import collections

import dpkt

import iptables
import pending_connection
import shutdown_hook
import china_ip


LOGGER = logging.getLogger(__name__)


def run():
    insert_iptables_rules()
    thread.start_new(handle_nfqueue, ())


def status():
    pass


def clean():
    delete_iptables_rules()

#=== private ===

NO_PROCESSING_MAGICAL_TTL = 255
TTL_TO_GFW = 9 # based on black magic

RULE_INPUT_SYN_ACK = (
    {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x12 NFQUEUE num 2'},
    ('filter', 'INPUT', '-i wlan0 -p tcp --tcp-flags ALL SYN,ACK -j NFQUEUE --queue-num 2')
)

RULE_INPUT_RST = (
    {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x04 NFQUEUE num 2'},
    ('filter', 'INPUT', '-i wlan0 -p tcp --tcp-flags ALL RST -j NFQUEUE --queue-num 2')
)

RULE_OUTPUT_PSH_ACK = (
    {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x18 NFQUEUE num 2'},
    ('filter', 'OUTPUT', '-o wlan0 -p tcp --tcp-flags ALL PSH,ACK -j NFQUEUE --queue-num 2')
)

RULES = (
    RULE_INPUT_SYN_ACK,
    RULE_INPUT_RST,
    RULE_OUTPUT_PSH_ACK
)

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
shutdown_hook.add(raw_socket.close)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)


def find_probe_src():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        return s.getsockname()[0]
    finally:
        s.close()


PROBE_SRC = find_probe_src() # local routing table decision

international_zone = set() # found by fake DNS packet sent back by GFW caused by our stimulation
domestic_zone = set() # found by china_ip or timeout
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
        if ip_packet.ttl in (NO_PROCESSING_MAGICAL_TTL, TTL_TO_GFW):
            nfqueue_element.accept()
            return
        if dpkt.tcp.TH_RST & ip_packet.tcp.flags:
            should_accept = handle_rst(ip_packet)
        elif dpkt.tcp.TH_PUSH & ip_packet.tcp.flags:
            should_accept = handle_psh_ack(ip_packet)
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
    LOGGER.error('received RST: %s' % format_ip_packet(rst))
    rst.src_ip = socket.inet_ntoa(rst.src)
    rst.dst_ip = socket.inet_ntoa(rst.dst)
    recent_rst_packets.append(rst)
    return True # receiving RST we already screwed, dropping it will not help


def handle_psh_ack(psh_ack):
    pos = psh_ack.tcp.data.find('Host:')
    if -1 == pos:
        return True
    LOGGER.debug('found HTTP GET: %s' % format_ip_packet(psh_ack))
    pos += len('Host')
    inject_scrambled_http_get_to_let_gfw_type2_miss_keyword(psh_ack, pos)
    return False


def handle_syn_ack(syn_ack):
    uncertain_ip = socket.inet_ntoa(syn_ack.src)
    if uncertain_ip in international_zone:
        inject_poison_ack_to_fill_gfw_buffer_with_garbage(syn_ack)
        return True
    elif uncertain_ip in domestic_zone:
        return True
    elif pending_connection.is_ip_pending(uncertain_ip):
        pending_connection.record_syn_ack(syn_ack)
        syn_ack_packets = pending_connection.pop_timeout_syn_ack_packets(uncertain_ip)
        if syn_ack_packets:
            international_ip = uncertain_ip
            LOGGER.info('treat ip as international due to timeout: %s' % international_ip)
            add_international_ip(international_ip, syn_ack_packets)
        return False
    elif china_ip.is_china_ip(uncertain_ip):
        domestic_ip = uncertain_ip
        LOGGER.info('found domestic ip: %s' % domestic_ip)
        domestic_zone.add(domestic_ip)
        return True
    else:
        pending_connection.record_syn_ack(syn_ack)
        return False


def add_international_ip(international_ip, syn_ack_packets):
    international_zone.add(international_ip)
    for syn_ack in syn_ack_packets:
        inject_poison_ack_to_fill_gfw_buffer_with_garbage(syn_ack)
    for syn_ack in syn_ack_packets:
        inject_back_syn_ack(syn_ack)


def inject_back_syn_ack(syn_ack):
    LOGGER.debug('inject back syn ack: %s' % format_ip_packet(syn_ack))
    syn_ack.ttl = NO_PROCESSING_MAGICAL_TTL
    syn_ack.sum = 0
    syn_ack.tcp.sum = 0
    raw_socket.sendto(str(syn_ack), (socket.inet_ntoa(syn_ack.dst), 0))


def format_ip_packet(ip_packet):
    return '%s=>%s %s' % (socket.inet_ntoa(ip_packet.src), socket.inet_ntoa(ip_packet.dst), repr(ip_packet))


def inject_scrambled_http_get_to_let_gfw_type2_miss_keyword(psh_ack, pos):
# we still need to make the keyword less obvious by splitting the packet into two
# to make it harder to rebuilt the stream, we injected two more fake packets to poison the stream
# first_packet .. fake_second_packet => GFW ? wrong
# fake_first_packet .. second_packet => GFW ? wrong
# first_packet .. second_packet => server ? yes, it is a HTTP GET
    first_part = psh_ack.tcp.data[:pos]
    second_part = psh_ack.tcp.data[pos:]
    first_packet = dpkt.ip.IP(str(psh_ack))
    first_packet.ttl = NO_PROCESSING_MAGICAL_TTL
    first_packet.tcp.data = first_part
    first_packet.sum = 0
    first_packet.tcp.sum = 0
    raw_socket.sendto(str(first_packet), (socket.inet_ntoa(first_packet.dst), 0))
    fake_second_packet = dpkt.ip.IP(str(psh_ack))
    fake_second_packet.ttl = TTL_TO_GFW
    fake_second_packet.tcp.seq += len(first_part)
    fake_second_packet.tcp.data = ': baidu.com\r\n\r\n'
    fake_second_packet.sum = 0
    fake_second_packet.tcp.sum = 0
    raw_socket.sendto(str(fake_second_packet), (socket.inet_ntoa(fake_second_packet.dst), 0))
    fake_first_packet = dpkt.ip.IP(str(psh_ack))
    fake_first_packet.ttl = TTL_TO_GFW
    fake_first_packet.tcp.data = len(first_part) * '0'
    fake_first_packet.sum = 0
    fake_first_packet.tcp.sum = 0
    raw_socket.sendto(str(fake_first_packet), (socket.inet_ntoa(fake_first_packet.dst), 0))
    second_packet = dpkt.ip.IP(str(psh_ack))
    second_packet.ttl = NO_PROCESSING_MAGICAL_TTL
    second_packet.tcp.seq += len(first_part)
    second_packet.tcp.data = second_part
    second_packet.sum = 0
    second_packet.tcp.sum = 0
    raw_socket.sendto(str(second_packet), (socket.inet_ntoa(second_packet.dst), 0))


def inject_poison_ack_to_fill_gfw_buffer_with_garbage(syn_ack):
# poison ack should blind most GFW checking rules
# because the seq of the tcp is the same, GFW will take the first one for the same seq
# seq 1: xxx
# seq 1: yyy
# GFW think it is xxx
# server think it is yyy
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

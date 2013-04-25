import logging
import thread
from netfilterqueue import NetfilterQueue
import socket
import time
import traceback

import dpkt

import iptables
import pending_connection
import shutdown_hook
import china_ip
import dns_service
import full_proxy_service

LOGGER = logging.getLogger('fqrouter.%s' % __name__)


def run():
    try:
        insert_iptables_rules()
        thread.start_new(handle_nfqueue, ())
    except:
        LOGGER.exception('failed to start tcp service')
        tcp_service_status.error = traceback.format_exc()


def status():
    return tcp_service_status.get_status_description()


def clean():
    delete_iptables_rules()

#=== private ===

MIN_TTL_TO_GFW = 8
MAX_TTL_TO_GFW = 14
DEFAULT_TTL_TO_GFW = 9
SAFETY_DELTA = 0 # the ttl detected - safety delta will be the one used
RANGE_OF_TTL_TO_GFW = range(MIN_TTL_TO_GFW, MAX_TTL_TO_GFW + 1)

RULES = []


def add_rules():
    RULE_INPUT_SYN_ACK = (
        {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x12 NFQUEUE num 2'},
        ('filter', 'scramble_INPUT', '-p tcp --tcp-flags ALL SYN,ACK -j NFQUEUE --queue-num 2')
    )
    RULES.append(RULE_INPUT_SYN_ACK)
    RULE_INPUT_RST = (
        {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x04 NFQUEUE num 2'},
        ('filter', 'scramble_INPUT', '-p tcp --tcp-flags ALL RST -j NFQUEUE --queue-num 2')
    )
    RULES.append(RULE_INPUT_RST)
    RULE_INPUT_ICMP = (
        {'target': 'NFQUEUE', 'extra': 'NFQUEUE num 2'},
        ('filter', 'scramble_INPUT', '-p icmp -j NFQUEUE --queue-num 2')
    )
    RULES.append(RULE_INPUT_ICMP)
    RULE_OUTPUT_PSH_ACK = (
        {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x18 NFQUEUE num 2'},
        ('filter', 'scramble_OUTPUT', '-p tcp --tcp-flags ALL PSH,ACK -j NFQUEUE --queue-num 2')
    )
    RULES.append(RULE_OUTPUT_PSH_ACK)
    RULE_OUTPUT_SYN = (
        {'target': 'NFQUEUE', 'extra': 'tcp flags:0x3F/0x18 NFQUEUE num 2'},
        ('filter', 'scramble_OUTPUT', '-p tcp --tcp-flags ALL SYN -j NFQUEUE --queue-num 2')
    )
    RULES.append(RULE_OUTPUT_SYN)


def add_scramble_output_chain():
    RULES.append((
        {'target': 'scramble_OUTPUT'},
        ('filter', 'OUTPUT', '-j scramble_OUTPUT')
    ))
    RULES.append((
        {'target': 'RETURN', 'extra': 'mark match 0xcafe'},
        ('filter', 'scramble_OUTPUT', '-m mark --mark 0xcafe -j RETURN')
    ))
    RULES.append((
        {'target': 'RETURN', 'iface_out': 'lo'},
        ('filter', 'scramble_OUTPUT', '-o lo -j RETURN')
    ))
    for lan_ip_range in [
        '0.0.0.0/8', '10.0.0.0/8', '127.0.0.0/8', '169.254.0.0/16',
        '172.16.0.0/12', '192.168.0.0/16', '224.0.0.0/4', '240.0.0.0/4']:
        RULES.append((
            {'target': 'RETURN', 'destination': lan_ip_range},
            ('filter', 'scramble_OUTPUT', '-d %s -j RETURN' % lan_ip_range)
        ))


def add_scramble_input_chain():
    RULES.append((
        {'target': 'scramble_INPUT'},
        ('filter', 'INPUT', '-j scramble_INPUT')
    ))
    RULES.append((
        {'target': 'RETURN', 'extra': 'mark match 0xcafe'},
        ('filter', 'scramble_INPUT', '-m mark --mark 0xcafe -j RETURN')
    ))
    RULES.append((
        {'target': 'RETURN', 'iface_in': 'lo'},
        ('filter', 'scramble_INPUT', '-i lo -j RETURN')
    ))
    for lan_ip_range in [
        '0.0.0.0/8', '10.0.0.0/8', '127.0.0.0/8', '169.254.0.0/16',
        '172.16.0.0/12', '192.168.0.0/16', '224.0.0.0/4', '240.0.0.0/4']:
        RULES.append((
            {'target': 'RETURN', 'source': lan_ip_range},
            ('filter', 'scramble_INPUT', '-s %s -j RETURN' % lan_ip_range)
        ))


def add_lan_chains():
    RULES.append((
        {'target': 'lan_unknown'},
        ('filter', 'FORWARD', '-j lan_unknown')
    ))
    RULES.append((
        {'target': 'RETURN', 'extra': 'mark match 0xcafe'},
        ('filter', 'lan_unknown', '-m mark --mark 0xcafe -j RETURN')
    ))
    RULES.append((
        {'target': 'RETURN', 'iface_in': 'lo'},
        ('filter', 'lan_unknown', '-i lo -j RETURN')
    ))
    RULES.append((
        {'target': 'RETURN', 'iface_out': 'lo'},
        ('filter', 'lan_unknown', '-o lo -j RETURN')
    ))
    for lan_ip_range in [
        '0.0.0.0/8', '10.0.0.0/8', '127.0.0.0/8', '169.254.0.0/16',
        '172.16.0.0/12', '192.168.0.0/16', '224.0.0.0/4', '240.0.0.0/4']:
        RULES.append((
            {'target': 'lan_dst', 'destination': lan_ip_range},
            ('filter', 'lan_unknown', '-d %s -j lan_dst' % lan_ip_range)
        ))
        RULES.append((
            {'target': 'lan_src', 'source': lan_ip_range},
            ('filter', 'lan_unknown', '-s %s -j lan_src' % lan_ip_range)
        ))
        RULES.append((
            {'target': 'RETURN', 'source': lan_ip_range},
            ('filter', 'lan_dst', '-s %s -j RETURN' % lan_ip_range)
        ))
        RULES.append((
            {'target': 'RETURN', 'destination': lan_ip_range},
            ('filter', 'lan_src', '-d %s -j RETURN' % lan_ip_range)
        ))
    RULES.append((
        {'target': 'scramble_OUTPUT'},
        ('filter', 'lan_src', '-j scramble_OUTPUT')
    ))
    RULES.append((
        {'target': 'scramble_INPUT'},
        ('filter', 'lan_dst', '-j scramble_INPUT')
    ))


add_scramble_input_chain()
add_scramble_output_chain()
add_lan_chains()
add_rules()

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
shutdown_hook.add(raw_socket.close)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)
SO_MARK = 36
raw_socket.setsockopt(socket.SOL_SOCKET, SO_MARK, 0xcafe)


class TcpServiceStatus(object):
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


international_zone = {} # found by icmp time exceeded
domestic_zone = set() # found by china_ip or icmp time exceeded or timeout
tcp_service_status = TcpServiceStatus()
syn_ack_ttl = {} # ttl observed from syn ack packet
pending_syn = {} # ip => time


def insert_iptables_rules():
    shutdown_hook.add(delete_iptables_rules)
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)
    iptables.delete_nfqueue_rules(2)
    iptables.delete_chain('scramble_INPUT')
    iptables.delete_chain('scramble_OUTPUT')
    iptables.delete_chain('lan_unknown')
    iptables.delete_chain('lan_src')
    iptables.delete_chain('lan_dst')


def handle_nfqueue():
    try:
        nfqueue = NetfilterQueue()
        nfqueue.bind(2, handle_packet)
        nfqueue.run()
    except:
        LOGGER.exception('stopped handling nfqueue')
        tcp_service_status.error = traceback.format_exc()


def handle_packet(nfqueue_element):
    try:
        ip_packet = dpkt.ip.IP(nfqueue_element.get_payload())
        if hasattr(ip_packet, 'tcp'):
            if dpkt.tcp.TH_RST & ip_packet.tcp.flags:
                should_accept = handle_rst(ip_packet)
            elif dpkt.tcp.TH_PUSH & ip_packet.tcp.flags:
                should_accept = handle_psh_ack(ip_packet)
            elif dpkt.tcp.TH_SYN & ip_packet.tcp.flags and dpkt.tcp.TH_ACK & ip_packet.tcp.flags:
                should_accept = handle_syn_ack(ip_packet)
            elif dpkt.tcp.TH_SYN == ip_packet.tcp.flags:
                should_accept = handle_syn(ip_packet)
            else:
                LOGGER.info('unexpected packet: %s' % format_ip_packet(ip_packet))
                should_accept = True
        elif hasattr(ip_packet, 'icmp'):
            icmp_packet = ip_packet.data
            if dpkt.icmp.ICMP_TIMEXCEED == icmp_packet.type and dpkt.icmp.ICMP_TIMEXCEED_INTRANS == icmp_packet.code:
                handle_time_exceeded(ip_packet)
            should_accept = True
        else:
            LOGGER.error('can not handle: %s' % repr(ip_packet))
            should_accept = True
        if should_accept:
            nfqueue_element.accept()
        else:
            nfqueue_element.drop()
        tcp_service_status.last_activity_at = time.time()
    except:
        LOGGER.exception('failed to handle packet')
        nfqueue_element.accept()


def handle_rst(rst):
    src = socket.inet_ntoa(rst.src)
    expected_ttl = syn_ack_ttl.get((src, rst.tcp.sport)) or 0
    if expected_ttl and abs(rst.ttl - expected_ttl) > 2:
        log_jamming_event(src, 'tcp rst spoofing')
        LOGGER.error(
            'received RST from GFW: expected ttl is %s, actually is %s, the packet %s' %
            (expected_ttl, rst.ttl, format_ip_packet(rst)))
    return False # might help when HTTP REDIRECT being detected by GFW


def handle_syn(syn):
    dst = socket.inet_ntoa(syn.dst)
    if '127.0.0.1' == dst:
        return
    if dst not in pending_syn and dst not in domestic_zone and dst not in international_zone \
        and not pending_connection.is_ip_pending(dst):
        pending_syn[dst] = time.time()
    for ip, sent_at in pending_syn.items():
        elapsed_seconds = time.time() - sent_at
        if elapsed_seconds > 2:
            log_jamming_event(ip, 'syn packet drop')
            del pending_syn[ip]
            full_proxy_service.add_to_black_list(ip, syn=syn)
            return False
    return True


def handle_psh_ack(psh_ack):
    pos = psh_ack.tcp.data.find('Host:')
    if -1 == pos:
        return True
    pos += len('Host')
    ttl_to_gfw = international_zone.get(socket.inet_ntoa(psh_ack.dst))
    if not ttl_to_gfw:
        return True
    inject_scrambled_http_get_to_let_gfw_type2_miss_keyword(psh_ack, pos, ttl_to_gfw)
    return False


def handle_syn_ack(syn_ack):
    uncertain_ip = socket.inet_ntoa(syn_ack.src)
    full_proxy_service.add_to_white_list(uncertain_ip)
    if uncertain_ip in pending_syn:
        del pending_syn[uncertain_ip]
    expected_ttl = syn_ack_ttl.get((uncertain_ip, syn_ack.tcp.sport)) or 0
    if expected_ttl and abs(syn_ack.ttl - expected_ttl) > 2:
        log_jamming_event(uncertain_ip, 'tcp syn ack spoofing')
        LOGGER.error(
            'received spoofed SYN ACK: expected ttl is %s, actually is %s, the packet %s' %
            (expected_ttl, syn_ack.ttl, format_ip_packet(syn_ack)))
    syn_ack_ttl[
        (uncertain_ip, syn_ack.tcp.sport)] = syn_ack.ttl # later one should be the correct one as GFW is closer to us
    if uncertain_ip in international_zone:
        inject_poison_ack_to_fill_gfw_buffer_with_garbage(syn_ack, international_zone[uncertain_ip])
        return True
    elif uncertain_ip in domestic_zone:
        return True
    elif pending_connection.is_ip_pending(uncertain_ip):
        pending_connection.record_syn_ack(syn_ack)
        timeouted = pending_connection.is_ip_timeouted(uncertain_ip)
        if timeouted:
            international_ip = uncertain_ip
            ttl_to_gfw = pending_connection.get_ttl_to_gfw(international_ip, exact_match_only=False)
            LOGGER.info('treat ip as international due to timeout: %s, %s, %s' %
                        (international_ip, ttl_to_gfw, pending_connection.get_detected_routers(international_ip)))
            add_international_ip(international_ip, (ttl_to_gfw or DEFAULT_TTL_TO_GFW) - SAFETY_DELTA)
        return False
    elif china_ip.is_china_ip(uncertain_ip):
        domestic_ip = uncertain_ip
        LOGGER.info('found domestic ip: %s' % domestic_ip)
        domestic_zone.add(domestic_ip)
        return True
    else:
        pending_connection.record_syn_ack(syn_ack)
        inject_ping_requests_to_find_right_ttl(uncertain_ip)
        return False


def log_jamming_event(ip, event):
    event = '%s: %s %s' % (dns_service.get_domain(ip) or 'unknown.com', ip, event)
    LOGGER.error('jamming event: %s' % event)


def inject_ping_requests_to_find_right_ttl(dst):
    def find_probe_src():
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect((dst, 80))
            return s.getsockname()[0]
        finally:
            s.close()

    LOGGER.debug('inject ping request: %s' % dst)
    for ttl in RANGE_OF_TTL_TO_GFW:
        icmp_packet = dpkt.icmp.ICMP(type=dpkt.icmp.ICMP_ECHO, data=dpkt.icmp.ICMP.Echo(id=ttl, seq=1, data=''))
        ip_packet = dpkt.ip.IP(src=socket.inet_aton(find_probe_src()), dst=socket.inet_aton(dst),
                               p=dpkt.ip.IP_PROTO_ICMP)
        ip_packet.ttl = ttl
        ip_packet.data = icmp_packet
        raw_socket.sendto(str(ip_packet), (dst, 0))
        raw_socket.sendto(str(ip_packet), (dst, 0))


def handle_time_exceeded(ip_packet):
    time_exceed = ip_packet.icmp.data
    if not isinstance(time_exceed.data, dpkt.ip.IP):
        return
    te_ip_packet = time_exceed.data
    if not isinstance(te_ip_packet.data, dpkt.icmp.ICMP):
        return
    te_icmp_packet = te_ip_packet.data
    if not isinstance(te_icmp_packet.data, dpkt.icmp.ICMP.Echo):
        return
    te_icmp_echo = te_icmp_packet.data
    ttl = te_icmp_echo.id
    dst_ip = socket.inet_ntoa(te_ip_packet.dst)
    router_ip = socket.inet_ntoa(ip_packet.src)
    is_china_router = china_ip.is_china_ip(router_ip)
    if is_china_router and MAX_TTL_TO_GFW == ttl:
        LOGGER.info('treat ip as domestic as max ttl is still in china: %s, %s' %
                    (dst_ip, pending_connection.get_detected_routers(dst_ip)))
        add_domestic_ip(dst_ip)
        return
    else:
        pending_connection.record_router(dst_ip, ttl, is_china_router)
        ttl_to_gfw = pending_connection.get_ttl_to_gfw(dst_ip)
        if ttl_to_gfw:
            LOGGER.info('found ttl to gfw: %s %s' % (dst_ip, ttl_to_gfw - SAFETY_DELTA))
            add_international_ip(dst_ip, ttl_to_gfw - SAFETY_DELTA)


def add_international_ip(international_ip, ttl):
    international_zone[international_ip] = ttl
    syn_ack_packets = pending_connection.pop_syn_ack_packets(international_ip)
    for syn_ack in syn_ack_packets:
        inject_poison_ack_to_fill_gfw_buffer_with_garbage(syn_ack, ttl)
    for syn_ack in syn_ack_packets:
        inject_back_syn_ack(syn_ack)


def add_domestic_ip(domestic_ip):
    domestic_zone.add(domestic_ip)
    syn_ack_packets = pending_connection.pop_syn_ack_packets(domestic_ip)
    for syn_ack in syn_ack_packets:
        inject_back_syn_ack(syn_ack)


def inject_back_syn_ack(syn_ack):
    raw_socket.sendto(str(syn_ack), (socket.inet_ntoa(syn_ack.dst), 0))


def format_ip_packet(ip_packet):
    return '%s=>%s %s' % (socket.inet_ntoa(ip_packet.src), socket.inet_ntoa(ip_packet.dst), repr(ip_packet))

#=== important ===
# above is just about finding the right time and right TTL
# below is doing the real stuff at the chosen time using the exact TTL

def inject_scrambled_http_get_to_let_gfw_type2_miss_keyword(psh_ack, pos, ttl_to_gfw):
# we still need to make the keyword less obvious by splitting the packet into two
# to make it harder to rebuilt the stream, we injected two more fake packets to poison the stream
# first_packet .. fake_second_packet => GFW ? wrong
# fake_first_packet .. second_packet => GFW ? wrong
# first_packet .. second_packet => server ? yes, it is a HTTP GET
    dst = socket.inet_ntoa(psh_ack.dst)
    LOGGER.debug('inject scrambled ack: %s' % dst)
    first_part = psh_ack.tcp.data[:pos]
    second_part = psh_ack.tcp.data[pos:]

    second_packet = dpkt.ip.IP(str(psh_ack))
    second_packet.ttl = 255
    second_packet.tcp.seq += len(first_part)
    second_packet.tcp.data = second_part
    second_packet.sum = 0
    second_packet.tcp.sum = 0
    raw_socket.sendto(str(second_packet), (dst, 0))

    fake_first_packet = dpkt.ip.IP(str(psh_ack))
    fake_first_packet.ttl = ttl_to_gfw
    fake_first_packet.tcp.data = (len(first_part) + 10) * '0'
    fake_first_packet.sum = 0
    fake_first_packet.tcp.sum = 0
    raw_socket.sendto(str(fake_first_packet), (dst, 0))

    fake_second_packet = dpkt.ip.IP(str(psh_ack))
    fake_second_packet.ttl = ttl_to_gfw
    fake_second_packet.tcp.seq += len(first_part) + 10
    fake_second_packet.tcp.data = ': baidu.com\r\n\r\n'
    fake_second_packet.sum = 0
    fake_second_packet.tcp.sum = 0
    raw_socket.sendto(str(fake_second_packet), (dst, 0))

    first_packet = dpkt.ip.IP(str(psh_ack))
    first_packet.ttl = 255
    first_packet.tcp.data = first_part
    first_packet.sum = 0
    first_packet.tcp.sum = 0
    raw_socket.sendto(str(first_packet), (dst, 0))


def inject_poison_ack_to_fill_gfw_buffer_with_garbage(syn_ack, ttl_to_gfw):
# poison ack should blind most GFW checking rules
# because the seq of the tcp is the same, GFW will take the first one for the same seq
# seq 1: xxx
# seq 1: yyy
# GFW think it is xxx
# server think it is yyy
    dst = socket.inet_ntoa(syn_ack.src)
    LOGGER.debug('inject poison ack: %s' % dst)
    tcp_packet = dpkt.tcp.TCP(
        sport=syn_ack.tcp.dport, dport=syn_ack.tcp.sport,
        flags=dpkt.tcp.TH_ACK, seq=syn_ack.tcp.ack, ack=syn_ack.tcp.seq + 1,
        data='', opts='')
    ip_packet = dpkt.ip.IP(dst=syn_ack.src, src=syn_ack.dst, p=dpkt.ip.IP_PROTO_TCP, ttl=ttl_to_gfw)
    ip_packet.data = ip_packet.tcp = tcp_packet
    raw_socket.sendto(str(ip_packet), (dst, 0))
    tcp_packet = dpkt.tcp.TCP(
        sport=syn_ack.tcp.dport, dport=syn_ack.tcp.sport,
        flags=dpkt.tcp.TH_ACK, seq=syn_ack.tcp.ack, ack=syn_ack.tcp.seq + 1,
        data=5 * '0', opts='')
    ip_packet = dpkt.ip.IP(dst=syn_ack.src, src=syn_ack.dst, p=dpkt.ip.IP_PROTO_TCP, ttl=ttl_to_gfw)
    ip_packet.data = ip_packet.tcp = tcp_packet
    raw_socket.sendto(str(ip_packet), (dst, 0))


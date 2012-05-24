# utility node used to test if the host/network support IP SPOOFING
# support of IP SPOOFING is a hard requirement for the IMPERSONATOR node
from contextlib import closing
import logging
import socket
from dpkt import ip, udp
import sys

LOGGER = logging.getLogger(__name__)
REQUEST_PING = 'ping'
RESPONSE_PONG = 'pong'
SPOOF_IP = '8.8.8.8'
SPOOF_PORT = 1984

def ping(args):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.sendto(REQUEST_PING, (args.host, args.port))
    LOGGER.info('Sent PING to %s:%s' % (args.host, args.port))
    response, (response_src, response_sport) = s.recvfrom(4096)
    if RESPONSE_PONG != response:
        LOGGER.error('Unexpected response: %s' % response)
        sys.exit(1)
    if SPOOF_IP != response_src:
        LOGGER.error('Unexpected src: %s' % response_src)
        sys.exit(1)
    if SPOOF_PORT != response_sport:
        LOGGER.error('Unexpected sport: %s' % response_sport)
        sys.exit(1)
    LOGGER.info('Received PONG as if it is from %s:%s' % (response_src, response_sport))


def pong(args):
    with closing(socket.socket(socket.AF_INET, socket.SOCK_DGRAM)) as server_socket:
        server_socket.bind(('0.0.0.0', args.port))
        request, (ping_ip, ping_port) = server_socket.recvfrom(4096)
        if REQUEST_PING != request:
            LOGGER.error('Unexpected request: %s' % request)
    LOGGER.info('Received PONG from %s:%s' % (ping_ip, ping_port))
    segment = udp.UDP(sport=SPOOF_PORT, dport=int(ping_port), sum=0)
    segment.data = 'pong'
    segment.ulen = len(segment)
    packet = ip.IP(src=socket.inet_aton(SPOOF_IP), dst=socket.inet_aton(ping_ip), p=ip.IP_PROTO_UDP)
    packet.data = segment
    packet.len += len(packet.data)
    with closing(socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)) as raw_socket:
        raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)
        raw_socket.sendto(str(packet), (ping_ip, 0))
    LOGGER.info('Sent PING as if it is from %s:%s' % (SPOOF_IP, SPOOF_PORT))

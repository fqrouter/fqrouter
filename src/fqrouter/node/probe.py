from contextlib import closing
import json
import socket
import logging
from dpkt import ip, tcp

LOGGER = logging.getLogger(__name__)
DEFAULT_PORT = 19840

def start(args):
    with closing(socket.socket(socket.AF_INET, socket.SOCK_DGRAM)) as s:
        s.bind(('0.0.0.0', args.port))
        while True:
            probe_request = json.loads(s.recv(4096))
            if LOGGER.isEnabledFor(logging.DEBUG):
                LOGGER.debug('[PROBE REQUEST] %s:%s' % (probe_request['dst'], probe_request['dport']))
            for guess_id, guess in probe_request['guesses'].items():
                verify_guess(probe_request['dst'], probe_request['dport'], guess_id, guess)


def verify_guess(dst, dport, guess_id, guess):
    ip_packet = ip.IP(src=socket.inet_aton(dst), dst=socket.inet_aton(guess['ip']), p=ip.IP_PROTO_TCP)
    ip_packet.data = tcp.TCP(sport=int(dport), dport=int(guess['port']), ack=int(guess_id), flags=tcp.TH_SYN | tcp.TH_ACK)
    ip_packet.len += len(ip_packet.data)
    with closing(socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)) as s:
        s.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)
        s.sendto(str(ip_packet), (guess['ip'], 0))




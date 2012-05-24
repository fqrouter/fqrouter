# send ip packet on behalf of SMUGGLER to SERVER
# SMUGGLER =IP/UDP/ORIG_IP_PACKET=> IMPERSONATOR =ORIG_IP_PACKET=> SERVER
from contextlib import closing
import socket
from dpkt import ip

def start(args):
    with closing(socket.socket(socket.AF_INET, socket.SOCK_DGRAM)) as server_socket:
        server_socket.bind(('0.0.0.0', args.port))
        with closing(socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)) as raw_socket:
            raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)
            while True:
                packet_bytes = server_socket.recv(4096)
                packet = ip.IP(packet_bytes)
                dst = socket.inet_ntoa(packet.dst)
                print('%s:%s => %s:%s' % (socket.inet_ntoa(packet.src), packet.data.sport, dst, packet.data.dport))
                raw_socket.sendto(packet_bytes, (dst, 0))


# send ip packet on behalf of SMUGGLER to SERVER
# SMUGGLER =IP/UDP/ORIG_IP_PACKET=> IMPERSONATOR =ORIG_IP_PACKET=> SERVER
from contextlib import closing
import socket

def start(args):
    with closing(socket.socket(socket.AF_INET, socket.SOCK_DGRAM)) as server_socket:
        server_socket.bind(('0.0.0.0', args.port))
        with closing(socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)) as raw_socket:
            raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)
            while True:
                packet = server_socket.recv(4096)
                raw_socket.send(packet)


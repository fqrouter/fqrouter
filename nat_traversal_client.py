import socket
import sys

server_ip = sys.argv[1]
server_port = int(sys.argv[2])

def punch_tcp_hole():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(('0.0.0.0', 19841))
    try:
        s.connect((server_ip, server_port))
        data = s.recv(1024)
        outbound_ip, outbound_port = data.split(':')
        print(outbound_ip, outbound_port)
    finally:
        s.close()

punch_tcp_hole()
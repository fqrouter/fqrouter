import socket
import sys

bind_ip = sys.argv[1]
bind_port = int(sys.argv[2])

def start_server():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind((bind_ip, bind_port))
    s.listen(1)
    while True:
        conn, client_addr = s.accept()
        client_ip, client_port = client_addr
        try:
            conn.send('%s:%s' % (client_ip, client_port))
        finally:
            conn.close()

start_server()
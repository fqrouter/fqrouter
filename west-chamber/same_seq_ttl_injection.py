from netfilterqueue import NetfilterQueue
import time
import socket
import traceback
import dpkt

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)

def inject_same_seq_dummy_packet(nfqueue_element):
    try:
        ip_packet = dpkt.ip.IP(nfqueue_element.get_payload())
        ip_packet.ttl = 10
        tcp_packet = ip_packet.tcp
        tcp_packet.data = 5 * '0'
        tcp_packet.sum = 0
        ip_packet.sum = 0
        raw_socket.sendto(str(ip_packet), (socket.inet_ntoa(ip_packet.dst), 0))
        nfqueue_element.accept()
    except:
        traceback.print_exc()
        nfqueue_element.accept()

nfqueue = NetfilterQueue()
nfqueue.bind(0, inject_same_seq_dummy_packet)
try:
    print('running..')
    nfqueue.run()
except KeyboardInterrupt:
    print('bye')

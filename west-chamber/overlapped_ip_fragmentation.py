from netfilterqueue import NetfilterQueue
import time
import socket
import traceback
import dpkt

# It is not working, GFW can deal with overlapped ip fragment correctly

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)

def split_ip_packet_to_overlapped_fragments(nfqueue_element):
    try:
        fragment1 = dpkt.ip.IP(nfqueue_element.get_payload())
        fragment1.off = 1 << 13 # Set More Fragment Flag
        fragment1.data = str(fragment1.data)[:16] + 3 * '0'
        fragment1.sum = 0
        fragment1.len = len(fragment1)
        fragment2 = dpkt.ip.IP(nfqueue_element.get_payload())
        fragment2.off = 2 # Offset is 2 * 8 bytes
        fragment2.data = str(fragment2.data)[16:]
        fragment2.sum = 0
        fragment2.len = len(fragment2)
        raw_socket.sendto(str(fragment1), (socket.inet_ntoa(fragment1.dst), 0))
        nfqueue_element.set_payload(str(fragment2))
        nfqueue_element.accept()
    except:
        traceback.print_exc()
        nfqueue_element.accept()

nfqueue = NetfilterQueue()
nfqueue.bind(0, split_ip_packet_to_overlapped_fragments)
try:
    print('running..')
    nfqueue.run()
except KeyboardInterrupt:
    print('bye')

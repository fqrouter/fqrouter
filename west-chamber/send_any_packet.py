import dpkt
import socket
import binascii

hex_ip_packets = [
]

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)
for hex_ip_packet in hex_ip_packets:
    ip_packet = dpkt.ip.IP(binascii.unhexlify(hex_ip_packet))
    raw_socket.sendto(str(ip_packet), (socket.inet_ntoa(ip_packet.dst), 0))
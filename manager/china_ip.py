import os
import socket
import struct
import math


def load_ip_ranges():
    path = os.path.join(os.path.dirname(__file__), 'delegated-apnic-latest.txt')
    with open(path) as f:
        lines = f.readlines()
    for line in lines:
        line = line.strip()
        if not line:
            continue
        if line.startswith('#'):
            continue
        if 'CN|ipv4' not in line:
            continue
            # apnic|CN|ipv4|223.255.252.0|512|20110414|allocated
        _, _, _, start_ip, ip_count, _, _ = line.split('|')
        start_ip_as_int = ip_to_int(start_ip)
        end_ip_as_int = start_ip_as_int + int(ip_count)
        yield start_ip_as_int, end_ip_as_int
    yield translate_ip_range('111.0.0.0', 10) # china mobile


def translate_ip_range(ip, netmask):
    return ip_to_int(ip), ip_to_int(ip) + int(math.pow(2, 32 - netmask))


def ip_to_int(ip):
    return struct.unpack('!i', socket.inet_aton(ip))[0]


ip_ranges = list(load_ip_ranges())


def is_china_ip(ip):
    ip_as_int = ip_to_int(ip)
    for start_ip_as_int, end_ip_as_int in ip_ranges:
        if start_ip_as_int <= ip_as_int <= end_ip_as_int:
            return True
    return False


if '__main__' == __name__:
    print(is_china_ip('8.8.8.8'))
    print(is_china_ip('219.158.3.44'))
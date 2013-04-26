import socket

from pynetfilter_conntrack.IPy import IP


LOCAL_NETWORKS = [
    IP('0.0.0.0/8'), IP('10.0.0.0/8'), IP('127.0.0.0/8'), IP('169.254.0.0/16'),
    IP('172.16.0.0/12'), IP('192.168.0.0/16'), IP('224.0.0.0/4'), IP('240.0.0.0/4')]


def is_lan_traffic(ip_packet):
    src = IP(socket.inet_ntoa(ip_packet.src))
    dst = IP(socket.inet_ntoa(ip_packet.dst))
    from_lan = is_lan_ip(src)
    to_lan = is_lan_ip(dst)
    return from_lan and to_lan


def is_lan_ip(ip):
    return any(ip in network for network in LOCAL_NETWORKS)
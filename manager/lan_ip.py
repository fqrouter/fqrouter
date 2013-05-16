import socket
import fqsocks.china_ip

LOCAL_NETWORKS = [
    fqsocks.china_ip.translate_ip_range('0.0.0.0', 8),
    fqsocks.china_ip.translate_ip_range('10.0.0.0', 8),
    fqsocks.china_ip.translate_ip_range('127.0.0.0', 8),
    fqsocks.china_ip.translate_ip_range('169.254.0.0', 16),
    fqsocks.china_ip.translate_ip_range('172.16.0.0', 12),
    fqsocks.china_ip.translate_ip_range('192.168.0.0', 16),
    fqsocks.china_ip.translate_ip_range('224.0.0.0', 4),
    fqsocks.china_ip.translate_ip_range('240.0.0.0', 4)]


def is_lan_traffic(ip_packet):
    src = socket.inet_ntoa(ip_packet.src)
    dst = socket.inet_ntoa(ip_packet.dst)
    from_lan = is_lan_ip(src)
    to_lan = is_lan_ip(dst)
    return from_lan and to_lan


def is_lan_ip(ip):
    ip_as_int = fqsocks.china_ip.ip_to_int(ip)
    return any(start_ip_as_int <= ip_as_int <= end_ip_as_int for start_ip_as_int, end_ip_as_int in LOCAL_NETWORKS)
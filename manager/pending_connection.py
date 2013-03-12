import socket
import time
# used to track syn ack we are holding before we know a proper TTL to inject
SYN_ACK_TIMEOUT = 2
pending = {}


class PendingConnection(object):
    def __init__(self):
        self.started_at = time.time()
        self.syn_ack_packets = {} # (dst, dport, sport) => packet


def is_ip_pending(ip):
    return ip in pending


def record_syn_ack(syn_ack):
    src = socket.inet_ntoa(syn_ack.src)
    dst = socket.inet_ntoa(syn_ack.dst)
    key = (dst, syn_ack.tcp.dport, syn_ack.tcp.sport)
    pending.setdefault(src, PendingConnection()).syn_ack_packets[key] = syn_ack


def pop_timeout_syn_ack_packets(ip):
    connection = pending.get(ip)
    if not connection:
        return ()
    if time.time() - connection.started_at > SYN_ACK_TIMEOUT:
        packets = connection.syn_ack_packets.values()
        pending.pop(ip)
        return packets
    else:
        return ()
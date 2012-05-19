import nfqueue
import socket
from dpkt import ip, tcp

def handle_output(payload):
    ip_packet = ip.IP(payload.get_data())
    if ip.IP_PROTO_TCP != ip_packet.p:
        return
    tcp_packet = ip_packet.data
    if not (tcp.TH_SYN & tcp_packet.flags):
        return
    handle_syn(payload, ip_packet)


def handle_syn(payload, ip_packet):
    ip_packet.ttl = 5
    ip_packet.sum = 0 # force re-compute
    payload.set_verdict_modified(nfqueue.NF_ACCEPT, str(ip_packet), len(str(ip_packet)))


def monitor_output():
    q = nfqueue.queue()
    try:
        q.set_callback(handle_output)
        q.fast_open(0, socket.AF_INET)
        q.set_queue_maxlen(50000)
        q.try_run()
    finally:
        q.unbind(socket.AF_INET)
        q.close()
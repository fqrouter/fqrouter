import functools
import random
import nfqueue
import socket
from dpkt import ip, tcp

def handle_output(external_ips, payload):
    ip_packet = ip.IP(payload.get_data())
    if ip.IP_PROTO_TCP != ip_packet.p:
        return
    tcp_packet = ip_packet.data
    if not (tcp.TH_SYN & tcp_packet.flags):
        return
    handle_syn(external_ips, payload, ip_packet)


def handle_syn(external_ips, payload, ip_packet):
    ip_packet.ttl = 5
    ip_packet.sum = 0 # force re-compute
    payload.set_verdict_modified(nfqueue.NF_ACCEPT, str(ip_packet), len(str(ip_packet)))
    tcp_packet = ip_packet.data
    guesses = []
    for external_ip in external_ips:
        for port_offset in range(2):
            guesses.append({
                'id': random.randint(19840, 20120),
                'ip': external_ip,
                'port': tcp_packet.sport + port_offset
            })
    print(guesses)


def monitor_output(external_ips, queue_number):
    q = nfqueue.queue()
    try:
        q.set_callback(functools.partial(handle_output, external_ips))
        q.fast_open(queue_number, socket.AF_INET)
        q.set_queue_maxlen(50000)
        q.try_run()
    finally:
        q.unbind(socket.AF_INET)
        q.close()
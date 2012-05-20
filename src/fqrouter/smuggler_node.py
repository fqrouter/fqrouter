from contextlib import contextmanager, closing
import functools
import threading
import nfqueue
import socket
from dpkt import ip, tcp
import sys
from . import probe_node
from . import rfc_3489
from . import shell
from .protocol import Protocol

LOCK_PROBE_SOCKET = threading.RLock()

def start(args):
    with forward_output_to_nfqueue():
        with closing(socket.socket(socket.AF_INET, socket.SOCK_DGRAM)) as probe_socket:
            protocol = Protocol(
                detect_reflexive_transport_addresses=functools.partial(detect_reflexive_transport_addresses,
                    rfc_3489_servers=args.rfc_3489_servers),
                monitor_nfqueue=functools.partial(monitor_nfqueue,
                    queue_number=args.queue_number),
                send_guesses=functools.partial(send_guesses,
                    s=probe_socket, addr=args.probe_node))
            protocol.start_smuggler()


@contextmanager
def forward_output_to_nfqueue():
    shell.call('iptables -t mangle -I OUTPUT -p tcp -j NFQUEUE')
    try:
        yield
    finally:
        shell.call('iptables -t mangle -D OUTPUT -p tcp -j NFQUEUE')


def detect_reflexive_transport_addresses(rfc_3489_servers):
    results = []
    for stun_server in rfc_3489_servers:
        if isinstance(stun_server, (list, tuple)):
            results.append(rfc_3489.detect_reflexive_transport_address(*stun_server))
        else:
            results.append(rfc_3489.detect_reflexive_transport_address(stun_server))
    results = [r.get(ignores_error=True) for r in results if r.get(ignores_error=True)]
    return results


def monitor_nfqueue(queue_number):
    q = nfqueue.queue()
    try:
        q.open()
        try:
            q.unbind(socket.AF_INET)
            q.bind(socket.AF_INET)
        except:
            print('Can not bind to nfqueue, are you running as ROOT?')
            sys.exit(1)
        q.set_callback(handle_packet)
        q.create_queue(queue_number)
        q.try_run()
    finally:
        try:
            q.unbind(socket.AF_INET)
        except:
            pass # tried your best
        try:
            q.close()
        except:
            pass # tried your best


def handle_packet(nfqueue_element):
    ip_packet = ip.IP(nfqueue_element.get_data())
    if ip.IP_PROTO_TCP != ip_packet.p:
        return
    tcp_packet = ip_packet.data
    if not (tcp.TH_SYN & tcp_packet.flags):
        return
    Protocol.INSTANCE.on_outbound_syn(nfqueue_element)


def send_guesses(s, addr, data):
    with LOCK_PROBE_SOCKET:
        s.sendto(data, parse_addr(addr, probe_node.DEFAULT_PORT))


def parse_addr(addr, default_port):
    if ':' in addr:
        return addr.split(':')
    else:
        return addr, default_port
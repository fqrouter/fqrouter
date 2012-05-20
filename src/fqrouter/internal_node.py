import argparse
import functools
import subprocess
import nfqueue
import socket
from dpkt import ip, tcp
import sys
from .protocol import GamblingProtocol
from . import shell

def start(args):
    shell.call('iptables -t mangle -I OUTPUT -p tcp -j NFQUEUE')
    try:
        sockets = {'guessing': socket.socket(socket.AF_INET, socket.SOCK_DGRAM)}
        send_guesses = functools.partial(send_to_external_node,
            s=sockets['guessing'], **parse_addr(args.verify_guesses_via, 19840))
        protocol = GamblingProtocol()
        protocol.on_start(
            monitor_nfqueue=functools.partial(monitor_nfqueue,
                protocol=protocol,
                queue_number=args.queue_number,
                send_guesses=send_guesses),
            rfc_3489_servers=args.rfc_3489_servers)
    finally:
        shell.call('iptables -t mangle -D OUTPUT -p tcp -j NFQUEUE')


def parse_addr(addr, default_port):
    if ':' in addr:
        host, port = addr.split(':')
    else:
        host, port = addr, default_port
    return {
        'host': host,
        'port': port
    }

def send_to_external_node(buf,
                          # bound by functools.partial
                          s, host, port):
    s.sendto(buf, (host, port))


def monitor_nfqueue(external_ips,
                    # bound by functools.partial
                    protocol, queue_number, send_guesses):
    q = nfqueue.queue()
    try:
        q.open()
        try:
            q.unbind(socket.AF_INET)
            q.bind(socket.AF_INET)
        except:
            print('Can not bind to nfqueue, are you running as ROOT?')
            sys.exit(1)
        q.set_callback(functools.partial(handle_packet,
            protocol=protocol,
            external_ips=external_ips,
            send_guesses=send_guesses))
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


def handle_packet(nfqueue_element,
                  # bound by functools.partial
                  protocol, external_ips, send_guesses):
    ip_packet = ip.IP(nfqueue_element.get_data())
    if ip.IP_PROTO_TCP != ip_packet.p:
        return
    tcp_packet = ip_packet.data
    if not (tcp.TH_SYN & tcp_packet.flags):
        return
    protocol.on_outbound_syn(
        nfqueue_element=nfqueue_element,
        external_ips=external_ips,
        send_guesses=send_guesses)

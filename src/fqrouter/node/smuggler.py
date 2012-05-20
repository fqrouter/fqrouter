from contextlib import contextmanager, closing
import json
import logging
import random
import socket
import sys
import traceback
from dpkt import tcp, ip
try:
    import nfqueue
except ImportError:
    nfqueue = None
from fqrouter.node import probe
from fqrouter.utility import rfc_3489
from fqrouter.utility import shell

LOGGER = logging.getLogger(__name__)

def start(args):
    if not nfqueue:
        LOGGER.error('No nfqueue')
        sys.exit(1)
    socket.setdefaulttimeout(5)
    probe_node_host, probe_node_port = parse_addr(args.probe_node, probe.DEFAULT_PORT)
    external_ips = check_NAT(args.rfc_3489_servers)
    with forward_output_to_nfqueue():
        smuggler = Smuggler(
            external_ips=external_ips,
            probe_node_host=probe_node_host,
            probe_node_port=probe_node_port)
        smuggler.monitor_nfqueue(args.queue_number)


class Smuggler(object):
    def __init__(self, external_ips, probe_node_host, probe_node_port):
        super(Smuggler, self).__init__()
        self.external_ips = external_ips
        self.probe_node_host = probe_node_host
        self.probe_node_port = probe_node_port

    def monitor_nfqueue(self, queue_number):
        q = nfqueue.queue()
        try:
            q.open()
            try:
                q.unbind(socket.AF_INET)
                q.bind(socket.AF_INET)
            except:
                print('Can not bind to nfqueue, are you running as ROOT?')
                sys.exit(1)
            q.set_callback(self.on_nfqueue_element)
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

    def on_nfqueue_element(self, nfqueue_element):
        ip_packet = ip.IP(nfqueue_element.get_data())
        if ip.IP_PROTO_TCP != ip_packet.p:
            return
        tcp_packet = ip_packet.data
        if tcp.TH_SYN == tcp_packet.flags:
            self.on_outbound_syn(nfqueue_element)

    def on_outbound_syn(self, nfqueue_element):
        ip_packet = ip.IP(nfqueue_element.get_data())
        tcp_packet = ip_packet.data
        if LOGGER.isEnabledFor(logging.DEBUG):
            LOGGER.debug('Received SYN from %s:%s to %s:%s'
            % (socket.inet_ntoa(ip_packet.src), tcp_packet.sport, socket.inet_ntoa(ip_packet.dst), tcp_packet.dport))
        self.make_guesses_then_send(ip_packet, tcp_packet)

    def make_guesses_then_send(self, ip_packet, tcp_packet):
        guesses = {}
        for external_ip in self.external_ips:
            for port_offset in range(2):
                guesses[generate_guess_id(set(tcp_packet.seq + 1).union(guesses.keys()))] = {
                    'ip': external_ip,
                    'port': tcp_packet.sport + port_offset
                }
        self.send_probe_request(data=json.dumps({
            'dst': socket.inet_ntoa(ip_packet.dst),
            'dport': tcp_packet.dport,
            'guesses': guesses
        }))
        if LOGGER.isEnabledFor(logging.DEBUG):
            LOGGER.debug('Sent PROBE REQUEST: %s' % str(guesses))

    def send_probe_request(self, data):
        with closing(socket.socket(socket.AF_INET, socket.SOCK_DGRAM)) as s:
            s.sendto(data, (self.probe_node_host, self.probe_node_port))


@contextmanager
def forward_output_to_nfqueue():
    shell.call('iptables -t mangle -I OUTPUT -p tcp -j NFQUEUE')
    try:
        yield
    except:
        traceback.print_exc()
    finally:
        shell.call('iptables -t mangle -D OUTPUT -p tcp -j NFQUEUE')


def check_NAT(rfc_3489_servers):
    LOGGER.debug('Checking NAT...')
    results = []
    for stun_server in rfc_3489_servers:
        if isinstance(stun_server, (list, tuple)):
            results.append(rfc_3489.detect_reflexive_transport_address(*stun_server, logs_exception=False))
        else:
            results.append(rfc_3489.detect_reflexive_transport_address(stun_server, logs_exception=False))
    results = [r.get(ignores_error=True) for r in results if r.get(ignores_error=True)]
    external_ips = set()
    for result in results:
        external_addr, local_port = result
        external_ip, external_port = external_addr
        if not (local_port <= external_port <= local_port + 5):
            LOGGER.error('Can not guess port from this type of NAT')
            sys.exit(1)
        external_ips.add(external_ip)
    LOGGER.debug('External IP(s): %s' % str(external_ips))
    return external_ips


def parse_addr(addr, default_port):
    if ':' in addr:
        return addr.split(':')
    else:
        return addr, default_port


def generate_guess_id(used_ids):
    id = random.randint(1, 65534)
    while id in used_ids:
        id = random.randint(1, 65534)
    return id

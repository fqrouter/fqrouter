from contextlib import contextmanager, closing
import json
import logging
import random
import socket
import sys
import threading
from dpkt import tcp, ip

try:
    import nfqueue
except ImportError:
    nfqueue = None
from fqrouter.node import probe
from fqrouter.utility import rfc_3489
from fqrouter.utility import shell

LOGGER = logging.getLogger(__name__)
MARK_OUTBOUND = 1
MARK_INBOUND = 2

handlers = {}

def start(args):
    if not nfqueue:
        LOGGER.error('No nfqueue')
        sys.exit(1)
    socket.setdefaulttimeout(5)
    probe_node_host, probe_node_port = parse_addr(args.probe_node, probe.DEFAULT_PORT)
    external_ips = check_NAT(args.rfc_3489_servers)
    connection_tracker = ConnectionTracker()
    handlers['=>syn'] = OutboundSynHandler(connection_tracker, external_ips, probe_node_host, probe_node_port)
    handlers['<=syn+ack'] = InboundSynAckHandler(connection_tracker)
    with mark_outbound_packets():
        with forward_outbound_packets_to_nfqueue():
            with mark_inbound_packets():
                with forward_inbound_packets_to_nfqueue():
                    monitor_nfqueue(args.queue_number)


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
        q.set_callback(on_nfqueue_element)
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


def on_nfqueue_element(nfqueue_element):
    if MARK_OUTBOUND == nfqueue_element.get_nfmark():
        return on_outbound_nfqueue_element(nfqueue_element)
    if MARK_INBOUND == nfqueue_element.get_nfmark():
        return on_inbound_nfqueue_element(nfqueue_element)


def on_outbound_nfqueue_element(nfqueue_element):
    ip_packet = ip.IP(nfqueue_element.get_data())
    if ip.IP_PROTO_TCP != ip_packet.p:
        return
    tcp_packet = ip_packet.data
    if tcp.TH_SYN == tcp_packet.flags:
        handler = handlers.get('=>syn')
        if handler:
            handler(nfqueue_element)


def on_inbound_nfqueue_element(nfqueue_element):
    ip_packet = ip.IP(nfqueue_element.get_data())
    if ip.IP_PROTO_TCP != ip_packet.p:
        return
    tcp_packet = ip_packet.data
    if (tcp.TH_SYN | tcp.TH_ACK) == tcp_packet.flags:
        handler = handlers.get('<=syn+ack')
        if handler:
            handler(nfqueue_element)


class OutboundSynHandler(object):
    def __init__(self, connection_tracker, external_ips, probe_node_host, probe_node_port):
        super(OutboundSynHandler, self).__init__()
        self.connection_tracker = connection_tracker
        self.external_ips = external_ips
        self.probe_node_host = probe_node_host
        self.probe_node_port = probe_node_port

    def __call__(self, nfqueue_element):
        ip_packet = ip.IP(nfqueue_element.get_data())
        tcp_packet = ip_packet.data
        if LOGGER.isEnabledFor(logging.DEBUG):
            LOGGER.debug('Received SYN from %s:%s to %s:%s'
            % (socket.inet_ntoa(ip_packet.src), tcp_packet.sport, socket.inet_ntoa(ip_packet.dst), tcp_packet.dport))
        guesses = {}
        for external_ip in self.external_ips:
            for port_offset in range(2):
                guesses[self.generate_guess_id(set([tcp_packet.seq + 1]).union(guesses.keys()))] = {
                    'ip': external_ip,
                    'port': tcp_packet.sport + port_offset
                }
        probe_request = {'dst': socket.inet_ntoa(ip_packet.dst), 'dport': tcp_packet.dport, 'guesses': guesses}
        self.connection_tracker.on_outbound_syn(ip_packet, tcp_packet, probe_request)
        self.send_probe_request(data=json.dumps(probe_request))
        if LOGGER.isEnabledFor(logging.DEBUG):
            LOGGER.debug('Sent PROBE REQUEST: %s' % str(guesses))

    def send_probe_request(self, data):
        with closing(socket.socket(socket.AF_INET, socket.SOCK_DGRAM)) as s:
            s.sendto(data, (self.probe_node_host, self.probe_node_port))

    @staticmethod
    def generate_guess_id(used_ids):
        id = random.randint(1, 65534)
        while id in used_ids:
            id = random.randint(1, 65534)
        return id


class InboundSynAckHandler(object):
    def __init__(self, connection_tracker):
        super(InboundSynAckHandler, self).__init__()
        self.connection_tracker = connection_tracker

    def __call__(self, nfqueue_element):
        ip_packet = ip.IP(nfqueue_element.get_data())
        tcp_packet = ip_packet.data
        self.connection_tracker.on_inbound_syn_ack(ip_packet, tcp_packet)


class ConnectionTracker(object):
    def __init__(self):
        super(ConnectionTracker, self).__init__()
        self.connections = {} # (src, sport, dst, dport) => connection
        self.lock = threading.RLock()

    def on_outbound_syn(self, ip_packet, tcp_packet, probe_request):
        key = (ip_packet.src, tcp_packet.sport, ip_packet.dst, tcp_packet.dport)
        with self.lock:
            self.connections[key] = Connection(ip_packet, probe_request['guesses'])

    def on_inbound_syn_ack(self, ip_packet, tcp_packet):
        key = (ip_packet.dst, tcp_packet.dport, ip_packet.src, tcp_packet.sport)
        with self.lock:
            connection = self.connections.get(key)
        if connection:
            connection.on_inbound_syn_ack(ip_packet, tcp_packet)


class Connection(object):
    def __init__(self, syn_ip_packet, guesses):
        super(Connection, self).__init__()
        self.syn_ip_packet = syn_ip_packet
        self.guesses = guesses
        self.lock = threading.RLock()

    def on_inbound_syn_ack(self, ip_packet, tcp_packet):
        guess = self.guesses.get(tcp_packet.ack)
        if guess:
            LOGGER.info('We found it! %s' % guess)


@contextmanager
def mark_outbound_packets():
    shell.call('iptables -t mangle -A OUTPUT -p tcp -j MARK --set-mark %s' % MARK_OUTBOUND)
    try:
        yield
    finally:
        shell.call('iptables -t mangle -D OUTPUT -p tcp -j MARK --set-mark %s' % MARK_OUTBOUND)


@contextmanager
def forward_outbound_packets_to_nfqueue():
    shell.call('iptables -t mangle -A OUTPUT -p tcp -j NFQUEUE')
    try:
        yield
    finally:
        shell.call('iptables -t mangle -D OUTPUT -p tcp -j NFQUEUE')


@contextmanager
def mark_inbound_packets():
    shell.call('iptables -t mangle -A INPUT -p tcp -j MARK --set-mark %s' % MARK_INBOUND)
    try:
        yield
    finally:
        shell.call('iptables -t mangle -D INPUT -p tcp -j MARK --set-mark %s' % MARK_INBOUND)


@contextmanager
def forward_inbound_packets_to_nfqueue():
    shell.call('iptables -t mangle -A INPUT -p tcp -j NFQUEUE')
    try:
        yield
    finally:
        shell.call('iptables -t mangle -D INPUT -p tcp -j NFQUEUE')


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

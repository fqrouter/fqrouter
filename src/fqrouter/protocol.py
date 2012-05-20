import json
import logging
import random
import socket
from dpkt import ip

LOGGER = logging.getLogger(__name__)

# send/receive packets
class Protocol(object):
    def __init__(self, detect_reflexive_transport_addresses, monitor_nfqueue, send_guesses):
        super(Protocol, self).__init__()
        self.detect_reflexive_transport_addresses = detect_reflexive_transport_addresses
        self.monitor_nfqueue = monitor_nfqueue
        self.send_guesses = send_guesses
        self.external_ips = set()
        Protocol.INSTANCE = self

    def start_smuggler(self):
        LOGGER.debug('Detecting reflexive transport addresses...')
        results = self.detect_reflexive_transport_addresses()
        LOGGER.debug('Reflexive transport addresses: %s' % results)
        for result in results:
            external_addr, local_port = result
            external_ip, external_port = external_addr
            if not (local_port <= external_port <= local_port + 5):
                raise Exception('Can not guess port from this type of NAT')
            self.external_ips.add(external_ip)
        self.monitor_nfqueue()

    def on_outbound_syn(self, nfqueue_element):
        ip_packet = ip.IP(nfqueue_element.get_data())
        tcp_packet = ip_packet.data
        if LOGGER.isEnabledFor(logging.DEBUG):
            LOGGER.debug('Received SYN from %s:%s to %s:%s'
            % (socket.inet_ntoa(ip_packet.src), tcp_packet.sport, socket.inet_ntoa(ip_packet.dst), tcp_packet.dport))
        self.make_guesses_then_send(tcp_packet)

    def make_guesses_then_send(self, tcp_packet):
        guesses = []
        for external_ip in self.external_ips:
            for port_offset in range(2):
                guesses.append({
                    'id': random.randint(19840, 20120),
                    'ip': external_ip,
                    'port': tcp_packet.sport + port_offset
                })
        self.send_guesses(data=json.dumps(guesses))
        if LOGGER.isEnabledFor(logging.DEBUG):
            LOGGER.debug('Sent GUESSES: %s' % guesses)
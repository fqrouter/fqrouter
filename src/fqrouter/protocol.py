import json
import logging
import random
import socket
from dpkt import ip
from . import rfc_3489

LOGGER = logging.getLogger(__name__)

# punch through NAT by rolling the dices
class GamblingProtocol(object):
    def on_start(self, monitor_nfqueue, rfc_3489_servers):
        results = []
        for stun_server in rfc_3489_servers:
            if isinstance(stun_server, (list, tuple)):
                results.append(rfc_3489.detect_reflexive_transport_address(*stun_server))
            else:
                results.append(rfc_3489.detect_reflexive_transport_address(stun_server))
        results = [r.get(ignores_error=True) for r in results if r.get(ignores_error=True)]
        external_ips = set()
        for result in results:
            external_addr, local_port = result
            external_ip, external_port = external_addr
            if not (local_port <= external_port <= local_port + 5):
                raise Exception('Can not guess port from this type of NAT')
            external_ips.add(external_ip)
        monitor_nfqueue(external_ips)

    def on_outbound_syn(self, nfqueue_element, external_ips, send_guesses):
        ip_packet = ip.IP(nfqueue_element.get_data())
        tcp_packet = ip_packet.data
        if LOGGER.isEnabledFor(logging.DEBUG):
            LOGGER.debug('Received SYN from %s:%s to %s:%s'
            % (socket.inet_ntoa(ip_packet.src), tcp_packet.sport, socket.inet_ntoa(ip_packet.dst), tcp_packet.dport))
        guesses = []
        for external_ip in external_ips:
            for port_offset in range(2):
                guesses.append({
                    'id': random.randint(19840, 20120),
                    'ip': external_ip,
                    'port': tcp_packet.sport + port_offset
                })
        send_guesses(json.dumps(guesses))
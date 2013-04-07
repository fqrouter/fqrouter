import logging
import thread
from netfilterqueue import NetfilterQueue
import socket

import dpkt

import shutdown_hook
import iptables
import network_interface


LOGGER = logging.getLogger(__name__)


def run():
    try:
        insert_iptables_rules()
        thread.start_new(start_full_proxy, ())
    except:
        LOGGER.exception('failed to start full proxy service')


def status():
    return 'N/A'


def clean():
    delete_iptables_rules()


#=== private ===


RULES = []
targets = set()

for iface in network_interface.list_data_network_interfaces():
    RULES.append((
        {'target': 'fp_OUTPUT', 'iface_out': iface},
        ('nat', 'OUTPUT', '-o %s -j fp_OUTPUT' % iface)
    ))
    for lan_ip_range in [
        '0.0.0.0/8', '10.0.0.0/8', '127.0.0.0/8', '169.254.0.0/16',
        '172.16.0.0/12', '192.168.0.0/16', '224.0.0.0/4', '240.0.0.0/4']:
        RULES.append((
            {'target': 'RETURN', 'destination': lan_ip_range, 'iface_out': iface},
            ('nat', 'fp_OUTPUT', '-o %s -d %s -j RETURN' % (iface, lan_ip_range))
        ))
    RULES.append((
        {'target': 'NFQUEUE', 'iface_out': iface, 'extra': 'mark match ! 0xbabe NFQUEUE num 3'},
        ('nat', 'fp_OUTPUT', '-o %s -p tcp -m mark ! --mark 0xbabe -j NFQUEUE --queue-num 3' % iface)
    ))
    RULES.append((
        {'target': 'REDIRECT', 'iface_out': iface, 'extra': 'mark match 0xbabe redir ports 12345'},
        ('nat', 'fp_OUTPUT', '-o %s -p tcp -m mark --mark 0xbabe -j REDIRECT --to-ports 12345' % iface)
    ))


def insert_iptables_rules():
    shutdown_hook.add(delete_iptables_rules)
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)
    iptables.delete_chain('fp_OUTPUT')
    iptables.delete_chain('fp_FORWARD')


def start_full_proxy():
    handle_nfqueue()


def handle_nfqueue():
    try:
        nfqueue = NetfilterQueue()
        nfqueue.bind(3, handle_packet)
        nfqueue.run()
    except:
        LOGGER.exception('stopped handling nfqueue')


def handle_packet(nfqueue_element):
    try:
        ip_packet = dpkt.ip.IP(nfqueue_element.get_payload())
        if socket.inet_ntoa(ip_packet.dst) in targets:
            nfqueue_element.set_mark(0xbabe)
            nfqueue_element.repeat()
        else:
            nfqueue_element.accept()
    except:
        LOGGER.exception('failed to handle packet')
        nfqueue_element.accept()


def add_target(ip):
    targets.add(ip)
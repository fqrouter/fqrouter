# send the outbound traffic received from CLIENT to IMPERSONATOR
# CLIENT =NFQUEUE=> SMUGGLER =IP/UDP/ORIG_IP_PACKET=> IMPERSONATOR
from contextlib import contextmanager
import logging
import socket
import sys
from dpkt import ip, udp
from fqrouter.utility import shell

try:
    import nfqueue
except ImportError:
    nfqueue = None

LOGGER = logging.getLogger(__name__)

def not_implemented_handler(nfqueue_element):
    raise NotImplementedError()

handlers = {
    '=>': not_implemented_handler
}

def start(args):
    if not nfqueue:
        LOGGER.error('nfqueue binding not installed')
        sys.exit(1)
    impersonator_host, impersonator_port = parse_addr(args.impersonator_address, 19840)
    impersonator_ip = socket.gethostbyname(impersonator_host)
    handlers['=>'] = DefaultOutboundHandler(
        impersonator_ip, impersonator_port, args.my_internal_ip, args.my_internal_ip)
    with forward_outbound_packets_to_nfqueue():
        monitor_nfqueue()


def parse_addr(addr, default_port):
    parts = addr.split(':')
    if 1 == len(parts):
        return addr, int(default_port)
    elif 2 == len(parts):
        return parts[0], int(parts[1])
    LOGGER.error('%s is not valid address' % addr)
    sys.exit(1)


@contextmanager
def forward_outbound_packets_to_nfqueue():
    rule = ('mangle', 'OUTPUT', '-p tcp --dport 80', '-j NFQUEUE')
    append_iptables_rule(*rule)
    try:
        yield
    finally:
        delete_iptables_rule(*rule)


def append_iptables_rule(table, chain, match, target):
    shell.call('iptables -t %s -A %s %s %s' % (table, chain, match, target))


def delete_iptables_rule(table, chain, match, target):
    shell.call('iptables -t %s -D %s %s %s' % (table, chain, match, target))


def monitor_nfqueue():
    q = nfqueue.queue()
    try:
        q.open()
        try:
            q.unbind(socket.AF_INET)
            q.bind(socket.AF_INET)
        except:
            LOGGER.error('Can not bind to nfqueue, are you running as ROOT?')
            sys.exit(1)
        q.set_callback(on_nfqueue_element)
        q.create_queue(0)
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
    handlers['=>'](nfqueue_element)


class DefaultOutboundHandler(object):
    def __init__(self, impersonator_ip, impersonator_port, my_internal_ip, my_external_ip):
        super(DefaultOutboundHandler, self).__init__()
        self.impersonator_ip = socket.inet_aton(impersonator_ip)
        self.impersonator_port = int(impersonator_port)
        self.my_internal_ip = socket.inet_aton(my_internal_ip)
        self.my_external_ip = socket.inet_aton(my_external_ip)

    def __call__(self, nfqueue_element):
        orig_packet = ip.IP(nfqueue_element.get_data())
        segment = udp.UDP(dport=self.impersonator_port, sport=0)
        segment.data = str(self.alter_src(orig_packet, self.my_external_ip))
        segment.ulen = len(segment)
        packet = ip.IP(src=self.my_internal_ip, dst=self.impersonator_ip, p=ip.IP_PROTO_UDP)
        packet.data = segment
        packet.len += len(packet.data)
        nfqueue_element.set_verdict_modified(nfqueue.NF_ACCEPT, str(packet), len(packet))

    def alter_src(self, packet, new_src):
        packet.src = new_src
        packet.sum = 0
        return packet

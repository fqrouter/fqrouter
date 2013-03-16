import os
import socket
import logging

import tornado.web
import tornado.template
import dpkt

import shutdown_hook


LOGGER = logging.getLogger(__name__)
ERROR_NO_DATA = 11

ROOT_DIR = os.path.dirname(__file__)
template_loader = tornado.template.Loader(ROOT_DIR)

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
shutdown_hook.add(raw_socket.close)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)

icmp_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_ICMP)
shutdown_hook.add(icmp_socket.close)
icmp_socket.settimeout(0)


class SelfCheckHandler(tornado.web.RequestHandler):
    def get(self):
        action = self.get_argument('action', default=None)
        if 'resolve-domain' == action:
            domain = self.get_argument('domain')
            ip = socket.gethostbyname(domain)
            self.write(ip)
            return
        if 'send-ping' == action:
            send_ping(self.get_argument('ip'))
            return
        if 'receive-pong' == action:
            while True:
                packet = try_receive_packet(icmp_socket)
                if not packet:
                    break
                LOGGER.debug('received: %s' % repr(packet))
                if not isinstance(packet.data, dpkt.icmp.ICMP):
                    continue
                if not isinstance(packet.data.data, dpkt.icmp.ICMP.Echo):
                    continue
                if 'foobar' != packet.data.data.data:
                    continue
                self.write('received')
                return
            self.write('not received')
            return
        self.get_template_path()
        template = template_loader.load('self-check.html')
        self.write(template.generate())


def send_ping(dst):
    def find_probe_src():
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect((dst, 80))
            return s.getsockname()[0]
        finally:
            s.close()

    icmp_packet = dpkt.icmp.ICMP(type=dpkt.icmp.ICMP_ECHO, data=dpkt.icmp.ICMP.Echo(id=1024, seq=1, data='foobar'))
    ip_packet = dpkt.ip.IP(src=socket.inet_aton(find_probe_src()), dst=socket.inet_aton(dst), p=dpkt.ip.IP_PROTO_ICMP)
    ip_packet.data = icmp_packet
    ip_packet.len += len(ip_packet.data)
    raw_socket.sendto(str(ip_packet), (dst, 0))


def try_receive_packet(s):
    try:
        packet = dpkt.ip.IP(s.recv(1024))
        return packet
    except socket.error as e:
        if ERROR_NO_DATA == e[0]:
            return None
        else:
            raise
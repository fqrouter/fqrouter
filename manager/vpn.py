import gevent.monkey

gevent.monkey.patch_all(ssl=False, thread=False)

import logging
import logging.handlers
import sys
import os
import _multiprocessing
import socket
import httplib
import fqdns
import fqsocks.fqsocks
import fqsocks.config_file
import fqsocks.gateways.proxy_client
import fqsocks.networking
import contextlib
import gevent
import gevent.socket
import dpkt
import config
import traceback
import urllib2
import fqsocks.httpd

FQROUTER_VERSION = 'UNKNOWN'
LOGGER = logging.getLogger('fqrouter.%s' % __name__)
LOG_DIR = '/data/data/fq.router2/log'
MANAGER_LOG_FILE = os.path.join(LOG_DIR, 'manager.log')
FQDNS_LOG_FILE = os.path.join(LOG_DIR, 'fqdns.log')

nat_map = {} # sport => (dst, dport), src always be 10.25.1.1
default_dns_server = config.get_default_dns_server()
DNS_HANDLER = fqdns.DnsHandler(
    enable_china_domain=True, enable_hosted_domain=True,
    original_upstream=('udp', default_dns_server, 53) if default_dns_server else None)


def handle_ping(environ, start_response):
    try:
        LOGGER.info('VPN PONG/%s' % FQROUTER_VERSION)
    except:
        traceback.print_exc()
        os._exit(1)
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield 'VPN PONG/%s' % FQROUTER_VERSION


fqsocks.httpd.HANDLERS[('GET', 'ping')] = handle_ping


def handle_exit(environ, start_response):
    gevent.spawn(exit_later)
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return ['EXITING']


fqsocks.httpd.HANDLERS[('POST', 'exit')] = handle_exit


def redirect_tun_traffic(tun_fd):
    while True:
        try:
            redirect_ip_packet(tun_fd)
        except:
            LOGGER.exception('failed to handle ip packet')


def redirect_ip_packet(tun_fd):
    gevent.socket.wait_read(tun_fd)
    try:
        ip_packet = dpkt.ip.IP(os.read(tun_fd, 8192))
    except OSError, e:
        LOGGER.error('read packet failed: %s' % e)
        gevent.sleep(3)
        return
    src = socket.inet_ntoa(ip_packet.src)
    dst = socket.inet_ntoa(ip_packet.dst)
    if hasattr(ip_packet, 'udp'):
        l4_packet = ip_packet.udp
    elif hasattr(ip_packet, 'tcp'):
        l4_packet = ip_packet.tcp
    else:
        return
    if src != '10.25.1.1':
        return
    if dst == '10.25.1.100':
        orig_dst_addr = nat_map.get(l4_packet.dport)
        if not orig_dst_addr:
            raise Exception('failed to get original destination')
        orig_dst, orig_dport = orig_dst_addr
        ip_packet.src = socket.inet_aton(orig_dst)
        ip_packet.dst = socket.inet_aton('10.25.1.1')
        ip_packet.sum = 0
        l4_packet.sport = orig_dport
        l4_packet.sum = 0
    else:
        nat_map[l4_packet.sport] = (dst, l4_packet.dport)
        ip_packet.src = socket.inet_aton('10.25.1.100')
        ip_packet.dst = socket.inet_aton('10.25.1.1')
        ip_packet.sum = 0
        l4_packet.dport = 12345
        l4_packet.sum = 0
    gevent.socket.wait_write(tun_fd)
    os.write(tun_fd, str(ip_packet))


def get_original_destination(sock, src_ip, src_port):
    if src_ip != '10.25.1.100': # fake connection from 10.25.1.100
        raise Exception('unexpected src ip: %s' % src_ip)
    return nat_map.get(src_port)


fqsocks.networking.SPI['get_original_destination'] = get_original_destination


def create_tcp_socket(server_ip, server_port, connect_timeout):
    fdsock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    with contextlib.closing(fdsock):
        fdsock.connect('\0fdsock2')
        fdsock.sendall('OPEN TCP,%s,%s,%s\n' % (server_ip, server_port, connect_timeout * 1000))
        gevent.socket.wait_read(fdsock.fileno())
        fd = _multiprocessing.recvfd(fdsock.fileno())
        if fd == 1:
            LOGGER.error('failed to create tcp socket: %s:%s' % (server_ip, server_port))
            raise socket.error('failed to create tcp socket: %s:%s' % (server_ip, server_port))
        sock = socket.fromfd(fd, socket.AF_INET, socket.SOCK_STREAM)
        os.close(fd)
        return sock


fqsocks.networking.SPI['create_tcp_socket'] = create_tcp_socket


def create_udp_socket():
    fdsock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    with contextlib.closing(fdsock):
        fdsock.connect('\0fdsock2')
        fdsock.sendall('OPEN UDP\n')
        gevent.socket.wait_read(fdsock.fileno())
        fd = _multiprocessing.recvfd(fdsock.fileno())
        if fd == 1:
            LOGGER.error('failed to create udp socket')
            raise socket.error('failed to create udp socket')
        sock = socket.fromfd(fd, socket.AF_INET, socket.SOCK_DGRAM)
        os.close(fd)
        return sock


fqdns.SPI['create_udp_socket'] = create_udp_socket
fqdns.SPI['create_tcp_socket'] = create_tcp_socket


def setup_logging():
    logging.basicConfig(stream=sys.stdout, level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
    handler = logging.handlers.RotatingFileHandler(
        MANAGER_LOG_FILE, maxBytes=1024 * 256, backupCount=0)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('fqrouter').addHandler(handler)
    handler = logging.handlers.RotatingFileHandler(
        FQDNS_LOG_FILE, maxBytes=1024 * 256, backupCount=0)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('fqdns').addHandler(handler)


def exit_later():
    gevent.sleep(0.5)
    os._exit(1)


def read_tun_fd_until_ready():
    LOGGER.info('connecting to fdsock')
    while True:
        tun_fd = read_tun_fd()
        if tun_fd:
            return tun_fd
        else:
            LOGGER.info('retry in 3 seconds')
            gevent.sleep(3)


def read_tun_fd():
    fdsock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    with contextlib.closing(fdsock):
        try:
            fdsock.connect('\0fdsock2')
            fdsock.sendall('TUN\n')
            gevent.socket.wait_read(fdsock.fileno(), timeout=3)
            tun_fd = _multiprocessing.recvfd(fdsock.fileno())
            if tun_fd == 1:
                LOGGER.error('received invalid tun fd')
                return None
            return tun_fd
        except:
            return None


class VpnUdpHandler(object):
    def __init__(self, dns_handler):
        self.dns_handler = dns_handler

    def __call__(self, sendto, request, address):
        try:
            src_ip, src_port = address
            dst_ip, dst_port = get_original_destination(None, src_ip, src_port)
            if 53 ==  get_original_destination(None, src_ip, src_port)[1]:
                self.dns_handler(sendto, request, address)
            else:
                sock = fqdns.create_udp_socket()
                try:
                    sock.sendto(request, (dst_ip, dst_port))
                    response = sock.recv(8192)
                    sendto(response, address)
                finally:
                    sock.close()
        except:
            LOGGER.exception('failed to handle udp')

fqsocks.fqsocks.DNS_HANDLER = VpnUdpHandler(DNS_HANDLER)

if '__main__' == __name__:
    setup_logging()
    LOGGER.info('environment: %s' % os.environ.items())
    LOGGER.info('default dns server: %s' % default_dns_server)
    FQROUTER_VERSION = os.getenv('FQROUTER_VERSION')
    try:
        gevent.monkey.patch_ssl()
    except:
        LOGGER.exception('failed to patch ssl')
    args = [
        '--log-level', 'INFO',
        '--log-file', '/data/data/fq.router2/log/fqsocks.log',
        '--tcp-gateway-listen', '10.25.1.1:12345',
        '--dns-server-listen', '10.25.1.1:12345',
        '--no-http-manager', # already started before
        '--no-tcp-scrambler', # no root permission
    ]
    args = config.configure_fqsocks(args)
    fqsocks.fqsocks.init_config(args)
    fqsocks.config_file.path = '/data/data/fq.router2/etc/fqsocks.json'
    http_manager_port = fqsocks.config_file.read_config()['http_manager']['port']
    try:
        response = urllib2.urlopen('http://127.0.0.1:%s/exit' % http_manager_port, '').read()
        if 'EXITING' == response:
            LOGGER.critical('!!! find previous instance, exiting !!!')
            gevent.sleep(3)
    except:
        LOGGER.exception('failed to exit previous')
    fqsocks.httpd.LISTEN_IP, fqsocks.httpd.LISTEN_PORT = '', http_manager_port
    fqsocks.httpd.server_greenlet = gevent.spawn(fqsocks.httpd.serve_forever)
    try:
        tun_fd = read_tun_fd_until_ready()
        LOGGER.info('tun fd: %s' % tun_fd)
    except:
        LOGGER.exception('failed to get tun fd')
        sys.exit(1)
    greenlet = gevent.spawn(redirect_tun_traffic, tun_fd)
    gevent.spawn(fqsocks.fqsocks.main)
    greenlet.join()

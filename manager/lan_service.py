import time
import subprocess
import logging
import re
import socket
import httplib
import threading
import binascii

import dpkt
from pynetfilter_conntrack.IPy import IP

import wifi


RE_MAC_ADDRESS = re.compile(r'[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+')
RE_DEFAULT_GATEWAY = re.compile(r'default via (\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})')
CONCURRENT_CHECKERS_COUNT = 20

LOGGER = logging.getLogger('fqrouter.%s' % __name__)

scan_results = []
picked_devices = {}


def run():
    thread = threading.Thread(target=send_loop)
    thread.daemon = True
    thread.start()


def send_loop():
    try:
        while True:
            if not picked_devices:
                time.sleep(3)
                continue
            if not send_for_five_minutes():
                LOGGER.info('too many retries, give up')
                break
    except:
        LOGGER.exception('failed to send forged default gateway')


def send_for_five_minutes():
    for i in range(3):
        try:
            s = socket.socket(socket.PF_PACKET, socket.SOCK_RAW)
            try:
                s.setblocking(0)
                s.bind((wifi.WIFI_INTERFACE, dpkt.ethernet.ETH_TYPE_ARP))
                my_ip, my_mac_address = get_my_ip_mac_address(wifi.WIFI_INTERFACE)
                default_gateway_ip = get_default_gateway(wifi.WIFI_INTERFACE)
                default_gateway_mac_address = arping(default_gateway_ip)
                for i in range(60):
                    time.sleep(5)
                    send_forged_default_gateway(s, my_mac_address, default_gateway_ip, default_gateway_mac_address)
            finally:
                s.close()
            return True
        except:
            LOGGER.exception('failed to send forged default gateway, retry in 10 seconds')
            time.sleep(10)
    return False


def send_forged_default_gateway(s, my_mac_address, default_gateway_ip, default_gateway_mac_address):
    for picked_ip, picked_mac_address in picked_devices.items():
        LOGGER.info('send forged default gateway %s to %s [%s]' % (default_gateway_ip, picked_ip, picked_mac_address))
        arp = dpkt.arp.ARP()
        arp.sha = eth_aton(my_mac_address)
        arp.spa = socket.inet_aton(default_gateway_ip)
        arp.tha = eth_aton(picked_mac_address)
        arp.tpa = socket.inet_aton(picked_ip)
        arp.op = dpkt.arp.ARP_OP_REPLY
        eth = dpkt.ethernet.Ethernet()
        eth.src = arp.sha
        eth.dst = eth_aton(picked_mac_address)
        eth.data = arp
        eth.type = dpkt.ethernet.ETH_TYPE_ARP
        s.send(str(eth))
        arp = dpkt.arp.ARP()
        arp.sha = eth_aton(my_mac_address)
        arp.spa = socket.inet_aton(picked_ip)
        arp.tha = eth_aton(default_gateway_mac_address)
        arp.tpa = socket.inet_aton(default_gateway_ip)
        arp.op = dpkt.arp.ARP_OP_REPLY
        eth = dpkt.ethernet.Ethernet()
        eth.src = arp.sha
        eth.dst = eth_aton(default_gateway_mac_address)
        eth.data = arp
        eth.type = dpkt.ethernet.ETH_TYPE_ARP
        s.send(str(eth))


def get_my_ip_mac_address(ifname):
    for line in wifi.shell_execute('netcfg').splitlines():
        if line.startswith(ifname):
            parts = [p for p in line.split(' ') if p]
            ip, mask = parts[2].split('/')
            return ip, parts[4]
    return None, None


def eth_aton(buffer):
    sp = buffer.split(':')
    buffer = ''.join(sp)
    return binascii.unhexlify(buffer)


def clean():
    pass


def handle_forge_default_gateway(environ, start_response):
    ip = environ['REQUEST_ARGUMENTS']['ip'].value
    mac_address = environ['REQUEST_ARGUMENTS']['mac_address'].value
    picked_devices[ip] = mac_address
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return []


def handle_restore_default_gateway(environ, start_response):
    ip = environ['REQUEST_ARGUMENTS']['ip'].value
    if ip in picked_devices:
        del picked_devices[ip]
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return []


def handle_scan(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    if scan_results:
        LOGGER.info('return cached scan results')
        for line in scan_results:
            ip, mac_address, host_name = line
            yield '%s,%s,%s,%s\n' % (ip, mac_address, host_name, 'TRUE' if ip in picked_devices else 'FALSE')
        return
    ip_range = get_ip_range(wifi.WIFI_INTERFACE)
    if not ip_range:
        LOGGER.warn('ip range not found')
        return
    if not ip_range.endswith('/24'): # home router only
        LOGGER.warn('ip range is too big: %s' % ip_range)
        return
    default_gateway = get_default_gateway(wifi.WIFI_INTERFACE)
    LOGGER.info('default gateway: %s' % default_gateway)
    ip_range = '%s/24' % '.'.join(ip_range.split('.')[:3] + ['0'])
    LOGGER.info('scan started: %s' % ip_range)
    for ip, mac_address, host_name in scan(ip_range):
        LOGGER.info('scan: %s,%s,%s' % (ip, mac_address, host_name))
        if default_gateway == ip:
            LOGGER.info('skip default gateway')
        else:
            scan_results.append((ip, mac_address, host_name))
            yield '%s,%s,%s,%s\n' % (ip, mac_address, host_name, 'TRUE' if ip in picked_devices else 'FALSE')


def handle_clear_scan_results(environ, start_response):
    global scan_results
    scan_results = []
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return []


def get_ip_range(ifname):
    for line in wifi.shell_execute('netcfg').splitlines():
        if line.startswith(ifname):
            parts = [p for p in line.split(' ') if p]
            return parts[2]
    return None


def get_default_gateway(ifname):
    for line in wifi.shell_execute('/data/data/fq.router/busybox ip route').splitlines():
        if 'dev %s' % ifname not in line:
            continue
        match = RE_DEFAULT_GATEWAY.search(line)
        if match:
            return match.group(1)
    return None


def scan(ip_range):
    unscanned_ip_addresses = list(IP(ip_range))
    checkers = []
    while len(unscanned_ip_addresses) + len(checkers):
        for checker in list(checkers):
            try:
                mac_address = checker.is_ok()
                if mac_address:
                    try:
                        mac_address = normalize_mac_address(mac_address)
                        host_name = resolve_host_name(checker.ip)
                        yield checker.ip, mac_address, host_name
                    except:
                        LOGGER.exception('post process ip failed: %s' % checker.ip)
                        yield checker.ip, '', ''
                    checkers.remove(checker)
                elif checker.is_failed():
                    yield checker.ip, '', ''
                    checkers.remove(checker)
                elif checker.is_timed_out():
                    yield checker.ip, '', ''
                    checkers.remove(checker)
                    checker.kill()
            except:
                yield checker.ip, '', ''
                LOGGER.exception('failed to finish checking: %s' % checker.ip)
                checkers.remove(checker)
        new_checkers_count = CONCURRENT_CHECKERS_COUNT - len(checkers)
        for i in range(new_checkers_count):
            if unscanned_ip_addresses:
                ip = unscanned_ip_addresses.pop()
                checkers.append(Checker(str(ip)))
        time.sleep(0.2)


def normalize_mac_address(mac_address):
    return ':'.join(['0%s' % p if 1 == len(p) else p for p in mac_address.split(':')])


def resolve_host_name(ip):
    try:
        return socket.gethostbyaddr(ip)
    except:
        return 'unknown'


def arping(ip):
    checker = Checker(ip)
    while True:
        mac_address = checker.is_ok()
        if mac_address:
            return normalize_mac_address(mac_address)
        elif checker.is_failed():
            raise Exception('failed to arping: %s' % ip)
        elif checker.is_timed_out():
            checker.kill()
            raise Exception('arping timed out: %s' % ip)
        time.sleep(0.2)


class Checker(object):
    def __init__(self, ip):
        self.ip = ip
        self.proc = subprocess.Popen(
            ['/data/data/fq.router/busybox', 'arping', '-I', wifi.WIFI_INTERFACE, '-f', '-w', '3', ip],
            stderr=subprocess.STDOUT, stdout=subprocess.PIPE)
        self.started_at = time.time()

    def is_ok(self):
        if 0 == self.proc.poll():
            match = RE_MAC_ADDRESS.search(self.proc.stdout.read())
            if match:
                return match.group(0)
        return False

    def is_failed(self):
        return self.proc.poll()

    def is_timed_out(self):
        return time.time() - self.started_at > 5

    def kill(self):
        try:
            self.proc.kill()
        except:
            LOGGER.exception('failed to kill checker for %s' % self.ip)
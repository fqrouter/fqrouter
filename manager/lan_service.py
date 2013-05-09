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
import shell

RE_DEFAULT_GATEWAY = re.compile(r'default via (\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})')
RE_IP_RANGE = re.compile(r'(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/\d+)')
CONCURRENT_CHECKERS_COUNT = 20

LOGGER = logging.getLogger('fqrouter.%s' % __name__)

scan_results = []
picked_devices = {}
previous_default_gateway = ''


def run():
    thread = threading.Thread(target=start_lan_service)
    thread.daemon = True
    thread.start()


def start_lan_service():
    try:
        wifi.enable_ipv4_forward()
        while True:
            if not picked_devices:
                time.sleep(3)
                continue
            if not send_for_ten_minutes():
                LOGGER.info('too many retries, give up')
                break
    except:
        LOGGER.exception('failed to send forged default gateway')


def send_for_ten_minutes():
    for i in range(3):
        try:
            s = socket.socket(socket.PF_PACKET, socket.SOCK_RAW)
            try:
                s.setblocking(0)
                s.bind((wifi.WIFI_INTERFACE, dpkt.ethernet.ETH_TYPE_ARP))
                my_ip, my_mac = wifi.get_ip_and_mac(wifi.WIFI_INTERFACE)
                default_gateway_ip = get_default_gateway(wifi.WIFI_INTERFACE)
                default_gateway_mac = arping(default_gateway_ip)
                for i in range(60):
                    send_forged_default_gateway(s, my_mac, default_gateway_ip, default_gateway_mac)
                    time.sleep(10)
            finally:
                s.close()
            return True
        except:
            LOGGER.exception('failed to send forged default gateway, retry in 10 seconds')
            time.sleep(10)
    return False


def send_forged_default_gateway(s, my_mac, default_gateway_ip, default_gateway_mac):
    for picked_ip, picked_mac in picked_devices.items():
        LOGGER.info('send forged default gateway %s to %s [%s]' % (default_gateway_ip, picked_ip, picked_mac))
        arp = dpkt.arp.ARP()
        arp.sha = eth_aton(my_mac)
        arp.spa = socket.inet_aton(default_gateway_ip)
        arp.tha = eth_aton(picked_mac)
        arp.tpa = socket.inet_aton(picked_ip)
        arp.op = dpkt.arp.ARP_OP_REPLY
        eth = dpkt.ethernet.Ethernet()
        eth.src = arp.sha
        eth.dst = eth_aton(picked_mac)
        eth.data = arp
        eth.type = dpkt.ethernet.ETH_TYPE_ARP
        s.send(str(eth))
        arp = dpkt.arp.ARP()
        arp.sha = eth_aton(my_mac)
        arp.spa = socket.inet_aton(picked_ip)
        arp.tha = eth_aton(default_gateway_mac)
        arp.tpa = socket.inet_aton(default_gateway_ip)
        arp.op = dpkt.arp.ARP_OP_REPLY
        eth = dpkt.ethernet.Ethernet()
        eth.src = arp.sha
        eth.dst = eth_aton(default_gateway_mac)
        eth.data = arp
        eth.type = dpkt.ethernet.ETH_TYPE_ARP
        s.send(str(eth))


def eth_aton(buffer):
    sp = buffer.split(':')
    buffer = ''.join(sp)
    return binascii.unhexlify(buffer)


def clean():
    pass


def handle_forge_default_gateway(environ, start_response):
    ip = environ['REQUEST_ARGUMENTS']['ip'].value
    mac = environ['REQUEST_ARGUMENTS']['mac'].value
    picked_devices[ip] = mac
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return []


def handle_restore_default_gateway(environ, start_response):
    ip = environ['REQUEST_ARGUMENTS']['ip'].value
    if ip in picked_devices:
        del picked_devices[ip]
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return [str(len(picked_devices))]


def handle_scan(environ, start_response):
    if scan_results:
        LOGGER.info('return cached scan results')
        start_response(httplib.OK, [('Content-Type', 'text/plain')])
        for line in scan_results:
            ip, mac, host_name = line
            yield '%s,%s,%s,%s\n' % (ip, mac, host_name, 'TRUE' if ip in picked_devices else 'FALSE')
        return
    try:
        ip_range = get_ip_range(wifi.WIFI_INTERFACE)
        if not ip_range:
            LOGGER.warn('ip range not found')
            return
        if not ip_range.endswith('/24'): # home router only
            LOGGER.warn('ip range is too big: %s' % ip_range)
            return
        default_gateway = get_default_gateway(wifi.WIFI_INTERFACE)
        LOGGER.info('default gateway: %s' % default_gateway)
        my_ip, _ = wifi.get_ip_and_mac(wifi.WIFI_INTERFACE)
        LOGGER.info('my ip: %s' % my_ip)
        LOGGER.info('scan started: %s' % ip_range)
    except:
        start_response(httplib.INTERNAL_SERVER_ERROR, [('Content-Type', 'text/plain')])
        LOGGER.exception('failed to prepare scan')
        return
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    try:
        for ip, mac, host_name in scan(ip_range):
            LOGGER.info('scan: %s,%s,%s' % (ip, mac, host_name))
            if default_gateway == ip:
                LOGGER.info('skip default gateway: %s' % default_gateway)
            elif my_ip == ip:
                LOGGER.info('skip my ip: %s' % my_ip)
            else:
                scan_results.append((ip, mac, host_name))
                yield '%s,%s,%s,%s\n' % (ip, mac, host_name, 'TRUE' if ip in picked_devices else 'FALSE')
    except:
        LOGGER.exception('failed to scan')
        return


def handle_clear_scan_results(environ, start_response):
    global scan_results
    scan_results = []
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return []


def get_ip_range(ifname):
    for line in shell.execute('ip route').splitlines():
        if 'dev %s' % ifname in line:
            match = RE_IP_RANGE.search(line)
            if match:
                return match.group(0)
    return None


def get_default_gateway(ifname):
    global previous_default_gateway
    for line in shell.execute('/data/data/fq.router/busybox ip route').splitlines():
        if 'dev %s' % ifname not in line:
            continue
        match = RE_DEFAULT_GATEWAY.search(line)
        if match:
            previous_default_gateway = match.group(1)
            return previous_default_gateway
    if previous_default_gateway:
        return previous_default_gateway
    else:
        raise Exception('failed to find default gateway: %s' % ifname)


def scan(ip_range):
    unscanned_ip_addresses = list(IP(ip_range))
    checkers = []
    while len(unscanned_ip_addresses) + len(checkers):
        for checker in list(checkers):
            try:
                mac = checker.is_ok()
                if mac:
                    try:
                        mac = normalize_mac(mac)
                        host_name = resolve_host_name(checker.ip)
                        yield checker.ip, mac, host_name
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


def normalize_mac(mac):
    return ':'.join(['0%s' % p if 1 == len(p) else p for p in mac.split(':')])


def resolve_host_name(ip):
    try:
        host_name = socket.gethostbyaddr(ip)[0]
        return '' if ip == host_name else host_name
    except:
        return ''


def arping(ip):
    LOGGER.info('arping: %s' % ip)
    checker = Checker(ip)
    while True:
        mac = checker.is_ok()
        if mac:
            return normalize_mac(mac)
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
            match = wifi.RE_MAC_ADDRESS.search(self.proc.stdout.read())
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
import time
import subprocess
import logging
import re
import socket
import httplib

from pynetfilter_conntrack.IPy import IP

import wifi


RE_MAC_ADDRESS = re.compile(r'[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+')
RE_DEFAULT_GATEWAY = re.compile(r'default via (\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})')
CONCURRENT_CHECKERS_COUNT = 20

LOGGER = logging.getLogger('fqrouter.%s' % __name__)


def handle_scan(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
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
            yield '%s,%s,%s\n' % (ip, mac_address, host_name)


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
#!/usr/bin/env python
import subprocess
import sys
import os
import argparse
import urllib2
import functools
import httplib
import signal
import atexit
from fqsocks import fqsocks
from fqsocks import http_connect
import time

import gevent
import gevent.pool
import gevent.monkey
import gevent.event


sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

argument_parser = argparse.ArgumentParser()
argument_parser.add_argument('--proxy-list', action='append')
argument_parser.add_argument('--proxy', action='append')
args = argument_parser.parse_args()

PROXY_LIST_DIR = os.path.join(os.path.dirname(__file__), 'proxy-list')
CONCURRENT_CHECKERS_COUNT = 8


def log(message):
    sys.stderr.write(message)
    sys.stderr.write('\n')


proxies = set()
black_list = {
    '198.154.114.118',
    '173.213.113.111',
    '66.35.68.145',
    '91.193.75.101',
    '5.135.81.16',
    '122.38.94.49',
    '95.140.119.40',
    '95.140.123.78',
    '109.195.54.231',
    '95.140.118.193',
    '178.33.105.59',
    '92.46.119.60',
    '201.73.200.82',
    '212.119.97.198',
    '211.138.120.125',
    '202.29.216.236',
    '120.194.22.114',
    '202.29.216.236',
    '120.194.22.114',
    '60.2.227.123'
}


def add_proxy(line):
    line = line.strip()
    if not line:
        return
    try:
        ip, port = line.split(':')
        if fqsocks.china_ip.is_china_ip(ip):
            log('skip china ip: %s' % ip)
        elif ip in black_list:
            log('skip blacklisted ip: %s' % ip)
        else:
            proxies.add((ip, port))
    except:
        log('skip illegal proxy: %s' % line)


if args.proxy:
    for proxy in args.proxy:
        add_proxy(proxy)
if args.proxy_list:
    for command in args.proxy_list:
        try:
            before = len(proxies)
            log('executing %s' % command)
            lines = subprocess.check_output(command, shell=True, cwd=PROXY_LIST_DIR).splitlines(False)
            for line in lines:
                add_proxy(line)
            after = len(proxies)
            log('succeeded, %s new proxies' % (after - before))
        except subprocess.CalledProcessError, e:
            log('failed, output:')
            log(e.output)


class BoundHTTPSHandler(urllib2.HTTPSHandler):
    def __init__(self, source_address=None, debuglevel=0):
        urllib2.HTTPSHandler.__init__(self, debuglevel)
        self.https_class = functools.partial(httplib.HTTPSConnection, source_address=source_address)

    def https_open(self, req):
        return self.do_open(self.https_class, req)


handler = BoundHTTPSHandler(source_address=('10.26.1.101', 0))
opener = urllib2.build_opener(handler)
fqsocks.OUTBOUND_IP = '10.1.2.3'
fqsocks.LISTEN_IP = '127.0.0.1'
fqsocks.LISTEN_PORT = 1101
fqsocks.CHINA_PROXY = None
# fqsocks.setup_logging('INFO')
good_proxies = {}
done = gevent.event.Event()


class CheckingHttpConnectProxy(http_connect.HttpConnectProxy):
    def __init__(self, *args, **kwargs):
        super(CheckingHttpConnectProxy, self).__init__(*args, **kwargs)
        self.successes = []

    def do_forward(self, client):
        before = time.time()
        try:
            super(CheckingHttpConnectProxy, self).do_forward(client)
            after = time.time()
            elapsed = after - before
            if elapsed > 10:
                raise Exception('taking too long')
            self.successes.append(elapsed)
            log('OK[%s] %s %s:%s' % (len(self.successes), elapsed, self.proxy_ip, self.proxy_port))
            if len(self.successes) >= 10:
                average = float(sum(self.successes)) / len(self.successes)
                good_proxies[(self.proxy_ip, self.proxy_port)] = average
                self.died = True
        except:
            self.died = True
            log('FAILED %s:%s' % (self.proxy_ip, self.proxy_port))
            raise


fqsocks.mandatory_proxies = [CheckingHttpConnectProxy(ip, port) for ip, port in proxies]


def check_twitter_access():
    try:
        opener.open('https://twitter.com/').read()
    except:
        pass


def keep_fqsocks_busy():
    while True:
        pool = gevent.pool.Pool(size=16)
        greenlets = []
        for i in range(100):
            greenlets.append(pool.apply_async(check_twitter_access))
        while len(pool) > 0:
            for greenlet in list(pool):
                try:
                    greenlet.join(timeout=10)
                except:
                    pass
        try:
            pool.kill()
        except:
            pass


def check_if_all_died():
    while True:
        gevent.sleep(1)
        if all(p.died for p in fqsocks.mandatory_proxies):
            done.set()


def setup():
    subprocess.check_call('ifconfig lo:proxy 10.26.1.101 netmask 255.255.255.255', shell=True)
    subprocess.check_call('iptables -t nat -I OUTPUT -s 10.26.1.101 -p tcp -j REDIRECT --to-port 1101', shell=True)


def teardown():
    subprocess.check_call('iptables -t nat -D OUTPUT -s 10.26.1.101 -p tcp -j REDIRECT --to-port 1101', shell=True)


def main():
    signal.signal(signal.SIGTERM, lambda signum, fame: teardown())
    signal.signal(signal.SIGINT, lambda signum, fame: teardown())
    atexit.register(teardown)
    setup()
    gevent.monkey.patch_all(thread=False)
    gevent.spawn(fqsocks.start_server)
    gevent.spawn(keep_fqsocks_busy)
    gevent.spawn(check_if_all_died)
    done.wait()
    for ip_port, average in sorted(good_proxies.items(), key=lambda e: e[1])[:20]:
        ip, port = ip_port
        log('picked %s:%s %s' % (ip, port, average))
        print('%s:%s' % (ip, port))
    print('')


if '__main__' == __name__:
    main()
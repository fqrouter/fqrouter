#!/usr/bin/env python
import sys
import os
import random

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
import urllib2
import functools
import httplib
import gevent
import gevent.monkey
import gevent.pool
import gevent.event
import subprocess
import signal
import atexit
from fqsocks import fqsocks
from fqsocks import goagent

T1_APP_IDS = ['freegoagent%03d' % i for i in range(1, 1000)]
T2_APP_IDS = ['fgabootstrap001', 'fgabootstrap002', 'fgabootstrap003', 'fgabootstrap004',
             'fgabootstrap005', 'fgabootstrap006', 'fgabootstrap007', 'fgabootstrap008', 'fgabootstrap009',
             'fgabootstrap010', 'fgaupdate001', 'fgaupdate002', 'fgaupdate003', 'fgaupdate004', 'fgaupdate005',
             'fgaupdate006', 'fgaupdate007', 'fgaupdate008', 'fgaupdate009', 'fgaupdate010', 'fganr001', 'fganr002',
             'fanyueproxy1-01', 'fanyueproxy1-02', 'fanyueproxy1-03', 'fanyueproxy1-04', 'fanyueproxy1-05'] + [
              'vi88com1', 'vi88com10', 'vi88com 11', 'vi88com2', 'vi88com12', 'vi88com3', 'vi88com13', 'vi88com4',
              'vi88com14', 'vi88com5', 'vi88com15', 'vi88com6', 'vi88com16', 'vi88com7', 'vi88com17', 'vi88com8',
              'vi88com18', 'vi88com19', 'vip6xlgonggongid01', 'gongongid02', 'gonggongid03', 'gonggongid04',
              'gonggongid05', 'gonggongid06', 'gonggongid07', 'gonggongid08', 'gonggongid09', 'gonggongid10',
              'goagent-dup001', 'goagent-dup002', 'goagent-dup003', 'gonggongid11', 'gonggongid12', 'gonggongid13',
              'gonggongid14', 'gonggongid15', 'gonggongid16', 'gonggongid17', 'gonggongid18', 'gonggongid19',
              'gonggongid20', 'gfwsbgfwsbgfwsb', '1.sbgfwsbgfwsbgfw', '1.wyq476137265', '1.wangyuqi19961213',
              'xinxijishuwyq21', 'xinxijishuwyq22', 'xinxijishuwyq23', 'xinxijishuwyq24', 'xinxijishuwyq25',
              'wuxinchengboy', 'wuxinchengforever', 'wuxinchenghappy','wuxinchengjava', 'wuxinchengjoy',
              'wuxinchenglad','wuxinchenglove','wuxinchengood', 'wuxinchengsuccessful','wuxinchengsunshine']

if len(sys.argv) > 1:
    TI_APP_IDS = [sys.argv[1]]
    T2_APP_IDS = []

random.shuffle(T1_APP_IDS)
good_app_ids = set()
done = gevent.event.Event()


class BoundHTTPHandler(urllib2.HTTPHandler):
    def __init__(self, source_address=None, debuglevel=0):
        urllib2.HTTPHandler.__init__(self, debuglevel)
        self.http_class = functools.partial(httplib.HTTPConnection, source_address=source_address)

    def http_open(self, req):
        return self.do_open(self.http_class, req)


handler = BoundHTTPHandler(source_address=('10.26.1.100', 0))
opener = urllib2.build_opener(handler)
fqsocks.OUTBOUND_IP = '10.1.2.3'
fqsocks.LISTEN_IP = '127.0.0.1'
fqsocks.LISTEN_PORT = 1100
fqsocks.CHINA_PROXY = None


class CheckingGoAgentProxy(fqsocks.GoAgentProxy):
    def forward(self, client):
        try:
            super(CheckingGoAgentProxy, self).forward(client)
            sys.stderr.write('found: ')
            sys.stderr.write(self.appid)
            sys.stderr.write('\n')
            if self.appid not in good_app_ids:
                good_app_ids.add(self.appid)
                print(self.appid)
                if len(good_app_ids) >= 10:
                    done.set()
            self.died = True
        except fqsocks.ProxyFallBack as e:
            self.died = True
            raise
        except:
            self.died = True
            raise


for appid in T1_APP_IDS:
    fqsocks.mandatory_proxies.append(CheckingGoAgentProxy(appid))


def check_baidu_access():
    try:
        opener.open('http://www.baidu.com').read()
    except:
        pass


def keep_fqsocks_busy():
    goagent.GoAgentProxy.GOOGLE_HOSTS = ['goagent-google-ip.fqrouter.com']
    goagent.GoAgentProxy.refresh(fqsocks.mandatory_proxies, fqsocks.create_udp_socket, fqsocks.create_tcp_socket)
    while True:
        pool = gevent.pool.Pool(size=16)
        greenlets = []
        for i in range(100):
            greenlets.append(pool.apply_async(check_baidu_access))
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
        not_died_count = len([p for p in fqsocks.mandatory_proxies if not p.died])
        sys.stderr.write('not died count: ')
        sys.stderr.write(str(not_died_count))
        sys.stderr.write('\n')
        if not not_died_count:
            done.set()


def setup():
    subprocess.check_call('ifconfig lo:goagent 10.26.1.100 netmask 255.255.255.255', shell=True)
    subprocess.check_call('ifconfig lo:1 10.1.2.3 netmask 255.255.255.255', shell=True)
    subprocess.check_call('iptables -t nat -I OUTPUT -s 10.26.1.100 -p tcp -j REDIRECT --to-port 1100', shell=True)
    subprocess.check_call('iptables -t nat -I POSTROUTING -s 10.1.2.3 -j MASQUERADE', shell=True)


def teardown():
    subprocess.check_call('iptables -t nat -D OUTPUT -s 10.26.1.100 -p tcp -j REDIRECT --to-port 1100', shell=True)
    subprocess.check_call('iptables -t nat -D POSTROUTING -s 10.1.2.3 -j MASQUERADE', shell=True)


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
    if len(good_app_ids) >= 10:
        print('')
    else:
        for appid in T2_APP_IDS:
            fqsocks.mandatory_proxies.append(CheckingGoAgentProxy(appid))
        done.clear()
        done.wait()
        print('')


if '__main__' == __name__:
    main()

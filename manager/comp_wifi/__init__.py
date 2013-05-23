import httplib

import gevent

from wifi import WIFI_INTERFACE
from wifi import get_working_hotspot_iface
from wifi import setup_lo_alias
from wifi import start_hotspot
from wifi import stop_hotspot
from wifi import setup_networking
from wifi import get_ip_and_mac
from utils import config


def start():
    setup_lo_alias()
    return [
        ('POST', 'wifi/start', handle_start),
        ('POST', 'wifi/stop', handle_stop),
        ('GET', 'wifi/started', handle_started),
        ('POST', 'wifi/setup', handle_setup)
    ]


def stop():
    pass


def is_alive():
    return get_working_hotspot_iface()


def handle_start(environ, start_response):
    cfg = config.read()
    ssid = cfg.get('fqrouter', 'WifiHotspotSSID')
    password = cfg.get('fqrouter', 'WifiHotspotPassword')
    success, message = start_hotspot(ssid, password)
    status = httplib.OK if success else httplib.BAD_GATEWAY
    start_response(status, [('Content-Type', 'text/plain')])
    yield message


def handle_stop(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield stop_hotspot()


def handle_setup(environ, start_response):
    for i in range(10):
        iface = get_working_hotspot_iface()
        if not iface:
            gevent.sleep(2)
            continue
        ip, _ = get_ip_and_mac(iface)
        if '10.24.1.1' != ip:
            setup_networking(iface)
        start_response(httplib.OK, [('Content-Type', 'text/plain')])
        return []


def handle_started(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield 'TRUE' if get_working_hotspot_iface() else 'FALSE'

import httplib

from wifi import WIFI_INTERFACE
from wifi import get_working_hotspot_iface
from wifi import setup_lo_alias
from wifi import start_hotspot
from wifi import stop_hotspot
from wifi import setup_networking
from wifi import enable_wifi_p2p_service
from wifi import restore_config_files
from wifi import get_ip_and_mac
from wifi import netd_execute
from utils import config


def start():
    setup_lo_alias()
    return [
        ('POST', 'wifi-repeater/start', handle_start),
        ('POST', 'wifi-repeater/stop', handle_stop),
        ('POST', 'wifi-repeater/reset', handle_reset),
        ('GET', 'wifi-repeater/is-started', handle_is_started),
    ]


def stop():
    pass


def is_alive():
    return get_working_hotspot_iface()


def handle_start(environ, start_response):
    cfg = config.read()
    ssid = str(cfg['wifi_hotspot_ssid'])
    password = str(cfg['wifi_hotspot_password'])
    success, message = start_hotspot(ssid, password)
    status = httplib.OK if success else httplib.BAD_GATEWAY
    start_response(status, [('Content-Type', 'text/plain')])
    yield message


def handle_stop(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield stop_hotspot()


def handle_reset(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    enable_wifi_p2p_service()
    restore_config_files()
    stop_hotspot()
    return []


def handle_is_started(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield 'TRUE' if get_working_hotspot_iface() else 'FALSE'

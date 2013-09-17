import httplib
import fqsocks.httpd
import logging

import config
import wifi


LOGGER = logging.getLogger('fqrouter.%s' % __name__)


def handle_start(environ, start_response):
    cfg = config.read()
    ssid = str(cfg['wifi_hotspot_ssid'])
    password = str(cfg['wifi_hotspot_password'])
    success, message = wifi.start_hotspot(ssid, password)
    status = httplib.OK if success else httplib.BAD_GATEWAY
    start_response(status, [('Content-Type', 'text/plain')])
    yield message


fqsocks.httpd.HANDLERS[('POST', 'wifi-repeater/start')] = handle_start


def handle_stop(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield wifi.stop_hotspot()


fqsocks.httpd.HANDLERS[('POST', 'wifi-repeater/stop')] = handle_stop


def handle_reset(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    wifi.enable_wifi_p2p_service()
    wifi.restore_config_files()
    wifi.stop_hotspot()
    return []


fqsocks.httpd.HANDLERS[('POST', 'wifi-repeater/reset')] = handle_reset


def handle_is_started(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield 'TRUE' if wifi.get_working_hotspot_iface() else 'FALSE'


fqsocks.httpd.HANDLERS[('GET', 'wifi-repeater/is-started')] = handle_is_started

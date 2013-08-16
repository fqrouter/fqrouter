import gevent.monkey

gevent.monkey.patch_all(ssl=False)

import os
import logging
import logging.handlers
import sys
import httplib

from utils import shutdown_hook
from utils import config
from utils import httpd
from utils import shell

import comp_wifi
import comp_dns
import comp_scrambler
import comp_proxy
import comp_shortcut
import comp_lan
import subprocess
import shlex
import urllib2
import traceback

FQROUTER_VERSION = 'UNKNOWN'

ROOT_DIR = os.path.dirname(__file__)
LOG_DIR = '/data/data/fq.router2/log'
MANAGER_LOG_FILE = os.path.join(LOG_DIR, 'manager.log')
WIFI_LOG_FILE = os.path.join(LOG_DIR, 'wifi.log')

LOGGER = logging.getLogger('fqrouter.%s' % __name__)
ALL_COMPONENTS = [comp_wifi, comp_dns, comp_scrambler, comp_proxy, comp_lan, comp_shortcut]


def handle_ping(environ, start_response):
    try:
        LOGGER.info('PONG/%s' % FQROUTER_VERSION)
    except:
        traceback.print_exc()
        sys.exit(1)
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield 'PONG/%s' % FQROUTER_VERSION


def handle_free_internet_connect(environ, start_response):
    components = [comp_dns, comp_scrambler, comp_proxy, comp_shortcut]
    if not config.read().get('comp_dns_enabled', True):
        LOGGER.info('dns component disabled by config')
        components.remove(comp_dns)
    if not config.read().get('comp_proxy_enabled', True):
        LOGGER.info('proxy component disabled by config')
        components.remove(comp_proxy)
    if not config.read().get('tcp_scrambler_enabled', True):
        LOGGER.info('scrambler component disabled by config')
        components.remove(comp_scrambler)
    if not config.read().get('china_shortcut_enabled', True):
        LOGGER.info('shortcut component disabled by config china_short_enabled')
        components.remove(comp_shortcut)
    if not config.read().get('direct_access_enabled', True):
        LOGGER.info('shortcut component disabled by config direct_access_enabled')
        if comp_shortcut in components:
            components.remove(comp_shortcut)
    start_components(*components)
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return []


def handle_free_internet_disconnect(environ, start_response):
    stop_components(comp_dns, comp_scrambler, comp_proxy, comp_shortcut)
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return []


def handle_free_internet_is_connected(environ, start_response):
    is_connected = is_free_internet_connected()
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield 'TRUE' if is_connected else 'FALSE'


def is_free_internet_connected():
    return comp_dns.is_alive() and comp_proxy.is_alive()


def run():
    if config.read().get('comp_wifi_enabled', True):
        start_components(comp_wifi, comp_lan)
    else:
        LOGGER.info('wifi component disabled by config')
        comp_wifi.setup_lo_alias()
        start_components(comp_lan)
    httpd.HANDLERS[('GET', 'ping')] = handle_ping
    httpd.HANDLERS[('POST', 'free-internet/connect')] = handle_free_internet_connect
    httpd.HANDLERS[('POST', 'free-internet/disconnect')] = handle_free_internet_disconnect
    httpd.HANDLERS[('GET', 'free-internet/is-connected')] = handle_free_internet_is_connected
    gevent.spawn(check_ping)
    httpd.serve_forever()


def check_ping():
    gevent.sleep(1)
    try:
        if 'PONG/%s' % FQROUTER_VERSION == urllib2.urlopen('http://127.0.0.1:8318/ping').read():
            LOGGER.info('check ping succeed')
        else:
            raise Exception('ping does not respond correctly')
    except:
        LOGGER.exception('check ping failed')
        sys.exit(1)


def start_components(*components):
    for comp in components:
        try:
            shutdown_hook.add(comp.stop)
            handlers = comp.start()
            for method, url, handler in handlers or []:
                httpd.HANDLERS[(method, url)] = handler
            LOGGER.info('started component: %s' % comp.__name__)
        except:
            LOGGER.exception('failed to start component: %s' % comp.__name__)
            comp.stop()
            if getattr(comp, '__MANDATORY__', False):
                raise
            LOGGER.info('skipped component: %s' % comp.__name__)


def stop_components(*components):
    for comp in reversed(components):
        try:
            comp.stop()
        except:
            LOGGER.exception('failed to stop: %s' % comp.__name__)


def clean():
    LOGGER.info('clean...')
    try:
        stop_components(*ALL_COMPONENTS)
        try:
            LOGGER.info('iptables -L -v -n')
            LOGGER.info(shell.check_output(shlex.split('iptables -L -v -n')))
        except subprocess.CalledProcessError, e:
            LOGGER.error('failed to dump filter table: %s' % (sys.exc_info()[1]))
            LOGGER.error(e.output)
        try:
            LOGGER.info('iptables -t nat -L -v -n')
            LOGGER.info(shell.check_output(shlex.split('iptables -t nat -L -v -n')))
        except subprocess.CalledProcessError, e:
            LOGGER.error('failed to dump nat table: %s' % (sys.exc_info()[1]))
            LOGGER.error(e.output)
    except:
        LOGGER.exception('clean failed')


def setup_logging():
    logging.basicConfig(stream=sys.stdout, level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
    handler = logging.handlers.RotatingFileHandler(
        MANAGER_LOG_FILE, maxBytes=1024 * 256, backupCount=0)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('fqrouter').addHandler(handler)
    handler = logging.handlers.RotatingFileHandler(
        WIFI_LOG_FILE, maxBytes=1024 * 512, backupCount=1)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('wifi').addHandler(handler)


def needs_su():
    if os.getuid() == 0:
        return False
    else:
        return True


if '__main__' == __name__:
    setup_logging()
    LOGGER.info('environment: %s' % os.environ.items())
    FQROUTER_VERSION = os.getenv('FQROUTER_VERSION')
    try:
        gevent.monkey.patch_ssl()
    except:
        LOGGER.exception('failed to patch ssl')
    action = sys.argv[1]
    if 'clean' == action:
        shell.USE_SU = needs_su()
        clean()
    elif 'run' == action:
        shell.USE_SU = needs_su()
        run()
    elif 'netd-execute' == action:
        comp_wifi.netd_execute(sys.argv[2])
    else:
        raise Exception('unknown action: %s' % action)

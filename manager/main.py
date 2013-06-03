import os
import logging
import logging.handlers
import sys
import httplib

import gevent.monkey

from utils import shutdown_hook
from utils import config
from utils import httpd

import comp_wifi
import comp_dns
import comp_scrambler
import comp_proxy
import comp_shortcut
import comp_lan
import comp_version


ROOT_DIR = os.path.dirname(__file__)
LOG_DIR = '/data/data/fq.router'
MANAGER_LOG_FILE = os.path.join(LOG_DIR, 'manager.log')
WIFI_LOG_FILE = os.path.join(LOG_DIR, 'wifi.log')

LOGGER = logging.getLogger('fqrouter.%s' % __name__)
COMPONENTS = [comp_wifi, comp_dns, comp_scrambler, comp_proxy, comp_lan, comp_shortcut, comp_version]


def handle_ping(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    for comp in COMPONENTS:
        if not comp.is_alive() and getattr(comp, '__MANDATORY__', False):
            LOGGER.error('%s COMPONENT DIED' % comp.__name__)
            yield '%s COMPONENT DIED' % comp.__name__
    LOGGER.info('PONG')
    yield 'PONG'


def run():
    skipped_components = []
    LOGGER.info('environment: %s' % os.environ.items())
    if not config.read().getboolean('fqrouter', 'IsScramblerEnabled'):
        LOGGER.info('scrambler component disabled by config')
        COMPONENTS.remove(comp_scrambler)
    for comp in COMPONENTS:
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
            skipped_components.append(comp.__name__)
    LOGGER.info('all components started except: %s' % skipped_components)
    httpd.HANDLERS[('GET', 'ping')] = handle_ping
    httpd.serve_forever()


def clean():
    LOGGER.info('clean...')
    for comp in reversed(COMPONENTS):
        try:
            comp.stop()
        except:
            LOGGER.exception('failed to clean: %s' % comp.__name__)


def setup_logging():
    logging.basicConfig(stream=sys.stdout, level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
    handler = logging.handlers.RotatingFileHandler(
        MANAGER_LOG_FILE, maxBytes=1024 * 256, backupCount=0)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('fqrouter').addHandler(handler)
    handler = logging.handlers.RotatingFileHandler(
        WIFI_LOG_FILE, maxBytes=1024 * 512, backupCount=0)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('wifi').addHandler(handler)


if '__main__' == __name__:
    gevent.monkey.patch_all()
    setup_logging()
    if len(sys.argv) > 1:
        action = sys.argv[1]
        if 'clean' == action:
            clean()
        else:
            raise Exception('unknown action: %s' % action)
    else:
        run()

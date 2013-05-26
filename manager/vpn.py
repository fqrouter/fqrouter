import logging
import logging.handlers
import sys
import os
import _multiprocessing
import socket
import httplib

import gevent

import gevent.monkey

import gevent.socket

from utils import httpd


fdsock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)

LOGGER = logging.getLogger('fqrouter.%s' % __name__)
LOG_DIR = '/data/data/fq.router'
MANAGER_LOG_FILE = os.path.join(LOG_DIR, 'manager.log')


def redirect_tun_traffic():
    LOGGER.info('environment: %s' % os.environ.items())
    try:
        LOGGER.info('connecting to fdsock')
        while True:
            try:
                fdsock.connect('\0fdsock')
                break
            except:
                LOGGER.info('retry in 5 seconds')
                gevent.sleep(5)
        LOGGER.info('connected to fdsock')
        gevent.socket.wait_read(fdsock.fileno())
        tun_fd = _multiprocessing.recvfd(fdsock.fileno())
        LOGGER.info('tun fd: %s' % tun_fd)
    except:
        LOGGER.exception('failed to get tun fd')


def setup_logging():
    logging.basicConfig(stream=sys.stdout, level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
    handler = logging.handlers.RotatingFileHandler(
        MANAGER_LOG_FILE, maxBytes=1024 * 256, backupCount=0)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('fqrouter').addHandler(handler)


def handle_ping(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    LOGGER.info('VPN PONG')
    yield 'VPN PONG'


if '__main__' == __name__:
    gevent.monkey.patch_all()
    setup_logging()
    httpd.HANDLERS[('GET', 'ping')] = handle_ping
    greenlets = [
        gevent.spawn(redirect_tun_traffic),
        gevent.spawn(httpd.serve_forever)
    ]
    for greenlet in greenlets:
        greenlet.join()
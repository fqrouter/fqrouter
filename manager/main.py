import os
import logging
import logging.handlers
import lan

ROOT_DIR = os.path.dirname(__file__)
LOG_DIR = '/data/data/fq.router'
LOG_FILE = os.path.join(LOG_DIR, 'manager.log')


def setup_logging():
    logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
    handler = logging.handlers.RotatingFileHandler(
        LOG_FILE, maxBytes=1024 * 1024, backupCount=1)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('fqrouter').addHandler(handler)


if '__main__' == __name__:
    setup_logging()

LOGGER = logging.getLogger('fqrouter.%s' % __name__)

import httplib
import wifi
import version
import cgi
import wsgiref.simple_server
from SocketServer import ThreadingMixIn
import dns_service
import tcp_service
import full_proxy_service
import sys


def handle_ping(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield 'PONG'


def handle_logs(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/html')])
    yield '<html><body><pre>'
    lines = []
    with open(LOG_FILE) as f:
        lines.append(f.read())
    for line in reversed(lines):
        yield line
    yield '</pre></body></html>'


HANDLERS = {
    ('GET', 'ping'): handle_ping,
    ('GET', 'logs'): handle_logs,
    ('POST', 'wifi/start'): wifi.handle_start,
    ('POST', 'wifi/stop'): wifi.handle_stop,
    ('GET', 'wifi/started'): wifi.handle_started,
    ('POST', 'wifi/setup'): wifi.handle_setup,
    ('GET', 'lan/scan'): lan.handle_scan,
    ('POST', 'lan/clear-scan-results'): lan.handle_clear_scan_results,
    ('GET', 'version/latest'): version.handle_latest
}


def handle_request(environ, start_response):
    method = environ.get('REQUEST_METHOD')
    path = environ.get('PATH_INFO', '').strip('/')
    environ['REQUEST_ARGUMENTS'] = cgi.FieldStorage(
        fp=environ['wsgi.input'],
        environ=environ,
        keep_blank_values=True)
    handler = HANDLERS.get((method, path))
    if handler:
        try:
            lines = handler(environ, lambda status, headers: start_response(get_http_response(status), headers))
        except:
            LOGGER.exception('failed to handle request: %s %s' % (method, path))
            raise
    else:
        start_response(get_http_response(httplib.NOT_FOUND), [('Content-Type', 'text/plain')])
        lines = []
    for line in lines:
        yield line


def get_http_response(code):
    return '%s %s' % (code, httplib.responses[code])


class MultiThreadedWSGIServer(ThreadingMixIn, wsgiref.simple_server.WSGIServer):
    pass


if '__main__' == __name__:
    LOGGER.info('environment: %s' % os.environ.items())
    dns_service.run()
    tcp_service.run()
    full_proxy_service.run()
    wifi.setup_lo_alias()
    LOGGER.info('services started')
    try:
        httpd = wsgiref.simple_server.make_server(
            '127.0.0.1', 8318, handle_request,
            server_class=MultiThreadedWSGIServer)
        LOGGER.info('serving HTTP on port 8318...')
    except:
        LOGGER.exception('failed to start HTTP server on port 8318')
        sys.exit(1)
    httpd.serve_forever()

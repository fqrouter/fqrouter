import os
import logging
import logging.handlers
import sys
from SocketServer import ThreadingMixIn


ROOT_DIR = os.path.dirname(__file__)
LOG_DIR = '/data/data/fq.router'
LOG_FILE = os.path.join(LOG_DIR, 'manager.log')


def setup_logging(log_file=LOG_FILE, maxBytes=1024 * 512):
    logging.basicConfig(stream=sys.stdout, level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
    handler = logging.handlers.RotatingFileHandler(
        log_file, maxBytes=maxBytes, backupCount=0)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('fqrouter').addHandler(handler)


LOGGER = logging.getLogger('fqrouter.%s' % __name__)

import httplib
import wifi
import version
import cgi
import wsgiref.simple_server
import dns_service
import proxy_service
import scrambler_service
import shutdown_hook
import lan_service


def handle_ping(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    if not dns_service.is_alive():
        yield 'DNS SERVICE DIED'
    elif not proxy_service.is_alive():
        yield 'PROXY SERVICE DIED'
    else:
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
    ('GET', 'lan/scan'): lan_service.handle_scan,
    ('POST', 'lan/forge-default-gateway'): lan_service.handle_forge_default_gateway,
    ('POST', 'lan/restore-default-gateway'): lan_service.handle_restore_default_gateway,
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

# TODO: redesign shutdown hook
# TODO: put wifi operation into process
class MultiThreadedWSGIServer(ThreadingMixIn, wsgiref.simple_server.WSGIServer):
    pass


def run():
    setup_logging(LOG_FILE)
    LOGGER.info('environment: %s' % os.environ.items())
    wifi.setup_lo_alias()
    dns_service.run()
    scrambler_service.run()
    proxy_service.run()
    lan_service.run()
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


def clean():
    setup_logging(LOG_FILE)
    LOGGER.info('clean...')
    dns_service.clean()
    scrambler_service.clean()
    proxy_service.clean()
    lan_service.clean()


if '__main__' == __name__:
    if len(sys.argv) > 1:
        shutdown_hook.shutdown_hooks = []
        action = sys.argv[1]
        if 'wifi-start-hotspot' == action:
            setup_logging(os.path.join(LOG_DIR, 'wifi.log'), maxBytes=1024 * 512)
            sys.stderr.write(repr(wifi.start_hotspot(*sys.argv[2:])))
        elif 'wifi-stop-hotspot' == action:
            setup_logging(os.path.join(LOG_DIR, 'wifi.log'), maxBytes=1024 * 512)
            sys.stderr.write(repr(wifi.stop_hotspot()))
        elif 'clean' == action:
            clean()
    else:
        run()

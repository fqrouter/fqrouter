import gevent.monkey

gevent.monkey.patch_all()

from gevent.wsgi import WSGIServer
import gevent
import logging
import httplib
import cgi
import os
import signal
import urllib2

LOGGER = logging.getLogger(__name__)
RAW_URL = 'http://down.iscka.com/jx/360sQUYB8tmjPaqdm.apk'
HANDLERS = {}

real_link = None


def handle_request(environ, start_response):
    method = environ.get('REQUEST_METHOD')
    path = environ.get('PATH_INFO', '').strip('/')
    field_storage = cgi.FieldStorage(
        fp=environ['wsgi.input'],
        environ=environ,
        keep_blank_values=True)
    environ['REQUEST_ARGUMENTS'] = {key: field_storage[key].value for key in field_storage.keys()}
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


def http_handler(method, url):
    def decorator(func):
        HANDLERS[(method, url)] = func
        return func

    return decorator


@http_handler('GET', '')
def home_page(environ, start_response):
    start_response(httplib.TEMPORARY_REDIRECT, [('Location', real_link)])
    return []


def main():
    logging.basicConfig(level=logging.DEBUG)
    signal.signal(signal.SIGINT, lambda signum, fame: os._exit(0))

    try:
        server = WSGIServer(('', 8090), handle_request)
        LOGGER.info('serving HTTP on port 8090...')
    except:
        LOGGER.exception('failed to start HTTP server on port 8090')
        os._exit(1)
    gevent.spawn(refresh_real_link)
    server.serve_forever()


def refresh_real_link():
    while True:
        try:
            update_real_link()
        except:
            LOGGER.exception('failed to update link')
        gevent.sleep(60 * 60)


def update_real_link():
    global real_link
    html = urllib2.urlopen(RAW_URL).read()
    real_link = html.rpartition('url=')[2][:-2]
    LOGGER.info('updated link: %s' % real_link)


if '__main__' == __name__:
    main()
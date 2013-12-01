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
import urllib

LOGGER = logging.getLogger(__name__)
PAN_WEIYUN_URL = 'https://duyaoblog.duapp.com/download.php?type=w&fid=e7ddc2708c20dc20e0c7a47d53d2f722&uid='
PAN_360_URL = 'http://down.iscka.com/jx/360sQUYB8tmjPaqdm.apk'
FALLBACK_URL = 'http://69.163.40.146:8080/fqrouter-2.8.6.apk'
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
    global real_link
    while True:
        try:
            if real_link:
                verify_real_link(real_link)
                gevent.sleep(60)
                continue
        except:
            LOGGER.exception('real link expired')
        try:
            real_link = update_from_pan_360()
        except:
            LOGGER.exception('failed to update from pan 360')
            try:
                real_link = update_from_pan_weiyun()
            except:
                LOGGER.exception('failed to update from pan weiyun')
                LOGGER.critical('!!! stop update, fallback !!!')
                real_link = FALLBACK_URL
                return
        gevent.sleep(60)


def update_from_pan_360():
    html = urllib2.urlopen(PAN_360_URL).read()
    real_link = html.rpartition('url=')[2][:-2]
    if 'yunpan.cn' not in real_link:
        raise Exception('invalid link: %s' % real_link)
    verify_real_link(real_link)
    LOGGER.info('updated pan 360: %s' % real_link)
    return real_link


class NoRedirectHandler(urllib2.HTTPRedirectHandler):
    def http_error_302(self, req, fp, code, msg, headers):
        infourl = urllib.addinfourl(fp, headers, req.get_full_url())
        infourl.status = code
        infourl.code = code
        return infourl

    http_error_300 = http_error_302
    http_error_301 = http_error_302
    http_error_303 = http_error_302
    http_error_307 = http_error_302


def update_from_pan_weiyun():
    opener = urllib2.build_opener(NoRedirectHandler())
    urllib2.install_opener(opener)
    real_link = opener.open(PAN_WEIYUN_URL).headers['Location']
    if 'qq.com' not in real_link:
        raise Exception('invalid link: %s' % real_link)
    verify_real_link(real_link)
    LOGGER.info('updated pan weiyun: %s' % real_link)
    return real_link


def verify_real_link(link):
    assert int(urllib2.urlopen(link).headers['Content-Length']) > (1024 * 1024)


if '__main__' == __name__:
    main()
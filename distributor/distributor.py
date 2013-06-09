import logging
import logging.handlers
import httplib
import sys
import cgi
import json
import urllib2
import gevent.monkey
import gevent.server
import gevent.wsgi
import contextlib
import base64
import dpkt
import os
import time
import socket

sent_emails = {} # email => sent_at
ANSWER_OK = '2.3.3.0'
ANSWER_TOO_QUICK = '5.5.5.5'
LOGGER = logging.getLogger('distributor')
FQROUTER_INSTALLER_APK_URL = 'https://s3-ap-southeast-1.amazonaws.com/fqrouter/fqrouter-installer.apk'
with contextlib.closing(urllib2.urlopen(FQROUTER_INSTALLER_APK_URL)) as f:
    FQROUTER_INSTALLER_APK = f.read()


def main():
    gevent.monkey.patch_all()
    log_level = logging.INFO
    log_file = 'distributor.log'
    logging.basicConfig(stream=sys.stdout, level=log_level, format='%(asctime)s %(levelname)s %(message)s')
    if log_file:
        handler = logging.handlers.RotatingFileHandler(
            log_file, maxBytes=1024 * 1024 * 10, backupCount=0)
        handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
        handler.setLevel(log_level)
        logging.getLogger('distributor').addHandler(handler)
    gevent.joinall([
        gevent.spawn(serve_http),
        gevent.spawn(serve_dns)
    ])


def serve_http():
    try:
        httpd = gevent.wsgi.WSGIServer(('', 19840), handle_http)
        LOGGER.info('serving HTTP on port 19840...')
    except:
        LOGGER.exception('failed to start HTTP server on port 19840')
        sys.exit(1)
    httpd.serve_forever()


def handle_http(environ, start_response):
    start_response(get_http_response(httplib.OK), [('Content-Type', 'text/plain')])
    try:
        args = cgi.FieldStorage(
            fp=environ['wsgi.input'],
            environ=environ,
            keep_blank_values=True)
        for event in json.loads(args['mandrill_events'].value):
            email = event['msg']['from_email']
            send_email(email)
    except:
        LOGGER.exception('failed to handle http')
    return []


def get_http_response(code):
    return '%s %s' % (code, httplib.responses[code])


def serve_dns():
    address = ('', 53)
    server = HandlerDatagramServer(address, handle_dns)
    LOGGER.info('dns server started at %s:%s' % address)
    try:
        server.serve_forever()
    except:
        LOGGER.exception('dns server failed')
    finally:
        LOGGER.info('dns server stopped')


def handle_dns(sendto, raw_request, address):
    try:
        request = dpkt.dns.DNS(raw_request)
        LOGGER.debug('request: %s' % repr(request))
        domain = ''
        for question in list(request.qd):
            if dpkt.dns.DNS_A == question.type:
                domain = question.name
        LOGGER.debug('domain: %s' % domain)
        email = domain.lower().replace('.want.fqrouter.com', '').replace('.at.', '@').replace('%20', '').strip()
        if '@' not in email:
            return
        if not email:
            return
        if email == 'zhang@163.com':
            return
        if time.time() - sent_emails.get(email, 0) > 60:
            answer = ANSWER_OK
        else:
            LOGGER.debug('ignore email: %s' % email)
            answer = ANSWER_TOO_QUICK
        sent_emails[email] = time.time()
        response = dpkt.dns.DNS(raw_request)
        response.ar = []
        response.set_qr(True)
        response.an = [dpkt.dns.DNS.RR(
            name=domain, type=dpkt.dns.DNS_A, ttl=0,
            ip=socket.inet_aton(answer))]
        sendto(str(response), address)
        if ANSWER_OK == answer:
            send_email(email)
    except:
        LOGGER.exception('failed to handle dns')


class HandlerDatagramServer(gevent.server.DatagramServer):
    def __init__(self, address, handler):
        super(HandlerDatagramServer, self).__init__(address)
        self.handler = handler

    def handle(self, request, address):
        self.handler(self.sendto, request, address)


def send_email(email):
    LOGGER.info('send: %s' % email)
    try:
        data = json.dumps({
            'key': os.getenv('MANDRILLAPP_KEY'),
            'template_name': 'want',
            'template_content': [],
            'message': {
                'to': [{'email': email}],
                'merge_vars': [{'rcpt': email}],
                'attachments': [
                    {
                        'type': 'application/vnd.android.package-archive',
                        'name': 'fqrouter-installer.apk',
                        'content': base64.encodestring(FQROUTER_INSTALLER_APK)
                    }
                ]
            },
            'tags': ['distributor']
        })
        response = urllib2.urlopen('https://mandrillapp.com/api/1.0//messages/send-template.json', data=data)
        LOGGER.info('response: %s' % response.read())
    except:
        LOGGER.exception('failed to send')


if '__main__' == __name__:
    main()
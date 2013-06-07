#!/usr/bin/env python
import logging
import logging.handlers
import gevent.server
import gevent.monkey
import sys
import dpkt
import socket
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.multipart import MIMEBase
from email.mime.text import MIMEText
from email import encoders
import os
import time

LOGGER = logging.getLogger('distributor')
DNS_OPT = 41
sent_emails = {} # email => sent_at
ANSWER_OK = '2.3.3.0'
ANSWER_TOO_QUICK = '5.5.5.5'
with open('fqrouter.apk', 'rb') as f:
    FQROUTER_APK = f.read()

def main():
    log_level = logging.INFO
    log_file = 'distributor.log'
    logging.basicConfig(stream=sys.stdout, level=log_level, format='%(asctime)s %(levelname)s %(message)s')
    if log_file:
        handler = logging.handlers.RotatingFileHandler(
            log_file, maxBytes=1024 * 1024 * 10, backupCount=0)
        handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
        handler.setLevel(log_level)
        logging.getLogger('distributor').addHandler(handler)
    address = ('', 53)
    server = HandlerDatagramServer(address, handle)
    LOGGER.info('dns server started at %s:%s' % address)
    try:
        server.serve_forever()
    except:
        LOGGER.exception('dns server failed')
    finally:
        LOGGER.info('dns server stopped')


def handle(sendto, raw_request, address):
    try:
        request = dpkt.dns.DNS(raw_request)
        LOGGER.debug('request: %s' % repr(request))
        domain = ''
        for question in list(request.qd):
            if dpkt.dns.DNS_A == question.type:
                domain = question.name
        LOGGER.debug('domain: %s' % domain)
        email = domain.lower().replace('.want.fqrouter.com', '').replace('.at.', '@').strip()
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
            LOGGER.info('distribute to email: %s' % email)
            send_email(email)
    except:
        LOGGER.exception('failed to handle')


def send_email(to_email):
    from_email = 'fqrt001@gmail.com'
    msg = MIMEMultipart('alternative')
    msg['Subject'] = 'fqrouter installer'
    msg['From'] = from_email
    msg['To'] = to_email
    msg['X-SMTPAPI'] = '{"category" : "%s"}' % 'distributor'
    part1 = MIMEText('Install the attached apk file to your Android mobile phone.', 'plain', 'utf8')
    msg.attach(part1)
    part2 = MIMEBase('application', 'vnd.android.package-archive')
    part2.set_payload(FQROUTER_APK)
    encoders.encode_base64(part2)
    part2.add_header('Content-Disposition', 'attachment; filename="fqrouter-installer.apk"')
    msg.attach(part2)
    username = os.getenv('SENDGRID_USERNAME')
    password = os.getenv('SENDGRID_PASSWORD')
    s = smtplib.SMTP('smtp.sendgrid.net')
    s.login(username, password)
    s.sendmail(from_email, to_email, msg.as_string())
    s.quit()
    return {}


class HandlerDatagramServer(gevent.server.DatagramServer):
    def __init__(self, address, handler):
        super(HandlerDatagramServer, self).__init__(address)
        self.handler = handler

    def handle(self, request, address):
        self.handler(self.sendto, request, address)


if '__main__' == __name__:
    gevent.monkey.patch_all()
    main()

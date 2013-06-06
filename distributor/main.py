#!/usr/bin/env python
import logging
import logging.handlers
import gevent.server
import gevent.monkey
import sys
import dpkt
import socket

LOGGER = logging.getLogger('distributor')


def main():
    log_level = 'DEBUG'
    log_file = ''
    logging.basicConfig(stream=sys.stdout, level=log_level, format='%(asctime)s %(levelname)s %(message)s')
    if log_file:
        handler = logging.handlers.RotatingFileHandler(
            log_file, maxBytes=1024 * 256, backupCount=0)
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
    request = dpkt.dns.DNS(raw_request)
    domains = [question.name for question in request.qd if dpkt.dns.DNS_A == question.type]
    domain = domains[0]
    LOGGER.info('domain: %s' % domain)
    answer = '2.3.3.0'
    response = dpkt.dns.DNS(raw_request)
    response.set_qr(True)
    response.an = [dpkt.dns.DNS.RR(
        name=domain, type=dpkt.dns.DNS_A, ttl=0,
        rlen=len(socket.inet_aton(answer)),
        rdata=socket.inet_aton(answer))]
    sendto(str(response), address)


class HandlerDatagramServer(gevent.server.DatagramServer):
    def __init__(self, address, handler):
        super(HandlerDatagramServer, self).__init__(address)
        self.handler = handler

    def handle(self, request, address):
        self.handler(self.sendto, request, address)


if '__main__' == __name__:
    gevent.monkey.patch_all()
    main()

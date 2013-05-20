#!/usr/bin/env python
import logging
import logging.handlers
import argparse
import sys
import socket
import fqsocks.china_ip

import dpkt


LOGGER = logging.getLogger('nfqueue-ipset')

RULES = []


def main():
    global DEFAULT_VERDICT
    argument_parser = argparse.ArgumentParser()
    argument_parser.add_argument('--log-file')
    argument_parser.add_argument('--log-level', choices=['INFO', 'DEBUG'], default='INFO')
    argument_parser.add_argument('--queue-number', default=0, type=int)
    argument_parser.add_argument(
        '--rule', default=[], action='append', help='direction,ip_set_name,verdict, for example dst,china,0xfeed1')
    argument_parser.add_argument('--default', default='ACCEPT', help='if no rule matched')
    args = argument_parser.parse_args()
    log_level = getattr(logging, args.log_level)
    logging.basicConfig(stream=sys.stdout, level=log_level, format='%(asctime)s %(levelname)s %(message)s')
    if args.log_file:
        handler = logging.handlers.RotatingFileHandler(
            args.log_file, maxBytes=1024 * 16, backupCount=0)
        handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
        handler.setLevel(log_level)
        logging.getLogger('nfqueue-ipset').addHandler(handler)
    for input in args.rule:
        RULES.append(Rule.parse(input))
    Rule.DEFAULT_VERDICT = Rule.parse_verdict(args.default)
    Rule.MATCHED_DEFAULT = 'default,%s,%s => ' + args.default
    handle_nfqueue(args.queue_number)


def handle_nfqueue(queue_number):
    from netfilterqueue import NetfilterQueue
    while True:
        try:
            nfqueue = NetfilterQueue()
            nfqueue.bind(queue_number, handle_packet)
            LOGGER.info('handling nfqueue at queue number %s' % queue_number)
            nfqueue.run()
        except:
            LOGGER.exception('failed to handle nfqueue')
            return
        finally:
            LOGGER.info('stopped handling nfqueue')


def handle_packet(nfqueue_element):
    try:
        ip_packet = dpkt.ip.IP(nfqueue_element.get_payload())
        src = socket.inet_ntoa(ip_packet.src)
        dst = socket.inet_ntoa(ip_packet.dst)
        verdict = Rule.get_verdict(src, dst)
        if 'ACCEPT' == verdict:
            nfqueue_element.accept()
        elif 'DROP' == verdict:
            nfqueue_element.drop()
        else:
            nfqueue_element.set_mark(verdict)
            nfqueue_element.repeat()
    except:
        LOGGER.exception('failed to handle packet')
        nfqueue_element.accept()


class Rule(object):
    DEFAULT_VERDICT = None
    MATCHED_DEFAULT = None

    def __init__(self, direction, ipset_name, verdict):
        super(Rule, self).__init__()
        self.direction = direction
        self.ipset_name = ipset_name
        self.matched_src = 'src,%s => ' + verdict
        self.matched_dst = 'dst,%s => ' + verdict
        self.verdict = Rule.parse_verdict(verdict)
        assert self.ipset_name == 'china' # it is not a generic implementation yet
        self.match = getattr(self, 'match_%s' % direction)

    def match_src(self, src, dst):
        matched = fqsocks.china_ip.is_china_ip(src)
        if matched:
            LOGGER.info(self.matched_src % src)
        return matched

    def match_dst(self, src, dst):
        matched = fqsocks.china_ip.is_china_ip(dst)
        if matched:
            LOGGER.info(self.matched_dst % dst)
        return matched

    @classmethod
    def get_verdict(cls, src, dst):
        for rule in RULES:
            if rule.match(src, dst):
                return rule.verdict
        LOGGER.info(Rule.MATCHED_DEFAULT % (src, dst))
        return Rule.DEFAULT_VERDICT

    @classmethod
    def parse_verdict(cls, verdict):
        if verdict in ('ACCEPT', 'DROP'):
            return verdict
        else:
            return eval(verdict)

    @classmethod
    def parse(cls, input):
        return Rule(*input.split(','))


if '__main__' == __name__:
    main()
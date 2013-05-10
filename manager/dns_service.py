import logging
import subprocess
import json

import shell
import iptables
import shutdown_hook

LOGGER = logging.getLogger('fqrouter.%s' % __name__)
fqdns_process = None


def run():
    shutdown_hook.add(clean)
    insert_iptables_rules()


def clean():
    delete_iptables_rules()
    try:
        if fqdns_process:
            fqdns_process.terminate()
    except:
        LOGGER.exception('failed to terminate fqdns')


RULES = [
    (
        {'target': 'ACCEPT', 'extra': 'udp dpt:53 mark match 0xcafe'},
        ('nat', 'OUTPUT', '-p udp --dport 53 -m mark --mark 0xcafe -j ACCEPT')
    ), (
        {'target': 'ACCEPT', 'extra': 'udp dpt:53 owner UID match 0'},
        ('nat', 'OUTPUT', '-p udp --dport 53 -m owner --uid-owner 0 -j ACCEPT')
    ), (
        {'target': 'REDIRECT', 'extra': 'udp dpt:53 redir ports 5353'},
        ('nat', 'OUTPUT', '-p udp --dport 53 -j REDIRECT --to-ports 5353')
    ), (
        {'target': 'ACCEPT', 'extra': 'udp dpt:53 mark match 0xcafe'},
        ('nat', 'PREROUTING', '-p udp --dport 53 -m mark --mark 0xcafe -j ACCEPT')
    ), (
        {'target': 'ACCEPT', 'extra': 'udp dpt:53 owner UID match 0'},
        ('nat', 'PREROUTING', '-p udp --dport 53 -m owner --uid-owner 0 -j ACCEPT')
    ), (
        {'target': 'DNAT', 'extra': 'udp dpt:53 to:10.1.2.3:5353'},
        ('nat', 'PREROUTING', '-p udp --dport 53 -j DNAT --to-destination 10.1.2.3:5353')
    )]


def insert_iptables_rules():
    global fqdns_process
    iptables.insert_rules(RULES)
    fqdns_process = subprocess.Popen(
        [shell.PYTHON_PATH, '-m', 'fqdns',
         '--log-file', '/data/data/fq.router/dns.log',
         '--mark', '0xcafe',
         'serve', '--listen', '*:5353'])


def delete_iptables_rules():
    iptables.delete_rules(RULES)


def resolve(record_type, domain_names):
    try:
        args = [shell.PYTHON_PATH, '-m', 'fqdns', 'resolve',
                '--retry', '3', '--timeout', '2',
                '--record-type', record_type] + domain_names
        LOGGER.info('executing: %s' % str(args))
        proc = subprocess.Popen(args, stderr=subprocess.PIPE)
        _, output = proc.communicate()
        LOGGER.info('resolved: %s' % output)
        return json.loads(output)
    except:
        LOGGER.exception('failed to resolve: %s' % domain_names)
        return {}


def get_domain(ip):
    return None
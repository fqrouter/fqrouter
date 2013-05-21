import logging
import json

from gevent import subprocess
import gevent

from utils import shell
from utils import iptables

__MANDATORY__ = True
LOGGER = logging.getLogger('fqrouter.%s' % __name__)
fqdns_process = None


def start():
    global fqdns_process
    insert_iptables_rules()
    fqdns_process = subprocess.Popen(
        [shell.PYTHON_PATH, '-m', 'fqdns',
         '--log-level', 'INFO',
         '--log-file', '/data/data/fq.router/fqdns.log',
         '--outbound-ip', '10.1.2.3', # send from 10.1.2.3 so we can skip redirecting those traffic
         'serve', '--listen', '10.1.2.3:5353',
         '--enable-china-domain', '--enable-hosted-domain'],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    gevent.sleep(1)
    if fqdns_process.poll() is not None:
        try:
            output, _ = fqdns_process.communicate()
            LOGGER.error('fqdns exit output: %s' % output)
        except:
            LOGGER.exception('failed to log fqdns exit output')
        raise Exception('failed to start fqdns')
    LOGGER.info('fqdns started: %s' % fqdns_process.pid)
    gevent.spawn(monitor_fqdns)


def stop():
    delete_iptables_rules()
    try:
        if fqdns_process:
            LOGGER.info('terminate fqdns: %s' % fqdns_process.pid)
            fqdns_process.terminate()
    except:
        LOGGER.exception('failed to terminate fqdns')


def is_alive():
    if fqdns_process:
        return fqdns_process.poll() is None
    return False


RULES = [
    (
        {'target': 'ACCEPT', 'extra': 'udp dpt:53 mark match 0xcafe', 'optional': True},
        ('nat', 'OUTPUT', '-p udp --dport 53 -m mark --mark 0xcafe -j ACCEPT')
    ), (
        {'target': 'DNAT', 'extra': 'udp dpt:53 to:10.1.2.3:5353'},
        ('nat', 'OUTPUT', '-p udp ! -s 10.1.2.3 --dport 53 -j DNAT --to-destination 10.1.2.3:5353')
    ), (
        {'target': 'DNAT', 'extra': 'udp dpt:53 to:10.1.2.3:5353'},
        ('nat', 'PREROUTING', '-p udp ! -s 10.1.2.3 --dport 53 -j DNAT --to-destination 10.1.2.3:5353')
    )]


def insert_iptables_rules():
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)


def monitor_fqdns():
    try:
        output, _ = fqdns_process.communicate()
        if fqdns_process.poll():
            LOGGER.error('fqdns output: %s' % output[-200:])
    except:
        LOGGER.exception('fqdns died')


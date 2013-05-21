import logging

from gevent import subprocess
import gevent

from utils import shell
from utils import iptables

__MANDATORY__ = True
LOGGER = logging.getLogger('fqrouter.%s' % __name__)
fqsocks_process = None


def start():
    insert_iptables_rules()
    start_fqsocks()


def stop():
    delete_iptables_rules()
    try:
        if fqsocks_process:
            LOGGER.info('terminate fqsocks: %s' % fqsocks_process.pid)
            fqsocks_process.terminate()
    except:
        LOGGER.exception('failed to terminate fqsocks')


def is_alive():
    if fqsocks_process:
        return fqsocks_process.poll() is None
    return False


RULES = [
    (
        {'target': 'ACCEPT', 'destination': '127.0.0.1'},
        ('nat', 'OUTPUT', '-p tcp -d 127.0.0.1 -j ACCEPT')
    ), (
        {'target': 'DNAT', 'extra': 'to:10.1.2.3:8319'},
        ('nat', 'OUTPUT', '-p tcp ! -s 10.1.2.3 -j DNAT --to-destination 10.1.2.3:8319')
    ), (
        {'target': 'DNAT', 'extra': 'to:10.1.2.3:8319'},
        ('nat', 'PREROUTING', '-p tcp ! -s 10.1.2.3 -j DNAT --to-destination 10.1.2.3:8319')
    )]


def insert_iptables_rules():
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)


def start_fqsocks():
    global fqsocks_process
    fqsocks_process = subprocess.Popen(
        [shell.PYTHON_PATH, '-m', 'fqsocks',
         '--log-level', 'INFO',
         '--log-file', '/data/data/fq.router/fqsocks.log',
         '--outbound-ip', '10.1.2.3', # send from 10.1.2.3 so we can skip redirecting those traffic
         '--listen', '10.1.2.3:8319',
         '--http-request-mark', '0xbabe', # trigger scrambler
         '--proxy', 'dynamic,n=20,dns_record=proxy#n#.fqrouter.com',
         '--proxy', 'dynamic,n=10,type=goagent,dns_record=goagent#n#.fqrouter.com',
         '--google-host', 'goagent-google-ip.fqrouter.com'],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    gevent.sleep(1)
    if fqsocks_process.poll() is not None:
        try:
            output, _ = fqsocks_process.communicate()
            LOGGER.error('fqsocks exit output: %s' % output)
        except:
            LOGGER.exception('failed to log fqsocks exit output')
        raise Exception('failed to start fqsocks')
    LOGGER.info('fqsocks started: %s' % fqsocks_process.pid)
    gevent.spawn(monitor_fqsocks)


def monitor_fqsocks():
    try:
        output, _ = fqsocks_process.communicate()
        if fqsocks_process.poll():
            LOGGER.error('fqsocks output: %s' % output[-200:])
    except:
        LOGGER.exception('fqsocks died')
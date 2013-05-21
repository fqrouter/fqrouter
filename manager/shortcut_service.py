import logging
import os

from gevent import subprocess
import gevent

import shell
import iptables
import shutdown_hook


LOGGER = logging.getLogger('fqrouter.%s' % __name__)
nfqueue_ipset_process = None


def run():
    try:
        insert_iptables_rules()
        start_nfqueue_ipset()
    except:
        LOGGER.exception('failed to start shortcut service')
        clean()


def clean():
    delete_iptables_rules()
    try:
        if nfqueue_ipset_process:
            LOGGER.info('terminate nfqueue-ipset: %s' % nfqueue_ipset_process.pid)
            nfqueue_ipset_process.terminate()
    except:
        LOGGER.exception('failed to terminate nfqueue-ipset')


def is_alive():
    if nfqueue_ipset_process:
        return nfqueue_ipset_process.poll() is None
    return False


RULES = [
    (
        {'target': 'NFQUEUE', 'extra': 'mark match ! 0xdead NFQUEUE num 1'},
        ('nat', 'OUTPUT', '-m mark ! --mark 0xdead -p tcp -j NFQUEUE --queue-num 1')
    ), (
        {'target': 'NFQUEUE', 'extra': 'mark match ! 0xdead NFQUEUE num 1'},
        ('nat', 'PREROUTING', '-m mark ! --mark 0xdead -p tcp -j NFQUEUE --queue-num 1')
    )]


def insert_iptables_rules():
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)


def start_nfqueue_ipset():
    global nfqueue_ipset_process
    shutdown_hook.add(clean)
    nfqueue_ipset_process = subprocess.Popen(
        [shell.PYTHON_PATH, '-m', 'fqsocks.nfqueue_ipset',
         '--log-level', 'INFO',
         '--queue-number', '1',
         '--rule', 'dst,china,ACCEPT',
         '--default', '0xdead'],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, cwd=os.path.dirname(__file__))
    gevent.sleep(1)
    if nfqueue_ipset_process.poll() is not None:
        try:
            output, _ = nfqueue_ipset_process.communicate()
            LOGGER.error('nfqueue-ipset exit output: %s' % output)
        except:
            LOGGER.exception('failed to log nfqueue-ipset exit output')
        raise Exception('failed to start nfqueue-ipset')
    LOGGER.info('nfqueue-ipset started: %s' % nfqueue_ipset_process.pid)
    gevent.spawn(monitor_nfqueue_ipset)


def monitor_nfqueue_ipset():
    try:
        output, _ = nfqueue_ipset_process.communicate()
        if nfqueue_ipset_process.poll():
            LOGGER.error('nfqueue-ipset output: %s' % output[-200:])
    except:
        LOGGER.exception('nfqueue-ipset died')

import logging
import subprocess
import time
import thread
import os

import shell
import iptables
import shutdown_hook


LOGGER = logging.getLogger('fqrouter.%s' % __name__)
fqsocks_process = None
nfqueue_ipset_process = None


def run():
    insert_iptables_rules()
    start_fqsocks()
    start_nfqueue_ipset()


def clean():
    delete_iptables_rules()
    try:
        if fqsocks_process:
            fqsocks_process.terminate()
    except:
        LOGGER.exception('failed to terminate fqsocks')
    try:
        if nfqueue_ipset_process:
            nfqueue_ipset_process.terminate()
    except:
        LOGGER.exception('failed to terminate nfqueue-ipset')


def is_alive():
    if fqsocks_process:
        return fqsocks_process.poll() is None
    if nfqueue_ipset_process:
        return nfqueue_ipset_process.poll() is None
    return False


RULES = [
    (
        {'target': 'NFQUEUE', 'extra': 'mark match ! 0xdead NFQUEUE num 1'},
        ('nat', 'OUTPUT', '-m mark ! --mark 0xdead -p tcp -j NFQUEUE --queue-num 1')
    ),
    (
        {'target': 'NFQUEUE', 'extra': 'mark match ! 0xdead NFQUEUE num 1'},
        ('nat', 'PREROUTING', '-m mark ! --mark 0xdead -p tcp -j NFQUEUE --queue-num 1')
    ), (
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
    shutdown_hook.add(clean)
    fqsocks_process = subprocess.Popen(
        [shell.PYTHON_PATH, '-m', 'fqsocks',
         '--log-level', 'INFO',
         '--log-file', '/data/data/fq.router/socks.log',
         '--outbound-ip', '10.1.2.3', # send from 10.1.2.3 so we can skip redirecting those traffic
         '--listen', '10.1.2.3:8319',
         '--proxy', 'dynamic,n=20,dns_record=proxy#n#.fqrouter.com',
         '--proxy', 'dynamic,n=10,type=goagent,dns_record=goagent#n#.fqrouter.com',
         '--google-host', 'goagent-google-ip.fqrouter.com'],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    time.sleep(1)
    if fqsocks_process.poll() is not None:
        try:
            output, _ = fqsocks_process.communicate()
            LOGGER.error('fqsocks exit output: %s' % output)
        except:
            LOGGER.exception('failed to log fqsocks exit output')
        raise Exception('failed to start fqsocks')
    thread.start_new(monitor_fqsocks, ())


def monitor_fqsocks():
    try:
        output, _ = fqsocks_process.communicate()
        if fqsocks_process.poll():
            LOGGER.error('fqsocks output: %s' % output[-200:])
    except:
        LOGGER.exception('fqsocks died')


def start_nfqueue_ipset():
    global nfqueue_ipset_process
    shutdown_hook.add(clean)
    nfqueue_ipset_process = subprocess.Popen(
        [shell.PYTHON_PATH, '-m', 'nfqueue_ipset',
         '--log-level', 'INFO',
         '--log-file', '/data/data/fq.router/nfqueue-ipset.log',
         '--queue-number', '1',
         '--rule', 'dst,china,ACCEPT',
         '--default', '0xdead'],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, cwd=os.path.dirname(__file__))
    time.sleep(1)
    if nfqueue_ipset_process.poll() is not None:
        try:
            output, _ = nfqueue_ipset_process.communicate()
            LOGGER.error('nfqueue-ipset exit output: %s' % output)
        except:
            LOGGER.exception('failed to log nfqueue-ipset exit output')
        raise Exception('failed to start nfqueue-ipset')
    thread.start_new(monitor_nfqueue_ipset, ())


def monitor_nfqueue_ipset():
    try:
        output, _ = nfqueue_ipset_process.communicate()
        if nfqueue_ipset_process.poll():
            LOGGER.error('nfqueue-ipset output: %s' % output[-200:])
    except:
        LOGGER.exception('nfqueue-ipset died')
        
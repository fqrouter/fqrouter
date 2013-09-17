import logging

import shell
import iptables

LOGGER = logging.getLogger('fqrouter.%s' % __name__)
nfqueue_ipset_process = None


def start():
    if not is_alive():
        insert_iptables_rules()
        start_nfqueue_ipset()


def stop():
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
    nfqueue_ipset_process = shell.launch_python(
        'fqsocks.nfqueue_ipset', ('--log-level', 'INFO',
        '--queue-number', '1',
        '--rule', 'dst,china,ACCEPT',
        '--default', '0xdead'), on_exit=stop)
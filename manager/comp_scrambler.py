import logging

from gevent import subprocess
import gevent

from utils import shell
from utils import iptables

LOGGER = logging.getLogger('fqrouter.%s' % __name__)
fqting_process = None


def start():
    global fqting_process
    insert_iptables_rules()
    fqting_process = subprocess.Popen(
        [shell.PYTHON_PATH, '-m', 'fqting',
         '--log-level', 'INFO',
         '--log-file', '/data/data/fq.router/fqting.log',
         '--queue-number', '2',
         '--mark', '0xcafe'],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    gevent.sleep(1)
    if fqting_process.poll() is not None:
        try:
            output, _ = fqting_process.communicate()
            LOGGER.error('fqting exit output: %s' % output)
        except:
            LOGGER.exception('failed to log fqting exit output')
        raise Exception('failed to start fqting')
    LOGGER.info('fqting started: %s' % fqting_process.pid)
    gevent.spawn(monitor_fqting)


def stop():
    delete_iptables_rules()
    try:
        if fqting_process:
            LOGGER.info('terminate fqting: %s' % fqting_process.pid)
            fqting_process.terminate()
    except:
        LOGGER.exception('failed to terminate fqting')


def is_alive():
    if fqting_process:
        return fqting_process.poll() is None
    return False


RULES = []


def add_rules(is_forward):
    if not is_forward:
        RULE_INPUT_ICMP = (
            {'target': 'NFQUEUE', 'extra': 'NFQUEUE num 2'},
            ('filter', 'INPUT', '-p icmp -j NFQUEUE --queue-num 2')
        )
        RULES.append(RULE_INPUT_ICMP)
        RULE_INPUT_DNS_RESPONSE = (
            {'target': 'NFQUEUE', 'extra': 'udp spt:53 dpt:1 NFQUEUE num 2'},
            ('filter', 'INPUT', '-p udp --sport 53 --dport 1 -j NFQUEUE --queue-num 2')
        )
        RULES.append(RULE_INPUT_DNS_RESPONSE)
        RULE_OUTPUT_HTTP_REQUST = (
            {'target': 'NFQUEUE', 'extra': 'mark match 0xbabe NFQUEUE num 2'},
            ('filter', 'OUTPUT', '-p tcp -m mark --mark 0xbabe -j NFQUEUE --queue-num 2')
        )
        RULES.append(RULE_OUTPUT_HTTP_REQUST)
    RULE_INPUT_SYN_ACK = (
        {'target': 'NFQUEUE', 'extra': 'tcpflags: 0x3F/0x12 NFQUEUE num 2'},
        ('filter', 'FORWARD' if is_forward else 'INPUT', '-p tcp --tcp-flags ALL SYN,ACK -j NFQUEUE --queue-num 2')
    )
    RULES.append(RULE_INPUT_SYN_ACK)
    RULE_INPUT_RST = (
        {'target': 'NFQUEUE', 'extra': 'tcpflags: 0x3F/0x04 NFQUEUE num 2'},
        ('filter', 'FORWARD' if is_forward else 'INPUT', '-p tcp --tcp-flags ALL RST -j NFQUEUE --queue-num 2')
    )
    RULES.append(RULE_INPUT_RST)
    RULE_OUTPUT_SYN = (
        {'target': 'NFQUEUE', 'extra': 'tcpflags: 0x3F/0x02 NFQUEUE num 2'},
        ('filter', 'FORWARD' if is_forward else 'OUTPUT', '-p tcp --tcp-flags ALL SYN -j NFQUEUE --queue-num 2')
    )
    RULES.append(RULE_OUTPUT_SYN)


add_rules(is_forward=False)
add_rules(is_forward=True)


def insert_iptables_rules():
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)


def monitor_fqting():
    try:
        output, _ = fqting_process.communicate()
        if fqting_process.poll():
            LOGGER.error('fqting output: %s' % output[-200:])
    except:
        LOGGER.exception('fqting died')
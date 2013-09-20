#!/usr/bin/env python
# used for debugging purpose, run on ubuntu 12.04
import subprocess
import signal
import atexit
import os
import gevent
import gevent.monkey

RULES = [
    # masquerade
    'POSTROUTING -t nat -s 10.1.2.3 -j MASQUERADE',
    # dns
    'OUTPUT -t nat -p udp --dport 53 -m mark --mark 0xcafe -j ACCEPT',
    'OUTPUT -t nat -p udp ! -s 10.1.2.3 --dport 53 -j DNAT --to-destination 10.1.2.3:12345',
    # proxy
    'OUTPUT -t nat -p tcp ! -s 10.1.2.3 -j DNAT --to-destination 10.1.2.3:12345',
    # scrambler
    'INPUT -t filter -p icmp -j NFQUEUE --queue-num 2',
    'INPUT -t filter -p udp --sport 53 --dport 1 -j NFQUEUE --queue-num 2',
    'OUTPUT -t filter -p tcp -m mark --mark 0xbabe -j NFQUEUE --queue-num 2',
    'INPUT -t filter -p tcp --tcp-flags ALL SYN,ACK -j NFQUEUE --queue-num 2',
    'INPUT -t filter -p tcp --tcp-flags ALL RST -j NFQUEUE --queue-num 2',
    'OUTPUT -t filter -p tcp --tcp-flags ALL SYN -j NFQUEUE --queue-num 2'
]
processes = []


def main():
    gevent.monkey.patch_all()
    signal.signal(signal.SIGTERM, lambda signum, fame: teardown())
    signal.signal(signal.SIGINT, lambda signum, fame: teardown())
    atexit.register(teardown)
    setup()
    gevent.joinall([gevent.spawn(process.communicate) for process in processes])


def setup():
    for rule in reversed(RULES):
        subprocess.call('iptables -I %s' % rule, shell=True)
    subprocess.call('ifconfig lo:1 10.1.2.3 netmask 255.255.255.255', shell=True)
    processes.append(subprocess.Popen(
        'python -m fqsocks '
        '--tcp-listen 10.1.2.3:12345 '
        '--dns-listen 10.1.2.3:12345 '
        '--http-listen *:2516 '
        '--manager-listen *:2515 '
        '--outbound-ip 10.1.2.3 '
        # '--log-level DEBUG '
        '--config-dir %s '
        '--proxy directory,src=proxies.fqrouter.com,goagent=True,ss=True '
        # '--proxy dynamic,n=8,type=ss,dns_record=ss#n#.fqrouter.com,priority=3 '
        # '--proxy dynamic,n=10,type=goagent,dns_record=goagent#n#.fqrouter.com,priority=1 '
        '--disable-access-check '
        # '--disable-direct-access '
        '--google-host goagent-google-ip.fqrouter.com '
        '--google-host goagent-google-ip2.fqrouter.com ' % os.path.join(os.path.dirname(__file__), 'etc'),
        shell=True,
        # stderr=subprocess.STDOUT, stdout=subprocess.PIPE
    ))
    processes.append(subprocess.Popen(
        'python -m fqting --queue-number 2 --mark 0xcafe --log-level DEBUG', shell=True,
        # stderr=subprocess.STDOUT, stdout=subprocess.PIPE
    ))


def teardown():
    for rule in RULES:
        subprocess.call('iptables -D %s' % rule, shell=True)
    for process in processes:
        try:
            process.terminate()
        except:
            pass


if '__main__' == __name__:
    main()

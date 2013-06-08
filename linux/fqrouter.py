#!/usr/bin/env python
# used for debugging purpose, run on ubuntu 12.04
import subprocess
import signal
import atexit
import gevent
import gevent.monkey

RULES = [
    # dns
    'OUTPUT -t nat -p udp ! -s 10.1.2.3 --dport 53 -j REDIRECT --to-ports 5353',
    'POSTROUTING -t nat -s 10.1.2.3 -j MASQUERADE',
    # proxy
    'OUTPUT -t nat -p tcp ! -s 10.1.2.3 -j DNAT --to-destination 10.1.2.3:8319',
    # scrambler
    # 'INPUT -t filter -p icmp -j NFQUEUE --queue-num 2',
    # 'INPUT -t filter -p udp --sport 53 --dport 1 -j NFQUEUE --queue-num 2',
    # 'OUTPUT -t filter -p tcp -m mark --mark 0xbabe -j NFQUEUE --queue-num 2',
    # 'INPUT -t filter -p tcp --tcp-flags ALL SYN,ACK -j NFQUEUE --queue-num 2',
    # 'INPUT -t filter -p tcp --tcp-flags ALL RST -j NFQUEUE --queue-num 2',
    # 'OUTPUT -t filter -p tcp --tcp-flags ALL SYN -j NFQUEUE --queue-num 2'
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
    for rule in RULES:
        subprocess.call('iptables -I %s' % rule, shell=True)
    subprocess.call('ifconfig lo:1 10.1.2.3 netmask 255.255.255.255', shell=True)
    processes.append(subprocess.Popen(
        'python -m fqdns --outbound-ip 10.1.2.3 '
        'serve --listen 127.0.0.1:5353 '
        '--enable-hosted-domain '
        '--enable-china-domain',
        shell=True, stderr=subprocess.STDOUT, stdout=subprocess.PIPE))
    processes.append(subprocess.Popen(
        'python -m fqsocks --outbound-ip 10.1.2.3 '
        '--listen 10.1.2.3:8319 '
        # '--log-level DEBUG '
        '--http-request-mark 0xbabe '
        '--proxy dynamic,n=4,type=ss,dns_record=ss#n#.fqrouter.com '
        '--proxy dynamic,n=20,dns_record=proxy#n#.fqrouter.com '
        '--proxy dynamic,n=5,dns_record=proxy2#n#.fqrouter.com,is_public=False '
        '--proxy dynamic,n=10,type=goagent,dns_record=goagent#n#.fqrouter.com '
        '--google-host goagent-google-ip.fqrouter.com ',
        # '--disable-access-check',
        shell=True))
    processes.append(subprocess.Popen('python -m fqting --queue-number 2 --mark 0xcafe', shell=True))


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
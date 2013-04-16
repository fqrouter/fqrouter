#!/usr/bin/env python
import subprocess
import shlex
import time
import sys
import os
import argparse

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
import manager.china_ip

argument_parser = argparse.ArgumentParser()
argument_parser.add_argument('--proxy-list', action='append')
argument_parser.add_argument('--proxy', action='append')
args = argument_parser.parse_args()

PROXY_LIST_DIR = os.path.join(os.path.dirname(__file__), 'proxy-list')


def log(message):
    sys.stderr.write(message)
    sys.stderr.write('\n')


proxies = set()


def add_proxy(line):
    line = line.strip()
    if not line:
        return
    ip, port = line.split(':')
    if manager.china_ip.is_china_ip(ip):
        log('skip china ip: %s' % ip)
    else:
        proxies.add((ip, port, 0))


if args.proxy:
    for proxy in args.proxy:
        add_proxy(proxy)
if args.proxy_list:
    for command in args.proxy_list:
        try:
            log('executing %s' % command)
            lines = subprocess.check_output(command, shell=True, cwd=PROXY_LIST_DIR).splitlines(False)
            log('succeeded, %s lines' % len(lines))
            for line in lines:
                add_proxy(line)
        except subprocess.CalledProcessError, e:
            log('failed, output:')
            log(e.output)


class Checker(object):
    def __init__(self, ip, port, elapsed):
        self.ip = ip
        self.port = port
        self.elapsed = elapsed
        self.proc = subprocess.Popen(
            shlex.split('curl --proxy %s:%s https://www.amazon.com/404' % (ip, port)),
            stderr=subprocess.STDOUT, stdout=subprocess.PIPE)
        self.started_at = time.time()

    def is_ok(self):
        if 0 == self.proc.poll() and 'Amazon.com' in self.proc.stdout.read():
            return round(time.time() - self.started_at, 2)
        return 0

    def is_failed(self):
        return self.proc.poll()

    def is_timed_out(self):
        return time.time() - self.started_at > 5

    def kill(self):
        self.proc.kill()


checkers = []
checked_proxies = []
for i in range(10):
    log('PASS %s' % (i + 1))
    while len(proxies) + len(checkers):
        for checker in list(checkers):
            try:
                ok = checker.is_ok()
                if ok:
                    log('OK[%s] %s:%s' % (ok, checker.ip, checker.port))
                    checked_proxies.append((checker.ip, checker.port, checker.elapsed + ok))
                    checkers.remove(checker)
                elif checker.is_failed():
                    log('FAILED %s:%s' % (checker.ip, checker.port))
                    checkers.remove(checker)
                elif checker.is_timed_out():
                    log('TIMEOUT %s:%s' % (checker.ip, checker.port))
                    checkers.remove(checker)
                    try:
                        checker.kill()
                    except:
                        pass
            except:
                log('FATAL %s:%s' % (checker.ip, checker.port))
                checkers.remove(checker)
        new_checkers_count = 4 - len(checkers)
        for i in range(new_checkers_count):
            if proxies:
                ip, port, elapsed = proxies.pop()
                checkers.append(Checker(ip, port, elapsed))
        time.sleep(0.2)
    proxies = checked_proxies
    checked_proxies = []

for ip, port, elapsed in sorted(proxies, key=lambda proxy: proxy[2])[:30]:
    print('%s:%s' % (ip, port))
print('')
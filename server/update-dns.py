#!/usr/bin/env python
import sys
import subprocess

proxies = {}
current_index = 1
while True:
    line = sys.stdin.readline()
    line = line.strip()
    if not line:
        break
    ip, port = line.split(':')
    domain = 'proxy%s' % current_index
    value = 'http-connect:%s:%s::' % (ip, port)
    print('%s => %s' % (domain, value))
    subprocess.call('cli53 rrcreate fqrouter.com %s TXT %s --ttl 3600 --replace' % (domain, value), shell=True)
    current_index += 1


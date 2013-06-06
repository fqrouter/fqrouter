#!/usr/bin/env python
import sys
import subprocess
import traceback

proxies = {}
current_index = 1
while True:
    line = sys.stdin.readline()
    line = line.strip()
    if not line:
        break
    print('line: %s' % line)
    try:
        domain = 'proxy%s' % current_index
        ip, port = line.split(':')
        value = 'http-connect:%s:%s::' % (ip, port)
        proxies[domain] = value
        current_index += 1
    except:
        traceback.print_exc()
for domain, value in proxies.items():
    print('%s => %s' % (domain, value))
    subprocess.call('cli53 rrcreate fqrouter.com %s TXT %s --ttl 3600 --replace' % (domain, value), shell=True)
for i in range(20 - len(proxies)):
    domain = 'proxy%s' % (20 - i)
    print('%s => %s' % (domain, ''))
    subprocess.call('cli53 rrcreate fqrouter.com %s TXT %s --ttl 3600 --replace' % (domain, '""'), shell=True)


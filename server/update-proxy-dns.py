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
        ip, port = line.split(':')
        domain = 'proxy%s' % current_index
        value = 'http-connect:%s:%s::' % (ip, port)
        print('%s => %s' % (domain, value))
        subprocess.call('cli53 rrcreate fqrouter.com %s TXT %s --ttl 3600 --replace' % (domain, value), shell=True)
        current_index += 1
    except:
        traceback.print_exc()


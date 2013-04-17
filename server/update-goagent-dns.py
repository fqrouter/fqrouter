#!/usr/bin/env python
import sys
import subprocess

proxies = {}
current_index = 1
while True:
    appid = sys.stdin.readline()
    appid = appid.strip()
    if not appid:
        break
    domain = 'goagent%s' % current_index
    print('%s => %s' % (domain, appid))
    subprocess.call('cli53 rrcreate fqrouter.com %s TXT %s --ttl 3600 --replace' % (domain, appid), shell=True)
    current_index += 1


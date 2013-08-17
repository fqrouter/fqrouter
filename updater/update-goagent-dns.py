#!/usr/bin/env python
import sys
import subprocess
import itertools

proxies = {}
current_index = 1
appids = []
while True:
    appid = sys.stdin.readline()
    appid = appid.strip()
    if not appid:
        break
    appids.append(appid)
if len(appids) < 10:
    appids += list(itertools.repeat('""', 10 - len(appids)))
for appid in appids:
    domain = 'goagent%s' % current_index
    print('%s => %s' % (domain, appid))
    subprocess.call('cli53 rrcreate fqrouter.com %s TXT %s --ttl 900 --replace' % (domain, appid), shell=True)
    current_index += 1


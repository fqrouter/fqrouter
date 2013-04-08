#!/usr/bin/env python
import sys
import os
import subprocess

entries = {}
if len(sys.argv) > 1:
    ip, port = sys.argv[2].split(':')
    env_name = 'TOKEN_PROXY%s' % sys.argv[1]
    token = os.getenv(env_name)
    if not token:
        raise Exception('missing token: %s' % env_name)
    entries[token] = 'http-connect:%s:%s::' % (ip, port)
else:
    print('not implemented')

for token, value in entries.items():
    subprocess.check_call(
        'curl -k -X PUT -d "ip=%s" https://entrydns.net/records/modify/%s' % (value, token), shell=True)
#!/usr/bin/env python
import subprocess
import time
import random
import datetime

while True:
    subprocess.call('cli53 rrcreate fqrouter.com ss5 TXT 192.241.219.92:292%s:111222:table --ttl 900 --replace'
                    % random.randint(0, 9), shell=True)
    subprocess.call('cli53 rrcreate fqrouter.com ss6 TXT 192.241.214.176:292%s:111222:table --ttl 900 --replace'
                    % random.randint(0, 9), shell=True)
    subprocess.call('cli53 rrcreate fqrouter.com ss7 TXT 192.241.216.203:292%s:111222:table --ttl 900 --replace'
                    % random.randint(0, 9), shell=True)
    subprocess.call('cli53 rrcreate fqrouter.com ss8 TXT 192.241.225.189:292%s:111222:table --ttl 900 --replace'
                    % random.randint(0, 9), shell=True)
    print('%s done' % datetime.datetime.now())
    time.sleep(900)

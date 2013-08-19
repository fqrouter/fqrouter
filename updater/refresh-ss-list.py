#!/usr/bin/env python
import subprocess
import time
import random
import datetime
import socket

while True:
    subprocess.call('cli53 rrcreate fqrouter.com ss5 TXT %s:292%s:111222:table --ttl 900 --replace'
                    % (socket.gethostbyname('socks2.masaila.info'), random.randint(0, 9)), shell=True)
    subprocess.call('cli53 rrcreate fqrouter.com ss6 TXT %s:292%s:111222:table --ttl 900 --replace'
                    % (socket.gethostbyname('socks3.masaila.info'), random.randint(0, 9)), shell=True)
    subprocess.call('cli53 rrcreate fqrouter.com ss7 TXT %s:292%s:111222:table --ttl 900 --replace'
                    % (socket.gethostbyname('socks4.masaila.info'), random.randint(0, 9)), shell=True)
    subprocess.call('cli53 rrcreate fqrouter.com ss8 TXT %s:292%s:111222:table --ttl 900 --replace'
                    % (socket.gethostbyname('socks5.masaila.info'), random.randint(0, 9)), shell=True)
    print('%s done' % datetime.datetime.now())
    time.sleep(900)

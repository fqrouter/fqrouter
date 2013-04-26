#!/usr/bin/env python
import urllib2
import re
import struct
import socket
import sys

import lxml.html
from pyquery.pyquery import PyQuery

if len(sys.argv) > 1:
    country = sys.argv[1]
else:
    country = 'us' # us,br,ru,fr,de,ua,pl,id

RE_ENCODED_IP = re.compile(r'decode\("(.+)"\)')

opener = urllib2.build_opener()
opener.addheaders = [('User-agent', 'Mozilla/5.0')]
html = opener.open('http://www.proxynova.com/proxy-server-list/country-%s/' % country).read()
d = PyQuery(lxml.html.fromstring(html))
proxies = []
for tr in d('tr').items():
    script = tr.find('.row_proxy_ip').text()
    if not script:
        continue
    match = RE_ENCODED_IP.search(script)
    if not match:
        continue
    try:
        ip_int = int(match.group(1).replace('fgh', '2').replace('iop', '1').replace('ray', '0'))
        ip = socket.inet_ntoa(struct.pack('!I', ip_int))
        port = tr.find('.row_proxy_port').text()
        print('%s:%s' % (ip, port))
    except:
        continue
print('')
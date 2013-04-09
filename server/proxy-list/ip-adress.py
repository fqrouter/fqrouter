#!/usr/bin/env python
import urllib2
import re

import lxml.html
from pyquery.pyquery import PyQuery

RE_PROXY = re.compile(r'(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)')
opener = urllib2.build_opener()
opener.addheaders = [('User-agent', 'Mozilla/5.0')]
html = opener.open('http://www.ip-adress.com/proxy_list/').read()
d = PyQuery(lxml.html.fromstring(html))
proxies = []
for td in d('td').items():
    match = RE_PROXY.match(td.text())
    if match:
        ip = match.group(1)
        port = match.group(2)
        print('%s:%s' % (ip, port))
print('')
#!/usr/bin/env python
import urllib2
import re
import sys
import os

import lxml.html
from pyquery.pyquery import PyQuery

if len(sys.argv) > 1:
    page = sys.argv[1]
else:
    page = 1
sys.path.append(os.path.join(os.path.dirname(__file__), '../../'))

RE_PROXY = re.compile(r'(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}) document.write\(":"(.*)\)')
DICT = {
    '+r': '8',
    '+d': '0',
    '+z': '3',
    '+m': '4',
    '+b': '5',
    '+k': '2',
    '+l': '9',
    '+i': '7',
    '+w': '6',
    '+c': '1'
}
opener = urllib2.build_opener()
opener.addheaders = [('User-agent', 'Mozilla/5.0')]
html = opener.open('http://www.cnproxy.com/proxy%s.html' % page).read()
d = PyQuery(lxml.html.fromstring(html))
proxies = []
for td in d('td').items():
    match = RE_PROXY.match(td.text())
    if match:
        ip = match.group(1)
        port = match.group(2)
        for k, v in DICT.items():
            port = port.replace(k, v)
        print('%s:%s' % (ip, port))
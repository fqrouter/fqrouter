#!/usr/bin/env python
import urllib2
import re
import sys

import lxml.html
from pyquery.pyquery import PyQuery

if len(sys.argv) > 1:
    page = sys.argv[1]
else:
    page = 1

RE_PROXY = re.compile(r'(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}) document.write\(":"(.*)\)')
RE_MAPPING = re.compile(r'(.)="(.)"')
DICT = {}
opener = urllib2.build_opener()
opener.addheaders = [('User-agent', 'Mozilla/5.0')]
html = opener.open('http://www.cnproxy.com/proxy%s.html' % page).read()
d = PyQuery(lxml.html.fromstring(html))
for mapping in d('script:first').text().split(';'):
    match = RE_MAPPING.match(mapping)
    if match:
        DICT['+%s' % match.group(1)] = match.group(2)
proxies = []
for td in d('td').items():
    match = RE_PROXY.match(td.text())
    if match:
        ip = match.group(1)
        port = match.group(2)
        for k, v in DICT.items():
            port = port.replace(k, v)
        print('%s:%s' % (ip, port))
print('')
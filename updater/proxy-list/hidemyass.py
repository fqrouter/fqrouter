#!/usr/bin/env python
import urllib2
import sys

import lxml.html
from pyquery.pyquery import PyQuery

if len(sys.argv) > 1:
    page = sys.argv[1]
else:
    page = 1

opener = urllib2.build_opener()
opener.addheaders = [('User-agent', 'Mozilla/5.0')]
html = opener.open('http://www.hidemyass.com/proxy-list/%s' % page).read()
d = PyQuery(lxml.html.fromstring(html))
proxies = []
for tr in d('#listtable tr').items():
    style_element = tr.find('style')
    style_src = style_element.text()
    if not style_src:
        continue
    for line in style_src.splitlines():
        if 'display:none' in line:
            css_class = line[:line.find('{')]
            tr.find(css_class).remove()
    td_element = style_element.parents('td')
    style_element.remove()
    display_none_elements = []
    for span in list(td_element.find('span').items()):
        if span.attr('style') and 'display:none' in span.attr('style'):
            display_none_elements.append(span)
    for div in list(td_element.find('div').items()):
        if div.attr('style') and 'display:none' in div.attr('style'):
            display_none_elements.append(div)
    for span in display_none_elements:
        span.remove()
    ip = td_element.text().replace(' ', '')
    port = list(tr.find('td').items())[2].text()
    proxy_type = list(tr.find('td').items())[6].text()
    if 'HTTPS' == proxy_type:
        print('%s:%s' % (ip, port))
print('')
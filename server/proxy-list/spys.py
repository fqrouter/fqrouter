#!/usr/bin/env python
import urllib2
import cookielib
import re
import sys

import lxml.html
from pyquery.pyquery import PyQuery

DICT = [
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
    'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
    'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D',
    'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X']
RE_IP = re.compile(r'(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})')
RE_DOCUMENT_WRITE = re.compile(r'document\.write\(.*?\((.*?)\)\)')
RE_PARAM = re.compile(r"\}\('(.+);',(\d+),(\d+),'(.+)'.split")
RE_SUB = re.compile(r'(.)=(.)(\^(.);)?')
RE_PORT = re.compile(r'\((.+?)\^(.+?)\)')

if len(sys.argv) > 1:
    page = sys.argv[1]
else:
    page = 1


def main():
    cj = cookielib.CookieJar()
    opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(cj))
    opener.addheaders = [('User-agent', 'Mozilla/5.0')]
    html = opener.open('http://spys.ru/en/https-ssl-proxy/%s/' % page).read()
    d = PyQuery(lxml.html.fromstring(html))
    vars = None
    for script in d('script').items():
        if 'eval' in script.text():
            vars = eval_vars(script.text())
    if not vars:
        return
    cur = 0
    while True:
        ip_match = RE_IP.search(html, cur)
        if not ip_match:
            break
        port_match = RE_DOCUMENT_WRITE.search(html, ip_match.end())
        if not port_match:
            break
        cur = port_match.end()
        port_text = '(%s)' % port_match.group(1)
        port = parse_port(port_text, vars)
        print('%s:%s' % (ip_match.group(1), port))
    print('')


def eval_vars(src):
    match = RE_PARAM.search(src)
    pattern = match.group(1) + ';'
    input = match.group(4).split('^')
    s = {}
    for i in range(60):
        s[DICT[i]] = input[i] or DICT[i]
    vars = {}
    for match in RE_SUB.findall(pattern):
        name, val1, _, val2 = match
        name = s[name]
        val1 = s[val1]
        if val1 in vars:
            val1 = vars[val1]
        try:
            val1 = int(val1)
        except:
            val1 = 0
        if not val2:
            vars[name] = val1
            continue
        val2 = s[val2]
        if val2 in vars:
            val2 = vars[val2]
        try:
            val2 = int(val2)
        except:
            val2 = 0
        vars[name] = val1 ^ val2
    return vars


def parse_port(port_text, vars):
    port = []
    for part in RE_PORT.findall(port_text):
        val1, val2 = part
        port.append(str(vars[val1] ^ vars[val2]))
    return ''.join(port)


main()
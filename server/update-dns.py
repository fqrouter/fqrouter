#!/usr/bin/env python
import urllib2
import urllib
import cookielib
import os
import sys

from pyquery.pyquery import PyQuery
import lxml.html


PASSWORD = os.getenv('PASSWORD')
if not PASSWORD:
    raise Exception('missing password')

proxies = {}
current_index = 1
while True:
    line = sys.stdin.readline()
    line = line.strip()
    if not line:
        break
    ip, port = line.split(':')
    proxies['proxy%s.test.fqrouter.com' % current_index] = 'http-connect:%s:%s::' % (ip, port)
    current_index += 1

cj = cookielib.CookieJar()
opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(cj))
opener.addheaders = [
    ('User-Agent', 'Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.2.11) Gecko/20101012 Firefox/3.6.11'),
]


class Page(object):
    def __init__(self, html):
        self.html = html
        self.q = PyQuery(lxml.html.fromstring(html))

    def fill(self, selector, value):
        self.q(selector).attr('value', value)

    def submit(self, selector):
        form = self.q(selector)
        values = {}
        for input in form.find('input').items():
            values[input.attr('name').encode('utf8')] = input.attr('value').encode('utf8')
        return Page.open(form.attr('action'), data=urllib.urlencode(values))

    @classmethod
    def open(cls, url, data=None):
        return cls(opener.open('https://entrydns.net%s' % url, data=data).read())


login_page = Page.open('/users/sign_in')
print('LOGIN FORM LOADED')
login_page.fill('form [name=user\[email\]]', 'fqrouter@gmail.com')
login_page.fill('form [name=user\[password\]]', PASSWORD)
home_page = login_page.submit('form')
print('LOGIN FORM SUBMITTED')
for link in home_page.q('a').items():
    href = link.attr('href')
    if href and '/records' in href:
        records_page = Page.open(href)
        print('RECORDS PAGE')
        for tr in records_page.q('tr.record').items():
            domain = tr.find('.name-column').text()
            edit_url = tr.find('a.edit').attr('href')
            if domain in proxies:
                edit_page = Page.open(edit_url)
                edit_page.fill('form [name=record\[content\]]', proxies[domain])
                edit_page.submit('form')
                print('UPDATED: %s => %s' % (domain, proxies[domain]))

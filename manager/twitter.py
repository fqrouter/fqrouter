import urllib2
import sys

import full_proxy_service


def check():
    success = 0
    times = full_proxy_service.PROXIES_COUNT * 2
    for i in range(times):
        try:
            urllib2.urlopen('https://www.twitter.com', timeout=10).read()
            success += 1
        except:
            pass
    sys.stderr.write(repr('%s/%s' % (success, times)))
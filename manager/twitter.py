import urllib2
import logging

import gevent

import full_proxy_service


LOGGER = logging.getLogger('fqrouter.%s' % __name__)


def check():
    import gevent.monkey

    gevent.monkey.patch_all()
    LOGGER.info('checking twitter access...')
    success = 0
    times = full_proxy_service.PROXIES_COUNT * 2
    checkers = []
    for i in range(times):
        checker = gevent.spawn(check_once, i)
        checkers.append(checker)
    for checker in checkers:
        if checker.get():
            success += 1
    success_rate = '%s/%s' % (success, times)
    LOGGER.info('twitter access success rate: %s' % success)
    return success_rate


def check_once(index):
    try:
        LOGGER.info('[attempt-%s] started' % index)
        urllib2.urlopen('https://www.twitter.com', timeout=10).read()
        LOGGER.info('[attempt-%s] succeed' % index)
        return True
    except:
        LOGGER.exception('[attempt-%s] failed' % index)
    return False




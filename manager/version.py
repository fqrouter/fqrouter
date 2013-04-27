import logging
import httplib

import dpkt
import dns_resolver


LOGGER = logging.getLogger('fqrouter.%s' % __name__)
VER_DOMAIN = 'beta.android.ver.fqrouter.com'


def handle_latest(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield resolve_latest_version()


def resolve_latest_version():
    try:
        answers = dns_resolver.resolve(dpkt.dns.DNS_TXT, [VER_DOMAIN])
        answer = answers.get(VER_DOMAIN)
        if not answer:
            return None
        ver = ''.join(e for e in answer if e.isalnum() or e in {'.', '|', ':', '/', '-', '_'})
        LOGGER.info('resolved latest version %s => %s' % (VER_DOMAIN, ver))
        return ver
    except:
        LOGGER.exception('failed to resolve latest version %s' % VER_DOMAIN)
        return None
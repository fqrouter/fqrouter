import logging
import httplib

import dpkt
import dns_resolver


LOGGER = logging.getLogger('fqrouter.%s' % __name__)
VER_DOMAIN = 'beta.android.ver.fqrouter.com'


def handle_latest(environ):
    return httplib.OK, [('Content-Type', 'text/plain')], resolve_latest_version()


def resolve_latest_version():
    try:
        answer = dns_resolver.resolve(VER_DOMAIN, record_type=dpkt.dns.DNS_TXT)
        ver = ''.join(e for e in answer.rdata if e.isalnum() or e in {'.', '|', ':', '/', '-', '_'})
        LOGGER.info('resolved latest version %s => %s' % (VER_DOMAIN, ver))
        return ver
    except:
        LOGGER.exception('failed to resolve latest version %s' % VER_DOMAIN)
        return None
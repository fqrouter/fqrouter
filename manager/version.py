import logging
import socket
import httplib

import dpkt


LOGGER = logging.getLogger(__name__)
DOMAIN = 'beta.android.ver.fqrouter.com'

def handle_latest(environ):
    return httplib.OK, [('Content-Type', 'text/plain')], resolve_latest_version()


def resolve_latest_version():
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
        sock.settimeout(3)
        request = dpkt.dns.DNS(
            qd=[dpkt.dns.DNS.Q(name=DOMAIN, type=dpkt.dns.DNS_TXT)])
        sock.sendto(str(request), ('8.8.8.8', 53))
        data, addr = sock.recvfrom(1024)
        response = dpkt.dns.DNS(data)
        answer = response.an[0]
        ver = ''.join(e for e in answer.rdata if e.isalnum() or e in {'.', '|', ':', '/', '-', '_'})
        LOGGER.info('resolved latest version %s => %s' % (DOMAIN, ver))
        return ver
    except:
        LOGGER.exception('failed to resolve latest version %s' % DOMAIN)
        return None
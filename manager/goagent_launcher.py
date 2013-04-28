import logging
import sys

import goagent


LOGGER = logging.getLogger('fqrouter.%s' % __name__)
failures_count = 0
server = None


def modified_gae_urlfetch(*args, **kwargs):
    global server
    global failures_count
    response = goagent.gae_urlfetch(*args, **kwargs)
    if 200 == response.app_status:
        failures_count = 0
    else:
        failures_count += 1
    if failures_count > 5:
        LOGGER.error('goagent failed for more than 5 times, disable service')
        if server:
            try:
                server.shutdown()
            finally:
                server = None
    return response


def monkey_patch_goagent():
    goagent.logging.info = goagent.logging.debug
    goagent.GAEProxyHandler.urlfetch = staticmethod(modified_gae_urlfetch)


monkey_patch_goagent()


def main():
    global server
    logging.basicConfig(level=logging.INFO)
    try:
        goagent.common.GAE_APPIDS = sys.argv[1:]
        if not goagent.common.GAE_APPIDS:
            LOGGER.error("no appids, disable goagent service")
            return
        goagent.common.GAE_FETCHSERVER = '%s://%s.appspot.com%s?' % (
            goagent.common.GOOGLE_MODE, goagent.common.GAE_APPIDS[0], goagent.common.GAE_PATH)
        server = goagent.gevent.server.StreamServer(
            (goagent.common.LISTEN_IP, goagent.common.LISTEN_PORT), goagent.GAEProxyHandler)
        server.serve_forever()
    finally:
        LOGGER.info('goagent server shutdown')
        server = None


if '__main__' == __name__:
    main()
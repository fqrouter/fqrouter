import atexit
import signal
import logging
import os

import gevent


LOGGER = logging.getLogger('fqrouter.%s' % __name__)

shutdown_hooks = []


def add(hook):
    shutdown_hooks.append(hook)


def execute():
    for hook in shutdown_hooks:
        try:
            hook()
        except:
            LOGGER.exception('failed to execute shutdown hook: %s' % hook)
    LOGGER.info('shutdown hook completed')
    os._exit(0)


atexit.register(execute)
gevent.signal(signal.SIGTERM, execute)
gevent.signal(signal.SIGINT, execute)
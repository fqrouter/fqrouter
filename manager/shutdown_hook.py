import os
import atexit
import signal
import logging


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


atexit.register(execute)


def handle_exit_signals(signum, frame):
    try:
        execute()
    finally:
        signal.signal(signum, signal.SIG_DFL)
        os.kill(os.getpid(), signum) # Rethrow signal


signal.signal(signal.SIGTERM, handle_exit_signals)
signal.signal(signal.SIGINT, handle_exit_signals)
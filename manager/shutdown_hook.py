import os
import atexit
import signal
import logging
import subprocess
import shlex


LOGGER = logging.getLogger(__name__)

shutdown_hooks = []


def add(hook):
    shutdown_hooks.append(hook)


def execute():
    try:
        LOGGER.info('before exit, dump iptables filter table')
        LOGGER.info(subprocess.check_output(shlex.split('iptables -L -v -n')))
    except:
        LOGGER.exception('failed to dump iptables filter table')
    try:
        LOGGER.info('before exit, dump iptables nat table')
        LOGGER.info(subprocess.check_output(shlex.split('iptables -t nat -L -v -n')))
    except:
        LOGGER.exception('failed to dump iptables nat table')
    for hook in shutdown_hooks:
        try:
            hook()
        except:
            LOGGER.exception('failed to execute shutdown hook: %s' % hook)


atexit.register(execute)


def handle_exit_signals(signum, frame):
    try:
        execute()
    finally:
        signal.signal(signum, signal.SIG_DFL)
        os.kill(os.getpid(), signum) # Rethrow signal


signal.signal(signal.SIGTERM, handle_exit_signals)
signal.signal(signal.SIGINT, handle_exit_signals)
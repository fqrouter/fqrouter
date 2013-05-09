import subprocess
import logging
import time
import thread
import os

LOGGER = logging.getLogger('fqrouter.%s' % __name__)
PYTHON_PATH = '/data/data/fq.router/python/bin/python'
GOAGENT_LAUNCHER_PATH = os.path.join(os.path.dirname(__file__), 'goagent_launcher.py')

on_goagent_died = None
shutting_down = False
goagent_process = None


def start_goagent(appids):
    global goagent_process
    if is_goagent_live():
        LOGGER.error('another goagent instance is running')
        return
    LOGGER.info('starting goagent with %s' % appids)
    global shutting_down
    shutting_down = False
    goagent_process = subprocess.Popen(
        [PYTHON_PATH, GOAGENT_LAUNCHER_PATH] + appids,
        stderr=subprocess.STDOUT, stdout=subprocess.PIPE)
    time.sleep(0.5)
    if goagent_process.poll() is None:
        LOGGER.info('goagent seems started: %s' % goagent_process.pid)
        thread.start_new(monitor_goagent, ())
    else:
        LOGGER.error('goagent output:')
        LOGGER.error(goagent_process.stdout.read())
        try:
            goagent_process.communicate()
        except:
            pass
        raise Exception('failed to start redsocks')


def monitor_goagent():
    the_process = goagent_process
    try:
        output, _ = the_process.communicate()
        LOGGER.info('goagent output: %s' % output)
        if not shutting_down and the_process.poll():
            LOGGER.error('goagent output: %s' % output)
    except:
        LOGGER.exception('goagent died')
    finally:
        on_goagent_died()


def kill_goagent():
    try:
        if goagent_process:
            LOGGER.info('found existing goagent')
            goagent_process.terminate()
            goagent_process.communicate()
        LOGGER.info('goagent killed')
    except:
        LOGGER.exception('failed to kill goagent')
        time.sleep(2)


def is_goagent_live():
    try:
        if not goagent_process:
            return False
        return goagent_process.poll() is None
    except:
        LOGGER.exception('failed to tell if goagent is live')
        return False
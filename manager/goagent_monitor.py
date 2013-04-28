import subprocess
import logging
import time
import threading
import os
import signal

LOGGER = logging.getLogger('fqrouter.%s' % __name__)
BUSYBOX_PATH = '/data/data/fq.router/busybox'
PYTHON_LAUNCHER_PATH = '/data/data/fq.router/python/bin/python-launcher.sh'
GOAGENT_LAUNCHER_PATH = os.path.join(os.path.dirname(__file__), 'goagent_launcher.py')

on_goagent_died = None
shutting_down = False


def start_goagent(appids):
    LOGGER.info('starting goagent with %s' % appids)
    global shutting_down
    shutting_down = False
    goagent_process = subprocess.Popen(
        [BUSYBOX_PATH, 'sh', PYTHON_LAUNCHER_PATH, GOAGENT_LAUNCHER_PATH] + appids,
        stderr=subprocess.STDOUT, stdout=subprocess.PIPE)
    time.sleep(0.5)
    if goagent_process.poll() is None:
        LOGGER.info('goagent seems started: %s' % goagent_process.pid)
        t = threading.Thread(target=monitor_goagent, args=(goagent_process,))
        t.daemon = True
        t.start()
    else:
        LOGGER.error('goagent output:')
        LOGGER.error(goagent_process.stdout.read())
        try:
            goagent_process.communicate()
        except:
            pass
        raise Exception('failed to start redsocks')


def monitor_goagent(goagent_process):
    try:
        output, _ = goagent_process.communicate()
        LOGGER.info('goagent output: %s' % output)
        if not shutting_down and goagent_process.poll():
            LOGGER.error('goagent output: %s' % output)
    except:
        LOGGER.exception('goagent died')
    finally:
        on_goagent_died()


def kill_goagent():
    found = False
    for i in range(10):
        if kill_goagent_once():
            found = True
            time.sleep(1)
        else:
            return found
    kill_goagent_once(force=True)
    return found


def kill_goagent_once(force=False):
    global shutting_down
    shutting_down = True
    found = False
    try:
        for pid in os.listdir('/proc'):
            cmdline_path = os.path.join('/proc', pid, 'cmdline')
            if not os.path.exists(cmdline_path):
                continue
            with open(cmdline_path) as f:
                if 'goagent_launcher.py' in f.read():
                    found = True
                    try:
                        LOGGER.info('kill exiting goagent: %s' % pid)
                        os.kill(int(pid), signal.SIGKILL if force else signal.SIGTERM)
                    except:
                        pass
    except:
        LOGGER.exception('failed to kill goagent')
    return found

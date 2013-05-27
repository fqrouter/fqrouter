import logging
from gevent import subprocess
import gevent

LOGGER = logging.getLogger('fqrouter.%s' % __name__)

PYTHON_PATH = '/data/data/fq.router/python/bin/python'


def launch_python(name, *args):
    proc = subprocess.Popen(
        [PYTHON_PATH, '-m', name] + list(args),
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    gevent.sleep(0.5)
    if proc.poll() is not None:
        try:
            output, _ = proc.communicate()
            LOGGER.error('%s exit output: %s' % (name, output))
        except:
            LOGGER.exception('failed to log %s exit output' % name)
        raise Exception('failed to start %s' % name)
    LOGGER.info('%s started: %s' % (name, proc.pid))
    gevent.spawn(monitor_process, name, proc)
    return proc


def monitor_process(name, proc):
    try:
        output, _ = proc.communicate()
        if proc.poll():
            LOGGER.error('%s output: %s' % (name, output[-200:]))
    except:
        LOGGER.exception('%s died' % name)
    finally:
        LOGGER.info('%s exited' % name)
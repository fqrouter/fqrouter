import logging
from gevent import subprocess
import gevent

LOGGER = logging.getLogger('fqrouter.%s' % __name__)

PYTHON_PATH = '/data/data/fq.router2/python/bin/python'


def launch_python(name, args, on_exit=None):
    command = [PYTHON_PATH, '-m', name] + list(args)
    LOGGER.info('launch python: %s' % ' '.join(command))
    proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    gevent.sleep(0.5)
    if proc.poll() is not None:
        try:
            output, _ = proc.communicate()
            LOGGER.error('%s exit output: %s' % (name, output))
        except:
            LOGGER.exception('failed to log %s exit output' % name)
        raise Exception('failed to start %s' % name)
    LOGGER.info('%s started: %s' % (name, proc.pid))
    gevent.spawn(monitor_process, name, proc, on_exit)
    return proc


def monitor_process(name, proc, on_exit):
    try:
        output, _ = proc.communicate()
        if proc.poll():
            LOGGER.error('%s output: %s' % (name, output[-1000:]))
    except:
        LOGGER.exception('%s died' % name)
    finally:
        LOGGER.info('%s exited' % name)
        if on_exit:
            try:
                on_exit()
            except:
                LOGGER.exception('failed to execute on_exit hook for %s' % name)
import logging
from gevent import subprocess
import gevent
import os
import functools
import os

LOGGER = logging.getLogger('fqrouter.%s' % __name__)

BUSYBOX_PATH = '/data/data/fq.router2/busybox'
PYTHON_PATH = '/data/data/fq.router2/python/bin/python'
PYTHON_HOME = '/data/data/fq.router2/python'
USE_SU = False


def launch_python(name, args, on_exit=None):
    command = [PYTHON_PATH, '-m', name] + list(args)
    LOGGER.info('launch python: %s' % ' '.join(command))
    env = os.environ.copy()
    env['PYTHONHOME'] = PYTHON_HOME
    if USE_SU:
        proc = subprocess.Popen('su', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, stdin=subprocess.PIPE, env=env)
        proc.terminate = functools.partial(sudo_kill, proc.pid)
        proc.stdin.write('PYTHONHOME=%s ' % PYTHON_HOME)
        proc.stdin.write(' '.join(command))
        proc.stdin.write('\nexit\n')
    else:
        proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, env=env)
    gevent.sleep(0.5)
    retcode = proc.poll()
    if retcode:
        try:
            output, _ = proc.communicate()
            LOGGER.error('%s exit %s output: %s' % (name, retcode, output))
        except:
            LOGGER.exception('failed to log %s exit output' % name)
        raise Exception('failed to start %s' % name)
    LOGGER.info('%s started: %s' % (name, proc.pid))
    if USE_SU:
        proc.terminate = functools.partial(sudo_kill, name)
    gevent.spawn(monitor_process, name, proc, on_exit)
    return proc

def find_real_pid(name):
    for pid in os.listdir('/proc'):
        cmdline_file = os.path.join('/proc', pid, 'cmdline')
        if os.path.exists(cmdline_file):
            with open(cmdline_file) as f:
                cmdline = f.read()
                if name in cmdline:
                    return pid
    return None

def sudo_kill(name):
    pid = find_real_pid(name)
    if pid:
        LOGGER.info('kill %s' % pid)
        proc = subprocess.Popen('su', stdin=subprocess.PIPE)
        proc.stdin.write('exec /data/data/fq.router2/busybox kill %s\n' % pid)
        proc.communicate()
    else:
        LOGGER.info('%s not found in /proc' % name)

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


def call(args):
    if USE_SU:
        proc = subprocess.Popen('su', stdin=subprocess.PIPE)
        proc.stdin.write(' '.join(args))
        proc.stdin.write('\nexit\n')
        proc.communicate()
        return proc.poll()
    else:
        return subprocess.call(args)


def check_call(args):
    if USE_SU:
        proc = subprocess.Popen('su', stdin=subprocess.PIPE)
        proc.stdin.write(' '.join(args))
        proc.stdin.write('\nexit\n')
        proc.communicate()
        retcode = proc.poll()
        if retcode:
            raise subprocess.CalledProcessError(retcode, args)
        return 0
    else:
        return subprocess.check_call(args)


def check_output(args):
    if USE_SU:
        proc = subprocess.Popen('su', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, stdin=subprocess.PIPE)
        proc.stdin.write(' '.join(args))
        proc.stdin.write('\nexit\n')
        output = proc.communicate()[0]
        retcode = proc.poll()
        if retcode:
            raise subprocess.CalledProcessError(retcode, args, output=output)
        return output
    else:
        return subprocess.check_output(args, stderr=subprocess.STDOUT)


def Popen(args, **kwargs):
    if USE_SU:
        proc = subprocess.Popen('su', stdin=subprocess.PIPE, **kwargs)
        proc.stdin.write('PYTHONHOME=%s ' % PYTHON_HOME)
        proc.stdin.write(' '.join(args))
        proc.stdin.write('\nexit\n')
        return proc
    else:
        return subprocess.Popen(args, **kwargs)

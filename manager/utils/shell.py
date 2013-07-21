import logging
from gevent import subprocess
import gevent
import os

LOGGER = logging.getLogger('fqrouter.%s' % __name__)

PYTHON_PATH = '/data/data/fq.router2/python/bin/python'
IS_ROOT = 0 == os.getuid()
USE_SU = False


def launch_python(name, args, on_exit=None):
    command = [PYTHON_PATH, '-m', name] + list(args)
    LOGGER.info('launch python: %s' % ' '.join(command))
    try:
        if IS_ROOT and USE_SU:
            proc = subprocess.Popen('su', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, stdin=subprocess.PIPE)
            proc.stdin.write(' '.join(command))
            proc.stdin.write('\n')
            proc.stdin.write('exit\n')
        else:
            proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    except OSError:
        LOGGER.exception('failed to start python in normal mode, try su')
        proc = subprocess.Popen('su', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, stdin=subprocess.PIPE)
        proc.stdin.write(' '.join(command))
        proc.stdin.write('\n')
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


def call(args):
    global USE_SU
    if not USE_SU:
        try:
            return subprocess.call(args)
        except OSError:
            LOGGER.exception('failed to start subprocess in normal mode, try su')
            if not IS_ROOT:
                raise
    proc = subprocess.Popen('su', stdin=subprocess.PIPE)
    proc.stdin.write(' '.join(args))
    proc.stdin.write('\n')
    proc.stdin.write('exit\n')
    proc.communicate()
    USE_SU = True
    return proc.poll()


def check_call(args):
    global USE_SU
    if not USE_SU:
        try:
            return subprocess.check_call(args)
        except OSError:
            LOGGER.exception('failed to start subprocess in normal mode, try su')
            if not IS_ROOT:
                raise
    proc = subprocess.Popen('su', stdin=subprocess.PIPE)
    proc.stdin.write(' '.join(args))
    proc.stdin.write('\n')
    proc.stdin.write('exit\n')
    proc.communicate()
    USE_SU = True
    if proc.poll():
        raise subprocess.CalledProcessError(proc.poll(), args)
    return 0


def check_output(args):
    global USE_SU
    if not USE_SU:
        try:
            return subprocess.check_output(args)
        except OSError:
            LOGGER.exception('failed to start subprocess in normal mode, try su')
            if not IS_ROOT:
                raise
    proc = subprocess.Popen('su', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, stdin=subprocess.PIPE)
    LOGGER.info(' '.join(args))
    proc.stdin.write(' '.join(args))
    proc.stdin.write('\n')
    proc.stdin.write('exit\n')
    output = proc.communicate()[0]
    USE_SU = True
    retcode = proc.poll()
    if retcode:
        raise subprocess.CalledProcessError(retcode, args, output=output)
    return output

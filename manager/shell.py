import logging
import os
import subprocess
import shlex

LOGGER = logging.getLogger('fqrouter.%s' % __name__)

PYTHON_PATH = '/data/data/fq.router/python/bin/python'
MAIN_PY_PATH = os.path.join(os.path.dirname(__file__), 'main.py')


def fqrouter_execute(action, *args):
    args = [PYTHON_PATH, MAIN_PY_PATH, action] + list(args)
    LOGGER.info('executing: %s' % str(args))
    start_hotspot_process = subprocess.Popen(
        args, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
    _, return_value = start_hotspot_process.communicate()
    LOGGER.info('return value: %s' % return_value)
    return eval(return_value)


def execute(command):
    LOGGER.info('execute: %s' % command)
    try:
        output = subprocess.check_output(shlex.split(command), stderr=subprocess.STDOUT)
        LOGGER.info('succeed, output: %s' % output)
    except subprocess.CalledProcessError, e:
        LOGGER.error('failed, output: %s' % e.output)
        raise
    return output
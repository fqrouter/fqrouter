import logging
import httplib
import json

from gevent import subprocess

from utils import shell


LOGGER = logging.getLogger('fqrouter.%s' % __name__)
VER_DOMAIN = 'beta.android.ver.fqrouter.com'


def start():
    return [
        ('GET', 'version/latest', handle_latest)
    ]


def stop():
    pass


def is_alive():
    return True


def handle_latest(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield str(resolve_latest_version())


def resolve_latest_version():
    try:
        answers = resolve('TXT', [VER_DOMAIN])
        answer = answers.get(VER_DOMAIN)
        answer = answer[0] if answer else ''
        LOGGER.info('resolved latest version %s => %s' % (VER_DOMAIN, answer))
        return answer.strip() or ''
    except:
        LOGGER.exception('failed to resolve latest version %s' % VER_DOMAIN)
        return ''


def resolve(record_type, domain_names):
    try:
        args = [shell.PYTHON_PATH, '-m', 'fqdns', 'resolve',
                '--retry', '3', '--timeout', '2',
                '--record-type', record_type] + domain_names
        LOGGER.info('executing: %s' % str(args))
        proc = subprocess.Popen(args, stderr=subprocess.PIPE)
        _, output = proc.communicate()
        LOGGER.info('resolved: %s' % output)
        return json.loads(output)
    except:
        LOGGER.exception('failed to resolve: %s' % domain_names)
        return {}
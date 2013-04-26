import subprocess
import time
import logging
import logging.handlers
import threading
import re
import os

import redsocks_template


LOGGER = logging.getLogger('fqrouter.%s' % __name__)

ROOT_DIR = os.path.dirname(__file__)
LOG_DIR = '/data/data/fq.router'
REDSOCKS_LOG_FILE = os.path.join(LOG_DIR, 'redsocks.log')
REDSOCKS_LOGGER = logging.getLogger('redsocks')
handler = logging.handlers.RotatingFileHandler(
    REDSOCKS_LOG_FILE, maxBytes=1024 * 1024, backupCount=1)
handler.setFormatter(logging.Formatter('%(asctime)s %(message)s'))
REDSOCKS_LOGGER.handlers = [handler]

RE_IP_PORT = r'(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)'
RE_REDSOCKS_CLIENT = re.compile(RE_IP_PORT + '->')

# call back from full_proxy_service
list_proxies = None
handle_proxy_error = None
clear_proxies = None
update_proxy = None

# internal
redsocks_process = None
status_updated_at = None


def start_redsocks(proxies):
    global redsocks_process
    cfg_path = '/data/data/fq.router/redsocks.conf'
    with open(cfg_path, 'w') as f:
        f.write(redsocks_template.render(proxies))
    redsocks_process = subprocess.Popen(
        ['/data/data/fq.router/proxy-tools/redsocks', '-c', cfg_path],
        stderr=subprocess.STDOUT, stdout=subprocess.PIPE, bufsize=1, close_fds=True)
    time.sleep(2)
    if redsocks_process.poll() is None:
        LOGGER.info('redsocks seems started: %s' % redsocks_process.pid)
        t = threading.Thread(target=monitor_redsocks)
        t.daemon = True
        t.start()
    else:
        LOGGER.error('redsocks output:')
        LOGGER.error(redsocks_process.stdout.read())
        raise Exception('failed to start redsocks')


def monitor_redsocks():
    try:
        while is_redsocks_live():
            for line in iter(redsocks_process.stdout.readline, b''):
                REDSOCKS_LOGGER.info(line.strip())
                if 'HTTP/' in line or 'No route to host' in line:
                    match = RE_REDSOCKS_CLIENT.search(line)
                    if match:
                        ip = match.group(1)
                        port = int(match.group(2))
                        for local_port, proxy in list_proxies():
                            if (ip, port) in proxy['clients']:
                                LOGGER.error(line.strip())
                                handle_proxy_error(local_port, proxy)
                update_proxies_status_every_five_minutes()
            time.sleep(1)
        LOGGER.error('redsocks died, clear proxies: %s' % redsocks_process.poll())
        redsocks_process.communicate()
        clear_proxies()
    except:
        LOGGER.exception('failed to poll redsocks output')
        clear_proxies()


def update_proxies_status_every_five_minutes():
    global status_updated_at
    if status_updated_at is None:
        status_updated_at = time.time()
    elif (time.time() - status_updated_at) > 60 * 5:
        for local_port, proxy in list_proxies():
            rank = int(proxy['pre_rank'] / 2) # factor in the previous performance
            LOGGER.info('update proxy %s rank: %s %s' %
                        (local_port, rank, str(proxy['connection_info'])))
            update_proxy(local_port, rank=rank, pre_rank=rank, clients=set())
        status_updated_at = time.time()


def kill_redsocks():
    return not subprocess.call(['/data/data/fq.router/busybox', 'killall', 'redsocks'])


def is_redsocks_live():
    if not redsocks_process:
        return False
    return redsocks_process.poll() is None

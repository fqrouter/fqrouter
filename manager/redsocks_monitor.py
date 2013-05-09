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

REFRESH_INTERVAL = 30
UPDATE_INTERVAL = 60 * 5

RE_IP_PORT = r'(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)'
RE_REDSOCKS_CLIENT = re.compile(RE_IP_PORT + '->')

# call back from full_proxy_service
list_proxies = None
handle_proxy_error = None
update_proxy = None
refresh_proxies = None

# internal
redsocks_process = None
updated_at = None
refreshed_at = None


def start_redsocks(proxies):
    for i in range(3):
        try:
            start_redsocks_once(proxies)
            return True
        except:
            LOGGER.exception('failed to start redsocks, retry')
            kill_redsocks()
    LOGGER.error('retry starting redsocks too many times, give up')
    return False


def start_redsocks_once(proxies):
    global redsocks_process
    if is_redsocks_live():
        LOGGER.error('another redsocks instace is running')
        return
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
    the_process = redsocks_process
    try:
        while is_redsocks_live():
            for line in iter(the_process.stdout.readline, b''):
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
                update_proxies_according_to_schedule()
                refresh_proxies_according_to_schedule()
            time.sleep(1)
        LOGGER.error('redsocks died, clear proxies: %s' % the_process.poll())
        the_process.communicate()
        refresh_proxies()
    except:
        LOGGER.exception('failed to poll redsocks output')
        refresh_proxies()


def update_proxies_according_to_schedule():
    global updated_at
    if updated_at is None:
        updated_at = time.time()
    elif (time.time() - updated_at) > UPDATE_INTERVAL:
        updated_at = time.time()
        LOGGER.info('update proxies every %s seconds' % UPDATE_INTERVAL)
        for local_port, proxy in list_proxies():
            rank = int(proxy['pre_rank'] / 2) # factor in the previous performance
            LOGGER.info('update proxy %s rank: %s %s' %
                        (local_port, rank, str(proxy['connection_info'])))
            update_proxy(local_port, rank=rank, pre_rank=rank, clients=set())


def refresh_proxies_according_to_schedule():
    global refreshed_at
    if refreshed_at is None:
        refreshed_at = time.time()
    elif (time.time() - refreshed_at) > REFRESH_INTERVAL:
        refreshed_at = time.time()
        LOGGER.info('refresh proxies every %s seconds' % REFRESH_INTERVAL)
        time.sleep(5)
        kill_redsocks()


def kill_redsocks():
    try:
        if redsocks_process:
            LOGGER.info('found existing redsocks')
            redsocks_process.terminate()
            redsocks_process.communicate()
        LOGGER.info('redsocks killed')
    except:
        LOGGER.exception('failed to kill redsocks')
        time.sleep(2)


def is_redsocks_live():
    try:
        if not redsocks_process:
            return False
        return redsocks_process.poll() is None
    except:
        LOGGER.exception('failed to tell if redsocks is live')
        return False

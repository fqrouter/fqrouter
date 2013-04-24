import subprocess
import time
import logging
import threading
import re
import os
import signal

import redsocks_template


LOGGER = logging.getLogger(__name__)
RE_IP_PORT = r'(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)'
RE_REDSOCKS_CLIENT = re.compile(RE_IP_PORT + '->')
RE_REDSOCKS_INSTANCE = re.compile(r'Dumping client list for instance ' + RE_IP_PORT)

# call back from full_proxy_service
list_proxies = None
handle_proxy_error = None
clear_proxies = None
update_proxy = None

# internal
redsocks_process = None
redsocks_dumped_at = None


def start_redsocks(proxies):
    global redsocks_process
    cfg_path = '/data/data/fq.router/redsocks.conf'
    with open(cfg_path, 'w') as f:
        f.write(redsocks_template.render(proxies.values()))
    redsocks_process = subprocess.Popen(
        ['/data/data/fq.router/proxy-tools/redsocks', '-c', cfg_path],
        stderr=subprocess.STDOUT, stdout=subprocess.PIPE, bufsize=1, close_fds=True)
    time.sleep(0.5)
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
        current_instance = None
        current_clients = set()
        while is_redsocks_live():
            for line in iter(redsocks_process.stdout.readline, b''):
                match = RE_REDSOCKS_INSTANCE.search(line)
                if match:
                    current_instance = int(match.group(2))
                    current_clients = set()
                    LOGGER.debug('dump redsocks instance %s' % current_instance)
                if current_instance:
                    match = RE_REDSOCKS_CLIENT.search(line)
                    if match:
                        ip = match.group(1)
                        port = int(match.group(2))
                        current_clients.add((ip, port))
                        LOGGER.debug('client %s:%s' % (ip, port))
                else:
                    if 'http-connect.c:149' in line:
                        match = RE_REDSOCKS_CLIENT.search(line)
                        if match:
                            ip = match.group(1)
                            port = int(match.group(2))
                            for mark, proxy in list_proxies():
                                if (ip, port) in proxy['clients']:
                                    LOGGER.error(line.strip())
                                    handle_proxy_error(mark, proxy)
                if 'End of client list' in line:
                    update_proxy_status(current_instance, current_clients)
                    current_instance = None
                dump_redsocks_client_list()
        LOGGER.error('redsocks died, clear proxies')
        redsocks_process.stdout.close()
        clear_proxies()
    except:
        LOGGER.exception('failed to poll redsocks output')
        clear_proxies()


def update_proxy_status(current_instance, current_clients):
    for mark, proxy in list_proxies():
        if proxy['local_port'] == current_instance:
            hangover_penalty = int(proxy['pre_rank'] / 2)
            rank = len(current_clients) + hangover_penalty # factor in the previous performance
            LOGGER.info('update proxy 0x%x rank: [%s+%s] %s' %
                        (mark, proxy['pre_rank'], hangover_penalty, str(proxy['connection_info'])))
            update_proxy(mark, rank=rank, pre_rank=rank, clients=current_clients)
            return
    LOGGER.debug('this redsocks instance has been removed from proxy list')


def dump_redsocks_client_list(should_dump=False):
    global redsocks_dumped_at
    if redsocks_dumped_at is None:
        redsocks_dumped_at = time.time()
    elif time.time() - redsocks_dumped_at > 60:
        should_dump = True
    if should_dump:
        LOGGER.info('dump redsocks client list')
        os.kill(redsocks_process.pid, signal.SIGUSR1)
        redsocks_dumped_at = time.time()


def kill_redsocks():
    return not subprocess.call(['/data/data/fq.router/busybox', 'killall', 'redsocks'])


def is_redsocks_live():
    return not subprocess.call(['/data/data/fq.router/busybox', 'killall', '-0', 'redsocks'])

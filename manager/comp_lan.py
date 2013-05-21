import logging
import httplib
import json

from gevent import subprocess
import gevent

import comp_wifi
from utils import shell


LOGGER = logging.getLogger('fqrouter.%s' % __name__)

picked_devices = {}
fqlan_process = None


def start():
    return [
        ('GET', 'lan/scan', handle_scan),
        ('POST', 'lan/forge-default-gateway', handle_forge_default_gateway),
        ('POST', 'lan/restore-default-gateway', handle_restore_default_gateway)
    ]


def stop():
    global fqlan_process
    try:
        if fqlan_process:
            LOGGER.info('terminate fqlan: %s' % fqlan_process.pid)
            fqlan_process.terminate()
    except:
        LOGGER.exception('failed to terminate fqlan')
    fqlan_process = None


def is_alive():
    if fqlan_process:
        return fqlan_process.poll() is None
    return False


def handle_forge_default_gateway(environ, start_response):
    ip = environ['REQUEST_ARGUMENTS']['ip'].value
    mac = environ['REQUEST_ARGUMENTS']['mac'].value
    picked_devices[ip] = mac
    restart_fqlan()
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return []


def handle_restore_default_gateway(environ, start_response):
    ip = environ['REQUEST_ARGUMENTS']['ip'].value
    if ip in picked_devices:
        del picked_devices[ip]
    restart_fqlan()
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return [str(len(picked_devices))]


def handle_scan(environ, start_response):
    try:
        scan_process = subprocess.Popen(
            [shell.PYTHON_PATH, '-m', 'fqlan',
             '--log-level', 'INFO',
             '--log-file', '/data/data/fq.router/scan.log',
             '--lan-interface', comp_wifi.WIFI_INTERFACE,
             '--ifconfig-command', '/data/data/fq.router/busybox',
             '--ip-command', '/data/data/fq.router/busybox',
             'scan', '--hostname', '--mark', '0xcafe'],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        _, output = scan_process.communicate()
    except:
        LOGGER.exception('failed to scan')
        start_response(httplib.INTERNAL_SERVER_ERROR, [('Content-Type', 'text/plain')])
        return
    try:
        start_response(httplib.OK, [('Content-Type', 'text/plain')])
        for line in output.splitlines():
            ip, mac, hostname = json.loads(line)
            yield str('%s,%s,%s,%s\n' % (ip, mac, hostname, 'TRUE' if ip in picked_devices else 'FALSE'))
    except:
        LOGGER.exception('failed to return scan results')


def restart_fqlan():
    global fqlan_process
    stop()
    if not picked_devices:
        LOGGER.info('no picked devices, fqlan will not start')
        return
    fqlan_process = subprocess.Popen(
        [shell.PYTHON_PATH, '-m', 'fqlan',
         '--log-level', 'INFO',
         '--log-file', '/data/data/fq.router/fqlan.log',
         '--lan-interface', comp_wifi.WIFI_INTERFACE,
         '--ifconfig-command', '/data/data/fq.router/busybox',
         '--ip-command', '/data/data/fq.router/busybox',
         'forge'] + ['%s,%s' % (ip, mac) for ip, mac in picked_devices.items()],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    gevent.sleep(1)
    if fqlan_process.poll() is not None:
        try:
            output, _ = fqlan_process.communicate()
            LOGGER.error('fqlan exit output: %s' % output)
        except:
            LOGGER.exception('failed to log fqlan exit output')
        raise Exception('failed to start fqlan')
    LOGGER.info('fqlan started: %s' % fqlan_process.pid)
    gevent.spawn(monitor_fqlan)


def monitor_fqlan():
    try:
        output, _ = fqlan_process.communicate()
        if fqlan_process.poll():
            LOGGER.error('fqdns output: %s' % output[-200:])
    except:
        LOGGER.exception('fqdns died')
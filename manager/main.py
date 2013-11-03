import gevent.monkey

gevent.monkey.patch_all(ssl=False, thread=False)

import logging
import logging.handlers
import sys
import os
import config
import traceback
import httplib
import fqsocks.httpd
import fqsocks.fqsocks
import wifi
import shell
import iptables
import shutdown_hook
import shlex
import subprocess
import functools
import comp_scrambler
import comp_shortcut
import fqsocks.pages.downstream
import fqsocks.config_file
import fqdns

FQROUTER_VERSION = 'UNKNOWN'
LOGGER = logging.getLogger('fqrouter.%s' % __name__)
LOG_DIR = '/data/data/fq.router2/log'
MANAGER_LOG_FILE = os.path.join(LOG_DIR, 'manager.log')
WIFI_LOG_FILE = os.path.join(LOG_DIR, 'wifi.log')
FQDNS_LOG_FILE = os.path.join(LOG_DIR, 'fqdns.log')
FQLAN_LOG_FILE = os.path.join(LOG_DIR, 'fqlan.log')
DNS_RULES = [
    (
        {'target': 'ACCEPT', 'extra': 'udp dpt:53 mark match 0xcafe', 'optional': True},
        ('nat', 'OUTPUT', '-p udp --dport 53 -m mark --mark 0xcafe -j ACCEPT')
    ), (
        {'target': 'DNAT', 'extra': 'udp dpt:53 to:10.1.2.3:12345'},
        ('nat', 'OUTPUT', '-p udp ! -s 10.1.2.3 --dport 53 -j DNAT --to-destination 10.1.2.3:12345')
    ), (
        {'target': 'DNAT', 'extra': 'udp dpt:53 to:10.1.2.3:12345'},
        ('nat', 'PREROUTING', '-p udp ! -s 10.1.2.3 --dport 53 -j DNAT --to-destination 10.1.2.3:12345')
    )]
SOCKS_RULES = [
    (
        {'target': 'DROP', 'extra': 'icmp type 5'},
        ('filter', 'OUTPUT', '-p icmp --icmp-type 5 -j DROP')
    ), (
        {'target': 'ACCEPT', 'destination': '127.0.0.1'},
        ('nat', 'OUTPUT', '-p tcp -d 127.0.0.1 -j ACCEPT')
    ), (
        {'target': 'DNAT', 'extra': 'to:10.1.2.3:12345'},
        ('nat', 'OUTPUT', '-p tcp ! -s 10.1.2.3 -j DNAT --to-destination 10.1.2.3:12345')
    ), (
        {'target': 'DNAT', 'extra': 'to:10.1.2.3:12345'},
        ('nat', 'PREROUTING', '-p tcp ! -s 10.1.2.3 -j DNAT --to-destination 10.1.2.3:12345')
    )]
default_dns_server = config.get_default_dns_server()
DNS_HANDLER = fqdns.DnsHandler(
    enable_china_domain=True, enable_hosted_domain=True,
    original_upstream=('udp', default_dns_server, 53) if default_dns_server else None)
fqsocks.fqsocks.DNS_HANDLER = DNS_HANDLER


def handle_ping(environ, start_response):
    try:
        LOGGER.info('PONG/%s' % FQROUTER_VERSION)
    except:
        traceback.print_exc()
        os._exit(1)
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield 'PONG/%s' % FQROUTER_VERSION


fqsocks.httpd.HANDLERS[('GET', 'ping')] = handle_ping


def setup_logging():
    logging.basicConfig(stream=sys.stdout, level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
    handler = logging.handlers.RotatingFileHandler(
        MANAGER_LOG_FILE, maxBytes=1024 * 256, backupCount=0)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('fqrouter').addHandler(handler)
    handler = logging.handlers.RotatingFileHandler(
        FQDNS_LOG_FILE, maxBytes=1024 * 256, backupCount=0)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('fqdns').addHandler(handler)
    handler = logging.handlers.RotatingFileHandler(
        FQLAN_LOG_FILE, maxBytes=1024 * 256, backupCount=0)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('fqlan').addHandler(handler)
    handler = logging.handlers.RotatingFileHandler(
        WIFI_LOG_FILE, maxBytes=1024 * 512, backupCount=1)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
    logging.getLogger('wifi').addHandler(handler)


def needs_su():
    if os.getuid() == 0:
        return False
    else:
        return True


def run():
    iptables.init_fq_chains()
    shutdown_hook.add(iptables.flush_fq_chain)
    iptables.insert_rules(DNS_RULES)
    shutdown_hook.add(functools.partial(iptables.delete_rules, DNS_RULES))
    iptables.insert_rules(SOCKS_RULES)
    shutdown_hook.add(functools.partial(iptables.delete_rules, SOCKS_RULES))
    wifi.setup_lo_alias()
    args = [
        '--log-level', 'INFO',
        '--log-file', '/data/data/fq.router2/log/fqsocks.log',
        '--ifconfig-command', '/data/data/fq.router2/busybox',
        '--ip-command', '/data/data/fq.router2/busybox',
        '--outbound-ip', '10.1.2.3',
        '--tcp-gateway-listen', '10.1.2.3:12345',
        '--dns-server-listen', '10.1.2.3:12345']
    if shell.USE_SU:
        args.append('--no-tcp-scrambler')
    args = config.configure_fqsocks(args)
    fqsocks.fqsocks.init_config(args)
    if fqsocks.config_file.read_config()['tcp_scrambler_enabled']:
        try:
            comp_scrambler.start()
            shutdown_hook.add(comp_scrambler.stop)
        except:
            LOGGER.exception('failed to start comp_scrambler')
            comp_scrambler.stop()
    if fqsocks.config_file.read_config()['china_shortcut_enabled']:
        try:
            comp_shortcut.start()
            shutdown_hook.add(comp_shortcut.stop)
        except:
            LOGGER.exception('failed to start comp_shortcut')
            comp_shortcut.stop()
    fqsocks.fqsocks.main()


def clean():
    LOGGER.info('clean...')
    try:
        iptables.flush_fq_chain()
        try:
            LOGGER.info('iptables -L -v -n')
            LOGGER.info(shell.check_output(shlex.split('iptables -L -v -n')))
        except subprocess.CalledProcessError, e:
            LOGGER.error('failed to dump filter table: %s' % (sys.exc_info()[1]))
            LOGGER.error(e.output)
        try:
            LOGGER.info('iptables -t nat -L -v -n')
            LOGGER.info(shell.check_output(shlex.split('iptables -t nat -L -v -n')))
        except subprocess.CalledProcessError, e:
            LOGGER.error('failed to dump nat table: %s' % (sys.exc_info()[1]))
            LOGGER.error(e.output)
    except:
        LOGGER.exception('clean failed')


def wifi_reset():
    wifi.enable_wifi_p2p_service()
    wifi.restore_config_files()
    wifi.stop_hotspot()

_is_wifi_repeater_supported = None


def check_wifi_repeater_supported():
    try:
        api_version = wifi.shell_execute('getprop ro.build.version.sdk').strip()
        if api_version:
            return int(api_version) >= 14
        else:
            return True
    except:
        LOGGER.exception('failed to get api version')
        return True


def is_wifi_repeater_supported():
    global _is_wifi_repeater_supported
    if _is_wifi_repeater_supported is None:
        _is_wifi_repeater_supported = check_wifi_repeater_supported()
    return _is_wifi_repeater_supported


def is_wifi_repeater_started():
    if wifi.has_started_before:
        return wifi.get_working_hotspot_iface()
    return False


fqsocks.pages.downstream.spi_wifi_repeater = {
    'is_supported': is_wifi_repeater_supported,
    'is_started': is_wifi_repeater_started,
    'start': wifi.start_hotspot,
    'stop': wifi.stop_hotspot,
    'reset': wifi_reset
}

if '__main__' == __name__:
    setup_logging()
    LOGGER.info('environment: %s' % os.environ.items())
    LOGGER.info('default dns server: %s' % default_dns_server)
    FQROUTER_VERSION = os.getenv('FQROUTER_VERSION')
    action = sys.argv[1]
    if 'clean' == action:
        shell.USE_SU = needs_su()
        clean()
    elif 'run' == action:
        shell.USE_SU = needs_su()
        run()
    elif 'netd-execute' == action:
        wifi.netd_execute(sys.argv[2])
    else:
        raise Exception('unknown action: %s' % action)
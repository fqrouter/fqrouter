import logging

from utils import shell
from utils import iptables
from utils import config

__MANDATORY__ = True
LOGGER = logging.getLogger('fqrouter.%s' % __name__)
fqsocks_process = None


def start():
    if not is_alive():
        insert_iptables_rules()
        start_fqsocks()


def stop():
    delete_iptables_rules()
    try:
        if fqsocks_process:
            LOGGER.info('terminate fqsocks: %s' % fqsocks_process.pid)
            fqsocks_process.terminate()
    except:
        LOGGER.exception('failed to terminate fqsocks')


def is_alive():
    if fqsocks_process:
        return fqsocks_process.poll() is None
    return False


RULES = [
    (
        {'target': 'ACCEPT', 'destination': '127.0.0.1'},
        ('nat', 'OUTPUT', '-p tcp -d 127.0.0.1 -j ACCEPT')
    ), (
        {'target': 'DNAT', 'extra': 'to:10.1.2.3:8319'},
        ('nat', 'OUTPUT', '-p tcp ! -s 10.1.2.3 -j DNAT --to-destination 10.1.2.3:8319')
    ), (
        {'target': 'DNAT', 'extra': 'to:10.1.2.3:8319'},
        ('nat', 'PREROUTING', '-p tcp ! -s 10.1.2.3 -j DNAT --to-destination 10.1.2.3:8319')
    )]


def insert_iptables_rules():
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)


def start_fqsocks():
    global fqsocks_process
    args = [
        '--log-level', 'INFO',
        '--log-file', '/data/data/fq.router2/log/fqsocks.log',
        '--outbound-ip', '10.1.2.3', # send from 10.1.2.3 so we can skip redirecting those traffic
        '--listen', '10.1.2.3:8319',
        '--http-request-mark', '0xbabe' # trigger scrambler
    ]
    args = configure(args)
    fqsocks_process = shell.launch_python('fqsocks', args, on_exit=stop)


def configure(args):
    args += ['--google-host', 'goagent-google-ip.fqrouter.com']
    if not config.read().get('auto_access_check_enabled', True):
        args += ['--disable-access-check']
    if not config.read().get('china_shortcut_enabled', True):
        args += ['--disable-china-shortcut']
    if not config.read().get('direct_access_enabled', True):
        args += ['--disable-direct-access']
    if config.read().get('youtube_scrambler_enabled', True):
        args += ['--enable-youtube-scrambler']
    if config.read().get('goagent_public_servers_enabled', True):
        args += ['--proxy', 'dynamic,n=10,type=goagent,dns_record=goagent#n#.fqrouter.com,priority=1']
    for server in config.list_goagent_private_servers():
        proxy_config = 'goagent,appid=%s,path=%s,password=%s' % (server['appid'], server['path'], server['password'])
        args += ['--proxy', proxy_config]
    if config.read().get('shadowsocks_public_servers_enabled', True):
        args += ['--proxy', 'dynamic,n=7,type=ss,dns_record=ss#n#.fqrouter.com,priority=2']
    for server in config.list_shadowsocks_private_servers():
        proxy_config = 'ss,proxy_ip=%s,proxy_port=%s,password=%s,encrypt_method=%s' % (
            server['host'], server['port'], server['password'], server['encryption_method'])
        args += ['--proxy', proxy_config]
    if config.read().get('http_proxy_public_servers_enabled', True):
        args += ['--proxy', 'dynamic,n=20,dns_record=proxy#n#.fqrouter.com,is_public=True,priority=4']
        args += ['--proxy', 'dynamic,n=5,dns_record=proxy2#n#.fqrouter.com,priority=2']
    for server in config.list_http_proxy_private_servers():
        if 'spdy (webvpn)' == server['transport_type']:
            proxy_config = 'proxy_ip=%s,proxy_port=%s,username=%s,password=%s,requested_spdy_version=%s' % \
                           (server['host'], server['port'], server['username'], server['password'],
                            server['spdy_version'])
            for i in range(server['spdy_connections_count']):
                if 'http only' == server['traffic_type']:
                    args += ['--proxy', 'spdy-relay,%s' % proxy_config]
                elif 'https only' == server['traffic_type']:
                    args += ['--proxy', 'spdy-connect,%s' % proxy_config]
                else:
                    args += ['--proxy', 'spdy-relay,%s' % proxy_config]
                    args += ['--proxy', 'spdy-connect,%s' % proxy_config]
        else:
            is_secured = 'True' if 'ssl' == server['transport_type'] else 'False'
            proxy_config = 'proxy_ip=%s,proxy_port=%s,username=%s,password=%s,is_secured=%s' % \
                           (server['host'], server['port'], server['username'], server['password'], is_secured)
            if 'http only' == server['traffic_type']:
                args += ['--proxy', 'http-relay,%s' % proxy_config]
            elif 'https only' == server['traffic_type']:
                args += ['--proxy', 'http-connect,%s' % proxy_config]
            else:
                args += ['--proxy', 'http-relay,%s' % proxy_config]
                args += ['--proxy', 'http-connect,%s' % proxy_config]
    for server in config.list_ssh_private_servers():
        proxy_config = 'proxy_ip=%s,proxy_port=%s,username=%s,password=%s' % \
                       (server['host'], server['port'], server['username'], server['password'])
        for i in range(server['connections_count']):
            args += ['--proxy', 'ssh,%s' % proxy_config]
    return args
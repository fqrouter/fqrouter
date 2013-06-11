import logging

from utils import shell
from utils import iptables
from utils import config

__MANDATORY__ = True
LOGGER = logging.getLogger('fqrouter.%s' % __name__)
fqsocks_process = None


def start():
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
        '--log-file', '/data/data/fq.router/log/fqsocks.log',
        '--outbound-ip', '10.1.2.3', # send from 10.1.2.3 so we can skip redirecting those traffic
        '--listen', '10.1.2.3:8319',
        '--http-request-mark', '0xbabe', # trigger scrambler
        '--proxy', 'dynamic,n=20,dns_record=proxy#n#.fqrouter.com,is_public=True,priority=4',
        '--proxy', 'dynamic,n=5,dns_record=proxy2#n#.fqrouter.com,priority=2']
    args = configure(args)
    fqsocks_process = shell.launch_python('fqsocks', args, on_exit=stop)

def configure(args):
    args += ['--google-host', 'goagent-google-ip.fqrouter.com']
    if config.read().get('youtube_scrambler_enabled', True):
        args += ['--enable-youtube-scrambler']
    if config.read().get('goagent_public_servers_enabled', True):
        args += ['--proxy', 'dynamic,n=10,type=goagent,dns_record=goagent#n#.fqrouter.com,priority=1']
    for server in config.list_goagent_private_servers():
        proxy_config = 'goagent,appid=%s,path=%s,password=%s' % (server['appid'], server['path'], server['password'])
        args += ['--proxy', proxy_config]
    if config.read().get('shadowsocks_public_servers_enabled', True):
        args += ['--proxy', 'dynamic,n=4,type=ss,dns_record=ss#n#.fqrouter.com,priority=3']
    for server in config.list_shadowsocks_private_servers():
        proxy_config = 'ss,proxy_ip=%s,proxy_port=%s,password=%s,encrypt_method=%s' % (
            server['host'], server['port'], server['password'], server['encryption_method'])
        args += ['--proxy', proxy_config]
    return args
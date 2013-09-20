import json
import os
import shell
import logging

LOGGER = logging.getLogger(__name__)


def read():
    with open('/data/data/fq.router2/etc/fqrouter.json') as f:
        config_json = f.read()
    config = json.loads(config_json)
    if os.path.exists('/data/data/fq.router2/etc/fqrouter-overrides.json'):
        with open('/data/data/fq.router2/etc/fqrouter-overrides.json') as f:
            config_json = f.read()
        config.update(json.loads(config_json))
    return config


def list_goagent_private_servers():
    path = '/data/data/fq.router2/etc/goagent.json'
    if not os.path.exists(path):
        return []
    with open(path) as f:
        config_json = f.read()
    return json.loads(config_json)


def list_shadowsocks_private_servers():
    path = '/data/data/fq.router2/etc/shadowsocks.json'
    if not os.path.exists(path):
        return []
    with open(path) as f:
        config_json = f.read()
    return json.loads(config_json)


def list_http_proxy_private_servers():
    path = '/data/data/fq.router2/etc/http-proxy.json'
    if not os.path.exists(path):
        return []
    with open(path) as f:
        config_json = f.read()
    return json.loads(config_json)


def list_ssh_private_servers():
    path = '/data/data/fq.router2/etc/ssh.json'
    if not os.path.exists(path):
        return []
    with open(path) as f:
        config_json = f.read()
    return json.loads(config_json)


def get_default_dns_server():
    try:
        default_dns_server = shell.check_output(['getprop', 'net.dns1']).strip()
        if default_dns_server:
            return '%s:53' % default_dns_server
        else:
            return ''
    except:
        LOGGER.exception('failed to get default dns server')
        return ''

def configure_fqsocks(args):
    args += ['--ip-command', '/data/data/fq.router2/busybox']
    args += ['--ifconfig-command', '/data/data/fq.router2/busybox']
    args += ['--google-host', 'goagent-google-ip.fqrouter.com']
    args += ['--google-host', 'goagent-google-ip2.fqrouter.com']
    if not read().get('auto_access_check_enabled', True):
        args += ['--no-access-check']
    if not read().get('china_shortcut_enabled', True):
        args += ['--no-china-shortcut']
    if not read().get('direct_access_enabled', True):
        args += ['--no-direct-access']
    if read().get('youtube_scrambler_enabled', True):
        args += ['--youtube-scrambler']
    public_server_types = []
    if read().get('goagent_public_servers_enabled', True):
        public_server_types.append('goagent')
    if read().get('shadowsocks_public_servers_enabled', True):
        public_server_types.append('ss')
    if read().get('http_proxy_public_servers_enabled', True):
        public_server_types.append('http-connect')
        public_server_types.append('http-relay')
        public_server_types.append('spdy-connect')
        public_server_types.append('spdy-relay')
    if public_server_types:
        args += ['--proxy', 'directory,src=proxies.fqrouter.com,%s' % ','.join(['%s=True' % t for t in public_server_types])]
    for server in list_goagent_private_servers():
        proxy_config = 'goagent,appid=%s,path=%s,password=%s' % (server['appid'], server['path'], server['password'])
        args += ['--proxy', proxy_config]
    for server in list_shadowsocks_private_servers():
        proxy_config = 'ss,proxy_host=%s,proxy_port=%s,password=%s,encrypt_method=%s' % (
            server['host'], server['port'], server['password'], server['encryption_method'])
        args += ['--proxy', proxy_config]
    for server in list_http_proxy_private_servers():
        if 'spdy (webvpn)' == server['transport_type']:
            proxy_config = 'proxy_host=%s,proxy_port=%s,username=%s,password=%s,requested_spdy_version=%s' % \
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
            proxy_config = 'proxy_host=%s,proxy_port=%s,username=%s,password=%s,is_secured=%s' % \
                           (server['host'], server['port'], server['username'], server['password'], is_secured)
            if 'http only' == server['traffic_type']:
                args += ['--proxy', 'http-relay,%s' % proxy_config]
            elif 'https only' == server['traffic_type']:
                args += ['--proxy', 'http-connect,%s' % proxy_config]
            else:
                args += ['--proxy', 'http-relay,%s' % proxy_config]
                args += ['--proxy', 'http-connect,%s' % proxy_config]
    for server in list_ssh_private_servers():
        proxy_config = 'proxy_host=%s,proxy_port=%s,username=%s,password=%s' % \
                       (server['host'], server['port'], server['username'], server['password'])
        for i in range(server['connections_count']):
            args += ['--proxy', 'ssh,%s' % proxy_config]
    return args

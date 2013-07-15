import json
import os


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

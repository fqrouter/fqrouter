import json
import os


def read():
    with open('/data/data/fq.router/etc/fqrouter.json') as f:
        config_json = f.read()
    return json.loads(config_json)


def list_goagent_private_servers():
    path = '/data/data/fq.router/etc/goagent.json'
    if not os.path.exists(path):
        return []
    with open(path) as f:
        config_json = f.read()
    return json.loads(config_json)


def list_shadowsocks_private_servers():
    path = '/data/data/fq.router/etc/shadowsocks.json'
    if not os.path.exists(path):
        return []
    with open(path) as f:
        config_json = f.read()
    return json.loads(config_json)

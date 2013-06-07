import json

def read():
    with open('/data/data/fq.router/etc/fqrouter.json') as f:
        config_json = f.read()
    return json.loads(config_json)

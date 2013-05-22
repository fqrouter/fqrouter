from ConfigParser import ConfigParser
from cStringIO import StringIO


def read():
    parser = ConfigParser()
    parser.readfp(StringIO('\n'.join([line.strip() for line in """
    [fqrouter]
    WifiHotspotSSID=fqrouter
    WifiHotspotPassword=12345678
    BypassDirectlyEnabled=true
    """.splitlines()])), 'default')
    parser.read('/data/data/fq.router/config')
    return parser

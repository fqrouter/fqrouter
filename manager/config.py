import logging

import shell


LOGGER = logging.getLogger(__name__)

def get_default_dns_server():
    try:
        default_dns_server = shell.check_output(['getprop', 'net.dns1']).strip()
        if default_dns_server:
            return default_dns_server
        else:
            return ''
    except:
        LOGGER.exception('failed to get default dns server')
        return ''

def configure_fqsocks(args):
    args += ['--config-file', '/data/data/fq.router2/etc/fqsocks.json']
    args += ['--ip-command', '/data/data/fq.router2/busybox']
    args += ['--ifconfig-command', '/data/data/fq.router2/busybox']
    args += ['--google-host', 'goagent-google-ip.fqrouter.com']
    args += ['--google-host', 'goagent-google-ip2.fqrouter.com']
    return args

import config
import argparse
import fqsocks.gateways.proxy_client
import gevent
import httplib
import fqsocks.httpd

is_free_internet_connected = True

def handle_free_internet_connect(environ, start_response):
    global is_free_internet_connected
    is_free_internet_connected = True
    args = config.configure_fqsocks([])
    argument_parser = argparse.ArgumentParser()
    argument_parser.add_argument('--proxy', action='append', default=[])
    args, _ = argument_parser.parse_known_args(args)
    for props in args.proxy:
        props = props.split(',')
        prop_dict = dict(p.split('=') for p in props[1:])
        fqsocks.gateways.proxy_client.add_proxies(props[0], prop_dict)
    fqsocks.gateways.proxy_client.last_refresh_started_at = 0
    gevent.spawn(fqsocks.gateways.proxy_client.init_proxies)
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return []


fqsocks.httpd.HANDLERS[('POST', 'free-internet/connect')] = handle_free_internet_connect


def handle_free_internet_disconnect(environ, start_response):
    global is_free_internet_connected
    is_free_internet_connected = False
    fqsocks.gateways.proxy_client.proxy_directories = []
    fqsocks.gateways.proxy_client.proxies = []
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    return []


fqsocks.httpd.HANDLERS[('POST', 'free-internet/disconnect')] = handle_free_internet_disconnect


def handle_free_internet_is_connected(environ, start_response):
    start_response(httplib.OK, [('Content-Type', 'text/plain')])
    yield 'TRUE' if is_free_internet_connected else 'FALSE'


fqsocks.httpd.HANDLERS[('GET', 'free-internet/is-connected')] = handle_free_internet_is_connected

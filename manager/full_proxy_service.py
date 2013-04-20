import logging
import thread
from netfilterqueue import NetfilterQueue
import socket
import subprocess
import time
import os
import signal
import threading
import re
import urllib2
import struct

import dpkt
from pynetfilter_conntrack import Conntrack

import shutdown_hook
import iptables
import network_interface
import redsocks_template
import china_ip
import goagent_service


LOGGER = logging.getLogger(__name__)


def run():
    try:
        insert_iptables_rules()
        thread.start_new(start_full_proxy, ())
    except:
        LOGGER.exception('failed to start full proxy service')


def status():
    return 'N/A'


def clean():
    delete_iptables_rules()
    kill_redsocks()


#=== private ===

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
shutdown_hook.add(raw_socket.close)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)
SO_MARK = 36
raw_socket.setsockopt(socket.SOL_SOCKET, SO_MARK, 0xcafe)

PROXIES_COUNT = 20
RE_IP_PORT = r'(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)'
RE_REDSOCKS_CLIENT = re.compile(RE_IP_PORT + '->')
RE_REDSOCKS_INSTANCE = re.compile(r'Dumping client list for instance ' + RE_IP_PORT)
RULES = []
white_list = set()
black_list = set()
pending_list = {} # ip => started_at
proxies = {} # mark => proxy
redsocks_process = None
redsocks_started_at = 0
redsocks_dumped_at = None
uses_goagent = True

for iface in network_interface.list_data_network_interfaces():
    RULES.append((
        {'target': 'fp_OUTPUT', 'iface_out': iface, 'extra': 'tcp dpt:80'},
        ('nat', 'OUTPUT', '-o %s -p tcp --dport 80 -j fp_OUTPUT' % iface)
    ))
    RULES.append((
        {'target': 'fp_OUTPUT', 'iface_out': iface, 'extra': 'tcp dpt:443'},
        ('nat', 'OUTPUT', '-o %s -p tcp --dport 443 -j fp_OUTPUT' % iface)
    ))
RULES.append((
    {'target': 'fp_PREROUTING', 'extra': 'tcp dpt:80'},
    ('nat', 'PREROUTING', '-p tcp -s 192.168.49.0/24 --dport 80 -j fp_PREROUTING')
))
RULES.append((
    {'target': 'fp_PREROUTING', 'extra': 'tcp dpt:443'},
    ('nat', 'PREROUTING', '-p tcp -s 192.168.49.0/24 --dport 443 -j fp_PREROUTING')
))


def add_full_proxy_chain(is_prerouting):
    chain_name = 'fp_PREROUTING' if is_prerouting else 'fp_OUTPUT'
    for lan_ip_range in [
        '0.0.0.0/8', '10.0.0.0/8', '127.0.0.0/8', '169.254.0.0/16',
        '172.16.0.0/12', '192.168.0.0/16', '224.0.0.0/4', '240.0.0.0/4']:
        RULES.append((
            {'target': 'RETURN', 'destination': lan_ip_range},
            ('nat', chain_name, '-d %s -j RETURN' % lan_ip_range)
        ))
    RULES.append((
        {'target': 'RETURN', 'extra': 'mark match 0xcafe'},
        ('nat', chain_name, '-p tcp -m mark --mark 0xcafe -j RETURN')
    ))
    RULES.append((
        {'target': 'NFQUEUE', 'extra': 'mark match ! 0xbabe/0xffff NFQUEUE num 3'},
        ('nat', chain_name, '-p tcp -m mark ! --mark 0xbabe/0xffff -j NFQUEUE --queue-num 3')
    ))
    for i in range(1, 1 + PROXIES_COUNT + 1): # the final one is for goagent
        if is_prerouting:
            RULES.append((
                {'target': 'CONNMARK', 'extra': 'mark match 0x%sbabe CONNMARK set 0x%sbabe' % (i, i)},
                ('nat', chain_name, '-p tcp -m mark --mark 0x%sbabe -j CONNMARK --set-mark 0x%sbabe' % (i, i))
            ))
            RULES.append((
                {'target': 'CONNMARK', 'extra': 'mark match 0x%sbabe CONNMARK save' % i},
                ('nat', chain_name, '-p tcp -m mark --mark 0x%sbabe -j CONNMARK --save-mark' % i)
            ))
            RULES.append((
                {'target': 'DNAT', 'extra': 'mark match 0x%sbabe to:192.168.49.1:%s' % (i, 19830 + i)},
                ('nat', chain_name, '-p tcp -m mark --mark 0x%sbabe -j DNAT'
                                    ' --to-destination 192.168.49.1:%s' % (i, 19830 + i))
            ))
        else:
            RULES.append((
                {'target': 'CONNMARK', 'extra': 'mark match 0x%sbabe CONNMARK set 0x%sbabe' % (i, i)},
                ('nat', chain_name, '-p tcp -m mark --mark 0x%sbabe -j CONNMARK --set-mark 0x%sbabe' % (i, i))
            ))
            RULES.append((
                {'target': 'CONNMARK', 'extra': 'mark match 0x%sbabe CONNMARK save' % i},
                ('nat', chain_name, '-p tcp -m mark --mark 0x%sbabe -j CONNMARK --save-mark' % i)
            ))
            RULES.append((
                {'target': 'REDIRECT', 'extra': 'mark match 0x%sbabe redir ports %s' % (i, 19830 + i)},
                ('nat', chain_name, '-p tcp -m mark --mark 0x%sbabe -j REDIRECT --to-ports %s' % (i, 19830 + i))
            ))


add_full_proxy_chain(is_prerouting=False)
add_full_proxy_chain(is_prerouting=True)


def insert_iptables_rules():
    shutdown_hook.add(delete_iptables_rules)
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)
    iptables.delete_chain('fp_PREROUTING')
    iptables.delete_chain('fp_OUTPUT')


def start_full_proxy():
    try:
        thread.start_new(keep_proxies_fresh, ())
    except:
        LOGGER.exception('failed to start keep_proxies_fresh thread')
        proxies.clear()
    handle_nfqueue()


def keep_proxies_fresh():
    global redsocks_started_at
    global uses_goagent
    shutdown_hook.add(kill_redsocks)
    try:
        while True:
            if not proxies:
                LOGGER.info('no proxies, start redsocks now')
                try:
                    if kill_redsocks():
                        LOGGER.info('existing redsocks killed')
                        time.sleep(2)
                    start_redsocks()
                except:
                    LOGGER.exception('failed to start redsocks')
                    kill_redsocks()
                    return
                if proxies:
                    redsocks_started_at = time.time()
                    if not can_access_twitter():
                        LOGGER.info('still can not access twitter, retry in 120 seconds')
                        proxies.clear()
                        time.sleep(120)
                else:
                    LOGGER.info('still no proxies after redsocks started, retry in 120 seconds')
                    time.sleep(120)
            if time.time() - redsocks_started_at > 60 * 30:
                LOGGER.info('refresh now, restart redsocks')
                proxies.clear()
            if uses_goagent and not goagent_service.enabled:
                LOGGER.info('goagent service disabled, restart redsocks')
                uses_goagent = False
                proxies.clear()
            time.sleep(1)
            dump_redsocks_client_list()
    except:
        LOGGER.exception('failed to keep proxies fresh')


def can_access_twitter():
    success = 0
    for i in range(PROXIES_COUNT):
        try:
            urllib2.urlopen('https://www.twitter.com', timeout=5).read()
            success += 1
        except:
            pass
    LOGGER.info('twitter access success rate: %s/%s' % (success, PROXIES_COUNT))
    return success


def start_redsocks():
    global redsocks_process
    cfg_path = '/data/data/fq.router/redsocks.conf'
    for i in range(1, 1 + PROXIES_COUNT):
        resolve_proxy(eval('0x%sbabe' % i), 19830 + i, 'proxy%s.fqrouter.com' % i)
    if uses_goagent:
        proxies[eval('0x%sbabe' % (PROXIES_COUNT + 1))] = {
            'clients': set(),
            'rank': 0, # lower is better
            'pre_rank': 0, # lower is better
            'error_penalty': 256, # if error is found, this will be added to rank
            'connection_info': ('http-relay', '127.0.0.1', '8319', '', ''),
            'local_port': 19830 + PROXIES_COUNT + 1
        }
    with open(cfg_path, 'w') as f:
        f.write(redsocks_template.render(proxies.values()))
    redsocks_process = subprocess.Popen(
        ['/data/data/fq.router/proxy-tools/redsocks', '-c', cfg_path],
        stderr=subprocess.STDOUT, stdout=subprocess.PIPE, bufsize=1, close_fds=True)
    time.sleep(0.5)
    if redsocks_process.poll() is None:
        LOGGER.info('redsocks seems started: %s' % redsocks_process.pid)
        t = threading.Thread(target=poll_redsocks_output)
        t.daemon = True
        t.start()
    else:
        LOGGER.error('redsocks output:')
        LOGGER.error(redsocks_process.stdout.read())
        raise Exception('failed to start redsocks')


def poll_redsocks_output():
    try:
        current_instance = None
        current_clients = set()
        for line in iter(redsocks_process.stdout.readline, b''):
            match = RE_REDSOCKS_INSTANCE.search(line)
            if match:
                current_instance = int(match.group(2))
                current_clients = set()
                LOGGER.debug('dump redsocks instance %s' % current_instance)
            if current_instance:
                match = RE_REDSOCKS_CLIENT.search(line)
                if match:
                    ip = match.group(1)
                    port = int(match.group(2))
                    current_clients.add((ip, port))
                    LOGGER.debug('client %s:%s' % (ip, port))
            else:
                if 'http-connect.c:149' in line:
                    match = RE_REDSOCKS_CLIENT.search(line)
                    if match:
                        ip = match.group(1)
                        port = int(match.group(2))
                        for mark, proxy in proxies.items():
                            if (ip, port) in proxy['clients']:
                                LOGGER.error(line.strip())
                                handle_proxy_error(mark, proxy)
            if 'End of client list' in line:
                update_proxy_status(current_instance, current_clients)
                current_instance = None
        LOGGER.error('redsocks died, clear proxies')
        redsocks_process.stdout.close()
        proxies.clear()
    except:
        LOGGER.exception('failed to poll redsocks output')
        proxies.clear()


def handle_proxy_error(mark, proxy):
    error_penalty = proxy['error_penalty']
    if error_penalty > 256 * 2 * 2 * 2:
        LOGGER.error('proxy 0x%x purged due to too many errors: %s' % (mark, str(proxy['connection_info'])))
        del proxies[mark]
    else:
        LOGGER.error('add error penalty to proxy 0x%x: %s to %s' % (mark, error_penalty, str(proxy['connection_info'])))
        proxy['rank'] += error_penalty
        proxy['pre_rank'] += error_penalty
        proxy['error_penalty'] *= 2


def update_proxy_status(current_instance, current_clients):
    for mark, proxy in proxies.items():
        if proxy['local_port'] == current_instance:
            hangover_penalty = int(proxy['pre_rank'] / 2)
            rank = len(current_clients) + hangover_penalty # factor in the previous performance
            LOGGER.info('update proxy 0x%x rank: [%s+%s] %s' %
                        (mark, proxy['pre_rank'], hangover_penalty, str(proxy['connection_info'])))
            proxy['rank'] = rank
            proxy['pre_rank'] = rank
            proxy['clients'] = current_clients
            return
    LOGGER.debug('this redsocks instance has been removed from proxy list')


def dump_redsocks_client_list():
    global redsocks_dumped_at
    if redsocks_dumped_at is None:
        redsocks_dumped_at = time.time()
    elif time.time() - redsocks_dumped_at > 60:
        LOGGER.info('dump redsocks client list')
        os.kill(redsocks_process.pid, signal.SIGUSR1)
        redsocks_dumped_at = time.time()


def resolve_proxy(mark, local_port, name):
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
        sock.settimeout(3)
        request = dpkt.dns.DNS(qd=[dpkt.dns.DNS.Q(name=name, type=dpkt.dns.DNS_TXT)])
        sock.sendto(str(request), ('8.8.8.8', 53))
        data, addr = sock.recvfrom(1024)
        response = dpkt.dns.DNS(data)
        answer = response.an[0]
        connection_info = ''.join(e for e in answer.rdata if e.isalnum() or e in [':', '.', '-'])
        connection_info = connection_info.split(':') # proxy_type:ip:port:username:password
        proxies[mark] = {
            'clients': set(),
            'rank': 0, # lower is better
            'pre_rank': 0, # lower is better
            'error_penalty': 256, # if error is found, this will be added to rank
            'connection_info': connection_info,
            'local_port': local_port
        }
        LOGGER.info('resolved proxy 0x%x: %s' % (mark, proxies[mark]))
    except:
        LOGGER.exception('failed to resolve proxy 0x%x: %s %s' % (mark, local_port, name))


def kill_redsocks():
    return not subprocess.call(['/data/data/fq.router/busybox', 'killall', 'redsocks'])


def handle_nfqueue():
    try:
        nfqueue = NetfilterQueue()
        nfqueue.bind(3, handle_packet)
        nfqueue.run()
    except:
        LOGGER.exception('stopped handling nfqueue')


def handle_packet(nfqueue_element):
    try:
        ip_packet = dpkt.ip.IP(nfqueue_element.get_payload())
        ip = socket.inet_ntoa(ip_packet.dst)
        if china_ip.is_china_ip(ip):
            nfqueue_element.accept()
        elif ip in white_list:
            nfqueue_element.accept()
        elif ip in black_list:
            set_verdict_proxy(nfqueue_element, ip_packet)
        else:
            nfqueue_element.accept()
    except:
        LOGGER.exception('failed to handle packet')
        nfqueue_element.accept()


def set_verdict_proxy(nfqueue_element, ip_packet):
    mark = pick_proxy(ip_packet)
    if mark:
        nfqueue_element.set_mark(mark)
        nfqueue_element.repeat()
    else:
        nfqueue_element.accept()


def pick_proxy(ip_packet):
    if not proxies:
        return None
    marks = {}
    for mark, proxy in proxies.items():
        if 'http-relay' == proxy['connection_info'][0]:
            if 80 == ip_packet.tcp.dport:
                marks[proxy['rank'] - 5] = mark
            continue
        marks[proxy['rank']] = mark
    mark = marks[sorted(marks.keys())[0]]
    ip = socket.inet_ntoa(ip_packet.src)
    port = ip_packet.tcp.sport
    proxy = proxies[mark]
    proxy['rank'] += 1
    proxy['clients'].add((ip, port))
    LOGGER.info('full proxy via 0x%x [%s] %s: %s:%s => %s:%s' % (
        mark, proxy['rank'], str(proxy['connection_info']),
        ip, port, socket.inet_ntoa(ip_packet.dst), ip_packet.tcp.dport))
    return mark


def add_to_black_list(ip, syn=None):
    if ip not in black_list and ip not in white_list:
        for mark, proxy in proxies.items():
            if ip == proxy['connection_info'][1]:
                LOGGER.error('proxy died: %s' % ip)
                del proxies[mark]
                return
        LOGGER.info('add black list ip: %s' % ip)
        black_list.add(ip)
        if syn:
            delete_existing_conntrack_entry(ip)
            raw_socket.sendto(str(syn), (socket.inet_ntoa(syn.dst), 0))
    pending_list.pop(ip, None)


def delete_existing_conntrack_entry(ip):
    conntrack = Conntrack()
    for entry in conntrack.dump_table():
        dst = socket.inet_ntoa(struct.pack('!I', entry.orig_ipv4_dst))
        if 0 == entry.mark and ip == dst:
            LOGGER.info('delete %s' % entry)
            conntrack.destroy_conntrack(entry)


def add_to_white_list(ip):
    if ip not in white_list and not china_ip.is_china_ip(ip):
        if ip in black_list:
            LOGGER.info('add white list ip from black list: %s' % ip)
            black_list.remove(ip)
        else:
            LOGGER.info('add white list ip: %s' % ip)
        white_list.add(ip)
    pending_list.pop(ip, None)
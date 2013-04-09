import logging
import thread
from netfilterqueue import NetfilterQueue
import socket
import subprocess
import time
import random
import os
import signal
import threading
import re

import dpkt

import shutdown_hook
import iptables
import network_interface
import redsocks_template
import china_ip


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

RE_IP_PORT = r'(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)'
RE_REDSOCKS_CLIENT = re.compile(RE_IP_PORT + '->')
RE_REDSOCKS_INSTANCE = re.compile(r'Dumping client list for instance ' + RE_IP_PORT)
RULES = []
white_list = set()
black_list = set()
pending_list = {} # ip => started_at
proxies = {} # mark => last_used_at
redsocks_process = None
redsocks_dumped_at = None

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
    for i in [1, 2, 3, 4, 5, 6, 7, 8, 9]:
        if is_prerouting:
            RULES.append((
                {'target': 'DNAT', 'extra': 'mark match 0x%sbabe to:192.168.49.1:1983%s' % (i, i)},
                ('nat', chain_name, '-p tcp -m mark --mark 0x%sbabe -j DNAT'
                                    ' --to-destination 192.168.49.1:1983%s' % (i, i))
            ))
        else:
            RULES.append((
                {'target': 'REDIRECT', 'extra': 'mark match 0x%sbabe redir ports 1983%s' % (i, i)},
                ('nat', chain_name, '-p tcp -m mark --mark 0x%sbabe -j REDIRECT --to-ports 1983%s' % (i, i))
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
        start_redsocks()
    except:
        LOGGER.exception('failed to start redsocks')
        proxies.clear()
    handle_nfqueue()


def start_redsocks():
    global redsocks_process
    cfg_path = '/data/data/fq.router/redsocks.conf'
    resolve_proxy(0x1babe, 19831, 'proxy2.fqrouter.com')
    resolve_proxy(0x2babe, 19832, 'proxy3.fqrouter.com')
    resolve_proxy(0x3babe, 19833, 'proxy4.fqrouter.com')
    resolve_proxy(0x4babe, 19834, 'proxy5.fqrouter.com')
    resolve_proxy(0x5babe, 19835, 'proxy6.fqrouter.com')
    resolve_proxy(0x6babe, 19836, 'proxy7.fqrouter.com')
    resolve_proxy(0x7babe, 19837, 'proxy8.fqrouter.com')
    resolve_proxy(0x8babe, 19838, 'proxy9.fqrouter.com')
    resolve_proxy(0x9babe, 19839, 'proxy10.fqrouter.com')
    with open(cfg_path, 'w') as f:
        f.write(redsocks_template.render(proxies.values()))
    redsocks_process = subprocess.Popen(
        ['/data/data/fq.router/proxy-tools/redsocks', '-c', cfg_path],
        stderr=subprocess.STDOUT, stdout=subprocess.PIPE, bufsize=1, close_fds=True)
    shutdown_hook.add(kill_redsocks)
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
                    for proxy in proxies.values():
                        if (ip, port) in proxy['clients']:
                            LOGGER.error(line.strip())
                            LOGGER.error('add penalty: %s' % str(proxy['connection_info']))
                            proxy['rank'] += 256 # 128, 64, 32, 16
                            proxy['pre_rank'] += 256 # 128, 64, 32, 16
        if 'End of client list' in line:
            update_proxy_status(current_instance, current_clients)
            current_instance = None
        dump_redsocks_client_list()
    redsocks_process.stdout.close()
    proxies.clear()


def update_proxy_status(current_instance, current_clients):
    for mark, proxy in proxies.items():
        if proxy['local_port'] == current_instance:
            penalty = int(proxy['pre_rank'] / 2)
            rank = len(current_clients) + penalty # factor in the previous performance
            if rank > 500:
                LOGGER.error('purge proxy 0x%x: rank is %s' % (mark, rank))
                del proxies[mark]
                return
            LOGGER.info('update proxy 0x%x rank: [%s+%s] %s' %
                        (mark, proxy['pre_rank'], penalty, str(proxy['connection_info'])))
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
        sock.settimeout(2)
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
            'connection_info': connection_info,
            'local_port': local_port
        }
        LOGGER.info('resolved proxy 0x%x: %s' % (mark, proxies[mark]))
    except:
        LOGGER.exception('failed to resolve proxy 0x%x: %s %s' % (mark, local_port, name))


def kill_redsocks():
    subprocess.call(['/data/data/fq.router/busybox', 'killall', 'redsocks'])


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
        elif ip in black_list:
            set_verdict_proxy(nfqueue_element, ip_packet)
        elif ip in white_list:
            nfqueue_element.accept()
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


def add_to_black_list(ip):
    if ip not in black_list:
        for mark, proxy in proxies.items():
            if ip == proxy['connection_info'][1]:
                LOGGER.error('proxy died: %s' % ip)
                del proxies[mark]
                return
        LOGGER.info('add black list ip: %s' % ip)
        black_list.add(ip)
    pending_list.pop(ip, None)


def add_to_white_list(ip):
    if ip not in white_list and not china_ip.is_china_ip(ip):
        LOGGER.info('add white list ip: %s' % ip)
        white_list.add(ip)
    pending_list.pop(ip, None)
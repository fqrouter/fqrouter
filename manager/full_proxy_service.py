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
    if is_prerouting:
        RULES.append((
            {'target': 'DNAT', 'extra': 'mark match 0x1babe to:192.168.49.1:12345'},
            ('nat', chain_name, '-p tcp -m mark --mark 0x1babe -j DNAT --to-destination 192.168.49.1:12345')
        ))
    else:
        RULES.append((
            {'target': 'REDIRECT', 'extra': 'mark match 0x1babe redir ports 12345'},
            ('nat', chain_name, '-p tcp -m mark --mark 0x1babe -j REDIRECT --to-ports 12345')
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
    handle_nfqueue()


def start_redsocks():
    global redsocks_process
    cfg_path = '/data/data/fq.router/redsocks.conf'
    resolve_proxy(0x1babe, 'proxy1.fqrouter.com')
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
    should_log = False
    for line in iter(redsocks_process.stdout.readline, b''):
        if 'Dumping client list' in line:
            should_log = True
            # if should_log:
        LOGGER.info(line.strip())
        if 'End of client list' in line:
            should_log = False
        dump_redsocks_client_list()
    redsocks_process.stdout.close()
    proxies.clear()


def dump_redsocks_client_list():
    global redsocks_dumped_at
    if redsocks_dumped_at is None:
        redsocks_dumped_at = time.time()
    elif time.time() - redsocks_dumped_at > 60:
        LOGGER.info('dump redsocks client list')
        os.kill(redsocks_process.pid, signal.SIGUSR1)
        redsocks_dumped_at = time.time()


def resolve_proxy(mark, name):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
    request = dpkt.dns.DNS(qd=[dpkt.dns.DNS.Q(name=name, type=dpkt.dns.DNS_TXT)])
    sock.sendto(str(request), ('8.8.8.8', 53))
    data, addr = sock.recvfrom(1024)
    response = dpkt.dns.DNS(data)
    answer = response.an[0]
    connection_info = ''.join(e for e in answer.rdata if e.isalnum() or e in [':', '.'])
    connection_info = connection_info.split(':') # proxy_type:ip:port:username:password
    proxies[mark] = {
        'clients': set(), # set((ip, port))
        'connection_info': connection_info
    }


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
        mark = pick_proxy()
        if not mark:
            nfqueue_element.accept()
        elif china_ip.is_china_ip(ip):
            nfqueue_element.accept()
        elif ip in black_list:
            nfqueue_element.set_mark(mark)
            nfqueue_element.repeat()
        elif ip in white_list:
            nfqueue_element.accept()
        else:
            if ip in pending_list:
                if time.time() - pending_list[ip] > 10:
                    add_to_black_list(ip)
            else:
                LOGGER.info('probe connectivity: %s' % ip)
                pending_list[ip] = time.time()
            ip_packet.tcp.sport = random.randint(1024, 65535)
            ip_packet.tcp.sum = 0
            ip_packet.sum = 0
            raw_socket.sendto(str(ip_packet), (ip, 0)) # send probe, SYN ACK will received by tcp service
            nfqueue_element.set_mark(mark)
            nfqueue_element.repeat()
    except:
        LOGGER.exception('failed to handle packet')
        nfqueue_element.accept()


def pick_proxy():
    if not proxies:
        return None
    return random.choice(proxies.keys())


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
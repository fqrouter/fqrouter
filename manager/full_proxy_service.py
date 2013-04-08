import logging
import thread
from netfilterqueue import NetfilterQueue
import socket
import subprocess
import time
import random

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
proxy_marks = []
redsocks_process = None

for iface in network_interface.list_data_network_interfaces():
    RULES.append((
        {'target': 'fp_OUTPUT', 'iface_out': iface},
        ('nat', 'OUTPUT', '-o %s -j fp_OUTPUT' % iface)
    ))
    for lan_ip_range in [
        '0.0.0.0/8', '10.0.0.0/8', '127.0.0.0/8', '169.254.0.0/16',
        '172.16.0.0/12', '192.168.0.0/16', '224.0.0.0/4', '240.0.0.0/4']:
        RULES.append((
            {'target': 'RETURN', 'destination': lan_ip_range, 'iface_out': iface},
            ('nat', 'fp_OUTPUT', '-o %s -d %s -j RETURN' % (iface, lan_ip_range))
        ))
    RULES.append((
        {'target': 'RETURN', 'iface_out': iface, 'extra': 'mark match 0xcafe'},
        ('nat', 'fp_OUTPUT', '-o %s -p tcp -m mark --mark 0xcafe -j RETURN' % iface)
    ))
    RULES.append((
        {'target': 'NFQUEUE', 'iface_out': iface, 'extra': 'mark match ! 0xbabe/0xffff NFQUEUE num 3'},
        ('nat', 'fp_OUTPUT', '-o %s -p tcp -m mark ! --mark 0xbabe/0xffff -j NFQUEUE --queue-num 3' % iface)
    ))
    RULES.append((
        {'target': 'REDIRECT', 'iface_out': iface, 'extra': 'mark match 0x1babe redir ports 12345'},
        ('nat', 'fp_OUTPUT', '-o %s -p tcp -m mark --mark 0x1babe -j REDIRECT --to-ports 12345' % iface)
    ))


def insert_iptables_rules():
    shutdown_hook.add(delete_iptables_rules)
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)
    iptables.delete_chain('fp_OUTPUT')
    iptables.delete_chain('fp_FORWARD')


def start_full_proxy():
    try:
        start_redsocks()
    except:
        LOGGER.exception('failed to start redsocks')
    handle_nfqueue()


def start_redsocks():
    global redsocks_process
    cfg_path = '/data/data/fq.router/redsocks.conf'
    proxy_type, proxy_ip, proxy_port, proxy_username, proxy_password = resolve_proxy()
    with open(cfg_path, 'w') as f:
        white_list.add(proxy_ip)
        f.write(redsocks_template.render(proxy_type, proxy_ip, proxy_port, proxy_username, proxy_password))
    kill_redsocks()
    time.sleep(1)
    redsocks_process = subprocess.Popen(
        ['/data/data/fq.router/proxy-tools/redsocks', '-c', cfg_path], stderr=subprocess.STDOUT)
    shutdown_hook.add(kill_redsocks)
    time.sleep(1)
    pid = redsocks_process.pid
    if not redsocks_process.poll():
        LOGGER.info('redsocks seems started: %s' % pid)
        proxy_marks.append(0x1babe)


def resolve_proxy():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
    request = dpkt.dns.DNS(qd=[dpkt.dns.DNS.Q(name='proxy1.fqrouter.com', type=dpkt.dns.DNS_TXT)])
    sock.sendto(str(request), ('8.8.8.8', 53))
    data, addr = sock.recvfrom(1024)
    response = dpkt.dns.DNS(data)
    answer = response.an[0]
    proxy_info = ''.join(e for e in answer.rdata if e.isalnum() or e in [':', '.'])
    return proxy_info.split(':') # proxy_type:ip:port:username:password


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
        if not proxy_marks:
            nfqueue_element.accept()
        elif china_ip.is_china_ip(ip):
            nfqueue_element.accept()
        elif ip in white_list:
            nfqueue_element.accept()
        elif ip in black_list:
            mark = random.choice(proxy_marks)
            nfqueue_element.set_mark(mark)
            nfqueue_element.repeat()
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
            mark = random.choice(proxy_marks)
            nfqueue_element.set_mark(mark)
            nfqueue_element.repeat()
    except:
        LOGGER.exception('failed to handle packet')
        nfqueue_element.accept()


def add_to_black_list(ip):
    if ip not in black_list:
        LOGGER.info('add black list ip: %s' % ip)
        black_list.add(ip)
    pending_list.pop(ip, None)


def add_to_white_list(ip):
    if ip not in white_list and not china_ip.is_china_ip(ip):
        LOGGER.info('add white list ip: %s' % ip)
        white_list.add(ip)
    pending_list.pop(ip, None)
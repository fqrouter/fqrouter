import logging
import thread
from netfilterqueue import NetfilterQueue
import socket
import time
import urllib2
import threading
import subprocess

import dpkt

import shutdown_hook
import iptables
import china_ip
import redsocks_monitor
import goagent_monitor


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
    global enabled
    enabled = False
    delete_iptables_rules()
    redsocks_monitor.kill_redsocks()
    goagent_monitor.kill_goagent()


#=== private ===

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
shutdown_hook.add(raw_socket.close)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)
SO_MARK = 36
raw_socket.setsockopt(socket.SOL_SOCKET, SO_MARK, 0xcafe)

REFRESH_INTERVAL = 60 * 30
PROXIES_COUNT = 20
RULES = []
white_list = set()
black_list = set()
pending_list = {} # ip => started_at
proxies = {} # mark => proxy
proxies_refreshed_at = 0
enabled = True


def add_lan_chains():
    # all 443,80 goes to lan_unknown before routing
    RULES.append((
        {'target': 'lan_unknown', 'extra': 'tcp dpt:80'},
        ('nat', 'OUTPUT', '-p tcp --dport 80 -j lan_unknown')
    ))
    RULES.append((
        {'target': 'lan_unknown', 'extra': 'tcp dpt:443'},
        ('nat', 'OUTPUT', '-p tcp --dport 443 -j lan_unknown')
    ))
    RULES.append((
        {'target': 'lan_unknown', 'extra': 'tcp dpt:80'},
        ('nat', 'PREROUTING', '-p tcp --dport 80 -j lan_unknown')
    ))
    RULES.append((
        {'target': 'lan_unknown', 'extra': 'tcp dpt:443'},
        ('nat', 'PREROUTING', '-p tcp --dport 443 -j lan_unknown')
    ))
    # ignore mark 0xcafe
    RULES.append((
        {'target': 'RETURN', 'extra': 'mark match 0xcafe'},
        ('nat', 'lan_unknown', '-m mark --mark 0xcafe -j RETURN')
    ))
    # filter out lan_src
    for lan_ip_range in [
        '0.0.0.0/8', '10.0.0.0/8', '127.0.0.0/8', '169.254.0.0/16',
        '172.16.0.0/12', '192.168.0.0/16', '224.0.0.0/4', '240.0.0.0/4']:
        RULES.append((
            {'target': 'lan_src', 'source': lan_ip_range},
            ('nat', 'lan_unknown', '-s %s -j lan_src' % lan_ip_range)
        ))
        RULES.append((
            {'target': 'RETURN', 'destination': lan_ip_range},
            ('nat', 'lan_src', '-d %s -j RETURN' % lan_ip_range)
        ))
        # from lan => not lan, that is outgoing traffic
    RULES.append((
        {'target': 'full_proxy'},
        ('nat', 'lan_src', '-j full_proxy')
    ))


def add_full_proxy_chain():
    RULES.append((
        {'target': 'NFQUEUE', 'extra': 'mark match ! 0xbabe/0xffff NFQUEUE num 3'},
        ('nat', 'full_proxy', '-p tcp -m mark ! --mark 0xbabe/0xffff -j NFQUEUE --queue-num 3')
    ))
    for i in range(1, 1 + PROXIES_COUNT + 1): # the final one is for goagent
        RULES.append((
            {'target': 'DNAT', 'extra': 'mark match 0x%sbabe to:10.1.2.3:%s' % (i, 19830 + i)},
            ('nat', 'full_proxy', '-p tcp -m mark --mark 0x%sbabe -j DNAT'
                                  ' --to-destination 10.1.2.3:%s' % (i, 19830 + i))
        ))


add_lan_chains()
add_full_proxy_chain()


def insert_iptables_rules():
    shutdown_hook.add(delete_iptables_rules)
    iptables.insert_rules(RULES)


def delete_iptables_rules():
    iptables.delete_rules(RULES)
    iptables.delete_chain('full_proxy')
    iptables.delete_chain('lan_unknown')
    iptables.delete_chain('lan_src')


def start_full_proxy():
    try:
        thread.start_new(keep_proxies_fresh, ())
    except:
        LOGGER.exception('failed to start keep proxies fresh thread')
        proxies.clear()
    handle_nfqueue()


def keep_proxies_fresh():
    global proxies_refreshed_at
    shutdown_hook.add(redsocks_monitor.kill_redsocks)
    try:
        while enabled:
            if not proxies:
                LOGGER.info('no proxies, refresh now')
                if not start_proxies():
                    return
                proxies_refreshed_at = time.time()
            if time.time() - proxies_refreshed_at > REFRESH_INTERVAL:
                LOGGER.info('refresh now, restart redsocks')
                proxies.clear()
            if not redsocks_monitor.is_redsocks_live():
                LOGGER.info('redsocks died, restart')
                proxies.clear()
            time.sleep(15)
    except:
        LOGGER.exception('failed to keep proxies fresh')
    finally:
        LOGGER.info('keep proxies fresh thread died')


def start_proxies():
    if redsocks_monitor.kill_redsocks():
        LOGGER.info('existing redsocks killed')
        time.sleep(2)
    LOGGER.info('starting goagent')
    try:
        start_goagent()
    except:
        LOGGER.exception('failed to start goagent')
        goagent_monitor.kill_goagent()
    LOGGER.info('resolving free proxies')
    resolve_free_proxies()
    LOGGER.info('starting redsocks')
    try:
        start_redsocks()
    except:
        LOGGER.exception('failed to start redsocks')
        proxies.clear()
        redsocks_monitor.kill_redsocks()
        return False
    if proxies:
        if not can_access_twitter():
            LOGGER.info('still can not access twitter, retry in 120 seconds')
            proxies.clear()
            time.sleep(120)
        redsocks_monitor.dump_redsocks_client_list(should_dump=True)
    else:
        LOGGER.info('still no proxies after redsocks started, retry in 120 seconds')
        time.sleep(120)
    return True


def start_goagent():
    goagent_monitor.kill_goagent()
    goagent_monitor.on_goagent_died = on_goagent_died
    goagent_monitor.start_goagent()
    proxies[eval('0x%sbabe' % (PROXIES_COUNT + 1))] = {
        'clients': set(),
        'rank': 0, # lower is better
        'pre_rank': 0, # lower is better
        'error_penalty': 256, # if error is found, this will be added to rank
        'connection_info': ('http-relay', '127.0.0.1', '8319', '', ''),
        'local_port': 19830 + PROXIES_COUNT + 1
    }


def on_goagent_died():
    LOGGER.info('goagent died')
    mark = eval('0x%sbabe' % (PROXIES_COUNT + 1))
    if mark in proxies:
        del proxies[mark]


def resolve_free_proxies():
    for i in range(1, 1 + PROXIES_COUNT):
        resolve_free_proxy(eval('0x%sbabe' % i), 19830 + i, 'proxy%s.fqrouter.com' % i)


def resolve_free_proxy(mark, local_port, name):
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
        LOGGER.exception('failed to resolve free proxy 0x%x: %s %s' % (mark, local_port, name))


def start_redsocks():
    redsocks_monitor.list_proxies = proxies.items
    redsocks_monitor.clear_proxies = proxies.clear
    redsocks_monitor.handle_proxy_error = handle_proxy_error
    redsocks_monitor.update_proxy = update_proxy
    redsocks_monitor.start_redsocks(proxies)


def update_proxy(mark, **kwargs):
    proxies[mark].update(kwargs)


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


def can_access_twitter():
    checkers = []
    for i in range(PROXIES_COUNT * 2):
        checker = TwitterAccessChecker()
        checker.daemon = True
        checker.start()
        checkers.append(checker)
        time.sleep(0.5)
    success_count = 0
    for checker in checkers:
        checker.join()
        if checker.success:
            success_count += 1
    LOGGER.info('twitter access success rate: %s/%s' % (success_count, len(checkers)))
    return success_count


class TwitterAccessChecker(threading.Thread):
    def __init__(self):
        super(TwitterAccessChecker, self).__init__()
        self.success = False

    def run(self):
        try:
            urllib2.urlopen('https://www.twitter.com', timeout=10).read()
            self.success = True
        except:
            pass


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
                marks[proxy['rank'] - 15] = mark
            continue
        marks[proxy['rank']] = mark
    if not marks:
        return None
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
    try:
        output = subprocess.check_output(
            ['/data/data/fq.router/python/bin/conntrack', '-D', '-p', 'tcp', '--reply-src', ip],
            stderr=subprocess.STDOUT).strip()
        LOGGER.info('succeed: %s' % output)
    except subprocess.CalledProcessError, e:
        LOGGER.error('failed: %s' % e.output)
        LOGGER.exception('failed to delete existing conntrack entry %s' % ip)


def add_to_white_list(ip):
    if ip not in white_list and not china_ip.is_china_ip(ip):
        if ip in black_list:
            LOGGER.info('add white list ip from black list: %s' % ip)
            black_list.remove(ip)
        else:
            LOGGER.info('add white list ip: %s' % ip)
        white_list.add(ip)
    pending_list.pop(ip, None)
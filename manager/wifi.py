import os
import logging
import socket
import subprocess
import shlex
import time

import tornado.web
import iptables
import network_interface
import hostapd_template


LOGGER = logging.getLogger(__name__)
MODALIAS_PATH = '/sys/class/net/%s/device/modalias' % network_interface.WIFI_INTERFACE
WPA_SUPPLICANT_CONF_PATH = '/data/misc/wifi/wpa_supplicant.conf'
P2P_CLI_PATH = '/data/data/fq.router/wifi-tools/p2p_cli'
netd_socket = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
netd_socket.connect('/dev/socket/netd')

RULES = []
for iface in network_interface.list_data_network_interfaces():
    RULES.append((
        {'target': 'MASQUERADE', 'iface_out': iface, 'source': '0.0.0.0/0', 'destination': '0.0.0.0/0'},
        ('nat', 'POSTROUTING', '-o %s -j MASQUERADE' % iface)
    ))


def clean():
    working_hotspot_iface = get_working_hotspot_iface()
    if working_hotspot_iface:
        stop_hotspot(working_hotspot_iface)


class WifiHandler(tornado.web.RequestHandler):
    def post(self):
        action = self.get_argument('action')
        if 'start-hotspot' == action:
            try:
                working_hotspot_iface = get_working_hotspot_iface()
                if working_hotspot_iface:
                    self.write('hotspot is already working, start skipped')
                else:
                    start_hotspot()
                    self.write('hotspot started successfully')
            except:
                LOGGER.exception('failed to start hotspot')
                self.write('failed to start hotspot')
        elif 'stop-hotspot' == action:
            try:
                working_hotspot_iface = get_working_hotspot_iface()
                if working_hotspot_iface:
                    stop_hotspot(working_hotspot_iface)
                    self.write('hotspot stopped successfully')
                else:
                    self.write('hotspot has not been started yet')
            except:
                LOGGER.exception('failed to stop hotspot')
                self.write('failed to stop hotspot')


def get_working_hotspot_iface():
    ifaces = list_wifi_ifaces()
    for iface, is_hotspot in ifaces.items():
        if is_hotspot:
            return iface
    return None


def list_wifi_ifaces():
    ifaces = {}
    current_iface = None
    for line in shell_execute('iw dev').splitlines(False):
        line = line.strip()
        if not line:
            continue
        if line.startswith('Interface '):
            current_iface = line.replace('Interface ', '')
            ifaces[current_iface] = False
            continue
        if 'type AP' in line or 'type P2P-GO' in line:
            ifaces[current_iface] = True
    return ifaces


def stop_hotspot(iface):
    iptables.delete_rules(RULES)
    netd_execute('tether stop')
    stop_p2p_persistent_network(get_wpa_supplicant_control_socket_dir(), iface)
    netd_execute('softap fwreload wlan0 STA')
    shell_execute('netcfg wlan0 down')
    shell_execute('netcfg wlan0 up')
    shell_execute('killall hostapd')


def start_hotspot():
    if not os.path.exists(MODALIAS_PATH):
        raise Exception('wifi chipset unknown: %s not found' % MODALIAS_PATH)
    with open(MODALIAS_PATH) as f:
        wifi_chipset = f.read().strip()
    if '0x4330' == wifi_chipset:
        start_hotspot_on_bcm()
    elif '0x6628' == wifi_chipset:
        start_hotspot_on_mtk()
    elif 'platform:wl12xx' == wifi_chipset:
        start_hotspot_on_wl12xx()
    else:
        raise Exception('wifi chipset is not supported: %s' % wifi_chipset)
    if not get_working_hotspot_iface():
        raise Exception('working hotspot iface not found after start')


def start_hotspot_on_bcm():
    netd_execute('softap fwreload wlan0 P2P')
    shell_execute('netcfg wlan0 down')
    shell_execute('netcfg wlan0 up')
    time.sleep(1)
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    delete_existing_p2p_persistent_networks('wlan0', control_socket_dir)
    start_p2p_persistent_network('wlan0', control_socket_dir)
    p2p_persistent_iface = get_p2p_persistent_iface()
    netd_execute('interface setcfg %s 192.168.49.1 24' % p2p_persistent_iface)
    netd_execute('tether stop')
    netd_execute('tether interface add %s' % p2p_persistent_iface)
    netd_execute('tether start 192.168.49.2 192.168.49.254')
    netd_execute('tether dns set 8.8.8.8')
    enable_ipv4_forward()
    shell_execute('iptables -P FORWARD ACCEPT')
    iptables.insert_rules(RULES)


def start_hotspot_on_mtk():
    netd_execute('1 softap fwreload wlan0 AP') # TODO: netd might expect sequence number or not
    time.sleep(1)
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    delete_existing_p2p_persistent_networks('ap0', control_socket_dir)
    start_p2p_persistent_network('ap0', control_socket_dir)
    netd_execute('2 interface setcfg ap0 192.168.49.1 24')
    netd_execute('3 tether stop')
    netd_execute('4 tether interface add ap0')
    netd_execute('5 tether start 192.168.49.2 192.168.49.254')
    netd_execute('6 tether dns set 8.8.8.8')
    enable_ipv4_forward()
    shell_execute('iptables -P FORWARD ACCEPT')
    iptables.insert_rules(RULES)


def start_hotspot_on_wl12xx():
    if 'ap0' not in list_wifi_ifaces():
        shell_execute('iw wlan0 interface add ap0 type managed')
    assert 'ap0' in list_wifi_ifaces()
    with open('/data/misc/wifi/fqrouter.conf', 'w') as f:
        f.write(hostapd_template.render(channel=1)) # TODO: get channel from iwlist
    LOGGER.info('start hostapd')
    proc = subprocess.Popen(
        ['hostapd', '/data/misc/wifi/fqrouter.conf'],
        cwd='/data/misc/wifi', stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    time.sleep(2)
    if proc.poll():
        LOGGER.error('hostapd failed')
        LOGGER.error(proc.stdout.read())
        raise Exception('hostapd failed')
    else:
        LOGGER.info('hostapd seems like started successfully')


def enable_ipv4_forward():
    LOGGER.info('enable ipv4 forward')
    with open('/proc/sys/net/ipv4/ip_forward', 'w') as f:
        f.write('1')


def start_p2p_persistent_network(iface, control_socket_dir):
    index = shell_execute('%s -p %s -i %s add_network' % (P2P_CLI_PATH, control_socket_dir, iface)).strip()
    shell_execute('%s -p %s -i %s set_network %s mode 3' % (P2P_CLI_PATH, control_socket_dir, iface, index))
    shell_execute('%s -p %s -i %s set_network %s disabled 2' % (P2P_CLI_PATH, control_socket_dir, iface, index))
    shell_execute('%s -p %s -i %s set_network %s ssid \'"spike"\'' % (P2P_CLI_PATH, control_socket_dir, iface, index))
    shell_execute('%s -p %s -i %s set_network %s key_mgmt WPA-PSK' % (P2P_CLI_PATH, control_socket_dir, iface, index))
    shell_execute('%s -p %s -i %s set_network %s proto RSN' % (P2P_CLI_PATH, control_socket_dir, iface, index))
    shell_execute('%s -p %s -i %s set_network %s pairwise CCMP' % (P2P_CLI_PATH, control_socket_dir, iface, index))
    shell_execute('%s -p %s -i %s set_network %s psk \'"12345678"\'' % (P2P_CLI_PATH, control_socket_dir, iface, index))
    shell_execute('%s -p %s -i %s p2p_group_add persistent=%s' % (P2P_CLI_PATH, control_socket_dir, iface, index))
    time.sleep(1)


def get_p2p_persistent_iface():
    for line in shell_execute('netcfg').splitlines(False):
        if line.startswith('p2p-wlan0'):
            return line.split(' ')[0]
    raise Exception('can not find just started p2p persistent network interface')


def stop_p2p_persistent_network(control_socket_dir, iface):
    shell_execute('%s -p %s -i wlan0 p2p_group_remove %s' % (P2P_CLI_PATH, control_socket_dir, iface))
    time.sleep(1)


def delete_existing_p2p_persistent_networks(iface, control_socket_dir):
    LOGGER.info('delete existing p2p persistent networks')
    existing_networks = list_existing_networks(iface, control_socket_dir)
    for i in sorted(existing_networks.keys(), reverse=True):
        network = existing_networks[i]
        if 'P2P-PERSISTENT' in network['status']:
            delete_network(control_socket_dir, i)


def delete_network(control_socket_dir, index):
    shell_execute('%s -p %s -i wlan0 remove_network %s' % (P2P_CLI_PATH, control_socket_dir, index))


def list_existing_networks(iface, control_socket_dir):
    output = shell_execute('%s -p %s -i %s list_network' % (P2P_CLI_PATH, control_socket_dir, iface))
    LOGGER.info('existing networks: %s' % output)
    existing_networks = {}
    for line in output.splitlines(False)[1:]:
        index, ssid, bssid, status = line.split('\t')
        existing_networks[int(index)] = {
            'ssid': ssid,
            'bssid': bssid,
            'status': status
        }
    return existing_networks


def get_wpa_supplicant_control_socket_dir():
    try:
        if not os.path.exists(WPA_SUPPLICANT_CONF_PATH):
            raise Exception('can not find wpa_supplicant.conf')
        with open(WPA_SUPPLICANT_CONF_PATH) as f:
            lines = f.readlines()
        for line in lines:
            line = line.strip()
            if not line:
                continue
            if line.startswith('ctrl_interface='):
                line = line.replace('ctrl_interface=', '')
                parts = line.split(' ')
                for part in parts:
                    if part.startswith('DIR='): # if there is DIR=
                        return part.replace('DIR=', '')
                return line # otherwise just return the ctrl_interface=
        raise Exception('can not find ctrl_interface dir from wpa_supplicant.conf')
    except:
        LOGGER.exception('failed to get wpa_supplicant control socket dir')
        return '/data/misc/wifi/wpa_supplicant'


def netd_execute(command):
    LOGGER.info('send: %s' % command)
    netd_socket.send(command + '\0')
    output = netd_socket.recv(1024)
    LOGGER.info('received: %s' % output)


def shell_execute(command):
    LOGGER.info('execute: %s' % command)
    output = subprocess.check_output(shlex.split(command), stderr=subprocess.STDOUT)
    LOGGER.info('output: %s' % output)
    return output

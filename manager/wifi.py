import os
import logging
import socket
import subprocess
import shlex
import time

import tornado.web
import iptables
import network_interface


LOGGER = logging.getLogger(__name__)
SDIO_DEVICE_PATH = '/sys/bus/sdio/devices/mmc2:0001:1/device'
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
    current_iface = None
    for line in shell_execute('iw dev').splitlines(False):
        line = line.strip()
        if not line:
            continue
        if line.startswith('Interface '):
            current_iface = line.replace('Interface ', '')
            continue
        if 'type AP' in line or 'type P2P-GO' in line:
            return current_iface
    return None


def stop_hotspot(iface):
    iptables.delete_rules(RULES)
    netd_execute('tether stop')
    stop_p2p_persistent_network(get_wpa_supplicant_control_socket_dir(), iface)
    netd_execute('softap fwreload wlan0 STA')
    shell_execute('netcfg wlan0 down')
    shell_execute('netcfg wlan0 up')


def start_hotspot():
    if not os.path.exists(SDIO_DEVICE_PATH):
        raise Exception('wifi chipset unknown: path to sdio device not found')
    with open(SDIO_DEVICE_PATH) as f:
        wifi_chipset = f.read().strip()
        if '0x4330' != wifi_chipset:
            raise Exception('wifi chipset is not supported: %s' % wifi_chipset)
    netd_execute('softap fwreload wlan0 P2P')
    shell_execute('netcfg wlan0 down')
    shell_execute('netcfg wlan0 up')
    time.sleep(1)
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    delete_existing_p2p_persistent_networks(control_socket_dir)
    p2p_persistent_iface = start_p2p_persistent_network(control_socket_dir)
    netd_execute('interface setcfg %s 192.168.49.1 24' % p2p_persistent_iface)
    netd_execute('tether stop')
    netd_execute('tether interface add %s' % p2p_persistent_iface)
    netd_execute('tether start 192.168.49.2 192.168.49.254')
    netd_execute('tether dns set 8.8.8.8')
    enable_ipv4_forward()
    shell_execute('iptables -P FORWARD ACCEPT')
    iptables.insert_rules(RULES)


def enable_ipv4_forward():
    LOGGER.info('enable ipv4 forward')
    with open('/proc/sys/net/ipv4/ip_forward', 'w') as f:
        f.write('1')


def start_p2p_persistent_network(control_socket_dir):
    index = shell_execute('%s -p %s -i wlan0 add_network' % (P2P_CLI_PATH, control_socket_dir)).strip()
    shell_execute('%s -p %s -i wlan0 set_network %s mode 3' % (P2P_CLI_PATH, control_socket_dir, index))
    shell_execute('%s -p %s -i wlan0 set_network %s disabled 2' % (P2P_CLI_PATH, control_socket_dir, index))
    shell_execute('%s -p %s -i wlan0 set_network %s ssid \'"spike"\'' % (P2P_CLI_PATH, control_socket_dir, index))
    shell_execute('%s -p %s -i wlan0 set_network %s key_mgmt WPA-PSK' % (P2P_CLI_PATH, control_socket_dir, index))
    shell_execute('%s -p %s -i wlan0 set_network %s proto RSN' % (P2P_CLI_PATH, control_socket_dir, index))
    shell_execute('%s -p %s -i wlan0 set_network %s pairwise CCMP' % (P2P_CLI_PATH, control_socket_dir, index))
    shell_execute('%s -p %s -i wlan0 set_network %s psk \'"12345678"\'' % (P2P_CLI_PATH, control_socket_dir, index))
    shell_execute('%s -p %s -i wlan0 p2p_group_add persistent=%s' % (P2P_CLI_PATH, control_socket_dir, index))
    time.sleep(1)
    for line in shell_execute('netcfg').splitlines(False):
        if line.startswith('p2p-wlan0'):
            return line.split(' ')[0]
    raise Exception('can not find just started p2p persistent network interface')


def stop_p2p_persistent_network(control_socket_dir, iface):
    shell_execute('%s -p %s -i wlan0 p2p_group_remove %s' % (P2P_CLI_PATH, control_socket_dir, iface))
    time.sleep(1)


def delete_existing_p2p_persistent_networks(control_socket_dir):
    LOGGER.info('delete existing p2p persistent networks')
    existing_networks = list_existing_networks(control_socket_dir)
    for i in sorted(existing_networks.keys(), reverse=True):
        network = existing_networks[i]
        if 'P2P-PERSISTENT' in network['status']:
            delete_network(control_socket_dir, i)


def delete_network(control_socket_dir, index):
    shell_execute('%s -p %s -i wlan0 remove_network %s' % (P2P_CLI_PATH, control_socket_dir, index))


def list_existing_networks(control_socket_dir):
    output = shell_execute('%s -p %s -i wlan0 list_network' % (P2P_CLI_PATH, control_socket_dir))
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
                    if part.startswith('DIR='):
                        return part.replace('DIR=', '')
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

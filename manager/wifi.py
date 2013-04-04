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
IW_PATH = '/data/data/fq.router/wifi-tools/iw'
IWLIST_PATH = '/data/data/fq.router/wifi-tools/iwlist'
netd_socket = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
netd_socket.connect('/dev/socket/netd')
netd_sequence_number = None # turn off by default

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
                try:
                    dump_wifi_status()
                finally:
                    working_hotspot_iface = get_working_hotspot_iface()
                    if working_hotspot_iface:
                        stop_hotspot(working_hotspot_iface)
                    else:
                        netd_execute('softap fwreload wlan0 STA')
                        shell_execute('netcfg wlan0 down')
                        shell_execute('netcfg wlan0 up')
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


def dump_wifi_status():
    shell_execute('netcfg')
    shell_execute('%s phy' % IW_PATH)
    for iface in list_wifi_ifaces():
        try:
            shell_execute('%s %s channel' % (IWLIST_PATH, iface))
        except:
            LOGGER.exception('failed to log iwlist channel')
        try:
            control_socket_dir = get_wpa_supplicant_control_socket_dir()
            shell_execute('%s -p %s -i %s status' % (P2P_CLI_PATH, control_socket_dir, iface))
            shell_execute('%s -p %s -i %s list_network' % (P2P_CLI_PATH, control_socket_dir, iface))
        except:
            LOGGER.exception('failed to log wpa_cli status')
    for pid in os.listdir('/proc'):
        cmdline_path = '/proc/%s/cmdline' % pid
        if os.path.exists(cmdline_path):
            with open(cmdline_path) as f:
                cmdline = f.read()
                if 'supplicant' in cmdline:
                    LOGGER.info('pid %s: %s' % (pid, cmdline))
                    dump_wpa_supplicant(cmdline)


def dump_wpa_supplicant(cmdline):
    pos_start = cmdline.find('-c')
    pos_end = cmdline.find('-', pos_start + 2)
    if -1 != pos_start and -1 != pos_end:
        cfg_path = cmdline[pos_start + 2: pos_end].replace('\0', '')
        cfg_path_exists = os.path.exists(cfg_path) if cfg_path else False
        LOGGER.info('cfg path: %s [%s]' % (cfg_path, cfg_path_exists))
        if cfg_path_exists:
            with open(cfg_path) as f:
                LOGGER.info(f.read())
        dump_wpa_supplicant(cmdline[pos_end:])


def get_working_hotspot_iface():
    ifaces = list_wifi_ifaces()
    for iface, is_hotspot in ifaces.items():
        if is_hotspot:
            return iface
    return None


def list_wifi_ifaces():
    ifaces = {}
    current_iface = None
    for line in shell_execute('%s dev' % IW_PATH).splitlines(False):
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
    try:
        stop_p2p_persistent_network(get_wpa_supplicant_control_socket_dir(), iface)
    except:
        LOGGER.exception('failed to stop p2p persistent network')
    try:
        shell_execute('%s dev %s del' % (IW_PATH, iface))
    except:
        LOGGER.exception('failed to delete wifi interface')
    netd_execute('softap fwreload wlan0 STA')
    shell_execute('netcfg wlan0 down')
    shell_execute('netcfg wlan0 up')
    try:
        shell_execute('killall hostapd')
    except:
        LOGGER.exception('failed to killall hostapd')


def start_hotspot():
    if not os.path.exists(MODALIAS_PATH):
        raise Exception('wifi chipset unknown: %s not found' % MODALIAS_PATH)
    with open(MODALIAS_PATH) as f:
        wifi_chipset = f.read().strip()
        LOGGER.info('wifi chipset: %s' % wifi_chipset)
    if 'platform:wcnss_wlan' == wifi_chipset or wifi_chipset.endswith('4330') or wifi_chipset.endswith('4334'):
    # only tested on sdio:c00v02D0d4330
    # support of bcm4334 is a wild guess
    # support of wcnss_wlan is a wild guess
        hotspot_interface = start_hotspot_on_bcm()
    elif wifi_chipset.endswith('6620') or wifi_chipset.endswith('6628'):
    # only tested on sdio:c00v037Ad6628
    # support of mt6620 is a wild gues
        hotspot_interface = start_hotspot_on_mtk()
    elif 'platform:wl12xx' == wifi_chipset:
        hotspot_interface = start_hotspot_on_wl12xx()
    else:
        raise Exception('wifi chipset is not supported: %s' % wifi_chipset)
    if not get_working_hotspot_iface():
        raise Exception('working hotspot iface not found after start')
    setup_networking(hotspot_interface)


def start_hotspot_on_bcm():
    raise Exception('')
    netd_execute('softap fwreload wlan0 P2P')
    shell_execute('netcfg wlan0 down')
    shell_execute('netcfg wlan0 up')
    time.sleep(1)
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    delete_existing_p2p_persistent_networks('wlan0', control_socket_dir)
    start_p2p_persistent_network('wlan0', control_socket_dir)
    return get_p2p_persistent_iface()


def start_hotspot_on_mtk():
    if 'ap0' not in list_wifi_ifaces():
        netd_execute('softap fwreload wlan0 AP')
        for i in range(5):
            time.sleep(1)
            if 'ap0' in list_wifi_ifaces():
                break
    assert 'ap0' in list_wifi_ifaces()
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    shell_execute('%s -p %s -i ap0 reconfigure' % (P2P_CLI_PATH, control_socket_dir))
    delete_existing_p2p_persistent_networks('ap0', control_socket_dir)
    network_index = start_p2p_persistent_network('ap0', control_socket_dir)
    # restart p2p persistent group otherwise the ssid is not usable
    shell_execute('%s -p %s -i ap0 p2p_group_remove ap0' % (P2P_CLI_PATH, control_socket_dir))
    shell_execute('%s -p %s -i ap0 p2p_group_add persistent=%s' % (P2P_CLI_PATH, control_socket_dir, network_index))
    return 'ap0'


def start_hotspot_on_wl12xx():
    if 'ap0' not in list_wifi_ifaces():
        shell_execute('iw wlan0 interface add ap0 type managed')
    assert 'ap0' in list_wifi_ifaces()
    with open('/data/misc/wifi/fqrouter.conf', 'w') as f:
        f.write(hostapd_template.render(channel=get_upstream_channel() or 1))
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
    return 'ap0'


def get_upstream_channel():
    output = shell_execute('%s wlan0 channel' % IWLIST_PATH)
    for line in output.splitlines():
        line = line.strip()
        if not line:
            continue
        if line.startswith('Current Frequency:'):
            return line.split(' ')[-1][:-1]
    return None


def setup_networking(hotspot_interface):
    netd_execute('interface setcfg %s 192.168.49.1 24' % hotspot_interface)
    netd_execute('tether stop')
    netd_execute('tether interface add %s' % hotspot_interface)
    netd_execute('tether start 192.168.49.2 192.168.49.254')
    netd_execute('tether dns set 8.8.8.8')
    enable_ipv4_forward()
    shell_execute('iptables -P FORWARD ACCEPT')
    iptables.insert_rules(RULES)


def enable_ipv4_forward():
    LOGGER.info('enable ipv4 forward')
    with open('/proc/sys/net/ipv4/ip_forward', 'w') as f:
        f.write('1')


def start_p2p_persistent_network(iface, control_socket_dir):
    index = shell_execute('%s -p %s -i %s add_network' % (P2P_CLI_PATH, control_socket_dir, iface)).strip()

    def set_network(param):
        shell_execute('%s -p %s -i %s set_network %s %s' % (P2P_CLI_PATH, control_socket_dir, iface, index, param))

    set_network('mode 3')
    set_network('disabled 2')
    set_network('ssid \'"spike"\'')
    set_network('key_mgmt WPA-PSK')
    set_network('proto RSN')
    set_network('pairwise CCMP')
    set_network('psk \'"12345678"\'')
    shell_execute('%s -p %s -i %s p2p_group_add persistent=%s' % (P2P_CLI_PATH, control_socket_dir, iface, index))
    time.sleep(1)
    return index


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
    global netd_sequence_number
    if netd_sequence_number:
        netd_sequence_number += 1
        LOGGER.info('send: %s %s' % (netd_sequence_number, command))
        netd_socket.send('%s %s\0' % (netd_sequence_number, command))
    else:
        LOGGER.info('send: %s' % command)
        netd_socket.send('%s\0' % command)
    output = netd_socket.recv(1024)
    LOGGER.info('received: %s' % output)
    if not netd_sequence_number and 'Invalid sequence number' in output:
        LOGGER.info('resend command to netd with sequence number')
        netd_sequence_number = 1
        netd_execute(command)


def shell_execute(command):
    LOGGER.info('execute: %s' % command)
    output = subprocess.check_output(shlex.split(command), stderr=subprocess.STDOUT)
    LOGGER.info('output: %s' % output)
    return output

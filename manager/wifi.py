import os
import logging
import socket
import subprocess
import shlex
import time
import httplib
import re

import tornado.web

import iptables
import network_interface
import hostapd_template


RE_CURRENT_FREQUENCY = re.compile(r'Current Frequency:(\d+\.\d+) GHz \(Channel (\d+)\)')

LOGGER = logging.getLogger(__name__)
MODALIAS_PATH = '/sys/class/net/%s/device/modalias' % network_interface.WIFI_INTERFACE
WPA_SUPPLICANT_CONF_PATH = '/data/misc/wifi/wpa_supplicant.conf'
P2P_SUPPLICANT_CONF_PATH = '/data/misc/wifi/p2p_supplicant.conf'
P2P_CLI_PATH = '/data/data/fq.router/wifi-tools/p2p_cli'
IW_PATH = '/data/data/fq.router/wifi-tools/iw'
IWLIST_PATH = '/data/data/fq.router/wifi-tools/iwlist'
netd_socket = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
netd_socket.connect('/dev/socket/netd')
netd_sequence_number = None # turn off by default
on_wifi_hotspot_started = None

RULES = []
for iface in network_interface.list_data_network_interfaces():
    RULES.append((
        {'target': 'MASQUERADE', 'iface_out': iface, 'source': '0.0.0.0/0', 'destination': '0.0.0.0/0'},
        ('nat', 'POSTROUTING', '-o %s -j MASQUERADE' % iface)
    ))


def clean():
    stop_hotspot()


class WifiStartHandler(tornado.web.RequestHandler):
    def post(self):
        success, message = start_hotspot()
        if not success:
            self.set_status(httplib.INTERNAL_SERVER_ERROR)
        self.write(message)


class WifiStopHandler(tornado.web.RequestHandler):
    def post(self):
        self.write(stop_hotspot())


class WifiIsStartedHandler(tornado.web.RequestHandler):
    def get(self):
        if get_working_hotspot_iface():
            self.write('TRUE')
        else:
            self.write('FALSE')


def stop_hotspot():
    try:
        try:
            working_hotspot_iface = get_working_hotspot_iface()
            if working_hotspot_iface:
                stop_hotspot_interface(working_hotspot_iface)
                return 'hotspot stopped successfully'
            else:
                return 'hotspot has not been started yet'
        finally:
            try:
                control_socket_dir = get_wpa_supplicant_control_socket_dir()
                delete_existing_p2p_persistent_networks(network_interface.WIFI_INTERFACE, control_socket_dir)
            except:
                LOGGER.exception('failed to delete existing p2p persistent networks')
    except:
        LOGGER.exception('failed to stop hotspot')
        return 'failed to stop hotspot'


def start_hotspot():
    try:
        iptables.delete_rules(RULES)
        working_hotspot_iface = get_working_hotspot_iface()
        if working_hotspot_iface:
            return True, 'hotspot is already working, start skipped'
        else:
            LOGGER.info('=== Before Starting Hotspot ===')
            dump_wifi_status()
            LOGGER.info('=== Start Hotspot ===')
            wifi_chipset = get_wifi_chipset()
            hotspot_interface = start_hotspot_interface(wifi_chipset)
            setup_networking(hotspot_interface)
            LOGGER.info('=== Started Hotspot ===')
            dump_wifi_status()
            if on_wifi_hotspot_started:
                on_wifi_hotspot_started()
            return True, 'hotspot started successfully'
    except:
        LOGGER.exception('failed to start hotspot')
        try:
            LOGGER.error('=== Failed to Start Hotspot ===')
            dump_wifi_status()
        finally:
            stop_hotspot()
        return False, 'failed to start hotspot'


def dump_wifi_status():
    try:
        shell_execute('netcfg')
        get_wifi_chipset()
        shell_execute('%s phy' % IW_PATH)
        for iface in list_wifi_ifaces():
            try:
                shell_execute('%s %s channel' % (IWLIST_PATH, iface))
            except:
                LOGGER.exception('failed to log iwlist channel')
            try:
                if 'p2p' in iface or 'ap0' == iface:
                    control_socket_dir = get_p2p_supplicant_control_socket_dir()
                else:
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
        if os.path.exists(P2P_SUPPLICANT_CONF_PATH):
            LOGGER.info('content of %s: ' % P2P_SUPPLICANT_CONF_PATH)
            with open(P2P_SUPPLICANT_CONF_PATH) as f:
                LOGGER.info(f.read())
        if os.path.exists(WPA_SUPPLICANT_CONF_PATH):
            LOGGER.info('content of %s: ' % WPA_SUPPLICANT_CONF_PATH)
            with open(WPA_SUPPLICANT_CONF_PATH) as f:
                LOGGER.info(f.read())
        dump_dir('/dev/socket')
        try:
            dump_unix_sockets()
        except:
            LOGGER.exception('failed to dump unix sockets')
    except:
        LOGGER.exception('failed to dump wifi status')


def dump_wpa_supplicant(cmdline):
    pos_start = cmdline.find('-c')
    pos_end = cmdline.find('\0', pos_start + 2)
    if -1 != pos_start and -1 != pos_end:
        cfg_path = cmdline[pos_start + 2: pos_end].replace('\0', '')
        cfg_path_exists = os.path.exists(cfg_path) if cfg_path else False
        LOGGER.info('cfg path: %s [%s]' % (cfg_path, cfg_path_exists))
        if cfg_path_exists:
            with open(cfg_path) as f:
                content = f.read()
                LOGGER.info(content)
            control_socket_dir = parse_wpa_supplicant_conf(content)
            LOGGER.info('parsed control socket dir: %s' % control_socket_dir)
            dump_dir(control_socket_dir)
        dump_wpa_supplicant(cmdline[pos_end:])


def dump_dir(dir):
    if os.path.exists(dir):
        LOGGER.info('dump %s: %s' % (dir, os.listdir(dir)))
    else:
        LOGGER.error('dir %s does not exist' % dir)


def dump_unix_sockets():
    LOGGER.info('dump unix sockets')
    with open('/proc/net/unix') as f:
        lines = f.readlines()
    for line in lines:
        line = line.strip()
        if not line:
            continue
        if '/' in line:
            LOGGER.info(line)


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


def stop_hotspot_interface(iface):
    netd_execute('tether stop')
    try:
        shell_execute('%s dev %s del' % (IW_PATH, iface))
    except:
        LOGGER.exception('failed to delete wifi interface')
    netd_execute('softap fwreload %s STA' % network_interface.WIFI_INTERFACE)
    shell_execute('netcfg %s down' % network_interface.WIFI_INTERFACE)
    shell_execute('netcfg %s up' % network_interface.WIFI_INTERFACE)
    try:
        shell_execute('killall hostapd')
    except:
        LOGGER.exception('failed to killall hostapd')


def start_hotspot_interface(wifi_chipset):
    try:
        shell_execute('start p2p_supplicant')
    except:
        LOGGER.exception('failed to start p2p_supplicant')
    if wifi_chipset.endswith('4330') or wifi_chipset.endswith('4334') or wifi_chipset.endswith('4324'):
    # only tested on sdio:c00v02D0d4330
    # support of bcm43241(4324) is a wild guess
    # support of bcm4334(4334) is a wild guess
        hotspot_interface = start_hotspot_on_bcm()
    elif 'platform:wcnss_wlan' == wifi_chipset:
        hotspot_interface = start_hotspot_on_wcnss()
    elif wifi_chipset.endswith('6620') or wifi_chipset.endswith('6628'):
    # only tested on sdio:c00v037Ad6628
    # support of mt6620 is a wild gues
        hotspot_interface = start_hotspot_on_mtk()
    elif 'platform:wl12xx' == wifi_chipset or wifi_chipset.endswith('0301'):
    # ar6003 is c00v0271d0301
        hotspot_interface = start_hotspot_on_wl12xx()
    else:
        raise Exception('wifi chipset is not supported: %s' % wifi_chipset)
    if not get_working_hotspot_iface():
        raise Exception('working hotspot iface not found after start')
    return hotspot_interface


def get_wifi_chipset():
    mediatek_wifi_chipset = get_mediatek_wifi_chipset()
    if mediatek_wifi_chipset:
        return mediatek_wifi_chipset
    if not os.path.exists(MODALIAS_PATH):
        raise Exception('wifi chipset unknown: %s not found' % MODALIAS_PATH)
    with open(MODALIAS_PATH) as f:
        wifi_chipset = f.read().strip()
        LOGGER.info('wifi chipset: %s' % wifi_chipset)
        return wifi_chipset


def get_mediatek_wifi_chipset():
    try:
        return shell_execute('getprop mediatek.wlan.chip').strip()
    except:
        LOGGER.exception('failed to get mediatek wifi chipset')
        return None


def start_hotspot_on_bcm():
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    log_upstream_wifi_status('before load p2p firmware', control_socket_dir)
    netd_execute('softap fwreload %s P2P' % network_interface.WIFI_INTERFACE)
    shell_execute('netcfg %s down' % network_interface.WIFI_INTERFACE)
    shell_execute('netcfg %s up' % network_interface.WIFI_INTERFACE)
    time.sleep(1)
    log_upstream_wifi_status('after loaded p2p firmware', control_socket_dir)
    if 'p2p0' in list_wifi_ifaces():
    # bcmdhd can optionally have p2p0 interface
        LOGGER.info('start p2p persistent group using p2p0')
        shell_execute('netcfg p2p0 up')
        p2p_control_socket_dir = get_p2p_supplicant_control_socket_dir()
        delete_existing_p2p_persistent_networks('p2p0', p2p_control_socket_dir)
        start_p2p_persistent_network('p2p0', p2p_control_socket_dir)
        p2p_persistent_iface = get_p2p_persistent_iface()
        log_upstream_wifi_status('after p2p persistent group created', control_socket_dir)
        return p2p_persistent_iface
    else:
        LOGGER.info('start p2p persistent group using %s' % network_interface.WIFI_INTERFACE)
        delete_existing_p2p_persistent_networks(network_interface.WIFI_INTERFACE, control_socket_dir)
        start_p2p_persistent_network(network_interface.WIFI_INTERFACE, control_socket_dir)
        p2p_persistent_iface = get_p2p_persistent_iface()
        log_upstream_wifi_status('after p2p persistent group created', control_socket_dir)
        return p2p_persistent_iface


def start_hotspot_on_wcnss():
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    log_upstream_wifi_status('before load p2p firmware', control_socket_dir)
    netd_execute('softap fwreload %s P2P' % network_interface.WIFI_INTERFACE)
    shell_execute('netcfg %s down' % network_interface.WIFI_INTERFACE)
    shell_execute('netcfg %s up' % network_interface.WIFI_INTERFACE)
    time.sleep(1)
    log_upstream_wifi_status('after loaded p2p firmware', control_socket_dir)
    reset_p2p_channels(network_interface.WIFI_INTERFACE, control_socket_dir)
    reset_p2p_channels('p2p0', get_p2p_supplicant_control_socket_dir())
    delete_existing_p2p_persistent_networks(network_interface.WIFI_INTERFACE, control_socket_dir)
    start_p2p_persistent_network(network_interface.WIFI_INTERFACE, control_socket_dir)
    p2p_persistent_iface = get_p2p_persistent_iface()
    log_upstream_wifi_status('after p2p persistent group created', control_socket_dir)
    return p2p_persistent_iface


def load_ap_firmware():
    for i in range(3):
        if 'ap0' not in list_wifi_ifaces():
            netd_execute('softap fwreload %s AP' % network_interface.WIFI_INTERFACE)
            for i in range(5):
                time.sleep(1)
                if 'ap0' in list_wifi_ifaces():
                    return


def start_hotspot_on_mtk():
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    log_upstream_wifi_status('before load ap firmware', control_socket_dir)
    load_ap_firmware()
    assert 'ap0' in list_wifi_ifaces()
    log_upstream_wifi_status('after loaded ap firmware', control_socket_dir)
    shell_execute('%s -p %s -i ap0 reconfigure' % (P2P_CLI_PATH, control_socket_dir))
    delete_existing_p2p_persistent_networks('ap0', control_socket_dir)
    network_index = start_p2p_persistent_network('ap0', control_socket_dir)
    # restart p2p persistent group otherwise the ssid is not usable
    shell_execute('%s -p %s -i ap0 p2p_group_remove ap0' % (P2P_CLI_PATH, control_socket_dir))
    shell_execute('%s -p %s -i ap0 p2p_group_add persistent=%s' % (P2P_CLI_PATH, control_socket_dir, network_index))
    log_upstream_wifi_status('after p2p persistent group created', control_socket_dir)
    return 'ap0'


def start_hotspot_on_wl12xx():
    if 'ap0' not in list_wifi_ifaces():
        shell_execute('iw %s interface add ap0 type managed' % network_interface.WIFI_INTERFACE)
    assert 'ap0' in list_wifi_ifaces()
    with open('/data/misc/wifi/fqrouter.conf', 'w') as f:
        frequency, channel = get_upstream_frequency_and_channel()
        f.write(hostapd_template.render(channel=channel or 1))
    LOGGER.info('start hostapd')
    proc = subprocess.Popen(
        ['/data/data/fq.router/wifi-tools/hostapd', '-dd', '/data/misc/wifi/fqrouter.conf'],
        cwd='/data/misc/wifi', stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    time.sleep(2)
    if proc.poll():
        LOGGER.error('hostapd failed')
        LOGGER.error(proc.stdout.read())
        raise Exception('hostapd failed')
    else:
        LOGGER.info('hostapd seems like started successfully')
    return 'ap0'


def get_upstream_frequency_and_channel():
    try:
        output = shell_execute('%s %s channel' % (IWLIST_PATH, network_interface.WIFI_INTERFACE))
        for line in output.splitlines():
            line = line.strip()
            if not line:
                continue
            match = RE_CURRENT_FREQUENCY.match(line)
            if match:
                return match.group(1), match.group(2)
        return None, None
    except:
        LOGGER.exception('failed to get upstream frequency and channel')
        return None, None


def setup_networking(hotspot_interface):
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    netd_execute('interface setcfg %s 192.168.49.1 24' % hotspot_interface)
    netd_execute('tether stop')
    netd_execute('tether interface add %s' % hotspot_interface)
    netd_execute('tether start 192.168.49.2 192.168.49.254')
    log_upstream_wifi_status('after tether started', control_socket_dir)
    netd_execute('tether dns set 8.8.8.8')
    enable_ipv4_forward()
    shell_execute('iptables -P FORWARD ACCEPT')
    iptables.insert_rules(RULES)
    log_upstream_wifi_status('after setup networking', control_socket_dir)


def enable_ipv4_forward():
    LOGGER.info('enable ipv4 forward')
    with open('/proc/sys/net/ipv4/ip_forward', 'w') as f:
        f.write('1')


def log_upstream_wifi_status(log, control_socket_dir):
    try:
        LOGGER.info('=== %s ===' % log)
        shell_execute('%s -p %s -i %s status' % (P2P_CLI_PATH, control_socket_dir, network_interface.WIFI_INTERFACE))
        shell_execute('netcfg')
    except:
        LOGGER.exception('failed to log upstream wifi status')


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
    frequency, channel = get_upstream_frequency_and_channel()
    if frequency:
        shell_execute('%s -p %s -i %s p2p_group_add persistent=%s freq=%s ' %
                      (P2P_CLI_PATH, control_socket_dir, iface, index, frequency.replace('.', '')))
    else:
        shell_execute('%s -p %s -i %s p2p_group_add persistent=%s' % (P2P_CLI_PATH, control_socket_dir, iface, index))
    time.sleep(1)
    return index


def get_p2p_persistent_iface():
    for line in shell_execute('netcfg').splitlines(False):
        if line.startswith('p2p-'):
            return line.split(' ')[0]
    raise Exception('can not find just started p2p persistent network interface')


def stop_p2p_persistent_network(control_socket_dir, control_iface, iface):
    try:
        shell_execute(
            '%s -p %s -i %s p2p_group_remove %s' %
            (P2P_CLI_PATH, control_socket_dir, control_iface, iface))
    except:
        LOGGER.error('failed to stop p2p persistent network')


def reset_p2p_channels(iface, control_socket_dir):
    try:
        frequency, channel = get_upstream_frequency_and_channel()
        channel = channel or 6
        shell_execute('%s -p %s -i %s set p2p_oper_channel %s' % (P2P_CLI_PATH, control_socket_dir, iface, channel))
        shell_execute('%s -p %s -i %s set p2p_oper_reg_class 81' % (P2P_CLI_PATH, control_socket_dir, iface))
        shell_execute('%s -p %s -i %s set p2p_listen_channel %s' % (P2P_CLI_PATH, control_socket_dir, iface, channel))
        shell_execute('%s -p %s -i %s set p2p_listen_reg_class 81' % (P2P_CLI_PATH, control_socket_dir, iface))
        shell_execute('%s -p %s -i %s save_config' % (P2P_CLI_PATH, control_socket_dir, iface))
    except:
        LOGGER.exception('failed to reset p2p channels')


def delete_existing_p2p_persistent_networks(iface, control_socket_dir):
    LOGGER.info('delete existing p2p persistent networks')
    existing_networks = list_existing_networks(iface, control_socket_dir)
    for i in sorted(existing_networks.keys(), reverse=True):
        network = existing_networks[i]
        if 'P2P-PERSISTENT' in network['status']:
            delete_network(iface, control_socket_dir, i)


def delete_network(iface, control_socket_dir, index):
    shell_execute(
        '%s -p %s -i %s remove_network %s' %
        (P2P_CLI_PATH, control_socket_dir, iface, index))


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


def get_p2p_supplicant_control_socket_dir():
    try:
        return get_wpa_supplicant_control_socket_dir(P2P_SUPPLICANT_CONF_PATH)
    except:
        LOGGER.exception('failed to get p2p supplicant control socket dir')
        return get_wpa_supplicant_control_socket_dir()


def get_wpa_supplicant_control_socket_dir(conf_path=WPA_SUPPLICANT_CONF_PATH):
    try:
        if not os.path.exists(conf_path):
            raise Exception('can not find wpa_supplicant.conf')
        with open(conf_path) as f:
            content = f.read()
        control_socket_dir = parse_wpa_supplicant_conf(content)
        if WPA_SUPPLICANT_CONF_PATH == conf_path:
            control_socket_dir = fix_wrong_control_socket_dir(control_socket_dir)
        if control_socket_dir:
            return control_socket_dir
        else:
            raise Exception('can not find ctrl_interface dir from wpa_supplicant.conf')
    except:
        LOGGER.exception('failed to get wpa_supplicant control socket dir')
        return '/data/misc/wifi/wpa_supplicant'


def fix_wrong_control_socket_dir(control_socket_dir):
    if control_socket_dir == network_interface.WIFI_INTERFACE:
        return 'anydir'
    if control_socket_dir:
        control_socket_not_exists = not os.path.exists(
            os.path.join(control_socket_dir, network_interface.WIFI_INTERFACE))
    else:
        control_socket_not_exists = True
    dev_socket_exists = os.path.exists('/dev/socket/wpa_%s' % network_interface.WIFI_INTERFACE)
    if dev_socket_exists and control_socket_not_exists:
        return 'anydir' # any valid dir will cause wpa_cli fail
    return control_socket_dir


def parse_wpa_supplicant_conf(content):
    for line in content.splitlines():
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
    return None


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
    try:
        output = subprocess.check_output(shlex.split(command), stderr=subprocess.STDOUT)
        LOGGER.info('succeed, output: %s' % output)
    except subprocess.CalledProcessError, e:
        LOGGER.error('failed, output: %s' % e.output)
        raise
    return output

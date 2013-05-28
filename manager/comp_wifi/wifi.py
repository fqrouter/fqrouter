import os
import logging
import socket
import re
import traceback
import shlex

from gevent import subprocess
import gevent

from utils import iptables
import hostapd_template


try:
    WIFI_INTERFACE = subprocess.check_output(['getprop', 'wifi.interface']).strip() or 'wlan0'
except:
    traceback.print_exc()
    WIFI_INTERFACE = 'wlan0'

RE_CURRENT_FREQUENCY = re.compile(r'Current Frequency:(\d+\.\d+) GHz \(Channel (\d+)\)')
RE_FREQ = re.compile(r'freq: (\d+)')
RE_IFCONFIG_IP = re.compile(r'inet addr:(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})')
RE_MAC_ADDRESS = re.compile(r'[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+')
RE_DEFAULT_GATEWAY_IFACE = re.compile('default via .+ dev (.+)')

LOGGER = logging.getLogger('wifi')
MODALIAS_PATH = '/sys/class/net/%s/device/modalias' % WIFI_INTERFACE
WPA_SUPPLICANT_CONF_PATH = '/data/misc/wifi/wpa_supplicant.conf'
P2P_SUPPLICANT_CONF_PATH = '/data/misc/wifi/p2p_supplicant.conf'
P2P_CLI_PATH = '/data/data/fq.router/wifi-tools/p2p_cli'
IW_PATH = '/data/data/fq.router/wifi-tools/iw'
HOSTAPD_PATH = '/data/data/fq.router/wifi-tools/hostapd'
IWCONFIG_PATH = '/data/data/fq.router/wifi-tools/iwconfig'
IWLIST_PATH = '/data/data/fq.router/wifi-tools/iwlist'
DNSMASQ_PATH = '/data/data/fq.router/wifi-tools/dnsmasq'
KILLALL_PATH = '/data/data/fq.router/busybox killall'
IFCONFIG_PATH = '/data/data/fq.router/busybox ifconfig'
IP_PATH = '/data/data/fq.router/busybox ip'
FQROUTER_HOSTAPD_CONF_PATH = '/data/data/fq.router/hostapd.conf'
CHANNELS = {
    '2412': 1, '2417': 2, '2422': 3, '2427': 4, '2432': 5, '2437': 6, '2442': 7,
    '2447': 8, '2452': 9, '2457': 10, '2462': 11, '2467': 12, '2472': 13, '2484': 14,
    '5180': 36, '5200': 40, '5220': 44, '5240': 48, '5260': 52, '5280': 56, '5300': 60,
    '5320': 64, '5500': 100, '5520': 104, '5540': 108, '5560': 112, '5580': 116,
    '5600': 120, '5620': 124, '5640': 128, '5660': 132, '5680': 136, '5700': 140,
    '5745': 149, '5765': 153, '5785': 157, '5805': 161, '5825': 165
}
netd_sequence_number = None # turn off by default

RULES = [
    (
        {'target': 'MASQUERADE', 'source': '10.24.1.0/24', 'destination': '0.0.0.0/0'},
        ('nat', 'POSTROUTING', '-s 10.24.1.0/24 -j MASQUERADE')
    ), (
        {'target': 'MASQUERADE', 'source': '10.1.2.3', 'destination': '0.0.0.0/0'},
        ('nat', 'POSTROUTING', '-s 10.1.2.3 -j MASQUERADE')
    )]


def stop_hotspot():
    try:
        working_hotspot_iface = get_working_hotspot_iface()
        try:
            shell_execute('%s dnsmasq' % KILLALL_PATH)
        except:
            LOGGER.exception('failed to killall dnsmasq')
        try:
            shell_execute('%s hostapd' % KILLALL_PATH)
        except:
            LOGGER.exception('failed to killall hostapd')
        try:
            control_socket_dir = get_p2p_supplicant_control_socket_dir()
            stop_p2p_persistent_network(control_socket_dir, 'p2p0', 'p2p0')
            stop_p2p_persistent_network(control_socket_dir, 'p2p0', working_hotspot_iface)
            delete_existing_p2p_persistent_networks('p2p0', control_socket_dir)
            shell_execute('%s -p %s -i %s save_config' % (P2P_CLI_PATH, control_socket_dir, 'p2p0'))
        except:
            LOGGER.exception('failed to delete existing p2p persistent networks')
        try:
            control_socket_dir = get_wpa_supplicant_control_socket_dir()
            stop_p2p_persistent_network(control_socket_dir, WIFI_INTERFACE, WIFI_INTERFACE)
            stop_p2p_persistent_network(control_socket_dir, working_hotspot_iface, working_hotspot_iface)
            delete_existing_p2p_persistent_networks(WIFI_INTERFACE, control_socket_dir)
            shell_execute('%s -p %s -i %s save_config' % (P2P_CLI_PATH, control_socket_dir, WIFI_INTERFACE))
        except:
            LOGGER.exception('failed to delete existing p2p persistent networks')
        try:
            netd_execute('softap fwreload %s STA' % WIFI_INTERFACE)
            shell_execute('netcfg %s down' % WIFI_INTERFACE)
            shell_execute('netcfg %s up' % WIFI_INTERFACE)
        except:
            LOGGER.exception('failed to reload STA firmware')
        if working_hotspot_iface:
            try:
                shell_execute('%s dev %s del' % (IW_PATH, working_hotspot_iface))
            except:
                LOGGER.exception('failed to delete wifi interface')
            return 'hotspot stopped successfully'
        else:
            return 'hotspot has not been started yet'
    except:
        LOGGER.exception('failed to stop hotspot')
        return 'failed to stop hotspot'


def start_hotspot(ssid, password):
    try:
        working_hotspot_iface = get_working_hotspot_iface()
        if working_hotspot_iface:
            return True, 'hotspot is already working, start skipped'
        else:
            LOGGER.info('=== Before Starting Hotspot ===')
            dump_wifi_status()
            LOGGER.info('=== Start Hotspot ===')
            wifi_chipset_family, wifi_chipset_model = get_wifi_chipset()
            LOGGER.info('chipset is: %s %s' % (wifi_chipset_family, wifi_chipset_model))
            if 'unsupported' == wifi_chipset_family:
                return False, 'wifi chipset [%s] is not supported' % wifi_chipset_model
            hotspot_interface = start_hotspot_interface(wifi_chipset_family, ssid, password)
            setup_networking(hotspot_interface)
            LOGGER.info('=== Started Hotspot ===')
            dump_wifi_status()
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
                shell_execute('%s dev %s link' % (IW_PATH, iface))
            except:
                LOGGER.exception('failed to log iw dev link')
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
        try:
            dump_unix_sockets()
        except:
            LOGGER.exception('failed to dump unix sockets')
    except:
        LOGGER.exception('failed to dump wifi status')


def dump_wpa_supplicant(cmdline):
    pos_start = cmdline.find('-c')
    if -1 == pos_start:
        return
    pos_end = cmdline.find('\0', pos_start + 2)
    if -1 == pos_end:
        return
    cfg_path = cmdline[pos_start + 2: pos_end].replace('\0', '')
    cfg_path_exists = os.path.exists(cfg_path) if cfg_path else False
    LOGGER.info('cfg path: %s [%s]' % (cfg_path, cfg_path_exists))
    if cfg_path_exists:
        with open(cfg_path) as f:
            content = f.read()
            for line in content.splitlines():
                if 'psk=' in line:
                    continue
                LOGGER.info(line)
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
    hotspot_iface = get_p2p_persistent_iface() or \
           get_working_hotspot_iface_using_nl80211() or \
           get_working_hotspot_iface_using_wext()
    if WIFI_INTERFACE == hotspot_iface:
        return None
    else:
        return hotspot_iface


def get_working_hotspot_iface_using_wext():
    try:
        if 'Mode:Master' in shell_execute('%s %s' % (IWCONFIG_PATH, 'wl0.1')):
            return 'wl0.1'
        if 'Mode:Master' in shell_execute('%s %s' % (IWCONFIG_PATH, WIFI_INTERFACE)):
            return WIFI_INTERFACE
        return None
    except:
        LOGGER.exception('failed to get working hotspot iface using wext')
        return None


def get_working_hotspot_iface_using_nl80211():
    try:
        ifaces = list_wifi_ifaces()
        for iface, is_hotspot in ifaces.items():
            if is_hotspot:
                return iface
        return None
    except:
        LOGGER.exception('failed to get working hotspot iface using nl80211')
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


def start_hotspot_interface(wifi_chipset_family, ssid, password):
    try:
        shell_execute('start p2p_supplicant')
    except:
        LOGGER.exception('failed to start p2p_supplicant')
    if 'bcm' == wifi_chipset_family:
        start_hotspot_on_bcm(ssid, password)
    elif 'wcnss' == wifi_chipset_family:
        start_hotspot_on_wcnss(ssid, password)
    elif 'wl12xx' == wifi_chipset_family:
        start_hotspot_on_wl12xx(ssid, password)
    elif 'mtk' == wifi_chipset_family:
        start_hotspot_on_mtk(ssid, password)
    else:
        raise Exception('wifi chipset family %s is not supported: %s' % wifi_chipset_family)
    hotspot_interface = get_working_hotspot_iface()
    if not hotspot_interface:
        try:
            shell_execute('logcat -d -v time -s wpa_supplicant:V')
        except:
            LOGGER.exception('failed to log wpa_supplicant')
        try:
            shell_execute('logcat -d -v time -s p2p_supplicant:V')
        except:
            LOGGER.exception('failed to log p2p_supplicant')
        raise Exception('working hotspot iface not found after start')
    return hotspot_interface


def wait_for_upstream_wifi_network_connected():
    for i in range(5):
        gevent.sleep(1)
        if get_ip_and_mac(WIFI_INTERFACE)[0]:
            return True
        try:
            default_gateway_iface = get_default_gateway_iface()
            if default_gateway_iface \
                and 'p2p' not in default_gateway_iface \
                and 'ap0' not in default_gateway_iface \
                and WIFI_INTERFACE not in default_gateway_iface:
                shell_execute('netcfg %s down' % default_gateway_iface)
                shell_execute('netcfg %s up' % default_gateway_iface)
        except:
            LOGGER.exception('failed to toggle 3G interface to force wlan reconnect')
    return False


def get_default_gateway_iface():
    try:
        output = shell_execute('%s route' % IP_PATH)
        match = RE_DEFAULT_GATEWAY_IFACE.search(output)
        if match:
            return match.group(1)
    except:
        LOGGER.exception('failed to get default gateway interface')


def get_ip_and_mac(ifname):
    try:
        output = shell_execute('%s %s' % (IFCONFIG_PATH, ifname)).lower()
        match = RE_MAC_ADDRESS.search(output)
        if match:
            mac = match.group(0)
        else:
            mac = None
        match = RE_IFCONFIG_IP.search(output)
        if match:
            ip = match.group(1)
        else:
            ip = None
        return ip, mac
    except:
        LOGGER.exception('failed to get ip and mac: %s' % ifname)
        return None, None


def get_wifi_chipset():
    chipset = get_mediatek_wifi_chipset() or get_wifi_modalias()
    if chipset:
        if chipset.endswith('4330'):
            return 'bcm', '4330'
        if chipset.endswith('4334'):
            return 'bcm', '4334'
        if chipset.endswith('4324'):
            return 'bcm', '4324' # 43241
        if chipset.endswith('4076'): # sdio:c00v0097d4076
            return 'wl12xx', 'unknown'
        if 'platform:wcnss_wlan' == chipset:
            return 'wcnss', 'unknown'
        if 'platform:wl12xx' == chipset:
            return 'wl12xx', 'unknown'
        if chipset.endswith('6620'):
            return 'mtk', '6620'
        if chipset.endswith('6628'):
            return 'mtk', '6628'
    else:
        if shell_execute('getprop ro.mediatek.platform').strip():
            return 'mtk', 'unknown'
        if os.path.exists('/sys/module/bcmdhd'):
            return 'bcm', 'unknown'
        if os.path.exists('/sys/module/wl12xx'):
            return 'wl12xx', 'unknown'
    return 'unsupported', chipset


def get_wifi_modalias():
    if not os.path.exists(MODALIAS_PATH):
        LOGGER.warn('wifi chipset unknown: %s not found' % MODALIAS_PATH)
        return ''
    with open(MODALIAS_PATH) as f:
        wifi_chipset = f.read().strip()
        LOGGER.info('wifi chipset: %s' % wifi_chipset)
        return wifi_chipset


def get_mediatek_wifi_chipset():
    try:
        return shell_execute('getprop mediatek.wlan.chip').strip()
    except:
        LOGGER.exception('failed to get mediatek wifi chipset')
        return ''


def start_hotspot_on_bcm(ssid, password):
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    load_p2p_firmware(control_socket_dir)
    if 'p2p0' in list_wifi_ifaces():
    # bcmdhd can optionally have p2p0 interface
        LOGGER.info('start p2p persistent group using p2p0')
        shell_execute('netcfg p2p0 up')
        p2p_control_socket_dir = get_p2p_supplicant_control_socket_dir()
        delete_existing_p2p_persistent_networks('p2p0', p2p_control_socket_dir)
        start_p2p_persistent_network('p2p0', p2p_control_socket_dir, ssid, password, sets_channel=True)
        log_upstream_wifi_status('after p2p persistent group created', control_socket_dir)
    else:
        LOGGER.info('start p2p persistent group using %s' % WIFI_INTERFACE)
        delete_existing_p2p_persistent_networks(WIFI_INTERFACE, control_socket_dir)
        start_p2p_persistent_network(WIFI_INTERFACE, control_socket_dir, ssid, password, sets_channel=True)
        log_upstream_wifi_status('after p2p persistent group created', control_socket_dir)


def load_p2p_firmware(control_socket_dir):
    was_using_wifi_network = get_ip_and_mac(WIFI_INTERFACE)[0]
    log_upstream_wifi_status('before load p2p firmware', control_socket_dir)
    netd_execute('softap fwreload %s P2P' % WIFI_INTERFACE)
    reset_wifi_interface()
    log_upstream_wifi_status('after loaded p2p firmware', control_socket_dir)
    if was_using_wifi_network:
        if wait_for_upstream_wifi_network_connected():
            LOGGER.info('wifi reconnected')
        else:
            LOGGER.error('wifi failed to reconnect')


def reset_wifi_interface():
    shell_execute('netcfg %s down' % WIFI_INTERFACE)
    shell_execute('netcfg %s up' % WIFI_INTERFACE)
    gevent.sleep(1)


def start_hotspot_on_wcnss(ssid, password):
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    load_p2p_firmware(control_socket_dir)
    delete_existing_p2p_persistent_networks(WIFI_INTERFACE, control_socket_dir)
    start_p2p_persistent_network(WIFI_INTERFACE, control_socket_dir, ssid, password, sets_channel=True)
    log_upstream_wifi_status('after p2p persistent group created', control_socket_dir)


def load_ap_firmware():
    for i in range(3):
        try:
            shell_execute('%s ap0' % IFCONFIG_PATH)
            return
        except:
            pass
        netd_execute('softap fwreload %s AP' % WIFI_INTERFACE)
        for i in range(5):
            gevent.sleep(1)
            try:
                shell_execute('%s ap0' % IFCONFIG_PATH)
                return
            except:
                pass
    raise Exception('failed to start ap0 interface')


def start_hotspot_on_mtk(ssid, password):
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    log_upstream_wifi_status('before load ap firmware', control_socket_dir)
    load_ap_firmware()
    shell_execute('netcfg ap0 up')
    gevent.sleep(2)
    log_upstream_wifi_status('after loaded ap firmware', control_socket_dir)
    if 'ap0' == get_working_hotspot_iface_using_nl80211():
        return
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    shell_execute('%s -p %s -i ap0 reconfigure' % (P2P_CLI_PATH, control_socket_dir))
    delete_existing_p2p_persistent_networks('ap0', control_socket_dir)
    network_index = start_p2p_persistent_network('ap0', control_socket_dir, ssid, password, sets_channel=True)
    # restart p2p persistent group otherwise the ssid is not usable
    shell_execute('%s -p %s -i ap0 p2p_group_remove ap0' % (P2P_CLI_PATH, control_socket_dir))
    shell_execute('%s -p %s -i ap0 p2p_group_add persistent=%s' % (P2P_CLI_PATH, control_socket_dir, network_index))
    log_upstream_wifi_status('after p2p persistent group created', control_socket_dir)


def start_hotspot_on_wl12xx(ssid, password):
    if 'ap0' not in list_wifi_ifaces():
        shell_execute(
            '%s %s interface add ap0 type managed' % (IW_PATH, WIFI_INTERFACE))
    assert 'ap0' in list_wifi_ifaces()
    shell_execute('netcfg ap0 up')
    with open(FQROUTER_HOSTAPD_CONF_PATH, 'w') as f:
        frequency, channel = get_upstream_frequency_and_channel()
        f.write(hostapd_template.render(WIFI_INTERFACE, channel=channel or 1, ssid=ssid, password=password))
    os.chmod(FQROUTER_HOSTAPD_CONF_PATH, 0666)
    try:
        shell_execute('%s hostapd' % KILLALL_PATH)
    except:
        LOGGER.exception('failed to killall hostapd')
    LOGGER.info('start hostapd')
    if start_hostapd():
        return
    shell_execute('%s dev ap0 del' % IW_PATH)
    LOGGER.info('try start hostapd without ap0')
    if not start_hostapd():
        raise Exception('failed to start hotspot on wl12xx')


def start_hostapd():
    proc = subprocess.Popen(
        [HOSTAPD_PATH, '-dd', FQROUTER_HOSTAPD_CONF_PATH],
        cwd='/data/misc/wifi', stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    gevent.sleep(2)
    if proc.poll():
        LOGGER.error('hostapd failed: %s' % str(proc.communicate()))
        shell_execute('logcat -d -v time -s hostapd:V')
        return False
    else:
        LOGGER.info('hostapd seems like started successfully')
        return True


def get_upstream_frequency_and_channel():
    try:
        output = shell_execute('%s %s channel' % (IWLIST_PATH, WIFI_INTERFACE))
        for line in output.splitlines():
            line = line.strip()
            if not line:
                continue
            match = RE_CURRENT_FREQUENCY.search(line)
            if match:
                return match.group(1), match.group(2)
        frequency = get_upstream_frequency()
        return frequency, CHANNELS.get(frequency)
    except:
        LOGGER.exception('failed to get upstream frequency and channel')
        return None, None


def get_upstream_frequency():
    try:
        output = shell_execute('%s dev %s link' % (IW_PATH, WIFI_INTERFACE))
        for line in output.splitlines():
            if not line:
                continue
            match = RE_FREQ.search(line)
            if match:
                return match.group(1)
        return None
    except:
        LOGGER.exception('failed to get upstream frequency')
        return None


def setup_networking(hotspot_interface):
    control_socket_dir = get_wpa_supplicant_control_socket_dir()
    setup_network_interface_ip(hotspot_interface, '10.24.1.1', '255.255.255.0')
    try:
        shell_execute('%s dnsmasq' % KILLALL_PATH)
    except:
        LOGGER.exception('failed to killall dnsmasq')
    shell_execute('%s -i %s --dhcp-authoritative --no-negcache --user=root --no-resolv --no-hosts '
                  '--server=8.8.8.8 --dhcp-range=10.24.1.2,10.24.1.254,12h '
                  '--dhcp-leasefile=/data/data/fq.router/dnsmasq.leases '
                  '--pid-file=/data/data/fq.router/dnsmasq.pid' % (DNSMASQ_PATH, hotspot_interface))
    log_upstream_wifi_status('after setup networking', control_socket_dir)


def setup_lo_alias():
    setup_network_interface_ip('lo:1', '10.1.2.3', '255.255.255.255')
    enable_ipv4_forward()
    shell_execute('iptables -P FORWARD ACCEPT')
    iptables.insert_rules(RULES)


def setup_network_interface_ip(iface, ip, netmask):
    shell_execute('%s %s %s netmask %s' % (IFCONFIG_PATH, iface, ip, netmask))


def enable_ipv4_forward():
    LOGGER.info('enable ipv4 forward')
    with open('/proc/sys/net/ipv4/ip_forward', 'w') as f:
        f.write('1')


def log_upstream_wifi_status(log, control_socket_dir):
    try:
        LOGGER.info('=== %s ===' % log)
        shell_execute('%s -p %s -i %s status' % (P2P_CLI_PATH, control_socket_dir, WIFI_INTERFACE))
        shell_execute('%s dev %s link' % (IW_PATH, WIFI_INTERFACE))
        shell_execute('netcfg')
    except:
        LOGGER.exception('failed to log upstream wifi status')


def start_p2p_persistent_network(iface, control_socket_dir, ssid, password, sets_channel=False):
    wpa_supplicant_control_socket_dir = get_wpa_supplicant_control_socket_dir()
    try:
        shell_execute('%s -p %s -i %s p2p_set disabled 0' % (P2P_CLI_PATH, control_socket_dir, iface))
    except:
        LOGGER.exception('failed to p2p_set disabled')
    set_driver_param(control_socket_dir, iface, 'use_p2p_group_interface=1')
    if iface != WIFI_INTERFACE:
        try:
            shell_execute('%s -p %s -i %s p2p_set disabled 0' %
                          (P2P_CLI_PATH, wpa_supplicant_control_socket_dir, WIFI_INTERFACE))
        except:
            LOGGER.exception('failed to p2p_set disabled')
        set_driver_param(wpa_supplicant_control_socket_dir, WIFI_INTERFACE, 'use_p2p_group_interface=1')
    index = shell_execute('%s -p %s -i %s add_network' % (P2P_CLI_PATH, control_socket_dir, iface)).strip()

    def set_network(param):
        shell_execute('%s -p %s -i %s set_network %s %s' % (P2P_CLI_PATH, control_socket_dir, iface, index, param))

    set_network('mode 3')
    set_network('disabled 2')
    set_network('ssid \'"%s"\'' % ssid)
    set_network('key_mgmt WPA-PSK')
    set_network('proto RSN')
    set_network('pairwise CCMP')
    set_network('psk \'"%s"\'' % password)
    frequency, channel = get_upstream_frequency_and_channel()
    if channel:
        channel = channel if sets_channel else 0
        reg_class = 81 if sets_channel else 0
        reset_p2p_channels(iface, control_socket_dir, channel, reg_class)
        if iface != WIFI_INTERFACE:
            reset_p2p_channels(WIFI_INTERFACE, wpa_supplicant_control_socket_dir, channel, reg_class)
    do_p2p_group_add(iface, control_socket_dir, index, frequency)
    gevent.sleep(2)
    if get_working_hotspot_iface():
        return index
    LOGGER.error('restart p2p_supplicant to fix unexpected GO creation')
    restart_service('p2p_supplicant')
    do_p2p_group_add(iface, control_socket_dir, index, frequency)
    if get_working_hotspot_iface():
        return index
    LOGGER.error('restart wpa_supplicant to fix unexpected GO creation')
    do_p2p_group_add(iface, control_socket_dir, index, frequency)
    if get_working_hotspot_iface():
        return index
    LOGGER.error('restart *_supplicant did not fix the problem')
    return index


def do_p2p_group_add(iface, control_socket_dir, index, frequency):
    if frequency:
        shell_execute('%s -p %s -i %s p2p_group_add persistent=%s freq=%s ' %
                      (P2P_CLI_PATH, control_socket_dir, iface, index, frequency.replace('.', '')))
    else:
        shell_execute('%s -p %s -i %s p2p_group_add persistent=%s' % (P2P_CLI_PATH, control_socket_dir, iface, index))


def restart_service(name):
    try:
        shell_execute('stop %s' % name)
    except:
        LOGGER.exception('failed to stop %s' % name)
    try:
        shell_execute('start %s' % name)
    except:
        LOGGER.exception('failed to start %s' % name)
    gevent.sleep(1)


def set_driver_param(control_socket_dir, iface, param):
    try:
        shell_execute(
            '%s -p %s -i %s set driver_param %s' %
            (P2P_CLI_PATH, control_socket_dir, iface, param))
    except:
        LOGGER.exception('failed to set driver_param %s' % param)


def reset_p2p_channels(iface, control_socket_dir, channel, reg_class):
    try:
        shell_execute('%s -p %s -i %s set p2p_oper_channel %s' %
                      (P2P_CLI_PATH, control_socket_dir, iface, channel))
        shell_execute('%s -p %s -i %s set p2p_oper_reg_class %s' %
                      (P2P_CLI_PATH, control_socket_dir, iface, reg_class))
        shell_execute('%s -p %s -i %s set p2p_listen_channel %s' %
                      (P2P_CLI_PATH, control_socket_dir, iface, channel))
        shell_execute('%s -p %s -i %s set p2p_listen_reg_class %s' %
                      (P2P_CLI_PATH, control_socket_dir, iface, reg_class))
        shell_execute('%s -p %s -i %s save_config' % (P2P_CLI_PATH, control_socket_dir, iface))
    except:
        LOGGER.exception('failed to reset p2p channels')


def get_p2p_persistent_iface():
    for line in shell_execute('netcfg').splitlines(False):
        if line.startswith('p2p-'):
            return line.split(' ')[0]
        if line.startswith('ap0'):
            return 'ap0'
    return None


def stop_p2p_persistent_network(control_socket_dir, control_iface, iface):
    try:
        shell_execute(
            '%s -p %s -i %s p2p_group_remove %s' %
            (P2P_CLI_PATH, control_socket_dir, control_iface, iface))
    except:
        LOGGER.error('failed to stop p2p persistent network')


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
    if control_socket_dir == WIFI_INTERFACE:
        return 'anydir'
    if control_socket_dir:
        control_socket_not_exists = not os.path.exists(
            os.path.join(control_socket_dir, WIFI_INTERFACE))
    else:
        control_socket_not_exists = True
    dev_socket_exists = os.path.exists('/dev/socket/wpa_%s' % WIFI_INTERFACE)
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
    netd_socket = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    try:
        netd_socket.connect('/dev/socket/netd')
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
    finally:
        netd_socket.close()


def shell_execute(command):
    LOGGER.info('execute: %s' % command)
    try:
        output = subprocess.check_output(shlex.split(command), stderr=subprocess.STDOUT)
        LOGGER.info('succeed, output: %s' % output)
    except subprocess.CalledProcessError, e:
        LOGGER.error('failed, output: %s' % e.output)
        raise
    return output
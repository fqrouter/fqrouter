import subprocess
import traceback

try:
    WIFI_INTERFACE = subprocess.check_output(['/system/bin/getprop', 'wifi.interface']).strip() or 'wlan0'
except:
    traceback.print_exc()
    WIFI_INTERFACE = 'wlan0'


def list_data_network_interfaces():
    return WIFI_INTERFACE, 'ccmni0'
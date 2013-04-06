import subprocess
import traceback

try:
    WIFI_INTERFACE = subprocess.check_output(['getprop', 'wifi.interface']).strip() or 'wlan0'
except:
    traceback.print_exc()
    WIFI_INTERFACE = 'wlan0'


def get_mobile_data_interface():
    output = subprocess.check_output(['netcfg'])
    if 'ccmni0' in output:
        return 'ccmni0'
    if 'rmnet_usb0' in output:
        return 'rmnet_usb0'
    if 'rmnet0' in output:
        return 'rmnet0'
    if 'pdp0' in output:
        return 'pdp0'
    if 'ppp0' in output:
        return 'ppp0'
    raise Exception('failed get mobile data interface')


try:
    MOBILE_DATA_INTERFACE = get_mobile_data_interface()
except:
    MOBILE_DATA_INTERFACE = 'pdp0'


def list_data_network_interfaces():
    return WIFI_INTERFACE, MOBILE_DATA_INTERFACE
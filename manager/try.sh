adb push main.py /sdcard/manager/main.py
adb push comp_dns.py /sdcard/manager/comp_dns.py
adb push comp_lan.py /sdcard/manager/comp_lan.py
adb push comp_proxy.py /sdcard/manager/comp_proxy.py
adb push comp_shortcut.py /sdcard/manager/comp_shortcut.py
adb push comp_scrambler.py /sdcard/manager/comp_scrambler.py
adb push comp_wifi/__init__.py /sdcard/manager/comp_wifi/__init__.py
adb push comp_wifi/wifi.py /sdcard/manager/comp_wifi/wifi.py
adb push comp_wifi/hostapd_template.py /sdcard/manager/comp_wifi/hostapd_template.py
adb push utils/__init__.py /sdcard/manager/utils/__init__.py
adb push utils/iptables.py /sdcard/manager/utils/iptables.py
adb push utils/shutdown_hook.py /sdcard/manager/utils/shutdown_hook.py
adb push utils/shell.py /sdcard/manager/utils/shell.py
adb push utils/config.py /sdcard/manager/utils/config.py
adb shell su -c "PYTHONHOME=/data/data/fq.router2/python /data/data/fq.router2/busybox sh /data/data/fq.router2/python/bin/python-launcher.sh /sdcard/manager/main.py"

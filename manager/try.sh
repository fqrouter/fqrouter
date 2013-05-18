adb push dns_service.py /sdcard/manager/dns_service.py
adb push iptables.py /sdcard/manager/iptables.py
adb push shutdown_hook.py /sdcard/manager/shutdown_hook.py
adb push main.py /sdcard/manager/main.py
adb push wifi.py /sdcard/manager/wifi.py
adb push hostapd_template.py /sdcard/manager/hostapd_template.py
adb push lan_service.py /sdcard/manager/lan_service.py
adb push version.py /sdcard/manager/version.py
adb push shell.py /sdcard/manager/shell.py
adb push socks_service.py /sdcard/manager/socks_service.py
adb push nfqueue_ipset.py /sdcard/manager/nfqueue_ipset.py
adb push tcp_service.py /sdcard/manager/tcp_service.py
adb push lan_ip.py /sdcard/manager/lan_ip.py
adb push pending_connection.py /sdcard/manager/pending_connection.py
adb shell su -c "rm /sdcard/manager/*.pyc"
adb shell su -c "PYTHONHOME=/data/data/fq.router/python /data/data/fq.router/busybox sh /data/data/fq.router/python/bin/python-launcher.sh /sdcard/manager/main.py"

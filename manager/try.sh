adb shell su -c "killall python"
adb push delegated-apnic-latest.txt /sdcard/manager/delegated-apnic-latest.txt
adb push pending_connection.py /sdcard/manager/pending_connection.py
adb push lan_ip.py /sdcard/manager/lan_ip.py
adb push china_ip.py /sdcard/manager/china_ip.py
adb push china_domain.py /sdcard/manager/china_domain.py
adb push tcp_service.py /sdcard/manager/tcp_service.py
adb push dns_resolver.py /sdcard/manager/dns_resolver.py
adb push dns_server.py /sdcard/manager/dns_server.py
adb push dns_service.py /sdcard/manager/dns_service.py
adb push full_proxy_service.py /sdcard/manager/full_proxy_service.py
adb push redsocks_template.py /sdcard/manager/redsocks_template.py
adb push redsocks_monitor.py /sdcard/manager/redsocks_monitor.py
adb push iptables.py /sdcard/manager/iptables.py
adb push shutdown_hook.py /sdcard/manager/shutdown_hook.py
adb push main.py /sdcard/manager/main.py
adb push wifi.py /sdcard/manager/wifi.py
adb push hostapd_template.py /sdcard/manager/hostapd_template.py
adb push goagent_launcher.py /sdcard/manager/goagent_launcher.py
adb push goagent_monitor.py /sdcard/manager/goagent_monitor.py
adb push goagent.py /sdcard/manager/goagent.py
adb push goagent.ini /sdcard/manager/goagent.ini
adb push lan_service.py /sdcard/manager/lan_service.py
adb push version.py /sdcard/manager/version.py
adb push twitter.py /sdcard/manager/twitter.py
adb push shell.py /sdcard/manager/shell.py
adb shell su -c "rm /sdcard/manager/*.pyc"
adb shell su -c "PYTHONHOME=/data/data/fq.router/python /data/data/fq.router/busybox sh /data/data/fq.router/python/bin/python-launcher.sh /sdcard/manager/main.py"

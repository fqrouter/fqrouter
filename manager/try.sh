adb shell su -c "killall python"
adb push dns_agent.py /sdcard/manager/dns_agent.py
adb push iptables.py /sdcard/manager/iptables.py
adb push main.py /sdcard/manager/main.py
adb push dashboard.html /sdcard/manager/dashboard.html
adb shell su -c "PYTHONHOME=/data/data/fq.router/python python /sdcard/manager/main.py"
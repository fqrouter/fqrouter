logcat -d -v time -s fqrouter:V > /sdcard/logcat.log
getprop > /sdcard/getprop.log
dmesg > /sdcard/dmesg.log
iptables -L -v -n > /sdcard/iptables.log
iptables -t nat -L -v -n >> /sdcard/iptables.log
/data/data/fq.router/busybox cp /data/data/fq.router/manager.log /sdcard/manager.log
/data/data/fq.router/busybox cp /data/data/fq.router/redsocks.log /sdcard/redsocks.log
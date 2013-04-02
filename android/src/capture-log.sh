/system/bin/logcat -d -v time -s fqrouter:V > /sdcard/logcat.log
/system/bin/getprop > /sdcard/getprop.log
/system/bin/dmesg > /sdcard/dmesg.log
/data/data/fq.router/busybox cp /data/data/fq.router/manager.log /sdcard/manager.log
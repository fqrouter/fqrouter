import subprocess
import time

VERSION = '1.0-snapshot-{}'.format(time.strftime("%Y%m%d%H%M%S", time.localtime()))
ROOTFS_FILE_NAME = 'bin/x86/{}.rootfs'.format(VERSION)

def execute(command):
    print(command)
    subprocess.call(command, shell=True)

execute('dd if=/dev/zero of={} bs=1M count=128'.format(ROOTFS_FILE_NAME))
execute('losetup /dev/loop6 {}'.format(ROOTFS_FILE_NAME))
try:
    execute('mkfs -t ext4 -v /dev/loop6')
    execute('mkdir bin/x86/rootfs')
    execute('mount -t ext4 /dev/loop6 bin/x86/rootfs')
    try:
        execute('tar -xzf bin/x86/openwrt-x86-generic-rootfs.tar.gz -C bin/x86/rootfs')
        time.sleep(1)
    finally:
        execute('umount -l bin/x86/rootfs')
finally:
    execute('losetup -d /dev/loop6')
execute('vmware-mount /mnt/hgfs/host/openwrt-spike/u-disk.vmdk /udisk/')
try:
    execute('rm -rf /udisk/*'.format(ROOTFS_FILE_NAME))
    execute('mkdir /udisk/fqrouter')
    with open('/udisk/fqrouter/boot', 'w') as f:
        f.write('FQROUTER_VERSION={}\n'.format(VERSION))
    execute('mv {} /udisk/fqrouter'.format(ROOTFS_FILE_NAME))
    execute('cp bin/x86/openwrt-x86-generic-vmlinuz /udisk/fqrouter/{}.initramfs'.format(VERSION))
    time.sleep(1)
finally:
    try:
        execute('umount -l -d /udisk')
        execute('vmware-mount -x')
    finally:
        execute('vmware-mount -x')
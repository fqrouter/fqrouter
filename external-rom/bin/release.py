import subprocess
import time
import sys
import os

if len(sys.argv) < 2:
    raise Exception('must specify hardware')
hardware = sys.argv[1]
introm_version_path = os.path.join(os.path.dirname(__file__), '../../internal-rom/generic/files/etc/fqrouter_introm_version')
if not os.path.exists(introm_version_path):
    raise Exception('missing {}'.find(introm_version_path))
with open(introm_version_path) as f:
    introm_version = f.read()

ARCHITECTURES = {
    'tl-wr703n': 'ar71xx',
    'tl-wr720n': 'ar71xx'
}
ARCHITECTURE = ARCHITECTURES[hardware]
VERSION = '{}-{}-extrom{}'.format(hardware, introm_version, time.strftime("%Y%m%d%H%M%S", time.localtime()))
ROOTFS_TAR_GZ = 'bin/{}/openwrt-{}-generic-rootfs.tar.gz'.format(ARCHITECTURE, ARCHITECTURE)
ROOTFS_PATH = 'bin/{}/{}.rootfs'.format(ARCHITECTURE, VERSION)
KERNEL_PATH = 'bin/{}/openwrt-{}-generic-vmlinux-initramfs.elf'.format(ARCHITECTURE, ARCHITECTURE)

def execute(command):
    print(command)
    subprocess.call(command, shell=True)

def prepare_rootfs():
    execute('dd if=/dev/zero of={} bs=1M count=16'.format(ROOTFS_PATH))
    execute('losetup /dev/loop6 {}'.format(ROOTFS_PATH))
    try:
        execute('mkfs -t ext4 -v /dev/loop6')
        execute('mkdir bin/{}/rootfs'.format(ARCHITECTURE))
        execute('mount -t ext4 /dev/loop6 bin/{}/rootfs'.format(ARCHITECTURE))
        try:
            execute('tar -xzf {} -C bin/{}/rootfs'.format(ROOTFS_TAR_GZ, ARCHITECTURE))
            time.sleep(1)
        finally:
            execute('umount -l bin/{}/rootfs'.format(ARCHITECTURE))
    finally:
        execute('losetup -d /dev/loop6')

def to_usb():
    execute('rm -rf /opt/usb/*')
    execute('mkdir /opt/usb/fqrouter')
    with open('/opt/usb/fqrouter/boot', 'w') as f:
        f.write('FQROUTER_VERSION={}\n'.format(VERSION))
    execute('cp {} /opt/usb/fqrouter/{}.rootfs'.format(ROOTFS_PATH, VERSION))
    execute('cp {} /opt/usb/fqrouter/{}.kernel'.format(KERNEL_PATH, VERSION))
    subprocess.call('cd /mnt/hgfs/usb/ && zip -r {}.zip fqrouter'.format(VERSION), shell=True)

prepare_rootfs()
to_usb()
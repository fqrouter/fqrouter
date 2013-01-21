import subprocess
import time
import sys

if len(sys.argv) < 2:
    raise Exception('must specify hardware')
hardware = sys.argv[1]
recovery_version = 'recovery20130121'

ARCHITECTURES = {
    '703n': 'ar71xx',
    '720n': 'ar71xx'
}
KERNEL_COMMAND_LINES = {
    '703n': 'board=TL-WR703N console=ttyATH0,115200',
    '720n': 'board=TL-WR720N console=ttyATH0,115200'
}
ARCHITECTURE = ARCHITECTURES[hardware]
KERNEL_COMMAND_LINE = KERNEL_COMMAND_LINES[hardware]
VERSION = '{}-{}-snapshot{}'.format(hardware, recovery_version, time.strftime("%Y%m%d%H%M%S", time.localtime()))
ROOTFS_TAR_GZ = 'bin/{}/openwrt-{}-generic-rootfs.tar.gz'.format(ARCHITECTURE, ARCHITECTURE)
ROOTFS_PATH = 'bin/{}/{}.rootfs'.format(ARCHITECTURE, VERSION)
INITRAMFS_PATH = 'bin/{}/openwrt-{}-generic-vmlinux-initramfs.elf'.format(ARCHITECTURE, ARCHITECTURE)

def execute(command):
    print(command)
    subprocess.call(command, shell=True)

def prepare_rootfs():
    execute('dd if=/dev/zero of={} bs=1M count=128'.format(ROOTFS_PATH))
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
        f.write('KERNEL_COMMAND_LINE="{}"\n'.format(KERNEL_COMMAND_LINE))
    execute('cp {} /opt/usb/fqrouter/{}.rootfs'.format(ROOTFS_PATH, VERSION))
    execute('cp {} /opt/usb/fqrouter/{}.initramfs'.format(INITRAMFS_PATH, VERSION))

prepare_rootfs()
to_usb()
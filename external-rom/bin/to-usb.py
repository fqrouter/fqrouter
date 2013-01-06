import subprocess
import sys

ARCHITECTURE = 'ar71xx'
ROOTFS_PATH = 'bin/{}/openwrt-{}-generic-rootfs.tar.gz'.format(ARCHITECTURE, ARCHITECTURE)
KERNEL_PATH = 'bin/{}/openwrt-{}-generic-vmlinux-initramfs.elf'.format(ARCHITECTURE, ARCHITECTURE)

usb_path = '/media/{}'.format(sys.argv[1])
subprocess.call('rm -rf {}/*'.format(usb_path), shell=True)
subprocess.call('tar -C {}/ -zxf {}'.format(usb_path, ROOTFS_PATH), shell=True)
subprocess.call('cp {} {}/linux.elf'.format(KERNEL_PATH, usb_path), shell=True)
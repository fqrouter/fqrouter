rm -rf /media/$1/*
sudo tar -C /media/$1/ -zxf bin/ar71xx/openwrt-ar71xx-generic-rootfs.tar.gz
sudo cp bin/ar71xx/openwrt-ar71xx-generic-vmlinux-initramfs.elf /media/$1/linux.elf
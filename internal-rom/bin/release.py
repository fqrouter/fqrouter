import subprocess
import time
import sys
import os

if len(sys.argv) < 2:
    raise Exception('must specify hardware')
hardware = sys.argv[1]
introm_version_path = os.path.join(os.path.dirname(__file__), '../generic/files/etc/fqrouter_introm_version')
if not os.path.exists(introm_version_path):
    raise Exception('missing {}'.format(introm_version_path))
with open(introm_version_path) as f:
    introm_version = f.read()

ARCHITECTURES = {
    'tl-wr703n': 'ar71xx',
    'tl-wr720n': 'ar71xx'
}
ARCHITECTURE = ARCHITECTURES[hardware]
ROOTFS_PATH = 'bin/{}/*factory.bin'.format(ARCHITECTURE)
subprocess.call('cp {} /mnt/hgfs/usb/{}-{}.bin'.format(ROOTFS_PATH, hardware, introm_version), shell=True)
subprocess.call('cd /mnt/hgfs/usb/ && zip -r {}-{}.zip {}-{}.bin'.format(
    hardware, introm_version, hardware, introm_version), shell=True)

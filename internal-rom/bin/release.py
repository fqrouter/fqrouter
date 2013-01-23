import subprocess
import time
import sys
import os

if len(sys.argv) < 2:
    raise Exception('must specify hardware')
hardware = sys.argv[1]
recovery_version_path = os.path.join(os.path.dirname(__file__), '../generic/files/etc/fqrouter_recovery_version')
if not os.path.exists(recovery_version_path):
    raise Exception('missing {}'.format(recovery_version_path))
with open(recovery_version_path) as f:
    recovery_version = f.read()

ARCHITECTURES = {
    '703n': 'ar71xx',
    '720n': 'ar71xx'
}
ARCHITECTURE = ARCHITECTURES[hardware]
ROOTFS_PATH = 'bin/{}/*factory.bin'.format(ARCHITECTURE)
subprocess.call('cp {} /mnt/hgfs/usb/{}-{}.recovery'.format(ROOTFS_PATH, hardware, recovery_version), shell=True)
subprocess.call('cd /mnt/hgfs/usb/ && zip -r introm-{}-{}.zip {}-{}.recovery'.format(
    hardware, recovery_version, hardware, recovery_version), shell=True)

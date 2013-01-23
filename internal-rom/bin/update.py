import subprocess
import os.path
import sys

if len(sys.argv) < 2:
    raise Exception('must specify hardware')
hardware = sys.argv[1]
hardware_dir = os.path.join(os.path.dirname(__file__), '../{}'.format(hardware))
generic_dir = os.path.join(os.path.dirname(__file__), '../generic')

subprocess.call('cp .config {}'.format(os.path.join(os.path.dirname(__file__), '../{}'.format(hardware))), shell=True)
subprocess.call('rm -rf files', shell=True)
subprocess.call('rm -rf bin', shell=True)
subprocess.call('cp -r -p {}/* .'.format(generic_dir), shell=True)
subprocess.call('cp -r -p {}/* .'.format(hardware_dir), shell=True)
subprocess.call('make -j 5', shell=True)
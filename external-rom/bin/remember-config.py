import subprocess
import os.path

subprocess.call('cp .config {}/../720n/.config'.format(os.path.dirname(__file__)), shell=True)
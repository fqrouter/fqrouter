#!/usr/bin/env python
import subprocess

subprocess.call('rm AndroidManifest.xml', shell=True)
subprocess.check_call('ln -s AndroidManifest-installer.xml AndroidManifest.xml', shell=True)
subprocess.call('rm assets', shell=True)
subprocess.check_call('ln -s assets-installer assets', shell=True)
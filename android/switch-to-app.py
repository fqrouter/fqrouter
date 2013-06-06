#!/usr/bin/env python
import subprocess

subprocess.call('rm AndroidManifest.xml', shell=True)
subprocess.check_call('ln -s AndroidManifest-app.xml AndroidManifest.xml', shell=True)
subprocess.call('rm assets', shell=True)
subprocess.check_call('ln -s assets-app assets', shell=True)
subprocess.check_call('python prepare-payload.py', shell=True)

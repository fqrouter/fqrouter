#!/usr/bin/env python
import os
import urllib
import subprocess
import zipfile
import sys
import time

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
PAYLOAD_DIR = os.path.join(ROOT_DIR, 'payload')
ASSETS_DIR = os.path.join(ROOT_DIR, 'assets')
SRC_DIR = os.path.join(ROOT_DIR, 'src')
PYTHON_ZIP_FILE = os.path.join(PAYLOAD_DIR, 'python.zip')
PYTHON_DIR = os.path.join(PAYLOAD_DIR, 'python')
WIFI_TOOLS_ZIP_FILE = os.path.join(PAYLOAD_DIR, 'wifi-tools.zip')
WIFI_TOOLS_DIR = os.path.join(PAYLOAD_DIR, 'wifi-tools')
DPKT_ZIP_FILE = os.path.join(PAYLOAD_DIR, 'dpkt.zip')
DPKT_DIR = os.path.join(PAYLOAD_DIR, 'dpkt-fqrouter')
DPKT_PACKAGE_DIR = os.path.join(DPKT_DIR, 'dpkt')
JINJA2_TAR_GZ_FILE = os.path.join(PAYLOAD_DIR, 'jinja2.tar.gz')
JINJA2_DIR = os.path.join(PAYLOAD_DIR, 'Jinja2-2.7.1')
JINJA2_PACKAGE_DIR = os.path.join(JINJA2_DIR, 'jinja2')
BUSYBOX_FILE = os.path.join(ASSETS_DIR, 'busybox')
PROXY_TOOLS_DIR = os.path.join(PAYLOAD_DIR, 'proxy-tools')
FQDNS_PY = os.path.join(PAYLOAD_DIR, 'python', 'lib', 'python2.7', 'site-packages', 'fqdns.py')
FQDNS_PY_SRC = os.path.join(os.path.dirname(__file__), '..', '..', 'fqdns', 'fqdns.py')
FQTING_PY = os.path.join(PAYLOAD_DIR, 'python', 'lib', 'python2.7', 'site-packages', 'fqting.py')
FQTING_PY_SRC = os.path.join(os.path.dirname(__file__), '..', '..', 'fqting', 'fqting.py')
FQLAN_PY = os.path.join(PAYLOAD_DIR, 'python', 'lib', 'python2.7', 'site-packages', 'fqlan.py')
FQLAN_PY_SRC = os.path.join(os.path.dirname(__file__), '..', '..', 'fqlan', 'fqlan.py')
FQSOCKS_DIR = os.path.join(PAYLOAD_DIR, 'python', 'lib', 'python2.7', 'site-packages', 'fqsocks')
FQSOCKS_DIR_SRC = os.path.join(os.path.dirname(__file__), '..', '..', 'fqsocks', 'fqsocks')
MANAGER_DIR = os.path.join(ROOT_DIR, '../manager')


def main():
    if not os.path.exists(ASSETS_DIR):
        os.mkdir(ASSETS_DIR)
    if not os.path.exists(PAYLOAD_DIR):
        os.mkdir(PAYLOAD_DIR)
    if not os.path.exists(PROXY_TOOLS_DIR):
        os.mkdir(PROXY_TOOLS_DIR)
    download_python27()
    unzip_python27()
    download_wifi_tools()
    unzip_wifi_tools()
    download_dpkt()
    unzip_dpkt()
    download_jinja2()
    untargz_jinja2()
    download_busybox()
    copy_fqdns()
    copy_fqting()
    copy_fqlan()
    copy_fqsocks()
    zip_payload()


def download_python27():
    if os.path.exists(PYTHON_ZIP_FILE):
        return
    urllib.urlretrieve('http://cdn.fqrouter.com/python.zip', PYTHON_ZIP_FILE)


def unzip_python27():
    if os.path.exists(PYTHON_DIR):
        return
    os.mkdir(PYTHON_DIR)
    subprocess.check_call('unzip %s' % PYTHON_ZIP_FILE, cwd=PYTHON_DIR, shell=True)
    if not os.path.exists(os.path.join(PYTHON_DIR, 'bin/python')):
        print('zip file not as expected')
        sys.exit(1)


def download_wifi_tools():
    if os.path.exists(WIFI_TOOLS_ZIP_FILE):
        return
    urllib.urlretrieve('http://cdn.fqrouter.com/wifi-tools.zip', WIFI_TOOLS_ZIP_FILE)


def unzip_wifi_tools():
    if os.path.exists(WIFI_TOOLS_DIR):
        return
    subprocess.check_call('unzip %s' % WIFI_TOOLS_ZIP_FILE, cwd=PAYLOAD_DIR, shell=True)


def download_dpkt():
    if os.path.exists(DPKT_ZIP_FILE):
        return
    urllib.urlretrieve('https://github.com/fqrouter/dpkt/archive/fqrouter.zip', DPKT_ZIP_FILE)


def unzip_dpkt():
    if os.path.exists(DPKT_DIR):
        return
    subprocess.check_call('unzip %s' % DPKT_ZIP_FILE, cwd=PAYLOAD_DIR, shell=True)
    if not os.path.exists(os.path.join(DPKT_DIR, 'setup.py')):
        print('dpkt.zip file not as expected')
        sys.exit(1)

def download_jinja2():
    if os.path.exists(JINJA2_TAR_GZ_FILE):
        return
    urllib.urlretrieve('https://pypi.python.org/packages/source/J/Jinja2/Jinja2-2.7.1.tar.gz#md5=282aed153e69f970d6e76f78ed9d027a', JINJA2_TAR_GZ_FILE)


def untargz_jinja2():
    if os.path.exists(JINJA2_DIR):
        return
    subprocess.check_call('tar xvzf %s' % JINJA2_TAR_GZ_FILE, cwd=PAYLOAD_DIR, shell=True)
    if not os.path.exists(os.path.join(JINJA2_DIR, 'setup.py')):
        print('jinja2.tar.gz file not as expected')
        sys.exit(1)


def download_busybox():
    if os.path.exists(BUSYBOX_FILE):
        return
    urllib.urlretrieve('http://www.busybox.net/downloads/binaries/latest/busybox-armv6l', BUSYBOX_FILE)


def copy_fqdns():
    subprocess.check_call('cp %s %s' % (FQDNS_PY_SRC, FQDNS_PY), shell=True)


def copy_fqting():
    subprocess.check_call('cp %s %s' % (FQTING_PY_SRC, FQTING_PY), shell=True)


def copy_fqlan():
    subprocess.check_call('cp %s %s' % (FQLAN_PY_SRC, FQLAN_PY), shell=True)


def copy_fqsocks():
    if not os.path.exists(FQSOCKS_DIR):
        os.mkdir(FQSOCKS_DIR)
    subprocess.check_call('cp -r %s/* %s' % (FQSOCKS_DIR_SRC, FQSOCKS_DIR), shell=True)
    subprocess.check_call('rm %s/*.pyc' % FQSOCKS_DIR, shell=True)


def zip_payload():
    payload_zip_path = os.path.join(ASSETS_DIR, 'payload.zip')
    if os.path.exists(payload_zip_path):
        os.remove(payload_zip_path)
    payload_zip = zipfile.ZipFile(payload_zip_path, 'w', compression=zipfile.ZIP_DEFLATED)

    def include_directory(directory, relative_to, base=None):
        for root, dir_names, file_names in os.walk(directory):
            for file_name in file_names:
                file_path = os.path.join(root, file_name)
                archive_path = os.path.relpath(file_path, relative_to)
                if base:
                    archive_path = '%s/%s' % (base, archive_path)
                payload_zip.write(file_path, archive_path)

    include_directory(PYTHON_DIR, PAYLOAD_DIR)
    include_directory(WIFI_TOOLS_DIR, PAYLOAD_DIR)
    include_directory(PROXY_TOOLS_DIR, PAYLOAD_DIR)
    include_directory(MANAGER_DIR, os.path.dirname(MANAGER_DIR))
    include_directory(DPKT_PACKAGE_DIR, DPKT_DIR, 'python/lib/python2.7/site-packages')
    include_directory(JINJA2_PACKAGE_DIR, JINJA2_DIR, 'python/lib/python2.7/site-packages')

    payload_zip.close()
    time.sleep(1)


if '__main__' == __name__:
    main()

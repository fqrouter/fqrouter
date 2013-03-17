#!/usr/bin/env python
import os
import urllib
import subprocess
import zipfile
import sys
from pyquery import PyQuery as pq

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
PAYLOAD_DIR = os.path.join(ROOT_DIR, 'payload')
ASSETS_DIR = os.path.join(ROOT_DIR, 'assets')
PYTHON_ZIP_FILE = os.path.join(PAYLOAD_DIR, 'python.zip')
PYTHON_DIR = os.path.join(PAYLOAD_DIR, 'python')
WIFI_TOOLS_ZIP_FILE = os.path.join(PAYLOAD_DIR, 'wifi-tools.zip')
WIFI_TOOLS_DIR = os.path.join(PAYLOAD_DIR, 'wifi-tools')
TORNADO_ZIP_FILE = os.path.join(PAYLOAD_DIR, 'tornado.zip')
TORNADO_DIR = os.path.join(PAYLOAD_DIR, 'tornado-branch2.4')
TORNADO_PACKAGE_DIR = os.path.join(TORNADO_DIR, 'tornado')
DPKT_TAR_GZ_FILE = os.path.join(PAYLOAD_DIR, 'dpkt-1.7.tar.gz')
DPKT_DIR = os.path.join(PAYLOAD_DIR, 'dpkt-1.7')
DPKT_PACKAGE_DIR = os.path.join(DPKT_DIR, 'dpkt')
BUSYBOX_FILE = os.path.join(ASSETS_DIR, 'busybox')
MANAGER_DIR = os.path.join(ROOT_DIR, '../manager')


def main():
    if not os.path.exists(ASSETS_DIR):
        os.mkdir(ASSETS_DIR)
    if not os.path.exists(PAYLOAD_DIR):
        os.mkdir(PAYLOAD_DIR)
    download_python27()
    unzip_python27()
    download_wifi_tools()
    unzip_wifi_tools()
    download_tornado()
    unzip_tornado()
    download_dpkt()
    untargz_dpkt()
    download_busybox()
    zip_payload()


def download_python27():
    if os.path.exists(PYTHON_ZIP_FILE):
        return
    retrieve_from_baidu_pan('http://fqrouter.tumblr.com/android-python27', PYTHON_ZIP_FILE)


def unzip_python27():
    if os.path.exists(PYTHON_DIR):
        return
    subprocess.check_call('unzip %s' % PYTHON_ZIP_FILE, cwd=PAYLOAD_DIR, shell=True)
    if not os.path.exists(os.path.join(PYTHON_DIR, 'bin/python')):
        print('zip file not as expected')
        sys.exit(1)


def download_wifi_tools():
    if os.path.exists(WIFI_TOOLS_ZIP_FILE):
        return
    retrieve_from_baidu_pan('http://fqrouter.tumblr.com/android-wifi-tools', WIFI_TOOLS_ZIP_FILE)


def unzip_wifi_tools():
    if os.path.exists(WIFI_TOOLS_DIR):
        return
    os.mkdir(WIFI_TOOLS_DIR)
    subprocess.check_call('unzip %s' % WIFI_TOOLS_DIR, cwd=WIFI_TOOLS_DIR, shell=True)


def download_tornado():
    if os.path.exists(TORNADO_ZIP_FILE):
        return
    urllib.urlretrieve('https://github.com/facebook/tornado/archive/branch2.4.zip', TORNADO_ZIP_FILE)


def unzip_tornado():
    if os.path.exists(TORNADO_DIR):
        return
    subprocess.check_call('unzip %s' % TORNADO_ZIP_FILE, cwd=PAYLOAD_DIR, shell=True)
    if not os.path.exists(os.path.join(TORNADO_DIR, 'setup.py')):
        print('zip file not as expected')
        sys.exit(1)


def download_dpkt():
    if os.path.exists(DPKT_TAR_GZ_FILE):
        return
    urllib.urlretrieve('http://dpkt.googlecode.com/files/dpkt-1.7.tar.gz', DPKT_TAR_GZ_FILE)


def untargz_dpkt():
    if os.path.exists(DPKT_DIR):
        return
    subprocess.check_call('tar xvzf %s' % DPKT_TAR_GZ_FILE, cwd=PAYLOAD_DIR, shell=True)
    if not os.path.exists(os.path.join(DPKT_DIR, 'setup.py')):
        print('tar.gz file not as expected')
        sys.exit(1)


def download_busybox():
    if os.path.exists(BUSYBOX_FILE):
        return
    urllib.urlretrieve('http://www.busybox.net/downloads/binaries/latest/busybox-armv6l', BUSYBOX_FILE)


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
    include_directory(MANAGER_DIR, os.path.dirname(MANAGER_DIR))
    include_directory(TORNADO_PACKAGE_DIR, TORNADO_DIR, 'python/lib/python2.7/site-packages')
    include_directory(DPKT_PACKAGE_DIR, DPKT_DIR, 'python/lib/python2.7/site-packages')

    payload_zip.close()


def retrieve_from_baidu_pan(url, destination):
    d = pq(url=url)
    link = d('#downFileButtom').attr('href')
    urllib.urlretrieve(link, destination)


if '__main__' == __name__:
    main()

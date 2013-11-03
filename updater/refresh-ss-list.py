#!/usr/bin/env python
import subprocess
import time
import random
import datetime
import socket
import struct
import logging
from fqsocks.proxies import encrypt

LOGGER = logging.getLogger(__name__)

google_ip = socket.gethostbyname('www.google.com')
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s %(levelname)s %(message)s')


def check_proxy(ip, port, password, encrypt_method):
    try:
        sock = socket.socket()
        sock.settimeout(10)
        sock.connect((ip, port))
        encryptor = encrypt.Encryptor(password, encrypt_method)
        addr_to_send = '\x01'
        addr_to_send += socket.inet_aton(google_ip)
        addr_to_send += struct.pack('>H', 80)
        sock.sendall(encryptor.encrypt(addr_to_send))
        sock.sendall(encryptor.encrypt('GET http://www.google.com/ncr HTTP/1.1\r\n\r\n'))
        response = encryptor.decrypt(sock.recv(8192))
        ok = '302' in response
        if ok:
            LOGGER.info('[OK] %s:%s' % (ip, port))
            return True
        else:
            LOGGER.info('[FAIL] %s:%s\n%s' % (ip, port, response))
            return False
    except:
        LOGGER.exception('[FAIL] %s:%s' % (ip, port))
        return False


while True:
    try:
        proxies = [
            # ('69.163.40.146', 220, 'A76nOIHdZEYw', 'rc4'), #ds1
            # ('69.163.40.146', 221, 'A76nOIHdZEYw', 'rc4'), #ds1
            ('174.140.169.55', random.choice([220, 221, 222, 223]), 'A76nOIHdZEYw', 'rc4'), #ds2
            ('174.140.171.244', random.choice([220, 221]), 'A76nOIHdZEYw', 'rc4'), #ds3
            ('192.184.80.11', random.choice([220, 221]), 'A76nOIHdZEYw', 'rc4'), #ramnode2
            ('199.188.75.60', random.choice([220, 221]), 'A76nOIHdZEYw', 'rc4'), #raidlogic
            ('176.56.236.63', random.choice([220, 221]), 'A76nOIHdZEYw', 'rc4'), #ramnode3
            ('162.220.11.191', random.choice([220, 221]), 'A76nOIHdZEYw', 'rc4'), #crissic
            ('209.141.57.22', random.choice([220, 221]), 'A76nOIHdZEYw', 'rc4'), #buyvm2
            ('174.140.169.62', random.choice([220, 221]), 'A76nOIHdZEYw', 'rc4'), #ds4
            ('174.140.169.65', random.choice([220, 221, 222, 223]), 'A76nOIHdZEYw', 'rc4'), #ds5
            ('162.217.248.65', random.choice([220, 221, 222, 223]), 'A76nOIHdZEYw', 'rc4'), #iniz1
            ('162.217.248.91', random.choice([220, 221, 222, 223]), 'A76nOIHdZEYw', 'rc4'), #iniz2
            ('192.249.61.233', random.choice([220, 221]), 'A76nOIHdZEYw', 'rc4'), #ramnode1
            ('198.98.49.121', random.choice([220, 221]), 'A76nOIHdZEYw', 'rc4'), #buyvm1
            ('192.184.94.236', random.choice([220, 221]), 'A76nOIHdZEYw', 'rc4'), #ramnode4
            ('208.117.11.211', random.choice([220, 221, 222, 223]), 'A76nOIHdZEYw', 'rc4'), #daring
        ]
        random.shuffle(proxies)
        i = 1
        for ip, port, password, encrypt_method in proxies:
            if check_proxy(ip, port, password, encrypt_method):
                subprocess.call('cli53 rrcreate fqrouter.com ss%s.a TXT %s:%s:%s:%s --ttl 450 --replace'
                                % (i, ip, port, password, encrypt_method), shell=True)
                i += 1
        for j in range(i, 11):
            LOGGER.info('[N/A] ss%s.fqrouter.com' % j)
            subprocess.call('cli53 rrcreate fqrouter.com ss%s.a TXT "" --ttl 450 --replace' % j, shell=True)
        print('%s done' % datetime.datetime.now())
    except:
        LOGGER.exception('failed to update proxies')
    time.sleep(300)

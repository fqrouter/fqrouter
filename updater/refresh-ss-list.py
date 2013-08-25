#!/usr/bin/env python
import subprocess
import time
import random
import datetime
import socket
import struct
import logging
from fqsocks import encrypt

LOGGER = logging.getLogger(__name__)

google_ip = socket.gethostbyname('www.google.com')
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s %(levelname)s %(message)s')


def check_proxy(ip, port, password, encrypt_method):
    try:
        sock = socket.socket()
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
            ('192.81.133.165', 12121, 'wendangku.org', 'aes-256-cfb'),
            ('198.199.92.59', 20, 'u1rRWTssNv0p', 'rc4'),
            ('88.191.139.226', 23, 'u1rRWTssNv0p', 'rc4'),
            ('192.184.80.11', 8318, 'fqrouter', 'rc4'),
            (socket.gethostbyname('socks2.masaila.info'), 2920 + random.randint(0, 9), '111222', 'table'),
            (socket.gethostbyname('socks3.masaila.info'), 2920 + random.randint(0, 9), '111222', 'table'),
            (socket.gethostbyname('socks4.masaila.info'), 2920 + random.randint(0, 9), '111222', 'table'),
            (socket.gethostbyname('socks5.masaila.info'), 2920 + random.randint(0, 9), '111222', 'table')
        ]
        success = 0
        for i, (ip, port, password, encrypt_method) in enumerate(proxies):
            if check_proxy(ip, port, password, encrypt_method):
                success += 1
                subprocess.call('cli53 rrcreate fqrouter.com ss%s TXT %s:%s:%s:%s --ttl 300 --replace'
                                % (i + 1, ip, port, password, encrypt_method), shell=True)
            else:
                subprocess.call('cli53 rrcreate fqrouter.com ss%s TXT "" --ttl 300 --replace' % (i + 1), shell=True)
        if not success:
            subprocess.call(
                'cli53 rrcreate fqrouter.com ss1 TXT 192.184.80.11:8318:fqrouter:rc4 --ttl 300 --replace', shell=True)
            subprocess.call(
                'cli53 rrcreate fqrouter.com ss2 TXT 192.249.61.233:8318:fqrouter:rc4 --ttl 300 --replace', shell=True)
        print('%s done' % datetime.datetime.now())
    except:
        LOGGER.exception('failed to update proxies')
    time.sleep(300)

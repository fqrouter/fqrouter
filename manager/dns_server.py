import socket
import traceback
import logging
import china_domain

LOGGER = logging.getLogger('fqrouter.%s' % __name__)

PRIMARY_DEFAULT_DNS_IP = '8.8.8.8'
PRIMARY_DEFAULT_DNS_PORT = 53
SECONDARY_DEFAULT_DNS_IP = '8.8.4.4'
SECONDARY_DEFAULT_DNS_PORT = 53

DNS_SERVERS = {
    'default': (
        (PRIMARY_DEFAULT_DNS_IP, PRIMARY_DEFAULT_DNS_PORT),
        (SECONDARY_DEFAULT_DNS_IP, SECONDARY_DEFAULT_DNS_PORT)),
    'china': (
        ('114.114.114.114', 53),
        ('114.114.115.115', 53))
}

VER_DOMAIN = 'd2anp67vmqk4wc.cloudfront.net'
try:
    PRIMARY_VER_DNS_IP = socket.gethostbyname('ns-51.awsdns-06.com')
    PRIMARY_VER_DNS_PORT = 53
except:
    LOGGER.exception('failed to resolve ns-51.awsdns-06.com')
    PRIMARY_VER_DNS_IP = PRIMARY_DEFAULT_DNS_IP
    PRIMARY_VER_DNS_PORT = PRIMARY_DEFAULT_DNS_PORT
try:
    SECONDARY_VER_DNS_IP = socket.gethostbyname('ns-738.awsdns-28.net')
    SECONDARY_VER_DNS_PORT = 53
except:
    LOGGER.exception('failed to resolve ns-738.awsdns-28.net')
    SECONDARY_VER_DNS_IP = SECONDARY_DEFAULT_DNS_IP
    SECONDARY_VER_DNS_PORT = SECONDARY_DEFAULT_DNS_PORT
DNS_SERVERS['ver'] = (
    (PRIMARY_VER_DNS_IP, PRIMARY_VER_DNS_PORT),
    (SECONDARY_VER_DNS_IP, SECONDARY_VER_DNS_PORT))

GOOGLE_COM_DOMAIN = {
    'google.com', 'www.google.com',
    'mail.google.com', 'chatenabled.mail.google.com',
    'filetransferenabled.mail.google.com', 'apis.google.com',
    'mobile-gtalk.google.com', 'mtalk.google.com'
}
try:
    PRIMARY_GOOGLE_COM_DNS_IP = socket.gethostbyname('ns-285.awsdns-35.com')
    PRIMARY_GOOGLE_COM_DNS_PORT = 53
except:
    LOGGER.exception('failed to resolve ns-285.awsdns-35.com')
    PRIMARY_GOOGLE_COM_DNS_IP = PRIMARY_DEFAULT_DNS_IP
    PRIMARY_GOOGLE_COM_DNS_PORT = PRIMARY_DEFAULT_DNS_PORT
try:
    SECONDARY_GOOGLE_COM_DNS_IP = socket.gethostbyname('ns-914.awsdns-50.net')
    SECONDARY_GOOGLE_COM_DNS_PORT = 53
except:
    LOGGER.exception('failed to resolve ns-914.awsdns-50.net')
    SECONDARY_GOOGLE_COM_DNS_IP = SECONDARY_DEFAULT_DNS_IP
    SECONDARY_GOOGLE_COM_DNS_PORT = SECONDARY_DEFAULT_DNS_PORT
DNS_SERVERS['google-com'] = (
    (PRIMARY_GOOGLE_COM_DNS_IP, PRIMARY_GOOGLE_COM_DNS_PORT),
    (SECONDARY_GOOGLE_COM_DNS_IP, SECONDARY_GOOGLE_COM_DNS_PORT))

GOOGLE_COM_HK_DOMAIN = {'google.com.hk', 'www.google.com.hk'}
try:
    PRIMARY_GOOGLE_COM_HK_DNS_IP = socket.gethostbyname('ns-320.awsdns-40.com')
    PRIMARY_GOOGLE_COM_HK_DNS_PORT = 53
except:
    traceback.print_exc()
    PRIMARY_GOOGLE_COM_HK_DNS_IP = PRIMARY_DEFAULT_DNS_IP
    PRIMARY_GOOGLE_COM_HK_DNS_PORT = PRIMARY_DEFAULT_DNS_PORT
try:
    SECONDARY_GOOGLE_COM_HK_DNS_IP = socket.gethostbyname('ns-590.awsdns-09.net')
    SECONDARY_GOOGLE_COM_HK_DNS_PORT = 53
except:
    traceback.print_exc()
    SECONDARY_GOOGLE_COM_HK_DNS_IP = SECONDARY_DEFAULT_DNS_IP
    SECONDARY_GOOGLE_COM_HK_DNS_PORT = SECONDARY_DEFAULT_DNS_PORT
DNS_SERVERS['google-com-hk'] = (
    (PRIMARY_GOOGLE_COM_HK_DNS_IP, PRIMARY_GOOGLE_COM_HK_DNS_PORT),
    (SECONDARY_GOOGLE_COM_HK_DNS_IP, SECONDARY_GOOGLE_COM_HK_DNS_PORT))


def list_dns_servers():
    for dns_server in DNS_SERVERS.values():
        yield dns_server[0] # primary
        yield dns_server[1] # secondary


def select_dns_server(domain, is_primary):
    dns_server_name = select_dns_server_name(domain)
    if is_primary:
        dns_server = DNS_SERVERS[dns_server_name][0]
    else:
        dns_server = DNS_SERVERS[dns_server_name][1]
    return dns_server_name if is_primary else dns_server_name, dns_server[0], dns_server[1]


def select_dns_server_name(domain):
    if not domain:
        return 'default'
    if VER_DOMAIN == domain:
        return 'ver'
    if domain in GOOGLE_COM_DOMAIN:
        return 'google-com'
    if domain in GOOGLE_COM_HK_DOMAIN:
        return 'google-com-hk'
    if china_domain.is_china_domain(domain):
        return 'china'
    return 'default'
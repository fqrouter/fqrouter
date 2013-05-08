import logging
import china_domain

LOGGER = logging.getLogger('fqrouter.%s' % __name__)

PRIMARY_DEFAULT_DNS_IP = '208.67.222.222'
PRIMARY_DEFAULT_DNS_PORT = 53
SECONDARY_DEFAULT_DNS_IP = '208.67.220.220'
SECONDARY_DEFAULT_DNS_PORT = 53

DNS_SERVERS = {
    'default': (
        (PRIMARY_DEFAULT_DNS_IP, PRIMARY_DEFAULT_DNS_PORT),
        (SECONDARY_DEFAULT_DNS_IP, SECONDARY_DEFAULT_DNS_PORT)),
    'china': (
        ('114.114.114.114', 53),
        ('114.114.115.115', 53))
}

HOSTED_DOMAINS = {
    # cdn
    'd2anp67vmqk4wc.cloudfront.net',
    # google.com
    'google.com', 'www.google.com',
    'mail.google.com', 'chatenabled.mail.google.com',
    'filetransferenabled.mail.google.com', 'apis.google.com',
    'mobile-gtalk.google.com', 'mtalk.google.com',
    # google.com.hk
    'google.com.hk', 'www.google.com.hk',
    # google.cn
    'google.cn', 'www.google.cn'
}


def list_dns_servers():
    for dns_server in DNS_SERVERS.values():
        yield dns_server[0] # primary
        yield dns_server[1] # secondary


def transform_domain_to_hosted_form(domain):
    if not domain:
        return None
    if domain in HOSTED_DOMAINS:
        return '%s.fqrouter.com' % domain
    else:
        return None


def transform_domain_from_hosted_form(domain):
    if not domain:
        return None
    if domain.endswith('.fqrouter.com'):
        original_form = domain[:-len('.fqrouter.com')]
        if original_form in HOSTED_DOMAINS:
            return original_form
    return None


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
    if china_domain.is_china_domain(domain):
        return 'china'
    return 'default'
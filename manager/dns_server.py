import socket

PRIMARY_DNS_IP = '8.8.8.8'
PRIMARY_DNS_MARK = 0x1feed
EXACT_MATCH_DNS = [
    ('google.com@route53', 0x2feed, socket.gethostbyname('ns-1052.awsdns-03.org'), {
        'google.com',
        'www.google.com'
    }),
    ('google.com.hk@route53', 0x3feed, socket.gethostbyname('ns-1521.awsdns-62.org'), {
        'google.com.hk',
        'www.google.com.hk'
    })
]


def list_dns_servers():
    yield PRIMARY_DNS_MARK, PRIMARY_DNS_IP
    for server_name, mark, ip, targets in EXACT_MATCH_DNS:
        yield mark, ip


def select_dns_server(domain):
    for server_name, mark, ip, targets in EXACT_MATCH_DNS:
        if domain in targets:
            return server_name, mark
    return 'primary', PRIMARY_DNS_MARK
# *Scout* work with remote *Helpers* (STUN servers, etc) to know:
# If port of NAT(s) can be guessed (if port is in range of [local_port, local_port + N]
# External ips of my NAT(s), so that *Guesser* can make educated guess base upon that
from . import rfc_3489

def detect_nat_status(stun_servers=()):
    results = []
    for stun_server in stun_servers:
        if isinstance(stun_server, (list, tuple)):
            results.append(rfc_3489.detect_reflexive_transport_address(*stun_server))
        else:
            results.append(rfc_3489.detect_reflexive_transport_address(stun_server))
    results = [r.get(ignores_error=True) for r in results if r.get(ignores_error=True)]
    external_ips = set()
    for result in results:
        external_addr, local_port = result
        external_ip, external_port = external_addr
        if not (local_port <= external_port <= local_port + 5):
            raise Exception('Can not guess port from this type of NAT')
        external_ips.add(external_ip)
    return external_ips
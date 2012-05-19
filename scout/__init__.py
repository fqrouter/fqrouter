from . import rfc_3489

def detect_nat_status(stun_servers=()):
    addrs = []
    for stun_server in stun_servers:
        if isinstance(stun_server, (list, tuple)):
            addrs.append(rfc_3489.detect_reflexive_transport_address(*stun_server))
        else:
            addrs.append(rfc_3489.detect_reflexive_transport_address(stun_server))
    external_ips = set()
    for addr in [a.get(ignores_error=True) for a in addrs if a.get(ignores_error=True)]:
        ip, port = addr
        external_ips.add(ip)
    return external_ips
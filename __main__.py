import scout

reflexive_transport_addr = scout.detect_reflexive_transport_address(stun_server_ip='sip1.lakedestiny.cordiaip.com')
if reflexive_transport_addr:
    print(reflexive_transport_addr)
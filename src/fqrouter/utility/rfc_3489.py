# rfc_3489 is old version of STUN protocol, but serves our purpose
import socket
import binascii
import struct
from dpkt import stun, tcp
import random
from fqrouter.utility.future import async

IP_ADDR_FAMILY_V4 = 0x01

@async
def detect_reflexive_transport_address(stun_server_host, stun_server_port=3478):
# => Future((external_ip, external_port), local_port)
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        local_port = random.randint(1025, tcp.TCP_PORT_MAX)
        s.bind(('0.0.0.0', local_port))
        request = stun.STUN(type=stun.BINDING_REQUEST, len=0, xid=generateTransactionId())
        s.sendto(str(request), (stun_server_host, stun_server_port))
        response = stun.STUN(s.recv(4096))
        if request.xid != response.xid:
            return None
        buffer = response.data
        while buffer:
            t, l, v, buffer = stun.tlv(buffer)
            if stun.MAPPED_ADDRESS == t:
                return unpack_mapped_address(v), local_port
        return None
    finally:
        s.close()

# === IMPLEMENTATION ===

def generateTransactionId():
    """
    11.1  Message Header

   All STUN messages consist of a 20 byte header:

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |      STUN Message Type        |         Message Length        |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                            Transaction ID
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                                                   |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

   The Message Types can take on the following values:

      0x0001  :  Binding Request
      0x0101  :  Binding Response
      0x0111  :  Binding Error Response
      0x0002  :  Shared Secret Request
      0x0102  :  Shared Secret Response
      0x0112  :  Shared Secret Error Response

   The message length is the count, in bytes, of the size of the
   message, not including the 20 byte header.

   The transaction ID is a 128 bit identifier.  It also serves as salt
   to randomize the request and the response.  All responses carry the
   same identifier as the request they correspond to.
    """
    a = ''.join([random.choice('0123456789ABCDEF') for x in xrange(32)])
    return binascii.a2b_hex(a)


def unpack_mapped_address(bytes):
    """
    11.2.1 MAPPED-ADDRESS

   The MAPPED-ADDRESS attribute indicates the mapped IP address and
   port.  It consists of an eight bit address family, and a sixteen bit
   port, followed by a fixed length value representing the IP address.

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |x x x x x x x x|    Family     |           Port                |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                             Address                           |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

   The port is a network byte ordered representation of the mapped port.
   The address family is always 0x01, corresponding to IPv4.  The first
   8 bits of the MAPPED-ADDRESS are ignored, for the purposes of
   aligning parameters on natural boundaries.  The IPv4 address is 32
   bits.
    """
    _, ip_addr_family, port, ip_addr_1, ip_addr_2, ip_addr_3, ip_addr_4 = struct.unpack('!BBHBBBB', bytes)
    if IP_ADDR_FAMILY_V4 != ip_addr_family:
        return None
    return '%s.%s.%s.%s' % (ip_addr_1, ip_addr_2, ip_addr_3, ip_addr_4), port
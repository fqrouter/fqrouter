import dpkt.ip
import dpkt.tcp
import socket
import traceback
from netfilterqueue import NetfilterQueue

raw_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
raw_socket.setsockopt(socket.SOL_IP, socket.IP_HDRINCL, 1)
injecting_connection_packets = {} # (src, sport, dst, dport) => [packets...]
injected_connections = set()

def inject_rst_after_syn_ack(nfqueue_element):
    try:
        ip_packet = dpkt.ip.IP(nfqueue_element.get_payload())
        tcp_packet = ip_packet.tcp
        connection = get_connection(ip_packet)
        if connection in injected_connections:
            print('[success] {}'.format(repr_connection(connection)))
            nfqueue_element.accept()
        elif tcp_packet.flags & dpkt.tcp.TH_FIN and connection in injected_connections:
            print('[close] {}'.format(repr_connection(connection)))
            injected_connections.remove(connection)
            nfqueue_element.accept()
        elif tcp_packet.flags & dpkt.tcp.TH_SYN and tcp_packet.flags & dpkt.tcp.TH_ACK:
            injection_begin(ip_packet)
            nfqueue_element.drop()
        elif tcp_packet.flags & dpkt.tcp.TH_RST:
            injection_end(connection)
            nfqueue_element.drop()
        elif connection in injecting_connection_packets:
            injection_queue(ip_packet)
            nfqueue_element.drop()
        else:
            nfqueue_element.accept()
    except:
        traceback.print_exc()
        nfqueue_element.accept()


def injection_begin(syn_ack_packet):
    connection = get_connection(syn_ack_packet)
    print('[begin] {}'.format(repr_connection(connection)))
    if connection in injecting_connection_packets:
        injecting_connection_packets[connection].append(syn_ack_packet)
        return # wait longer
    injecting_connection_packets[connection] = [syn_ack_packet]

    def inject(flags, seq, ack=None):
        tcp_packet = dpkt.tcp.TCP(
            sport=syn_ack_packet.tcp.dport, dport=syn_ack_packet.tcp.sport,
            flags=flags, seq=seq,
            data='', opts='')
        if ack:
            tcp_packet.ack = ack
        ip_packet = dpkt.ip.IP(dst=syn_ack_packet.src, src=syn_ack_packet.dst, p=dpkt.ip.IP_PROTO_TCP)
        ip_packet.data = ip_packet.tcp = tcp_packet
        raw_socket.sendto(str(ip_packet), (socket.inet_ntoa(ip_packet.dst), 0))

    o_seq = syn_ack_packet.tcp.ack - 1
    o_ack = syn_ack_packet.tcp.seq
    inject(flags=dpkt.tcp.TH_RST, seq=o_seq)
    inject(flags=dpkt.tcp.TH_ACK, seq=o_seq - 1, ack=o_ack)
    inject(flags=dpkt.tcp.TH_ACK, seq=o_seq - 1, ack=o_ack)
    inject(flags=dpkt.tcp.TH_PUSH, seq=o_seq - 100, ack=o_ack)
    inject(flags=dpkt.tcp.TH_PUSH, seq=o_seq - 100, ack=o_ack)


def injection_queue(ip_packet):
    connection = get_connection(ip_packet)
    print('[queue] {}'.format(repr_connection(connection)))
    injecting_connection_packets[connection].append(ip_packet)


def injection_end(connection):
    if connection not in injecting_connection_packets:
        return
    print('[end] {}'.format(repr_connection(connection)))
    for ip_packet in injecting_connection_packets[connection]:
        print(socket.inet_ntoa(ip_packet.dst), repr(ip_packet))
        raw_socket.sendto(str(ip_packet), (socket.inet_ntoa(ip_packet.dst), 0))
    del injecting_connection_packets[connection]
    injected_connections.add(connection)


def get_connection(ip_packet):
    return socket.inet_ntoa(ip_packet.src), ip_packet.data.sport, socket.inet_ntoa(ip_packet.dst), ip_packet.data.dport


def repr_connection(connection):
    return '{}:{} => {}:{}'.format(*connection)


nfqueue = NetfilterQueue()
nfqueue.bind(0, inject_rst_after_syn_ack)
try:
    print('running..')
    nfqueue.run()
except KeyboardInterrupt:
    print('bye')

package fq.router2.utils;

import net.sf.ivmaidns.dns.DNSConnection;
import net.sf.ivmaidns.dns.DNSMsgHeader;
import net.sf.ivmaidns.dns.DNSName;
import net.sf.ivmaidns.dns.DNSRecord;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DnsUtils {
    private final static InetSocketAddress[] DNS_SERVERS = new InetSocketAddress[]{
            new InetSocketAddress("8.8.8.8", 53),
            new InetSocketAddress("208.67.222.222", 443),
            new InetSocketAddress("208.67.220.220", 443),
            new InetSocketAddress("199.91.73.222", 3389),
            new InetSocketAddress("87.118.100.175", 110),
            new InetSocketAddress("87.118.85.241", 110),
            new InetSocketAddress("77.109.139.29", 110),
            new InetSocketAddress("77.109.138.45", 110)
    };
    private final static HashSet<String> WRONG_ANSWERS = new HashSet<String>() {{
        for (String ip : new String[]{
                "4.36.66.178",
                "8.7.198.45",
                "37.61.54.158",
                "46.82.174.68",
                "59.24.3.173",
                "64.33.88.161",
                "64.33.99.47",
                "64.66.163.251",
                "65.104.202.252",
                "65.160.219.113",
                "66.45.252.237",
                "72.14.205.99",
                "72.14.205.104",
                "78.16.49.15",
                "93.46.8.89",
                "128.121.126.139",
                "159.106.121.75",
                "169.132.13.103",
                "192.67.198.6",
                "202.106.1.2",
                "202.181.7.85",
                "203.161.230.171",
                "203.98.7.65",
                "207.12.88.98",
                "208.56.31.43",
                "209.36.73.33",
                "209.145.54.50",
                "209.220.30.174",
                "211.94.66.147",
                "213.169.251.35",
                "216.221.188.182",
                "216.234.179.13",
                "243.185.187.39",
                "74.125.127.102",
                "74.125.155.102",
                "74.125.39.113",
                "74.125.39.102",
                "209.85.229.138"
        }) {
            add(ip);
        }
    }};

    public static List<Inet4Address> resolveA(String domain) throws Exception {
        for (InetSocketAddress dnsServer : DNS_SERVERS) {
            try {
                return resolveA(domain, dnsServer);
            } catch (Exception e) {
                LogUtils.e("failed to resolve: " + domain, e);
            }
        }
        return new ArrayList<Inet4Address>();
    }

    public static List<Inet4Address> resolveA(String domain, InetSocketAddress dnsServer) throws Exception {
        DNSMsgHeader qHeader = DNSMsgHeader.construct(
                DNSMsgHeader.QUERY, true, 1, 0, 0, 0, false);
        DNSRecord[] records = new DNSRecord[1];
        records[0] = new DNSRecord(new DNSName(domain, null), DNSRecord.A, DNSRecord.IN);
        byte[] query = DNSConnection.encode(qHeader, records);
        try {
            return resolveAOverUdp(dnsServer, query);
        } catch (Exception e) {
            LogUtils.e("failed to resolve over udp", e);
            return resolveAOverTcp(dnsServer, query);
        }
    }

    private static List<Inet4Address> resolveAOverTcp(InetSocketAddress dnsServer, byte[] query) throws Exception {
        DNSConnection dnsConnection = new DNSConnection();
        try {
            dnsConnection.open(dnsServer.getAddress());
            dnsConnection.send(query);
            return toIps(dnsConnection.receive(true));
        } finally {
            dnsConnection.close();
        }
    }

    private static List<Inet4Address> resolveAOverUdp(InetSocketAddress dnsServer, byte[] query) throws Exception {
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.setSoTimeout(1000);
        try {
            datagramSocket.connect(dnsServer.getAddress(), dnsServer.getPort());
            datagramSocket.send(new DatagramPacket(query, query.length));
            while (true) {
                List<Inet4Address> ips = readIps(datagramSocket);
                if (!isWrong(ips)) {
                    return ips;
                }
            }
        } finally {
            datagramSocket.close();
        }
    }

    private static List<Inet4Address> readIps(DatagramSocket datagramSocket) throws Exception {
        DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
        datagramSocket.receive(packet);
        return toIps(packet.getData());
    }

    private static List<Inet4Address> toIps(byte[] buffer) {
        DNSRecord[] records = DNSConnection.decode(buffer);
        List<Inet4Address> ips = new ArrayList<Inet4Address>();
        for (DNSRecord record : records) {
            if (DNSRecord.A == record.getRType()) {
                if (record.getRData().length > 0) {
                    ips.add((Inet4Address) record.getRData()[0]);
                }
            }
        }
        return ips;
    }

    private static boolean isWrong(List<Inet4Address> ips) {
        if (ips.isEmpty()) {
            return true;
        }
        for (Inet4Address ip : ips) {
            if (WRONG_ANSWERS.contains(ip.getHostAddress())) {
                return true;
            }
        }
        return false;
    }

    public static String resolveTXT(String domain) throws Exception {
        for (InetSocketAddress dnsServer : DNS_SERVERS) {
            try {
                return resolveTXT(domain, dnsServer);
            } catch (Exception e) {
                LogUtils.e("failed to resolve: " + domain, e);
            }
        }
        return "";
    }

    public static String resolveTXT(String domain, InetSocketAddress dnsServer) throws Exception {
        DNSMsgHeader qHeader = DNSMsgHeader.construct(
                DNSMsgHeader.QUERY, true, 1, 0, 0, 0, false);
        DNSRecord[] records = new DNSRecord[1];
        records[0] = new DNSRecord(new DNSName(domain, null), DNSRecord.TXT, DNSRecord.IN);
        byte[] request = DNSConnection.encode(qHeader, records);

        try {
            return resolveTXTOverUdp(dnsServer, request);
        } catch (Exception e) {
            LogUtils.e("failed to resolve txt over udp at " + dnsServer, e);
            return resolveTXTOverTcp(dnsServer, request);
        }
    }

    private static String resolveTXTOverUdp(InetSocketAddress dnsServer, byte[] query) throws Exception {
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.setSoTimeout(2000);
        try {
            datagramSocket.connect(dnsServer.getAddress(), dnsServer.getPort());
            datagramSocket.send(new DatagramPacket(query, query.length));
            DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
            datagramSocket.receive(packet);
            return toTXT(packet.getData());
        } finally {
            datagramSocket.close();
        }
    }

    private static String resolveTXTOverTcp(InetSocketAddress dnsServer, byte[] request) throws IOException {
        DNSConnection connection = new DNSConnection();
        try {
            connection.open(dnsServer.getAddress());
            connection.send(request);
            return toTXT(connection.receive(true));
        } finally {
            connection.close();
        }
    }

    private static String toTXT(byte[] buffer) {
        DNSRecord[] records = DNSConnection.decode(buffer);
        for (DNSRecord record : records) {
            if (DNSRecord.TXT == record.getRType() && record.getRData().length > 0) {
                return (String) record.getRData()[0];
            }
        }
        throw new RuntimeException("not found");
    }
}

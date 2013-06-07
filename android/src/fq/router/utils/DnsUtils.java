package fq.router.utils;

import net.sf.ivmaidns.dns.DNSConnection;
import net.sf.ivmaidns.dns.DNSMsgHeader;
import net.sf.ivmaidns.dns.DNSName;
import net.sf.ivmaidns.dns.DNSRecord;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class DnsUtils {

    private final static String[] DNS_SERVERS = new String[]{"8.8.8.8", "4.2.2.2", "208.67.222.222", "8.8.4.4"};

    public static List<Inet4Address> resolveA(String domain) throws Exception {
        for (String dnsServer : DNS_SERVERS) {
            try {
                return resolveA(domain, dnsServer);
            } catch (Exception e) {
                LogUtils.e("failed to resolve: " + domain, e);
            }
        }
        return new ArrayList<Inet4Address>();
    }

    public static List<Inet4Address> resolveA(String domain, String dnsServer) throws Exception {
        List<Inet4Address> ips = new ArrayList<Inet4Address>();
        DNSMsgHeader qHeader = DNSMsgHeader.construct(
                DNSMsgHeader.QUERY, true, 1, 0, 0, 0, false);
        DNSRecord[] records = new DNSRecord[1];
        records[0] = new DNSRecord(new DNSName(domain, null), DNSRecord.ANY, DNSRecord.IN);
        byte[] msgBytes = DNSConnection.encode(qHeader, records);
        DNSConnection connection = new DNSConnection();
        try {
            connection.open(InetAddress.getByName(dnsServer));
            connection.send(msgBytes);
            msgBytes = connection.receive(true);
            records = DNSConnection.decode(msgBytes);
            for (DNSRecord record : records) {
                if (DNSRecord.CNAME == record.getRType()) {
                    return resolveA(((DNSName) record.getRData()[0]).getAbsolute(), dnsServer);
                } else if (DNSRecord.A == record.getRType()) {
                    ips.add((Inet4Address) record.getRData()[0]);
                }
            }
        } finally {
            connection.close();
        }
        return ips;
    }

    public static String resolveTXT(String domain) throws Exception {
        for (String dnsServer : DNS_SERVERS) {
            try {
                return resolveTXT(domain, dnsServer);
            } catch (Exception e) {
                LogUtils.e("failed to resolve: " + domain, e);
            }
        }
        return "";
    }

    public static String resolveTXT(String domain, String dnsServer) throws Exception {
        DNSMsgHeader qHeader = DNSMsgHeader.construct(
                DNSMsgHeader.QUERY, true, 1, 0, 0, 0, false);
        DNSRecord[] records = new DNSRecord[1];
        records[0] = new DNSRecord(new DNSName(domain, null), DNSRecord.TXT, DNSRecord.IN);
        byte[] msgBytes = DNSConnection.encode(qHeader, records);
        DNSConnection connection = new DNSConnection();
        try {
            connection.open(InetAddress.getByName(dnsServer));
            connection.send(msgBytes);
            msgBytes = connection.receive(true);
            records = DNSConnection.decode(msgBytes);
            for (DNSRecord record : records) {
                if (DNSRecord.TXT == record.getRType() && record.getRData().length > 0) {
                    return (String) record.getRData()[0];
                }
            }
        } finally {
            connection.close();
        }
        return "";
    }
}

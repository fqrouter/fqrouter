/*
 * @(#) src/net/sf/ivmaidns/dns/DNSRecord.java --
 * Class for representing DNS resource record.
 **
 * Copyright (c) 1999-2001 Ivan Maidanski <ivmai@mail.ru>
 * All rights reserved.
 */

/*
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 **
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License (GPL) for more details.
 **
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 **
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module. An independent module is a module which is not derived from
 * or based on this library. If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package net.sf.ivmaidns.dns;

import java.io.InvalidObjectException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.sf.ivmaidns.util.ByteVector;
import net.sf.ivmaidns.util.Immutable;
import net.sf.ivmaidns.util.Indexable;
import net.sf.ivmaidns.util.JavaConsts;
import net.sf.ivmaidns.util.ReallyCloneable;
import net.sf.ivmaidns.util.Sortable;
import net.sf.ivmaidns.util.UnsignedInt;
import net.sf.ivmaidns.util.Verifiable;

/**
 * Class for representing DNS resource record (as defined in
 * RFC1035).
 **
 * @version 3.0
 * @author Ivan Maidanski
 */
public final class DNSRecord
 implements Immutable, ReallyCloneable, Serializable, Indexable,
            Sortable, Verifiable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 2.2
 */
 private static final long serialVersionUID = 7339921300775713171L;

/**
 * NOTE: These are standard DNS resource types (qType or rType).
 */
 public static final int A = 1; /* RFC1035 */

 public static final int NS = 2; /* RFC1035 */

 public static final int MD = 3; /* RFC1035 */

 public static final int MF = 4; /* RFC1035 */

 public static final int CNAME = 5; /* RFC1035 */

 public static final int SOA = 6; /* RFC1035 */

 public static final int MB = 7; /* RFC1035 */

 public static final int MG = 8; /* RFC1035 */

 public static final int MR = 9; /* RFC1035 */

 public static final int NULL = 10; /* RFC1035 */

 public static final int WKS = 11; /* RFC1035 */

 public static final int PTR = 12; /* RFC1035 */

 public static final int HINFO = 13; /* RFC1035 */

 public static final int MINFO = 14; /* RFC1035 */

 public static final int MX = 15; /* RFC1035 */

 public static final int TXT = 16; /* RFC1035 */

 public static final int RP = 17; /* RFC1183 */

 public static final int AFSDB = 18; /* RFC1183 */

 public static final int X25 = 19; /* RFC1183 */

 public static final int ISDN = 20; /* RFC1183 */

 public static final int RT = 21; /* RFC1183 */

 public static final int NSAP = 22; /* RFC1706 */

 public static final int NSAP_PTR = 23; /* RFC1348 */

 public static final int SIG = 24; /* RFC2535 */

 public static final int KEY = 25; /* RFC2535 */

 public static final int PX = 26; /* RFC2163 */

 public static final int GPOS = 27; /* RFC1712 */

 public static final int AAAA = 28; /* RFC1886 */

 public static final int LOC = 29; /* RFC1876 */

 public static final int NXT = 30; /* RFC2065 */

 public static final int EID = 31; /* RFC1992 */

 public static final int NIMLOC = 32; /* RFC1992 */

 public static final int SRV = 33; /* RFC2052 */

 public static final int ATMA = 34; /* RFC2225 */

 public static final int NAPTR = 35; /* RFC2168 */

 public static final int KX = 36; /* RFC2230 */

 public static final int CERT = 37; /* RFC2538 */

 public static final int DNAME = 39; /* RFC2672 */

/**
 * NOTE: These are DNS meta-data records types (rType).
 **
 * @since 2.3
 */
 public static final int OPT = 41; /* RFC2671 */

 public static final int TKEY = 249; /* RFC2930 */

 public static final int TSIG = 250; /* RFC2845 */

/**
 * NOTE: These are standard DNS additional question types (qType).
 */
 public static final int IXFR = 251; /* (zone update) RFC1995 */

 public static final int AXFR = 252; /* (entire zone) RFC1035 */

 public static final int MAILB = 253; /* (MB/MG/MR/MINFO) RFC1035 */

 public static final int MAILA = 254; /* (MD/MF/MX) RFC1035 */

/**
 * NOTE: This is standard DNS question "*" type/class (qType or
 * qClass).
 */
 public static final int ANY = 255; /* (wildcard) RFC1035 */

/**
 * NOTE: These are standard DNS resource classes (qClass or rClass).
 */
 public static final int IN = 1; /* (Internet) RFC1035 */

 public static final int CH = 3; /* (Chaos) RFC1035 */

 public static final int HS = 4; /* (Hesiod) RFC1035 */

/**
 * NOTE: This is standard DNS record "NONE" class (rClass).
 */
 public static final int NONE = 254; /* RFC2136 */

/**
 * NOTE: These are all indices for SOA resource record rData fields
 * (RFC1035).
 **
 * @since 3.0
 */
 public static final int SOA_HOST_INDEX = 0;

 public static final int SOA_EMAIL_INDEX = 1;

 public static final int SOA_SERIAL_INDEX = 2;

 public static final int SOA_REFRESH_INDEX = 3;

 public static final int SOA_RETRY_INDEX = 4;

 public static final int SOA_EXPIRE_INDEX = 5;

 public static final int SOA_MINTTL_INDEX = 6;

/**
 * NOTE: The default values for SOA resource record rData fields
 * (RFC1035).
 */
 public static final int DEFAULT_SOA_REFRESH = 3600;

 public static final int DEFAULT_SOA_RETRY = 600;

 public static final int DEFAULT_SOA_EXPIRE = 3600000;

 public static final int DEFAULT_TTL = 3600;

/**
 * NOTE: These are lengthes (in bytes) of rType, rClass, ttl,
 * rDataLen.
 **
 * @since 2.1
 */
 public static final int RTYPE_LENGTH = 2;

 public static final int RCLASS_LENGTH = 2;

 public static final int TTL_LENGTH = 4;

 public static final int RDATA_LEN_LENGTH = 2;

/**
 * NOTE: This is the length of an IPv4 address value.
 **
 * @since 2.2
 */
 public static final int INET_ADDR_LEN = 4;

/**
 * NOTE: These are integer bit masks of defined types.
 **
 * @since 2.1
 */
 public static final int RTYPE_MASK =
  RTYPE_LENGTH < JavaConsts.INT_LENGTH ?
  ~(-1 << (RTYPE_LENGTH * JavaConsts.BYTE_SIZE)) : -1;

 public static final int RCLASS_MASK =
  RCLASS_LENGTH < JavaConsts.INT_LENGTH ?
  ~(-1 << (RCLASS_LENGTH * JavaConsts.BYTE_SIZE)) : -1;

 public static final int TTL_MASK =
  TTL_LENGTH < JavaConsts.INT_LENGTH ?
  ~(-1 << (TTL_LENGTH * JavaConsts.BYTE_SIZE)) : -1;

 public static final int RDATA_LEN_MASK =
  RDATA_LEN_LENGTH < JavaConsts.INT_LENGTH ?
  ~(-1 << (RDATA_LEN_LENGTH * JavaConsts.BYTE_SIZE)) : -1;

/**
 * NOTE: This string represents a packed sorted reference list of
 * abbreviations for the standard rClass values (defined in
 * RFC1035). This string is used only by UnsignedInt
 * parseAbbreviation(str) method.
 **
 * @since 3.0
 */
 public static final String RCLASS_REFER_ABBREVS =
  ",RCLASS,," +
  "*,,,,255," +
  "ANY,,255," +
  "CH,,,,,3," +
  "HS,,,,,4," +
  "IN,,,,,1," +
  "NONE,254,";

/**
 * NOTE: This string represents a packed sorted reference list of
 * abbreviations for the standard rType values (first defined in
 * RFC1035). This string is used only by UnsignedInt
 * parseAbbreviation(str) method.
 **
 * @since 3.0
 */
 public static final String RTYPE_REFER_ABBREVS =
  ",RTYPE,,,,,," +
  "*,,,,,,,255," +
  "A,,,,,,,,,1," +
  "AAAA,,,,,28," +
  "AFSDB,,,,18," +
  "ANY,,,,,255," +
  "ATMA,,,,,34," +
  "AXFR,,,,252," +
  "CERT,,,,,37," +
  "CNAME,,,,,5," +
  "DNAME,,,,39," +
  "EID,,,,,,31," +
  "GPOS,,,,,27," +
  "HINFO,,,,13," +
  "ISDN,,,,,20," +
  "IXFR,,,,251," +
  "KEY,,,,,,25," +
  "KX,,,,,,,36," +
  "LOC,,,,,,29," +
  "MAILA,,,254," +
  "MAILB,,,253," +
  "MB,,,,,,,,7," +
  "MD,,,,,,,,3," +
  "MF,,,,,,,,4," +
  "MG,,,,,,,,8," +
  "MINFO,,,,14," +
  "MR,,,,,,,,9," +
  "MX,,,,,,,15," +
  "NAPTR,,,,35," +
  "NIMLOC,,,32," +
  "NS,,,,,,,,2," +
  "NSAP,,,,,22," +
  "NSAP-PTR,23," +
  "NSAP_PTR,23," +
  "NULL,,,,,10," +
  "NXT,,,,,,30," +
  "OPT,,,,,,41," +
  "PTR,,,,,,12," +
  "PX,,,,,,,26," +
  "RP,,,,,,,17," +
  "RT,,,,,,,21," +
  "SIG,,,,,,24," +
  "SOA,,,,,,,6," +
  "SRV,,,,,,33," +
  "TKEY,,,,249," +
  "TSIG,,,,250," +
  "TXT,,,,,,16," +
  "WKS,,,,,,11," +
  "X25,,,,,,19,";

/**
 * NOTE: This string represents a packed abbreviations list for the
 * standard wksProtocol values (defined in RFC1700). The string is
 * used only by UnsignedInt toAbbreviation(wksProtocol) method.
 **
 * @since 3.0
 */
 public static final String WKS_PROTOCOL_ABBREVS =
  ",,,,,,,,,,,," +
  "0,,,,,,,,,,," +
  "ICMP,,,,,,,," +
  "IGMP,,,,,,,," +
  "GGP,,,,,,,,," +
  "IP,,,,,,,,,," +
  "ST,,,,,,,,,," +
  "TCP,,,,,,,,," +
  "UCL,,,,,,,,," +
  "EGP,,,,,,,,," +
  "IGP,,,,,,,,," +
  "BBN-RCC-MON," +
  "NVP-II,,,,,," +
  "PUP,,,,,,,,," +
  "ARGUS,,,,,,," +
  "EMCON,,,,,,," +
  "XNET,,,,,,,," +
  "CHAOS,,,,,,," +
  "UDP,,,,,,,,," +
  "MUX,,,,,,,,," +
  "DCN-MEAS,,,," +
  "HMP,,,,,,,,," +
  "PRM,,,,,,,,," +
  "XNS-IDP,,,,," +
  "TRUNK-1,,,,," +
  "TRUNK-2,,,,," +
  "LEAF-1,,,,,," +
  "LEAF-2,,,,,," +
  "RDP,,,,,,,,," +
  "IRTP,,,,,,,," +
  "ISO-TP4,,,,," +
  "NETBLT,,,,,," +
  "MFE-NSP,,,,," +
  "MERIT-INP,,," +
  "SEP,,,,,,,,," +
  ",,,,,,,,,,,," +
  "IDPR,,,,,,,," +
  "XTP,,,,,,,,," +
  "DDP,,,,,,,,," +
  "IDPR-CMTP,,," +
  "TPPP,,,,,,,," +
  "IL,,,,,,,,,," +
  "SIP,,,,,,,,," +
  "SDRP,,,,,,,," +
  "SR,,,,,,,,,," +
  "FRAG,,,,,,,," +
  "IDRP,,,,,,,," +
  "RSVP,,,,,,,," +
  "GRE,,,,,,,,," +
  "MHRP,,,,,,,," +
  "BNA,,,,,,,,," +
  "ESP,,,,,,,,," +
  "AH,,,,,,,,,," +
  "I-NLSP,,,,,," +
  "SWIPE,,,,,,," +
  "NHRP,,,,,,,," +
  ",,,,,,,,,,,," +
  ",,,,,,,,,,,," +
  ",,,,,,,,,,,," +
  ",,,,,,,,,,,," +
  ",,,,,,,,,,,," +
  ",,,,,,,,,,,," +
  ",,,,,,,,,,,," +
  "CFTP,,,,,,,," +
  ",,,,,,,,,,,," +
  "SAT-EXPAK,,," +
  "KRYPTOLAN,,," +
  "RVD,,,,,,,,," +
  "IPPC,,,,,,,," +
  ",,,,,,,,,,,," +
  "SAT-MON,,,,," +
  "VISA,,,,,,,," +
  "IPCV,,,,,,,," +
  "CPNX,,,,,,,," +
  "CPHB,,,,,,,," +
  "WSN,,,,,,,,," +
  "PVP,,,,,,,,," +
  "BR-SAT-MON,," +
  "SUN-ND,,,,,," +
  "WB-MON,,,,,," +
  "WB-EXPAK,,,," +
  "ISO-IP,,,,,," +
  "VMTP,,,,,,,," +
  "SECURE-VMTP," +
  "VINES,,,,,,," +
  "TTP,,,,,,,,," +
  "NSFNET-IGP,," +
  "DGP,,,,,,,,," +
  "TCF,,,,,,,,," +
  "IGRP,,,,,,,," +
  "OSPFIGP,,,,," +
  "SPRITE-RPC,," +
  "LARP,,,,,,,," +
  "MTP,,,,,,,,," +
  "AX25,,,,,,,," +
  "IPIP,,,,,,,," +
  "MICP,,,,,,,," +
  "SCC-SP,,,,,," +
  "ETHERIP,,,,," +
  "ENCAP,,,,,,," +
  ",,,,,,,,,,,," +
  "GMTP,,,,,,,,";

/**
 * NOTE: This string represents a packed sorted reference list of
 * abbreviations for the standard wksProtocol values (defined in
 * RFC1700). The string is used only by UnsignedInt
 * parseAbbreviation(str) method.
 **
 * @since 3.0
 */
 public static final String WKS_PROTOCOL_REFER_ABBREVS =
  ",PROTOCOL,,,,,," +
  "3PC,,,,,,,,,34," +
  "AH,,,,,,,,,,51," +
  "ARGUS,,,,,,,13," +
  "AX.25,,,,,,,93," +
  "AX25,,,,,,,,93," +
  "BBN,,,,,,,,,10," +
  "BBN-RCC,,,,,10," +
  "BBN-RCC-MON,10," +
  "BBN-RCCMON,,10," +
  "BBN_RCC,,,,,10," +
  "BBN_RCCMON,,10," +
  "BBN_RCC_MON,10," +
  "BNA,,,,,,,,,49," +
  "BR-SAT,,,,,,76," +
  "BR-SAT-MON,,76," +
  "BR-SATMON,,,76," +
  "BR_SAT,,,,,,76," +
  "BR_SATMON,,,76," +
  "BR_SAT_MON,,76," +
  "CFTP,,,,,,,,62," +
  "CHAOS,,,,,,,16," +
  "CPHB,,,,,,,,73," +
  "CPNX,,,,,,,,72," +
  "DCN,,,,,,,,,19," +
  "DCN-MEAS,,,,19," +
  "DCN_MEAS,,,,19," +
  "DDP,,,,,,,,,37," +
  "DGP,,,,,,,,,86," +
  "EGP,,,,,,,,,,8," +
  "EMCON,,,,,,,14," +
  "ENCAP,,,,,,,98," +
  "ESP,,,,,,,,,50," +
  "ETHERIP,,,,,97," +
  "FRAG,,,,,,,,44," +
  "GGP,,,,,,,,,,3," +
  "GMTP,,,,,,,100," +
  "GRE,,,,,,,,,47," +
  "HMP,,,,,,,,,20," +
  "I-NLSP,,,,,,52," +
  "ICMP,,,,,,,,,1," +
  "IDPR,,,,,,,,35," +
  "IDPR-CMTP,,,38," +
  "IDPR_CMTP,,,38," +
  "IDRP,,,,,,,,45," +
  "IGMP,,,,,,,,,2," +
  "IGP,,,,,,,,,,9," +
  "IGRP,,,,,,,,88," +
  "IL,,,,,,,,,,40," +
  "INLSP,,,,,,,52," +
  "IP,,,,,,,,,,,4," +
  "IPCV,,,,,,,,71," +
  "IPIP,,,,,,,,94," +
  "IPPC,,,,,,,,67," +
  "IRTP,,,,,,,,28," +
  "ISO-IP,,,,,,80," +
  "ISO-TP,,,,,,29," +
  "ISO-TP4,,,,,29," +
  "ISOTP4,,,,,,29," +
  "ISO_IP,,,,,,80," +
  "ISO_TP,,,,,,29," +
  "ISO_TP4,,,,,29," +
  "I_NLSP,,,,,,52," +
  "KRYPTOLAN,,,65," +
  "LARP,,,,,,,,91," +
  "LEAF-1,,,,,,25," +
  "LEAF-2,,,,,,26," +
  "LEAF1,,,,,,,25," +
  "LEAF2,,,,,,,26," +
  "LEAF_1,,,,,,25," +
  "LEAF_2,,,,,,26," +
  "MERIT,,,,,,,32," +
  "MERIT-INP,,,32," +
  "MERIT_INP,,,32," +
  "MFE,,,,,,,,,31," +
  "MFE-NSP,,,,,31," +
  "MFE_NSP,,,,,31," +
  "MHRP,,,,,,,,48," +
  "MICP,,,,,,,,95," +
  "MTP,,,,,,,,,92," +
  "MUX,,,,,,,,,18," +
  "NETBLT,,,,,,30," +
  "NHRP,,,,,,,,54," +
  "NSFNET-IGP,,85," +
  "NSFNET_IGP,,85," +
  "NVP-2,,,,,,,11," +
  "NVP-II,,,,,,11," +
  "NVP2,,,,,,,,11," +
  "NVP_2,,,,,,,11," +
  "NVP_II,,,,,,11," +
  "OSPFIG,,,,,,89," +
  "OSPFIGP,,,,,89," +
  "PRM,,,,,,,,,21," +
  "PUP,,,,,,,,,12," +
  "PVP,,,,,,,,,75," +
  "RDP,,,,,,,,,27," +
  "RSVP,,,,,,,,46," +
  "RVD,,,,,,,,,66," +
  "SAT-EXPAK,,,64," +
  "SAT-MON,,,,,69," +
  "SAT_EXPAK,,,64," +
  "SAT_MON,,,,,69," +
  "SCC-SP,,,,,,96," +
  "SCC_SP,,,,,,96," +
  "SDRP,,,,,,,,42," +
  "SECURE-VMTP,82," +
  "SECURE_VMTP,82," +
  "SEP,,,,,,,,,33," +
  "SIP,,,,,,,,,41," +
  "SIP-FRAG,,,,44," +
  "SIP-SR,,,,,,43," +
  "SIPP-AH,,,,,51," +
  "SIPP-ESP,,,,50," +
  "SIPP_AH,,,,,51," +
  "SIPP_ESP,,,,50," +
  "SIP_FRAG,,,,44," +
  "SIP_SR,,,,,,43," +
  "SPRITE,,,,,,90," +
  "SPRITE-RPC,,90," +
  "SPRITE_RPC,,90," +
  "SR,,,,,,,,,,43," +
  "ST,,,,,,,,,,,5," +
  "SUN-ND,,,,,,77," +
  "SUN_ND,,,,,,77," +
  "SWIPE,,,,,,,53," +
  "TCF,,,,,,,,,87," +
  "TCP,,,,,,,,,,6," +
  "TP++,,,,,,,,39," +
  "TP,,,,,,,,,,39," +
  "TPPP,,,,,,,,39," +
  "TRUNK-1,,,,,23," +
  "TRUNK-2,,,,,24," +
  "TRUNK1,,,,,,23," +
  "TRUNK2,,,,,,24," +
  "TRUNK_1,,,,,23," +
  "TRUNK_2,,,,,24," +
  "TTP,,,,,,,,,84," +
  "UCL,,,,,,,,,,7," +
  "UDP,,,,,,,,,17," +
  "VINES,,,,,,,83," +
  "VISA,,,,,,,,70," +
  "VMTP,,,,,,,,81," +
  "WB-EXPAK,,,,79," +
  "WB-MON,,,,,,78," +
  "WB_EXPAK,,,,79," +
  "WB_MON,,,,,,78," +
  "WSN,,,,,,,,,74," +
  "X25,,,,,,,,,93," +
  "XNET,,,,,,,,15," +
  "XNS-IDP,,,,,22," +
  "XNS_IDP,,,,,22," +
  "XTP,,,,,,,,,36,";

/**
 * NOTE: This string represents a packed abbreviations list for the
 * standard wksPort values (defined in RFC1700). The string is used
 * only by UnsignedInt toAbbreviation(wksPort) method.
 **
 * @since 3.0
 */
 public static final String WKS_PORT_ABBREVS =
  ",,,,,,,,,,,," +
  "0,,,,,,,,,,," +
  "tcpmux,,,,,," +
  "compressman," +
  "compressnet," +
  ",,,,,,,,,,,," +
  "rje,,,,,,,,," +
  ",,,,,,,,,,,," +
  "echo,,,,,,,," +
  ",,,,,,,,,,,," +
  "discard,,,,," +
  ",,,,,,,,,,,," +
  "systat,,,,,," +
  ",,,,,,,,,,,," +
  "daytime,,,,," +
  ",,,,,,,,,,,," +
  "netstat,,,,," +
  ",,,,,,,,,,,," +
  "qotd,,,,,,,," +
  "msp,,,,,,,,," +
  "chargen,,,,," +
  "ftp-data,,,," +
  "ftp,,,,,,,,," +
  ",,,,,,,,,,,," +
  "telnet,,,,,," +
  "anymail,,,,," +
  "smtp,,,,,,,," +
  ",,,,,,,,,,,," +
  "nsw-fe,,,,,," +
  ",,,,,,,,,,,," +
  "msg-icp,,,,," +
  ",,,,,,,,,,,," +
  "msg-auth,,,," +
  ",,,,,,,,,,,," +
  "dsp,,,,,,,,," +
  ",,,,,,,,,,,," +
  "anyprinter,," +
  ",,,,,,,,,,,," +
  "time,,,,,,,," +
  "rap,,,,,,,,," +
  "rlp,,,,,,,,," +
  ",,,,,,,,,,,," +
  "graphics,,,," +
  "nameserver,," +
  "nicname,,,,," +
  "mpm-flags,,," +
  "mpm,,,,,,,,," +
  "mpm-snd,,,,," +
  "ni-ftp,,,,,," +
  "auditd,,,,,," +
  "login,,,,,,," +
  "re-mail-ck,," +
  "la-maint,,,," +
  "xns-time,,,," +
  "domain,,,,,," +
  "xns-ch,,,,,," +
  "isi-gl,,,,,," +
  "xns-auth,,,," +
  "anyterminal," +
  "xns-mail,,,," +
  "anyfs,,,,,,," +
  ",,,,,,,,,,,," +
  "ni-mail,,,,," +
  "acas,,,,,,,," +
  ",,,,,,,,,,,," +
  "covia,,,,,,," +
  "tacacs-ds,,," +
  "sql-net,,,,," +
  "bootps,,,,,," +
  "bootpc,,,,,," +
  "tftp,,,,,,,," +
  "gopher,,,,,," +
  "netrjs-1,,,," +
  "netrjs-2,,,," +
  "netrjs-3,,,," +
  "netrjs-4,,,," +
  "anydialout,," +
  "deos,,,,,,,," +
  "anyrje,,,,,," +
  "vettcp,,,,,," +
  "finger,,,,,," +
  "www-http,,,," +
  "hosts2-ns,,," +
  "xfer,,,,,,,," +
  "mit-ml-dev,," +
  "ctf,,,,,,,,," +
  "mit-ml-dev2," +
  "mfcobol,,,,," +
  "anytermlink," +
  "kerberos,,,," +
  "su-mit-tg,,," +
  "dnsix,,,,,,," +
  "mit-dov,,,,," +
  "npp,,,,,,,,," +
  "dcp,,,,,,,,," +
  "objcall,,,,," +
  "supdup,,,,,," +
  "dixie,,,,,,," +
  "swift-rvf,,," +
  "tacnews,,,,," +
  "metagram,,,," +
  "newacct,,,,," +
  "hostname,,,," +
  "iso-tsap,,,," +
  "gppitnp,,,,," +
  "acr-nema,,,," +
  "csnet-ns,,,," +
  ",,,,,,,,,,,," +
  "rtelnet,,,,," +
  "snagas,,,,,," +
  "pop2,,,,,,,," +
  "pop3,,,,,,,," +
  "sunrpc,,,,,," +
  "mcidas,,,,,," +
  "auth,,,,,,,," +
  "audionews,,," +
  "sftp,,,,,,,," +
  "ansanotify,," +
  "uucp-path,,," +
  "sqlserv,,,,," +
  "nntp,,,,,,,," +
  "cfdptkt,,,,," +
  "erpc,,,,,,,," +
  "smakynet,,,," +
  "ntp,,,,,,,,," +
  "ansatrader,," +
  "locus-map,,," +
  "unitary,,,,," +
  "locus-con,,," +
  "gss-xlicen,," +
  "pwdgen,,,,,," +
  "cisco-fna,,," +
  "cisco-tna,,," +
  "cisco-sys,,," +
  "statsrv,,,,," +
  "ingres-net,," +
  "loc-srv,,,,," +
  "profile,,,,," +
  "netbios-ns,," +
  "netbios-dgm," +
  "netbios-ssn," +
  "emfis-data,," +
  "emfis-cntl,," +
  "bl-idm,,,,,," +
  "imap2,,,,,,," +
  "news,,,,,,,,";

/**
 * NOTE: This string represents a packed sorted reference list of
 * abbreviations for the standard wksPort values (defined in
 * RFC1700). The string is used only by UnsignedInt
 * parseAbbreviation(str) method.
 **
 * @since 3.0
 */
 public static final String WKS_PORT_REFER_ABBREVS =
  ",port,,,,,,,,,,," +
  "3com-tsmux,,106," +
  "3com_tsmux,,106," +
  "acas,,,,,,,,,62," +
  "acr-nema,,,,104," +
  "acr_nema,,,,104," +
  "ansanotify,,116," +
  "ansatrader,,124," +
  "anydial,,,,,,75," +
  "anydialout,,,75," +
  "anyfilesys,,,59," +
  "anyfs,,,,,,,,59," +
  "anymail,,,,,,24," +
  "anyprinter,,,35," +
  "anyprintserv,35," +
  "anyrje,,,,,,,77," +
  "anyterminal,,57," +
  "anytermlink,,87," +
  "audionews,,,114," +
  "auditd,,,,,,,48," +
  "auth,,,,,,,,113," +
  "bl-idm,,,,,,142," +
  "bl_idm,,,,,,142," +
  "bootp,,,,,,,,67," +
  "bootpc,,,,,,,68," +
  "bootps,,,,,,,67," +
  "cfdptkt,,,,,120," +
  "chargen,,,,,,19," +
  "cisco-fna,,,130," +
  "cisco-sys,,,132," +
  "cisco-tna,,,131," +
  "cisco_fna,,,130," +
  "cisco_sys,,,132," +
  "cisco_tna,,,131," +
  "compressman,,,2," +
  "compressnet,,,3," +
  "covia,,,,,,,,64," +
  "csnet-ns,,,,105," +
  "csnet_ns,,,,105," +
  "ctf,,,,,,,,,,84," +
  "daytime,,,,,,13," +
  "dcp,,,,,,,,,,93," +
  "deos,,,,,,,,,76," +
  "discard,,,,,,,9," +
  "dixie,,,,,,,,96," +
  "dns,,,,,,,,,,53," +
  "dnsix,,,,,,,,90," +
  "domain,,,,,,,53," +
  "dsp,,,,,,,,,,33," +
  "echo,,,,,,,,,,7," +
  "emfis-cntl,,141," +
  "emfis-data,,140," +
  "emfis_cntl,,141," +
  "emfis_data,,140," +
  "erpc,,,,,,,,121," +
  "finger,,,,,,,79," +
  "ftp,,,,,,,,,,21," +
  "ftp-data,,,,,20," +
  "ftpdata,,,,,,20," +
  "ftp_data,,,,,20," +
  "gopher,,,,,,,70," +
  "gppitnp,,,,,103," +
  "graphics,,,,,41," +
  "gss-xlicen,,128," +
  "gss_xlicen,,128," +
  "hostname,,,,101," +
  "hosts-ns,,,,,81," +
  "hosts-ns2,,,,81," +
  "hosts2,,,,,,,81," +
  "hosts2-ns,,,,81," +
  "hosts2_ns,,,,81," +
  "hosts_ns,,,,,81," +
  "hosts_ns2,,,,81," +
  "http,,,,,,,,,80," +
  "imap,,,,,,,,143," +
  "imap2,,,,,,,143," +
  "ingres*net,,134," +
  "ingres-net,,134," +
  "ingresnet,,,134," +
  "ingres_net,,134," +
  "isi-gl,,,,,,,55," +
  "isi_gl,,,,,,,55," +
  "iso-tsap,,,,102," +
  "iso_tsap,,,,102," +
  "kerberos,,,,,88," +
  "la-maint,,,,,51," +
  "la_maint,,,,,51," +
  "loc-srv,,,,,135," +
  "locus-con,,,127," +
  "locus-map,,,125," +
  "locus_con,,,127," +
  "locus_map,,,125," +
  "loc_srv,,,,,135," +
  "login,,,,,,,,49," +
  "mcidas,,,,,,112," +
  "metagram,,,,,99," +
  "mfcobol,,,,,,86," +
  "mit-dov,,,,,,91," +
  "mit-ml,,,,,,,83," +
  "mit-ml-dev,,,83," +
  "mit-ml-dev2,,85," +
  "mit-ml2,,,,,,85," +
  "mit_dov,,,,,,91," +
  "mit_ml,,,,,,,83," +
  "mit_ml2,,,,,,85," +
  "mit_ml_dev,,,83," +
  "mit_ml_dev2,,85," +
  "mpm,,,,,,,,,,45," +
  "mpm-flags,,,,44," +
  "mpm-snd,,,,,,46," +
  "mpm_flags,,,,44," +
  "mpm_snd,,,,,,46," +
  "msg-auth,,,,,31," +
  "msg-icp,,,,,,29," +
  "msg_auth,,,,,31," +
  "msg_icp,,,,,,29," +
  "msp,,,,,,,,,,18," +
  "nameserver,,,42," +
  "netbios,,,,,137," +
  "netbios-dgm,138," +
  "netbios-ns,,137," +
  "netbios-ssn,139," +
  "netbios_dgm,138," +
  "netbios_ns,,137," +
  "netbios_ssn,139," +
  "netrjs-1,,,,,71," +
  "netrjs-2,,,,,72," +
  "netrjs-3,,,,,73," +
  "netrjs-4,,,,,74," +
  "netrjs1,,,,,,71," +
  "netrjs2,,,,,,72," +
  "netrjs3,,,,,,73," +
  "netrjs4,,,,,,74," +
  "netrjs_1,,,,,71," +
  "netrjs_2,,,,,72," +
  "netrjs_3,,,,,73," +
  "netrjs_4,,,,,74," +
  "netstat,,,,,,15," +
  "newacct,,,,,100," +
  "news,,,,,,,,144," +
  "ni-ftp,,,,,,,47," +
  "ni-mail,,,,,,61," +
  "nicname,,,,,,43," +
  "ni_ftp,,,,,,,47," +
  "ni_mail,,,,,,61," +
  "nntp,,,,,,,,119," +
  "npp,,,,,,,,,,92," +
  "nsw-fe,,,,,,,27," +
  "nsw_fe,,,,,,,27," +
  "ntp,,,,,,,,,123," +
  "objcall,,,,,,94," +
  "pop,,,,,,,,,110," +
  "pop2,,,,,,,,109," +
  "pop3,,,,,,,,110," +
  "profile,,,,,136," +
  "pwdgen,,,,,,129," +
  "qotd,,,,,,,,,17," +
  "rap,,,,,,,,,,38," +
  "re-mail,,,,,,50," +
  "re-mail-ck,,,50," +
  "re_mail,,,,,,50," +
  "re_mail_ck,,,50," +
  "rje,,,,,,,,,,,5," +
  "rlp,,,,,,,,,,39," +
  "rtelnet,,,,,107," +
  "sftp,,,,,,,,115," +
  "smakynet,,,,122," +
  "smtp,,,,,,,,,25," +
  "snagas,,,,,,108," +
  "sql*net,,,,,,66," +
  "sql*serv,,,,118," +
  "sql-net,,,,,,66," +
  "sql-serv,,,,118," +
  "sqlnet,,,,,,,66," +
  "sqlserv,,,,,118," +
  "sql_net,,,,,,66," +
  "sql_serv,,,,118," +
  "statsrv,,,,,133," +
  "su-mit-tg,,,,89," +
  "sun-rpc,,,,,111," +
  "sunrpc,,,,,,111," +
  "sun_rpc,,,,,111," +
  "supdup,,,,,,,95," +
  "su_mit_tg,,,,89," +
  "swift-rvf,,,,97," +
  "swift_rvf,,,,97," +
  "systat,,,,,,,11," +
  "tac-news,,,,,98," +
  "tacacs-ds,,,,65," +
  "tacacs_ds,,,,65," +
  "tacnews,,,,,,98," +
  "tac_news,,,,,98," +
  "tcpmux,,,,,,,,1," +
  "telnet,,,,,,,23," +
  "tftp,,,,,,,,,69," +
  "time,,,,,,,,,37," +
  "unitary,,,,,126," +
  "uucp,,,,,,,,117," +
  "uucp-path,,,117," +
  "uucppath,,,,117," +
  "uucp_path,,,117," +
  "vettcp,,,,,,,78," +
  "www,,,,,,,,,,80," +
  "www-http,,,,,80," +
  "www_http,,,,,80," +
  "xfer,,,,,,,,,82," +
  "xns-auth,,,,,56," +
  "xns-ch,,,,,,,54," +
  "xns-mail,,,,,58," +
  "xns-time,,,,,52," +
  "xns_auth,,,,,56," +
  "xns_ch,,,,,,,54," +
  "xns_mail,,,,,58," +
  "xns_time,,,,,52,";

/**
 * NOTE: This is (limit + 1) for number of recognized fields in
 * rData.
 **
 * @since 2.3
 */
 protected static final int RDATA_MAX_FIELDS = 11;

/**
 * NOTE: This is padded string-table for codes of rData fields.
 * Field types are encoded as follows: '0' - '9' - UnsignedInt,
 * 'I' - InetAddress (INET_ADDR_LEN bytes), 'N' - DNSName
 * (case-insensitive), 'T' - ByteVector (till the end of
 * rDataBytes), 'S' - String (case-insensitive, with unsigned byte
 * prefix for length), 'R' - ByteVector (with unsigned
 * RDATA_LEN_LENGTH prefix for length), ',' - undefined. Each line
 * contains exactly RDATA_MAX_FIELDS characters (including last ','
 * to terminate line). The rest fields and the rest record types are
 * undefined.
 **
 * @since 2.3
 */
 protected static final String RDATA_FIELDS =
  ",,,,,,,,,,," +
  "I,,,,,,,,,," /* A ip-address */ +
  "N,,,,,,,,,," /* NS hostname */ +
  "N,,,,,,,,,," /* MD hostname */ +
  "N,,,,,,,,,," /* MF hostname */ +
  "N,,,,,,,,,," /* CNAME canonic_name */ +
  "NN44444,,,," /* SOA hostname e-name serial refresh retry expire
                  min_TTL */ +
  "N,,,,,,,,,," /* MB hostname */ +
  "N,,,,,,,,,," /* MG e-name */ +
  "N,,,,,,,,,," /* MR e-name */ +
  ",,,,,,,,,,," /* NULL */ +
  "I1T,,,,,,,," /* WKS interface protocol_abbr port_abbrs_map */ +
  "N,,,,,,,,,," /* PTR canonic_name */ +
  "SS,,,,,,,,," /* HINFO "computer" "OS" */ +
  "NN,,,,,,,,," /* MINFO e-name e-name */ +
  "2N,,,,,,,,," /* MX priority hostname */ +
  "SSSSSSSSSS," /* TXT "info1" . . . */ +
  "NN,,,,,,,,," /* RP e-name txt-record-name */ +
  "2N,,,,,,,,," /* AFSDB sub-type hostname */ +
  "S,,,,,,,,,," /* X25 "PSDN-address" */ +
  "SS,,,,,,,,," /* ISDN "ISDN-address" ["sub-address"] */ +
  "2N,,,,,,,,," /* RT priority hostname */ +
  "T,,,,,,,,,," /* NSAP address (hex arbitrary dot-separated) */ +
  "N,,,,,,,,,," /* NSAP-PTR name */ +
  "2114442NT,," /* SIG rType_abbr algorithm labels_count
                  original_ttl expiration_time (UT seconds)
                  inception_time (UT seconds) key_tag signer_name
                  signature (base64) */ +
  "211T,,,,,,," /* KEY flags protocol_code algorithm
                  key_data (base64) */ +
  "2NN,,,,,,,," /* PX priority name x400-name */ +
  "SSS,,,,,,,," /* GPOS longitude latitude altitude */ +
  "T,,,,,,,,,," /* AAAA ip6-addr (len == 16, hex, colon-separated
                  shorts) */ +
  "1111444,,,," /* LOC version ( == 0, not shown) coded_size (pos 4)
                  horiz_pre (pos 5) vert_pre (pos 6)
                  latitude (pos 1) longitude (pos 2)
                  altitude (pos 3) */ +
  "NT,,,,,,,,," /* NXT next_name rType_abbrs_map */ +
  "T,,,,,,,,,," /* EID end_point_identifier (hex) */ +
  "N,,,,,,,,,," /* NIMLOC end_point_locator */ +
  "222N,,,,,,," /* SRV priority weight port hostname */ +
  "T,,,,,,,,,," /* ATMA atm_address (hex) */ +
  "22SSSN,,,,," /* NAPTR order priority "flags" "service"
                  "regular_expression" replacement */ +
  "2N,,,,,,,,," /* KX priority hostname */ +
  "221T,,,,,,," /* CERT type key_tag algorithm
                  cert_data (base64) */ +
  ",,,,,,,,,,," +
  "N,,,,,,,,,," /* DNAME domain-name */ +
  ",,,,,,,,,,," +
  "2R2R2R2R2R," /* OPT option_code1 value1 (hex) . . . */;

/**
 * NOTE: Same as RDATA_FIELDS table but for query and meta-data
 * records.
 **
 * @since 2.3
 */
 protected static final String META_RDATA_FIELDS =
  ",,,,,,,,,,," /* ANY */ +
  ",,,,,,,,,,," /* MAILA */ +
  ",,,,,,,,,,," /* MAILB */ +
  ",,,,,,,,,,," /* AXFR */ +
  ",,,,,,,,,,," /* IXFR */ +
  "N62R22R,,,," /* TSIG algorithm-name signature_time (UT seconds)
                  seconds (of allowed error) MAC (base64) msgId
                  rCode other_data (base64) */ +
  "N4422RR,,,," /* TKEY algorithm-name inception_time (UT seconds)
                  expiration_time (UT seconds) mode rCode
                  key_data (base64) other_data (base64) */;

/**
 * NOTE: A constant initialized with an instance of empty byte
 * array.
 **
 * @since 3.0
 */
 protected static final byte[] EMPTY_BYTES = {};

/**
 * NOTE: rName must be != null.
 **
 * @serial
 */
 protected final DNSName rName;

/**
 * NOTE: rType is an unsigned short.
 **
 * @serial
 */
 protected final short rType;

/**
 * NOTE: rClass is an unsigned short.
 **
 * @serial
 */
 protected final short rClass;

/**
 * NOTE: ttl is unsigned (but should be >= 0, in fact).
 **
 * @serial
 */
 protected final int ttl;

/**
 * NOTE: rDataBytes must be != null and
 * (rDataBytes length & ~RDATA_LEN_MASK) == 0.
 **
 * @serial
 */
 protected final byte[] rDataBytes;

/**
 * NOTE: Question record constructor. rName must be != null. rType
 * and rClass must be valid. ttl is set to 0, rDataBytes is set to
 * an empty byte array.
 */
 public DNSRecord(DNSName rName, int rType, int rClass)
  throws NullPointerException, IllegalArgumentException
 {
  (this.rName = rName).equals(rName);
  if ((rType & ~RTYPE_MASK) != 0)
   throw new IllegalArgumentException("rType: " +
              UnsignedInt.toString(rType, false));
  if ((rClass & ~RCLASS_MASK) != 0)
   throw new IllegalArgumentException("rClass: " +
              UnsignedInt.toString(rClass, false));
  this.rType = (short)rType;
  this.rClass = (short)rClass;
  this.ttl = 0;
  this.rDataBytes = EMPTY_BYTES;
 }

/**
 * NOTE: rName must be != null, rData must be != null, rData[index]
 * must be != null for any index. rType, rClass and rData must be
 * valid.
 */
 public DNSRecord(DNSName rName, int rType, int rClass,
         int ttl, Object[] rData)
  throws NullPointerException, IllegalArgumentException
 {
  (this.rName = rName).equals(rName);
  if ((rType & ~RTYPE_MASK) != 0)
   throw new IllegalArgumentException("rType: " +
              UnsignedInt.toString(rType, false));
  if ((rClass & ~RCLASS_MASK) != 0)
   throw new IllegalArgumentException("rClass: " +
              UnsignedInt.toString(rClass, false));
  this.rType = (short)rType;
  this.rClass = (short)rClass;
  this.ttl = ttl;
  if (((this.rDataBytes = encodeRData(rType, rData)).length &
      ~RDATA_LEN_MASK) != 0)
   throw new IllegalArgumentException("rDataBytes length: " +
              UnsignedInt.toString(this.rDataBytes.length, false));
 }

/**
 * NOTE: rName must be != null, rDataBytes must be != null. rType,
 * rClass and rDataBytes length must be valid. rDataBytes array is
 * cloned (its content is not verified).
 */
 public DNSRecord(DNSName rName, int rType, int rClass,
         int ttl, byte[] rDataBytes)
  throws NullPointerException, IllegalArgumentException
 {
  (this.rName = rName).equals(rName);
  if ((rType & ~RTYPE_MASK) != 0)
   throw new IllegalArgumentException("rType: " +
              UnsignedInt.toString(rType, false));
  if ((rClass & ~RCLASS_MASK) != 0)
   throw new IllegalArgumentException("rClass: " +
              UnsignedInt.toString(rClass, false));
  if ((rDataBytes.length & ~RDATA_LEN_MASK) != 0)
   throw new IllegalArgumentException("rDataBytes length: " +
              UnsignedInt.toString(rDataBytes.length, false));
  this.rType = (short)rType;
  this.rClass = (short)rClass;
  this.ttl = ttl;
  this.rDataBytes = (byte[])rDataBytes.clone();
 }

/**
 * NOTE: record must be != null. Constructed DNS record is with
 * canonized rName and all recognized names in rDataBytes.
 **
 * @since 2.3
 */
 public DNSRecord(DNSRecord record)
  throws NullPointerException
 {
  this.rName = new DNSName(record.rName);
  this.rClass = record.rClass;
  this.ttl = record.ttl;
  byte[] recordRDataBytes = record.rDataBytes;
  byte[] rDataBytes = (byte[])recordRDataBytes.clone();
  this.rDataBytes = canonizeRData((this.rType = record.rType) &
   RTYPE_MASK, rDataBytes) ? rDataBytes : recordRDataBytes;
 }

/**
 * NOTE: Constructor for changing ttl. record must be != null.
 * Constructed record is the same as specified one except ttl.
 **
 * @since 2.3
 */
 public DNSRecord(DNSRecord record, int ttl)
  throws NullPointerException
 {
  this.rName = record.rName;
  this.rType = record.rType;
  this.rClass = record.rClass;
  this.ttl = ttl;
  this.rDataBytes = record.rDataBytes;
 }

/**
 * NOTE: Record constructor from msgBytes array (decompression
 * supported). msgBytes must be != null, ofsRef must be != null and
 * ofsRef length > 0 (only ofsRef[0] is used). If !isResource then
 * ttl is set to 0, rDataBytes is set to empty byte array (without
 * reading them from msgBytes). ArrayIndexOutOfBoundsException is
 * thrown only if ofsRef length == 0 or 0 > ofsRef[0] or ofsRef[0]
 * >= msgBytes length. On return ofsRef[0] is new offset (just after
 * constructed record). If IllegalArgumentException is thrown then
 * if ofsRef[0] > msgBytes length then out of msgBytes array else
 * the content of msgBytes array is invalid.
 **
 * @since 2.3
 */
 public DNSRecord(byte[] msgBytes, int[] ofsRef, boolean isResource)
  throws NullPointerException, ArrayIndexOutOfBoundsException,
         IllegalArgumentException
 {
  int offset, rDataBytesLen;
  this.rName = new DNSName(msgBytes, offset = ofsRef[0]);
  if ((rDataBytesLen = DNSName.lengthOf(msgBytes, offset)) < 0)
   rDataBytesLen = -rDataBytesLen;
  int ttl = (offset += rDataBytesLen) +
   (RTYPE_LENGTH + RCLASS_LENGTH);
  if (isResource && (ttl += TTL_LENGTH + RDATA_LEN_LENGTH) > 0 &&
      msgBytes.length >= ttl)
   ttl += rDataBytesLen = UnsignedInt.getFromByteArray(msgBytes,
    ttl - RDATA_LEN_LENGTH, RDATA_LEN_LENGTH);
  if (ttl <= 0)
   ttl = -1 >>> 1;
  if ((ofsRef[0] = ttl) > msgBytes.length)
   throw new IllegalArgumentException("Out of msgBytes");
  this.rType = (short)UnsignedInt.getFromByteArray(msgBytes,
   offset, RTYPE_LENGTH);
  this.rClass = (short)UnsignedInt.getFromByteArray(msgBytes,
   offset + RTYPE_LENGTH, RCLASS_LENGTH);
  byte[] rDataBytes = EMPTY_BYTES;
  ttl = 0;
  if (isResource)
  {
   ttl = UnsignedInt.getFromByteArray(msgBytes,
    offset += RTYPE_LENGTH + RCLASS_LENGTH, TTL_LENGTH);
   System.arraycopy(msgBytes,
    offset + (TTL_LENGTH + RDATA_LEN_LENGTH),
    rDataBytes = new byte[rDataBytesLen], 0, rDataBytesLen);
   rDataBytes = decompressRData(this.rType & RTYPE_MASK,
    rDataBytes, msgBytes);
  }
  this.ttl = ttl;
  this.rDataBytes = rDataBytes;
 }

/**
 * NOTE: Method for putting resource record to (message) byte array.
 * msgBytes must be != null. Enough capacity must be ensured (at
 * least getTotalLen() bytes). If !isResource then ttl and
 * rDataBytes are ignored. If msgBytes length > baseNameOffset then
 * domain names compression is performed (using only content at
 * baseNameOffset, which must be valid and not compressed). msgBytes
 * array is altered. Result is new offset (just after this record).
 **
 * @since 2.3
 */
 public int putTo(byte[] msgBytes, int offset, boolean isResource,
         int baseNameOffset)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int rDataBytesLen;
  offset = this.rName.putTo(msgBytes, rDataBytesLen = offset);
  if (baseNameOffset < rDataBytesLen)
   offset = DNSName.compressAt(msgBytes,
    rDataBytesLen, baseNameOffset);
  UnsignedInt.putToByteArray(msgBytes, offset,
   this.rType, RTYPE_LENGTH);
  UnsignedInt.putToByteArray(msgBytes, offset + RTYPE_LENGTH,
   this.rClass, RCLASS_LENGTH);
  offset += RTYPE_LENGTH + RCLASS_LENGTH;
  if (isResource)
  {
   UnsignedInt.putToByteArray(msgBytes, offset,
    this.ttl, TTL_LENGTH);
   System.arraycopy(this.rDataBytes, 0,
    msgBytes, offset += TTL_LENGTH + RDATA_LEN_LENGTH,
    rDataBytesLen = this.rDataBytes.length);
   if (baseNameOffset < msgBytes.length)
    rDataBytesLen = compressRData(this.rType & RTYPE_MASK,
     msgBytes, offset, rDataBytesLen, baseNameOffset) - offset;
   UnsignedInt.putToByteArray(msgBytes, offset - RDATA_LEN_LENGTH,
    rDataBytesLen, RDATA_LEN_LENGTH);
   offset += rDataBytesLen;
  }
  return offset;
 }

/**
 * NOTE: Result is the total length (in bytes) of this record.
 * Result > 0.
 **
 * @since 2.3
 */
 public int getTotalLen()
 {
  return this.rName.getBytesLen() + (RTYPE_LENGTH + RCLASS_LENGTH +
   TTL_LENGTH + RDATA_LEN_LENGTH) + this.rDataBytes.length;
 }

/**
 * NOTE: Result != null.
 */
 public final DNSName getRName()
 {
  return this.rName;
 }

/**
 * NOTE: Result >= 0. Result is the same as of getRName()
 * getLevel().
 **
 * @since 3.0
 */
 public int getLevel()
 {
  return this.rName.getLevel();
 }

/**
 * NOTE: Result is an unsigned short.
 */
 public final int getRType()
 {
  return this.rType & RTYPE_MASK;
 }

/**
 * NOTE: Result is an unsigned short.
 */
 public final int getRClass()
 {
  return this.rClass & RCLASS_MASK;
 }

/**
 * NOTE: Result is unsigned.
 */
 public final int getTTL()
 {
  return this.ttl & TTL_MASK;
 }

/**
 * NOTE: Result != null, result[index] != null for any index.
 * Expected/required length for rType >= result length.
 */
 public final Object[] getRData()
 {
  return decodeRData(this.rType & RTYPE_MASK, this.rDataBytes);
 }

/**
 * NOTE: Result != null, result is a copy.
 */
 public final byte[] getRDataBytes()
 {
  return (byte[])this.rDataBytes.clone();
 }

/**
 * NOTE: Result >= 0.
 */
 public final int getRDataBytesLen()
 {
  return this.rDataBytes.length;
 }

/**
 * NOTE: Result is the number of elements accessible through
 * getAt(int).
 **
 * @since 2.2
 */
 public int length()
 {
  return 5;
 }

/**
 * NOTE: Result is (new Object[] { getRName(),
 * new UnsignedInt(getRType()), new UnsignedInt(getRClass()),
 * new UnsignedInt(getTTL()), new ByteVector(getRDataBytes())
 * })[index].
 **
 * @since 2.2
 */
 public Object getAt(int index)
  throws ArrayIndexOutOfBoundsException
 {
  if (index <= 0)
  {
   if (index == 0)
    return this.rName;
  }
   else if (index <= 3)
    return new UnsignedInt(index < 2 ? this.rType & RTYPE_MASK :
     index == 2 ? this.rClass & RCLASS_MASK : this.ttl & TTL_MASK);
    else if (index == 4)
     return new ByteVector((byte[])this.rDataBytes.clone());
  throw new ArrayIndexOutOfBoundsException(index);
 }

/**
 * NOTE: rData must be != null, rData[index] must be != null for any
 * index. rData length may be not adequate to rType. rData
 * elements/fields must be instances of ByteArray, DNSName,
 * InetAddress, String or UnsignedInt (according to rType and field
 * number). Result != null (result length is not verified).
 */
 public static final byte[] encodeRData(int rType, Object[] rData)
  throws NullPointerException, IllegalArgumentException
 {
  int offset = 0, code, rDataLen = rData.length, bytesLen = 0;
  byte[] rDataBytes = EMPTY_BYTES;
  String fields = META_RDATA_FIELDS;
  int rTypeValue = rType;
  if ((code = JavaConsts.BYTE_MASK - rType) < 0 ||
      fields.length() / RDATA_MAX_FIELDS <= code)
  {
   code = 0;
   if ((fields = RDATA_FIELDS).length() / RDATA_MAX_FIELDS >
       rType && rType >= 0)
    code = rType;
  }
  byte[] newBytes;
  rType = code * RDATA_MAX_FIELDS;
  for (int index = 0; index < rDataLen; index++)
  {
   Object value = rData[index];
   value.equals(value);
   int len = -1;
   if ((char)((code = fields.charAt(rType++)) - '0') <= '9' - '0')
   {
    if (value instanceof Number)
    {
     len = ((Number)value).intValue();
     if ((code -= '0') >= JavaConsts.INT_LENGTH ||
         ((-1 << (code * JavaConsts.BYTE_SIZE)) & len) == 0)
     {
      if (bytesLen - offset < code)
      {
       if (offset + code <= 0)
        bytesLen = -1 >>> 1;
        else if ((bytesLen += bytesLen >> 1) <= offset + code)
         bytesLen = offset + code;
       System.arraycopy(rDataBytes, 0,
        newBytes = new byte[bytesLen], 0, offset);
       rDataBytes = newBytes;
      }
      UnsignedInt.putToByteArray(rDataBytes, offset, len, code);
      len = code;
     }
      else len = -1;
    }
   }
    else if (code != 'S')
    {
     byte[] bytes = null;
     if (code == 'I')
     {
      if (value instanceof InetAddress &&
          (bytes = ((InetAddress)value).getAddress()).length !=
          INET_ADDR_LEN)
       bytes = null;
     }
      else if (code == 'N')
      {
       if (value instanceof DNSName)
        bytes = ((DNSName)value).getBytes();
      }
       else if ((code == 'T' || code == 'R') &&
                value instanceof ByteVector)
       {
        bytes = ((ByteVector)value).array();
        if (code == 'R')
         if ((bytes.length & ~RDATA_LEN_MASK) == 0)
          offset += RDATA_LEN_LENGTH;
          else bytes = null;
         else if (bytes.length <= 0)
          bytes = null;
          else if (offset <= 0)
           bytes = (byte[])bytes.clone();
       }
     if (bytes != null)
     {
      len = bytes.length;
      if (offset != 0)
      {
       if (bytesLen - offset < len)
       {
        if (offset + len <= 0)
         bytesLen = -1 >>> 1;
         else if ((bytesLen += bytesLen >> 1) <= offset + len)
          bytesLen = offset + len;
        System.arraycopy(rDataBytes, 0,
         newBytes = new byte[bytesLen], 0,
         code == 'R' ? offset - RDATA_LEN_LENGTH : offset);
        rDataBytes = newBytes;
       }
       if (code == 'R')
        UnsignedInt.putToByteArray(rDataBytes,
         offset - RDATA_LEN_LENGTH, len, RDATA_LEN_LENGTH);
       System.arraycopy(bytes, 0, rDataBytes, offset, len);
      }
       else
       {
        rDataBytes = bytes;
        bytesLen = len;
       }
     }
    }
     else if (value instanceof String)
     {
      String str = (String)value;
      if (((len = str.length()) & ~JavaConsts.BYTE_MASK) == 0)
      {
       if (++len > bytesLen - offset)
       {
        if (offset + len <= 0)
         bytesLen = -1 >>> 1;
         else if ((bytesLen += bytesLen >> 1) <= offset + len)
          bytesLen = offset + len;
        System.arraycopy(rDataBytes, 0,
         newBytes = new byte[bytesLen], 0, offset);
        rDataBytes = newBytes;
       }
       while (--len > 0 &&
              (code = str.charAt(len - 1)) <= JavaConsts.BYTE_MASK)
        rDataBytes[offset + len] = (byte)code;
      }
      if (len == 0)
       rDataBytes[offset++] = (byte)(len = str.length());
       else len = -1;
     }
   if (len < 0)
    throw new IllegalArgumentException("rData[" +
               UnsignedInt.toString(index, false) +
               "]: " + value.toString() + " (" +
               rTypeAbbreviation(rTypeValue) + " type)");
   offset += len;
  }
  if (bytesLen > offset)
  {
   System.arraycopy(rDataBytes, 0,
    newBytes = new byte[offset], 0, offset);
   rDataBytes = newBytes;
  }
  return rDataBytes;
 }

/**
 * NOTE: rDataBytes must be != null (may be ill-formed). Extra bytes
 * are ignored. Result != null and result[index] != null for any
 * index (each result[index] is instance of ByteArray, DNSName,
 * InetAddress, String or UnsignedInt according to rType). Result
 * length may be less than expected.
 */
 public static final Object[] decodeRData(int rType,
         byte[] rDataBytes)
  throws NullPointerException
 {
  int offset = 0, code, bytesLen = rDataBytes.length;
  String fields = META_RDATA_FIELDS;
  if ((code = JavaConsts.BYTE_MASK - rType) < 0 ||
      fields.length() / RDATA_MAX_FIELDS <= code)
  {
   code = 0;
   if ((fields = RDATA_FIELDS).length() / RDATA_MAX_FIELDS >
       rType && rType >= 0)
    code = rType;
  }
  for (rType = code *= RDATA_MAX_FIELDS;
       fields.charAt(code) != ','; code++);
  Object[] rData = new Object[code - rType];
  for (int index = 0, len;
       (code = fields.charAt(rType++)) != ','; index++)
  {
   Object value = null;
   if ((char)(len = code - '0') <= '9' - '0')
   {
    if (bytesLen - offset >= len)
     value = new UnsignedInt(
      UnsignedInt.getFromByteArray(rDataBytes, offset, len));
   }
    else if (code == 'I')
    {
     try
     {
      if ((len = INET_ADDR_LEN) <= bytesLen - offset)
       value = InetAddress.getByName(ByteVector.toString(rDataBytes,
        offset, len, '.', true));
     }
     catch (UnknownHostException e) {}
    }
     else if (offset < bytesLen)
      if (code == 'N')
      {
       if ((len = DNSName.lengthOf(rDataBytes, offset)) > 0)
       {
        try
        {
         value = new DNSName(rDataBytes, offset);
        }
        catch (IllegalArgumentException e) {}
       }
      }
       else if (code != 'S')
       {
        len = bytesLen - offset;
        if (code != 'R' || len >= RDATA_LEN_LENGTH &&
            (len = UnsignedInt.getFromByteArray(rDataBytes,
            offset, RDATA_LEN_LENGTH)) <=
            bytesLen - offset - RDATA_LEN_LENGTH)
        {
         byte[] bytes;
         if (code == 'R')
          offset += RDATA_LEN_LENGTH;
         System.arraycopy(rDataBytes, offset,
          bytes = new byte[len], 0, len);
         value = new ByteVector(bytes);
        }
       }
        else if ((len = rDataBytes[offset] &
                 JavaConsts.BYTE_MASK) < bytesLen - offset)
        {
         char[] chars = new char[len];
         int end = offset += len + 1;
         while (len > 0)
          chars[--len] =
           (char)(rDataBytes[--offset] & JavaConsts.BYTE_MASK);
         offset = end;
         value = new String(chars);
        }
   if ((rData[index] = value) == null)
   {
    Object[] newRData;
    System.arraycopy(rData, 0,
     newRData = new Object[index], 0, index);
    rData = newRData;
    break;
   }
   offset += len;
  }
  return rData;
 }

/**
 * NOTE: rDataBytes must be != null, msgBytes must be != null.
 * Result != null. DNS name decompression is supported through
 * msgBytes. If result == rDataBytes then rDataBytes content is not
 * compressed. Original rDataBytes array itself and msgBytes array
 * are not altered.
 */
 public static final byte[] decompressRData(int rType,
         byte[] rDataBytes, byte[] msgBytes)
  throws NullPointerException
 {
  int offset = 0, code, bytesLen = rDataBytes.length;
  String fields = META_RDATA_FIELDS;
  if ((code = JavaConsts.BYTE_MASK - rType) < 0 ||
      fields.length() / RDATA_MAX_FIELDS <= code)
  {
   code = 0;
   if ((fields = RDATA_FIELDS).length() / RDATA_MAX_FIELDS >
       rType && rType >= 0)
    code = rType;
  }
  byte[] bytes;
  for (rType = code * RDATA_MAX_FIELDS;
       offset < bytesLen; offset += code)
   if ((code = fields.charAt(rType++)) == 'N')
    if ((bytes =
        DNSName.decompress(rDataBytes, offset, msgBytes)) != null &&
        (code = DNSName.lengthOf(rDataBytes = bytes, offset)) > 0)
     bytesLen = rDataBytes.length;
     else break;
    else if (code == 'I')
     code = INET_ADDR_LEN;
     else if (code == 'S')
      code = (rDataBytes[offset] & JavaConsts.BYTE_MASK) + 1;
      else if (code == 'R')
       if (bytesLen - offset >= RDATA_LEN_LENGTH)
        code = UnsignedInt.getFromByteArray(rDataBytes,
         offset, RDATA_LEN_LENGTH) + RDATA_LEN_LENGTH;
        else break;
       else if ((char)(code -= '0') > '9' - '0')
        break;
  return rDataBytes;
 }

/**
 * NOTE: If offset > baseNameOffset then compression of recognized
 * domain names is performed if possible (using only content at
 * baseNameOffset, which must be valid and not compressed). msgBytes
 * array is altered. Result is new offset (just after this processed
 * rData bytes).
 **
 * @since 2.3
 */
 public static final int compressRData(int rType, byte[] msgBytes,
         int offset, int rDataBytesLen, int baseNameOffset)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int code, newOffset;
  String fields = META_RDATA_FIELDS;
  if ((code = JavaConsts.BYTE_MASK - rType) < 0 ||
      fields.length() / RDATA_MAX_FIELDS <= code)
  {
   code = 0;
   if ((fields = RDATA_FIELDS).length() / RDATA_MAX_FIELDS >
       rType && rType >= 0)
    code = rType;
  }
  for (rType = code * RDATA_MAX_FIELDS; rDataBytesLen > 0;
       offset += code, rDataBytesLen -= code)
   if ((code = fields.charAt(rType++)) == 'N')
    if ((code = DNSName.lengthOf(msgBytes, offset)) > 0)
    {
     if ((newOffset = DNSName.compressAt(msgBytes,
         offset, baseNameOffset)) - offset < code)
     {
      System.arraycopy(msgBytes, offset + code,
       msgBytes, newOffset, rDataBytesLen -= code);
      offset = newOffset;
      code = 0;
     }
    }
     else break;
    else if (code == 'I')
     code = INET_ADDR_LEN;
     else if (code == 'S')
      code = (msgBytes[offset] & JavaConsts.BYTE_MASK) + 1;
      else if (code == 'R')
       if (rDataBytesLen >= RDATA_LEN_LENGTH)
        code = UnsignedInt.getFromByteArray(msgBytes,
         offset, RDATA_LEN_LENGTH) + RDATA_LEN_LENGTH;
        else break;
       else if ((char)(code -= '0') > '9' - '0')
        break;
  return offset + rDataBytesLen;
 }

/**
 * NOTE: rDataBytes must be != null. All found domain names in rData
 * are canonized as specified in RFC2535. Result is true if and only
 * if rDataBytes array is altered.
 **
 * @since 2.3
 */
 public static final boolean canonizeRData(int rType,
         byte[] rDataBytes)
  throws NullPointerException
 {
  int offset = 0, code, bytesLen = rDataBytes.length;
  String fields = META_RDATA_FIELDS;
  if ((code = JavaConsts.BYTE_MASK - rType) < 0 ||
      fields.length() / RDATA_MAX_FIELDS <= code)
  {
   code = 0;
   if ((fields = RDATA_FIELDS).length() / RDATA_MAX_FIELDS >
       rType && rType >= 0)
    code = rType;
  }
  boolean changed = false;
  for (rType = code * RDATA_MAX_FIELDS;
       offset < bytesLen; offset += code)
   if ((code = fields.charAt(rType++)) == 'N')
    if ((code = DNSName.lengthOf(rDataBytes, offset)) > 0)
     changed |= DNSName.canonize(rDataBytes, offset);
     else break;
    else if (code == 'I')
     code = INET_ADDR_LEN;
     else if (code == 'S')
      code = (rDataBytes[offset] & JavaConsts.BYTE_MASK) + 1;
      else if (code == 'R')
       if (bytesLen - offset >= RDATA_LEN_LENGTH)
        code = UnsignedInt.getFromByteArray(rDataBytes,
         offset, RDATA_LEN_LENGTH) + RDATA_LEN_LENGTH;
        else break;
       else if ((char)(code -= '0') > '9' - '0')
        break;
  return changed;
 }

/**
 * NOTE: rDataBytes must be != null. Only recognized rData fields
 * are hashed. Domain names and strings are hashed ignoring letters
 * case (to comply with RFC2535).
 */
 public static final int hashCodeRData(int rType, byte[] rDataBytes)
  throws NullPointerException
 {
  int offset = 0, hash = 0, code, bytesLen = rDataBytes.length;
  String fields = META_RDATA_FIELDS;
  if ((code = JavaConsts.BYTE_MASK - rType) < 0 ||
      fields.length() / RDATA_MAX_FIELDS <= code)
  {
   code = 0;
   if ((fields = RDATA_FIELDS).length() / RDATA_MAX_FIELDS >
       rType && rType >= 0)
    code = rType;
  }
  rType = code * RDATA_MAX_FIELDS;
  while (offset < bytesLen)
   if ((code = fields.charAt(rType++)) == 'N' || code == 'S')
   {
    if (code == 'S')
     if ((code = rDataBytes[offset] & JavaConsts.BYTE_MASK) <
         bytesLen - offset)
      hash ^= code;
      else break;
     else if ((code =
              DNSName.lengthOf(rDataBytes, offset) - 1) >= 0)
      hash ^= rDataBytes[offset] & JavaConsts.BYTE_MASK;
      else break;
    offset++;
    for (int value; code-- > 0; hash = ((hash << 5) - hash) ^ value)
     if ((char)((value = rDataBytes[offset++] &
         JavaConsts.BYTE_MASK) - 'A') <= 'Z' - 'A')
      value += 'a' - 'A';
    hash = (hash << 5) - hash;
   }
    else
    {
     if (code == 'I')
      code = INET_ADDR_LEN;
      else if (code == 'R')
       if (bytesLen - offset >= RDATA_LEN_LENGTH)
        code = UnsignedInt.getFromByteArray(rDataBytes,
         offset, RDATA_LEN_LENGTH) + RDATA_LEN_LENGTH;
        else break;
       else if (code == 'T')
        code = bytesLen - offset;
        else if ((char)(code -= '0') > '9' - '0')
         break;
     if (bytesLen - offset < code)
      break;
     while (code-- > 0)
     {
      hash ^= rDataBytes[offset++] & JavaConsts.BYTE_MASK;
      hash = (hash << 5) - hash;
     }
    }
  return hash ^ offset;
 }

/**
 * NOTE: rDataBytesA and rDataBytesB must be != null. If
 * !decodeRData then rData contents are treated as unsigned byte
 * arrays with upper-case letters in domain names converted to
 * lower-case (as specified in RFC2535). Else any recognized domain
 * name in rData is compared in label-by-label lower-case manner,
 * any found string is compared (before comparing length of string)
 * in the lower-case manner, content of any recognized ByteVector is
 * compared before its length.
 */
 public static final int compareRData(int rType, byte[] rDataBytesA,
         byte[] rDataBytesB, boolean decodeRData)
  throws NullPointerException
 {
  if (rDataBytesA != rDataBytesB)
  {
   int offset = 0, code = rDataBytesA.length, bytesLen, diff = 0;
   if ((bytesLen = rDataBytesB.length) >= code)
    bytesLen = code;
   String fields = META_RDATA_FIELDS;
   if ((code = JavaConsts.BYTE_MASK - rType) < 0 ||
       fields.length() / RDATA_MAX_FIELDS <= code)
   {
    code = 0;
    if ((fields = RDATA_FIELDS).length() / RDATA_MAX_FIELDS >
        rType && rType >= 0)
     code = rType;
   }
   rType = code * RDATA_MAX_FIELDS;
   while (offset < bytesLen)
   {
    if ((code = fields.charAt(rType++)) == 'N')
     if ((code = DNSName.lengthOf(rDataBytesA, offset)) <= 0 ||
         (diff = DNSName.lengthOf(rDataBytesB, offset)) <= 0)
     {
      diff = 0;
      code = bytesLen - offset;
     }
      else
      {
       if (decodeRData)
       {
        diff = DNSName.compareNames(rDataBytesA, offset,
         rDataBytesB, offset);
        offset += code;
       }
        else for (code = rDataBytesA[offset] & JavaConsts.BYTE_MASK;
                  (diff = code - (rDataBytesB[offset++] &
                  JavaConsts.BYTE_MASK)) == 0 && code > 0;
                  code = rDataBytesA[offset] & JavaConsts.BYTE_MASK)
         for (int valueA, valueB; code-- > 0; offset++)
          if (rDataBytesA[offset] != rDataBytesB[offset])
          {
           if ((char)((valueA = rDataBytesA[offset] &
               JavaConsts.BYTE_MASK) - 'A') <= 'Z' - 'A')
            valueA += 'a' - 'A';
           if ((char)((valueB = rDataBytesB[offset] &
               JavaConsts.BYTE_MASK) - 'A') <= 'Z' - 'A')
            valueB += 'a' - 'A';
           if ((valueA -= valueB) != 0)
            return valueA;
          }
       code = 0;
      }
     else if (code == 'S')
     {
      code = rDataBytesA[offset] & JavaConsts.BYTE_MASK;
      if (decodeRData)
      {
       if ((diff = code -
           (rDataBytesB[offset++] & JavaConsts.BYTE_MASK)) > 0)
        code -= diff;
       if (bytesLen - offset <= code)
        code = bytesLen - offset;
       for (int valueA, valueB; code-- > 0; offset++)
        if (rDataBytesA[offset] != rDataBytesB[offset])
        {
         if ((char)((valueA = rDataBytesA[offset] &
             JavaConsts.BYTE_MASK) - 'A') <= 'Z' - 'A')
          valueA += 'a' - 'A';
         if ((char)((valueB = rDataBytesB[offset] &
             JavaConsts.BYTE_MASK) - 'A') <= 'Z' - 'A')
          valueB += 'a' - 'A';
         if ((valueA -= valueB) != 0)
          return valueA;
        }
      }
      code++;
     }
      else if (code == 'I')
       code = INET_ADDR_LEN;
       else if (code == 'R')
       {
        if ((code = RDATA_LEN_LENGTH) <= bytesLen - offset)
        {
         code = UnsignedInt.getFromByteArray(rDataBytesA,
          offset, RDATA_LEN_LENGTH) + RDATA_LEN_LENGTH;
         if (decodeRData)
         {
          if ((diff = (code -= RDATA_LEN_LENGTH) -
              UnsignedInt.getFromByteArray(rDataBytesB,
              offset, RDATA_LEN_LENGTH)) >= 0)
           code -= diff;
          offset += RDATA_LEN_LENGTH;
         }
        }
       }
        else if ((char)(code -= '0') > '9' - '0')
         code = bytesLen - offset;
    if (bytesLen - offset <= code)
     code = bytesLen - offset;
    while (code-- > 0)
    {
     if (rDataBytesA[offset] != rDataBytesB[offset])
      return (rDataBytesA[offset] & JavaConsts.BYTE_MASK) -
       (rDataBytesB[offset] & JavaConsts.BYTE_MASK);
     offset++;
    }
    if (diff != 0)
     return diff;
   }
  }
  return rDataBytesA.length - rDataBytesB.length;
 }

/**
 * NOTE: Result >= decodeRData(rType, rDataBytes) length.
 **
 * @since 3.0
 */
 public static final int maxRDataLen(int rType)
 {
  int code;
  String fields = META_RDATA_FIELDS;
  if ((code = JavaConsts.BYTE_MASK - rType) < 0 ||
      fields.length() / RDATA_MAX_FIELDS <= code)
  {
   code = 0;
   if ((fields = RDATA_FIELDS).length() / RDATA_MAX_FIELDS >
       rType && rType >= 0)
    code = rType;
  }
  for (rType = code *= RDATA_MAX_FIELDS;
       fields.charAt(code) != ','; code++);
  return code - rType;
 }

/**
 * NOTE: str must be != null, beginIndex and endIndex must be in the
 * range. Result is true if and only if str region represents a
 * valid IP address according to RFC1123 (that is, has a form of
 * 'd.d.d.d' (with a possible '.' at the end), where 'd' is a
 * decimal value within the unsigned byte range with some possible
 * leading zeros).
 **
 * @since 2.0
 */
 public static final boolean isIPAddress(String str,
         int beginIndex, int endIndex)
  throws NullPointerException, StringIndexOutOfBoundsException
 {
  int count = str.length();
  if (beginIndex < 0)
   throw new StringIndexOutOfBoundsException(beginIndex);
  if (endIndex < beginIndex || endIndex > count)
   throw new StringIndexOutOfBoundsException(endIndex);
  if (endIndex - beginIndex >= (INET_ADDR_LEN << 1) - 1)
  {
   int group = INET_ADDR_LEN - 1, value = 0;
   count = 0;
   do
   {
    char ch;
    if ((ch = str.charAt(beginIndex++)) != '.')
    {
     ch -= '0';
     if (ch > '9' - '0' || (value =
         value * ('9' - '0' + 1) + ch) > JavaConsts.BYTE_MASK)
      return false;
     count++;
    }
     else if (count <= 0)
      return false;
      else if (beginIndex < endIndex && --group >= 0)
       value = count = 0;
       else break;
   } while (beginIndex < endIndex);
   if (group == 0)
    return true;
  }
  return false;
 }

/**
 * NOTE: host must be != null. host address is converted to reversed
 * Inet host address name (according to RFC1035). Result != null.
 **
 * @since 2.0
 */
 public static final DNSName toRevAddressName(InetAddress host)
  throws NullPointerException
 {
  byte[] address = host.getAddress();
  StringBuffer sBuf = new StringBuffer(15);
  int offset;
  if ((offset = address.length) > 0)
   do
   {
    sBuf.append(UnsignedInt.toString(address[--offset] &
     JavaConsts.BYTE_MASK, true));
    if (offset <= 0)
     break;
    sBuf.append((char)DNSName.SEPARATOR);
   } while (true);
  return new DNSName(new String(sBuf), DNSName.IN_ADDR_ARPA);
 }

/**
 * NOTE: ptr must be != null. Host ptr is converted to Inet host
 * address (according to RFC1035). Result != null.
 **
 * @since 2.0
 */
 public static final InetAddress fromRevAddressName(DNSName ptr)
  throws NullPointerException, IllegalArgumentException
 {
  if (ptr.isInDomain(DNSName.IN_ADDR_ARPA, true))
  {
   StringBuffer sBuf = new StringBuffer(15);
   int index = DNSName.IN_ADDR_ARPA.getLevel();
   int level = ptr.getLevel();
   do
   {
    sBuf.append(ptr.getLabelAt(index++));
    if (index >= level)
     break;
    sBuf.append('.');
   } while (true);
   String str = new String(sBuf);
   if (isIPAddress(str, 0, str.length()))
   {
    try
    {
     return InetAddress.getByName(str);
    }
    catch (UnknownHostException e) {}
   }
  }
  throw new IllegalArgumentException("ptr: " + ptr.getAbsolute());
 }

/**
 * NOTE: hostAAAA must be != null, offset and len must be valid.
 * hostAAAA IPv6 address is converted to reversed Inet IPv6 host
 * address name (according to RFC1886). Result != null.
 **
 * @since 2.0
 */
 public static final DNSName toRevIp6Name(byte[] hostAAAA,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int digit = hostAAAA.length;
  byte value;
  digit = 0;
  if (len > 0)
  {
   value = hostAAAA[offset];
   value = hostAAAA[(offset += len) - 1];
   if ((digit = (len << 2) - 1) < 0)
    digit = -1 >>> 1;
  }
  StringBuffer sBuf = new StringBuffer(digit);
  if (len > 0)
   do
   {
    if ((digit = (value = hostAAAA[--offset]) &
        ((1 << 4) - 1)) > '9' - '0')
     digit += 'a' - '9' - 1;
    sBuf.append((char)(digit + '0')).
     append((char)DNSName.SEPARATOR);
    if ((digit = (value >>> 4) & ((1 << 4) - 1)) > '9' - '0')
     digit += 'a' - '9' - 1;
    sBuf.append((char)(digit + '0'));
    if (--len <= 0)
     break;
    sBuf.append((char)DNSName.SEPARATOR);
   } while (true);
  return new DNSName(new String(sBuf), DNSName.IP6_INT);
 }

/**
 * NOTE: ptr must be != null. Host IPv6 ptr is converted to Inet
 * IPv6 host address (according to RFC1886). Result != null, result
 * length >= 0.
 **
 * @since 2.0
 */
 public static final byte[] fromRevIp6Name(DNSName ptr)
  throws NullPointerException, IllegalArgumentException
 {
  if (ptr.isInDomain(DNSName.IP6_INT, false))
  {
   char digit;
   int index = DNSName.IP6_INT.getLevel();
   int count = ptr.getLevel() - index, offset = 0;
   byte[] hostAAAA = new byte[(count + 1) >> 1];
   do
   {
    if (--count < 0)
     return hostAAAA;
    String label;
    if ((label = ptr.getLabelAt(index++)).length() != 1)
     break;
    if ((digit = (char)(label.charAt(0) - '0')) > '9' - '0')
    {
     digit -= 'A' - '0';
     if (digit >= 'a' - 'A')
      digit -= 'a' - 'A';
     if (digit >= (1 << 4) - ('9' - '0' + 1))
      break;
     digit += '9' - '0' + 1;
    }
    if ((offset & 1) == 0)
     hostAAAA[offset >> 1] = (byte)(digit << 4);
     else hostAAAA[offset >> 1] |= (byte)digit;
    offset++;
   } while (true);
  }
  throw new IllegalArgumentException("ptr: " + ptr.getAbsolute());
 }

/**
 * NOTE: Result != null, result length() > 0, result is 'in-line'.
 * These abbreviations are defined in RFC1035.
 */
 public static final String rClassAbbreviation(int rClass)
 {
  if (rClass == IN)
   return "IN";
  if (rClass == ANY)
   return "*";
  if (rClass == NONE)
   return "NONE";
  if (rClass == HS)
   return "HS";
  if (rClass == CH)
   return "CH";
  return "RCLASS" + UnsignedInt.toString(rClass, false);
 }

/**
 * NOTE: Result != null, result length() > 0, result is 'in-line'.
 * These abbreviations are first defined in RFC1035.
 */
 public static final String rTypeAbbreviation(int rType)
 {
  switch (rType)
  {
   case A:
    return "A";
   case NS:
    return "NS";
   case MD:
    return "MD";
   case MF:
    return "MF";
   case CNAME:
    return "CNAME";
   case SOA:
    return "SOA";
   case MB:
    return "MB";
   case MG:
    return "MG";
   case MR:
    return "MR";
   case NULL:
    return "NULL";
   case WKS:
    return "WKS";
   case PTR:
    return "PTR";
   case HINFO:
    return "HINFO";
   case MINFO:
    return "MINFO";
   case MX:
    return "MX";
   case TXT:
    return "TXT";
   case RP:
    return "RP";
   case AFSDB:
    return "AFSDB";
   case X25:
    return "X25";
   case ISDN:
    return "ISDN";
   case RT:
    return "RT";
   case NSAP:
    return "NSAP";
   case NSAP_PTR:
    return "NSAP-PTR";
   case SIG:
    return "SIG";
   case KEY:
    return "KEY";
   case PX:
    return "PX";
   case GPOS:
    return "GPOS";
   case AAAA:
    return "AAAA";
   case LOC:
    return "LOC";
   case NXT:
    return "NXT";
   case EID:
    return "EID";
   case NIMLOC:
    return "NIMLOC";
   case SRV:
    return "SRV";
   case ATMA:
    return "ATMA";
   case NAPTR:
    return "NAPTR";
   case KX:
    return "KX";
   case CERT:
    return "CERT";
   case DNAME:
    return "DNAME";
   case OPT:
    return "OPT";
  }
  switch (rType)
  {
   case TKEY:
    return "TKEY";
   case TSIG:
    return "TSIG";
   case IXFR:
    return "IXFR";
   case AXFR:
    return "AXFR";
   case MAILB:
    return "MAILB";
   case MAILA:
    return "MAILA";
   case ANY:
    return "*";
  }
  return "RTYPE" + UnsignedInt.toString(rType, false);
 }

/**
 * NOTE: portMap must be != null, offset and len must be valid.
 * Result != null, result is 'in-line' (defined in RFC1035).
 */
 public static final String wksPortMapToString(byte[] portMap,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int wksPort = portMap.length;
  if (len > 0)
   wksPort = portMap[offset] | portMap[offset + len - 1];
  byte bits = 0, oldBits;
  StringBuffer sBuf = new StringBuffer(58);
  boolean next = false;
  wksPort = 0;
  len++;
  do
  {
   if ((oldBits = (byte)(bits << 1)) == 0)
    if (--len > 0)
     bits = portMap[offset++];
     else break;
   if (bits < 0)
   {
    if (next)
     sBuf.append(' ');
    sBuf.append(UnsignedInt.toAbbreviation(wksPort,
     WKS_PORT_ABBREVS));
    next = true;
   }
   bits <<= 1;
   if (oldBits == 0)
    bits++;
  } while (++wksPort > 0);
  return new String(sBuf);
 }

/**
 * NOTE: typeMap must be != null, offset and len must be valid.
 * Result != null, result is 'in-line' (defined in RFC2065).
 */
 public static final String rTypeMapToString(byte[] typeMap,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int rType = typeMap.length;
  if (len > 0)
   rType = typeMap[offset] | typeMap[offset + len - 1];
  byte bits = 0, oldBits;
  StringBuffer sBuf = new StringBuffer(58);
  boolean next = false;
  rType = 0;
  len++;
  do
  {
   if ((oldBits = (byte)(bits << 1)) == 0)
    if (--len > 0)
     bits = typeMap[offset++];
     else break;
   if (bits < 0)
   {
    if (next)
     sBuf.append(' ');
    sBuf.append(rTypeAbbreviation(rType));
    next = true;
   }
   bits <<= 1;
   if (oldBits == 0)
    bits++;
  } while (++rType > 0);
  return new String(sBuf);
 }

/**
 * NOTE: address must be != null, offset and len must be valid.
 * Result != null, result is 'in-line' (defined in RFC1884).
 **
 * @since 3.0
 */
 public static final String addressToString(byte[] address,
         int offset, int len, boolean lowerCase,
         boolean zeroPadding, boolean noCompactFormat,
         boolean noMixedFormat)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int digit = address.length, shift, value, zeros, count;
  digit = 0;
  if (len > 0)
  {
   digit = address[offset] | address[offset + len - 1];
   if ((digit = len << 1) < 0)
    digit = -1 >>> 1;
  }
  StringBuffer sBuf = new StringBuffer(digit);
  if (len > 0)
  {
   shift = count = 0;
   value = (zeros = offset) + len;
   for (digit = (len & 1) + offset;
        digit < value; digit += 2, shift++)
    if ((address[digit + 1] | address[digit]) != 0)
     if (noCompactFormat)
      break;
      else
      {
       if (count < shift)
       {
        count = shift;
        zeros = digit;
       }
       shift = -1;
      }
   if (count < shift)
   {
    count = shift;
    zeros = digit;
   }
   if ((zeros -= count <<= 1) == offset)
   {
    digit = len - count;
    value = offset + count;
    if (!noCompactFormat && count > 2)
    {
     len = digit;
     offset = value;
     sBuf.append(':').append(':');
    }
    if (!noMixedFormat &&
        (digit == INET_ADDR_LEN || digit == INET_ADDR_LEN + 2 &&
        (address[value + 1] & address[value]) == -1))
     len += count = -INET_ADDR_LEN;
   }
   if (len > 0)
   {
    boolean next = zeroPadding;
    do
    {
     if ((value = address[offset++] &
         JavaConsts.BYTE_MASK) != 0 || next)
     {
      shift = (JavaConsts.BYTE_SIZE - 1) & ~3;
      if (!next)
      {
       while (value >> shift == 0)
        shift -= 4;
       next = true;
      }
      do
      {
       if ((digit = (value >>> shift) & ((1 << 4) - 1)) > '9' - '0')
       {
        digit += 'a' - '9' - 1;
        if (!lowerCase)
         digit -= 'a' - 'A';
       }
       sBuf.append((char)(digit + '0'));
      } while ((shift -= 4) >= 0);
     }
     if (--len <= 0)
      break;
     if ((len & 1) == 0)
     {
      if (!next)
      {
       sBuf.append('0');
       next = true;
      }
      sBuf.append(':');
      if (offset == zeros && count > 2)
      {
       sBuf.append(':');
       if ((len -= count) <= 0)
        break;
       offset += count;
      }
      next = zeroPadding;
     }
    } while (true);
    if (!next)
     sBuf.append('0');
    if (count < 0)
     sBuf.append(':');
   }
   if (count < 0)
    do
    {
     digit = value = address[offset++] & JavaConsts.BYTE_MASK;
     shift = 1;
     while ((digit /= '9' - '0' + 1) > 0)
      shift *= '9' - '0' + 1;
     do
     {
      sBuf.append((char)((digit = value / shift) + '0'));
      if (shift <= 1)
       break;
      value -= digit * shift;
      shift /= '9' - '0' + 1;
     } while (true);
     if (++count >= 0)
      break;
     sBuf.append('.');
    } while (true);
  }
  return new String(sBuf);
 }

/**
 * NOTE: str must be != null (RFC1035). Escaping is not allowed in
 * str. Result != null, result length() > 0, result is 'in-line'
 * (escaped).
 */
 public static final String quoteString(String str)
  throws NullPointerException
 {
  int index = 0, len = str.length();
  StringBuffer sBuf = new StringBuffer(len + 2);
  sBuf.append((char)DNSName.QUOTE);
  for (char ch; index < len; sBuf.append(ch))
   if ((ch = str.charAt(index++)) == DNSName.QUOTE ||
       ch == DNSName.ESCAPE)
    sBuf.append((char)DNSName.ESCAPE);
    else if ((char)(ch - ' ') >=
             (JavaConsts.BYTE_MASK >>> 1) - ' ')
    {
     sBuf.append((char)DNSName.ESCAPE).append((char)(ch /
      (('9' - '0' + 1) * ('9' - '0' + 1)) % ('9' - '0' + 1) + '0')).
      append((char)(ch / ('9' - '0' + 1) % ('9' - '0' + 1) + '0'));
     ch = (char)(ch % ('9' - '0' + 1) + '0');
    }
  return new String(sBuf.append((char)DNSName.QUOTE));
 }

/**
 * NOTE: Converts geographic location parameter to a string
 * (RFC1876). Default fields index order is 4, 5, 6, 1, 2, 3
 * (Latitude, Longitude, Altitude, Size, Horiz_Pre, Vert_Pre). Other
 * fields are converted to empty strings. Result != null, result
 * length() > 0 (for valid indices), result is 'in-line'.
 */
 public static final String locToString(int value, int index)
 {
  StringBuffer sBuf = new StringBuffer(15);
  if (index > 0 && index <= 6)
  {
   char sign = 'm';
   if (index > 3)
   {
    if (index == 6)
    {
     if ((value = (index = value) - 10000000) < 0 && index >= 0)
     {
      sBuf.append('-');
      value = -value;
     }
     if ((value -= (index =
         ((value >>> 1) / 100) << 1) * 100) >= 100)
     {
      value -= 100;
      index++;
     }
    }
     else
     {
      sign = 'E';
      if (index == 4)
       sign = 'N';
      if ((value -= 1 << 31) < 0)
      {
       sign = 'W';
       if (index == 4)
        sign = 'S';
       if ((value = -value) <= 0)
        value = 0;
      }
      value -=
       (index = value / (1000 * 60 * 60)) * (1000 * 60 * 60);
     }
    int digit = 1;
    while ((digit *= 10) <= index);
    do
    {
     int num;
     sBuf.append((char)((num = index / (digit /= 10)) + '0'));
     index -= num * digit;
    } while (digit > 1);
    if (sign != 'm')
    {
     digit = (index = value / (1000 * 60)) / 10;
     sBuf.append(' ').append((char)(digit + '0')).
      append((char)(index - digit * 10 + '0'));
     digit = (index = (value -= index * (1000 * 60)) / 1000) / 10;
     sBuf.append(' ').append((char)(digit + '0')).
      append((char)(index - digit * 10 + '0'));
     value -= index * 1000;
    }
    if (value > 0)
    {
     sBuf.append('.');
     if (sign != 'm')
     {
      value -= (index = value / 100) * 100;
      sBuf.append((char)(index + '0'));
     }
     index = value / 10;
     sBuf.append((char)(index + '0')).
      append((char)(value - index * 10 + '0'));
    }
    if (sign != 'm')
     sBuf.append(' ');
   }
    else
    {
     index = (value & 15) - 2;
     if ((value >>>= 4) == 0)
      index = 0;
     if (index < 0)
     {
      sBuf.append('0').append('.');
      if (index < -1)
       sBuf.append('0');
     }
     if (value >= 9)
      value = 9;
     sBuf.append((char)(value + '0'));
     while (index-- > 0)
      sBuf.append('0');
    }
   sBuf.append(sign);
  }
  return new String(sBuf);
 }

/**
 * NOTE: Converts UT seconds to a string (as defined in RFC2535).
 * value is the unsigned number of seconds elapsed since the
 * midnight of the 1st January, 1970 (Gregorian), GMT, ignoring leap
 * seconds. The output date/time format is "yyyyMMddHHmmss". Result
 * != null, result length() > 0, result is 'in-line'.
 **
 * @since 2.3
 */
 public static final String timeToString(int value)
 {
  int digit, day, month, year;
  StringBuffer sBuf = new StringBuffer(14);
  if ((value -= (day = ((value >>> 1) /
      (60 * 60 * 24)) << 1) * (60 * 60 * 24)) >= 60 * 60 * 24)
  {
   value -= 60 * 60 * 24;
   day++;
  }
  day += 1969 / 4 - 1969 / 100 + 1969 / 400 + 1969 * 365;
  day -= (month = day / ((365 * 100 + 25 - 1) * 4 + 1)) *
   ((365 * 100 + 25 - 1) * 4 + 1);
  day -=
   (digit = day / (365 * 100 + 25 - 1)) * (365 * 100 + 25 - 1);
  day -= (year = day / (365 * 4 + 1)) * (365 * 4 + 1);
  month = month * 100 + year;
  day -= (year = day / 365) * 365;
  if (((digit | year) >> 2) != 0)
  {
   day = 365;
   year--;
  }
  year += digit * 100 + (month << 2) + 1;
  if (day >= 31 + 28)
  {
   if ((year & 3) != 0 || year % 100 == 0 && year % 400 != 0)
    day++;
   if (day > 31 + 28)
    day++;
  }
  month = (day * 12 + (365 + 2 + 6)) / (365 + 2);
  day -= (((month >> 3) + month) >> 1) + month * 30 - 31;
  year -= (digit = year / 1000) * 1000;
  sBuf.append((char)(digit % 10 + '0'));
  year -= (digit = year / 100) * 100;
  sBuf.append((char)(digit + '0'));
  year -= (digit = year / 10) * 10;
  sBuf.append((char)(digit + '0')).append((char)(year + '0'));
  digit = month / 10;
  sBuf.append((char)(digit + '0')).
   append((char)(month - digit * 10 + '0'));
  digit = day / 10;
  sBuf.append((char)(digit + '0')).
   append((char)(day - digit * 10 + '0'));
  digit = (day = value / (60 * 60)) / 10;
  sBuf.append((char)(digit + '0')).
   append((char)(day - digit * 10 + '0'));
  digit = (day = (value -= day * (60 * 60)) / 60) / 10;
  sBuf.append((char)(digit + '0')).
   append((char)(day - digit * 10 + '0'));
  digit = (value -= day * 60) / 10;
  sBuf.append((char)(digit + '0')).
   append((char)(value - digit * 10 + '0'));
  return new String(sBuf);
 }

/**
 * NOTE: rDataValue must be != null. domain may be == null. Result
 * != null, result is 'in-line', result is as defined in RFC1035.
 */
 public static final String rDataToString(int rType,
         Object rDataValue, int index, DNSName domain)
  throws NullPointerException
 {
  if (rDataValue instanceof Number)
  {
   int intValue = ((Number)rDataValue).intValue();
   switch (rType)
   {
    case WKS:
     if (index != 1 || (intValue & ~JavaConsts.BYTE_MASK) != 0)
      break;
     return UnsignedInt.toAbbreviation(intValue,
      WKS_PROTOCOL_ABBREVS);
    case SIG:
     if (index >> 1 == 2)
      return timeToString(intValue);
     if (index != 0 || (intValue & ~RTYPE_MASK) != 0)
      break;
     return rTypeAbbreviation(intValue);
    case LOC:
     if (((6 - index) | index) < 0 ||
         index <= 3 && (intValue & ~JavaConsts.BYTE_MASK) != 0)
      break;
     return locToString(intValue, index);
    case TKEY:
     if ((index - 1) >> 1 == 0)
      return timeToString(intValue);
     if (index != 4)
      break;
     return UnsignedInt.toAbbreviation(intValue,
      DNSMsgHeader.RCODE_ABBREVS);
    case TSIG:
     if (index == 1)
      return timeToString(intValue);
     if (index != 5)
      break;
     return UnsignedInt.toAbbreviation(intValue,
      DNSMsgHeader.RCODE_ABBREVS);
   }
  }
   else if (rDataValue instanceof InetAddress)
    return ((InetAddress)rDataValue).getHostAddress();
    else if (rDataValue instanceof DNSName)
     return ((DNSName)rDataValue).getRelative(domain);
     else if (rDataValue instanceof String)
      return quoteString((String)rDataValue);
      else if (rDataValue instanceof ByteVector)
      {
       byte[] bytes;
       int len = (bytes = ((ByteVector)rDataValue).array()).length;
       switch (rType)
       {
        case WKS:
         return wksPortMapToString(bytes, 0, len);
        case NXT:
         return rTypeMapToString(bytes, 0, len);
        case NSAP:
         return ByteVector.toHexString(bytes, 0, len,
          false, '.', 1, true);
        case SIG:
        case KEY:
        case CERT:
        case TKEY:
        case TSIG:
         return ByteVector.toBase64String(bytes, 0, len);
        case AAAA:
         return addressToString(bytes, 0, len,
          false, false, false, false);
       }
       return ByteVector.toHexString(bytes, 0, len,
        true, ' ', 0, true);
      }
  return rDataValue.toString();
 }

/**
 * NOTE: Result is the index for rData array corresponding to
 * textualIndex. Negative textualIndex is treated as zero. In fact,
 * textualIndex should be iterated from 0 to maxRDataLen(rType) - 1,
 * inclusive. Result >= 0, result may be >= rData length.
 **
 * @since 3.0
 */
 public static final int rDataIndex(int rType, int textualIndex)
 {
  if (textualIndex <= 0)
   textualIndex = 0;
  if (rType == LOC && textualIndex != -1 >>> 1 &&
      ++textualIndex <= 6 && (textualIndex -= 3) <= 0)
   textualIndex += 6;
  return textualIndex;
 }

/**
 * NOTE: records must be != null, records[index] != null for any
 * index. Negative len is treated as zero. If rType != ANY then only
 * records with specified rType are accepted. If field is missing or
 * field value is equal to the previous one then it is ignored. Only
 * values of the specified field of rData of records are included
 * into resulting array. Result is exact instanceof Object[],
 * result[index] != null for any index, max(len, 0) >= result
 * length.
 **
 * @since 2.1
 */
 public static final Object[] getFieldsAt(int field, int rType,
         DNSRecord[] records, int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  Object[] values = new Object[len > 0 ? len : 0], rData;
  int index = 0;
  Object prevValue = null;
  while (len-- > 0)
  {
   DNSRecord record = records[offset++];
   if ((rType == ANY || (record.rType & RTYPE_MASK) == rType) &&
       (rData = decodeRData(rType, record.rDataBytes)).length >
       field && !rData[field].equals(prevValue))
    values[index++] = prevValue = rData[field];
  }
  if (values.length > index)
  {
   Object[] newValues;
   System.arraycopy(values, 0,
    newValues = new Object[index], 0, index);
   values = newValues;
  }
  return values;
 }

/**
 * NOTE: qdRecord may be == null. ANY qClass and IXFR, AXFR, MAILB,
 * MAILA and ANY qTypes of qdRecord are treated specially (like
 * wildcards).
 **
 * @since 2.0
 */
 public boolean equalsQuery(DNSRecord qdRecord)
 {
  if (this == qdRecord)
   return true;
  int qType, rType;
  if (qdRecord != null)
   if ((qType = qdRecord.rClass & RCLASS_MASK) == ANY ||
       qType == NONE || (this.rClass & RCLASS_MASK) == qType)
    if ((qType = qdRecord.rType & RTYPE_MASK) == IXFR ||
        qType == AXFR)
     return this.rName.isInDomain(qdRecord.rName, false);
     else if (qType == ANY || (rType = this.rType & RTYPE_MASK) ==
              qType || qType == MAILA && (rType == MX ||
              rType == MD || rType == MF) || qType == MAILB &&
              (rType == MB || rType == MG || rType == MR ||
              rType == MINFO))
      return this.rName.equals(qdRecord.rName);
  return false;
 }

/**
 * NOTE: record may be == null.
 **
 * @since 2.3
 */
 public boolean equalsExact(DNSRecord record)
 {
  if (this != record)
  {
   if (record == null || this.rType != record.rType ||
       this.rClass != record.rClass || this.ttl != record.ttl ||
       !this.rName.equalsExact(record.rName))
    return false;
   int offset;
   byte[] rDataBytes = this.rDataBytes, recordRDataBytes;
   if ((recordRDataBytes = record.rDataBytes) != rDataBytes)
    if ((offset = rDataBytes.length) != recordRDataBytes.length)
     return false;
     else while (offset-- > 0)
      if (rDataBytes[offset] != recordRDataBytes[offset])
       return false;
  }
  return true;
 }

 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof DNSRecord && obj != this)
    return obj;
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * NOTE: ttl is omitted (as if ttl == 0).
 */
 public int hashCode()
 {
  int rType = this.rType & RTYPE_MASK;
  return (((((((this.rName.hashCode() * 31) ^ rType) * 31) ^
   (this.rClass & RCLASS_MASK)) * (31 * 31)) ^
   hashCodeRData(rType, this.rDataBytes)) * 31) ^ 5;
 }

/**
 * NOTE: ttl is ignored. rData contents are compared in the
 * case-insensitive manner for DNSName fields. All unrecognized
 * fields are ignored too.
 */
 public boolean equals(Object obj)
 {
  return obj == this || obj instanceof DNSRecord &&
   compareTo((DNSRecord)obj, false) == 0;
 }

/**
 * NOTE: Method for canonical ordering (according to RFC2535).
 **
 * @since 2.3
 */
 public boolean greaterThan(Object obj)
 {
  return obj != this && obj instanceof DNSRecord &&
   compareTo((DNSRecord)obj, false) > 0;
 }

/**
 * NOTE: record must be != null. rClass is compared first, then
 * rName is compared, then rType is compared (SOA is handled
 * specially to be less than other types, SIG(rType) for any rType
 * is handled specially to be greater than rType and less than rType
 * + 1), and then rData contents are compared as byte arrays (with
 * upper-case letters in DNSName fields set to lower-case). ttl is
 * ignored. If decodeRData then rData contents are compared in the
 * field-by-field manner, each recognized field is compared
 * according to its type (in addition, strings are
 * case-insensitive). Records canonical order (decodeRData == false)
 * is defined in RFC2535 (except for SOA handling but it is
 * important since SOA record must be the first record in a zone).
 */
 public int compareTo(DNSRecord record, boolean decodeRData)
  throws NullPointerException
 {
  int diff = 0;
  if (this != record && (diff = (this.rClass & RCLASS_MASK) -
      (record.rClass & RCLASS_MASK)) == 0 &&
      (diff = this.rName.compareTo(record.rName)) == 0)
  {
   int rType = this.rType & RTYPE_MASK, cover;
   byte[] rDataBytes = this.rDataBytes;
   byte[] recordRDataBytes = record.rDataBytes;
   diff = (diff = record.rType & RTYPE_MASK) == rType ?
    compareRData(diff, rDataBytes, recordRDataBytes, decodeRData) :
    rType == SOA ? -1 : diff == SOA ? 1 : rType == SIG &&
    rDataBytes.length >= RTYPE_LENGTH ? ((cover =
    UnsignedInt.getFromByteArray(rDataBytes, 0, RTYPE_LENGTH)) ==
    SOA || cover < diff ? -1 : 1) : diff == SIG &&
    recordRDataBytes.length >= RTYPE_LENGTH ? ((cover =
    UnsignedInt.getFromByteArray(recordRDataBytes, 0,
    RTYPE_LENGTH)) == SOA || cover < rType ? 1 : -1) : rType - diff;
  }
  return diff;
 }

/**
 * NOTE: If domain == null then current zone domain is not
 * specified. If prevRecord == null then default rName, rClass and
 * ttl values are not specified. Else all defaults are omitted in
 * the result. The textual output format is
 * "[relativeRName] [rClassAbbr] [ttl] rTypeAbbr [rData]" (as
 * specified in RFC1035). If tabSeparated then all fields are
 * separated with a single tab character else these fields are
 * separated (and padded to be mostly aligned) with blanks. All
 * rData elements are separated with a single blank. Result != null,
 * result length() > 0, result is 'in-line'.
 */
 public String toString(DNSName domain, DNSRecord prevRecord,
         boolean tabSeparated)
 {
  StringBuffer sBuf = new StringBuffer(50);
  int rType = this.rType & RTYPE_MASK, len, value, pos;
  boolean padding = true;
  Object[] rData;
  String str;
  if ((pos = len =
      (rData = decodeRData(rType, this.rDataBytes)).length) > 0)
  {
   value = 50;
   do
   {
    pos--;
    value -=
     (str = rDataToString(rType, rData[pos], pos, domain)).length();
    rData[pos] = str;
   } while (pos > 0);
   if (value < len)
    padding = false;
  }
  str = null;
  value = this.rClass;
  if (prevRecord == null || prevRecord.rClass != value)
   str = rClassAbbreviation(value & RCLASS_MASK);
  boolean ttlShown = false;
  value = this.ttl;
  if (prevRecord == null || prevRecord.ttl != value)
   ttlShown = true;
  DNSName rName = this.rName;
  if (ttlShown || str != null || !rName.equals(prevRecord.rName))
  {
   String nameStr;
   sBuf.append(nameStr = rName.getRelative(domain));
   if (!ttlShown && (char)(nameStr.charAt(0) - '0') <= '9' - '0')
    ttlShown = true;
  }
  if (tabSeparated)
   sBuf.append('\t');
  if (str != null)
  {
   if (!tabSeparated)
   {
    if (padding)
     pos = 11 - sBuf.length();
    do
    {
     sBuf.append(' ');
    } while (--pos > 0);
   }
   sBuf.append(str);
  }
  if (tabSeparated)
   sBuf.append('\t');
  if (ttlShown)
  {
   str = UnsignedInt.toString(value & TTL_MASK, true);
   if (!tabSeparated)
   {
    if (padding)
     pos = 19 - sBuf.length() - str.length();
    do
    {
     sBuf.append(' ');
    } while (--pos > 0);
   }
   sBuf.append(str);
  }
  if (!tabSeparated)
  {
   if (padding)
    pos = 20 - sBuf.length();
   do
   {
    sBuf.append(' ');
   } while (--pos > 0);
  }
   else sBuf.append('\t');
  sBuf.append(rTypeAbbreviation(rType));
  if (len > 0)
  {
   if (!tabSeparated)
   {
    if (padding)
     pos = 26 - sBuf.length();
    do
    {
     sBuf.append(' ');
    } while (--pos > 0);
    pos = 0;
   }
    else sBuf.append('\t');
   int max = maxRDataLen(rType);
   padding = false;
   do
   {
    if ((value = rDataIndex(rType, pos)) < len)
    {
     if (padding)
      sBuf.append(' ');
     sBuf.append((String)rData[value]);
     padding = true;
    }
   } while (++pos < max);
  }
  return new String(sBuf);
 }

/**
 * NOTE: No default values. Result length > 0, result is 'in-line',
 * result format is specified in RFC1035 (fields are separated with
 * blanks).
 */
 public String toString()
 {
  return toString(null, null, false);
 }

/**
 * NOTE: Check record and its name objects for their integrity. For
 * debug purpose only.
 **
 * @since 2.3
 */
 public void integrityCheck()
 {
  DNSName rName;
  byte[] rDataBytes;
  if ((rName = this.rName) == null)
   throw new InternalError("rName: null");
  if ((rDataBytes = this.rDataBytes) == null)
   throw new InternalError("rDataBytes: null");
  if ((rDataBytes.length & ~RDATA_LEN_MASK) != 0)
   throw new InternalError("rDataBytes length: " +
              UnsignedInt.toString(rDataBytes.length, false));
  rName.integrityCheck();
 }

 private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
 {
  in.defaultReadObject();
  DNSName rName;
  byte[] rDataBytes;
  if ((rName = this.rName) == null)
   throw new InvalidObjectException("rName: null");
  if ((rDataBytes = this.rDataBytes) == null)
   throw new InvalidObjectException("rDataBytes: null");
  if ((rDataBytes.length & ~RDATA_LEN_MASK) != 0)
   throw new InvalidObjectException("rDataBytes length: " +
              UnsignedInt.toString(rDataBytes.length, false));
 }
}

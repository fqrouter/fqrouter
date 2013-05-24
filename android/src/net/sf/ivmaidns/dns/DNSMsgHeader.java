/*
 * @(#) src/net/sf/ivmaidns/dns/DNSMsgHeader.java --
 * Class for representing DNS msg header.
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

import java.io.Serializable;

import net.sf.ivmaidns.util.Immutable;
import net.sf.ivmaidns.util.Indexable;
import net.sf.ivmaidns.util.JavaConsts;
import net.sf.ivmaidns.util.ReallyCloneable;
import net.sf.ivmaidns.util.UnsignedInt;

/**
 * Class for representing DNS msg header (as defined in RFC1035).
 **
 * @version 3.0
 * @author Ivan Maidanski
 */
public final class DNSMsgHeader
 implements Immutable, ReallyCloneable, Serializable, Indexable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 2.1
 */
 private static final long serialVersionUID = 3319548900112653688L;

/**
 * NOTE: This opCode value (defined in RFC1035) indicates a
 * standard/normal query ('QUERY').
 */
 public static final int QUERY = 0;

/**
 * NOTE: This opCode value (defined in RFC1035) indicates an inverse
 * query ('IQUERY'). It is used mainly during a DNS server debugging
 * process.
 */
 public static final int IQUERY = 1;

/**
 * NOTE: This opCode value (defined in RFC1035) indicates a server
 * status request ('STATUS').
 */
 public static final int STATUS = 2;

/**
 * NOTE: This opCode value (defined in RFC1996) indicates a slave
 * server notification on zone changings ('NOTIFY').
 */
 public static final int NOTIFY = 4;

/**
 * NOTE: This opCode value (defined in RFC2136) indicates an
 * 'UPDATE' DNS message.
 */
 public static final int UPDATE = 5;

/**
 * NOTE: This rCode value (defined in RFC1035) indicates no error
 * condition ('NOERROR').
 */
 public static final int NOERROR = 0;

/**
 * NOTE: This rCode value (defined in RFC1035) indicates that the
 * name server was unable to interpret the request due to a format
 * error ('FORMERR').
 */
 public static final int FORMERR = 1;

/**
 * NOTE: This rCode value (defined in RFC1035) indicates that the
 * name server encountered an internal failure while processing this
 * request ('SERVFAIL'). This means an operating system error or
 * forwarding timeout.
 */
 public static final int SERVFAIL = 2;

/**
 * NOTE: This rCode value (defined in RFC1035) indicates that the
 * name referenced in the query ought to exist but does not exist
 * ('NXDOMAIN'). This is meaningful only for responses from an
 * authoritative name server.
 */
 public static final int NXDOMAIN = 3;

/**
 * NOTE: This rCode value (defined in RFC1035) indicates that the
 * name server does not support the requested kind of query
 * ('NOTIMP').
 */
 public static final int NOTIMP = 4;

/**
 * NOTE: This rCode value (defined in RFC1035) indicates that the
 * name server refuses to perform the specified operation for policy
 * or security reasons ('REFUSED').
 */
 public static final int REFUSED = 5;

/**
 * NOTE: This rCode value (defined in RFC2136) indicates that the
 * referenced name ought not to exist but does exist ('YXDOMAIN').
 */
 public static final int YXDOMAIN = 6;

/**
 * NOTE: This rCode value (defined in RFC2136) indicates that the
 * referenced resource records set ought not to exist but does exist
 * ('YXRRSET').
 */
 public static final int YXRRSET = 7;

/**
 * NOTE: This rCode value (defined in RFC2136) indicates that the
 * referenced resource records set ought to exist but does not exist
 * ('NXRRSET').
 */
 public static final int NXRRSET = 8;

/**
 * NOTE: This rCode value (defined in RFC2136) indicates that the
 * name server is not authoritative for the requested zone
 * ('NOTAUTH').
 */
 public static final int NOTAUTH = 9;

/**
 * NOTE: This rCode value (defined in RFC2136) indicates that the
 * specified name is not within the denoted zone ('NOTZONE').
 */
 public static final int NOTZONE = 10;

/**
 * NOTE: This extended rCode value (defined in RFC2845) indicates
 * that the digital signature referenced in the TSIG resource record
 * is invalid ('BADSIG'); this value also indicates an invalid OPT
 * record version error (as defined in RFC2671). Extended rCode
 * values are used in resource records (such as OPT, TSIG, TKEY)
 * only, not in a message header.
 **
 * @since 2.2
 */
 public static final int BADSIG = 16;

/**
 * NOTE: This extended rCode value (defined in RFC2845) indicates
 * that the key referenced in the TSIG resource record is invalid
 * ('BADKEY'). Extended rCode values are used in resource records
 * (such as OPT, TSIG, TKEY) only, not in a message header.
 **
 * @since 2.2
 */
 public static final int BADKEY = 17;

/**
 * NOTE: This extended rCode value (defined in RFC2845) indicates
 * that the time stamp referenced in the TSIG or TKEY resource
 * record is invalid ('BADTIME'). Extended rCode values are used in
 * resource records (such as OPT, TSIG, TKEY) only, not in a message
 * header.
 **
 * @since 2.2
 */
 public static final int BADTIME = 18;

/**
 * NOTE: This extended rCode value (defined in RFC2930) indicates
 * that the key exhange mode referenced in the TKEY resource record
 * is invalid ('BADMODE'). Extended rCode values are used in
 * resource records (such as OPT, TSIG, TKEY) only, not in a message
 * header.
 **
 * @since 2.2
 */
 public static final int BADMODE = 19;

/**
 * NOTE: This extended rCode value (defined in RFC2930) indicates
 * that the key name referenced in the TKEY resource record is
 * invalid ('BADNAME'). Extended rCode values are used in resource
 * records (such as OPT, TSIG, TKEY) only, not in a message header.
 **
 * @since 2.2
 */
 public static final int BADNAME = 20;

/**
 * NOTE: This extended rCode value (defined in RFC2930) indicates
 * that the key exhange algorithm referenced in the TKEY resource
 * record is invalid ('BADALG'). Extended rCode values are used in
 * resource records (such as OPT, TSIG, TKEY) only, not in a message
 * header.
 **
 * @since 2.2
 */
 public static final int BADALG = 21;

/**
 * NOTE: This is a bit mask for 'QR' DNS message flag (defined in
 * RFC1035). If this flag (query response) is set then the message
 * is a response, else it is a query.
 */
 public static final int QR = 0x8000;

/**
 * NOTE: This is a bit mask for 'AA' DNS message flag (defined in
 * RFC1035). This flag (authoritative answer) is valid in responses
 * and specifies that the responding name server is an authority for
 * the domain name in question section (the flag corresponds only to
 * the name which matches the query name, or the first owner name in
 * the answer section).
 */
 public static final int AA = 0x400;

/**
 * NOTE: This is a bit mask for 'TC' DNS message flag (defined in
 * RFC1035). This set flag (truncation) specifies that the message
 * was truncated due to length greater than that permitted on the
 * transmission channel.
 */
 public static final int TC = 0x200;

/**
 * NOTE: This is a bit mask for 'RD' DNS message flag (defined in
 * RFC1035). This flag (recursion desired) may be set in a query and
 * is copied into the response, if it is set then it directs the
 * name server (if available) to pursue the query recursively.
 */
 public static final int RD = 0x100;

/**
 * NOTE: This is a bit mask for 'RA' DNS message flag (defined in
 * RFC1035). This flag (recursion available) is set or cleared in a
 * response, and denotes whether recursive query support is
 * available in the name server.
 */
 public static final int RA = 0x80;

/**
 * NOTE: This is a bit mask for 'AD' DNS message flag (defined in
 * RFC2535). This flag (authentic data) indicates in a response that
 * all the data included in the answer and authority portion of the
 * response has been authenticated by the server according to the
 * policies of that server.
 **
 * @since 2.2
 */
 public static final int AD = 0x20;

/**
 * NOTE: This is a bit mask for 'CD' DNS message flag (defined in
 * RFC2535). This flag (checking disabled) may be set in a query and
 * is copied into the response, if it is set then it indicates that
 * pending (non-authenticated) data is acceptable to the resolver
 * sending the query.
 **
 * @since 2.2
 */
 public static final int CD = 0x10;

/**
 * NOTE: These are fields lengthes (in bytes).
 **
 * @since 2.2
 */
 public static final int ID_LENGTH = 2;

 public static final int FLAGS_LENGTH = 2;

 public static final int COUNT_LENGTH = 2;

/**
 * NOTE: This is total length (in bytes) of header.
 **
 * @since 2.2
 */
 public static final int HEADER_LEN =
  ID_LENGTH + FLAGS_LENGTH + COUNT_LENGTH * 4;

/**
 * NOTE: This is the default/maximum DNS UDP packet length
 * (PACKET_LEN > HEADER_LEN).
 **
 * @since 2.2
 */
 public static final int UDP_PACKET_LEN = 512;

/**
 * NOTE: These are fields bit masks.
 */
 public static final int MAX_ID =
  ID_LENGTH < JavaConsts.INT_LENGTH ?
  ~(-1 << (ID_LENGTH * JavaConsts.BYTE_SIZE)) : -1;

 public static final int MAX_FLAGS =
  FLAGS_LENGTH < JavaConsts.INT_LENGTH ?
  ~(-1 << (FLAGS_LENGTH * JavaConsts.BYTE_SIZE)) : -1;

 public static final int MAX_COUNT =
  COUNT_LENGTH < JavaConsts.INT_LENGTH ?
  ~(-1 << (COUNT_LENGTH * JavaConsts.BYTE_SIZE)) : -1;

 public static final int MAX_OPCODE = 0xF;

 public static final int MAX_RCODE = 0xF;

 public static final int OPCODE_SHIFT = 11;

/**
 * NOTE: This is the opCode abbreviations list string.
 **
 * @since 3.0
 */
 public static final String OPCODE_ABBREVS =
  ",OPCODE,," +
  "QUERY,,,," +
  "IQUERY,,," +
  "STATUS,,," +
  ",,,,,,,,," +
  "NOTIFY,,," +
  "UPDATE,,,";

/**
 * NOTE: This is the rCode abbreviations list string.
 **
 * @since 3.0
 */
 public static final String RCODE_ABBREVS =
  ",RCODE,,," +
  "NOERROR,," +
  "FORMERR,," +
  "SERVFAIL," +
  "NXDOMAIN," +
  "NOTIMP,,," +
  "REFUSED,," +
  "YXDOMAIN," +
  "YXRRSET,," +
  "NXRRSET,," +
  "NOTAUTH,," +
  "NOTZONE,," +
  ",,,,,,,,," +
  ",,,,,,,,," +
  ",,,,,,,,," +
  ",,,,,,,,," +
  ",,,,,,,,," +
  "BADSIG,,," +
  "BADKEY,,," +
  "BADTIME,," +
  "BADMODE,," +
  "BADNAME,," +
  "BADALG,,,";

/**
 * NOTE: An unsigned identifier assigned by the client that
 * generates a query message. This identifier is copied into the
 * corresponding reply and can be used by the requester to match up
 * replies to outstanding queries.
 **
 * @serial
 */
 protected final short id;

/**
 * NOTE: flags field consists of QR, opCode, AA, TC, RD, RA, a
 * reserved flag (should be zero), AD, CD and rCode. opCode field
 * (which is set by the originator of a query and copied into the
 * response) specifies the kind of query. rCode field (response
 * code) is set as part of responses.
 **
 * @serial
 */
 protected final short flags;

/**
 * NOTE: An unsigned short integer specifying the number of entries
 * in the question section.
 **
 * @serial
 */
 protected final short qdCount;

/**
 * NOTE: An unsigned short integer specifying the number of entries
 * in the answer section.
 **
 * @serial
 */
 protected final short anCount;

/**
 * NOTE: An unsigned short integer specifying the number of entries
 * in the authority section.
 **
 * @serial
 */
 protected final short nsCount;

/**
 * NOTE: An unsigned short integer specifying the number of entries
 * in the additional section.
 **
 * @serial
 */
 protected final short arCount;

/**
 * NOTE: DNS status query header constructor.
 */
 public DNSMsgHeader()
 {
  this.id = 1;
  this.flags = (short)((STATUS << OPCODE_SHIFT) | RD);
  this.qdCount = this.anCount = this.nsCount = this.arCount = 0;
 }

/**
 * NOTE: Primary DNS message header constructor. id, flags, qdCount,
 * anCount, nsCount and arCount are unsigned (must be valid).
 */
 public DNSMsgHeader(int id, int flags, int qdCount, int anCount,
         int nsCount, int arCount)
  throws IllegalArgumentException
 {
  if ((id & ~MAX_ID) != 0)
   throw new IllegalArgumentException("id: 0x" +
              UnsignedInt.toHexString(id, true, 0));
  if ((flags & ~MAX_FLAGS) != 0)
   throw new IllegalArgumentException("flags: 0x" +
              UnsignedInt.toHexString(flags, true, 0));
  if ((qdCount & ~MAX_COUNT) != 0)
   throw new IllegalArgumentException("qdCount: " +
              UnsignedInt.toString(qdCount, true));
  if ((anCount & ~MAX_COUNT) != 0)
   throw new IllegalArgumentException("anCount: " +
              UnsignedInt.toString(anCount, true));
  if ((nsCount & ~MAX_COUNT) != 0)
   throw new IllegalArgumentException("nsCount: " +
              UnsignedInt.toString(nsCount, true));
  if ((arCount & ~MAX_COUNT) != 0)
   throw new IllegalArgumentException("arCount: " +
              UnsignedInt.toString(arCount, true));
  this.id = (short)id;
  this.flags = (short)flags;
  this.qdCount = (short)qdCount;
  this.anCount = (short)anCount;
  this.nsCount = (short)nsCount;
  this.arCount = (short)arCount;
 }

/**
 * NOTE: msgBytes must be != null and msgBytes length >= HEADER_LEN.
 * Constructor for creating header from message bytes. msgBytes
 * array is not changed anyway.
 **
 * @since 2.2
 */
 public DNSMsgHeader(byte[] msgBytes)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  this.arCount = (short)UnsignedInt.getFromByteArray(msgBytes,
   ID_LENGTH + FLAGS_LENGTH + COUNT_LENGTH * 3, COUNT_LENGTH);
  this.nsCount = (short)UnsignedInt.getFromByteArray(msgBytes,
   ID_LENGTH + FLAGS_LENGTH + COUNT_LENGTH * 2, COUNT_LENGTH);
  this.anCount = (short)UnsignedInt.getFromByteArray(msgBytes,
   ID_LENGTH + FLAGS_LENGTH + COUNT_LENGTH, COUNT_LENGTH);
  this.qdCount = (short)UnsignedInt.getFromByteArray(msgBytes,
   ID_LENGTH + FLAGS_LENGTH, COUNT_LENGTH);
  this.flags = (short)UnsignedInt.getFromByteArray(msgBytes,
   ID_LENGTH, FLAGS_LENGTH);
  this.id = (short)UnsignedInt.getFromByteArray(msgBytes,
   0, ID_LENGTH);
 }

/**
 * NOTE: msgBytes must be != null and msgBytes length >= HEADER_LEN.
 * Method for putting header to message bytes. msgBytes array is
 * altered (unless an exception is thrown).
 **
 * @since 2.2
 */
 public void putTo(byte[] msgBytes)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  UnsignedInt.putToByteArray(msgBytes, ID_LENGTH + FLAGS_LENGTH +
   COUNT_LENGTH * 3, this.arCount, COUNT_LENGTH);
  UnsignedInt.putToByteArray(msgBytes, ID_LENGTH + FLAGS_LENGTH +
   COUNT_LENGTH * 2, this.nsCount, COUNT_LENGTH);
  UnsignedInt.putToByteArray(msgBytes, ID_LENGTH + FLAGS_LENGTH +
   COUNT_LENGTH, this.anCount, COUNT_LENGTH);
  UnsignedInt.putToByteArray(msgBytes, ID_LENGTH + FLAGS_LENGTH,
   this.qdCount, COUNT_LENGTH);
  UnsignedInt.putToByteArray(msgBytes, ID_LENGTH, this.flags,
   FLAGS_LENGTH);
  UnsignedInt.putToByteArray(msgBytes, 0, this.id, ID_LENGTH);
 }

/**
 * NOTE: msgBytes must be != null and msgBytes length >= HEADER_LEN.
 * msgBytes array is altered (unless an exception is thrown).
 **
 * @since 3.0
 */
 public static final void setTruncated(byte[] msgBytes)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  byte value;
  value = msgBytes[HEADER_LEN - 1];
  msgBytes[ID_LENGTH] |= (byte)(TC >>> JavaConsts.BYTE_SIZE);
 }

/**
 * NOTE: msgBytes must be != null and msgBytes length >= HEADER_LEN.
 * msgBytes array is not changed anyway. The result is the same as
 * of (new DNSMsgHeader(msgBytes)) isTruncated().
 **
 * @since 3.0
 */
 public static final boolean isTruncated(byte[] msgBytes)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  byte value;
  value = msgBytes[HEADER_LEN - 1];
  return (msgBytes[ID_LENGTH] & (TC >>> JavaConsts.BYTE_SIZE)) != 0;
 }

/**
 * NOTE: msgBytes must be != null and msgBytes length >= HEADER_LEN.
 * msgBytes array is not changed anyway. The result is the same as
 * of (new DNSMsgHeader(msgBytes)) getQdCount(). Result is unsigned.
 **
 * @since 3.0
 */
 public static final int getQdCount(byte[] msgBytes)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  byte value;
  value = msgBytes[HEADER_LEN - 1];
  return UnsignedInt.getFromByteArray(msgBytes,
   ID_LENGTH + FLAGS_LENGTH, COUNT_LENGTH);
 }

/**
 * NOTE: msgBytes must be != null and msgBytes length >= HEADER_LEN.
 * msgBytes array is not changed anyway. The result is the sum of
 * qdCount, anCount, nsCount and arCount of
 * (new DNSMsgHeader(msgBytes)). Result >= 0.
 **
 * @since 3.0
 */
 public static final int getTotalCount(byte[] msgBytes)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int qdCount, anCount, nsCount, arCount;
  if ((arCount = UnsignedInt.getFromByteArray(msgBytes, ID_LENGTH +
      FLAGS_LENGTH + COUNT_LENGTH * 3, COUNT_LENGTH)) < 0 ||
      (nsCount = UnsignedInt.getFromByteArray(msgBytes, ID_LENGTH +
      FLAGS_LENGTH + COUNT_LENGTH * 2, COUNT_LENGTH)) < 0 ||
      (anCount = UnsignedInt.getFromByteArray(msgBytes, ID_LENGTH +
      FLAGS_LENGTH + COUNT_LENGTH, COUNT_LENGTH)) < 0 ||
      (qdCount = UnsignedInt.getFromByteArray(msgBytes, ID_LENGTH +
      FLAGS_LENGTH, COUNT_LENGTH)) < 0 ||
      (qdCount += anCount) < 0 || (nsCount += arCount) < 0 ||
      (qdCount += nsCount) < 0)
   qdCount = -1 >>> 1;
  return qdCount;
 }

/**
 * NOTE: Any query message header constructor. opCode must be valid.
 * qdCount, anCount, nsCount and arCount are unsigned (must be
 * valid). Message id is random. Result != null.
 */
 public static DNSMsgHeader construct(int opCode,
         boolean isRecursionDesired, int qdCount, int anCount,
         int nsCount, int arCount, boolean isCheckingDisabled)
  throws IllegalArgumentException
 {
  if ((opCode & ~MAX_OPCODE) != 0)
   throw new IllegalArgumentException("opCode: " +
              UnsignedInt.toString(opCode, true));
  opCode <<= OPCODE_SHIFT;
  if (isRecursionDesired)
   opCode |= RD;
  if (isCheckingDisabled)
   opCode |= CD;
  return new DNSMsgHeader(((int)System.currentTimeMillis() *
   JavaConsts.GOLD_MEDIAN) >>>
   (JavaConsts.INT_SIZE - ID_LENGTH * JavaConsts.BYTE_SIZE),
   opCode, qdCount, anCount, nsCount, arCount);
 }

/**
 * NOTE: Server response message header constructor. rCode must be
 * valid. qdCount, anCount, nsCount and arCount are unsigned (must
 * be valid). Result != null, result != this.
 **
 * @since 2.2
 */
 public DNSMsgHeader constructResponse(int rCode,
         boolean isAuthoritativeAnswer, boolean isTruncated,
         boolean isRecursionAvailable, boolean isAuthenticData,
         int qdCount, int anCount, int nsCount, int arCount)
  throws IllegalArgumentException
 {
  if ((rCode & ~MAX_RCODE) != 0)
   throw new IllegalArgumentException("rCode: " +
              UnsignedInt.toString(rCode, true));
  if (isAuthoritativeAnswer)
   rCode |= AA;
  if (isTruncated)
   rCode |= TC;
  if (isRecursionAvailable)
   rCode |= RA;
  if (isAuthenticData)
   rCode |= AD;
  return new DNSMsgHeader(this.id & MAX_ID, this.flags &
   ((MAX_OPCODE << OPCODE_SHIFT) | RD | CD) | rCode | QR,
   qdCount, anCount, nsCount, arCount);
 }

/**
 * NOTE: May be 0 in AXFR response messages (starting from the
 * second message). Result is unsigned.
 */
 public final int getId()
 {
  return this.id & MAX_ID;
 }

/**
 * NOTE: Result is unsigned.
 */
 public final int getFlags()
 {
  return this.flags & MAX_FLAGS;
 }

 public final boolean isResponse()
 {
  return (this.flags & QR) != 0;
 }

/**
 * NOTE: Result >= 0.
 */
 public final int getOpCode()
 {
  return (this.flags >> OPCODE_SHIFT) & MAX_OPCODE;
 }

 public final boolean isAuthoritativeAnswer()
 {
  return (this.flags & AA) != 0;
 }

 public final boolean isTruncated()
 {
  return (this.flags & TC) != 0;
 }

 public final boolean isRecursionDesired()
 {
  return (this.flags & RD) != 0;
 }

 public final boolean isRecursionAvailable()
 {
  return (this.flags & RA) != 0;
 }

/**
 * NOTE: Result is the same as of ((getFlags() & AD) != 0).
 **
 * @since 2.2
 */
 public final boolean isAuthenticData()
 {
  return (this.flags & AD) != 0;
 }

/**
 * NOTE: Result is the same as of ((getFlags() & CD) != 0).
 **
 * @since 2.2
 */
 public final boolean isCheckingDisabled()
 {
  return (this.flags & CD) != 0;
 }

/**
 * NOTE: Result >= 0.
 */
 public final int getRCode()
 {
  return this.flags & MAX_RCODE;
 }

/**
 * NOTE: Result is unsigned.
 */
 public final int getQdCount()
 {
  return this.qdCount & MAX_COUNT;
 }

/**
 * NOTE: Result is unsigned.
 */
 public final int getAnCount()
 {
  return this.anCount & MAX_COUNT;
 }

/**
 * NOTE: Result is unsigned.
 */
 public final int getNsCount()
 {
  return this.nsCount & MAX_COUNT;
 }

/**
 * NOTE: Result is unsigned.
 */
 public final int getArCount()
 {
  return this.arCount & MAX_COUNT;
 }

/**
 * NOTE: Result is the number of elements accessible through
 * getAt(int).
 **
 * @since 2.1
 */
 public int length()
 {
  return 6;
 }

/**
 * NOTE: Result is new UnsignedInt((new int[] { getId(), getFlags(),
 * getQdCount(), getAnCount(), getNsCount(), getArCount()
 * })[index]).
 **
 * @since 2.1
 */
 public Object getAt(int index)
  throws ArrayIndexOutOfBoundsException
 {
  if (((5 - index) | index) >= 0)
   return new UnsignedInt(index < 2 ?
    (index == 0 ? this.id & MAX_ID : this.flags & MAX_FLAGS) :
    (index < 4 ? (index == 2 ? this.qdCount : this.anCount) :
    (index == 4 ? this.nsCount : this.arCount)) & MAX_COUNT);
  throw new ArrayIndexOutOfBoundsException(index);
 }

/**
 * NOTE: flags value is unsigned. Only known bit flags are
 * represented in the result. Result != null, result length() > 0,
 * result is 'in-line'.
 */
 public static final String flagBitsAbbreviation(int flags)
 {
  StringBuffer sBuf = new StringBuffer(13);
  char ch = 'Q';
  if ((flags & QR) != 0)
   ch = 'R';
  sBuf.append(ch).append('_');
  if ((flags & AA) != 0)
   sBuf.append('A').append('A');
  if ((flags & TC) != 0)
   sBuf.append('T').append('C');
  if ((flags & RD) != 0)
   sBuf.append('R').append('D');
  if ((flags & RA) != 0)
   sBuf.append('R').append('A');
  if ((flags & AD) != 0)
   sBuf.append('A').append('D');
  if ((flags & CD) != 0)
   sBuf.append('C').append('D');
  return new String(sBuf);
 }

 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof DNSMsgHeader && obj != this)
    return obj;
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

 public int hashCode()
 {
  return ((((((((((((this.id & MAX_ID) * 31) ^
   (this.flags & MAX_FLAGS)) * 31) ^
   (this.qdCount & MAX_COUNT)) * 31) ^
   (this.anCount & MAX_COUNT)) * 31) ^
   (this.nsCount & MAX_COUNT)) * 31) ^
   (this.arCount & MAX_COUNT)) * 31) ^ 6;
 }

 public boolean equals(Object obj)
 {
  DNSMsgHeader header;
  return obj == this || obj instanceof DNSMsgHeader &&
   (header = (DNSMsgHeader)obj).id == this.id &&
   header.flags == this.flags &&
   header.qdCount == this.qdCount &&
   header.anCount == this.anCount &&
   header.nsCount == this.nsCount &&
   header.arCount == this.arCount;
 }

/**
 * NOTE: Result != null, result length() > 0, result is 'in-line'.
 */
 public String toString()
 {
  short flags = this.flags;
  return new String((new StringBuffer(60)).append('0').append('x').
   append(UnsignedInt.toHexString(this.id & MAX_ID, true,
   ((ID_LENGTH * JavaConsts.BYTE_SIZE - 1) >> 2) + 1)).append(' ').
   append(flagBitsAbbreviation(flags & MAX_FLAGS)).append('/').
   append(UnsignedInt.toAbbreviation((flags >> OPCODE_SHIFT) &
   MAX_OPCODE, OPCODE_ABBREVS)).append('/').
   append(UnsignedInt.toAbbreviation(flags & MAX_RCODE,
   RCODE_ABBREVS)).append(' ').
   append(UnsignedInt.toString(this.qdCount & MAX_COUNT, true)).
   append(' ').
   append(UnsignedInt.toString(this.anCount & MAX_COUNT, true)).
   append(' ').
   append(UnsignedInt.toString(this.nsCount & MAX_COUNT, true)).
   append(' ').
   append(UnsignedInt.toString(this.arCount & MAX_COUNT, true)));
 }
}

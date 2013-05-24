/*
 * @(#) src/net/sf/ivmaidns/dns/DNSConnection.java --
 * Class for DNS TCP connection.
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

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Class for DNS TCP connection (client/server-side).
 **
 * @version 3.0
 * @author Ivan Maidanski
 */
public final class DNSConnection
{

/**
 * NOTE: Standard 'domain' service TCP/UDP port.
 */
 public static final int PORT = 53;

/**
 * NOTE: The maximum length of DNS message.
 **
 * @since 3.0
 */
 public static final int MAX_MSG_LEN = 0xFFFF;

/**
 * NOTE: If listener == null then no listening is performed.
 * listener is shared among all instances of this class.
 **
 * @since 3.0
 */
 protected static ServerSocket listener;

/**
 * NOTE: If socket == null then connection is closed.
 */
 protected Socket socket;

 protected byte[] msgBytes;

 protected int msgLen;

/**
 * NOTE: These are input and output streams.
 **
 * @since 3.0
 */
 protected BufferedInputStream in;

 protected OutputStream out;

/**
 * NOTE: Buffer for writing message length.
 **
 * @since 3.0
 */
 protected final byte[] lenBuf = new byte[2];

/**
 * NOTE: socket is closed initially.
 **
 * @since 2.2
 */
 public DNSConnection() {}

/**
 * NOTE: Start listening (if not already) on DNS port for incoming
 * TCP connections. If this port is already busy then BindException
 * (subclass of SocketException) is thrown. If SecurityException is
 * caught then SocketException is thrown.
 **
 * @since 3.0
 */
 public static void listen()
  throws IOException
 {
  ServerSocket curListener;
  if ((curListener = listener) == null)
  {
   try
   {
    listener = new ServerSocket(PORT);
   }
   catch (SecurityException e)
   {
    throw new SocketException("SecurityException: listen()");
   }
  }
  curListener = null;
 }

/**
 * NOTE: Stop listening on DNS port for incoming TCP connections.
 **
 * @since 3.0
 */
 public static void stopListening()
 {
  ServerSocket curListener;
  if ((curListener = listener) != null)
  {
   listener = null;
   try
   {
    curListener.close();
   }
   catch (IOException e) {}
   curListener = null;
  }
 }

/**
 * NOTE: old connection should be closed. Wait for any incoming
 * connection and accept it. If listening is not active or if
 * SecurityException is caught then SocketException is thrown. If
 * waiting fails then InterruptedIOException is thrown. Must be
 * synchronized outside.
 **
 * @since 3.0
 */
 public void openIncoming()
  throws IOException
 {
  ServerSocket curListener;
  if ((curListener = listener) != null)
  {
   try
   {
    Socket socket = curListener.accept();
    BufferedInputStream in =
     new BufferedInputStream(socket.getInputStream(),
     DNSMsgHeader.UDP_PACKET_LEN);
    this.out = socket.getOutputStream();
    this.in = in;
    this.msgBytes = null;
    this.socket = socket;
    return;
   }
   catch (SecurityException e) {}
  }
  throw new SocketException(curListener == null ? "Not listening" :
             "SecurityException: accept()");
 }

/**
 * NOTE: old connection should be closed. server must be != null. If
 * server is down or unreachable then NoRouteToHostException
 * (subclass of SocketException) is thrown. If connection is
 * remotely refused then ConnectException (subclass of
 * SocketException) is thrown. If SecurityException is caught then
 * SocketException is thrown. Must be synchronized outside.
 **
 * @since 2.2
 */
 public void open(InetAddress server)
  throws NullPointerException, IOException
 {
  server.hashCode();
  try
  {
   Socket socket = new Socket(server, PORT);
   BufferedInputStream in =
    new BufferedInputStream(socket.getInputStream(),
    DNSMsgHeader.UDP_PACKET_LEN);
   this.out = socket.getOutputStream();
   this.in = in;
   this.socket = socket;
  }
  catch (SecurityException e)
  {
   throw new SocketException("SecurityException: connect(" +
              server.getHostAddress() + ")");
  }
  this.msgBytes = null;
 }

/**
 * NOTE: Result != null unless connection is closed.
 **
 * @since 2.0
 */
 public final InetAddress getInetAddress()
 {
  Socket socket;
  InetAddress address = null;
  if ((socket = this.socket) != null)
   address = socket.getInetAddress();
  return address;
 }

/**
 * NOTE: msgBytes must be != null. If msgBytes array is too large
 * then it is truncated. msgBytes array is not changed anyway. Data
 * is flushed. Must be synchronized outside.
 */
 public void send(byte[] msgBytes)
  throws NullPointerException, IOException
 {
  int msgLen;
  if ((msgLen = msgBytes.length) >= MAX_MSG_LEN)
   msgLen = MAX_MSG_LEN;
  OutputStream out;
  if ((out = this.out) == null)
   throw new SocketException("Connection closed");
  byte[] lenBuf = this.lenBuf;
  lenBuf[0] = (byte)(msgLen >> 8);
  lenBuf[1] = (byte)msgLen;
  out.write(lenBuf, 0, 2);
  out.write(msgBytes, 0, msgLen);
  out.flush();
 }

/**
 * NOTE: If !wait and no message received yet then result == null.
 * InterruptedIOException and EOFException may be thrown (only if
 * wait is true). Connection remains valid even if IOException is
 * thrown. Must be synchronized outside.
 */
 public byte[] receive(boolean wait)
  throws IOException
 {
  byte[] msgBytes;
  int msgLen, len;
  BufferedInputStream in;
  if ((in = this.in) == null)
   throw new SocketException("Connection closed");
  if ((msgLen = this.msgLen) <= 0)
   msgLen = 0;
  if ((msgBytes = this.msgBytes) == null)
  {
   do
   {
    if (!wait && in.available() <= 0)
     return null;
     else if ((len = in.read()) < 0)
      throw new EOFException();
      else if (msgLen <= 0)
       this.msgLen = msgLen = len + 1;
       else break;
   } while (true);
   if ((msgLen = ((msgLen - 1) << 8) | len) <= 0)
    msgLen = 0;
   this.msgBytes = msgBytes = new byte[msgLen];
   msgLen = 0;
  }
  for (int avail; (len = msgBytes.length - (this.msgLen =
       msgLen)) > 0; msgLen += len)
   if (!wait && (avail = in.available()) < len &&
       (len = avail) <= 0)
    return null;
    else if ((len = in.read(msgBytes, msgLen, len)) < 0)
     throw new EOFException();
  this.msgBytes = null;
  this.msgLen = 0;
  return msgBytes;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public void close()
 {
  Socket socket;
  if ((socket = this.socket) != null)
  {
   this.socket = null;
   this.in = null;
   this.out = null;
   this.msgBytes = null;
   try
   {
    socket.close();
   }
   catch (IOException e) {}
   socket = null;
  }
 }

/**
 * NOTE: header must be != null, records must be != null and
 * records[index] != null for any index. records length may be not
 * adequate to header. Names compression is performed (relatively to
 * the name of the first record). records array is not changed
 * anyway. Result != null.
 **
 * @since 3.0
 */
 public static final byte[] encode(DNSMsgHeader header,
         DNSRecord[] records)
  throws NullPointerException
 {
  int msgLen = DNSMsgHeader.HEADER_LEN, totalCount = records.length;
  int capacity = DNSMsgHeader.UDP_PACKET_LEN;
  byte[] msgBytes, newMsgBytes;
  header.putTo(msgBytes = new byte[capacity]);
  for (int index = 0, qdCount = header.getQdCount(), recLen;
       index < totalCount; index++)
  {
   DNSRecord rec;
   if ((recLen = (rec = records[index]).getTotalLen()) >
       capacity - msgLen)
   {
    if ((recLen += msgLen) <= 0)
     capacity = -1 >>> 1;
     else if ((capacity += capacity >> 1) <= recLen)
      capacity = recLen;
    System.arraycopy(msgBytes, 0,
     newMsgBytes = new byte[capacity], 0, msgLen);
    msgBytes = newMsgBytes;
   }
   msgLen = rec.putTo(msgBytes, msgLen,
    index >= qdCount, DNSMsgHeader.HEADER_LEN);
  }
  if (capacity > msgLen)
  {
   System.arraycopy(msgBytes, 0,
    newMsgBytes = new byte[msgLen], 0, msgLen);
   msgBytes = newMsgBytes;
  }
  return msgBytes;
 }

/**
 * NOTE: msgBytes must be != null. If DNS message header is bad then
 * result == null. Else result != null and result[index] != null for
 * any index (the result length may be less than that declared in
 * the decoded header). msgBytes array is not changed anyway.
 **
 * @since 3.0
 */
 public static final DNSRecord[] decode(byte[] msgBytes)
  throws NullPointerException
 {
  DNSRecord[] records = null;
  if (msgBytes.length >= DNSMsgHeader.HEADER_LEN)
  {
   int totalCount = DNSMsgHeader.getTotalCount(msgBytes), index = 0;
   int[] ofsRef = new int[1];
   ofsRef[0] = DNSMsgHeader.HEADER_LEN;
   records = new DNSRecord[totalCount];
   try
   {
    for (int qdCount = DNSMsgHeader.getQdCount(msgBytes);
         index < totalCount && ofsRef[0] < msgBytes.length; index++)
     records[index] =
      new DNSRecord(msgBytes, ofsRef, index >= qdCount);
   }
   catch (IllegalArgumentException e) {}
   if (index < totalCount)
   {
    DNSRecord[] newRecords;
    System.arraycopy(records, 0,
     newRecords = new DNSRecord[index], 0, index);
    records = newRecords;
   }
  }
  return records;
 }
}

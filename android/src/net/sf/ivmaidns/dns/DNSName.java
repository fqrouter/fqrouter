/*
 * @(#) src/net/sf/ivmaidns/dns/DNSName.java --
 * Class for representing DNS resource name.
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

import net.sf.ivmaidns.util.Immutable;
import net.sf.ivmaidns.util.JavaConsts;
import net.sf.ivmaidns.util.ParserException;
import net.sf.ivmaidns.util.ReallyCloneable;
import net.sf.ivmaidns.util.Sortable;
import net.sf.ivmaidns.util.Verifiable;

/**
 * Class for representing DNS resource name (as defined in RFC1035).
 **
 * @version 3.0
 * @author Ivan Maidanski
 */
public final class DNSName
 implements Immutable, ReallyCloneable, Serializable, Sortable,
            Verifiable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 2.3
 */
 private static final long serialVersionUID = 902307735131026602L;

 public static final char SEPARATOR = '.';

 public static final char WILDCARD = '*';

 public static final char ESCAPE = '\\';

 public static final char THIS_ZONE = '@';

 public static final char EMAIL = '@';

 public static final char QUOTE = '"';

 public static final int MAX_LABEL_LEN = 0x3F;

 public static final int COMPRESSED_NAME_TAG = 0xC0;

/**
 * NOTE: These are possible error codes for DNSName(name, domain)
 * and parse(name, escapeSeparator, domainBytes, domainOffset).
 **
 * @since 3.0
 */
 public static final int ERROR_BAD_CHAR = 1;

 public static final int ERROR_BAD_ESCAPING = 2;

 public static final int ERROR_EMPTY_LABEL = 3;

 public static final int ERROR_LONG_NAME = 4;

 public static final int ERROR_UNSUPPORTED = 5;

 public static final DNSName ROOT = new DNSName(new String(), null);

 public static final DNSName IN_ADDR_ARPA =
  new DNSName("in-addr.arpa", null);

 public static final DNSName IP6_INT = new DNSName("ip6.int", null);

/**
 * NOTE: bytes != null. bytes length must be == lengthOf(bytes, 0).
 * bytes may be not canonical, bytes cannot be compressed. bytes
 * content is described in RFC1035.
 **
 * @serial
 */
 protected final byte[] bytes;

/**
 * NOTE: name must be != null, name may be == "", name may be
 * THIS_ZONE. domain may be == null.
 * ParserException(name, index, error) is thrown only if name cannot
 * be parsed.
 **
 * @since 2.0
 */
 public DNSName(String name, DNSName domain)
  throws NullPointerException, ParserException
 {
  this.bytes = parse(name.length() != 1 ||
   name.charAt(0) != THIS_ZONE ? name : new String(), true,
   domain != null ? domain.bytes : null, 0);
 }

/**
 * NOTE: name must be != null. Constructed DNS name is canonized.
 **
 * @since 2.5
 */
 public DNSName(DNSName name)
  throws NullPointerException
 {
  byte[] bytes, nameBytes = name.bytes;
  this.bytes = canonize(bytes = (byte[])nameBytes.clone(), 0) ?
   bytes : nameBytes;
 }

/**
 * NOTE: bytes must be != null. ArrayIndexOutOfBoundsException is
 * thrown only if 0 > offset or offset >= bytes length.
 * Decompression is supported. If bytes content is not valid then
 * IllegalArgumentException is thrown. bytes array is not changed
 * anyway.
 **
 * @since 2.1
 */
 public DNSName(byte[] bytes, int offset)
  throws NullPointerException, ArrayIndexOutOfBoundsException,
         IllegalArgumentException
 {
  int len;
  byte[] bytesCopy;
  if ((len = lengthOf(bytes, offset)) > 0)
  {
   System.arraycopy(bytes, offset,
    bytesCopy = new byte[len], 0, len);
   if (lengthOf(this.bytes = bytesCopy, 0) == len)
    return;
  }
   else if ((len = -len) > 0)
   {
    System.arraycopy(bytes, offset,
     bytesCopy = new byte[len], 0, len);
    if ((bytes = decompress(bytesCopy, 0, bytes)) != null &&
        lengthOf(this.bytes = bytes, 0) == bytes.length)
     return;
   }
  throw new IllegalArgumentException("Bad resource name");
 }

/**
 * NOTE: bytes must be != null. Method for putting resource name to
 * byte array. No compression. Enough capacity must be provided (at
 * least getBytesLen() bytes). Result is new offset (just after this
 * name).
 **
 * @since 2.5
 */
 public int putTo(byte[] bytes, int offset)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  byte[] bytesCopy;
  int len = (bytesCopy = this.bytes).length;
  System.arraycopy(bytesCopy, 0, bytes, offset, len);
  return offset + len;
 }

/**
 * NOTE: Result != null, result length > 0, result is copy.
 */
 public final byte[] getBytes()
 {
  return (byte[])this.bytes.clone();
 }

/**
 * NOTE: Result > 0.
 **
 * @since 2.1
 */
 public final int getBytesLen()
 {
  return this.bytes.length;
 }

/**
 * NOTE: Letters case in result is not adjusted to canonical.
 */
 public String getAbsolute()
 {
  byte[] bytes = this.bytes;
  return toString(bytes, 0, countLabels(bytes, 0), true, true);
 }

/**
 * NOTE: All separator characters in labels (if exist) are escaped.
 */
 public String[] getLabels()
 {
  byte[] bytes = this.bytes;
  int level = countLabels(bytes, 0);
  String[] labels = new String[level];
  for (int offset = 0; level > 0;
       offset += (bytes[offset] & JavaConsts.BYTE_MASK) + 1)
   labels[--level] = toString(bytes, offset, 1, true, false);
  return labels;
 }

 public String getLabelAt(int level)
  throws ArrayIndexOutOfBoundsException
 {
  byte[] bytes = this.bytes;
  int backLevel;
  if (level >= 0 &&
      (backLevel = countLabels(bytes, 0) - level - 1) >= 0)
   return toString(bytes, labelOffset(bytes, 0, backLevel),
    1, true, false);
  throw new ArrayIndexOutOfBoundsException(level);
 }

 public int getLevel()
 {
  return countLabels(this.bytes, 0);
 }

/**
 * NOTE: domain may be == null. Result != null, result != "".
 * THIS_ZONE may be returned.
 */
 public String getRelative(DNSName domain)
 {
  if (this != domain)
  {
   byte[] bytes = this.bytes, domainBytes;
   int level = countLabels(bytes, 0);
   boolean absolute = true;
   if (domain != null)
   {
    int domainLevel = countLabels(domainBytes = domain.bytes, 0);
    if (level >= domainLevel &&
        bytes.length >= domainBytes.length &&
        compareNames(bytes, labelOffset(bytes, 0,
        level - domainLevel), domainBytes, 0) == 0)
    {
     level -= domainLevel;
     absolute = false;
    }
   }
   if (absolute || level > 0)
    return toString(bytes, 0, level, true, absolute);
  }
  char[] chars = new char[1];
  chars[0] = THIS_ZONE;
  return new String(chars);
 }

/**
 * NOTE: domain may be == null. Letters case is ignored.
 */
 public boolean isInDomain(DNSName domain, boolean strict)
 {
  strict = !strict;
  if (this != domain)
  {
   if (domain == null)
    return false;
   byte[] bytes = this.bytes, domainBytes;
   int level;
   int domainLevel = countLabels(domainBytes = domain.bytes, 0);
   strict = (level = countLabels(bytes, 0)) >= domainLevel &&
    (strict || level != domainLevel) && bytes.length >=
    domainBytes.length && compareNames(bytes, labelOffset(bytes, 0,
    level - domainLevel), domainBytes, 0) == 0;
  }
  return strict;
 }

/**
 * NOTE: Negative level is treated as zero. Result != null.
 */
 public DNSName getDomain(int level)
  throws ArrayIndexOutOfBoundsException
 {
  byte[] bytes = this.bytes;
  int backLevel;
  if (level <= 0)
   level = 0;
  if ((backLevel = countLabels(bytes, 0) - level) >= 0)
   return new DNSName(bytes, labelOffset(bytes, 0, backLevel));
  throw new ArrayIndexOutOfBoundsException(level);
 }

/**
 * NOTE: Result != null.
 */
 public DNSName getDomain()
 {
  byte[] bytes = this.bytes;
  int offset;
  if ((offset = bytes[0] & JavaConsts.BYTE_MASK) > 0)
   offset++;
  return new DNSName(bytes, offset);
 }

 public String getLastLabel()
 {
  byte[] bytes = this.bytes;
  return toString(bytes, 0, bytes[0] != 0 ? 1 : 0, true, false);
 }

 public String getRelativeAt(int level)
  throws ArrayIndexOutOfBoundsException
 {
  byte[] bytes = this.bytes;
  if (level >= 0)
   return toString(bytes, 0, countLabels(bytes, 0) - level,
    true, false);
  throw new ArrayIndexOutOfBoundsException(level);
 }

 public boolean isCaseSensitive()
 {
  return false;
 }

 public String joinLabels(String[] labels, int level, int count,
         boolean absolute)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int len = 0, index;
  if (count > 0)
   for (index = level + count;
        index > level; len += labels[--index].length() + 1);
  if (len <= 0)
   len = 1;
  StringBuffer sBuf = new StringBuffer(len);
  if (count > 0)
  {
   if (labels[level].length() <= 0)
   {
    if (count == 1)
     sBuf.append(SEPARATOR);
    absolute = true;
   }
   level += count;
   do
   {
    String label = labels[--level];
    char ch;
    len = label.length();
    if (label.indexOf(SEPARATOR, 0) < 0)
    {
     sBuf.append(label);
     while (--len >= 0 && label.charAt(len) == ESCAPE);
     if (((label.length() - len) & 1) == 0)
      sBuf.append(ESCAPE);
    }
     else for (index = 0; index < len; sBuf.append(ch))
      if ((ch = label.charAt(index++)) == ESCAPE || ch == SEPARATOR)
      {
       if (ch == ESCAPE && index < len)
        ch = label.charAt(index++);
       sBuf.append(ESCAPE);
      }
    if (--count <= 0)
     break;
    sBuf.append(SEPARATOR);
   } while (true);
  }
  len = labels.length;
  if (absolute)
   sBuf.append(SEPARATOR);
  return new String(sBuf);
 }

 public final boolean reversed()
 {
  return true;
 }

 public final char separator()
 {
  return SEPARATOR;
 }

 public DNSName root()
 {
  return ROOT;
 }

/**
 * NOTE: bytes must be != null. ArrayIndexOutOfBoundsException is
 * thrown only if 0 > offset or offset >= bytes length. If result
 * > 0 then bytes content is valid (and not compressed), else if 0
 * > result then bytes content is compressed with result length
 * (compressed), else name is not valid.
 */
 public static final int lengthOf(byte[] bytes, int offset)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int len, bytesLen = bytes.length, beginning = offset;
  while ((len = bytes[offset++] & JavaConsts.BYTE_MASK) > 0)
   if (len > MAX_LABEL_LEN)
    if (len >= COMPRESSED_NAME_TAG && offset < bytesLen)
     return ~(offset - beginning);
     else return 0;
    else if ((offset += len) >= bytesLen)
     return 0;
  return offset - beginning;
 }

/**
 * NOTE: bytes and msgBytes must be != null. If bytes content is
 * valid and not compressed then result == bytes. Else if
 * compressed-label tag is found then name is decompressed to new
 * bytes array, preserving (copying) content from bytes array before
 * and after specified name. After processing all compressed-label
 * tags (in specified name) new bytes array is returned (if
 * decompression fails then result == null).
 * ArrayIndexOutOfBoundsException is thrown only if 0 > offset or
 * offset >= bytes length. bytes and msgBytes arrays are not changed
 * anyway.
 */
 public static final byte[] decompress(byte[] bytes, int offset,
         byte[] msgBytes)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int len, bytesLen = bytes.length, msgLength;
  int lastAddr = msgLength = msgBytes.length;
  while ((len = bytes[offset] & JavaConsts.BYTE_MASK) > 0)
   if (len > MAX_LABEL_LEN)
    if (len >= COMPRESSED_NAME_TAG)
    {
     if (bytesLen - 1 <= offset)
      return null;
     int address = len = bytes[offset + 1] & JavaConsts.BYTE_MASK |
      ((len - COMPRESSED_NAME_TAG) << JavaConsts.BYTE_SIZE), count;
     do
     {
      if (len >= msgLength)
       return null;
      if ((count = msgBytes[len++] & JavaConsts.BYTE_MASK) <= 0)
       break;
      if (count > MAX_LABEL_LEN)
       if (++len > msgLength)
        return null;
        else break;
      len += count;
     } while (true);
     if (len > lastAddr)
      return null;
     len -= lastAddr = address;
     if ((bytesLen -= 2) + len <= 0)
      return null;
     byte[] newBytes;
     System.arraycopy(bytes, offset + 2, newBytes =
      new byte[bytesLen + len], offset + len, bytesLen - offset);
     System.arraycopy(bytes, 0, newBytes, 0, offset);
     System.arraycopy(msgBytes, address, bytes =
      newBytes, offset, len);
     bytesLen += len;
    }
     else return null;
    else if ((offset += len + 1) >= bytesLen)
     return null;
  return bytes;
 }

/**
 * NOTE: msgBytes must be valid (at offset and at baseNameOffset)
 * and not compressed. If possible then name at offset is compressed
 * (using only content at baseNameOffset). msgBytes array is
 * altered. Result is new offset (after processed name).
 **
 * @since 2.5
 */
 public static final int compressAt(byte[] msgBytes, int offset,
         int baseNameOffset)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int len = countLabels(msgBytes, offset) -
   countLabels(msgBytes, baseNameOffset);
  int destOffset = 0, srcOffset = 0, beginning = offset;
  if (len > 0)
   do
   {
    offset += (msgBytes[offset] & JavaConsts.BYTE_MASK) + 1;
   } while (--len > 0);
   else while (++len <= 0)
    baseNameOffset +=
     (msgBytes[baseNameOffset] & JavaConsts.BYTE_MASK) + 1;
  for (int baseLen;
       (len = msgBytes[offset++] & JavaConsts.BYTE_MASK) > 0;
       offset += len, baseNameOffset += baseLen)
   if ((baseLen = msgBytes[baseNameOffset++] &
       JavaConsts.BYTE_MASK) == len)
   {
    do
    {
     if (msgBytes[offset] != msgBytes[baseNameOffset])
      break;
     offset++;
     baseNameOffset++;
    } while (--len > 0);
    if (len > 0)
     destOffset = 0;
     else if (destOffset <= 0)
     {
      destOffset = offset - baseLen;
      srcOffset = baseNameOffset - baseLen - 1;
     }
    baseLen = len;
   }
    else destOffset = 0;
  if (destOffset > 0 && baseNameOffset < beginning &&
      srcOffset >> JavaConsts.BYTE_SIZE <=
      JavaConsts.BYTE_MASK - COMPRESSED_NAME_TAG)
  {
   msgBytes[(offset = destOffset) - 1] = (byte)((srcOffset >>
    JavaConsts.BYTE_SIZE) + COMPRESSED_NAME_TAG);
   msgBytes[offset++] = (byte)srcOffset;
  }
  return offset;
 }

/**
 * NOTE: bytes must be valid (and not compressed). Upper-case
 * letters are converted to lower-case. Result is true if and only
 * if bytes array is altered.
 **
 * @since 2.5
 */
 public static final boolean canonize(byte[] bytes, int offset)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int value, len;
  boolean changed = false;
  while ((len = bytes[offset++] & JavaConsts.BYTE_MASK) > 0)
   do
   {
    if ((char)((value = bytes[offset] &
        JavaConsts.BYTE_MASK) - 'A') <= 'Z' - 'A')
    {
     bytes[offset] = (byte)(value + ('a' - 'A'));
     changed = true;
    }
    offset++;
   } while (--len > 0);
  return changed;
 }

/**
 * NOTE: bytes must be valid (and not compressed). Result >= 0.
 */
 public static final int countLabels(byte[] bytes, int offset)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int count = 0;
  for (int len; (len = bytes[offset] & JavaConsts.BYTE_MASK) > 0;
       offset += len + 1, count++);
  return count;
 }

/**
 * NOTE: bytes must be valid (and not compressed). Negative
 * backLevel is treated as zero. Result >= offset.
 */
 public static final int labelOffset(byte[] bytes, int offset,
         int backLevel)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  while (backLevel-- > 0)
   offset += (bytes[offset] & JavaConsts.BYTE_MASK) + 1;
  return offset;
 }

/**
 * NOTE: bytes must be valid (and not compressed). Upper-case
 * letters are treated as lower-case ones.
 */
 public static final int hashCode(byte[] bytes, int offset)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int code = 0, hash, index = 0;
  while ((hash = bytes[offset++] & JavaConsts.BYTE_MASK) > 0)
  {
   int len = hash, value;
   do
   {
    if ((char)((value = bytes[offset++] &
        JavaConsts.BYTE_MASK) - 'A') <= 'Z' - 'A')
     value += 'a' - 'A';
    hash = ((hash << 5) - hash) ^ value;
   } while (--len > 0);
   code ^= ++index * hash;
  }
  return code;
 }

/**
 * NOTE: bytesA and bytesB must be valid (and not compressed). Names
 * are compared in label-by-label manner, starting at level 0.
 * Upper-case letters are treated as lower-case (as defined in
 * RFC2065).
 */
 public static final int compareNames(byte[] bytesA, int offsetA,
         byte[] bytesB, int offsetB)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int levelA = 0, levelB = 0;
  if (offsetA != offsetB || bytesA != bytesB)
  {
   levelA = countLabels(bytesA, offsetA);
   levelB = countLabels(bytesB, offsetB);
   while ((--levelA | --levelB) >= 0)
   {
    int indexA, indexB, valueA, valueB;
    int lenA = bytesA[indexA = labelOffset(bytesA, offsetA,
     levelA)] & JavaConsts.BYTE_MASK;
    int lenB = bytesB[indexB = labelOffset(bytesB, offsetB,
     levelB)] & JavaConsts.BYTE_MASK;
    while ((--lenA | --lenB) >= 0)
     if (bytesA[++indexA] != bytesB[++indexB])
     {
      if ((char)((valueA = bytesA[indexA] &
          JavaConsts.BYTE_MASK) - 'A') <= 'Z' - 'A')
       valueA += 'a' - 'A';
      if ((char)((valueB = bytesB[indexB] &
          JavaConsts.BYTE_MASK) - 'A') <= 'Z' - 'A')
       valueB += 'a' - 'A';
      if (valueA != valueB)
       return valueA - valueB;
     }
    if ((lenA -= lenB) != 0)
     return lenA;
   }
  }
  return levelA - levelB;
 }

/**
 * NOTE: bytes must be valid (and not compressed). If
 * !escapeSeparator then separator characters in labels (if any) are
 * not escaped. Result != null, result is escaped, result is
 * 'in-line'. Negative labelsCount is treated as zero.
 */
 public static final String toString(byte[] bytes, int offset,
         int labelsCount, boolean escapeSeparator, boolean absolute)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int len = labelOffset(bytes, offset, labelsCount);
  StringBuffer sBuf = new StringBuffer(len - offset + 1);
  if (labelsCount > 0)
   do
   {
    char ch;
    for (len = bytes[offset++] & JavaConsts.BYTE_MASK;
         len-- > 0; sBuf.append(ch))
     if ((ch = (char)(bytes[offset++] & JavaConsts.BYTE_MASK)) !=
         SEPARATOR && (char)(ch - 'a') > 'z' - 'a' &&
         (char)(ch - 'A') > 'Z' - 'A' && (char)(ch - '0') >
         '9' - '0' && ch != WILDCARD && ch != '-' && ch != '_' ||
         ch == SEPARATOR && escapeSeparator)
     {
      sBuf.append(ESCAPE);
      if (ch == '[' || (char)(ch - (' ' + 1)) >=
          (JavaConsts.BYTE_MASK >>> 1) - (' ' + 1))
      {
       sBuf.append((char)(ch / (('9' - '0' + 1) * ('9' - '0' + 1)) +
        '0')).append((char)(ch / ('9' - '0' + 1) %
        ('9' - '0' + 1) + '0'));
       ch = (char)(ch % ('9' - '0' + 1) + '0');
      }
     }
    if (--labelsCount <= 0)
     break;
    sBuf.append(SEPARATOR);
   } while (true);
  if (absolute)
   sBuf.append(SEPARATOR);
  return new String(sBuf);
 }

/**
 * NOTE: name must be != null, domainBytes must be != null. If name
 * is not absolute then name is relative to the specified domain.
 * Escaping in name is allowed. name must not contain non-printable
 * characters. Result != null. ParserException(name, index, error)
 * is thrown only if name cannot be parsed.
 */
 public static final byte[] parse(String name,
         boolean escapeSeparator, byte[] domainBytes,
         int domainOffset)
  throws NullPointerException, ParserException,
         ArrayIndexOutOfBoundsException
 {
  int offset = 0, count = 0, len = name.length();
  int index = -1, error = 0;
  byte[] bytes = new byte[len + 1];
  char ch, ch1;
  while (++index < len)
   if ((char)((ch = name.charAt(index)) - (' ' + 1)) >=
       (JavaConsts.BYTE_MASK >>> 1) - (' ' + 1))
   {
    error = ERROR_BAD_CHAR;
    break;
   }
    else if (!escapeSeparator || ch != SEPARATOR)
    {
     if (ch == THIS_ZONE)
     {
      error = ERROR_BAD_CHAR;
      break;
     }
      else if (ch == ESCAPE && index + 1 < len)
       if ((ch = name.charAt(++index)) == '[')
       {
        error = ERROR_UNSUPPORTED;
        break;
       }
        else if ((char)(ch - '0') <= '9' - '0')
        {
         ch -= '0';
         if (index + 1 < len && (ch1 =
             (char)(name.charAt(index + 1) - '0')) <= '9' - '0')
         {
          ch = (char)(ch * ('9' - '0' + 1) + ch1);
          if (++index + 1 < len && (ch1 =
              (char)(name.charAt(index + 1) - '0')) <= '9' - '0')
          {
           ch = (char)(ch * ('9' - '0' + 1) + ch1);
           index++;
          }
         }
        }
     if (ch > JavaConsts.BYTE_MASK)
     {
      error = ERROR_BAD_ESCAPING;
      break;
     }
     if (++count > MAX_LABEL_LEN)
     {
      error = ERROR_LONG_NAME;
      break;
     }
     bytes[++offset] = (byte)ch;
    }
     else if (count > 0)
     {
      bytes[(offset++) - count] = (byte)count;
      count = 0;
     }
      else if (len > 1)
      {
       error = ERROR_EMPTY_LABEL;
       break;
      }
  if (error > 0)
   throw new ParserException(name, index, error);
  if (len > 0)
   bytes[(offset++) - count] = (byte)count;
  if ((len == 0 || count > 0) && (domainBytes == null ||
      (count = lengthOf(domainBytes, domainOffset)) <= 0))
   count = 1;
  if (bytes.length != offset + count)
  {
   byte[] newBytes;
   System.arraycopy(bytes, 0, newBytes =
    new byte[offset + count > 0 ? offset + count : -1 >>> 1], 0,
    offset);
   bytes = newBytes;
  }
  if (count > 1)
   System.arraycopy(domainBytes, domainOffset,
    bytes, offset, count);
  return bytes;
 }

/**
 * NOTE: Result is e-mail string represenation of DNS name. Result
 * != null, result is 'in-line'. Result == "" only if getLevel()
 * == 0.
 */
 public String getAsEmail()
 {
  byte[] bytes = this.bytes;
  int level;
  StringBuffer sBuf = new StringBuffer(bytes.length - 1);
  if ((level = countLabels(bytes, 0)) > 0)
   sBuf.append(toString(bytes, 0, 1, false, false)).append(EMAIL).
    append(toString(bytes, (bytes[0] & JavaConsts.BYTE_MASK) + 1,
    level - 1, true, level == 1));
  return new String(sBuf);
 }

 public boolean isWildcard()
 {
  byte[] bytes = this.bytes;
  return bytes[0] == 1 &&
   (bytes[1] & JavaConsts.BYTE_MASK) == WILDCARD;
 }

/**
 * NOTE: wildcard may be == null. Letters case is ignored.
 **
 * @since 2.0
 */
 public boolean equalsWildcard(DNSName wildcard)
 {
  if (this != wildcard)
  {
   if (wildcard == null)
    return false;
   byte[] bytes = this.bytes, wildcardBytes;
   if ((wildcardBytes = wildcard.bytes) != bytes)
   {
    int offset = 0, wildcardOffset = 0;
    if (wildcardBytes[0] == 1 &&
        (wildcardBytes[1] & JavaConsts.BYTE_MASK) == WILDCARD)
     if ((offset = countLabels(bytes, 0) -
         countLabels(wildcardBytes, wildcardOffset = 2)) > 0)
      offset = labelOffset(bytes, 0, offset);
      else return false;
    return compareNames(bytes, offset,
     wildcardBytes, wildcardOffset) == 0;
   }
  }
  return true;
 }

/**
 * NOTE: name may be == null. This comparison is case-sensitive.
 **
 * @since 2.5
 */
 public boolean equalsExact(DNSName name)
 {
  if (this != name)
  {
   if (name == null)
    return false;
   int offset;
   byte[] bytes = this.bytes, nameBytes;
   if ((nameBytes = name.bytes) != bytes)
    if ((offset = bytes.length) != nameBytes.length)
     return false;
     else while (offset-- > 0)
      if (bytes[offset] != nameBytes[offset])
       return false;
  }
  return true;
 }

 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof DNSName && obj != this)
    return obj;
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

 public int hashCode()
 {
  return hashCode(this.bytes, 0);
 }

/**
 * NOTE: Letters case is ignored (according to RFC2065).
 */
 public boolean equals(Object obj)
 {
  byte[] bytes = this.bytes, nameBytes;
  return obj == this || obj instanceof DNSName &&
   (nameBytes = ((DNSName)obj).bytes).length == bytes.length &&
   compareNames(bytes, 0, nameBytes, 0) == 0;
 }

/**
 * NOTE: Method for canonical ordering (according to RFC2065).
 **
 * @since 2.5
 */
 public boolean greaterThan(Object obj)
 {
  return obj != this && obj instanceof DNSName &&
   compareNames(this.bytes, 0, ((DNSName)obj).bytes, 0) > 0;
 }

/**
 * NOTE: name must be != null.
 */
 public int compareTo(DNSName name)
  throws NullPointerException
 {
  return compareNames(this.bytes, 0, name.bytes, 0);
 }

/**
 * NOTE: Result is 'in-line'.
 */
 public String toString()
 {
  return getAbsolute();
 }

/**
 * NOTE: Check object for its integrity. For debug purpose only.
 **
 * @since 2.5
 */
 public void integrityCheck()
 {
  byte[] bytes;
  if ((bytes = this.bytes) == null)
   throw new InternalError("bytes: null");
  if (bytes.length <= 0 || lengthOf(bytes, 0) != bytes.length)
   throw new InternalError("Bad resource name");
 }

 private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
 {
  in.defaultReadObject();
  byte[] bytes;
  if ((bytes = this.bytes) == null)
   throw new InvalidObjectException("bytes: null");
  if (bytes.length <= 0 || lengthOf(bytes, 0) != bytes.length)
   throw new InvalidObjectException("Bad resource name");
 }
}

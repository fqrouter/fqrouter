/*
 * @(#) src/net/sf/ivmaidns/util/ParserException.java --
 * Class for custom parser exceptions.
 **
 * Copyright (c) 2000 Ivan Maidanski <ivmai@mail.ru>
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

package net.sf.ivmaidns.util;

/**
 * Class for custom parser exceptions.
 **
 * This class extends the semantics of
 * <CODE>NumberFormatException</CODE> class and is used to signal
 * that a particular error has been reached at a particular index
 * unexpectedly while parsing a string or its region. Important
 * notes: this exception class may be overridden (if needed).
 **
 * @see UnsignedInt
 * @see UnsignedLong
 * @see ByteVector
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 1.8
 */
public class ParserException extends NumberFormatException
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.8
 */
 private static final long serialVersionUID = 2211498944992357376L;

/**
 * The string containing the region being parsed.
 **
 * Important notes: this value may be <CODE>null</CODE>.
 **
 * @serial
 **
 * @see ParserException#ParserException()
 * @see ParserException#ParserException(java.lang.String, int, int)
 * @see #getStr()
 * @see #index
 * @see #error
 */
 protected String str;

/**
 * The parsed string index at which <CODE>this</CODE> exception
 * occurs during the string region being parsed.
 **
 * Important notes: <VAR>index</VAR> may be of any integer value; if
 * its value is negative then it should be treated as zero; if its
 * value is too big then it should be treated as
 * <CODE>length()</CODE> of <VAR>str</VAR> (only if <VAR>str</VAR>
 * is not <CODE>null</CODE> else it should be treated as zero).
 **
 * @serial
 **
 * @see ParserException#ParserException()
 * @see ParserException#ParserException(java.lang.String, int, int)
 * @see #getIndex()
 * @see #str
 * @see #error
 */
 protected int index;

/**
 * The error reason code explaining why <CODE>this</CODE> exception
 * is thrown during the string region being parsed.
 **
 * Important notes: <VAR>error</VAR> may be of any integer value; if
 * its value is not positive then it should be treated as zero
 * (indicating an unknown error).
 **
 * @serial
 **
 * @see ParserException#ParserException()
 * @see ParserException#ParserException(java.lang.String, int, int)
 * @see #getError()
 * @see #str
 * @see #index
 */
 protected int error;

/**
 * The default no-argument exception constructor.
 **
 * <VAR>str</VAR> is set to <CODE>null</CODE>, <VAR>index</VAR> and
 * <VAR>error</VAR> are set to zero.
 **
 * @see ParserException#ParserException(java.lang.String, int, int)
 * @see #getMessage()
 */
 public ParserException() {}

/**
 * The standard parser exception constructor.
 **
 * The meaning of a particular error code is custom-defined (zero
 * code indicates an unknown/unset error).
 **
 * @param str
 * the string containing the region being parsed (may be
 * <CODE>null</CODE>, in fact).
 * @param index
 * the string index (may be of any integer value) at which parsing
 * has failed.
 * @param error
 * the reason code (may be of any integer value) explaining why
 * parsing has failed.
 **
 * @see ParserException#ParserException()
 * @see #getMessage()
 * @see #getStr()
 * @see #getIndex()
 * @see #getError()
 */
 public ParserException(String str, int index, int error)
 {
  super(str);
  this.str = str;
  this.index = index;
  this.error = error;
 }

/**
 * Returns the string containing the region being parsed.
 **
 * @return
 * the string (may be <CODE>null</CODE>) with the region being
 * parsed.
 **
 * @see ParserException#ParserException(java.lang.String, int, int)
 * @see #getIndex()
 * @see #getError()
 */
 public final String getStr()
 {
  return this.str;
 }

/**
 * Returns the string index at which parsing has failed.
 **
 * Important notes: result may be of any integer value; if its value
 * is negative then it should be treated as zero; if its value is
 * too big then it should be treated as <CODE>length()</CODE> of
 * <CODE>getStr()</CODE> (only if <CODE>getStr()</CODE> is not
 * <CODE>null</CODE> else it should be treated as zero).
 **
 * @return
 * the string index (may be of any integer value) at which parsing
 * has failed.
 **
 * @see ParserException#ParserException(java.lang.String, int, int)
 * @see #getStr()
 * @see #getError()
 */
 public final int getIndex()
 {
  return this.index;
 }

/**
 * Returns the error reason code explaining why parsing has failed.
 **
 * The meaning of a particular error code is custom-defined.
 **
 * @return
 * the reason code (may be of any integer value) explaining why
 * parsing has failed.
 **
 * @see ParserException#ParserException(java.lang.String, int, int)
 * @see #getStr()
 * @see #getIndex()
 */
 public final int getError()
 {
  return this.error;
 }

/**
 * Returns the detailed message of the exception.
 **
 * This method overrides the default <CODE>getMessage()</CODE> one.
 * The resulting string is a concatenation of the following:
 * "Error", <VAR>error</VAR> represented as a signed decimal
 * integer, ":", <VAR>index</VAR> represented as a signed decimal
 * integer too, ":" and <VAR>str</VAR> (if not <CODE>null</CODE>).
 **
 * @return
 * the message string (may be <CODE>null</CODE>) describing
 * <CODE>this</CODE> parser exception.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ParserException#ParserException(java.lang.String, int, int)
 * @see #getStr()
 * @see #getIndex()
 * @see #getError()
 */
 public String getMessage()
 {
  int value = 15, digit = 0, shift = 0;
  String str;
  if ((str = this.str) != null && (value += str.length()) < 0)
   value = -1 >>> 1;
  StringBuffer sBuf = new StringBuffer(value);
  sBuf.append("Error");
  if ((value = this.error) < 0)
   sBuf.append('-');
   else value = -value;
  shift = 1;
  for (digit = value; (digit /= '9' - '0' + 1) < 0;
       shift *= '9' - '0' + 1);
  do
  {
   sBuf.append((char)('0' - (digit = value / shift)));
   if (shift <= 1)
    break;
   value -= digit * shift;
   shift /= '9' - '0' + 1;
  } while (true);
  sBuf.append(':');
  if ((value = this.index) < 0)
   sBuf.append('-');
   else value = -value;
  shift = 1;
  for (digit = value; (digit /= '9' - '0' + 1) < 0;
       shift *= '9' - '0' + 1);
  do
  {
   sBuf.append((char)('0' - (digit = value / shift)));
   if (shift <= 1)
    break;
   value -= digit * shift;
   shift /= '9' - '0' + 1;
  } while (true);
  sBuf.append(':');
  if (str != null)
   sBuf.append(str);
  return new String(sBuf);
 }
}

/*
 * @(#) src/net/sf/ivmaidns/util/UnsignedLong.java --
 * Class for unsigned 'long' wrappers.
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

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class for unsigned 'long' wrappers.
 **
 * This class wraps a primitive unsigned <CODE>long</CODE> value
 * (like <CODE>Long</CODE> class). The class also contains
 * <CODE>static</CODE> methods for the unsigned <CODE>long</CODE>
 * integer arithmetic: comparison, median, multiplication (simple
 * and modular), division, remainder, power, logarithm, factorial,
 * 'greatest common divisor', modular inversion, conversion to/from
 * a byte array (in the direct and reversed orders), stream bits
 * writing/reading, conversion to/from a string (in some given
 * radix).
 **
 * @see UnsignedInt
 * @see LongVector
 * @see ByteVector
 * @see PseudoRandom
 * @see JavaConsts
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public final class UnsignedLong extends Number
 implements Immutable, ReallyCloneable, Sortable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.1
 */
 private static final long serialVersionUID = -458543887421779763L;

/**
 * The wrapped (encapsulated) unsigned <CODE>long</CODE> value.
 **
 * Important notes: this value is constant.
 **
 * @serial
 **
 * @see UnsignedLong#UnsignedLong(long)
 * @see #valueOf(java.lang.String)
 * @see #clone()
 * @see #hashCode()
 * @see #intValue()
 * @see #longValue()
 * @see #floatValue()
 * @see #doubleValue()
 * @see #equals(java.lang.Object)
 * @see #greaterThan(java.lang.Object)
 * @see #toString()
 */
 protected long unsignedValue;

/**
 * Constructs a new object that represents the specified primitive
 * unsigned <CODE>long</CODE> value.
 **
 * @param unsignedValue
 * the unsigned value to be wrapped.
 **
 * @see #valueOf(java.lang.String)
 * @see #clone()
 * @see #intValue()
 * @see #longValue()
 * @see #equals(java.lang.Object)
 * @see #greaterThan(java.lang.Object)
 * @see #toString()
 */
 public UnsignedLong(long unsignedValue)
 {
  this.unsignedValue = unsignedValue;
 }

/**
 * Converts <CODE>long</CODE> value into its hash code value.
 **
 * The bits of the specified value are mixed in a particular order
 * to produce a single <CODE>int</CODE> 'hash' value.
 **
 * @param value
 * the signed/unsigned value to be hashed.
 * @return
 * the resulting hash code value.
 **
 * @see #hashCode()
 * @see #intValue()
 **
 * @since 1.1
 */
 public static final int hashCode(long value)
 {
  return (int)((value >> (JavaConsts.INT_SIZE - 1)) >> 1) ^
   (int)value;
 }

/**
 * Tests whether the first specified unsigned integer is greater
 * than the second one.
 **
 * @param unsignedA
 * the first compared unsigned value.
 * @param unsignedB
 * the second compared unsigned value.
 * @return
 * <CODE>true</CODE> if and only if <VAR>unsignedA</VAR> is greater
 * than <VAR>unsignedB</VAR> (in the unsigned manner).
 **
 * @see #greaterOrEqual(long, long)
 * @see #compare(long, long)
 * @see #min(long, long)
 * @see #max(long, long)
 * @see #median(long, long, long)
 * @see #greaterThan(java.lang.Object)
 */
 public static final boolean greater(long unsignedA, long unsignedB)
 {
  if ((unsignedA ^ unsignedB) >= 0L)
   unsignedA = unsignedB - unsignedA;
  return unsignedA < 0L;
 }

/**
 * Tests whether the first specified unsigned integer is greater or
 * equal to the second one.
 **
 * @param unsignedA
 * the first compared unsigned value.
 * @param unsignedB
 * the second compared unsigned value.
 * @return
 * <CODE>true</CODE> if and only if <VAR>unsignedA</VAR> is not less
 * than <VAR>unsignedB</VAR> (in the unsigned manner).
 **
 * @see #greater(long, long)
 * @see #compare(long, long)
 * @see #min(long, long)
 * @see #max(long, long)
 * @see #median(long, long, long)
 * @see #equals(java.lang.Object)
 * @see #greaterThan(java.lang.Object)
 */
 public static final boolean greaterOrEqual(long unsignedA,
         long unsignedB)
 {
  if ((unsignedA ^ unsignedB) >= 0L)
   unsignedB = unsignedA - unsignedB;
  return unsignedB >= 0L;
 }

/**
 * Compares two given unsigned integer values.
 **
 * This method returns a signed integer indicating
 * 'less-equal-greater' relation between the specified
 * <CODE>long</CODE> values.
 **
 * @param unsignedA
 * the first compared unsigned value.
 * @param unsignedB
 * the second compared unsigned value.
 * @return
 * a negative integer, zero, or a positive integer as
 * <VAR>valueA</VAR> is less than, equal to, or greater than
 * <VAR>valueB</VAR> (in the unsigned manner).
 **
 * @see #signedCompare(long, long)
 * @see #greater(long, long)
 * @see #greaterOrEqual(long, long)
 * @see #min(long, long)
 * @see #max(long, long)
 * @see #median(long, long, long)
 * @see #equals(java.lang.Object)
 * @see #greaterThan(java.lang.Object)
 */
 public static final int compare(long unsignedA, long unsignedB)
 {
  int cmp = 0;
  if ((unsignedA ^ unsignedB) < 0L ||
      (unsignedA = unsignedB - unsignedA) != 0L)
  {
   cmp = -1;
   if (unsignedA < 0L)
    cmp = 1;
  }
  return cmp;
 }

/**
 * Compares two given (signed) integer values.
 **
 * This method returns a signed integer indicating
 * 'less-equal-greater' relation between the specified
 * <CODE>long</CODE> values.
 **
 * @param valueA
 * the first compared (signed) value.
 * @param valueB
 * the second compared (signed) value.
 * @return
 * a negative integer, zero, or a positive integer as
 * <VAR>valueA</VAR> is less than, equal to, or greater than
 * <VAR>valueB</VAR>.
 **
 * @see #compare(long, long)
 * @see #signedMedian(long, long, long)
 **
 * @since 2.0
 */
 public static final int signedCompare(long valueA, long valueB)
 {
  int cmp = 0;
  if (valueA != valueB)
  {
   cmp = -1;
   if (valueA > valueB)
    cmp = 1;
  }
  return cmp;
 }

/**
 * Returns the minimum of two given unsigned integer values.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * <VAR>unsignedA</VAR> if <VAR>unsignedA</VAR> is less (in the
 * unsigned manner) than <VAR>unsignedB</VAR>, else
 * <VAR>unsignedB</VAR>.
 **
 * @see #max(long, long)
 * @see #median(long, long, long)
 * @see #greater(long, long)
 * @see #greaterOrEqual(long, long)
 * @see #compare(long, long)
 */
 public static final long min(long unsignedA, long unsignedB)
 {
  long delta = unsignedB;
  if ((unsignedA ^ unsignedB) >= 0L)
   delta = unsignedA - unsignedB;
  if (delta >= 0L)
   unsignedA = unsignedB;
  return unsignedA;
 }

/**
 * Returns the maximum of two given unsigned integer values.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * <VAR>unsignedA</VAR> if <VAR>unsignedA</VAR> is greater (in the
 * unsigned manner) than <VAR>unsignedB</VAR>, else
 * <VAR>unsignedB</VAR>.
 **
 * @see #min(long, long)
 * @see #median(long, long, long)
 * @see #greater(long, long)
 * @see #greaterOrEqual(long, long)
 * @see #compare(long, long)
 */
 public static final long max(long unsignedA, long unsignedB)
 {
  long delta = unsignedB;
  if ((unsignedA ^ unsignedB) >= 0L)
   delta = unsignedA - unsignedB;
  if (delta >= 0L)
   unsignedB = unsignedA;
  return unsignedB;
 }

/**
 * Returns the 'median' (middle value) of three given unsigned
 * integer values.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @param unsignedC
 * the third unsigned value.
 * @return
 * <VAR>unsignedA</VAR>, <VAR>unsignedB</VAR> or
 * <VAR>unsignedC</VAR> according to which one of these values is
 * between (in the unsigned manner) the other values.
 **
 * @see #signedMedian(long, long, long)
 * @see #min(long, long)
 * @see #max(long, long)
 * @see #greater(long, long)
 * @see #greaterOrEqual(long, long)
 * @see #compare(long, long)
 **
 * @since 1.1
 */
 public static final long median(long unsignedA, long unsignedB,
         long unsignedC)
 {
  long delta;
  if (((delta = unsignedA) ^ unsignedB) >= 0L)
   delta = unsignedB - unsignedA;
  if (delta >= 0L)
  {
   delta = unsignedA;
   unsignedA = unsignedB;
   unsignedB = delta;
  }
  if (((delta = unsignedC) ^ unsignedA) >= 0L)
   delta = unsignedA - unsignedC;
  if (delta >= 0L)
  {
   if (((unsignedA = unsignedB) ^ unsignedC) >= 0L)
    unsignedB = unsignedC - unsignedA;
   if (unsignedB >= 0L)
    unsignedA = unsignedC;
  }
  return unsignedA;
 }

/**
 * Returns the 'median' (middle value) of three given (signed)
 * integer values.
 **
 * @param valueA
 * the first (signed) value.
 * @param valueB
 * the second (signed) value.
 * @param valueC
 * the third (signed) value.
 * @return
 * <VAR>valueA</VAR>, <VAR>valueB</VAR> or <VAR>valueC</VAR>
 * according to which one of these values is between the other
 * values.
 **
 * @see #median(long, long, long)
 * @see #signedCompare(long, long)
 **
 * @since 1.1
 */
 public static final long signedMedian(long valueA, long valueB,
         long valueC)
 {
  if (valueA <= valueB)
  {
   long delta = valueA;
   valueA = valueB;
   valueB = delta;
  }
  if (valueA >= valueC && (valueA = valueB) <= valueC)
   valueA = valueC;
  return valueA;
 }

/**
 * Returns the highest half of 'long' (full) product of two given
 * unsigned integers.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the (unsigned) top half of <VAR>unsignedA</VAR> multiplied by
 * <VAR>unsignedB</VAR>.
 **
 * @see #divHigh(long, long, long)
 * @see #mulLow(long, long)
 * @see #mulDiv(long, long, long, boolean)
 * @see #mulMod(long, long, long)
 * @see #power(long, int)
 * @see #factorial(int)
 */
 public static final long mulHigh(long unsignedA, long unsignedB)
 {
  long highA = unsignedA >>> (JavaConsts.LONG_SIZE >> 1);
  long highB = unsignedB >>> (JavaConsts.LONG_SIZE >> 1);
  unsignedA &= ~(-1L << (JavaConsts.LONG_SIZE >> 1));
  unsignedB &= ~(-1L << (JavaConsts.LONG_SIZE >> 1));
  unsignedA = ((unsignedA * unsignedB) >>>
   (JavaConsts.LONG_SIZE >> 1)) + unsignedA * highB;
  unsignedA += unsignedB *= highA;
  highA = highA * highB +
   (unsignedA >>> (JavaConsts.LONG_SIZE >> 1));
  if ((unsignedA ^ unsignedB) >= 0L)
   unsignedB = unsignedA - unsignedB;
  if (unsignedB < 0L)
   highA -= -1L << (JavaConsts.LONG_SIZE >> 1);
  return highA;
 }

/**
 * Returns the lowest half of 'long' (full) product of two given
 * unsigned integers.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the (unsigned) bottom half of <VAR>unsignedA</VAR> multiplied by
 * <VAR>unsignedB</VAR>.
 **
 * @see #divLow(long, long, long)
 * @see #remLow(long, long, long)
 * @see #mulHigh(long, long)
 * @see #mulDiv(long, long, long, boolean)
 * @see #mulMod(long, long, long)
 * @see #power(long, int)
 * @see #factorial(int)
 */
 public static final long mulLow(long unsignedA, long unsignedB)
 {
  return unsignedA * unsignedB;
 }

/**
 * Returns the result of integer division of one given unsigned
 * value by another one.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the unsigned result of <VAR>unsignedA</VAR> divided by
 * <VAR>unsignedB</VAR>.
 * @exception ArithmeticException
 * if <VAR>unsignedB</VAR> is <CODE>0</CODE>.
 **
 * @see #mulLow(long, long)
 * @see #rem(long, long)
 * @see #divHigh(long, long, long)
 * @see #divLow(long, long, long)
 * @see #mulDiv(long, long, long, boolean)
 * @see #gcd(long, long)
 * @see #inverseMod(long, long)
 */
 public static final long div(long unsignedA, long unsignedB)
  throws ArithmeticException
 {
  long unsignedRes = 0L;
  if (unsignedA >= 0L)
  {
   if (unsignedB >= 0L)
    unsignedRes = unsignedA / unsignedB;
  }
   else if (unsignedB >= 0L && (unsignedA -= (unsignedRes =
            ((unsignedA >>> 1) / unsignedB) << 1) *
            unsignedB) < 0L || unsignedA >= unsignedB)
    unsignedRes++;
  return unsignedRes;
 }

/**
 * Returns the highest half of the result of integer division of
 * join of two given halves of unsigned value by another one.
 **
 * The result is the same as of
 * <CODE>div(unsignedHighA, unsignedB)</CODE>.
 **
 * @param unsignedHighA
 * the top half of the first unsigned value.
 * @param unsignedLowA
 * the bottom half of the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the top half of the unsigned result of joined
 * <VAR>unsignedHighA</VAR>:<VAR>unsignedLowA</VAR> divided by
 * <VAR>unsignedB</VAR>.
 * @exception ArithmeticException
 * if <VAR>unsignedB</VAR> is <CODE>0</CODE>.
 **
 * @see #divLow(long, long, long)
 * @see #mulHigh(long, long)
 * @see #mulLow(long, long)
 * @see #div(long, long)
 * @see #mulDiv(long, long, long, boolean)
 */
 public static final long divHigh(long unsignedHighA,
         long unsignedLowA, long unsignedB)
  throws ArithmeticException
 {
  return div(unsignedHighA, unsignedB);
 }

/**
 * Returns the lowest half of the result of integer division of join
 * of two given halves of unsigned value by another one.
 **
 * @param unsignedHighA
 * the top half of the first unsigned value.
 * @param unsignedLowA
 * the bottom half of the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the bottom half of the unsigned result of joined
 * <VAR>unsignedHighA</VAR>:<VAR>unsignedLowA</VAR> divided by
 * <VAR>unsignedB</VAR>.
 * @exception ArithmeticException
 * if <VAR>unsignedB</VAR> is <CODE>0</CODE>.
 **
 * @see #divHigh(long, long, long)
 * @see #remLow(long, long, long)
 * @see #mulHigh(long, long)
 * @see #mulLow(long, long)
 * @see #div(long, long)
 * @see #mulDiv(long, long, long, boolean)
 * @see #gcd(long, long)
 */
 public static final long divLow(long unsignedHighA,
         long unsignedLowA, long unsignedB)
  throws ArithmeticException
 {
  if ((unsignedHighA = rem(unsignedHighA, unsignedB)) == 0L)
   return div(unsignedLowA, unsignedB);
  long unsignedRes;
  if ((unsignedB & (-1L << (JavaConsts.LONG_SIZE >> 1))) == 0L)
  {
   unsignedRes = div(unsignedHighA =
    (unsignedHighA << (JavaConsts.LONG_SIZE >> 1)) |
    (unsignedLowA >>> (JavaConsts.LONG_SIZE >> 1)), unsignedB);
   return (unsignedRes << (JavaConsts.LONG_SIZE >> 1)) |
    div(((unsignedHighA - unsignedRes * unsignedB) <<
    (JavaConsts.LONG_SIZE >> 1)) | unsignedLowA &
    ~(-1L << (JavaConsts.LONG_SIZE >> 1)), unsignedB);
  }
  if ((unsignedB & ~(-1L << (JavaConsts.LONG_SIZE >> 1))) == 0L)
  {
   unsignedB >>>= JavaConsts.LONG_SIZE >> 1;
   return (div(unsignedHighA, unsignedB) <<
    (JavaConsts.LONG_SIZE >> 1)) | div((rem(unsignedHighA,
    unsignedB) << (JavaConsts.LONG_SIZE >> 1)) |
    (unsignedLowA >>> (JavaConsts.LONG_SIZE >> 1)), unsignedB);
  }
  unsignedRes = 0L;
  for (int shift = JavaConsts.LONG_SIZE;
       shift-- > 0; unsignedLowA <<= 1)
  {
   long oldHighA = unsignedHighA;
   unsignedHighA <<= 1;
   if (unsignedLowA < 0L)
    unsignedHighA++;
   unsignedRes <<= 1;
   if (oldHighA < 0L || ((unsignedHighA ^ unsignedB) >= 0L ?
       unsignedHighA - unsignedB : unsignedB) >= 0L)
   {
    unsignedHighA -= unsignedB;
    unsignedRes++;
   }
  }
  return unsignedRes;
 }

/**
 * Returns the remainder of integer division of one given unsigned
 * value by another one.
 **
 * The result is the same as of
 * <CODE>(unsignedA - div(unsignedA, unsignedB) * unsignedB)</CODE>.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the unsigned remainder when <VAR>unsignedA</VAR> is divided by
 * <VAR>unsignedB</VAR>.
 * @exception ArithmeticException
 * if <VAR>unsignedB</VAR> is <CODE>0</CODE>.
 **
 * @see #mulLow(long, long)
 * @see #div(long, long)
 * @see #remLow(long, long, long)
 * @see #inverseMod(long, long)
 */
 public static final long rem(long unsignedA, long unsignedB)
  throws ArithmeticException
 {
  if (unsignedA >= 0L)
  {
   if (unsignedB >= 0L)
    unsignedA %= unsignedB;
  }
   else if (unsignedB >= 0L && (unsignedA = (((unsignedA >>> 1) %
            unsignedB) << 1) | unsignedA & 1L) < 0L ||
            unsignedA >= unsignedB)
    unsignedA -= unsignedB;
  return unsignedA;
 }

/**
 * Returns the remainder of integer division of join of two given
 * halves of unsigned value by another one.
 **
 * @param unsignedHighA
 * the top half of the first unsigned value.
 * @param unsignedLowA
 * the bottom half of the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the unsigned remainder when joined
 * <VAR>unsignedHighA</VAR>:<VAR>unsignedLowA</VAR> is divided by
 * <VAR>unsignedB</VAR>.
 * @exception ArithmeticException
 * if <VAR>unsignedB</VAR> is <CODE>0</CODE>.
 **
 * @see #rem(long, long)
 * @see #divLow(long, long, long)
 * @see #mulHigh(long, long)
 * @see #mulLow(long, long)
 * @see #mulMod(long, long, long)
 */
 public static final long remLow(long unsignedHighA,
         long unsignedLowA, long unsignedB)
  throws ArithmeticException
 {
  if ((unsignedHighA = rem(unsignedHighA, unsignedB)) == 0L)
   return rem(unsignedLowA, unsignedB);
  if ((unsignedB & (-1L << (JavaConsts.LONG_SIZE >> 1))) == 0L)
   return rem((rem((unsignedHighA << (JavaConsts.LONG_SIZE >> 1)) |
    (unsignedLowA >>> (JavaConsts.LONG_SIZE >> 1)), unsignedB) <<
    (JavaConsts.LONG_SIZE >> 1)) | unsignedLowA &
    ~(-1L << (JavaConsts.LONG_SIZE >> 1)), unsignedB);
  if ((unsignedB & ~(-1L << (JavaConsts.LONG_SIZE >> 1))) == 0L)
  {
   unsignedB >>>= JavaConsts.LONG_SIZE >> 1;
   return (rem((rem(unsignedHighA, unsignedB) <<
    (JavaConsts.LONG_SIZE >> 1)) | (unsignedLowA >>>
    (JavaConsts.LONG_SIZE >> 1)), unsignedB) <<
    (JavaConsts.LONG_SIZE >> 1)) |
    unsignedLowA & ~(-1L << (JavaConsts.LONG_SIZE >> 1));
  }
  for (int shift = JavaConsts.LONG_SIZE;
       shift-- > 0; unsignedLowA <<= 1)
  {
   long oldHighA = unsignedHighA;
   unsignedHighA <<= 1;
   if (unsignedLowA < 0L)
    unsignedHighA++;
   if (oldHighA < 0L || ((unsignedHighA ^ unsignedB) >= 0L ?
       unsignedHighA - unsignedB : unsignedB) >= 0L)
    unsignedHighA -= unsignedB;
  }
  return unsignedHighA;
 }

/**
 * Returns a given value involved to the power of the specified
 * degree.
 **
 * Overflow is not checked. If <VAR>degree</VAR> is negative then
 * <CODE>0</CODE> is returned. Important notes: <VAR>value</VAR> may
 * be signed or unsigned.
 **
 * @param value
 * the signed/unsigned value to be involved to the power.
 * @param degree
 * the degree of the power.
 * @return
 * the signed/unsigned result of involving <VAR>value</VAR> to the
 * power of <VAR>degree</VAR>.
 **
 * @see #mulLow(long, long)
 * @see #binLog(long)
 * @see #factorial(int)
 **
 * @since 1.1
 */
 public static final long power(long value, int degree)
 {
  long res = 0L;
  if (degree >= 0)
   if (((2L - value) | value) < 0L)
   {
    for (res = 1L; degree > 0; value *= value, degree >>= 1)
     if ((degree & 1) != 0)
      res *= value;
   }
    else if (degree == 0 || value != 0L)
    {
     res = 1L;
     if (value == 2L)
      if (degree < JavaConsts.LONG_SIZE)
       res <<= degree;
       else res = 0L;
    }
  return res;
 }

/**
 * Returns the integer part of binary logarithm of a given unsigned
 * value.
 **
 * If <VAR>unsignedValue</VAR> is zero then <CODE>-1</CODE> is
 * returned, else the result is in the range from <CODE>0</CODE> to
 * <CODE>(JavaConsts LONG_SIZE - 1)</CODE>, inclusive.
 **
 * @param unsignedValue
 * the unsigned argument of the logarithm.
 * @return
 * the result of logarithm computation for <VAR>unsignedValue</VAR>.
 **
 * @see #power(long, int)
 * @see #factorial(int)
 **
 * @since 1.2
 */
 public static final int binLog(long unsignedValue)
 {
  int res = -1;
  while ((unsignedValue & ~JavaConsts.INT_LMASK) != 0L)
  {
   unsignedValue >>>= JavaConsts.INT_SIZE;
   res += JavaConsts.INT_SIZE;
  }
  for (int value = (int)unsignedValue;
       value != 0; value >>>= 1, res++);
  return res;
 }

/**
 * Computes the factorial of a given value.
 **
 * Overflow is not checked. If <VAR>value</VAR> is negative or zero
 * then <CODE>0</CODE> is returned.
 **
 * @param value
 * the factorial argument.
 * @return
 * the unsigned result of the factorial computation for
 * <VAR>value</VAR>.
 **
 * @see #mulLow(long, long)
 * @see #power(long, int)
 * @see #binLog(long)
 **
 * @since 1.1
 */
 public static final long factorial(int value)
 {
  long unsignedRes = 0L;
  if (value > 0)
   for (unsignedRes = 1L; value > 1; unsignedRes *= value--);
  return unsignedRes;
 }

/**
 * Computes the greatest 'common divisor' (gcd) of two given
 * unsigned non-zero values.
 **
 * Important notes: zero value (for any argument or result) is
 * treated as the largest unsigned <CODE>long</CODE> value plus one.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the unsigned greatest common divisor of both <VAR>unsignedA</VAR>
 * and <VAR>unsignedB</VAR>.
 **
 * @see #inverseMod(long, long)
 * @see #mulMod(long, long, long)
 * @see #mulDiv(long, long, long, boolean)
 * @see #binLog(long)
 **
 * @since 1.1
 */
 public static final long gcd(long unsignedA, long unsignedB)
 {
  int shift = 0;
  long delta;
  if (unsignedA == 0L || unsignedB == 0L)
   if (((int)unsignedA & 1) == 0)
   {
    unsignedA = ((unsignedA - 1L) >>> 1) + 1L;
    if (((int)unsignedB & 1) == 0)
    {
     shift = 1;
     unsignedB = ((unsignedB - 1L) >>> 1) + 1L;
    }
   }
    else unsignedB = ~(-1L >>> 1);
  if (unsignedA != 1L)
   while (unsignedA != unsignedB)
    if (((int)unsignedA & 1) == 0)
    {
     unsignedA >>>= 1;
     if (((int)unsignedB & 1) == 0)
     {
      shift++;
      unsignedB >>>= 1;
     }
    }
     else if (((int)unsignedB & 1) == 0)
      unsignedB >>>= 1;
      else if ((delta = (unsignedA >>> 1) - (unsignedB >>> 1)) > 0L)
       unsignedA = delta;
       else unsignedB = -delta;
  return unsignedA << shift;
 }

/**
 * Computes the modular 'inversion' of a given unsigned value.
 **
 * It is the inverse operation for modular multiplication. That is,
 * if both <VAR>unsignedA</VAR> and <VAR>unsignedMax</VAR> are
 * non-zero and <CODE>gcd(unsignedA, unsignedMax + 1) == 1</CODE>
 * then <CODE>mulMod(result, unsignedA, unsignedMax)</CODE> is
 * <CODE>1</CODE>. The result of modular inversion is in the range
 * from <CODE>1</CODE> to <CODE>max(unsignedMax, 1)</CODE>,
 * inclusive.
 **
 * @param unsignedA
 * the unsigned value to inverse.
 * @param unsignedMax
 * the unsigned maximum for modular multiplication.
 * @return
 * the unsigned result (not zero) of modular inversion of
 * <VAR>unsignedA</VAR> with respect to <VAR>unsignedMax</VAR>.
 **
 * @see #mulMod(long, long, long)
 * @see #gcd(long, long)
 * @see #div(long, long)
 * @see #rem(long, long)
 **
 * @since 1.1
 */
 public static final long inverseMod(long unsignedA,
         long unsignedMax)
 {
  long unsignedB, unsignedRes = 1L;
  if (unsignedA < 0L)
  {
   if (++unsignedMax > 0L && (unsignedA = (((unsignedA >>> 1) %
       unsignedMax) << 1) | unsignedA & 1L) < 0L ||
       unsignedA >= unsignedMax)
    unsignedA -= unsignedMax;
  }
   else if (++unsignedMax > 0L)
    unsignedA %= unsignedMax;
  if (unsignedA != 0L &&
      (unsignedB = unsignedMax - unsignedA) != unsignedA)
  {
   long unsignedQ = 0L, unsignedDiv, max = unsignedMax;
   if (unsignedA < 0L)
   {
    unsignedMax = unsignedA;
    unsignedRes = -1L;
    unsignedB = unsignedMax - (unsignedA = unsignedB);
    unsignedQ = 1L;
   }
   if ((unsignedB = unsignedMax - (unsignedDiv =
       (((unsignedB >>> 1) / unsignedA) << 1) + 1L) *
       unsignedA) < 0L || unsignedB >= unsignedA)
   {
    unsignedB -= unsignedA;
    unsignedDiv++;
   }
   while (unsignedB > 0L)
   {
    unsignedMax = unsignedA;
    unsignedA = unsignedB;
    unsignedRes = unsignedQ - (unsignedB =
     unsignedRes) * unsignedDiv;
    unsignedQ = unsignedB;
    unsignedB = unsignedMax - (unsignedDiv =
     unsignedMax / unsignedA) * unsignedA;
   }
   if (unsignedRes < 0L)
    unsignedRes += max;
  }
  return unsignedRes;
 }

/**
 * Computes the modular 'multiplication' of two given unsigned
 * values.
 **
 * This method returns the remainder of the result of integer
 * division of 'long' (full) product of <VAR>unsignedA</VAR> and
 * <VAR>unsignedB</VAR> by (<VAR>unsignedMax</VAR> plus one). The
 * result is in the range from <CODE>0</CODE> to
 * <VAR>unsignedMax</VAR>, inclusive.
 **
 * @param unsignedA
 * the first unsigned value to multiply.
 * @param unsignedB
 * the second unsigned value to multiply.
 * @param unsignedMax
 * the unsigned maximum value for the result to have.
 * @return
 * the unsigned result of modular multiplication of
 * <VAR>unsignedA</VAR> and <VAR>unsignedB</VAR> with respect to
 * <VAR>unsignedMax</VAR>.
 **
 * @see #inverseMod(long, long)
 * @see #gcd(long, long)
 * @see #mulDiv(long, long, long, boolean)
 * @see #mulHigh(long, long)
 * @see #remLow(long, long, long)
 **
 * @since 1.1
 */
 public static final long mulMod(long unsignedA, long unsignedB,
         long unsignedMax)
 {
  long unsignedRes = 0L;
  if (unsignedA != 0L && unsignedB != 0L)
  {
   unsignedRes = unsignedA * unsignedB;
   if (++unsignedMax != 0L)
    unsignedRes = remLow(mulHigh(unsignedA, unsignedB),
     unsignedRes, unsignedMax);
  }
  return unsignedRes;
 }

/**
 * Returns the lowest half of the result of integer division of
 * 'long' (full) product of the first two given unsigned non-zero
 * values by the third given unsigned non-zero value.
 **
 * Important notes: zero value (for any argument) is treated as
 * the largest unsigned <CODE>long</CODE> value plus one.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @param unsignedC
 * the unsigned divisor value.
 * @param roundUp
 * <CODE>true</CODE> if the result of division is rounded upwards
 * (to the nearest greater integer value), else it is rounded
 * towards zero.
 * @return
 * the bottom half of the unsigned result of <VAR>unsignedA</VAR>
 * multiplied by <VAR>unsignedB</VAR> and divided by
 * <VAR>unsignedC</VAR>.
 **
 * @see #mulHigh(long, long)
 * @see #mulLow(long, long)
 * @see #divLow(long, long, long)
 * @see #gcd(long, long)
 * @see #mulMod(long, long, long)
 * @see #inverseMod(long, long)
 */
 public static final long mulDiv(long unsignedA, long unsignedB,
         long unsignedC, boolean roundUp)
 {
  long unsignedHigh;
  if (unsignedA == 0L || unsignedA == unsignedC)
  {
   unsignedHigh = unsignedB;
   unsignedB = unsignedA;
   unsignedA = unsignedHigh;
  }
  if (unsignedB != unsignedC)
  {
   if (unsignedB != 0L)
   {
    unsignedHigh = mulHigh(unsignedA, unsignedB);
    unsignedB *= unsignedA;
    unsignedA = unsignedHigh;
   }
   if (roundUp)
   {
    if (unsignedB == 0L)
     unsignedA--;
    unsignedB--;
   }
    else if ((unsignedA | unsignedB) == 0L)
    {
     unsignedA = -1L;
     unsignedB = -unsignedC;
     roundUp = true;
    }
   if (unsignedC != 0L)
    unsignedA = divLow(unsignedA, unsignedB, unsignedC);
   if (roundUp)
    unsignedA++;
  }
  return unsignedA;
 }

/**
 * Puts a given value into the specified byte array.
 **
 * This method splits <VAR>unsignedValue</VAR> to separate bytes and
 * puts them into <VAR>bytes</VAR> array sequentially (the last put
 * byte is the lowest of <VAR>unsignedValue</VAR>), starting at
 * <VAR>offset</VAR>. Negative <VAR>len</VAR> is treated as zero. If
 * an exception is thrown then <VAR>bytes</VAR> remains changed.
 * Else <VAR>bytes</VAR> content is altered. Important notes: if
 * <VAR>len</VAR> is greater than
 * <CODE>JavaConsts LONG_LENGTH</CODE> then <VAR>unsignedValue</VAR>
 * is zero-extended.
 **
 * @param bytes
 * the byte array (must be non-<CODE>null</CODE>) to put to.
 * @param offset
 * the first array index (must be in the range) to put at.
 * @param unsignedValue
 * the value, containing the bytes (in its lowest part) to be put.
 * @param len
 * the amount of bytes to put.
 * @exception NullPointerException
 * if <VAR>bytes</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>bytes</VAR> minus
 * <VAR>len</VAR>).
 **
 * @see #toByteArray(long, int)
 * @see #getFromByteArray(byte[], int, int)
 * @see #putToIntelArray(byte[], int, long, int)
 * @see #writeBits(java.io.OutputStream, long, int)
 **
 * @since 1.1
 */
 public static final void putToByteArray(byte[] bytes, int offset,
         long unsignedValue, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   byte temp;
   for (temp = bytes[offset + len - 1];
        len > JavaConsts.LONG_LENGTH; bytes[offset++] = 0, len--);
   len *= JavaConsts.BYTE_SIZE;
   while ((len -= JavaConsts.BYTE_SIZE) >= 0)
    bytes[offset++] = (byte)(unsignedValue >>> len);
  }
  len = bytes.length;
 }

/**
 * Converts a given value into a byte array.
 **
 * This method splits <VAR>unsignedValue</VAR> to separate bytes and
 * sequentially puts them into a newly created byte array (the last
 * put byte is the lowest of <VAR>unsignedValue</VAR>) of
 * <VAR>len</VAR> length. Negative <VAR>len</VAR> is treated as
 * zero. Important notes: if <VAR>len</VAR> is greater than
 * <CODE>JavaConsts LONG_LENGTH</CODE> then <VAR>unsignedValue</VAR>
 * is zero-extended.
 **
 * @param unsignedValue
 * the value, containing the bytes (in its lowest part) to be put.
 * @param len
 * the amount of bytes to put (<CODE>length</CODE> of the array to
 * create).
 * @return
 * a newly created byte array (not <CODE>null</CODE>, with
 * <CODE>length</CODE> the same as non-negative <VAR>len</VAR>),
 * containing the bytes of <VAR>unsignedValue</VAR>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #putToByteArray(byte[], int, long, int)
 * @see #getFromByteArray(byte[], int, int)
 * @see #toIntelArray(long, int)
 * @see #writeBits(java.io.OutputStream, long, int)
 **
 * @since 1.1
 */
 public static final byte[] toByteArray(long unsignedValue, int len)
 {
  if (len <= 0)
   len = 0;
  byte[] bytes = new byte[len];
  putToByteArray(bytes, 0, unsignedValue, len);
  return bytes;
 }

/**
 * Converts the specified region of a given byte array into an
 * unsigned value.
 **
 * This method gets the elements of <VAR>bytes</VAR> array
 * sequentially starting at <VAR>offset</VAR> and joins them into an
 * unsigned value (the last got byte is the lowest of the result).
 * Negative <VAR>len</VAR> is treated as zero. <VAR>bytes</VAR>
 * content is not modified.
 **
 * @param bytes
 * the byte array (must be non-<CODE>null</CODE>) to get from.
 * @param offset
 * the first array index (must be in the range) to get at.
 * @param len
 * the amount of bytes to get.
 * @return
 * an unsigned value, containing the got bytes (in its lowest part).
 * @exception NullPointerException
 * if <VAR>bytes</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>bytes</VAR> minus
 * <VAR>len</VAR>).
 **
 * @see #putToByteArray(byte[], int, long, int)
 * @see #toByteArray(long, int)
 * @see #getFromIntelArray(byte[], int, int)
 * @see #readBits(java.io.InputStream, int)
 **
 * @since 1.1
 */
 public static final long getFromByteArray(byte[] bytes,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  long unsignedValue = 0L;
  if (len > 0)
   do
   {
    unsignedValue = (unsignedValue << JavaConsts.BYTE_SIZE) |
     bytes[offset++] & JavaConsts.BYTE_MASK;
   } while (--len > 0);
  len = bytes.length;
  return unsignedValue;
 }

/**
 * Puts a given value into the specified byte array in the reversed
 * order.
 **
 * This method splits <VAR>unsignedValue</VAR> to separate bytes and
 * puts them into <VAR>bytes</VAR> array sequentially but in the
 * reversed ('Intel') order (the first put byte is the lowest of
 * <VAR>unsignedValue</VAR>), starting at <VAR>offset</VAR> (and
 * moving forward). Negative <VAR>len</VAR> is treated as zero. If
 * an exception is thrown then <VAR>bytes</VAR> remains changed.
 * Else <VAR>bytes</VAR> content is altered. Important notes: if
 * <VAR>len</VAR> is greater than
 * <CODE>JavaConsts LONG_LENGTH</CODE> then <VAR>unsignedValue</VAR>
 * is zero-extended.
 **
 * @param bytes
 * the byte array (must be non-<CODE>null</CODE>) to put to.
 * @param offset
 * the first array index (must be in the range) to put at.
 * @param unsignedValue
 * the value, containing the bytes (in its lowest part) to be put.
 * @param len
 * the amount of bytes to put.
 * @exception NullPointerException
 * if <VAR>bytes</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>bytes</VAR> minus
 * <VAR>len</VAR>).
 **
 * @see #toIntelArray(long, int)
 * @see #getFromIntelArray(byte[], int, int)
 * @see #putToByteArray(byte[], int, long, int)
 **
 * @since 2.0
 */
 public static final void putToIntelArray(byte[] bytes, int offset,
         long unsignedValue, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   byte temp;
   temp = bytes[offset + len - 1];
   do
   {
    bytes[offset++] = (byte)unsignedValue;
    if (--len <= 0)
     break;
    unsignedValue >>>= JavaConsts.BYTE_SIZE;
   } while (true);
  }
  len = bytes.length;
 }

/**
 * Converts a given value in the reversed order into a byte array.
 **
 * This method splits <VAR>unsignedValue</VAR> to separate bytes and
 * sequentially puts them into a newly created byte array but in the
 * reversed ('Intel') order (the first put byte is the lowest of
 * <VAR>unsignedValue</VAR>) of <VAR>len</VAR> length. Negative
 * <VAR>len</VAR> is treated as zero. Important notes: if
 * <VAR>len</VAR> is greater than
 * <CODE>JavaConsts LONG_LENGTH</CODE> then <VAR>unsignedValue</VAR>
 * is zero-extended.
 **
 * @param unsignedValue
 * the value, containing the bytes (in its lowest part) to be put.
 * @param len
 * the amount of bytes to put (<CODE>length</CODE> of the array to
 * create).
 * @return
 * a newly created byte array (not <CODE>null</CODE>, with
 * <CODE>length</CODE> the same as non-negative <VAR>len</VAR>),
 * containing the bytes of <VAR>unsignedValue</VAR>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #putToIntelArray(byte[], int, long, int)
 * @see #getFromIntelArray(byte[], int, int)
 * @see #toByteArray(long, int)
 **
 * @since 2.0
 */
 public static final byte[] toIntelArray(long unsignedValue,
         int len)
 {
  if (len <= 0)
   len = 0;
  byte[] bytes = new byte[len];
  putToIntelArray(bytes, 0, unsignedValue, len);
  return bytes;
 }

/**
 * Converts the specified region of a given byte array into an
 * unsigned value in the reversed order.
 **
 * This method gets the elements of <VAR>bytes</VAR> array
 * sequentially but in the reversed ('Intel') order, starting at
 * <VAR>offset</VAR> (and moving forward) and joins them into an
 * unsigned value (the first got byte is the lowest of the result).
 * Negative <VAR>len</VAR> is treated as zero. <VAR>bytes</VAR>
 * content is not modified.
 **
 * @param bytes
 * the byte array (must be non-<CODE>null</CODE>) to get from.
 * @param offset
 * the first array index (must be in the range) to get at.
 * @param len
 * the amount of bytes to get.
 * @return
 * an unsigned value, containing the got bytes (in its lowest part).
 * @exception NullPointerException
 * if <VAR>bytes</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>bytes</VAR> minus
 * <VAR>len</VAR>).
 **
 * @see #putToIntelArray(byte[], int, long, int)
 * @see #toIntelArray(long, int)
 * @see #getFromByteArray(byte[], int, int)
 **
 * @since 2.0
 */
 public static final long getFromIntelArray(byte[] bytes,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  long unsignedValue = 0;
  if (len > 0)
  {
   byte temp;
   temp = bytes[offset + len - 1];
   if (len >= JavaConsts.LONG_LENGTH)
    len = JavaConsts.LONG_LENGTH;
   int shift = 0;
   do
   {
    unsignedValue |= (long)(bytes[offset++] &
     JavaConsts.BYTE_MASK) << shift;
    if (--len <= 0)
     break;
    shift += JavaConsts.BYTE_SIZE;
   } while (true);
  }
  len = bytes.length;
  return unsignedValue;
 }

/**
 * Writes a group of bits to a given output stream.
 **
 * Negative <VAR>count</VAR> is treated as zero. Important notes: if
 * <VAR>count</VAR> is greater than
 * <CODE>JavaConsts LONG_SIZE</CODE> then the specified value
 * (containing the bits to write) is zero-extended; <VAR>out</VAR>
 * is a byte-oriented stream, so given bits are padded with zeros
 * (on the most significant side); the bits are written starting
 * from the most significant one.
 **
 * @param out
 * the stream (must be non-<CODE>null</CODE>) to write to.
 * @param unsignedValue
 * the unsigned value, containing bits (in its lowest part) to be
 * written.
 * @param count
 * the amount of bits to be written.
 * @exception NullPointerException
 * if <VAR>out</VAR> is <CODE>null</CODE>.
 * @exception IOException
 * if an I/O error occurs.
 **
 * @see #readBits(java.io.InputStream, int)
 **
 * @since 1.1
 */
 public static final void writeBits(OutputStream out,
         long unsignedValue, int count)
  throws NullPointerException, IOException
 {
  out.equals(out);
  if (count > 0)
  {
   int shift;
   if ((shift = count % JavaConsts.BYTE_SIZE) > 0)
    out.write((count -= shift) >= JavaConsts.LONG_SIZE ? 0 :
     (int)(unsignedValue >>> count) & ~(-1 << shift));
   while (count > JavaConsts.LONG_SIZE)
   {
    out.write(0);
    count -= JavaConsts.BYTE_SIZE;
   }
   while ((count -= JavaConsts.BYTE_SIZE) >= 0)
    out.write((int)(unsignedValue >>> count));
  }
 }

/**
 * Reads a group of bits from a given input stream.
 **
 * Negative <VAR>count</VAR> is treated as zero. If the
 * end-of-stream is detected then <CODE>EOFException</CODE>
 * (subclass of <CODE>IOException</CODE>) is thrown. Important
 * notes: if <VAR>count</VAR> is greater than
 * <CODE>JavaConsts LONG_SIZE</CODE> then the result contains only
 * tail (the least significant) read bits portion fit the result;
 * <VAR>in</VAR> is a byte-oriented stream, so padding bits are read
 * but set to zero (on the most significant side); the bits are read
 * starting from the most significant one.
 **
 * @param in
 * the stream (must be non-<CODE>null</CODE>) to read from.
 * @param count
 * the amount of bits to be read.
 * @return
 * an unsigned value, containing read bits (in its lowest part, the
 * highest part of the result is set to zero if <VAR>count</VAR> is
 * less than <CODE>JavaConsts LONG_SIZE</CODE>).
 * @exception NullPointerException
 * if <VAR>in</VAR> is <CODE>null</CODE>.
 * @exception IOException
 * if the end-of-stream has been reached or an I/O error occurs.
 **
 * @see #writeBits(java.io.OutputStream, long, int)
 **
 * @since 1.1
 */
 public static final long readBits(InputStream in, int count)
  throws NullPointerException, IOException
 {
  in.equals(in);
  long unsignedValue = 0L;
  if (count > 0)
  {
   int shift;
   if ((shift = count % JavaConsts.BYTE_SIZE) > 0)
   {
    if ((unsignedValue = in.read()) < 0L)
     throw new EOFException();
    unsignedValue &= ~(-1 << shift);
    count -= shift;
   }
   while ((count -= JavaConsts.BYTE_SIZE) >= 0)
    if ((shift = in.read()) >= 0)
     unsignedValue = shift & JavaConsts.BYTE_MASK |
      (unsignedValue << JavaConsts.BYTE_SIZE);
     else throw new EOFException();
  }
  return unsignedValue;
 }

/**
 * Converts a given signed/unsigned value into its string
 * representation in the specified radix.
 **
 * <VAR>value</VAR> is unsigned only if <VAR>isUnsigned</VAR> and
 * not <VAR>forceSign</VAR>. If <VAR>forceSign</VAR> then result
 * always has a sign (if <VAR>isUnsigned</VAR> then the positive
 * sign is space else '+' character) else result has a sign ('-'
 * character) if <VAR>value</VAR> is negative. The result is
 * left-padded with spaces (before the sign prefix) if
 * <VAR>minLength</VAR> is negative else with '0' characters (after
 * the sign prefix). The absolute value of <VAR>minLength</VAR>
 * specifies the minimal length of the result (anyhow, the result
 * contains at least one digit). The full digits character set is
 * '0' through '9', 'A' through 'Z' and 'a' through 'z'. If the
 * specified radix is invalid (less than two or too big) then it is
 * corrected to the nearest valid one.
 **
 * @param value
 * the signed/unsigned value to be converted.
 * @param isUnsigned
 * <CODE>true</CODE> if <VAR>value</VAR> must be treated as an
 * unsigned value (but only if not <VAR>forceSign</VAR>), else
 * <VAR>value</VAR> is signed.
 * @param forceSign
 * <CODE>true</CODE> if and only if the result must always have a
 * sign (positive or negative).
 * @param radix
 * the radix (any value) to be used as a base of the value string
 * format.
 * @param upperCase
 * <CODE>true</CODE> if and only if (digit) characters in the result
 * are in the upper case.
 * @param minLength
 * the minimal length of the result (absolute value) and pad prefix
 * specifier (if negative then space padding else zero padding).
 * @return
 * the string representation (not <CODE>null</CODE>, with
 * <CODE>length()</CODE> not less than
 * <CODE>max(abs(minLength), 1)</CODE>) of <VAR>value</VAR> in the
 * specified radix.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #toBinaryString(long, int)
 * @see #toOctalString(long, int)
 * @see #toString(long, boolean)
 * @see #toHexString(long, boolean, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 */
 public static final String toString(long value, boolean isUnsigned,
         boolean forceSign, int radix, boolean upperCase,
         int minLength)
 {
  char[] chars;
  int offset, zeroPrefix;
  if (radix <= 2)
   radix = 2;
  if (radix >= ('9' - '0' + 1) + ('Z' - 'A' + 1))
   radix = ('9' - '0' + 1) + ('Z' - 'A' + 1);
  if ((zeroPrefix = minLength) < 0 && (minLength = -minLength) < 0)
   minLength--;
  if ((offset = (JavaConsts.LONG_SIZE + 1) - minLength) > 0)
   minLength = JavaConsts.LONG_SIZE + 1;
   else offset = 0;
  chars = new char[minLength];
  if (forceSign || !isUnsigned)
   if (value < 0L)
   {
    isUnsigned = forceSign = false;
    value = -value;
   }
    else if (!forceSign)
     isUnsigned = true;
  int digit = (int)value;
  if ((digit -= (int)(value =
      ((value >>> 1) / radix) << 1) * radix) >= radix)
  {
   digit -= radix;
   value++;
  }
  do
  {
   if (digit > '9' - '0')
   {
    digit += 'a' - '9' - 1;
    if (upperCase)
     digit -= 'a' - 'A';
   }
   chars[--minLength] = (char)(digit + '0');
   if (value <= 0L)
    break;
   digit = (int)value;
   digit -= (int)(value /= radix) * radix;
  } while (true);
  if (zeroPrefix > 0)
  {
   if ((forceSign || !isUnsigned) && offset + 1 > 0)
    offset++;
   while (offset < minLength)
    chars[--minLength] = '0';
  }
  if (!isUnsigned)
   chars[--minLength] = (char)(forceSign ? '+' : '-');
   else if (forceSign)
    chars[--minLength] = ' ';
  while (offset < minLength)
   chars[--minLength] = ' ';
  return new String(chars, minLength, chars.length - minLength);
 }

/**
 * Converts a given signed/unsigned value into its decimal string
 * representation.
 **
 * @param value
 * the signed/unsigned value to be converted.
 * @param isUnsigned
 * <CODE>true</CODE> if <VAR>value</VAR> must be treated as an
 * unsigned value, else <VAR>value</VAR> is signed.
 * @return
 * the string representation (not <CODE>null</CODE>, with non-zero
 * <CODE>length()</CODE>) of <VAR>value</VAR> in the decimal radix.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #toString(long, boolean, boolean, int, boolean, int)
 * @see #toBinaryString(long, int)
 * @see #toOctalString(long, int)
 * @see #toHexString(long, boolean, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 */
 public static final String toString(long value, boolean isUnsigned)
 {
  int minLength;
  char[] chars = new char[minLength =
   (JavaConsts.LONG_SIZE - 1) / 3 + 2];
  if (!isUnsigned)
   if (value < 0L)
    value = -value;
    else isUnsigned = true;
  int digit = (int)value;
  if ((digit -= (int)(value = ((value >>> 1) /
      ('9' - '0' + 1)) << 1) * ('9' - '0' + 1)) > '9' - '0')
  {
   digit -= '9' - '0' + 1;
   value++;
  }
  do
  {
   chars[--minLength] = (char)(digit + '0');
   if (value <= 0L)
    break;
   digit = (int)value;
   digit -= (int)(value /= '9' - '0' + 1) * ('9' - '0' + 1);
  } while (true);
  if (!isUnsigned)
   chars[--minLength] = '-';
  return new String(chars, minLength, chars.length - minLength);
 }

/**
 * Converts a given unsigned value into its binary string
 * representation.
 **
 * The result is left-padded with spaces if <VAR>minLength</VAR> is
 * negative else with '0' characters. The absolute value of
 * <VAR>minLength</VAR> specifies the minimal length of the result
 * (anyhow, the result contains at least one digit).
 **
 * @param unsignedValue
 * the unsigned value to be converted.
 * @param minLength
 * the minimal length of the result (absolute value) and pad prefix
 * specifier (if negative then space padding else zero padding).
 * @return
 * the string representation (not <CODE>null</CODE>, with
 * <CODE>length()</CODE> not less than
 * <CODE>max(abs(minLength), 1)</CODE>) of <VAR>unsignedValue</VAR>
 * in the binary radix.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #toString(long, boolean, boolean, int, boolean, int)
 * @see #toOctalString(long, int)
 * @see #toString(long, boolean)
 * @see #toHexString(long, boolean, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 */
 public static final String toBinaryString(long unsignedValue,
         int minLength)
 {
  char[] chars;
  int offset;
  char prefix = '0';
  if (minLength < 0)
   if ((minLength = -minLength) < 0)
    minLength--;
    else prefix = ' ';
  if ((offset = JavaConsts.LONG_SIZE - minLength) > 0)
   minLength = JavaConsts.LONG_SIZE;
   else offset = 0;
  chars = new char[minLength];
  do
  {
   chars[--minLength] = (char)(((int)unsignedValue & 1) + '0');
  } while ((unsignedValue >>>= 1) != 0L);
  while (offset < minLength)
   chars[--minLength] = prefix;
  return new String(chars, minLength, chars.length - minLength);
 }

/**
 * Converts a given unsigned value into its octal string
 * representation.
 **
 * The result is left-padded with spaces if <VAR>minLength</VAR> is
 * negative else with '0' characters. The absolute value of
 * <VAR>minLength</VAR> specifies the minimal length of the result
 * (anyhow, the result contains at least one digit).
 **
 * @param unsignedValue
 * the unsigned value to be converted.
 * @param minLength
 * the minimal length of the result (absolute value) and pad prefix
 * specifier (if negative then space padding else zero padding).
 * @return
 * the string representation (not <CODE>null</CODE>, with
 * <CODE>length()</CODE> not less than
 * <CODE>max(abs(minLength), 1)</CODE>) of <VAR>unsignedValue</VAR>
 * in the octal radix.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #toString(long, boolean, boolean, int, boolean, int)
 * @see #toBinaryString(long, int)
 * @see #toString(long, boolean)
 * @see #toHexString(long, boolean, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 */
 public static final String toOctalString(long unsignedValue,
         int minLength)
 {
  char[] chars;
  int offset;
  char prefix = '0';
  if (minLength < 0)
   if ((minLength = -minLength) < 0)
    minLength--;
    else prefix = ' ';
  if ((offset = ((JavaConsts.LONG_SIZE - 1) / 3 + 1) -
      minLength) > 0)
   minLength = (JavaConsts.LONG_SIZE - 1) / 3 + 1;
   else offset = 0;
  chars = new char[minLength];
  do
  {
   chars[--minLength] =
    (char)(((int)unsignedValue & ((1 << 3) - 1)) + '0');
  } while ((unsignedValue >>>= 3) != 0L);
  while (offset < minLength)
   chars[--minLength] = prefix;
  return new String(chars, minLength, chars.length - minLength);
 }

/**
 * Converts a given unsigned value into its hexadecimal string
 * representation.
 **
 * The result is left-padded with spaces if <VAR>minLength</VAR> is
 * negative else with '0' characters. The absolute value of
 * <VAR>minLength</VAR> specifies the minimal length of the result
 * (anyhow, the result contains at least one digit).
 **
 * @param unsignedValue
 * the unsigned value to be converted.
 * @param upperCase
 * <CODE>true</CODE> if and only if (digit) characters in the result
 * are in the upper case.
 * @param minLength
 * the minimal length of the result (absolute value) and pad prefix
 * specifier (if negative then space padding else zero padding).
 * @return
 * the string representation (not <CODE>null</CODE>, with
 * <CODE>length()</CODE> not less than
 * <CODE>max(abs(minLength), 1)</CODE>) of <VAR>unsignedValue</VAR>
 * in the hexadecimal radix.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #toString(long, boolean, boolean, int, boolean, int)
 * @see #toBinaryString(long, int)
 * @see #toOctalString(long, int)
 * @see #toString(long, boolean)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 */
 public static final String toHexString(long unsignedValue,
         boolean upperCase, int minLength)
 {
  char[] chars;
  int digit, offset;
  char prefix = '0';
  if (minLength < 0)
   if ((minLength = -minLength) < 0)
    minLength--;
    else prefix = ' ';
  if ((offset = (((JavaConsts.LONG_SIZE - 1) >> 2) + 1) -
      minLength) > 0)
   minLength = ((JavaConsts.LONG_SIZE - 1) >> 2) + 1;
   else offset = 0;
  chars = new char[minLength];
  do
  {
   if ((digit = (int)unsignedValue & ((1 << 4) - 1)) > '9' - '0')
   {
    digit += 'a' - '9' - 1;
    if (upperCase)
     digit -= 'a' - 'A';
   }
   chars[--minLength] = (char)(digit + '0');
  } while ((unsignedValue >>>= 4) != 0L);
  while (offset < minLength)
   chars[--minLength] = prefix;
  return new String(chars, minLength, chars.length - minLength);
 }

/**
 * Parses a given string region as a signed/unsigned integer in the
 * specified radix.
 **
 * Leading spaces (before the sign prefix) are ignored. Sign prefix
 * ('+' or '-') is permitted only if not <VAR>isUnsigned</VAR>. Any
 * leading '0' characters (after the sign prefix or leading spaces)
 * are ignored too. The next characters in the string region must
 * all be digits of the specified radix (the full digits character
 * set is '0' through '9', 'A' through 'Z' and 'a' through 'z').
 * Number overflow is checked properly (<CODE>ParserException</CODE>
 * is thrown if overflow occurs). If the specified radix is invalid
 * (less than two or too big) then it is corrected to the nearest
 * valid one. Important notes: use <CODE>('9' - '0' + 1)</CODE> to
 * parse a decimal number.
 **
 * @param str
 * the string (must be non-<CODE>null</CODE>), which region to
 * parse.
 * @param beginIndex
 * the string region beginning index (must be in the range),
 * inclusive.
 * @param endIndex
 * the string region ending index (must be in the range), exclusive.
 * @param isUnsigned
 * <CODE>true</CODE> if and only if the result must be an unsigned
 * value.
 * @param radix
 * the radix (any value) to be used as a base of the value string
 * format.
 * @return
 * a signed/unsigned integer value represented by <VAR>str</VAR>
 * region.
 * @exception NullPointerException
 * if <VAR>str</VAR> is <CODE>null</CODE>.
 * @exception StringIndexOutOfBoundsException
 * if <VAR>beginIndex</VAR> is negative, or if <VAR>endIndex</VAR>
 * is less than <VAR>beginIndex</VAR> or is greater than
 * <CODE>length()</CODE> of <VAR>str</VAR>.
 * @exception ParserException
 * if <VAR>str</VAR> region cannot be parsed as a signed/unsigned
 * integer (<VAR>error</VAR> is set to <CODE>1</CODE>,
 * <CODE>2</CODE> or <CODE>3</CODE> in the exception, meaning an
 * illegal character is found, number overflow occurs or unexpected
 * end of region is encountered at <VAR>index</VAR>, respectively).
 **
 * @see #toString(long, boolean, boolean, int, boolean, int)
 * @see #toBinaryString(long, int)
 * @see #toOctalString(long, int)
 * @see #toString(long, boolean)
 * @see #toHexString(long, boolean, int)
 * @see #decode(java.lang.String, int, int)
 * @see #valueOf(java.lang.String)
 */
 public static final long parse(String str, int beginIndex,
         int endIndex, boolean isUnsigned, int radix)
  throws NullPointerException, StringIndexOutOfBoundsException,
         ParserException
 {
  long value = str.length();
  if (beginIndex < 0)
   throw new StringIndexOutOfBoundsException(beginIndex);
  if (endIndex < beginIndex || endIndex > (int)value)
   throw new StringIndexOutOfBoundsException(endIndex);
  if (radix <= 2)
   radix = 2;
  if (radix >= ('9' - '0' + 1) + ('Z' - 'A' + 1))
   radix = ('9' - '0' + 1) + ('Z' - 'A' + 1);
  beginIndex--;
  char ch = ' ';
  while (++beginIndex < endIndex &&
         (ch = str.charAt(beginIndex)) == ' ');
  boolean negative = false;
  if (!isUnsigned)
  {
   if (ch == '-')
   {
    negative = true;
    beginIndex++;
   }
   if (ch == '+')
    beginIndex++;
  }
  if (beginIndex < endIndex)
  {
   long limit;
   if (~((int)(limit = ((-1L >>> 1) / radix) << 1) * radix) >=
       radix)
    limit++;
   value = 0L;
   do
   {
    if ((ch = (char)(str.charAt(beginIndex) - '0')) > '9' - '0')
    {
     ch -= 'A' - '0';
     if (ch >= 'a' - 'A')
      ch -= 'a' - 'A';
     if (ch < (char)-('9' - '0' + 1))
      ch += '9' - '0' + 1;
    }
    if (ch >= radix || (value = value * radix + ch) >= 0L &&
        value < ch)
     break;
    if (++beginIndex >= endIndex)
    {
     if (!isUnsigned)
     {
      beginIndex--;
      if (negative)
      {
       if ((value = -value) > 0L)
        break;
      }
       else if (value < 0L)
        break;
     }
     return value;
    }
   } while (((limit - value) | value) >= 0L);
  }
  throw new ParserException(str, beginIndex,
             beginIndex < endIndex ? (ch >= radix ? 1 : 2) : 3);
 }

/**
 * Decodes a given string region as an unsigned
 * octal/decimal/hexadecimal integer value.
 **
 * The following unsigned <CODE>long</CODE> value formats are
 * accepted: decimal, hexadecimal (with '0x', '0X' or '#' prefix)
 * and octal (with '0' prefix). Leading spaces (before the prefix)
 * are ignored. Sign prefix is not permitted. Leading '0' characters
 * (after the prefix) are ignored too.
 **
 * @param str
 * the string (must be non-<CODE>null</CODE>), which region to
 * parse.
 * @param beginIndex
 * the string region beginning index (must be in the range),
 * inclusive.
 * @param endIndex
 * the string region ending index (must be in the range), exclusive.
 * @return
 * an unsigned integer value represented by <VAR>str</VAR> region.
 * @exception NullPointerException
 * if <VAR>str</VAR> is <CODE>null</CODE>.
 * @exception StringIndexOutOfBoundsException
 * if <VAR>beginIndex</VAR> is negative, or if <VAR>endIndex</VAR>
 * is less than <VAR>beginIndex</VAR> or is greater than
 * <CODE>length()</CODE> of <VAR>str</VAR>.
 * @exception ParserException
 * if <VAR>str</VAR> region cannot be parsed (decoded) as an
 * unsigned integer (<VAR>error</VAR> is set to <CODE>1</CODE>,
 * <CODE>2</CODE> or <CODE>3</CODE> in the exception, meaning an
 * illegal character is found, number overflow occurs or unexpected
 * end of region is encountered at <VAR>index</VAR>, respectively).
 **
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #toString(long, boolean, boolean, int, boolean, int)
 * @see #toOctalString(long, int)
 * @see #toString(long, boolean)
 * @see #toHexString(long, boolean, int)
 * @see #valueOf(java.lang.String)
 */
 public static final long decode(String str,
         int beginIndex, int endIndex)
  throws NullPointerException, StringIndexOutOfBoundsException,
         ParserException
 {
  char ch = ' ';
  int radix = '9' - '0' + 1;
  if (((str.length() - endIndex) | beginIndex) >= 0)
  {
   beginIndex--;
   while (++beginIndex < endIndex &&
          (ch = str.charAt(beginIndex)) == ' ');
   if (ch == '#' || ch == '0' && beginIndex + 1 < endIndex)
   {
    radix = 1 << 3;
    if (ch == '#' ||
        (ch = str.charAt(++beginIndex)) == 'X' || ch == 'x')
    {
     radix = 1 << 4;
     if (++beginIndex < endIndex)
      ch = str.charAt(beginIndex);
    }
    if (ch == ' ')
     beginIndex--;
   }
  }
  return parse(str, beginIndex, endIndex, true, radix);
 }

/**
 * Converts a given string to an instance of this class.
 **
 * This method returns a new <CODE>UnsignedLong</CODE> object
 * initialized to the unsigned decimal integer value of the
 * specified string.
 **
 * @param str
 * the string (must be non-<CODE>null</CODE>, representing a valid
 * unsigned decimal integer) to be parsed.
 * @return
 * a newly constructed <CODE>UnsignedLong</CODE> instance (not
 * <CODE>null</CODE>) initialized to the value represented by
 * <VAR>str</VAR>.
 * @exception NullPointerException
 * if <VAR>str</VAR> is <CODE>null</CODE>.
 * @exception ParserException
 * if <VAR>str</VAR> cannot be parsed as an unsigned integer
 * (<VAR>error</VAR> is set to <CODE>1</CODE>, <CODE>2</CODE> or
 * <CODE>3</CODE> in the exception, meaning an illegal character is
 * found, number overflow occurs or unexpected end of string is
 * encountered at <VAR>index</VAR>, respectively).
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see UnsignedLong#UnsignedLong(long)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 * @see #longValue()
 * @see #toString()
 */
 public static UnsignedLong valueOf(String str)
  throws NullPointerException, ParserException
 {
  return new UnsignedLong(parse(str, 0, str.length(),
   true, '9' - '0' + 1));
 }

/**
 * Returns the value of <CODE>this</CODE> number as
 * <CODE>int</CODE>.
 **
 * The result is the same as of <CODE>(int)longValue()</CODE>.
 **
 * @return
 * the numeric <CODE>int</CODE> value represented by the object.
 **
 * @see UnsignedLong#UnsignedLong(long)
 * @see #longValue()
 * @see #floatValue()
 * @see #doubleValue()
 * @see #toString()
 */
 public int intValue()
 {
  return (int)this.unsignedValue;
 }

/**
 * Returns the value of <CODE>this</CODE> number as
 * <CODE>long</CODE>.
 **
 * @return
 * the numeric <CODE>long</CODE> value represented by the object.
 **
 * @see UnsignedLong#UnsignedLong(long)
 * @see #intValue()
 * @see #floatValue()
 * @see #doubleValue()
 * @see #toString()
 */
 public long longValue()
 {
  return this.unsignedValue;
 }

/**
 * Returns the value of <CODE>this</CODE> number as
 * <CODE>float</CODE>.
 **
 * The result is the same as of <CODE>(float)doubleValue()</CODE>.
 * Important notes: this may involve rounding; the result is always
 * non-negative.
 **
 * @return
 * the numeric <CODE>float</CODE> value represented by the object.
 **
 * @see UnsignedLong#UnsignedLong(long)
 * @see #intValue()
 * @see #longValue()
 * @see #doubleValue()
 * @see #toString()
 */
 public float floatValue()
 {
  long unsignedValue = this.unsignedValue;
  return (unsignedValue >>> 1) * 2.0F + ((int)unsignedValue & 1);
 }

/**
 * Returns the value of <CODE>this</CODE> number as
 * <CODE>double</CODE>.
 **
 * Important notes: this may involve rounding; the result is always
 * non-negative.
 **
 * @return
 * the numeric <CODE>double</CODE> value represented by the object.
 **
 * @see UnsignedLong#UnsignedLong(long)
 * @see #intValue()
 * @see #longValue()
 * @see #floatValue()
 * @see #toString()
 */
 public double doubleValue()
 {
  long unsignedValue = this.unsignedValue;
  return (unsignedValue >>> 1) * 2.0D + ((int)unsignedValue & 1);
 }

/**
 * Creates and returns a copy of <CODE>this</CODE> object.
 **
 * The result is the same as of
 * <CODE>new UnsignedLong(longValue())</CODE>.
 **
 * @return
 * a copy (not <CODE>null</CODE> and != <CODE>this</CODE>) of
 * <CODE>this</CODE> instance.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see UnsignedLong#UnsignedLong(long)
 * @see #valueOf(java.lang.String)
 * @see #longValue()
 * @see #equals(java.lang.Object)
 */
 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof UnsignedLong && obj != this)
    return obj;
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * Returns a hash code value for the object.
 **
 * The bits of the wrapped unsigned <CODE>long</CODE> value are
 * mixed in a particular order to produce a single <CODE>int</CODE>
 * 'hash' value. The result is the same as of
 * <CODE>hashCode(longValue())</CODE>.
 **
 * @return
 * a hash code value for <CODE>this</CODE> object.
 **
 * @see #hashCode(long)
 * @see #longValue()
 * @see #equals(java.lang.Object)
 */
 public int hashCode()
 {
  return hashCode(this.unsignedValue);
 }

/**
 * Indicates whether <CODE>this</CODE> object is equal to the
 * specified one.
 **
 * This method returns <CODE>true</CODE> if and only if
 * <VAR>obj</VAR> is instance of <CODE>this</CODE> class, and the
 * wrapped values of <CODE>this</CODE> and of <VAR>obj</VAR> are
 * equal.
 **
 * @param obj
 * the second compared object (may be <CODE>null</CODE>).
 * @return
 * <CODE>true</CODE> if and only if <CODE>this</CODE> value is the
 * same as <VAR>obj</VAR> value.
 **
 * @see #compare(long, long)
 * @see #longValue()
 * @see #hashCode()
 * @see #greaterThan(java.lang.Object)
 */
 public boolean equals(Object obj)
 {
  return obj == this || obj instanceof UnsignedLong &&
   ((UnsignedLong)obj).unsignedValue == this.unsignedValue;
 }

/**
 * Tests for being semantically greater than the argument.
 **
 * The result is <CODE>true</CODE> if and only if <VAR>obj</VAR> is
 * instance of <CODE>this</CODE> class and the wrapped value of
 * <CODE>this</CODE> object is greater (in the unsigned manner) than
 * the wrapped value of the specified object.
 **
 * @param obj
 * the second compared object (may be <CODE>null</CODE>).
 * @return
 * <CODE>true</CODE> if <VAR>obj</VAR> is comparable with
 * <CODE>this</CODE> and <CODE>this</CODE> object is greater than
 * <VAR>obj</VAR>, else <CODE>false</CODE>.
 **
 * @see #greater(long, long)
 * @see #compare(long, long)
 * @see #longValue()
 * @see #equals(java.lang.Object)
 **
 * @since 2.0
 */
 public boolean greaterThan(Object obj)
 {
  long unsignedA = 0L;
  if (obj != this && obj instanceof UnsignedLong)
  {
   long unsignedB = ((UnsignedLong)obj).unsignedValue;
   if (((unsignedA = this.unsignedValue) ^ unsignedB) >= 0L)
    unsignedA = unsignedB - unsignedA;
  }
  return unsignedA < 0L;
 }

/**
 * Converts <CODE>this</CODE> object to its 'in-line' string
 * representation.
 **
 * The wrapped value is converted to its unsigned decimal
 * representation and returned as a string, exactly as by
 * <CODE>toString(longValue(), true)</CODE>.
 **
 * @return
 * the string representation (not <CODE>null</CODE>, with non-zero
 * <CODE>length()</CODE>) of <CODE>this</CODE> object.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see UnsignedLong#UnsignedLong(long)
 * @see #longValue()
 * @see #toString(long, boolean)
 * @see #toString(long, boolean, boolean, int, boolean, int)
 * @see #valueOf(java.lang.String)
 */
 public String toString()
 {
  return toString(this.unsignedValue, true);
 }
}

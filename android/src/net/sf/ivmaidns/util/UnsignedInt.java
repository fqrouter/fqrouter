/*
 * @(#) src/net/sf/ivmaidns/util/UnsignedInt.java --
 * Class for unsigned integer wrappers.
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
 * Class for unsigned integer wrappers.
 **
 * This class wraps a primitive unsigned <CODE>int</CODE> value
 * (like <CODE>Integer</CODE> class). The class also contains
 * <CODE>static</CODE> methods for the unsigned integer arithmetic:
 * comparison, median, multiplication (simple and modular),
 * division, remainder, power, logarithm, factorial, modular
 * inversion, 'greatest common divisor', conversion to/from a byte
 * array (in the direct and reversed orders), stream bits
 * writing/reading, conversion to/from a string (in some given
 * radix, using Roman notation, or according to a given list of
 * mnemonics/abbreviations).
 **
 * @see UnsignedLong
 * @see IntVector
 * @see ByteVector
 * @see PseudoRandom
 * @see JavaConsts
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public final class UnsignedInt extends Number
 implements Immutable, ReallyCloneable, Sortable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.1
 */
 private static final long serialVersionUID = 1932227259095163224L;

/**
 * The Roman digits string specifier.
 **
 * This constant string (not <CODE>null</CODE>) is used internally
 * by the corresponding conversion methods. Important notes: the
 * digits are all latin upper-case characters; the digits are
 * specified starting from the most significant one (the last digit
 * has the weight of <CODE>1</CODE>, the previous one has the weight
 * of <CODE>(('9' - '0') / 2 + 1)</CODE>, the digit before it has
 * the weight of <CODE>('9' - '0' + 1)</CODE>, and so on).
 **
 * @see #toRomanString(int, boolean, int)
 * @see #parseRoman(java.lang.String, int, int)
 **
 * @since 2.0
 */
 public static final String ROMAN_DIGITS = "MDCLXVI";

/**
 * The wrapped (encapsulated) unsigned <CODE>int</CODE> value.
 **
 * Important notes: this value is constant.
 **
 * @serial
 **
 * @see UnsignedInt#UnsignedInt(int)
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
 protected int unsignedValue;

/**
 * Constructs a new object that represents the specified primitive
 * unsigned <CODE>int</CODE> value.
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
 public UnsignedInt(int unsignedValue)
 {
  this.unsignedValue = unsignedValue;
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
 * @see #greaterOrEqual(int, int)
 * @see #compare(int, int)
 * @see #min(int, int)
 * @see #max(int, int)
 * @see #median(int, int, int)
 * @see #greaterThan(java.lang.Object)
 */
 public static final boolean greater(int unsignedA, int unsignedB)
 {
  if ((unsignedA ^ unsignedB) >= 0)
   unsignedA = unsignedB - unsignedA;
  return unsignedA < 0;
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
 * @see #greater(int, int)
 * @see #compare(int, int)
 * @see #min(int, int)
 * @see #max(int, int)
 * @see #median(int, int, int)
 * @see #equals(java.lang.Object)
 * @see #greaterThan(java.lang.Object)
 */
 public static final boolean greaterOrEqual(int unsignedA,
         int unsignedB)
 {
  if ((unsignedA ^ unsignedB) >= 0)
   unsignedB = unsignedA - unsignedB;
  return unsignedB >= 0;
 }

/**
 * Compares two given unsigned integer values.
 **
 * This method returns a signed integer indicating
 * 'less-equal-greater' relation between the specified
 * <CODE>int</CODE> values.
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
 * @see #signedCompare(int, int)
 * @see #greater(int, int)
 * @see #greaterOrEqual(int, int)
 * @see #min(int, int)
 * @see #max(int, int)
 * @see #median(int, int, int)
 * @see #equals(java.lang.Object)
 * @see #greaterThan(java.lang.Object)
 */
 public static final int compare(int unsignedA, int unsignedB)
 {
  int cmp = unsignedB | 1;
  if ((unsignedA ^ unsignedB) >= 0)
   cmp = unsignedA - unsignedB;
  return cmp;
 }

/**
 * Compares two given (signed) integer values.
 **
 * This method returns a signed integer indicating
 * 'less-equal-greater' relation between the specified
 * <CODE>int</CODE> values.
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
 * @see #compare(int, int)
 * @see #signedMedian(int, int, int)
 **
 * @since 2.0
 */
 public static final int signedCompare(int valueA, int valueB)
 {
  int cmp = valueA | 1;
  if ((valueA ^ valueB) >= 0)
   cmp = valueA - valueB;
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
 * @see #max(int, int)
 * @see #median(int, int, int)
 * @see #greater(int, int)
 * @see #greaterOrEqual(int, int)
 * @see #compare(int, int)
 */
 public static final int min(int unsignedA, int unsignedB)
 {
  int delta = unsignedB;
  if ((unsignedA ^ unsignedB) >= 0)
   delta = unsignedA - unsignedB;
  if (delta >= 0)
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
 * @see #min(int, int)
 * @see #median(int, int, int)
 * @see #greater(int, int)
 * @see #greaterOrEqual(int, int)
 * @see #compare(int, int)
 */
 public static final int max(int unsignedA, int unsignedB)
 {
  int delta = unsignedB;
  if ((unsignedA ^ unsignedB) >= 0)
   delta = unsignedA - unsignedB;
  if (delta >= 0)
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
 * @see #signedMedian(int, int, int)
 * @see #min(int, int)
 * @see #max(int, int)
 * @see #greater(int, int)
 * @see #greaterOrEqual(int, int)
 * @see #compare(int, int)
 **
 * @since 1.1
 */
 public static final int median(int unsignedA, int unsignedB,
         int unsignedC)
 {
  int delta;
  if (((delta = unsignedA) ^ unsignedB) >= 0)
   delta = unsignedB - unsignedA;
  if (delta >= 0)
  {
   delta = unsignedA;
   unsignedA = unsignedB;
   unsignedB = delta;
  }
  if (((delta = unsignedC) ^ unsignedA) >= 0)
   delta = unsignedA - unsignedC;
  if (delta >= 0)
  {
   if (((unsignedA = unsignedB) ^ unsignedC) >= 0)
    unsignedB = unsignedC - unsignedA;
   if (unsignedB >= 0)
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
 * @see #median(int, int, int)
 * @see #signedCompare(int, int)
 **
 * @since 1.1
 */
 public static final int signedMedian(int valueA, int valueB,
         int valueC)
 {
  if (valueA <= valueB)
  {
   int delta = valueA;
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
 * @see #divHigh(int, int, int)
 * @see #mulLow(int, int)
 * @see #mul(int, int)
 * @see #mulDiv(int, int, int, boolean)
 * @see #mulMod(int, int, int)
 * @see #power(int, int)
 * @see #factorial(int)
 */
 public static final int mulHigh(int unsignedA, int unsignedB)
 {
  if (JavaConsts.LONG_SIZE >> 1 >= JavaConsts.INT_SIZE)
   return (int)(((unsignedA & JavaConsts.INT_LMASK) *
    (unsignedB & JavaConsts.INT_LMASK)) >>> JavaConsts.INT_SIZE);
  int highA = unsignedA >>> (JavaConsts.INT_SIZE >> 1);
  int highB = unsignedB >>> (JavaConsts.INT_SIZE >> 1);
  unsignedA &= ~(-1 << (JavaConsts.INT_SIZE >> 1));
  unsignedB &= ~(-1 << (JavaConsts.INT_SIZE >> 1));
  unsignedA = ((unsignedA * unsignedB) >>>
   (JavaConsts.INT_SIZE >> 1)) + unsignedA * highB;
  unsignedA += unsignedB *= highA;
  highA = highA * highB +
   (unsignedA >>> (JavaConsts.INT_SIZE >> 1));
  if ((unsignedA ^ unsignedB) >= 0)
   unsignedB = unsignedA - unsignedB;
  if (unsignedB < 0)
   highA -= -1 << (JavaConsts.INT_SIZE >> 1);
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
 * @see #divLow(int, int, int)
 * @see #remLow(int, int, int)
 * @see #mulHigh(int, int)
 * @see #mul(int, int)
 * @see #mulDiv(int, int, int, boolean)
 * @see #mulMod(int, int, int)
 * @see #power(int, int)
 * @see #factorial(int)
 */
 public static final int mulLow(int unsignedA, int unsignedB)
 {
  return unsignedA * unsignedB;
 }

/**
 * Returns the product of two given unsigned <CODE>int</CODE> values
 * extended to <CODE>long</CODE> type.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the unsigned <CODE>long</CODE> result of <VAR>unsignedA</VAR>
 * multiplied by <VAR>unsignedB</VAR>.
 **
 * @see #div(long, int)
 * @see #divHigh(long, int)
 * @see #divLow(long, int)
 * @see #rem(long, int)
 * @see #mulHigh(int, int)
 * @see #mulLow(int, int)
 * @see #mulDiv(int, int, int, boolean)
 * @see #mulMod(int, int, int)
 */
 public static final long mul(int unsignedA, int unsignedB)
 {
  return (unsignedA & JavaConsts.INT_LMASK) *
   (unsignedB & JavaConsts.INT_LMASK);
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
 * @see #mulLow(int, int)
 * @see #rem(int, int)
 * @see #div(long, int)
 * @see #divHigh(int, int, int)
 * @see #divLow(int, int, int)
 * @see #mulDiv(int, int, int, boolean)
 * @see #gcd(int, int)
 * @see #inverseMod(int, int)
 */
 public static final int div(int unsignedA, int unsignedB)
  throws ArithmeticException
 {
  int unsignedRes = 0;
  if (unsignedA >= 0)
  {
   if (unsignedB >= 0)
    unsignedRes = unsignedA / unsignedB;
  }
   else if (unsignedB >= 0 && (unsignedA -= (unsignedRes =
            ((unsignedA >>> 1) / unsignedB) << 1) *
            unsignedB) < 0 || unsignedA >= unsignedB)
    unsignedRes++;
  return unsignedRes;
 }

/**
 * Returns the result of integer division of one given unsigned
 * <CODE>long</CODE> value by another unsigned <CODE>int</CODE>
 * value.
 **
 * @param unsignedLongA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the unsigned result of <VAR>unsignedLongA</VAR> divided by
 * <VAR>unsignedB</VAR>.
 * @exception ArithmeticException
 * if <VAR>unsignedB</VAR> is <CODE>0</CODE>.
 **
 * @see #div(int, int)
 * @see #mul(int, int)
 * @see #rem(long, int)
 * @see #divHigh(long, int)
 * @see #divLow(long, int)
 **
 * @since 1.1
 */
 public static final long div(long unsignedLongA, int unsignedB)
  throws ArithmeticException
 {
  long unsignedLongRes, longB = unsignedB & JavaConsts.INT_LMASK;
  if (unsignedLongA >= 0L)
   unsignedLongRes = unsignedLongA / longB;
   else if ((unsignedLongA -= (unsignedLongRes =
            ((unsignedLongA >>> 1) / longB) << 1) * longB) < 0L ||
            unsignedLongA >= longB)
    unsignedLongRes++;
  return unsignedLongRes;
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
 * @see #divLow(int, int, int)
 * @see #mulHigh(int, int)
 * @see #mulLow(int, int)
 * @see #div(int, int)
 * @see #div(long, int)
 * @see #divHigh(long, int)
 * @see #mulDiv(int, int, int, boolean)
 */
 public static final int divHigh(int unsignedHighA,
         int unsignedLowA, int unsignedB)
  throws ArithmeticException
 {
  return div(unsignedHighA, unsignedB);
 }

/**
 * Returns the highest half of the result of integer division of one
 * given unsigned <CODE>long</CODE> value by another unsigned
 * <CODE>int</CODE> value.
 **
 * @param unsignedLongA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the top half of the unsigned result of <VAR>unsignedLongA</VAR>
 * divided by <VAR>unsignedB</VAR>.
 * @exception ArithmeticException
 * if <VAR>unsignedB</VAR> is <CODE>0</CODE>.
 **
 * @see #divHigh(int, int, int)
 * @see #divLow(long, int)
 * @see #mul(int, int)
 * @see #div(long, int)
 * @see #rem(long, int)
 **
 * @since 1.1
 */
 public static final int divHigh(long unsignedLongA, int unsignedB)
  throws ArithmeticException
 {
  return (int)(div(unsignedLongA, unsignedB) >>>
   JavaConsts.INT_SIZE);
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
 * @see #divHigh(int, int, int)
 * @see #remLow(int, int, int)
 * @see #mulHigh(int, int)
 * @see #mulLow(int, int)
 * @see #div(int, int)
 * @see #div(long, int)
 * @see #divLow(long, int)
 * @see #mulDiv(int, int, int, boolean)
 * @see #gcd(int, int)
 */
 public static final int divLow(int unsignedHighA, int unsignedLowA,
         int unsignedB)
  throws ArithmeticException
 {
  if (JavaConsts.LONG_SIZE >> 1 >= JavaConsts.INT_SIZE)
   return (int)div(((unsignedHighA & JavaConsts.INT_LMASK) <<
    JavaConsts.INT_SIZE) | unsignedLowA & JavaConsts.INT_LMASK,
    unsignedB);
  if ((unsignedHighA = rem(unsignedHighA, unsignedB)) == 0)
   return div(unsignedLowA, unsignedB);
  int unsignedRes;
  if ((unsignedB & (-1 << (JavaConsts.INT_SIZE >> 1))) == 0)
  {
   unsignedRes = div(unsignedHighA =
    (unsignedHighA << (JavaConsts.INT_SIZE >> 1)) |
    (unsignedLowA >>> (JavaConsts.INT_SIZE >> 1)), unsignedB);
   return (unsignedRes << (JavaConsts.INT_SIZE >> 1)) |
    div(((unsignedHighA - unsignedRes * unsignedB) <<
    (JavaConsts.INT_SIZE >> 1)) | unsignedLowA &
    ~(-1 << (JavaConsts.INT_SIZE >> 1)), unsignedB);
  }
  if ((unsignedB & ~(-1 << (JavaConsts.INT_SIZE >> 1))) == 0)
  {
   unsignedB >>>= JavaConsts.INT_SIZE >> 1;
   return (div(unsignedHighA, unsignedB) <<
    (JavaConsts.INT_SIZE >> 1)) | div((rem(unsignedHighA,
    unsignedB) << (JavaConsts.INT_SIZE >> 1)) |
    (unsignedLowA >>> (JavaConsts.INT_SIZE >> 1)), unsignedB);
  }
  unsignedRes = 0;
  for (int shift = JavaConsts.INT_SIZE;
       shift-- > 0; unsignedLowA <<= 1)
  {
   int oldHighA = unsignedHighA;
   unsignedHighA <<= 1;
   if (unsignedLowA < 0)
    unsignedHighA++;
   unsignedRes <<= 1;
   if (oldHighA < 0 || ((unsignedHighA ^ unsignedB) >= 0 ?
       unsignedHighA - unsignedB : unsignedB) >= 0)
   {
    unsignedHighA -= unsignedB;
    unsignedRes++;
   }
  }
  return unsignedRes;
 }

/**
 * Returns the lowest half of the result of integer division of one
 * given unsigned <CODE>long</CODE> value by another unsigned
 * <CODE>int</CODE> value.
 **
 * The result is the same as of
 * <CODE>(int)div(unsignedLongA, unsignedB)</CODE>.
 **
 * @param unsignedLongA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the bottom half of the unsigned result of
 * <VAR>unsignedLongA</VAR> divided by <VAR>unsignedB</VAR>.
 * @exception ArithmeticException
 * if <VAR>unsignedB</VAR> is <CODE>0</CODE>.
 **
 * @see #divLow(int, int, int)
 * @see #divHigh(long, int)
 * @see #mul(int, int)
 * @see #div(long, int)
 * @see #rem(long, int)
 **
 * @since 1.1
 */
 public static final int divLow(long unsignedLongA, int unsignedB)
  throws ArithmeticException
 {
  return (int)div(unsignedLongA, unsignedB);
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
 * @see #mulLow(int, int)
 * @see #div(int, int)
 * @see #rem(long, int)
 * @see #remLow(int, int, int)
 * @see #inverseMod(int, int)
 */
 public static final int rem(int unsignedA, int unsignedB)
  throws ArithmeticException
 {
  if (unsignedA >= 0)
  {
   if (unsignedB >= 0)
    unsignedA %= unsignedB;
  }
   else if (unsignedB >= 0 && (unsignedA = (((unsignedA >>> 1) %
            unsignedB) << 1) | unsignedA & 1) < 0 ||
            unsignedA >= unsignedB)
    unsignedA -= unsignedB;
  return unsignedA;
 }

/**
 * Returns the remainder of integer division of one given unsigned
 * <CODE>long</CODE> value by another unsigned <CODE>int</CODE>
 * value.
 **
 * @param unsignedLongA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the unsigned remainder when <VAR>unsignedLongA</VAR> is divided
 * by <VAR>unsignedB</VAR>.
 * @exception ArithmeticException
 * if <VAR>unsignedB</VAR> is <CODE>0</CODE>.
 **
 * @see #rem(int, int)
 * @see #remLow(int, int, int)
 * @see #mul(int, int)
 * @see #div(long, int)
 **
 * @since 1.1
 */
 public static final int rem(long unsignedLongA, int unsignedB)
  throws ArithmeticException
 {
  long longB = unsignedB & JavaConsts.INT_LMASK;
  if ((unsignedLongA = (((unsignedLongA >>> 1) % longB) << 1) |
      unsignedLongA & 1L) < 0L || unsignedLongA >= longB)
   unsignedLongA -= longB;
  return (int)unsignedLongA;
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
 * @see #rem(int, int)
 * @see #rem(long, int)
 * @see #divLow(int, int, int)
 * @see #mulHigh(int, int)
 * @see #mulLow(int, int)
 * @see #mulMod(int, int, int)
 */
 public static final int remLow(int unsignedHighA, int unsignedLowA,
         int unsignedB)
  throws ArithmeticException
 {
  if (JavaConsts.LONG_SIZE >> 1 >= JavaConsts.INT_SIZE)
   return rem(((unsignedHighA & JavaConsts.INT_LMASK) <<
    JavaConsts.INT_SIZE) | unsignedLowA & JavaConsts.INT_LMASK,
    unsignedB);
  if ((unsignedHighA = rem(unsignedHighA, unsignedB)) == 0)
   return rem(unsignedLowA, unsignedB);
  if ((unsignedB & (-1 << (JavaConsts.INT_SIZE >> 1))) == 0)
   return rem((rem((unsignedHighA << (JavaConsts.INT_SIZE >> 1)) |
    (unsignedLowA >>> (JavaConsts.INT_SIZE >> 1)), unsignedB) <<
    (JavaConsts.INT_SIZE >> 1)) | unsignedLowA &
    ~(-1 << (JavaConsts.INT_SIZE >> 1)), unsignedB);
  if ((unsignedB & ~(-1 << (JavaConsts.INT_SIZE >> 1))) == 0)
  {
   unsignedB >>>= JavaConsts.INT_SIZE >> 1;
   return (rem((rem(unsignedHighA, unsignedB) <<
    (JavaConsts.INT_SIZE >> 1)) | (unsignedLowA >>>
    (JavaConsts.INT_SIZE >> 1)), unsignedB) <<
    (JavaConsts.INT_SIZE >> 1)) |
    unsignedLowA & ~(-1 << (JavaConsts.INT_SIZE >> 1));
  }
  for (int shift = JavaConsts.INT_SIZE;
       shift-- > 0; unsignedLowA <<= 1)
  {
   int oldHighA = unsignedHighA;
   unsignedHighA <<= 1;
   if (unsignedLowA < 0)
    unsignedHighA++;
   if (oldHighA < 0 || ((unsignedHighA ^ unsignedB) >= 0 ?
       unsignedHighA - unsignedB : unsignedB) >= 0)
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
 * @see #mulLow(int, int)
 * @see #binLog(int)
 * @see #factorial(int)
 **
 * @since 1.1
 */
 public static final int power(int value, int degree)
 {
  int res = 0;
  if (degree >= 0)
   if (((2 - value) | value) < 0)
   {
    for (res = 1; degree > 0; value *= value, degree >>= 1)
     if ((degree & 1) != 0)
      res *= value;
   }
    else if (degree == 0 || value != 0)
    {
     res = 1;
     if (value == 2)
      if (degree < JavaConsts.INT_SIZE)
       res <<= degree;
       else res = 0;
    }
  return res;
 }

/**
 * Returns the integer part of binary logarithm of a given unsigned
 * value.
 **
 * If <VAR>unsignedValue</VAR> is zero then <CODE>-1</CODE> is
 * returned, else the result is in the range from <CODE>0</CODE> to
 * <CODE>(JavaConsts INT_SIZE - 1)</CODE>, inclusive.
 **
 * @param unsignedValue
 * the unsigned argument of the logarithm.
 * @return
 * the result of logarithm computation for <VAR>unsignedValue</VAR>.
 **
 * @see #power(int, int)
 * @see #factorial(int)
 **
 * @since 1.2
 */
 public static final int binLog(int unsignedValue)
 {
  int res = -1;
  while (unsignedValue != 0)
  {
   unsignedValue >>>= 1;
   res++;
  }
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
 * @see #mulLow(int, int)
 * @see #power(int, int)
 * @see #binLog(int)
 **
 * @since 1.1
 */
 public static final int factorial(int value)
 {
  int unsignedRes = 0;
  if (value > 0)
   for (unsignedRes = 1; value > 1; unsignedRes *= value--);
  return unsignedRes;
 }

/**
 * Computes the greatest 'common divisor' (gcd) of two given
 * unsigned non-zero values.
 **
 * Important notes: zero value (for any argument or result) is
 * treated as the largest unsigned <CODE>int</CODE> value plus one.
 **
 * @param unsignedA
 * the first unsigned value.
 * @param unsignedB
 * the second unsigned value.
 * @return
 * the unsigned greatest common divisor of both <VAR>unsignedA</VAR>
 * and <VAR>unsignedB</VAR>.
 **
 * @see #inverseMod(int, int)
 * @see #mulMod(int, int, int)
 * @see #mulDiv(int, int, int, boolean)
 * @see #binLog(int)
 **
 * @since 1.1
 */
 public static final int gcd(int unsignedA, int unsignedB)
 {
  int shift = 0, delta;
  if (unsignedA == 0 || unsignedB == 0)
   if ((unsignedA & 1) == 0)
   {
    unsignedA = ((unsignedA - 1) >>> 1) + 1;
    if ((unsignedB & 1) == 0)
    {
     shift = 1;
     unsignedB = ((unsignedB - 1) >>> 1) + 1;
    }
   }
    else unsignedB = ~(-1 >>> 1);
  if (unsignedA != 1)
   while (unsignedA != unsignedB)
    if ((unsignedA & 1) == 0)
    {
     unsignedA >>>= 1;
     if ((unsignedB & 1) == 0)
     {
      shift++;
      unsignedB >>>= 1;
     }
    }
     else if ((unsignedB & 1) == 0)
      unsignedB >>>= 1;
      else if ((delta = (unsignedA >>> 1) - (unsignedB >>> 1)) > 0)
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
 * @see #mulMod(int, int, int)
 * @see #gcd(int, int)
 * @see #div(int, int)
 * @see #rem(int, int)
 **
 * @since 1.1
 */
 public static final int inverseMod(int unsignedA, int unsignedMax)
 {
  int unsignedB, unsignedRes = 1;
  if (unsignedA < 0)
  {
   if (++unsignedMax > 0 && (unsignedA = (((unsignedA >>> 1) %
       unsignedMax) << 1) | unsignedA & 1) < 0 ||
       unsignedA >= unsignedMax)
    unsignedA -= unsignedMax;
  }
   else if (++unsignedMax > 0)
    unsignedA %= unsignedMax;
  if (unsignedA != 0 &&
      (unsignedB = unsignedMax - unsignedA) != unsignedA)
  {
   int unsignedQ = 0, unsignedDiv, max = unsignedMax;
   if (unsignedA < 0)
   {
    unsignedMax = unsignedA;
    unsignedRes = -1;
    unsignedB = unsignedMax - (unsignedA = unsignedB);
    unsignedQ = 1;
   }
   if ((unsignedB = unsignedMax - (unsignedDiv =
       (((unsignedB >>> 1) / unsignedA) << 1) + 1) *
       unsignedA) < 0 || unsignedB >= unsignedA)
   {
    unsignedB -= unsignedA;
    unsignedDiv++;
   }
   while (unsignedB > 0)
   {
    unsignedMax = unsignedA;
    unsignedA = unsignedB;
    unsignedRes = unsignedQ - (unsignedB =
     unsignedRes) * unsignedDiv;
    unsignedQ = unsignedB;
    unsignedB = unsignedMax - (unsignedDiv =
     unsignedMax / unsignedA) * unsignedA;
   }
   if (unsignedRes < 0)
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
 * @see #inverseMod(int, int)
 * @see #gcd(int, int)
 * @see #mulDiv(int, int, int, boolean)
 * @see #mulHigh(int, int)
 * @see #remLow(int, int, int)
 **
 * @since 1.1
 */
 public static final int mulMod(int unsignedA, int unsignedB,
         int unsignedMax)
 {
  int unsignedRes = 0;
  if (unsignedA != 0 && unsignedB != 0)
  {
   unsignedRes = unsignedA * unsignedB;
   if (++unsignedMax != 0)
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
 * the largest unsigned <CODE>int</CODE> value plus one.
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
 * @see #mul(int, int)
 * @see #mulHigh(int, int)
 * @see #mulLow(int, int)
 * @see #divLow(long, int)
 * @see #divLow(int, int, int)
 * @see #gcd(int, int)
 * @see #mulMod(int, int, int)
 * @see #inverseMod(int, int)
 */
 public static final int mulDiv(int unsignedA, int unsignedB,
         int unsignedC, boolean roundUp)
 {
  int unsignedHigh;
  if (unsignedA == 0 || unsignedA == unsignedC)
  {
   unsignedHigh = unsignedB;
   unsignedB = unsignedA;
   unsignedA = unsignedHigh;
  }
  if (unsignedB != unsignedC)
  {
   if (unsignedB != 0)
   {
    unsignedHigh = mulHigh(unsignedA, unsignedB);
    unsignedB *= unsignedA;
    unsignedA = unsignedHigh;
   }
   if (roundUp)
   {
    if (unsignedB == 0)
     unsignedA--;
    unsignedB--;
   }
    else if ((unsignedA | unsignedB) == 0)
    {
     unsignedA = -1;
     unsignedB = -unsignedC;
     roundUp = true;
    }
   if (unsignedC != 0)
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
 * <VAR>len</VAR> is greater than <CODE>JavaConsts INT_LENGTH</CODE>
 * then <VAR>unsignedValue</VAR> is zero-extended.
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
 * @see #toByteArray(int, int)
 * @see #getFromByteArray(byte[], int, int)
 * @see #putToIntelArray(byte[], int, int, int)
 * @see #writeBits(java.io.OutputStream, int, int)
 **
 * @since 1.1
 */
 public static final void putToByteArray(byte[] bytes, int offset,
         int unsignedValue, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   byte temp;
   for (temp = bytes[offset + len - 1];
        len > JavaConsts.INT_LENGTH; bytes[offset++] = 0, len--);
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
 * <CODE>JavaConsts INT_LENGTH</CODE> then <VAR>unsignedValue</VAR>
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
 * @see #putToByteArray(byte[], int, int, int)
 * @see #getFromByteArray(byte[], int, int)
 * @see #toIntelArray(int, int)
 * @see #writeBits(java.io.OutputStream, int, int)
 **
 * @since 1.1
 */
 public static final byte[] toByteArray(int unsignedValue, int len)
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
 * @see #putToByteArray(byte[], int, int, int)
 * @see #toByteArray(int, int)
 * @see #getFromIntelArray(byte[], int, int)
 * @see #readBits(java.io.InputStream, int)
 **
 * @since 1.1
 */
 public static final int getFromByteArray(byte[] bytes,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int unsignedValue = 0;
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
 * <VAR>len</VAR> is greater than <CODE>JavaConsts INT_LENGTH</CODE>
 * then <VAR>unsignedValue</VAR> is zero-extended.
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
 * @see #toIntelArray(int, int)
 * @see #getFromIntelArray(byte[], int, int)
 * @see #putToByteArray(byte[], int, int, int)
 **
 * @since 2.0
 */
 public static final void putToIntelArray(byte[] bytes, int offset,
         int unsignedValue, int len)
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
 * <VAR>len</VAR> is greater than <CODE>JavaConsts INT_LENGTH</CODE>
 * then <VAR>unsignedValue</VAR> is zero-extended.
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
 * @see #putToIntelArray(byte[], int, int, int)
 * @see #getFromIntelArray(byte[], int, int)
 * @see #toByteArray(int, int)
 **
 * @since 2.0
 */
 public static final byte[] toIntelArray(int unsignedValue, int len)
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
 * @see #putToIntelArray(byte[], int, int, int)
 * @see #toIntelArray(int, int)
 * @see #getFromByteArray(byte[], int, int)
 **
 * @since 2.0
 */
 public static final int getFromIntelArray(byte[] bytes,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int unsignedValue = 0;
  if (len > 0)
  {
   byte temp;
   temp = bytes[offset + len - 1];
   if (len >= JavaConsts.INT_LENGTH)
    len = JavaConsts.INT_LENGTH;
   int shift = 0;
   do
   {
    unsignedValue |=
     (bytes[offset++] & JavaConsts.BYTE_MASK) << shift;
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
 * <VAR>count</VAR> is greater than <CODE>JavaConsts INT_SIZE</CODE>
 * then the specified value (containing the bits to write) is
 * zero-extended; <VAR>out</VAR> is a byte-oriented stream, so given
 * bits are padded with zeros (on the most significant side); the
 * bits are written starting from the most significant one.
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
 * @see #readByte(java.io.InputStream)
 **
 * @since 1.1
 */
 public static final void writeBits(OutputStream out,
         int unsignedValue, int count)
  throws NullPointerException, IOException
 {
  out.equals(out);
  if (count > 0)
  {
   int shift;
   if ((shift = count % JavaConsts.BYTE_SIZE) > 0)
    out.write((count -= shift) >= JavaConsts.INT_SIZE ? 0 :
     (unsignedValue >>> count) & ~(-1 << shift));
   while (count > JavaConsts.INT_SIZE)
   {
    out.write(0);
    count -= JavaConsts.BYTE_SIZE;
   }
   while ((count -= JavaConsts.BYTE_SIZE) >= 0)
    out.write(unsignedValue >>> count);
  }
 }

/**
 * Reads a group of bits from a given input stream.
 **
 * Negative <VAR>count</VAR> is treated as zero. If the
 * end-of-stream is detected then <CODE>EOFException</CODE>
 * (subclass of <CODE>IOException</CODE>) is thrown. Important
 * notes: if <VAR>count</VAR> is greater than
 * <CODE>JavaConsts INT_SIZE</CODE> then the result contains only
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
 * less than <CODE>JavaConsts INT_SIZE</CODE>).
 * @exception NullPointerException
 * if <VAR>in</VAR> is <CODE>null</CODE>.
 * @exception IOException
 * if the end-of-stream has been reached or an I/O error occurs.
 **
 * @see #writeBits(java.io.OutputStream, int, int)
 * @see #readByte(java.io.InputStream)
 **
 * @since 1.1
 */
 public static final int readBits(InputStream in, int count)
  throws NullPointerException, IOException
 {
  in.equals(in);
  int unsignedValue = 0, shift;
  if (count > 0)
  {
   if ((shift = count % JavaConsts.BYTE_SIZE) > 0)
   {
    if ((unsignedValue = in.read()) < 0)
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
 * Reads one input <CODE>byte</CODE> as an unsigned value.
 **
 * If the end-of-stream is detected then <CODE>EOFException</CODE>
 * (subclass of <CODE>IOException</CODE>) is thrown.
 **
 * @param in
 * the stream (must be non-<CODE>null</CODE>) to read from.
 * @return
 * an unsigned value read from the stream in the range from
 * <CODE>0</CODE> to <CODE>JavaConsts BYTE_MASK</CODE>, inclusive.
 * @exception NullPointerException
 * if <VAR>in</VAR> is <CODE>null</CODE>.
 * @exception IOException
 * if the end-of-stream has been reached or an I/O error occurs.
 **
 * @see #writeBits(java.io.OutputStream, int, int)
 * @see #readBits(java.io.InputStream, int)
 */
 public static final int readByte(InputStream in)
  throws NullPointerException, IOException
 {
  int unsignedByte;
  if ((unsignedByte = in.read()) < 0)
   throw new EOFException();
  return unsignedByte & JavaConsts.BYTE_MASK;
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
 * @see #toBinaryString(int, int)
 * @see #toOctalString(int, int)
 * @see #toString(int, boolean)
 * @see #toHexString(int, boolean, int)
 * @see #toRomanString(int, boolean, int)
 * @see #toAbbreviation(int, java.lang.String)
 * @see #filledString(char, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 */
 public static final String toString(int value, boolean isUnsigned,
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
  if ((offset = (JavaConsts.INT_SIZE + 1) - minLength) > 0)
   minLength = JavaConsts.INT_SIZE + 1;
   else offset = 0;
  chars = new char[minLength];
  if (forceSign || !isUnsigned)
   if (value < 0)
   {
    isUnsigned = forceSign = false;
    value = -value;
   }
    else if (!forceSign)
     isUnsigned = true;
  int digit = value;
  if ((digit -= (value =
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
   if ((digit = value) <= 0)
    break;
   digit -= (value /= radix) * radix;
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
 * @see #toString(int, boolean, boolean, int, boolean, int)
 * @see #toBinaryString(int, int)
 * @see #toOctalString(int, int)
 * @see #toHexString(int, boolean, int)
 * @see #toRomanString(int, boolean, int)
 * @see #toAbbreviation(int, java.lang.String)
 * @see #filledString(char, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 */
 public static final String toString(int value, boolean isUnsigned)
 {
  int minLength;
  char[] chars = new char[minLength =
   (JavaConsts.INT_SIZE - 1) / 3 + 2];
  if (!isUnsigned)
   if (value < 0)
    value = -value;
    else isUnsigned = true;
  int digit = value;
  if ((digit -= (value = ((value >>> 1) /
      ('9' - '0' + 1)) << 1) * ('9' - '0' + 1)) > '9' - '0')
  {
   digit -= '9' - '0' + 1;
   value++;
  }
  do
  {
   chars[--minLength] = (char)(digit + '0');
   if ((digit = value) <= 0)
    break;
   digit -= (value /= '9' - '0' + 1) * ('9' - '0' + 1);
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
 * @see #toString(int, boolean, boolean, int, boolean, int)
 * @see #toOctalString(int, int)
 * @see #toString(int, boolean)
 * @see #toHexString(int, boolean, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 */
 public static final String toBinaryString(int unsignedValue,
         int minLength)
 {
  char[] chars;
  int offset;
  char prefix = '0';
  if (minLength < 0)
   if ((minLength = -minLength) < 0)
    minLength--;
    else prefix = ' ';
  if ((offset = JavaConsts.INT_SIZE - minLength) > 0)
   minLength = JavaConsts.INT_SIZE;
   else offset = 0;
  chars = new char[minLength];
  do
  {
   chars[--minLength] = (char)((unsignedValue & 1) + '0');
  } while ((unsignedValue >>>= 1) != 0);
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
 * @see #toString(int, boolean, boolean, int, boolean, int)
 * @see #toBinaryString(int, int)
 * @see #toString(int, boolean)
 * @see #toHexString(int, boolean, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 */
 public static final String toOctalString(int unsignedValue,
         int minLength)
 {
  char[] chars;
  int offset;
  char prefix = '0';
  if (minLength < 0)
   if ((minLength = -minLength) < 0)
    minLength--;
    else prefix = ' ';
  if ((offset = ((JavaConsts.INT_SIZE - 1) / 3 + 1) -
      minLength) > 0)
   minLength = (JavaConsts.INT_SIZE - 1) / 3 + 1;
   else offset = 0;
  chars = new char[minLength];
  do
  {
   chars[--minLength] =
    (char)((unsignedValue & ((1 << 3) - 1)) + '0');
  } while ((unsignedValue >>>= 3) != 0);
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
 * @see #toString(int, boolean, boolean, int, boolean, int)
 * @see #toBinaryString(int, int)
 * @see #toOctalString(int, int)
 * @see #toString(int, boolean)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 */
 public static final String toHexString(int unsignedValue,
         boolean upperCase, int minLength)
 {
  char[] chars;
  int digit, offset;
  char prefix = '0';
  if (minLength < 0)
   if ((minLength = -minLength) < 0)
    minLength--;
    else prefix = ' ';
  if ((offset = (((JavaConsts.INT_SIZE - 1) >> 2) + 1) -
      minLength) > 0)
   minLength = ((JavaConsts.INT_SIZE - 1) >> 2) + 1;
   else offset = 0;
  chars = new char[minLength];
  do
  {
   if ((digit = unsignedValue & ((1 << 4) - 1)) > '9' - '0')
   {
    digit += 'a' - '9' - 1;
    if (upperCase)
     digit -= 'a' - 'A';
   }
   chars[--minLength] = (char)(digit + '0');
  } while ((unsignedValue >>>= 4) != 0);
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
 * @see #toString(int, boolean, boolean, int, boolean, int)
 * @see #toBinaryString(int, int)
 * @see #toOctalString(int, int)
 * @see #toString(int, boolean)
 * @see #toHexString(int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 * @see #valueOf(java.lang.String)
 * @see #parseRoman(java.lang.String, int, int)
 * @see #parseAbbreviation(java.lang.String, int, int,
 * java.lang.String)
 */
 public static final int parse(String str, int beginIndex,
         int endIndex, boolean isUnsigned, int radix)
  throws NullPointerException, StringIndexOutOfBoundsException,
         ParserException
 {
  int value = str.length();
  if (beginIndex < 0)
   throw new StringIndexOutOfBoundsException(beginIndex);
  if (endIndex < beginIndex || endIndex > value)
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
   int limit;
   if (~((limit = ((-1 >>> 1) / radix) << 1) * radix) >= radix)
    limit++;
   value = 0;
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
    if (ch >= radix ||
        (value = value * radix + ch) >= 0 && value < ch)
     break;
    if (++beginIndex >= endIndex)
    {
     if (!isUnsigned)
     {
      beginIndex--;
      if (negative)
      {
       if ((value = -value) > 0)
        break;
      }
       else if (value < 0)
        break;
     }
     return value;
    }
   } while (((limit - value) | value) >= 0);
  }
  throw new ParserException(str, beginIndex,
             beginIndex < endIndex ? (ch >= radix ? 1 : 2) : 3);
 }

/**
 * Decodes a given string region as an unsigned
 * octal/decimal/hexadecimal integer value.
 **
 * The following unsigned <CODE>int</CODE> value formats are
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
 * @see #toString(int, boolean, boolean, int, boolean, int)
 * @see #toOctalString(int, int)
 * @see #toString(int, boolean)
 * @see #toHexString(int, boolean, int)
 * @see #valueOf(java.lang.String)
 * @see #parseRoman(java.lang.String, int, int)
 * @see #parseAbbreviation(java.lang.String, int, int,
 * java.lang.String)
 */
 public static final int decode(String str,
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
 * Converts a given value into its Roman notation.
 **
 * Zero value is represented as a single "-". If value is negative
 * then "-" is prepended before Roman digits. Spaces are prepended
 * to the result if needed (according to the absolute value of
 * <VAR>minLength</VAR>). The list of upper-case Roman digits is
 * specified by <CODE>ROMAN_DIGITS</CODE> built-in constant string
 * (the digits are ordered by their weight in it).
 **
 * @param value
 * the signed value to be converted.
 * @param upperCase
 * <CODE>true</CODE> if and only if (digit) characters in the result
 * are in the upper case.
 * @param minLength
 * the minimal length of the result (absolute value).
 * @return
 * the Roman string representation (not <CODE>null</CODE>, with
 * <CODE>length()</CODE> not less than
 * <CODE>max(abs(minLength), 1)</CODE>) of <VAR>value</VAR>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #ROMAN_DIGITS
 * @see #parseRoman(java.lang.String, int, int)
 * @see #toString(int, boolean, boolean, int, boolean, int)
 * @see #toString(int, boolean)
 * @see #toAbbreviation(int, java.lang.String)
 **
 * @since 2.0
 */
 public static final String toRomanString(int value,
         boolean upperCase, int minLength)
 {
  int offset = 1, len, digit;
  String digits;
  if ((len = (digits = ROMAN_DIGITS).length() - 1) < 0)
   value = 0;
  boolean negative = true;
  if (value != 0)
  {
   if (value > 0)
   {
    value = -value;
    negative = false;
   }
   digit = (('9' - '0') >> 1) + 1;
   if ((len & 1) == 0)
    digit = 1;
   for (offset = len >> 1; offset-- > 0; digit *= '9' - '0' + 1);
   if ((offset = ((len + 1) >> 1) * (('9' - '0' - 1) >> 1) -
       value / digit + 1) <= 0)
    offset = -1 >>> 1;
  }
  if (minLength < 0 && (minLength = -minLength) < 0)
   minLength--;
  if ((offset -= minLength) > 0)
   minLength += offset;
   else offset = 0;
  char[] chars = new char[minLength];
  while ((digit = value) != 0)
  {
   char ch, nextCh;
   if (len <= 1)
   {
    ch = digits.charAt(0);
    if (!upperCase)
     ch += 'a' - 'A';
    if (len > 0)
    {
     if ((digit = (value /= (('9' - '0') >> 1) + 1) *
         ((('9' - '0') >> 1) + 1) - digit) >= ('9' - '0') >> 1)
     {
      chars[--minLength] = ch;
      digit = 1;
     }
     if (digit > 0)
     {
      nextCh = digits.charAt(1);
      if (!upperCase)
       nextCh += 'a' - 'A';
      do
      {
       chars[--minLength] = nextCh;
      } while (--digit > 0);
     }
     if (value >= 0)
      break;
    }
    do
    {
     chars[--minLength] = ch;
    } while (++value < 0);
    break;
   }
   if ((digit = (value /= '9' - '0' + 1) * ('9' - '0' + 1) -
       digit) > 0)
   {
    boolean half = false;
    if (digit >= ('9' - '0') >> 1)
    {
     if ((digit -= (('9' - '0') >> 1) + 1) >= ('9' - '0' - 1) >> 1)
      digit = -2;
     half = true;
    }
    ch = digits.charAt(len);
    if (!upperCase)
     ch += 'a' - 'A';
    while (digit-- > 0)
     chars[--minLength] = ch;
    if (half)
    {
     nextCh = digits.charAt((digit >> 1) + len);
     if (!upperCase)
      nextCh += 'a' - 'A';
     chars[--minLength] = nextCh;
     if (digit < -1)
      chars[--minLength] = ch;
    }
   }
   len -= 2;
  }
  if (negative)
   chars[--minLength] = '-';
  while (offset < minLength)
   chars[--minLength] = ' ';
  return new String(chars, minLength, chars.length - minLength);
 }

/**
 * Parses a given string region as an integer in the Roman notation.
 **
 * Leading spaces (before a possible sign prefix) are ignored. The
 * permitted sign prefix is only either '+' or '-'. The next
 * characters (if present) in the string region must all be Roman
 * digits (which are specified by <CODE>ROMAN_DIGITS</CODE>). The
 * order checking for the digits (in the string region) is relaxed
 * here. But number overflow is checked properly
 * (<CODE>ParserException</CODE> is thrown if overflow occurs).
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
 * a signed integer value represented by <VAR>str</VAR> region.
 * @exception NullPointerException
 * if <VAR>str</VAR> is <CODE>null</CODE>.
 * @exception StringIndexOutOfBoundsException
 * if <VAR>beginIndex</VAR> is negative, or if <VAR>endIndex</VAR>
 * is less than <VAR>beginIndex</VAR> or is greater than
 * <CODE>length()</CODE> of <VAR>str</VAR>.
 * @exception ParserException
 * if <VAR>str</VAR> region cannot be parsed as an integer value in
 * the Roman notation (<VAR>error</VAR> is set to <CODE>1</CODE>,
 * <CODE>2</CODE> or <CODE>3</CODE> in the exception, meaning an
 * illegal character is found, number overflow occurs or unexpected
 * end of region is encountered at <VAR>index</VAR>, respectively).
 **
 * @see #ROMAN_DIGITS
 * @see #toRomanString(int, boolean, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 * @see #parseAbbreviation(java.lang.String, int, int,
 * java.lang.String)
 **
 * @since 2.0
 */
 public static final int parseRoman(String str,
         int beginIndex, int endIndex)
  throws NullPointerException, StringIndexOutOfBoundsException,
         ParserException
 {
  int value = str.length(), pos = 0;
  if (beginIndex < 0)
   throw new StringIndexOutOfBoundsException(beginIndex);
  if (endIndex < beginIndex || endIndex > value)
   throw new StringIndexOutOfBoundsException(endIndex);
  beginIndex--;
  char ch = ' ';
  while (++beginIndex < endIndex &&
         (ch = str.charAt(beginIndex)) == ' ');
  if (beginIndex < endIndex)
  {
   boolean negative = false;
   if (ch == '-')
   {
    negative = true;
    beginIndex++;
   }
   if (ch != '+')
    beginIndex--;
   String digits;
   int len = (digits = ROMAN_DIGITS).length() - 1;
   int sum = 0, last = 0, digit = -1;
   value = 0;
   do
   {
    if (++beginIndex >= endIndex)
    {
     if ((~(value = sum - value) & sum) >= 0 &&
         (negative || (value = -value) != ~(-1 >>> 1)))
      return value;
     beginIndex--;
     break;
    }
    if ((char)((ch = str.charAt(beginIndex)) - 'a') <= 'z' - 'a')
     ch -= 'a' - 'A';
    if (ch != digit)
    {
     if ((pos = digits.lastIndexOf(ch, len)) < 0)
      break;
     digit = (('9' - '0') >> 1) + 1;
     if (((pos = len - pos) & 1) == 0)
      digit = 1;
     if ((pos >>= 1) > 0)
     {
      do
      {
       digit *= '9' - '0' + 1;
      } while (--pos > 0);
     }
     if ((last < digit ? (value += sum) & ~sum :
         ~(value = sum - value) & sum) < 0)
      break;
     sum = value;
     value = 0;
     last = digit;
     digit = ch;
    }
   } while ((value += last) > 0);
  }
  throw new ParserException(str, beginIndex,
             beginIndex < endIndex ? (pos < 0 ? 1 : 2) : 3);
 }

/**
 * Converts a given value into its abbreviation according to the
 * specified packed list of abbreviations.
 **
 * If <VAR>value</VAR> has the corresponding string abbreviation
 * (specified in <VAR>paddedAbbrevsList</VAR>) then it is returned,
 * else the decimal string representation for <VAR>value</VAR> (with
 * '-' sign if negative) concatenated with the prefix and postfix
 * (specified in <VAR>paddedAbbrevsList</VAR> too) is returned. As
 * mentioned, all abbreviations are specified in a single
 * <VAR>paddedAbbrevsList</VAR> string which consists of zero or
 * more 'blocks' concatenated together. All blocks have the same
 * some length (the blocks are padded on the right to be of the same
 * length). The first block is as follows: its first character
 * denotes the separator character (used to pad/align
 * abbreviations), the next (zero or more) characters till
 * (exclusively) the next separator specifies the prefix string for
 * values without an assigned abbreviation, the following (zero or
 * more) characters till (exclusively) the next separator specifies
 * the postfix string (for values without an assigned abbreviation),
 * the next (zero or more) separators are appended to pad the block.
 * The second block (if present) must contain at the beginning at
 * least one non-separator character (the offset of this character
 * is, in fact, the length of each block). Every block, which ends
 * with the separator and does not start with the separator,
 * specifies a single abbreviation for a non-negative integer equal
 * to block number minus two. Important notes:
 * <VAR>paddedAbbrevsList</VAR> string is checked for the described
 * format, but no exception is thrown if the format string is bad
 * (in this case, the ordinary signed decimal integer string
 * representation is returned).
 **
 * @param value
 * the signed value to be converted.
 * @param paddedAbbrevsList
 * the string (must be non-<CODE>null</CODE>), which specifies
 * abbreviations list (in the form of concatenation of right-padded
 * abbreviations ordered by value).
 * @return
 * an abbreviation string representation (not <CODE>null</CODE>,
 * with non-zero <CODE>length()</CODE>) of <VAR>value</VAR>.
 * @exception NullPointerException
 * if <VAR>paddedAbbrevsList</VAR> is <CODE>null</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #toString(int, boolean, boolean, int, boolean, int)
 * @see #toRomanString(int, boolean, int)
 * @see #filledString(char, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 * @see #parseAbbreviation(java.lang.String, int, int,
 * java.lang.String)
 **
 * @since 2.0
 */
 public static final String toAbbreviation(int value,
         String paddedAbbrevsList)
  throws NullPointerException
 {
  int prefix, postfix, index;
  String prepend = null;
  if ((postfix = prefix = paddedAbbrevsList.length()) > 1)
  {
   char separator;
   if ((index = paddedAbbrevsList.indexOf(separator =
       paddedAbbrevsList.charAt(0), 1)) > 0 &&
       (index = paddedAbbrevsList.indexOf(separator,
       (prefix = index) + 1)) > 0)
   {
    int len = postfix;
    postfix = index;
    if (value >= 0)
    {
     while (++index < len &&
            paddedAbbrevsList.charAt(index) == separator);
     if ((len - 1) / index > value)
     {
      index *= value + 1;
      if ((len = paddedAbbrevsList.indexOf(separator, index)) >
          index)
       return paddedAbbrevsList.substring(index, len);
     }
    }
   }
   if (prefix > 1)
    prepend = paddedAbbrevsList.substring(1, prefix);
   prefix++;
  }
  String str = toString(value, false);
  if (prepend != null)
   str = prepend + str;
  if (prefix < postfix)
   str += paddedAbbrevsList.substring(prefix, postfix);
  return str;
 }

/**
 * Parses a given string region as an integer value abbreviation
 * according to the specified packed list of abbreviations.
 **
 * This is the opposite to <CODE>toAbbreviation(int, String)</CODE>
 * method. Leading spaces are ignored. All abbreviations are
 * case-insensitive. If the specified string region is recognized as
 * an abbreviation (specified in <VAR>sortedRefAbbrevsList</VAR>)
 * then its integer value (which stands for it) is returned, else
 * the string region is tried to be parsed as a signed decimal
 * integer representation with (or without) the prefix string and
 * with (or without) the postfix string (specified in
 * <VAR>sortedRefAbbrevsList</VAR> too) - its value is returned on
 * success, <CODE>ParserException</CODE> is thrown otherwise. As
 * mentioned, all abbreviations are specified in a single
 * <VAR>sortedRefAbbrevsList</VAR> string which either has the same
 * format as <VAR>paddedAbbrevsList</VAR> for
 * <CODE>toAbbreviation(int, String)</CODE> method or has the
 * following preferred format (which allows fast binary search of an
 * abbreviation). <VAR>sortedRefAbbrevsList</VAR> consists of zero
 * or more 'blocks' concatenated together. All the blocks have the
 * same some length. The first block is as follows: its first
 * character denotes the separator character (used to pad/align
 * abbreviations), the next (zero or more) characters till
 * (exclusively) the next separator specifies the prefix string for
 * values without an assigned abbreviation, the following (zero or
 * more) characters till (exclusively) the next separator specifies
 * the postfix string (again, for values without an assigned
 * abbreviation), the next (zero or more) separators are appended to
 * pad the block. The second block (if present) must contain at the
 * beginning at least one non-separator character (the offset of
 * this character is, in fact, the length of each block). Every
 * block (except for the first one) starts with a single
 * abbreviation (which the block specifies) followed with one or
 * more separator characters (to pad the block), and ends with a
 * non-negative decimal integer number (which stands for the denoted
 * abbreviation) followed with a single separator character. All the
 * blocks (except for the first one) are ordered alphabetically
 * ignoring characters case. Important notes:
 * <VAR>sortedRefAbbrevsList</VAR> string is checked to be of one of
 * these two described formats, if the format string is bad then the
 * specified string region is only tried to be parsed as a signed
 * decimal integer representation.
 **
 * @param str
 * the string (must be non-<CODE>null</CODE>), which region to
 * parse.
 * @param beginIndex
 * the string region beginning index (must be in the range),
 * inclusive.
 * @param endIndex
 * the string region ending index (must be in the range), exclusive.
 * @param sortedRefAbbrevsList
 * the string (must be non-<CODE>null</CODE>), which specifies valid
 * abbreviations list (in the form of concatenation of right-padded
 * abbreviations ordered by value or in the form of concatenation of
 * center-padded abbreviation-value pairs sorted alphabetically
 * ignoring case).
 * @return
 * a signed integer value represented by <VAR>str</VAR> region.
 * @exception NullPointerException
 * if <VAR>str</VAR> is <CODE>null</CODE> or
 * <VAR>sortedRefAbbrevsList</VAR> is <CODE>null</CODE>.
 * @exception StringIndexOutOfBoundsException
 * if <VAR>beginIndex</VAR> is negative, or if <VAR>endIndex</VAR>
 * is less than <VAR>beginIndex</VAR> or is greater than
 * <CODE>length()</CODE> of <VAR>str</VAR>.
 * @exception ParserException
 * if <VAR>str</VAR> region cannot be parsed as one among the
 * specified abbreviations or as a signed integer (<VAR>error</VAR>
 * is set to <CODE>1</CODE>, <CODE>2</CODE> or <CODE>3</CODE> in the
 * exception, meaning an unknown abbreviation is encountered, number
 * overflow occurs or unexpected end of region is encountered at
 * <VAR>index</VAR>, respectively).
 **
 * @see #toAbbreviation(int, java.lang.String)
 * @see #toString(int, boolean, boolean, int, boolean, int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 * @see #parseRoman(java.lang.String, int, int)
 **
 * @since 2.0
 */
 public static final int parseAbbreviation(String str,
         int beginIndex, int endIndex, String sortedRefAbbrevsList)
  throws NullPointerException, StringIndexOutOfBoundsException,
         ParserException
 {
  int prefix = sortedRefAbbrevsList.length();
  if (((str.length() - endIndex) | (prefix - 2) | beginIndex) >= 0)
  {
   beginIndex--;
   while (++beginIndex < endIndex && str.charAt(beginIndex) == ' ');
   if (beginIndex < endIndex)
   {
    char separator;
    int postfix = 0, len = prefix;
    int strLen = endIndex - beginIndex, index;
    if ((index = sortedRefAbbrevsList.indexOf(separator =
        sortedRefAbbrevsList.charAt(0), 1)) > 0 &&
        (index = sortedRefAbbrevsList.indexOf(separator,
        (prefix = index) + 1)) > 0)
    {
     postfix = index - prefix - 1;
     if (str.charAt(beginIndex) != separator &&
         str.charAt(endIndex - 1) != separator)
     {
      while (++index < len &&
             sortedRefAbbrevsList.charAt(index) == separator);
      int delta, value, match;
      char ch;
      if ((delta = index) <= len - index && (ch =
          sortedRefAbbrevsList.charAt(match = (index - 1) << 1)) !=
          separator && (char)(ch - '0') <= '9' - '0' &&
          sortedRefAbbrevsList.lastIndexOf(separator,
          match - 1) > index)
      {
       if (strLen < index)
       {
        len = len / index - 1;
        index = 1;
        int lowMatch = 0, highMatch = 0;
        while (index <= len)
        {
         int pos = (value = (index + len) >> 1) * delta, cmp;
         if ((match = lowMatch) >= highMatch)
          match = highMatch;
         match--;
         do
         {
          ch = sortedRefAbbrevsList.charAt(++match + pos);
          if (match < strLen)
           cmp = str.charAt(beginIndex + match);
           else if ((cmp = separator) == ch)
           {
            index = pos + delta;
            value--;
            while (sortedRefAbbrevsList.charAt(--index) ==
                   separator);
            if (pos + match < index)
            {
             pos = sortedRefAbbrevsList.lastIndexOf(separator,
              index - 1);
             value = 0;
             do
             {
              value = value * ('9' - '0' + 1) +
               sortedRefAbbrevsList.charAt(++pos) - '0';
             } while (pos < index);
            }
            return value;
           }
         } while (((cmp -= ch) == 0 ||
                  (cmp = Character.toUpperCase((char)(cmp + ch)) -
                  Character.toUpperCase(ch)) == 0) &&
                  ch != separator);
         if (cmp > 0)
         {
          index = value + 1;
          lowMatch = match;
         }
          else
          {
           len = value - 1;
           highMatch = match;
          }
        }
       }
      }
       else for (value = 0; (len -= delta) > 0 &&
                 (match = sortedRefAbbrevsList.indexOf(separator,
                 index)) > 0; index += delta, value++)
        if (index + strLen == match &&
            sortedRefAbbrevsList.regionMatches(true,
            index, str, beginIndex, strLen))
         return value;
     }
    }
    if (--prefix > 0 && strLen > prefix &&
        str.charAt(beginIndex + prefix) != ' ' &&
        sortedRefAbbrevsList.regionMatches(true, 1,
        str, beginIndex, prefix))
     beginIndex += prefix;
    if (postfix > 0 && endIndex - beginIndex > postfix &&
        sortedRefAbbrevsList.regionMatches(true, prefix + 2,
        str, endIndex - postfix, postfix))
     endIndex -= postfix;
   }
  }
  return parse(str, beginIndex, endIndex, false, '9' - '0' + 1);
 }

/**
 * Constructs and returns a string filled with a given character.
 **
 * The resulting string has the specified length and is entirely
 * filled with the specified character. Negative <VAR>len</VAR> is
 * treated as zero. Important notes:
 * <CODE>arraycopy(Object, int, Object, int, int)</CODE> method of
 * <CODE>System</CODE> class is used to fill the content of the
 * string.
 **
 * @param ch
 * the character to fill the string with.
 * @param len
 * the length of the string which is created and filled.
 * @return
 * the filled string (not <CODE>null</CODE>, with
 * <CODE>length()</CODE> equal to <CODE>max(len, 0)</CODE>).
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #toString(int, boolean)
 * @see #toBinaryString(int, int)
 * @see #toOctalString(int, int)
 * @see #toHexString(int, boolean, int)
 * @see #toAbbreviation(int, java.lang.String)
 **
 * @since 2.0
 */
 public static final String filledString(char ch, int len)
 {
  if (len <= 0)
   len = 0;
  char[] chars = new char[len];
  if (ch != 0)
  {
   int remain, next = 2, block;
   if ((remain = len) > 3)
    remain = 4;
   while (remain-- > 0)
    chars[remain] = ch;
   remain = len - 2;
   while ((remain -= next) > 0)
   {
    if ((block = next <<= 1) >= remain)
     next = remain;
    System.arraycopy(chars, 0, chars, block, next);
   }
  }
  return new String(chars, 0, len);
 }

/**
 * Converts a given string to an instance of this class.
 **
 * This method returns a new <CODE>UnsignedInt</CODE> object
 * initialized to the unsigned decimal integer value of the
 * specified string.
 **
 * @param str
 * the string (must be non-<CODE>null</CODE>, representing a valid
 * unsigned decimal integer) to be parsed.
 * @return
 * a newly constructed <CODE>UnsignedInt</CODE> instance (not
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
 * @see UnsignedInt#UnsignedInt(int)
 * @see #parse(java.lang.String, int, int, boolean, int)
 * @see #decode(java.lang.String, int, int)
 * @see #intValue()
 * @see #toString()
 */
 public static UnsignedInt valueOf(String str)
  throws NullPointerException, ParserException
 {
  return new UnsignedInt(parse(str, 0, str.length(),
   true, '9' - '0' + 1));
 }

/**
 * Returns the value of <CODE>this</CODE> number as
 * <CODE>int</CODE>.
 **
 * @return
 * the numeric <CODE>int</CODE> value represented by the object.
 **
 * @see UnsignedInt#UnsignedInt(int)
 * @see #longValue()
 * @see #floatValue()
 * @see #doubleValue()
 * @see #toString()
 */
 public int intValue()
 {
  return this.unsignedValue;
 }

/**
 * Returns the value of <CODE>this</CODE> number as
 * <CODE>long</CODE>.
 **
 * The result is the same as of
 * <CODE>(intValue() & JavaConsts INT_LMASK)</CODE>.
 **
 * @return
 * the numeric <CODE>long</CODE> value represented by the object.
 **
 * @see UnsignedInt#UnsignedInt(int)
 * @see #intValue()
 * @see #floatValue()
 * @see #doubleValue()
 * @see #toString()
 */
 public long longValue()
 {
  return this.unsignedValue & JavaConsts.INT_LMASK;
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
 * @see UnsignedInt#UnsignedInt(int)
 * @see #intValue()
 * @see #longValue()
 * @see #doubleValue()
 * @see #toString()
 */
 public float floatValue()
 {
  int unsignedValue = this.unsignedValue;
  return (unsignedValue >>> 1) * 2.0F + (unsignedValue & 1);
 }

/**
 * Returns the value of <CODE>this</CODE> number as
 * <CODE>double</CODE>.
 **
 * Important notes: the result is always non-negative.
 **
 * @return
 * the numeric <CODE>double</CODE> value represented by the object.
 **
 * @see UnsignedInt#UnsignedInt(int)
 * @see #intValue()
 * @see #longValue()
 * @see #floatValue()
 * @see #toString()
 */
 public double doubleValue()
 {
  int unsignedValue = this.unsignedValue;
  return (unsignedValue >>> 1) * 2.0D + (unsignedValue & 1);
 }

/**
 * Creates and returns a copy of <CODE>this</CODE> object.
 **
 * The result is the same as of
 * <CODE>new UnsignedInt(intValue())</CODE>.
 **
 * @return
 * a copy (not <CODE>null</CODE> and != <CODE>this</CODE>) of
 * <CODE>this</CODE> instance.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see UnsignedInt#UnsignedInt(int)
 * @see #valueOf(java.lang.String)
 * @see #intValue()
 * @see #equals(java.lang.Object)
 */
 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof UnsignedInt && obj != this)
    return obj;
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * Returns a hash code value for the object.
 **
 * The hash code value for this object is equal to the primitive
 * <CODE>int</CODE> value represented by this object.
 **
 * @return
 * a hash code value for <CODE>this</CODE> object.
 **
 * @see #intValue()
 * @see #equals(java.lang.Object)
 */
 public int hashCode()
 {
  return this.unsignedValue;
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
 * @see #compare(int, int)
 * @see #intValue()
 * @see #hashCode()
 * @see #greaterThan(java.lang.Object)
 */
 public boolean equals(Object obj)
 {
  return obj == this || obj instanceof UnsignedInt &&
   ((UnsignedInt)obj).unsignedValue == this.unsignedValue;
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
 * @see #greater(int, int)
 * @see #compare(int, int)
 * @see #intValue()
 * @see #equals(java.lang.Object)
 **
 * @since 2.0
 */
 public boolean greaterThan(Object obj)
 {
  int unsignedA = 0;
  if (obj != this && obj instanceof UnsignedInt)
  {
   int unsignedB = ((UnsignedInt)obj).unsignedValue;
   if (((unsignedA = this.unsignedValue) ^ unsignedB) >= 0)
    unsignedA = unsignedB - unsignedA;
  }
  return unsignedA < 0;
 }

/**
 * Converts <CODE>this</CODE> object to its 'in-line' string
 * representation.
 **
 * The wrapped value is converted to its unsigned decimal
 * representation and returned as a string, exactly as by
 * <CODE>toString(intValue(), true)</CODE>.
 **
 * @return
 * the string representation (not <CODE>null</CODE>, with non-zero
 * <CODE>length()</CODE>) of <CODE>this</CODE> object.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see UnsignedInt#UnsignedInt(int)
 * @see #intValue()
 * @see #toString(int, boolean)
 * @see #toString(int, boolean, boolean, int, boolean, int)
 * @see #toAbbreviation(int, java.lang.String)
 * @see #valueOf(java.lang.String)
 */
 public String toString()
 {
  return toString(this.unsignedValue, true);
 }
}

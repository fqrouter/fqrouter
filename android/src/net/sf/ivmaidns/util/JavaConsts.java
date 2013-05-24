/*
 * @(#) src/net/sf/ivmaidns/util/JavaConsts.java --
 * Class defining Java-specific constants.
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
 * Class defining Java-specific constants.
 **
 * This class contains such the specific fundamental constants of
 * Java platform (defined by 'The Java Language Specification') as
 * the sizes, masks and limits for all Java numeric primitive types.
 **
 * @see UnsignedInt
 * @see UnsignedLong
 * @see PseudoRandom
 * @see ByteVector
 * @see CharVector
 * @see DoubleVector
 * @see FloatVector
 * @see IntVector
 * @see LongVector
 * @see ShortVector
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 1.8
 */
public final class JavaConsts
{

/**
 * The size of <CODE>byte</CODE> primitive type in bits.
 **
 * This is a fundamental constant of Java (<CODE>8</CODE> by the
 * standard). Its value is the same as the result of execution of
 * <CODE>value = 1; do { size++; } while ((value *= 2) != 0);</CODE>
 * statement, where <VAR>value</VAR> is of <CODE>byte</CODE> type.
 **
 * @see #BYTE_MASK
 * @see #MIN_BYTE
 * @see #MAX_BYTE
 * @see #CHAR_SIZE
 * @see #SHORT_SIZE
 * @see #INT_SIZE
 * @see #LONG_SIZE
 */
 public static final int BYTE_SIZE = 8;

/**
 * The size of <CODE>char</CODE> primitive type in bits.
 **
 * This is a fundamental constant of Java (<CODE>16</CODE> by the
 * standard). Its value is the same as the result of execution of
 * <CODE>value = 1; do { size++; } while ((value *= 2) != 0);</CODE>
 * statement, where <VAR>value</VAR> is of <CODE>char</CODE> type.
 * Important notes: this size is not less than
 * <CODE>BYTE_SIZE</CODE>; see also the Unicode standard.
 **
 * @see #CHAR_LENGTH
 * @see #CHAR_MASK
 * @see #BYTE_SIZE
 * @see #SHORT_SIZE
 * @see #INT_SIZE
 * @see #LONG_SIZE
 */
 public static final int CHAR_SIZE = 16;

/**
 * The size of <CODE>short</CODE> primitive type in bits.
 **
 * This is a fundamental constant of Java (<CODE>16</CODE> by the
 * standard). Its value is the same as the result of execution of
 * <CODE>value = 1; do { size++; } while ((value *= 2) != 0);</CODE>
 * statement, where <VAR>value</VAR> is of <CODE>short</CODE> type.
 * Important notes: this size is not less than
 * <CODE>BYTE_SIZE</CODE>.
 **
 * @see #SHORT_LENGTH
 * @see #SHORT_MASK
 * @see #MIN_SHORT
 * @see #MAX_SHORT
 * @see #BYTE_SIZE
 * @see #CHAR_SIZE
 * @see #INT_SIZE
 * @see #LONG_SIZE
 */
 public static final int SHORT_SIZE = 16;

/**
 * The size of mantissa of <CODE>float</CODE> primitive type in
 * bits.
 **
 * This is a fundamental constant of Java (<CODE>23</CODE> by the
 * standard). Its value is the same as the result of execution of
 * <CODE>value = 1; while ((value *= 2) + 1 != value) size++;</CODE>
 * statement, where <VAR>value</VAR> is of <CODE>float</CODE> type.
 * Important notes: this size is less than <CODE>INT_SIZE</CODE>;
 * the whole size of <CODE>float</CODE> type is the same as the size
 * of <CODE>int</CODE> type.
 **
 * @see #FLOAT_M_MASK
 * @see #FLOAT_EXP_SIZE
 * @see #DOUBLE_M_SIZE
 * @see #INT_SIZE
 */
 public static final int FLOAT_M_SIZE = 23;

/**
 * The size of <CODE>int</CODE> primitive type in bits.
 **
 * This is a fundamental constant of Java (<CODE>32</CODE> by the
 * standard). Its value is the same as the result of execution of
 * <CODE>value = 1; do { size++; } while ((value *= 2) != 0);</CODE>
 * statement, where <VAR>value</VAR> is of <CODE>int</CODE> type.
 * Important notes: this size is not less than
 * <CODE>SHORT_SIZE</CODE>.
 **
 * @see #INT_LENGTH
 * @see #INT_LMASK
 * @see #MIN_INT
 * @see #MAX_INT
 * @see #BYTE_SIZE
 * @see #CHAR_SIZE
 * @see #SHORT_SIZE
 * @see #LONG_SIZE
 * @see #FLOAT_M_SIZE
 */
 public static final int INT_SIZE = 32;

/**
 * The size of mantissa of <CODE>double</CODE> primitive type in
 * bits.
 **
 * This is a fundamental constant of Java (<CODE>52</CODE> by the
 * standard). Its value is the same as the result of execution of
 * <CODE>value = 1; while ((value *= 2) + 1 != value) size++;</CODE>
 * statement, where <VAR>value</VAR> is of <CODE>double</CODE> type.
 * Important notes: this size is less than <CODE>LONG_SIZE</CODE>
 * and not less than <CODE>FLOAT_M_SIZE</CODE>; the whole size of
 * <CODE>double</CODE> type is the same as the size of
 * <CODE>long</CODE>.
 **
 * @see #DOUBLE_M_MASK
 * @see #DOUBLE_EXP_SIZE
 * @see #FLOAT_M_SIZE
 * @see #LONG_SIZE
 */
 public static final int DOUBLE_M_SIZE = 52;

/**
 * The size of <CODE>long</CODE> primitive type in bits.
 **
 * This is a fundamental constant of Java (<CODE>64</CODE> by the
 * standard). Its value is the same as the result of execution of
 * <CODE>value = 1; do { size++; } while ((value *= 2) != 0);</CODE>
 * statement, where <VAR>value</VAR> is of <CODE>long</CODE> type.
 * Important notes: this size is not less than
 * <CODE>INT_SIZE</CODE>.
 **
 * @see #LONG_LENGTH
 * @see #MIN_LONG
 * @see #MAX_LONG
 * @see #BYTE_SIZE
 * @see #SHORT_SIZE
 * @see #INT_SIZE
 * @see #DOUBLE_M_SIZE
 */
 public static final int LONG_SIZE = 64;

/**
 * Unsigned 'Gold Median' constant.
 **
 * This constant is the calculated integer value of
 * <CODE>(sqrt(5) - 1) * (MAX_INT + 1)</CODE> rounded to the
 * nearest prime. 'Gold Median' is used for various digest
 * calculations ('Multiplicative' method) as follows:
 * <CODE>(((value ^ seed) * GOLD_MEDIAN) >>> shift)</CODE>, where
 * <VAR>shift</VAR> is equal to
 * <CODE>(INT_SIZE - resultSize)</CODE>.
 **
 * @see #LONG_GOLD_MEDIAN
 * @see #INT_SIZE
 * @see #INT_LMASK
 * @see #MAX_INT
 */
 public static final int GOLD_MEDIAN = 0x9E3779B1;

/**
 * Unsigned 'Gold Median' constant for <CODE>long</CODE> type.
 **
 * This constant is the calculated <CODE>long</CODE> integer value
 * of <CODE>(sqrt(5) - 1) * (MAX_LONG + 1)</CODE> rounded to the
 * nearest prime. 'Gold Median' is used for various digest
 * calculations ('Multiplicative' method) as follows:
 * <CODE>(((value ^ seed) * LONG_GOLD_MEDIAN) >>> shift)</CODE>,
 * where <VAR>shift</VAR> is equal to
 * <CODE>(LONG_SIZE - resultSize)</CODE>.
 **
 * @see #GOLD_MEDIAN
 * @see #LONG_SIZE
 * @see #MAX_LONG
 */
 public static final long LONG_GOLD_MEDIAN = 0x9E3779B97F4A7C55L;

/**
 * The size of exponent of <CODE>float</CODE> primitive type in
 * bits.
 **
 * @see #FLOAT_EXP_MASK
 * @see #FLOAT_M_SIZE
 * @see #DOUBLE_EXP_SIZE
 * @see #INT_SIZE
 */
 public static final int FLOAT_EXP_SIZE =
  INT_SIZE - FLOAT_M_SIZE - 1;

/**
 * The size of exponent of <CODE>double</CODE> primitive type in
 * bits.
 **
 * @see #DOUBLE_EXP_MASK
 * @see #DOUBLE_M_SIZE
 * @see #FLOAT_EXP_SIZE
 * @see #LONG_SIZE
 */
 public static final int DOUBLE_EXP_SIZE =
  LONG_SIZE - DOUBLE_M_SIZE - 1;

/**
 * The length of <CODE>char</CODE> primitive type (in bytes).
 **
 * @see #CHAR_SIZE
 * @see #CHAR_MASK
 * @see #SHORT_LENGTH
 * @see #INT_LENGTH
 * @see #LONG_LENGTH
 */
 public static final int CHAR_LENGTH =
  (CHAR_SIZE - 1) / BYTE_SIZE + 1;

/**
 * The length of <CODE>short</CODE> primitive type (in bytes).
 **
 * @see #SHORT_SIZE
 * @see #SHORT_MASK
 * @see #MIN_SHORT
 * @see #MAX_SHORT
 * @see #CHAR_LENGTH
 * @see #INT_LENGTH
 * @see #LONG_LENGTH
 */
 public static final int SHORT_LENGTH =
  (SHORT_SIZE - 1) / BYTE_SIZE + 1;

/**
 * The length of <CODE>int</CODE> primitive type (in bytes).
 **
 * @see #INT_SIZE
 * @see #INT_LMASK
 * @see #MIN_INT
 * @see #MAX_INT
 * @see #CHAR_LENGTH
 * @see #SHORT_LENGTH
 * @see #LONG_LENGTH
 */
 public static final int INT_LENGTH =
  (INT_SIZE - 1) / BYTE_SIZE + 1;

/**
 * The length of <CODE>long</CODE> primitive type (in bytes).
 **
 * @see #LONG_SIZE
 * @see #MIN_LONG
 * @see #MAX_LONG
 * @see #CHAR_LENGTH
 * @see #SHORT_LENGTH
 * @see #INT_LENGTH
 */
 public static final int LONG_LENGTH =
  (LONG_SIZE - 1) / BYTE_SIZE + 1;

/**
 * The unsigned <CODE>int</CODE> mask of <CODE>byte</CODE> type.
 **
 * @see #BYTE_SIZE
 * @see #MIN_BYTE
 * @see #MAX_BYTE
 * @see #CHAR_MASK
 * @see #SHORT_MASK
 * @see #INT_LMASK
 */
 public static final int BYTE_MASK =
  ~((-1 << (BYTE_SIZE - 1)) << 1);

/**
 * The unsigned <CODE>int</CODE> mask of <CODE>char</CODE> type.
 **
 * @see #CHAR_SIZE
 * @see #CHAR_LENGTH
 * @see #BYTE_MASK
 * @see #SHORT_MASK
 * @see #INT_LMASK
 */
 public static final int CHAR_MASK =
  ~((-1 << (CHAR_SIZE - 1)) << 1);

/**
 * The unsigned <CODE>int</CODE> mask of <CODE>short</CODE> type.
 **
 * @see #SHORT_SIZE
 * @see #SHORT_LENGTH
 * @see #MIN_SHORT
 * @see #MAX_SHORT
 * @see #BYTE_MASK
 * @see #CHAR_MASK
 * @see #INT_LMASK
 */
 public static final int SHORT_MASK =
  ~((-1 << (SHORT_SIZE - 1)) << 1);

/**
 * The unsigned <CODE>long</CODE> mask of <CODE>int</CODE> type.
 **
 * @see #INT_SIZE
 * @see #INT_LENGTH
 * @see #MIN_INT
 * @see #MAX_INT
 * @see #BYTE_MASK
 * @see #CHAR_MASK
 * @see #SHORT_MASK
 * @see #FLOAT_M_MASK
 * @see #FLOAT_EXP_MASK
 */
 public static final long INT_LMASK = (-1 >>> 1) * 2L + 1L;

/**
 * The unsigned <CODE>int</CODE> mask of the mantissa part of
 * <CODE>float</CODE> type.
 **
 * @see #FLOAT_M_SIZE
 * @see #FLOAT_EXP_MASK
 * @see #DOUBLE_M_MASK
 * @see #INT_SIZE
 * @see #INT_LENGTH
 * @see #INT_LMASK
 */
 public static final int FLOAT_M_MASK = ~(-1 << FLOAT_M_SIZE);

/**
 * The unsigned <CODE>int</CODE> mask of the exponent part of
 * <CODE>float</CODE> type.
 **
 * @see #FLOAT_EXP_SIZE
 * @see #FLOAT_M_MASK
 * @see #DOUBLE_EXP_MASK
 * @see #INT_SIZE
 * @see #INT_LENGTH
 * @see #INT_LMASK
 */
 public static final int FLOAT_EXP_MASK =
  ~(-1 << FLOAT_EXP_SIZE) << FLOAT_M_SIZE;

/**
 * The unsigned <CODE>long</CODE> mask of the mantissa part of
 * <CODE>double</CODE> type.
 **
 * @see #DOUBLE_M_SIZE
 * @see #DOUBLE_EXP_MASK
 * @see #FLOAT_M_MASK
 * @see #LONG_SIZE
 * @see #LONG_LENGTH
 */
 public static final long DOUBLE_M_MASK = ~(-1L << DOUBLE_M_SIZE);

/**
 * The unsigned <CODE>long</CODE> mask of the exponent part of
 * <CODE>double</CODE> type.
 **
 * @see #DOUBLE_EXP_SIZE
 * @see #DOUBLE_M_MASK
 * @see #FLOAT_EXP_MASK
 * @see #LONG_SIZE
 * @see #LONG_LENGTH
 */
 public static final long DOUBLE_EXP_MASK =
  ~(-1L << DOUBLE_EXP_SIZE) << DOUBLE_M_SIZE;

/**
 * The smallest <CODE>byte</CODE> value.
 **
 * @see #BYTE_SIZE
 * @see #BYTE_MASK
 * @see #MAX_BYTE
 * @see #MIN_SHORT
 * @see #MIN_INT
 * @see #MIN_LONG
 */
 public static final int MIN_BYTE = ~(BYTE_MASK >>> 1);

/**
 * The largest <CODE>byte</CODE> value.
 **
 * @see #BYTE_SIZE
 * @see #BYTE_MASK
 * @see #MIN_BYTE
 * @see #MAX_SHORT
 * @see #MAX_INT
 * @see #MAX_LONG
 */
 public static final int MAX_BYTE = BYTE_MASK >>> 1;

/**
 * The smallest <CODE>short</CODE> value.
 **
 * @see #SHORT_SIZE
 * @see #SHORT_LENGTH
 * @see #SHORT_MASK
 * @see #MAX_SHORT
 * @see #MIN_BYTE
 * @see #MIN_INT
 * @see #MIN_LONG
 */
 public static final int MIN_SHORT = ~(SHORT_MASK >>> 1);

/**
 * The largest <CODE>short</CODE> value.
 **
 * @see #SHORT_SIZE
 * @see #SHORT_LENGTH
 * @see #SHORT_MASK
 * @see #MIN_SHORT
 * @see #MAX_BYTE
 * @see #MAX_INT
 * @see #MAX_LONG
 */
 public static final int MAX_SHORT = SHORT_MASK >>> 1;

/**
 * The smallest <CODE>int</CODE> value.
 **
 * @see #INT_SIZE
 * @see #INT_LENGTH
 * @see #INT_LMASK
 * @see #MAX_INT
 * @see #MIN_BYTE
 * @see #MIN_SHORT
 * @see #MIN_LONG
 */
 public static final int MIN_INT = ~(-1 >>> 1);

/**
 * The largest <CODE>int</CODE> value.
 **
 * @see #INT_SIZE
 * @see #INT_LENGTH
 * @see #INT_LMASK
 * @see #MIN_INT
 * @see #MAX_BYTE
 * @see #MAX_SHORT
 * @see #MAX_LONG
 */
 public static final int MAX_INT = -1 >>> 1;

/**
 * The smallest <CODE>long</CODE> value.
 **
 * @see #LONG_SIZE
 * @see #LONG_LENGTH
 * @see #MAX_LONG
 * @see #MIN_BYTE
 * @see #MIN_SHORT
 * @see #MIN_INT
 */
 public static final long MIN_LONG = ~(-1L >>> 1);

/**
 * The largest <CODE>long</CODE> value.
 **
 * @see #LONG_SIZE
 * @see #LONG_LENGTH
 * @see #MIN_LONG
 * @see #MAX_BYTE
 * @see #MAX_SHORT
 * @see #MAX_INT
 */
 public static final long MAX_LONG = -1L >>> 1;

/**
 * A dummy <CODE>private</CODE> constructor prohibiting
 * instantiation of this class.
 */
 private JavaConsts() {}
}

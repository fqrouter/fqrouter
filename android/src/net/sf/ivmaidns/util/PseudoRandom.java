/*
 * @(#) src/net/sf/ivmaidns/util/PseudoRandom.java --
 * Class for pseudo-random generator.
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

import java.io.Serializable;

/**
 * Class for pseudo-random generator.
 **
 * An instance of this class is used to generate a stream of
 * pseudo-random numbers. The class uses a 64-bit mutable seed,
 * which contains 61-bit and 3-bit shift registers (which stand for
 * <CODE>((x pow 61) + (x pow 3) + 1)</CODE> and
 * <CODE>((y pow 3) + y + 1)</CODE> factory polynomes). Each single
 * generated pseudo-random bit is <CODE>((x ^ y) & 1)</CODE>, where
 * new <VAR>x</VAR> is set to
 * <CODE>(x * 2 | ((x >> 60) ^ (x >> 2)) & 1)</CODE> and new
 * <VAR>y</VAR> is set to <CODE>(y * 2 | ((y >> 2) ^ y) & 1)</CODE>,
 * which are the current non-zero values of the 61-bit and 3-bit
 * shift registers, respectively. Such construction of these two
 * shift registers guarantees good (but not cryptographically
 * strong) uniformly distributed pseudo-random bits sequence with
 * the aperiodity length of
 * <CODE>(((2 pow 61) - 1) * ((2 pow 3) - 1))</CODE>. Of course, the
 * actual algorithm is supplied with the fixes for zero values of
 * these shift registers (if <VAR>x</VAR> is zero then a non-zero
 * constant is added to <CODE>this</CODE> <VAR>seed</VAR>, if
 * <VAR>y</VAR> is zero then it is filled with the first non-zero
 * bits of <VAR>x</VAR>). The algorithm of this generator is
 * entirely implemented in <CODE>nextInt(int)</CODE> core method,
 * the others use it indirectly. The class also contains a method
 * for generation of normally distributed ('Gaussian') pseudo-random
 * numbers.
 **
 * @see UnsignedInt
 * @see UnsignedLong
 * @see JavaConsts
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public class PseudoRandom
 implements ReallyCloneable, Serializable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.1
 */
 private static final long serialVersionUID = -1655238629402664209L;

/**
 * Size of the first 'shift' register in <VAR>seed</VAR>
 * (<CODE>61</CODE>).
 **
 * @see #GEN_B_SIZE
 * @see #seed
 * @see #nextInt(int)
 **
 * @since 1.1
 */
 protected static final int GEN_A_SIZE = 61;

/**
 * Size of the second 'shift' register in the lowest part of
 * <VAR>seed</VAR> (<CODE>3</CODE>).
 **
 * @see #GEN_A_SIZE
 * @see #seed
 * @see #nextInt(int)
 **
 * @since 1.1
 */
 protected static final int GEN_B_SIZE = 3;

/**
 * The internal state associated with <CODE>this</CODE>
 * pseudo-random number generator.
 **
 * <VAR>seed</VAR> (which consists of two shift registers) is
 * initially set by the constructor and modified each time
 * <CODE>this</CODE> generator produces a new value. <VAR>seed</VAR>
 * may be of any value, which may be modified asynchronously (since
 * the algorithm includes the checks for zero values of any or both
 * shift registers).
 **
 * @serial
 **
 * @see PseudoRandom#PseudoRandom(long)
 * @see #clone()
 * @see #nextInt(int)
 * @see #toString()
 */
 protected long seed;

/**
 * Creates a new pseudo-random generator with the predefined initial
 * state.
 **
 * Initial state (<VAR>seed</VAR>) of generator is fully (but not
 * trivially) determined by <VAR>initializer</VAR>. Later, this
 * state is altered only by every (direct or indirect) call of
 * <CODE>nextInt(int)</CODE>. Important notes: if two instances of
 * this class are created with the same value of
 * <VAR>initializer</VAR>, and the same sequence of method calls is
 * made for each, they will generate and return identical sequences
 * of numbers (and these instances will be equal); on the other
 * hand, if output reproducibility of a pseudo-random generator is
 * not required then it may be initialized on the current time.
 **
 * @param initializer
 * the long value which fully determines the output pseudo-random
 * bits sequence of the created generator.
 **
 * @see #nextInt(int)
 * @see #clone()
 * @see #equals(java.lang.Object)
 * @see #toString()
 */
 public PseudoRandom(long initializer)
 {
  initializer = (initializer ^
   0x5DEECE66DL) * JavaConsts.LONG_GOLD_MEDIAN;
  initializer = (initializer >>> (GEN_A_SIZE - 1)) ^
   (initializer << (GEN_B_SIZE + 1));
  if (GEN_A_SIZE + GEN_B_SIZE < JavaConsts.LONG_SIZE)
   initializer &= ~(-1L << (GEN_A_SIZE + GEN_B_SIZE));
  this.seed = initializer;
 }

/**
 * Generates and returns the next uniformly distributed unsigned
 * <CODE>int</CODE> pseudo-random number according to the specified
 * maximum.
 **
 * Returned value is drawn from the bits sequence of
 * <CODE>this</CODE> random number generator. The unsigned result is
 * uniformly distributed in the range from <CODE>0</CODE> to
 * <VAR>unsignedMax</VAR>, inclusive. All <VAR>unsignedMax</VAR>
 * plus one possible <CODE>int</CODE> values are produced with
 * (approximately) equal probability. The hedge 'approximately' is
 * used in the foregoing description only because the method is only
 * approximately an unbiased source of independently chosen bits.
 * This is a core method of the generator. This method alters state
 * of <CODE>this</CODE> generator. Important notes: synchronization
 * is not needed even outside (unless two or more threads use the
 * same pseudo-random generator constructed with some specified
 * initializer), since <VAR>seed</VAR> may be modified in an
 * asynchronous (even non-atomary) way by multiple threads; this
 * method may be overridden in the subclasses.
 **
 * @param unsignedMax
 * the unsigned maximum on the random number to be returned.
 * @return
 * a pseudo-random, uniformly distributed unsigned <CODE>int</CODE>
 * value between <CODE>0</CODE> and <VAR>unsignedMax</VAR>
 * (inclusive).
 **
 * @see #nextBits(int)
 * @see #nextLong(long)
 * @see #nextBytes(byte[], int, int)
 * @see #nextName(int)
 * @see #nextFloat()
 */
 public int nextInt(int unsignedMax)
 {
  if (unsignedMax != 0)
  {
   int value;
   long seed;
   if (((seed = this.seed) & (GEN_A_SIZE + GEN_B_SIZE <
       JavaConsts.LONG_SIZE ? ~(-1L << GEN_A_SIZE) << GEN_B_SIZE :
       -1 << GEN_B_SIZE)) == 0L)
    seed += JavaConsts.LONG_GOLD_MEDIAN;
   if (((int)seed & ~(-1 << GEN_B_SIZE)) == 0)
   {
    long high = seed;
    do
    {
     high >>>= GEN_B_SIZE;
    } while ((value = (int)high & ~(-1 << GEN_B_SIZE)) == 0);
    seed |= value;
   }
   int max = unsignedMax;
   do
   {
    int result = 0;
    unsignedMax = max;
    do
    {
     value = (int)seed & ~(1 << GEN_B_SIZE);
     value ^= (value >> (GEN_B_SIZE - 1)) ^ (value << 1);
     if (GEN_A_SIZE + GEN_B_SIZE < JavaConsts.LONG_SIZE ? (seed &
         (1L << (GEN_A_SIZE + GEN_B_SIZE - 1))) != 0L : seed < 0L)
      value ^= 1 << GEN_B_SIZE;
     seed = (value & ((1 << GEN_B_SIZE) | 1)) ^ (seed << 1);
     result = ((value >> GEN_B_SIZE) ^ value) & 1 | (result << 1);
    } while ((unsignedMax >>>= 1) != 0);
    unsignedMax = result;
   } while (((~max | unsignedMax) & (max - unsignedMax)) < 0);
   if (GEN_A_SIZE + GEN_B_SIZE < JavaConsts.LONG_SIZE)
    seed &= ~(-1L << (GEN_A_SIZE + GEN_B_SIZE));
   this.seed = seed;
  }
  return unsignedMax;
 }

/**
 * Generates and returns the next uniformly distributed unsigned
 * <CODE>long</CODE> pseudo-random number according to the specified
 * maximum.
 **
 * This method uses only <CODE>nextInt(int)</CODE> core method. The
 * unsigned result is uniformly distributed in the range from
 * <CODE>0</CODE> to <VAR>unsignedMax</VAR>, inclusive. All
 * <VAR>unsignedMax</VAR> plus one possible <CODE>long</CODE> values
 * are produced with (approximately) equal probability. In fact,
 * this is a secondary 'core' method.
 **
 * @param unsignedMax
 * the unsigned maximum on the random number to be returned.
 * @return
 * a pseudo-random, uniformly distributed unsigned <CODE>long</CODE>
 * value between <CODE>0</CODE> and <VAR>unsignedMax</VAR>
 * (inclusive).
 **
 * @see #nextInt(int)
 * @see #nextDouble()
 * @see #nextGaussian()
 */
 public long nextLong(long unsignedMax)
 {
  int count, size = 0;
  long value = unsignedMax;
  while ((value & ~JavaConsts.INT_LMASK) != 0L)
  {
   value >>>= JavaConsts.INT_SIZE;
   size += JavaConsts.INT_SIZE;
  }
  if ((count = (int)value) != 0)
   for (size += JavaConsts.INT_SIZE;
        count > 0; count <<= 1, size--);
  do
  {
   value = 0L;
   count = size;
   while ((count -= JavaConsts.INT_SIZE) >= 0)
    value = nextInt(-1) & JavaConsts.INT_LMASK |
     (value << JavaConsts.INT_SIZE);
   if ((count += JavaConsts.INT_SIZE) > 0)
    value = nextInt(~(-1 << count)) | (value << count);
  } while (((~unsignedMax | value) & (unsignedMax - value)) < 0L);
  return value;
 }

/**
 * Generates and returns the next pseudo-random bits sequence packed
 * into <CODE>int</CODE> value.
 **
 * The resulting sequence is in lowest <VAR>count</VAR> bits of the
 * returned value (top bits are set to zero). Each bit of the
 * sequence may be <CODE>0</CODE> or <CODE>1</CODE> with the equal
 * probability. Negative <VAR>count</VAR> is treated as zero. If the
 * sequence is too long (to fit <CODE>int</CODE> value) then it is
 * truncated. This method uses only <CODE>nextInt(int)</CODE>
 * method.
 **
 * @param count
 * the count of bits to be generated.
 * @return
 * a packed sequence of pseudo-random bits.
 **
 * @see #nextInt(int)
 * @see #nextLongBits(int)
 * @see #nextBytes(byte[], int, int)
 **
 * @since 1.1
 */
 public final int nextBits(int count)
 {
  int bits = 0;
  if (count > 0)
  {
   bits = -1;
   if (count < JavaConsts.INT_SIZE)
    bits = ~(-1 << count);
   bits = nextInt(bits);
  }
  return bits;
 }

/**
 * Generates and returns the next pseudo-random bits sequence packed
 * into <CODE>long</CODE> value.
 **
 * The resulting sequence is in lowest <VAR>count</VAR> bits of the
 * returned value (top bits are set to zero). Each bit of the
 * sequence may be <CODE>0</CODE> or <CODE>1</CODE> with the equal
 * probability. Negative <VAR>count</VAR> is treated as zero. If the
 * sequence is too long (to fit <CODE>long</CODE> value) then it is
 * truncated. This method uses only <CODE>nextLong(long)</CODE>
 * method.
 **
 * @param count
 * the count of bits to be generated.
 * @return
 * a packed sequence of pseudo-random bits.
 **
 * @see #nextLong(long)
 * @see #nextBits(int)
 * @see #nextBytes(byte[], int, int)
 **
 * @since 1.1
 */
 public final long nextLongBits(int count)
 {
  long bits = 0L;
  if (count > 0)
  {
   bits = -1L;
   if (count < JavaConsts.LONG_SIZE)
    bits = ~(-1L << count);
   bits = nextLong(bits);
  }
  return bits;
 }

/**
 * Generates pseudo-random bytes and places them into the supplied
 * <CODE>byte</CODE> array at the specified offset.
 **
 * Each byte is uniformly distributed in all its range. Negative
 * <VAR>len</VAR> is treated as zero. If an exception is thrown then
 * generator state and <VAR>bytes</VAR> content remain unchanged.
 * This method uses only <CODE>nextInt(int)</CODE> core method.
 **
 * @param bytes
 * the byte array (must be non-<CODE>null</CODE> and of enough
 * length) in which to put the generated pseudo-random bytes.
 * @param offset
 * the offset (in the supplied byte array) at which to put the
 * generated pseudo-random bytes.
 * @param len
 * the amount of pseudo-random bytes to generate.
 * @exception NullPointerException
 * if <VAR>bytes</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>bytes</VAR> minus
 * <VAR>len</VAR>).
 **
 * @see #nextInt(int)
 * @see #nextBits(int)
 * @see #nextLongBits(int)
 * @see #nextName(int)
 */
 public void nextBytes(byte[] bytes, int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int value = bytes.length;
  if (len > 0)
  {
   value = bytes[offset] | bytes[offset + len - 1];
   do
   {
    bytes[offset++] = (byte)nextInt(JavaConsts.BYTE_MASK);
   } while (--len > 0);
  }
 }

/**
 * Generates and returns the next pseudo-random (file) name.
 **
 * This method is useful for generation of random names for
 * temporary files. The resulting string contains only uniformly
 * distributed characters from the set of '0' through '9' and 'a'
 * through 'z'. Negative <VAR>len</VAR> is treated as zero. If an
 * exception is thrown then generator state remains unchanged. This
 * method uses only <CODE>nextInt(int)</CODE> core method.
 **
 * @param len
 * the amount of characters to generate.
 * @return
 * a string (not <CODE>null</CODE>, with <CODE>length()</CODE> of
 * <CODE>max(len, 0)</CODE>), which is just created and contains
 * only the pseudo-random characters from the set denoted above.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #nextInt(int)
 * @see #nextBytes(byte[], int, int)
 **
 * @since 2.0
 */
 public String nextName(int len)
 {
  if (len <= 0)
   len = 0;
  char[] chars = new char[len];
  for (int value; len > 0; chars[--len] = (char)value)
   if ((value = nextInt(('9' - '0' + 1) + ('z' - 'a')) + '0') > '9')
    value += 'a' - '9' - 1;
  return new String(chars);
 }

/**
 * Generates and returns the next uniformly distributed
 * <CODE>float</CODE> pseudo-random number in the range from
 * <CODE>0</CODE> (inclusive) to <CODE>1</CODE> (exclusive).
 **
 * All possible floating-point values from the denoted range are
 * produced with (approximately) equal probability. This method uses
 * only <CODE>nextInt(int)</CODE> core method (to fill up the
 * mantissa of the floating-point value).
 **
 * @return
 * a pseudo-random, uniformly distributed <CODE>float</CODE> value
 * between <CODE>0</CODE> (inclusive) and <CODE>1</CODE>
 * (exclusive).
 **
 * @see #nextInt(int)
 * @see #nextDouble()
 * @see #nextGaussian()
 */
 public final float nextFloat()
 {
  return (float)nextInt(JavaConsts.FLOAT_M_MASK) /
   (JavaConsts.FLOAT_M_MASK + 1);
 }

/**
 * Generates and returns the next uniformly distributed
 * <CODE>double</CODE> pseudo-random number in the range from
 * <CODE>0</CODE> (inclusive) to <CODE>1</CODE> (exclusive).
 **
 * All possible floating-point values from the denoted range are
 * produced with (approximately) equal probability. This method uses
 * only <CODE>nextLong(long)</CODE> method (to fill up the mantissa
 * of the floating-point value).
 **
 * @return
 * a pseudo-random, uniformly distributed <CODE>double</CODE> value
 * between <CODE>0</CODE> (inclusive) and <CODE>1</CODE>
 * (exclusive).
 **
 * @see #nextLong(long)
 * @see #nextFloat()
 * @see #nextGaussian()
 */
 public final double nextDouble()
 {
  return (double)nextLong(JavaConsts.DOUBLE_M_MASK) /
   (JavaConsts.DOUBLE_M_MASK + 1L);
 }

/**
 * Generates and returns the next normally distributed
 * <CODE>double</CODE> pseudo-random number.
 **
 * Here, so called 'Polar Algorithm' is used to produce normally
 * distributed ('Gaussian') pseudo-random numbers with the standard
 * mean and deviation (mean <CODE>0</CODE> and deviation
 * <CODE>1</CODE>). The method uses only <CODE>nextLong(long)</CODE>
 * method (to fill up the mantissa of the floating-point values),
 * and <CODE>log(double)</CODE>, <CODE>sqrt(double)</CODE> functions
 * of <CODE>Math</CODE> class (to compute 'Gaussian' numbers).
 * Important notes: most of all the produced Gaussian numbers are in
 * the range from <CODE>-6</CODE> to <CODE>6</CODE>.
 **
 * @return
 * a pseudo-random, normally distributed <CODE>double</CODE> value
 * with mean <CODE>0</CODE> and deviation <CODE>1</CODE>.
 **
 * @see #nextDouble()
 * @see #nextLong(long)
 */
 public double nextGaussian()
 {
  double s, v, w;
  do
  {
   v = (double)nextLong(JavaConsts.DOUBLE_M_MASK) /
    ((JavaConsts.DOUBLE_M_MASK >>> 1) + 1L) - 1.0D;
   w = (double)nextLong(JavaConsts.DOUBLE_M_MASK) /
    ((JavaConsts.DOUBLE_M_MASK >>> 1) + 1L) - 1.0D;
  } while ((s = v * v + w * w) <= 0.0D || s >= 1.0D);
  s = Math.sqrt(-2.0D * Math.log(s) / s);
  return v * s; // (w * s) may be used as another independent result
 }

/**
 * Creates and returns a copy of <CODE>this</CODE> object.
 **
 * This method creates a new instance of the class of this object
 * and initializes its <VAR>seed</VAR> value with the same as
 * <VAR>seed</VAR> value of <CODE>this</CODE> object.
 **
 * @return
 * a copy (not <CODE>null</CODE> and != <CODE>this</CODE>) of
 * <CODE>this</CODE> instance.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see PseudoRandom#PseudoRandom(long)
 * @see #equals(java.lang.Object)
 */
 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof PseudoRandom && obj != this)
    return obj;
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * Returns a hash code value for the object.
 **
 * This method returns <VAR>seed</VAR> value 'squeezed' (hashed) to
 * value of <CODE>int</CODE> type.
 **
 * @return
 * a hash code value for <CODE>this</CODE> object.
 **
 * @see #equals(java.lang.Object)
 */
 public int hashCode()
 {
  long seed = this.seed;
  return (int)((seed >> (JavaConsts.INT_SIZE - 1)) >> 1) ^
   (int)seed;
 }

/**
 * Indicates whether <CODE>this</CODE> object is equal to the
 * specified one.
 **
 * This method returns <CODE>true</CODE> if and only if
 * <VAR>obj</VAR> is instance of this class and its <VAR>seed</VAR>
 * value is the same as <VAR>seed</VAR> value of <CODE>this</CODE>
 * object.
 **
 * @param obj
 * the object (may be <CODE>null</CODE>) with which to compare.
 * @return
 * <CODE>true</CODE> if and only if <CODE>this</CODE> value is the
 * same as <VAR>obj</VAR> value.
 **
 * @see PseudoRandom#PseudoRandom(long)
 * @see #clone()
 * @see #hashCode()
 */
 public boolean equals(Object obj)
 {
  return obj == this || obj instanceof PseudoRandom &&
   ((PseudoRandom)obj).seed == this.seed;
 }

/**
 * Returns the string representation of the object.
 **
 * This method returns the hexadecimal (with '0x' prefix)
 * zero-padded representation of <VAR>seed</VAR>.
 **
 * @return
 * the string representation (not <CODE>null</CODE>, with non-zero
 * <CODE>length()</CODE>) of <CODE>this</CODE> object.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 */
 public String toString()
 {
  int digit, offset;
  long seed = this.seed;
  if (GEN_A_SIZE + GEN_B_SIZE < JavaConsts.LONG_SIZE)
   seed &= ~(-1L << (GEN_A_SIZE + GEN_B_SIZE));
  char[] chars = new char[offset =
   ((GEN_A_SIZE + GEN_B_SIZE - 1) >> 2) + 3];
  do
  {
   if ((digit = (int)seed & ((1 << 4) - 1)) > '9' - '0')
    digit += 'A' - '9' - 1;
   chars[--offset] = (char)(digit + '0');
   seed >>>= 4;
  } while (offset > 2);
  chars[1] = 'x';
  chars[0] = '0';
  return new String(chars);
 }
}

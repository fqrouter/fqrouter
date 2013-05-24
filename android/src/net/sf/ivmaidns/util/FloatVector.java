/*
 * @(#) src/net/sf/ivmaidns/util/FloatVector.java --
 * Class for 'float' array wrappers.
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

import java.io.InvalidObjectException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Class for 'float' array wrappers.
 **
 * This class wraps a primitive <CODE>float</CODE>-type array, and
 * has the possibility to resize (when required) the wrapped array.
 * This class supports cloning, serialization and comparison of its
 * instances. In addition, the class contains <CODE>static</CODE>
 * methods for <CODE>float</CODE> arrays resizing, filling in,
 * reversing, vector arithmetics (addition, subtraction,
 * multiplication by a value, scalar multiplication, polynome
 * evaluation), elements summing and non-zero elements counting,
 * linear/binary searching in for a value or sequence,
 * natural/binary equality testing, mismatches counting,
 * 'less-equal-greater' comparison, sorting, and 'to-string'
 * conversion.
 **
 * @see ByteVector
 * @see CharVector
 * @see DoubleVector
 * @see IntVector
 * @see LongVector
 * @see ShortVector
 * @see BooleanVector
 * @see ObjectVector
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public final class FloatVector
 implements ReallyCloneable, Serializable, Indexable, Sortable,
            Verifiable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.8
 */
 private static final long serialVersionUID = 1170284172753300166L;

/**
 * A constant initialized with an instance of empty
 * <CODE>float</CODE> array.
 **
 * @see #array
 */
 protected static final float[] EMPTY = {};

/**
 * The wrapped (encapsulated) custom <CODE>float</CODE> array.
 **
 * <VAR>array</VAR> must be non-<CODE>null</CODE>.
 **
 * @serial
 **
 * @see #EMPTY
 * @see FloatVector#FloatVector()
 * @see FloatVector#FloatVector(int)
 * @see FloatVector#FloatVector(float[])
 * @see #setArray(float[])
 * @see #array()
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, float)
 * @see #getFloatAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #integrityCheck()
 */
 protected float[] array;

/**
 * Constructs an empty <CODE>float</CODE> vector.
 **
 * This constructor is used for the creation of a resizable vector.
 * The length of such a vector is changed only by
 * <CODE>resize(int)</CODE> and <CODE>ensureSize(int)</CODE>
 * methods.
 **
 * @see FloatVector#FloatVector(int)
 * @see FloatVector#FloatVector(float[])
 * @see #array()
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, float)
 * @see #getFloatAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #toString()
 */
 public FloatVector()
 {
  this.array = EMPTY;
 }

/**
 * Constructs a new <CODE>float</CODE> vector of the specified
 * length.
 **
 * This constructor is typically used for the creation of a vector
 * with a fixed size. All elements of the created vector are set to
 * zero.
 **
 * @param size
 * the initial length (unsigned) of the vector to be created.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see FloatVector#FloatVector()
 * @see FloatVector#FloatVector(float[])
 * @see #array()
 * @see #length()
 * @see #setAt(int, float)
 * @see #getFloatAt(int)
 * @see #copyAt(int, int, int)
 * @see #fill(float[], int, int, float)
 * @see #clone()
 * @see #toString()
 */
 public FloatVector(int size)
 {
  if (size < 0)
   size = -1 >>> 1;
  this.array = new float[size];
 }

/**
 * Constructs a new <CODE>float</CODE> array wrapper.
 **
 * This constructor is used for the creation of a vector which wraps
 * the specified array (without copying it). The wrapped array may
 * be further replaced with another one only by
 * <CODE>setArray(float[])</CODE> and by <CODE>resize(int)</CODE>,
 * <CODE>ensureSize(int)</CODE> methods.
 **
 * @param array
 * the <CODE>float</CODE> array (must be non-<CODE>null</CODE>) to
 * be wrapped.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see FloatVector#FloatVector()
 * @see FloatVector#FloatVector(int)
 * @see #setArray(float[])
 * @see #array()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, float)
 * @see #getFloatAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #toString()
 **
 * @since 2.0
 */
 public FloatVector(float[] array)
  throws NullPointerException
 {
  int len;
  len = array.length;
  this.array = array;
 }

/**
 * Sets another array to be wrapped by <CODE>this</CODE> vector.
 **
 * Important notes: <CODE>resize(int)</CODE> and
 * <CODE>ensureSize(int)</CODE> methods may change the array to be
 * wrapped too (but only with its copy of a different length); this
 * method does not copy <VAR>array</VAR>. If an exception is thrown
 * then <CODE>this</CODE> vector remains unchanged.
 **
 * @param array
 * the <CODE>float</CODE> array (must be non-<CODE>null</CODE>) to
 * be wrapped.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see FloatVector#FloatVector()
 * @see FloatVector#FloatVector(float[])
 * @see #array()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, float)
 * @see #getFloatAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 **
 * @since 2.0
 */
 public void setArray(float[] array)
  throws NullPointerException
 {
  int len;
  len = array.length;
  this.array = array;
 }

/**
 * Returns array wrapped by <CODE>this</CODE> vector.
 **
 * Important notes: this method does not copy <VAR>array</VAR>.
 **
 * @return
 * the <CODE>float</CODE> array (not <CODE>null</CODE>), which is
 * wrapped.
 **
 * @see FloatVector#FloatVector(float[])
 * @see #setArray(float[])
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 **
 * @since 1.8
 */
 public final float[] array()
 {
  return this.array;
 }

/**
 * Returns the number of elements in <CODE>this</CODE> vector.
 **
 * The result is the same as <CODE>length</CODE> of
 * <CODE>array()</CODE>.
 **
 * @return
 * the length (non-negative value) of <CODE>this</CODE> vector.
 **
 * @see #setArray(float[])
 * @see #array()
 * @see #setAt(int, float)
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #getFloatAt(int)
 * @see #getAt(int)
 **
 * @since 1.8
 */
 public int length()
 {
  return this.array.length;
 }

/**
 * Returns the wrapped value of the element at the specified index.
 **
 * The result is the same as of
 * <CODE>new Float(array()[index])</CODE>.
 **
 * @param index
 * the index (must be in the range) at which to return an element.
 * @return
 * an element (instance of <CODE>Float</CODE>) at <VAR>index</VAR>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #getFloatAt(int)
 * @see #array()
 * @see #length()
 */
 public Object getAt(int index)
  throws ArrayIndexOutOfBoundsException
 {
  return new Float(this.array[index]);
 }

/**
 * Returns value of the element at the specified index.
 **
 * The result is the same as of <CODE>array()[index]</CODE>.
 **
 * @param index
 * the index (must be in the range) at which to return an element.
 * @return
 * a <CODE>float</CODE> element at <VAR>index</VAR>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 **
 * @see #array()
 * @see #length()
 * @see #setAt(int, float)
 * @see #resize(int)
 * @see #ensureSize(int)
 */
 public final float getFloatAt(int index)
  throws ArrayIndexOutOfBoundsException
 {
  return this.array[index];
 }

/**
 * Assigns a new value to the element at the specified index.
 **
 * If an exception is thrown then <CODE>this</CODE> vector remains
 * unchanged.
 **
 * @param index
 * the index (must be in the range) at which to assign a new value.
 * @param value
 * the value to be assigned.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 **
 * @see #setArray(float[])
 * @see #array()
 * @see #length()
 * @see #getFloatAt(int)
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #copyAt(int, int, int)
 * @see #fill(float[], int, int, float)
 */
 public void setAt(int index, float value)
  throws ArrayIndexOutOfBoundsException
 {
  this.array[index] = value;
 }

/**
 * Copies a region of values at one offset to another offset in
 * <CODE>this</CODE> vector.
 **
 * Copying is performed here through
 * <CODE>arraycopy(Object, int, Object, int, int)</CODE> method of
 * <CODE>System</CODE> class. Negative <VAR>len</VAR> is treated as
 * zero. If an exception is thrown then <CODE>this</CODE> vector
 * remains unchanged.
 **
 * @param srcOffset
 * the source first index (must be in the range) of the region to be
 * copied.
 * @param destOffset
 * the first index (must be in the range) of the region copy
 * destination.
 * @param len
 * the length of the region to be copied.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>srcOffset</VAR> is
 * negative or is greater than <CODE>length()</CODE> minus
 * <VAR>len</VAR>, or <VAR>destOffset</VAR> is negative or is
 * greater than <CODE>length()</CODE> minus <VAR>len</VAR>).
 **
 * @see #array()
 * @see #length()
 * @see #setAt(int, float)
 * @see #getFloatAt(int)
 * @see #resize(int)
 * @see #ensureSize(int)
 */
 public void copyAt(int srcOffset, int destOffset, int len)
  throws ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   float[] array = this.array;
   System.arraycopy(array, srcOffset, array, destOffset, len);
  }
 }

/**
 * Resizes <CODE>this</CODE> vector.
 **
 * The result is the same as of
 * <CODE>setArray(resize(array(), size))</CODE>. This method changes
 * the length of <CODE>this</CODE> vector to the specified one.
 * Important notes: if size (length) of the vector grows then its
 * new elements are set to zero. If an exception is thrown then
 * <CODE>this</CODE> vector remains unchanged.
 **
 * @param size
 * the (unsigned) length of <CODE>this</CODE> vector to set.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see FloatVector#FloatVector(int)
 * @see #setArray(float[])
 * @see #array()
 * @see #length()
 * @see #ensureSize(int)
 * @see #resize(float[], int)
 */
 public void resize(int size)
 {
  int len;
  float[] array = this.array;
  if ((len = array.length) != size)
  {
   float[] newArray = EMPTY;
   if (size != 0)
   {
    if (len > size)
     if (size < 0)
      size = -1 >>> 1;
      else len = size;
    System.arraycopy(array, 0, newArray = new float[size], 0, len);
   }
   this.array = newArray;
  }
 }

/**
 * Ensures the size (capacity) of <CODE>this</CODE> vector.
 **
 * The result is the same as of
 * <CODE>setArray(ensureSize(array(), size))</CODE>. This method
 * changes (only if <VAR>size</VAR> is greater than
 * <CODE>length()</CODE>) the length of <CODE>this</CODE> vector to
 * a value not less than <VAR>size</VAR>. Important notes: if size
 * (length) of the vector grows then its new elements are set to
 * zero. If an exception is thrown then <CODE>this</CODE> vector
 * remains unchanged.
 **
 * @param size
 * the (unsigned) length of <CODE>this</CODE> vector to be ensured.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #array()
 * @see #length()
 * @see #setAt(int, float)
 * @see #resize(int)
 * @see #ensureSize(float[], int)
 */
 public void ensureSize(int size)
 {
  int len;
  float[] array = this.array, newArray;
  if ((((len = array.length) - size) | size) < 0)
  {
   if (size < 0)
    size = -1 >>> 1;
   if ((len += len >> 1) >= size)
    size = len;
   System.arraycopy(array, 0,
    newArray = new float[size], 0, array.length);
   this.array = newArray;
  }
 }

/**
 * Resizes a given array.
 **
 * This method 'changes' (creates a new array and copies the content
 * to it) the length of the specified array to the specified one.
 * Important notes: <VAR>array</VAR> elements are not changed; if
 * <CODE>length</CODE> of <VAR>array</VAR> is the same as
 * <VAR>size</VAR> then <VAR>array</VAR> is returned else
 * <VAR>array</VAR> content is copied into the result (all new
 * elements are set to zero).
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be resized.
 * @param size
 * the (unsigned) length of the array to set.
 * @return
 * the resized array (not <CODE>null</CODE>, with
 * <CODE>length</CODE> equal to <VAR>size</VAR>).
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #resize(int)
 * @see #ensureSize(float[], int)
 * @see #fill(float[], int, int, float)
 */
 public static final float[] resize(float[] array, int size)
  throws NullPointerException
 {
  int len;
  if ((len = array.length) != size)
  {
   float[] newArray = EMPTY;
   if (size != 0)
   {
    if (len > size)
     if (size < 0)
      size = -1 >>> 1;
      else len = size;
    System.arraycopy(array, 0, newArray = new float[size], 0, len);
   }
   array = newArray;
  }
  return array;
 }

/**
 * Ensures the length (capacity) of a given array.
 **
 * This method 'grows' (only if <VAR>size</VAR> is greater than
 * <CODE>length</CODE> of <VAR>array</VAR>) the length of
 * <VAR>array</VAR>. Important notes: <VAR>array</VAR> elements are
 * not changed; if <CODE>length</CODE> of <VAR>array</VAR> is
 * greater or the same as <VAR>size</VAR> then <VAR>array</VAR> is
 * returned else <VAR>array</VAR> content is copied into the result
 * (all new elements are set to zero).
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be length-ensured.
 * @param size
 * the (unsigned) length of the array to ensure.
 * @return
 * the length-ensured array (not <CODE>null</CODE>, with
 * <CODE>length</CODE> not less than <VAR>size</VAR>).
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #ensureSize(int)
 * @see #resize(float[], int)
 * @see #fill(float[], int, int, float)
 */
 public static final float[] ensureSize(float[] array, int size)
  throws NullPointerException
 {
  int len;
  if ((((len = array.length) - size) | size) < 0)
  {
   if (size < 0)
    size = -1 >>> 1;
   if ((len += len >> 1) >= size)
    size = len;
   float[] newArray;
   System.arraycopy(array, 0,
    newArray = new float[size], 0, array.length);
   array = newArray;
  }
  return array;
 }

/**
 * Fills in the region of a given array with the specified value.
 **
 * All the elements in the specified region of <VAR>array</VAR> are
 * set to <VAR>value</VAR>. Negative <VAR>len</VAR> is treated as
 * zero. If an exception is thrown then <VAR>array</VAR> remains
 * unchanged. Else <VAR>array</VAR> content is altered. Important
 * notes: region filling is performed using
 * <CODE>arraycopy(Object, int, Object, int, int)</CODE> method of
 * <CODE>System</CODE> class.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be filled in.
 * @param offset
 * the first index (must be in the range) of the region to fill in.
 * @param len
 * the length of the region to be filled.
 * @param value
 * the value to fill with.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>array</VAR> minus
 * <VAR>len</VAR>).
 **
 * @see #array()
 * @see #copyAt(int, int, int)
 * @see #indexOf(float[], int, int, int, float[])
 * @see #lastIndexOf(float[], int, int, int, float[])
 * @see #toString(float[], int, int, char)
 * @see #quickSort(float[], int, int)
 * @see #binarySearch(float[], int, int, float)
 **
 * @since 2.0
 */
 public static final void fill(float[] array, int offset, int len,
         float value)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int next = array.length, block;
  if (len > 0)
  {
   float temp;
   temp = array[(block = offset) + (--len)];
   if ((next = len) > 2)
    next = 3;
   do
   {
    array[block++] = value;
   } while (next-- > 0);
   len--;
   next = 2;
   while ((len -= next) > 0)
   {
    if ((block = next <<= 1) >= len)
     next = len;
    System.arraycopy(array, offset, array, offset + block, next);
   }
  }
 }

/**
 * Reverses the elements order in a given array.
 **
 * The first element is exchanged with the least one, the second one
 * is exchanged with the element just before the last one, etc.
 * <VAR>array</VAR> content is altered.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be reversed.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #addTo(float[], float[])
 * @see #subtractFrom(float[], float[])
 * @see #countNonZero(float[])
 * @see #indexOf(float, int, float[])
 * @see #lastIndexOf(float, int, float[])
 * @see #hashCode(float[])
 * @see #equals(float[], float[])
 * @see #compare(float[], int, int, float[], int, int)
 * @see #mismatches(float[], int, float[], int, int)
 */
 public static final void reverse(float[] array)
  throws NullPointerException
 {
  int offset = 0, len = array.length;
  while (--len > offset)
  {
   float value = array[offset];
   array[offset++] = array[len];
   array[len] = value;
  }
 }

/**
 * Adds a given vector (array) to another one.
 **
 * Every element of the second array (missing element is treated to
 * be zero) is added to the corresponding element (if not missing)
 * of the first array. <VAR>arrayA</VAR> content is altered.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>) to be added to.
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>) to add.
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #reverse(float[])
 * @see #subtractFrom(float[], float[])
 * @see #multiplyBy(float[], float)
 * @see #sumOf(float[], int, int)
 * @see #scalarMul(float[], float[])
 * @see #polynome(double, float[])
 * @see #mathEquals(float[], float[])
 **
 * @since 2.0
 */
 public static final void addTo(float[] arrayA, float[] arrayB)
  throws NullPointerException
 {
  int offset = arrayA.length, len;
  if (arrayA != arrayB)
  {
   if ((len = arrayB.length) <= offset)
    offset = len;
   while (offset-- > 0)
    arrayA[offset] += arrayB[offset];
  }
  while (offset-- > 0)
   arrayA[offset] *= 2;
 }

/**
 * Subtracts a given vector (array) from another one.
 **
 * Every element of the second array (missing element is treated to
 * be zero) is subtracted from the corresponding element (if not
 * missing) of the first array. <VAR>arrayA</VAR> content is
 * altered.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>) to be subtracted
 * from.
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>) to subtract.
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #fill(float[], int, int, float)
 * @see #reverse(float[])
 * @see #addTo(float[], float[])
 * @see #multiplyBy(float[], float)
 * @see #sumOf(float[], int, int)
 * @see #scalarMul(float[], float[])
 * @see #polynome(double, float[])
 * @see #mathEquals(float[], float[])
 **
 * @since 2.0
 */
 public static final void subtractFrom(float[] arrayA,
         float[] arrayB)
  throws NullPointerException
 {
  int offset = arrayA.length, len;
  if (arrayA != arrayB)
  {
   if ((len = arrayB.length) <= offset)
    offset = len;
   while (offset-- > 0)
    arrayA[offset] -= arrayB[offset];
  }
  while (offset-- > 0)
   arrayA[offset] = 0.0F;
 }

/**
 * Multiplies a given vector (array) by a value.
 **
 * Every element of the specified array is multiplied by
 * <VAR>value</VAR>. <VAR>array</VAR> content is altered.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be multiplied.
 * @param value
 * the value to multiply by.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #fill(float[], int, int, float)
 * @see #reverse(float[])
 * @see #addTo(float[], float[])
 * @see #subtractFrom(float[], float[])
 * @see #sumOf(float[], int, int)
 * @see #scalarMul(float[], float[])
 * @see #polynome(double, float[])
 * @see #countNonZero(float[])
 * @see #mathEquals(float[], float[])
 **
 * @since 2.0
 */
 public static final void multiplyBy(float[] array, float value)
  throws NullPointerException
 {
  int offset = array.length;
  if (value != 1.0F)
   while (offset-- > 0)
    array[offset] *= value;
 }

/**
 * Multiplies two given vectors (arrays) in a scalar way.
 **
 * Every element of the first array is multiplied (as a
 * <CODE>double</CODE> value) by the corresponding element of the
 * second array (missing element is treated to be zero) and the
 * results of these multiplications are summed together.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>) to multiply.
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>) to multiply.
 * @return
 * the multiplication result (<CODE>double</CODE> value).
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #addTo(float[], float[])
 * @see #subtractFrom(float[], float[])
 * @see #multiplyBy(float[], float)
 * @see #sumOf(float[], int, int)
 * @see #polynome(double, float[])
 * @see #mathEquals(float[], float[])
 * @see #mismatches(float[], int, float[], int, int)
 **
 * @since 2.0
 */
 public static final double scalarMul(float[] arrayA,
         float[] arrayB)
  throws NullPointerException
 {
  int offset;
  double result = 0.0D;
  int len = arrayA.length;
  if ((offset = arrayB.length) >= len)
   offset = len;
  while (offset-- > 0)
   result += (double)arrayA[offset] * arrayB[offset];
  return result;
 }

/**
 * Computes the result of substitution of a given value into the
 * polynome specified by its coefficients.
 **
 * The result is the same as of
 * <CODE>sum(array[index] * power(value, index))</CODE>. If
 * <CODE>length</CODE> of <VAR>array</VAR> is zero then
 * <CODE>0</CODE> is returned.
 **
 * @param value
 * the <CODE>double</CODE> value to be substituted.
 * @param array
 * the array (must be non-<CODE>null</CODE>) of the polynome
 * coefficients, arranged by their weight.
 * @return
 * the result (<CODE>double</CODE> value) of the substitution.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #addTo(float[], float[])
 * @see #subtractFrom(float[], float[])
 * @see #multiplyBy(float[], float)
 * @see #scalarMul(float[], float[])
 * @see #sumOf(float[], int, int)
 * @see #countNonZero(float[])
 * @see #mathEquals(float[], float[])
 **
 * @since 2.0
 */
 public static final double polynome(double value, float[] array)
  throws NullPointerException
 {
  int offset = array.length - 1;
  double result = 0.0D;
  if (offset >= 0)
  {
   if (value == 0.0D)
    offset = 0;
   for (result = array[offset]; offset > 0;
        result = result * value + array[--offset]);
  }
  return result;
 }

/**
 * Sums the elements in the region of a given array.
 **
 * The elements in the region are summed as <CODE>double</CODE>
 * values. Negative <VAR>len</VAR> is treated as zero.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) which elements to be
 * summed.
 * @param offset
 * the first index (must be in the range) of the region.
 * @param len
 * the length of the region.
 * @return
 * the sum (<CODE>double</CODE> value) for a given region.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>array</VAR> minus
 * <VAR>len</VAR>).
 **
 * @see #array()
 * @see #fill(float[], int, int, float)
 * @see #addTo(float[], float[])
 * @see #subtractFrom(float[], float[])
 * @see #multiplyBy(float[], float)
 * @see #countNonZero(float[])
 * @see #mathEquals(float[], float[])
 * @see #mismatches(float[], int, float[], int, int)
 * @see #scalarMul(float[], float[])
 * @see #polynome(double, float[])
 **
 * @since 2.0
 */
 public static final double sumOf(float[] array,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  double result = 0.0D;
  while (len-- > 0)
   result += array[offset++];
  len = array.length;
  return result;
 }

/**
 * Count non-zero elements in a given array.
 **
 * This method returns the count of elements of <VAR>array</VAR>
 * which are not equal to zero (natural comparison is used).
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to count non-zero
 * elements in.
 * @return
 * the count (non-negative and not greater than <CODE>length</CODE>
 * of <VAR>array</VAR>) of non-zero elements.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #fill(float[], int, int, float)
 * @see #sumOf(float[], int, int)
 * @see #scalarMul(float[], float[])
 * @see #polynome(double, float[])
 * @see #mathEquals(float[], float[])
 * @see #compare(float[], int, int, float[], int, int)
 * @see #mismatches(float[], int, float[], int, int)
 **
 * @since 2.0
 */
 public static final int countNonZero(float[] array)
  throws NullPointerException
 {
  int offset = array.length, count = 0;
  while (offset-- > 0)
   if (array[offset] != 0.0F)
    count++;
  return count;
 }

/**
 * Searches forward for value in a given array.
 **
 * Negative <VAR>index</VAR> is treated as zero, too big
 * <VAR>index</VAR> is treated as <CODE>length</CODE> of
 * <VAR>array</VAR>. If <VAR>value</VAR> is not found then the
 * result is <CODE>-1</CODE>. Important notes: any two values are
 * treated as equal if and only if their binary representations are
 * equal.
 **
 * @param value
 * the value to sequentially search for.
 * @param index
 * the first index, from which to begin forward searching.
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be searched in.
 * @return
 * the index (non-negative) of the found value or <CODE>-1</CODE>
 * (if not found).
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #lastIndexOf(float, int, float[])
 * @see #indexOf(float[], int, int, int, float[])
 * @see #binarySearch(float[], int, int, float)
 * @see #equals(float[], float[])
 * @see #compare(float[], int, int, float[], int, int)
 */
 public static final int indexOf(float value, int index,
         float[] array)
  throws NullPointerException
 {
  if (index <= 0)
   index = 0;
  index--;
  int len = array.length, bits;
  if (value == 0.0F)
  {
   bits = Float.floatToIntBits(value);
   while (++index < len && ((value = array[index]) != 0.0F ||
          Float.floatToIntBits(value) != bits));
  }
   else if (value != value)
   {
    bits = Float.floatToIntBits(value);
    do
    {
     if (++index >= len)
      break;
     value = array[index];
    } while (value == value || Float.floatToIntBits(value) != bits);
   }
    else while (++index < len && array[index] != value);
  if (index >= len)
   index = -1;
  return index;
 }

/**
 * Searches backward for value in a given array.
 **
 * Negative <VAR>index</VAR> is treated as <CODE>-1</CODE>, too big
 * <VAR>index</VAR> is treated as <CODE>length</CODE> of
 * <VAR>array</VAR> minus one. If <VAR>value</VAR> is not found then
 * the result is <CODE>-1</CODE>. Important notes: any two values
 * are treated as equal if and only if their binary representations
 * are equal.
 **
 * @param value
 * the value to sequentially search for.
 * @param index
 * the first index, from which to begin backward searching.
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be searched in.
 * @return
 * the index (non-negative) of the found value or <CODE>-1</CODE>
 * (if not found).
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #indexOf(float, int, float[])
 * @see #lastIndexOf(float[], int, int, int, float[])
 * @see #binarySearch(float[], int, int, float)
 * @see #reverse(float[])
 * @see #equals(float[], float[])
 * @see #compare(float[], int, int, float[], int, int)
 */
 public static final int lastIndexOf(float value, int index,
         float[] array)
  throws NullPointerException
 {
  if (index < 0)
   index = -1;
  int bits;
  if ((bits = array.length) <= index)
   index = bits - 1;
  index++;
  if (value == 0.0F)
  {
   bits = Float.floatToIntBits(value);
   while (index-- > 0 && ((value = array[index]) != 0.0F ||
          Float.floatToIntBits(value) != bits));
  }
   else if (value != value)
   {
    bits = Float.floatToIntBits(value);
    do
    {
     if (--index < 0)
      break;
     value = array[index];
    } while (value == value || Float.floatToIntBits(value) != bits);
   }
    else while (index-- > 0 && array[index] != value);
  return index;
 }

/**
 * Searches forward for the specified sequence in a given array.
 **
 * The searched sequence of values is specified by
 * <VAR>subArray</VAR>, <VAR>offset</VAR> and <VAR>len</VAR>.
 * Negative <VAR>len</VAR> is treated as zero. Negative
 * <VAR>index</VAR> is treated as zero, too big <VAR>index</VAR> is
 * treated as <CODE>length</CODE> of <VAR>array</VAR>. If the
 * sequence is not found then the result is <CODE>-1</CODE>.
 * Important notes: any two elements are treated as equal if and
 * only if their binary representations are equal.
 **
 * @param subArray
 * the array (must be non-<CODE>null</CODE>) specifying the sequence
 * of values to search for.
 * @param offset
 * the offset (must be in the range) of the sequence in
 * <VAR>subArray</VAR>.
 * @param len
 * the length of the sequence.
 * @param index
 * the first index, from which to begin forward searching.
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be searched in.
 * @return
 * the index (non-negative) of the found sequence or <CODE>-1</CODE>
 * (if not found).
 * @exception NullPointerException
 * if <VAR>subArray</VAR> is <CODE>null</CODE> or <VAR>array</VAR>
 * is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>subArray</VAR>
 * minus <VAR>len</VAR>).
 **
 * @see #array()
 * @see #indexOf(float, int, float[])
 * @see #lastIndexOf(float[], int, int, int, float[])
 * @see #equals(float[], float[])
 * @see #compare(float[], int, int, float[], int, int)
 */
 public static final int indexOf(float[] subArray,
         int offset, int len, int index, float[] array)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int curOffset = subArray.length, arrayLen = array.length;
  if (index <= 0)
   index = 0;
  if (len > 0)
  {
   arrayLen -= len;
   float value = subArray[offset];
   float temp = subArray[len += offset - 1];
   int bits = Float.floatToIntBits(value);
   index--;
   while (++index <= arrayLen)
    if ((temp = array[index]) == value &&
        (value != 0.0F || Float.floatToIntBits(temp) == bits) ||
        value != value && Float.floatToIntBits(temp) == bits)
    {
     curOffset = offset;
     int curIndex = index;
     while (++curOffset <= len)
     {
      float curValue = subArray[curOffset];
      if ((temp = array[++curIndex]) != curValue)
      {
       if (curValue == curValue || Float.floatToIntBits(temp) !=
           Float.floatToIntBits(curValue))
        break;
      }
       else if (curValue == 0.0F && Float.floatToIntBits(temp) !=
                Float.floatToIntBits(curValue))
        break;
     }
     if (curOffset > len)
      break;
    }
  }
  if (index > arrayLen)
   index = -1;
  return index;
 }

/**
 * Searches backward for the specified sequence in a given array.
 **
 * The searched sequence of values is specified by
 * <VAR>subArray</VAR>, <VAR>offset</VAR> and <VAR>len</VAR>.
 * Negative <VAR>len</VAR> is treated as zero. Negative
 * <VAR>index</VAR> is treated as <CODE>-1</CODE>, too big
 * <VAR>index</VAR> is treated as <CODE>length</CODE> of
 * <VAR>array</VAR> minus one. If the sequence is not found then the
 * result is <CODE>-1</CODE>. Important notes: any two elements are
 * treated as equal if and only if their binary representations are
 * equal.
 **
 * @param subArray
 * the array (must be non-<CODE>null</CODE>) specifying the sequence
 * of values to search for.
 * @param offset
 * the offset (must be in the range) of the sequence in
 * <VAR>subArray</VAR>.
 * @param len
 * the length of the sequence.
 * @param index
 * the first index, from which to begin backward searching.
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be searched in.
 * @return
 * the index (non-negative) of the found sequence or <CODE>-1</CODE>
 * (if not found).
 * @exception NullPointerException
 * if <VAR>subArray</VAR> is <CODE>null</CODE> or <VAR>array</VAR>
 * is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>subArray</VAR>
 * minus <VAR>len</VAR>).
 **
 * @see #array()
 * @see #lastIndexOf(float, int, float[])
 * @see #indexOf(float[], int, int, int, float[])
 * @see #equals(float[], float[])
 * @see #compare(float[], int, int, float[], int, int)
 */
 public static final int lastIndexOf(float[] subArray,
         int offset, int len, int index, float[] array)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int curOffset = subArray.length, arrayLen;
  if (len <= 0)
   len = 0;
  if ((arrayLen = array.length - len) <= index)
   index = arrayLen;
  if (index < 0)
   index = -1;
  if (len > 0)
  {
   float value = subArray[offset];
   float temp = subArray[len += offset - 1];
   int bits = Float.floatToIntBits(value);
   index++;
   while (index-- > 0)
    if ((temp = array[index]) == value &&
        (value != 0.0F || Float.floatToIntBits(temp) == bits) ||
        value != value && Float.floatToIntBits(temp) == bits)
    {
     curOffset = offset;
     arrayLen = index;
     while (++curOffset <= len)
     {
      float curValue = subArray[curOffset];
      if ((temp = array[++arrayLen]) != curValue)
      {
       if (curValue == curValue || Float.floatToIntBits(temp) !=
           Float.floatToIntBits(curValue))
        break;
      }
       else if (curValue == 0.0F && Float.floatToIntBits(temp) !=
                Float.floatToIntBits(curValue))
        break;
     }
     if (curOffset > len)
      break;
    }
  }
  return index;
 }

/**
 * Converts the region of a given array to its 'in-line' string
 * representation.
 **
 * The string representations of <CODE>float</CODE> values (of the
 * specified region of <VAR>array</VAR>) are placed into the
 * resulting string in the direct index order, delimited by a single
 * <VAR>separator</VAR> character. Negative <VAR>len</VAR> is
 * treated as zero.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be converted.
 * @param offset
 * the first index (must be in the range) of the region to be
 * converted.
 * @param len
 * the length of the region to be converted.
 * @param separator
 * the delimiter character.
 * @return
 * the string representation (not <CODE>null</CODE>) of the
 * specified region.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>array</VAR> minus
 * <VAR>len</VAR>).
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #array()
 * @see #toString()
 * @see #fill(float[], int, int, float)
 * @see #quickSort(float[], int, int)
 * @see #binarySearch(float[], int, int, float)
 */
 public static final String toString(float[] array,
         int offset, int len, char separator)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int capacity = array.length;
  capacity = 0;
  if (len > 0)
  {
   float value = array[offset];
   value = array[offset + len - 1];
   if ((capacity = len << 2) <= 24)
    capacity = 24;
  }
  StringBuffer sBuf = new StringBuffer(capacity);
  if (len > 0)
   do
   {
    sBuf.append(Float.toString(array[offset++]));
    if (--len <= 0)
     break;
    sBuf.append(separator);
   } while (true);
  return new String(sBuf);
 }

/**
 * Produces a hash code value for a given array.
 **
 * This method mixes the binary representations of all the elements
 * of <VAR>array</VAR> to produce a single hash code value.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to evaluate hash of.
 * @return
 * the hash code value for <VAR>array</VAR>.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #hashCode()
 * @see #fill(float[], int, int, float)
 * @see #reverse(float[])
 * @see #sumOf(float[], int, int)
 * @see #countNonZero(float[])
 * @see #indexOf(float, int, float[])
 * @see #lastIndexOf(float, int, float[])
 * @see #equals(float[], float[])
 * @see #compare(float[], int, int, float[], int, int)
 * @see #mismatches(float[], int, float[], int, int)
 */
 public static final int hashCode(float[] array)
  throws NullPointerException
 {
  int code = 0, offset = 0;
  for (int len = array.length; offset < len;
       code = (code << 5) - code)
   code ^= Float.floatToIntBits(array[offset++]);
  return code ^ offset;
 }

/**
 * Tests whether or not the specified two arrays are mathematically
 * equal.
 **
 * This method returns <CODE>true</CODE> if and only if both of the
 * arrays are of the same length and all the elements of the first
 * array are naturally equal to the corresponding elements of the
 * second array.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>) to be compared.
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>) to compare with.
 * @return
 * <CODE>true</CODE> if and only if <VAR>arrayA</VAR> content is the
 * same as <VAR>arrayB</VAR> content.
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #subtractFrom(float[], float[])
 * @see #scalarMul(float[], float[])
 * @see #sumOf(float[], int, int)
 * @see #countNonZero(float[])
 * @see #indexOf(float, int, float[])
 * @see #lastIndexOf(float, int, float[])
 * @see #equals(float[], float[])
 * @see #compare(float[], int, int, float[], int, int)
 * @see #mismatches(float[], int, float[], int, int)
 **
 * @since 2.0
 */
 public static final boolean mathEquals(float[] arrayA,
         float[] arrayB)
  throws NullPointerException
 {
  int offset = arrayA.length;
  if (arrayA != arrayB)
   if (arrayB.length != offset)
    return false;
    else while (offset-- > 0)
     if (arrayA[offset] != arrayB[offset])
      return false;
  return true;
 }

/**
 * Tests whether or not the specified two arrays are equal.
 **
 * This method returns <CODE>true</CODE> if and only if both of the
 * arrays are of the same length and all the elements of the first
 * array are equal to the corresponding elements of the second
 * array. Important notes: any two elements are treated as equal if
 * and only if their binary representations are equal.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>) to be compared.
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>) to compare with.
 * @return
 * <CODE>true</CODE> if and only if <VAR>arrayA</VAR> content is the
 * same as <VAR>arrayB</VAR> content.
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #mathEquals(float[], float[])
 * @see #equals(java.lang.Object)
 * @see #fill(float[], int, int, float)
 * @see #reverse(float[])
 * @see #indexOf(float, int, float[])
 * @see #lastIndexOf(float, int, float[])
 * @see #hashCode(float[])
 * @see #compare(float[], int, int, float[], int, int)
 * @see #mismatches(float[], int, float[], int, int)
 **
 * @since 2.0
 */
 public static final boolean equals(float[] arrayA, float[] arrayB)
  throws NullPointerException
 {
  int offset = arrayA.length;
  if (arrayA != arrayB)
   if (arrayB.length != offset)
    return false;
    else while (offset-- > 0)
    {
     float value, temp = arrayB[offset];
     if ((value = arrayA[offset]) != temp)
     {
      if (value == value || Float.floatToIntBits(value) !=
          Float.floatToIntBits(temp))
       return false;
     }
      else if (value == 0.0F && Float.floatToIntBits(value) !=
               Float.floatToIntBits(temp))
       return false;
    }
  return true;
 }

/**
 * Count the mismatches of two given array regions.
 **
 * This method returns the count of elements of the first array
 * region which are not equal to the corresponding elements of the
 * second array region. Negative <VAR>len</VAR> is treated as zero.
 * Important notes: any two elements are treated as equal if and
 * only if their binary representations are equal.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>) to be compared.
 * @param offsetA
 * the first index (must be in the range) of the first region.
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>) to compare with.
 * @param offsetB
 * the first index (must be in the range) of the second region.
 * @param len
 * the length of the regions.
 * @return
 * the count (non-negative) of found mismatches of the regions.
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offsetA</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>arrayA</VAR> minus
 * <VAR>len</VAR>, or <VAR>offsetB</VAR> is negative or is greater
 * than <CODE>length</CODE> of <VAR>arrayB</VAR> minus
 * <VAR>len</VAR>).
 **
 * @see #array()
 * @see #fill(float[], int, int, float)
 * @see #reverse(float[])
 * @see #subtractFrom(float[], float[])
 * @see #scalarMul(float[], float[])
 * @see #sumOf(float[], int, int)
 * @see #countNonZero(float[])
 * @see #hashCode(float[])
 * @see #mathEquals(float[], float[])
 * @see #equals(float[], float[])
 * @see #compare(float[], int, int, float[], int, int)
 **
 * @since 2.0
 */
 public static final int mismatches(float[] arrayA, int offsetA,
         float[] arrayB, int offsetB, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int count = arrayA.length - arrayB.length;
  count = 0;
  if (len > 0)
  {
   float value = arrayA[offsetA];
   value = arrayA[offsetA + len - 1];
   value = arrayB[offsetB];
   value = arrayB[offsetB + len - 1];
   if (offsetA != offsetB || arrayA != arrayB)
    do
    {
     float temp = arrayB[offsetB++];
     if ((value = arrayA[offsetA++]) != temp)
     {
      if (value == value || Float.floatToIntBits(value) !=
          Float.floatToIntBits(temp))
       count++;
     }
      else if (value == 0.0F && Float.floatToIntBits(value) !=
               Float.floatToIntBits(temp))
       count++;
    } while (--len > 0);
  }
  return count;
 }

/**
 * Compares two given array regions.
 **
 * This method returns a signed integer indicating
 * 'less-equal-greater' relation between the specified array regions
 * of <CODE>float</CODE> values (the absolute value of the result,
 * in fact, is the distance between the first found mismatch and the
 * end of the bigger-length region). Negative <VAR>lenA</VAR> is
 * treated as zero. Negative <VAR>lenB</VAR> is treated as zero.
 * Important notes: the content of array regions is compared before
 * comparing their length; any two <CODE>float</CODE> values are
 * compared in the natural way, except for <CODE>0</CODE> (which is
 * also greater than <CODE>-0</CODE>) and for <CODE>NaN</CODE>
 * (which is greater than any non-<CODE>NaN</CODE>).
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>) to be compared.
 * @param offsetA
 * the first index (must be in the range) of the first region.
 * @param lenA
 * the length of the first region.
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>) to compare with.
 * @param offsetB
 * the first index (must be in the range) of the second region.
 * @param lenB
 * the length of the second region.
 * @return
 * a negative integer, zero, or a positive integer as
 * <VAR>arrayA</VAR> region is less than, equal to, or greater than
 * <VAR>arrayB</VAR> one.
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>lenA</VAR> is positive and (<VAR>offsetA</VAR> is
 * negative or is greater than <CODE>length</CODE> of
 * <VAR>arrayA</VAR> minus <VAR>lenA</VAR>), or if <VAR>lenB</VAR>
 * is positive and (<VAR>offsetB</VAR> is negative or is greater
 * than <CODE>length</CODE> of <VAR>arrayB</VAR> minus
 * <VAR>lenB</VAR>).
 **
 * @see #array()
 * @see #greaterThan(java.lang.Object)
 * @see #fill(float[], int, int, float)
 * @see #reverse(float[])
 * @see #sumOf(float[], int, int)
 * @see #indexOf(float, int, float[])
 * @see #lastIndexOf(float, int, float[])
 * @see #hashCode(float[])
 * @see #mathEquals(float[], float[])
 * @see #equals(float[], float[])
 * @see #mismatches(float[], int, float[], int, int)
 */
 public static final int compare(float[] arrayA, int offsetA,
         int lenA, float[] arrayB, int offsetB, int lenB)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  float value;
  int bits = arrayA.length - arrayB.length;
  if (lenA > 0)
  {
   value = arrayA[offsetA];
   value = arrayA[offsetA + lenA - 1];
  }
   else lenA = 0;
  if (lenB > 0)
  {
   value = arrayB[offsetB];
   value = arrayB[offsetB + lenB - 1];
  }
   else lenB = 0;
  if ((lenB = lenA - lenB) >= 0)
   lenA -= lenB;
  if (offsetA != offsetB || arrayA != arrayB)
  {
   for (bits = 0; lenA > 0; lenA--)
   {
    float temp = arrayB[offsetB++];
    if ((value = arrayA[offsetA++]) != temp)
    {
     if (value > temp)
      break;
     bits = -1;
     if (value < temp)
      break;
     int tempBits = Float.floatToIntBits(temp);
     if ((bits = Float.floatToIntBits(value) - tempBits) != 0)
     {
      if (((bits + tempBits) ^ tempBits) < 0)
       bits = ~tempBits;
      break;
     }
    }
     else if (value == 0.0F)
      if ((bits = Float.floatToIntBits(value)) ==
          Float.floatToIntBits(temp))
       bits = 0;
       else break;
   }
   if (lenA > 0)
   {
    if (lenB <= 0)
     lenB = -lenB;
    lenB += lenA;
    if (bits < 0)
     lenB = -lenB;
   }
  }
  return lenB;
 }

/**
 * Sorts the elements in the region of a given array using 'Quick'
 * algorithm.
 **
 * Elements in the region are sorted into ascending natural order.
 * But equal elements may be reordered (since the algorithm is not
 * 'stable'). A small working stack is allocated (since the
 * algorithm is 'in-place' and recursive). The algorithm cost is
 * <CODE>O(log(len) * len)</CODE> typically, but may be of
 * <CODE>O(len * len)</CODE> in the worst case (which is rare, in
 * fact). Negative <VAR>len</VAR> is treated as zero. If an
 * exception is thrown then <VAR>array</VAR> remains unchanged. Else
 * the region content is altered. Important notes: values comparison
 * is performed in the natural way, except for <CODE>0</CODE> (which
 * is also greater than <CODE>-0</CODE>) and for <CODE>NaN</CODE>
 * (which is greater than any non-<CODE>NaN</CODE>).
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be sorted.
 * @param offset
 * the first index (must be in the range) of the region to sort.
 * @param len
 * the length of the region to sort.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>array</VAR> minus
 * <VAR>len</VAR>).
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #array()
 * @see #binarySearch(float[], int, int, float)
 * @see #compare(float[], int, int, float[], int, int)
 * @see #fill(float[], int, int, float)
 * @see #toString(float[], int, int, char)
 */
 public static final void quickSort(float[] array,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   float value = array[offset], temp;
   if (len > 1)
   {
    value = array[len += offset - 1];
    int[] bounds = new int[(JavaConsts.INT_SIZE - 3) << 1];
    do
    {
     value = array[len];
    } while (value != value && --len > offset);
    int level = len, index, last;
    while (offset < level)
    {
     value = array[--level];
     if (value != value)
     {
      array[level] = array[len];
      array[len--] = value;
     }
    }
    if ((bounds[0] = offset) < len)
    {
     bounds[1] = len;
     level = 2;
     do
     {
      do
      {
       index = offset;
       if ((last = len) - offset < 8)
       {
        len = offset;
        do
        {
         value = array[offset = ++index];
         do
         {
          if (!((temp = array[offset - 1]) > value))
           break;
          array[offset--] = temp;
         } while (offset > len);
         array[offset] = value;
        } while (index < last);
        break;
       }
       value = array[len = (offset + len) >>> 1];
       array[len] = array[offset];
       array[offset] = value;
       len = last;
       do
       {
        while (++offset < len && value > array[offset]);
        len++;
        while (--len >= offset && array[len] > value);
        if (offset >= len)
         break;
        temp = array[len];
        array[len--] = array[offset];
        array[offset] = temp;
       } while (true);
       array[offset = index] = array[len];
       array[len] = value;
       if (len - offset > last - len)
       {
        offset = len + 1;
        len = last;
        last = offset - 2;
       }
        else index = (len--) + 1;
       bounds[level++] = index;
       bounds[level++] = last;
      } while (offset < len);
      len = bounds[--level];
      offset = bounds[--level];
     } while (level > 0);
     do
     {
      if ((value = array[level = (offset + len) >>> 1]) > 0.0F)
       len = level - 1;
       else if (value != 0.0F)
        offset = level + 1;
        else break;
     } while (offset <= len);
     if (offset < len)
     {
      index = level;
      while (--level >= offset && array[level] == 0.0F);
      while (++index <= len && array[index] == 0.0F);
      offset = level + 1;
      do
      {
       if (Float.floatToIntBits(value = array[offset]) < 0)
       {
        array[offset] = array[++level];
        array[level] = value;
       }
      } while (++offset < index);
     }
    }
   }
  }
  len = array.length;
 }

/**
 * Searches (fast) for value in a given sorted array.
 **
 * <VAR>array</VAR> (or its specified range) must be sorted
 * ascending, or the result is undefined. The algorithm cost is of
 * <CODE>O(log(len))</CODE>. Negative <VAR>len</VAR> is treated as
 * zero. If <VAR>value</VAR> is not found then
 * <CODE>(-result - 1)</CODE> is the offset of the insertion point
 * for <VAR>value</VAR>. Important notes: values comparison is
 * performed in the natural way, except for <CODE>0</CODE> (which is
 * also greater than <CODE>-0</CODE>) and for <CODE>NaN</CODE>
 * (which is greater than any non-<CODE>NaN</CODE>).
 **
 * @param array
 * the sorted array (must be non-<CODE>null</CODE>) to be searched
 * in.
 * @param offset
 * the first index (must be in the range) of the region to search
 * in.
 * @param len
 * the length of the region to search in.
 * @param value
 * the value to search for.
 * @return
 * the index (non-negative) of the found value or
 * <CODE>(-insertionOffset - 1)</CODE> (a negative integer) if not
 * found.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>array</VAR> minus
 * <VAR>len</VAR>).
 **
 * @see #array()
 * @see #indexOf(float, int, float[])
 * @see #lastIndexOf(float, int, float[])
 * @see #quickSort(float[], int, int)
 * @see #compare(float[], int, int, float[], int, int)
 * @see #fill(float[], int, int, float)
 * @see #toString(float[], int, int, char)
 */
 public static final int binarySearch(float[] array,
         int offset, int len, float value)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   int middle;
   float temp = array[offset];
   temp = array[len += offset - 1];
   int bits = Float.floatToIntBits(value), tempBits;
   do
   {
    if ((temp = array[middle = (offset + len) >>> 1]) > value)
     len = middle - 1;
     else if (temp < value)
      offset = middle + 1;
      else if (temp == value && value != 0.0F)
       break;
       else if ((tempBits = Float.floatToIntBits(temp)) > bits)
        len = middle - 1;
        else if (tempBits != bits)
         offset = middle + 1;
         else break;
   } while (offset <= len);
   if (offset <= len)
    offset = ~middle;
  }
  len = array.length;
  return ~offset;
 }

/**
 * Creates and returns a copy of <CODE>this</CODE> object.
 **
 * This method creates a new instance of the class of this object
 * and initializes its <VAR>array</VAR> with a copy of
 * <VAR>array</VAR> of <CODE>this</CODE> vector.
 **
 * @return
 * a copy (not <CODE>null</CODE> and != <CODE>this</CODE>) of
 * <CODE>this</CODE> instance.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see FloatVector#FloatVector()
 * @see #array()
 * @see #getFloatAt(int)
 * @see #equals(java.lang.Object)
 */
 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof FloatVector && obj != this)
   {
    FloatVector vector = (FloatVector)obj;
    vector.array = (float[])vector.array.clone();
    return obj;
   }
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * Computes and returns a hash code value for the object.
 **
 * This method mixes all the elements of <CODE>this</CODE> vector to
 * produce a single hash code value.
 **
 * @return
 * a hash code value for <CODE>this</CODE> object.
 **
 * @see #hashCode(float[])
 * @see #array()
 * @see #length()
 * @see #getFloatAt(int)
 * @see #equals(java.lang.Object)
 */
 public int hashCode()
 {
  return hashCode(this.array);
 }

/**
 * Indicates whether <CODE>this</CODE> object is equal to the
 * specified one.
 **
 * This method returns <CODE>true</CODE> if and only if
 * <VAR>obj</VAR> is instance of this vector class and all elements
 * of <CODE>this</CODE> vector are equal to the corresponding
 * elements of <VAR>obj</VAR> vector. Important notes: any two
 * elements are treated as equal if and only if their binary
 * representations are equal.
 **
 * @param obj
 * the object (may be <CODE>null</CODE>) with which to compare.
 * @return
 * <CODE>true</CODE> if and only if <CODE>this</CODE> value is the
 * same as <VAR>obj</VAR> value.
 **
 * @see FloatVector#FloatVector()
 * @see #equals(float[], float[])
 * @see #mathEquals(float[], float[])
 * @see #array()
 * @see #length()
 * @see #getFloatAt(int)
 * @see #hashCode()
 * @see #greaterThan(java.lang.Object)
 */
 public boolean equals(Object obj)
 {
  return obj == this || obj instanceof FloatVector &&
   equals(this.array, ((FloatVector)obj).array);
 }

/**
 * Tests for being semantically greater than the argument.
 **
 * The result is <CODE>true</CODE> if and only if <VAR>obj</VAR> is
 * instance of <CODE>this</CODE> class and <CODE>this</CODE> object
 * is greater than the specified object. Vectors are compared in the
 * element-by-element manner, starting at index <CODE>0</CODE>.
 * Important notes: any two <CODE>float</CODE> values are compared
 * in the natural way, except for <CODE>0</CODE> (which is also
 * greater than <CODE>-0</CODE>) and for <CODE>NaN</CODE> (which is
 * greater than any non-<CODE>NaN</CODE>).
 **
 * @param obj
 * the second compared object (may be <CODE>null</CODE>).
 * @return
 * <CODE>true</CODE> if <VAR>obj</VAR> is comparable with
 * <CODE>this</CODE> and <CODE>this</CODE> object is greater than
 * <VAR>obj</VAR>, else <CODE>false</CODE>.
 **
 * @see #compare(float[], int, int, float[], int, int)
 * @see #array()
 * @see #length()
 * @see #getFloatAt(int)
 * @see #equals(java.lang.Object)
 **
 * @since 2.0
 */
 public boolean greaterThan(Object obj)
 {
  if (obj != this && obj instanceof FloatVector)
  {
   float[] array = this.array;
   float[] otherArray = ((FloatVector)obj).array;
   if (compare(array, 0, array.length,
       otherArray, 0, otherArray.length) > 0)
    return true;
  }
  return false;
 }

/**
 * Converts <CODE>this</CODE> vector to its 'in-line' string
 * representation.
 **
 * The string representations of <CODE>float</CODE> values of the
 * wrapped <VAR>array</VAR> are placed into the resulting string in
 * the direct index order, delimited by a single space.
 **
 * @return
 * the string representation (not <CODE>null</CODE>) of
 * <CODE>this</CODE> object.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #toString(float[], int, int, char)
 * @see #array()
 * @see #length()
 */
 public String toString()
 {
  float[] array = this.array;
  return toString(array, 0, array.length, ' ');
 }

/**
 * Verifies <CODE>this</CODE> object for its integrity.
 **
 * For debug purpose only.
 **
 * @exception InternalError
 * if integrity violation is detected.
 **
 * @see FloatVector#FloatVector(float[])
 * @see #setArray(float[])
 * @see #array()
 **
 * @since 2.0
 */
 public void integrityCheck()
 {
  if (this.array == null)
   throw new InternalError("array: null");
 }

/**
 * Deserializes an object of this class from a given stream.
 **
 * This method is responsible for reading from <VAR>in</VAR> stream,
 * restoring the classes fields, and verifying that the serialized
 * object is not corrupted. First of all, it calls
 * <CODE>defaultReadObject()</CODE> for <VAR>in</VAR> to invoke the
 * default deserialization mechanism. Then, it restores the state of
 * <CODE>transient</CODE> fields and performs additional
 * verification of the deserialized object. This method is used only
 * internally by <CODE>ObjectInputStream</CODE> class.
 **
 * @param in
 * the stream (must be non-<CODE>null</CODE>) to read data from in
 * order to restore the object.
 * @exception NullPointerException
 * if <VAR>in</VAR> is <CODE>null</CODE>.
 * @exception IOException
 * if any I/O error occurs or the serialized object is corrupted.
 * @exception ClassNotFoundException
 * if the class for an object being restored cannot be found.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see FloatVector#FloatVector(float[])
 * @see #integrityCheck()
 */
 private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
 {
  in.defaultReadObject();
  if (this.array == null)
   throw new InvalidObjectException("array: null");
 }
}

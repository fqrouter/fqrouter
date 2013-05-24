/*
 * @(#) src/net/sf/ivmaidns/util/BooleanVector.java --
 * Class for 'boolean' array wrappers.
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
 * Class for 'boolean' array wrappers.
 **
 * This class wraps a primitive <CODE>boolean</CODE>-type array, and
 * has the possibility to resize (when required) the wrapped array.
 * This class supports cloning, serialization and comparison of its
 * instances. In addition, the class contains <CODE>static</CODE>
 * methods for <CODE>boolean</CODE> arrays resizing, filling in,
 * reversing, vector arithmetics (logical and, or, exclusive or,
 * inversion), <CODE>true</CODE> elements counting, searching in for
 * a sequence, mismatches counting, 'less-equal-greater' comparison,
 * and 'to-string' conversion. Important notes: <CODE>boolean</CODE>
 * arrays are not memory-efficient, so it is better to pack and
 * store <CODE>boolean</CODE> values into <CODE>int</CODE> arrays
 * (where it is possible).
 **
 * @see ByteVector
 * @see CharVector
 * @see DoubleVector
 * @see FloatVector
 * @see IntVector
 * @see LongVector
 * @see ShortVector
 * @see ObjectVector
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public final class BooleanVector
 implements ReallyCloneable, Serializable, Indexable, Sortable,
            Verifiable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.8
 */
 private static final long serialVersionUID = 8163381858820555196L;

/**
 * A constant initialized with an instance of empty
 * <CODE>boolean</CODE> array.
 **
 * @see #array
 */
 protected static final boolean[] EMPTY = {};

/**
 * The wrapped (encapsulated) custom <CODE>boolean</CODE> array.
 **
 * <VAR>array</VAR> must be non-<CODE>null</CODE>.
 **
 * @serial
 **
 * @see #EMPTY
 * @see BooleanVector#BooleanVector()
 * @see BooleanVector#BooleanVector(int)
 * @see BooleanVector#BooleanVector(boolean[])
 * @see #setArray(boolean[])
 * @see #array()
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, boolean)
 * @see #getBooleanAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #integrityCheck()
 */
 protected boolean[] array;

/**
 * Constructs an empty <CODE>boolean</CODE> vector.
 **
 * This constructor is used for the creation of a resizable vector.
 * The length of such a vector is changed only by
 * <CODE>resize(int)</CODE> and <CODE>ensureSize(int)</CODE>
 * methods.
 **
 * @see BooleanVector#BooleanVector(int)
 * @see BooleanVector#BooleanVector(boolean[])
 * @see #array()
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, boolean)
 * @see #getBooleanAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #toString()
 */
 public BooleanVector()
 {
  this.array = EMPTY;
 }

/**
 * Constructs a new <CODE>boolean</CODE> vector of the specified
 * length.
 **
 * This constructor is typically used for the creation of a vector
 * with a fixed size. All elements of the vector are set to
 * <CODE>false</CODE>.
 **
 * @param size
 * the initial length (unsigned) of the vector to be created.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see BooleanVector#BooleanVector()
 * @see BooleanVector#BooleanVector(boolean[])
 * @see #array()
 * @see #length()
 * @see #setAt(int, boolean)
 * @see #getBooleanAt(int)
 * @see #copyAt(int, int, int)
 * @see #fill(boolean[], int, int, boolean)
 * @see #clone()
 * @see #toString()
 */
 public BooleanVector(int size)
 {
  if (size < 0)
   size = -1 >>> 1;
  this.array = new boolean[size];
 }

/**
 * Constructs a new <CODE>boolean</CODE> array wrapper.
 **
 * This constructor is used for the creation of a vector which wraps
 * the specified array (without copying it). The wrapped array may
 * be further replaced with another one only by
 * <CODE>setArray(boolean[])</CODE> and by <CODE>resize(int)</CODE>,
 * <CODE>ensureSize(int)</CODE> methods.
 **
 * @param array
 * the <CODE>boolean</CODE> array (must be non-<CODE>null</CODE>) to
 * be wrapped.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see BooleanVector#BooleanVector()
 * @see BooleanVector#BooleanVector(int)
 * @see #setArray(boolean[])
 * @see #array()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, boolean)
 * @see #getBooleanAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #toString()
 **
 * @since 2.0
 */
 public BooleanVector(boolean[] array)
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
 * the <CODE>boolean</CODE> array (must be non-<CODE>null</CODE>) to
 * be wrapped.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see BooleanVector#BooleanVector()
 * @see BooleanVector#BooleanVector(boolean[])
 * @see #array()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, boolean)
 * @see #getBooleanAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 **
 * @since 2.0
 */
 public void setArray(boolean[] array)
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
 * the <CODE>boolean</CODE> array (not <CODE>null</CODE>), which is
 * wrapped.
 **
 * @see BooleanVector#BooleanVector(boolean[])
 * @see #setArray(boolean[])
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 **
 * @since 1.8
 */
 public final boolean[] array()
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
 * @see #setArray(boolean[])
 * @see #array()
 * @see #setAt(int, boolean)
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #getBooleanAt(int)
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
 * <CODE>new Boolean(array()[index])</CODE>.
 **
 * @param index
 * the index (must be in the range) at which to return an element.
 * @return
 * an element (instance of <CODE>Boolean</CODE>) at
 * <VAR>index</VAR>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #getBooleanAt(int)
 * @see #array()
 * @see #length()
 */
 public Object getAt(int index)
  throws ArrayIndexOutOfBoundsException
 {
  return new Boolean(this.array[index]);
 }

/**
 * Returns value of the element at the specified index.
 **
 * The result is the same as of <CODE>array()[index]</CODE>.
 **
 * @param index
 * the index (must be in the range) at which to return an element.
 * @return
 * a <CODE>boolean</CODE> element at <VAR>index</VAR>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 **
 * @see #array()
 * @see #length()
 * @see #setAt(int, boolean)
 * @see #resize(int)
 * @see #ensureSize(int)
 */
 public final boolean getBooleanAt(int index)
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
 * @see #setArray(boolean[])
 * @see #array()
 * @see #length()
 * @see #getBooleanAt(int)
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #copyAt(int, int, int)
 * @see #fill(boolean[], int, int, boolean)
 */
 public void setAt(int index, boolean value)
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
 * @see #setAt(int, boolean)
 * @see #getBooleanAt(int)
 * @see #resize(int)
 * @see #ensureSize(int)
 */
 public void copyAt(int srcOffset, int destOffset, int len)
  throws ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   boolean[] array = this.array;
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
 * new elements are set to <CODE>false</CODE>. If an exception is
 * thrown then <CODE>this</CODE> vector remains unchanged.
 **
 * @param size
 * the (unsigned) length of <CODE>this</CODE> vector to set.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see BooleanVector#BooleanVector(int)
 * @see #setArray(boolean[])
 * @see #array()
 * @see #length()
 * @see #ensureSize(int)
 * @see #resize(boolean[], int)
 */
 public void resize(int size)
 {
  int len;
  boolean[] array = this.array;
  if ((len = array.length) != size)
  {
   boolean[] newArray = EMPTY;
   if (size != 0)
   {
    if (len > size)
     if (size < 0)
      size = -1 >>> 1;
      else len = size;
    System.arraycopy(array, 0,
     newArray = new boolean[size], 0, len);
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
 * <CODE>false</CODE>. If an exception is thrown then
 * <CODE>this</CODE> vector remains unchanged.
 **
 * @param size
 * the (unsigned) length of <CODE>this</CODE> vector to be ensured.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #array()
 * @see #length()
 * @see #setAt(int, boolean)
 * @see #resize(int)
 * @see #ensureSize(boolean[], int)
 */
 public void ensureSize(int size)
 {
  int len;
  boolean[] array = this.array, newArray;
  if ((((len = array.length) - size) | size) < 0)
  {
   if (size < 0)
    size = -1 >>> 1;
   if ((len += len >> 1) >= size)
    size = len;
   System.arraycopy(array, 0,
    newArray = new boolean[size], 0, array.length);
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
 * elements are set to <CODE>false</CODE>).
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
 * @see #ensureSize(boolean[], int)
 * @see #fill(boolean[], int, int, boolean)
 */
 public static final boolean[] resize(boolean[] array, int size)
  throws NullPointerException
 {
  int len;
  if ((len = array.length) != size)
  {
   boolean[] newArray = EMPTY;
   if (size != 0)
   {
    if (len > size)
     if (size < 0)
      size = -1 >>> 1;
      else len = size;
    System.arraycopy(array, 0,
     newArray = new boolean[size], 0, len);
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
 * (all new elements are set to <CODE>false</CODE>).
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
 * @see #resize(boolean[], int)
 * @see #fill(boolean[], int, int, boolean)
 */
 public static final boolean[] ensureSize(boolean[] array, int size)
  throws NullPointerException
 {
  int len;
  if ((((len = array.length) - size) | size) < 0)
  {
   if (size < 0)
    size = -1 >>> 1;
   if ((len += len >> 1) >= size)
    size = len;
   boolean[] newArray;
   System.arraycopy(array, 0,
    newArray = new boolean[size], 0, array.length);
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
 * @see #xor(boolean[], boolean[])
 * @see #orNot(boolean[], boolean[])
 * @see #indexOf(boolean[], int, int, int, boolean[])
 * @see #lastIndexOf(boolean[], int, int, int, boolean[])
 * @see #toString(boolean[], int, int, char)
 **
 * @since 2.0
 */
 public static final void fill(boolean[] array, int offset, int len,
         boolean value)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int next = array.length, block;
  if (len > 0)
  {
   boolean temp;
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
 * @see #and(boolean[], boolean[])
 * @see #andNot(boolean[], boolean[])
 * @see #or(boolean[], boolean[])
 * @see #orNot(boolean[], boolean[])
 * @see #xor(boolean[], boolean[])
 * @see #invert(boolean[])
 * @see #countTrue(boolean[])
 * @see #indexOf(boolean[], int, int, int, boolean[])
 * @see #lastIndexOf(boolean[], int, int, int, boolean[])
 * @see #equals(boolean[], boolean[])
 */
 public static final void reverse(boolean[] array)
  throws NullPointerException
 {
  int offset = 0, len = array.length;
  while (--len > offset)
  {
   boolean value = array[offset];
   array[offset++] = array[len];
   array[len] = value;
  }
 }

/**
 * Performs logical 'and' operation on two given arrays.
 **
 * For every <CODE>false</CODE> element of the second array the
 * corresponding element (if not missing) of the first array is set
 * to <CODE>false</CODE>. <VAR>arrayA</VAR> content is altered.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>).
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>).
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #reverse(boolean[])
 * @see #andNot(boolean[], boolean[])
 * @see #or(boolean[], boolean[])
 * @see #orNot(boolean[], boolean[])
 * @see #xor(boolean[], boolean[])
 * @see #invert(boolean[])
 * @see #countTrue(boolean[])
 */
 public static final void and(boolean[] arrayA, boolean[] arrayB)
  throws NullPointerException
 {
  int offset = arrayA.length, len;
  if (arrayA != arrayB)
  {
   if ((len = arrayB.length) <= offset)
    offset = len;
   while (offset-- > 0)
    arrayA[offset] &= arrayB[offset];
  }
 }

/**
 * Performs logical 'and not' operation on two given arrays.
 **
 * For every <CODE>true</CODE> element of the second array the
 * corresponding element (if not missing) of the first array is set
 * to <CODE>false</CODE>. <VAR>arrayA</VAR> content is altered.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>).
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>).
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #fill(boolean[], int, int, boolean)
 * @see #reverse(boolean[])
 * @see #and(boolean[], boolean[])
 * @see #or(boolean[], boolean[])
 * @see #orNot(boolean[], boolean[])
 * @see #xor(boolean[], boolean[])
 * @see #invert(boolean[])
 * @see #countTrue(boolean[])
 */
 public static final void andNot(boolean[] arrayA, boolean[] arrayB)
  throws NullPointerException
 {
  int offset = arrayA.length, len;
  if (arrayA != arrayB)
  {
   if ((len = arrayB.length) <= offset)
    offset = len;
   while (offset-- > 0)
    if (arrayB[offset])
     arrayA[offset] = false;
  }
  while (offset-- > 0)
   arrayA[offset] = false;
 }

/**
 * Performs logical 'or' operation on two given arrays.
 **
 * For every <CODE>true</CODE> element of the second array the
 * corresponding element (if not missing) of the first array is set
 * to <CODE>true</CODE>. <VAR>arrayA</VAR> content is altered.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>).
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>).
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #reverse(boolean[])
 * @see #and(boolean[], boolean[])
 * @see #andNot(boolean[], boolean[])
 * @see #orNot(boolean[], boolean[])
 * @see #xor(boolean[], boolean[])
 * @see #invert(boolean[])
 * @see #countTrue(boolean[])
 */
 public static final void or(boolean[] arrayA, boolean[] arrayB)
  throws NullPointerException
 {
  int offset = arrayA.length, len;
  if (arrayA != arrayB)
  {
   if ((len = arrayB.length) <= offset)
    offset = len;
   while (offset-- > 0)
    arrayA[offset] |= arrayB[offset];
  }
 }

/**
 * Performs logical 'or not' operation on two given arrays.
 **
 * For every <CODE>false</CODE> element of the second array the
 * corresponding element (if not missing) of the first array is set
 * to <CODE>true</CODE>. <VAR>arrayA</VAR> content is altered.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>).
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>).
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #fill(boolean[], int, int, boolean)
 * @see #reverse(boolean[])
 * @see #and(boolean[], boolean[])
 * @see #andNot(boolean[], boolean[])
 * @see #or(boolean[], boolean[])
 * @see #xor(boolean[], boolean[])
 * @see #invert(boolean[])
 * @see #countTrue(boolean[])
 */
 public static final void orNot(boolean[] arrayA, boolean[] arrayB)
  throws NullPointerException
 {
  int offset = arrayA.length, len;
  if (arrayA != arrayB)
  {
   if ((len = arrayB.length) <= offset)
    offset = len;
   while (offset-- > 0)
    if (!arrayB[offset])
     arrayA[offset] = true;
  }
  while (offset-- > 0)
   arrayA[offset] = true;
 }

/**
 * Performs logical 'exclusive or' operation on two given arrays.
 **
 * Every element of the first array which differs from the
 * corresponding element (if not missing) of the second array is set
 * to <CODE>true</CODE>, else it is set to <CODE>false</CODE> (but
 * only if its corresponding element of the second array is not
 * missing). <VAR>arrayA</VAR> content is altered.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>).
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>).
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #fill(boolean[], int, int, boolean)
 * @see #reverse(boolean[])
 * @see #and(boolean[], boolean[])
 * @see #andNot(boolean[], boolean[])
 * @see #or(boolean[], boolean[])
 * @see #orNot(boolean[], boolean[])
 * @see #invert(boolean[])
 * @see #countTrue(boolean[])
 * @see #compare(boolean[], int, int, boolean[], int, int)
 * @see #mismatches(boolean[], int, boolean[], int, int)
 */
 public static final void xor(boolean[] arrayA, boolean[] arrayB)
  throws NullPointerException
 {
  int offset = arrayA.length, len;
  if (arrayA != arrayB)
  {
   if ((len = arrayB.length) <= offset)
    offset = len;
   while (offset-- > 0)
    arrayA[offset] ^= arrayB[offset];
  }
  while (offset-- > 0)
   arrayA[offset] = false;
 }

/**
 * Performs logical 'not' operation on a given array.
 **
 * Every element of the specified array is inverted.
 * <VAR>array</VAR> content is altered.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be inverted.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #reverse(boolean[])
 * @see #and(boolean[], boolean[])
 * @see #andNot(boolean[], boolean[])
 * @see #or(boolean[], boolean[])
 * @see #orNot(boolean[], boolean[])
 * @see #xor(boolean[], boolean[])
 * @see #countTrue(boolean[])
 */
 public static final void invert(boolean[] array)
  throws NullPointerException
 {
  int offset = array.length;
  while (offset-- > 0)
   array[offset] ^= true;
 }

/**
 * Count <CODE>true</CODE> elements in a given array.
 **
 * This method returns the count of elements of <VAR>array</VAR>
 * which are equal to <CODE>true</CODE>.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to count
 * <CODE>true</CODE> elements in.
 * @return
 * the count (non-negative and not greater than <CODE>length</CODE>
 * of <VAR>array</VAR>) of <CODE>true</CODE> elements.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #fill(boolean[], int, int, boolean)
 * @see #and(boolean[], boolean[])
 * @see #andNot(boolean[], boolean[])
 * @see #or(boolean[], boolean[])
 * @see #orNot(boolean[], boolean[])
 * @see #xor(boolean[], boolean[])
 * @see #invert(boolean[])
 * @see #equals(boolean[], boolean[])
 * @see #compare(boolean[], int, int, boolean[], int, int)
 * @see #mismatches(boolean[], int, boolean[], int, int)
 **
 * @since 2.0
 */
 public static final int countTrue(boolean[] array)
  throws NullPointerException
 {
  int offset = array.length, count = 0;
  while (offset-- > 0)
   if (array[offset])
    count++;
  return count;
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
 * @see #lastIndexOf(boolean[], int, int, int, boolean[])
 * @see #equals(boolean[], boolean[])
 * @see #compare(boolean[], int, int, boolean[], int, int)
 */
 public static final int indexOf(boolean[] subArray,
         int offset, int len, int index, boolean[] array)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int curOffset = subArray.length, arrayLen = array.length;
  if (index <= 0)
   index = 0;
  if (len > 0)
  {
   arrayLen -= len;
   boolean value = subArray[offset], temp;
   temp = subArray[len += offset - 1];
   index--;
   while (++index <= arrayLen)
    if (array[index] == value)
    {
     curOffset = offset;
     int curIndex = index;
     while (++curOffset <= len &&
            array[++curIndex] == subArray[curOffset]);
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
 * result is <CODE>-1</CODE>.
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
 * @see #indexOf(boolean[], int, int, int, boolean[])
 * @see #equals(boolean[], boolean[])
 * @see #compare(boolean[], int, int, boolean[], int, int)
 */
 public static final int lastIndexOf(boolean[] subArray,
         int offset, int len, int index, boolean[] array)
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
   boolean value = subArray[offset], temp;
   temp = subArray[len += offset - 1];
   index++;
   while (index-- > 0)
    if (array[index] == value)
    {
     curOffset = offset;
     arrayLen = index;
     while (++curOffset <= len &&
            array[++arrayLen] == subArray[curOffset]);
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
 * The string representations ("true" or "false") of
 * <CODE>boolean</CODE> values (of the specified region of
 * <VAR>array</VAR>) are placed into the resulting string in the
 * direct index order, delimited by a single <VAR>separator</VAR>
 * character. Negative <VAR>len</VAR> is treated as zero.
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
 * @see #fill(boolean[], int, int, boolean)
 * @see #and(boolean[], boolean[])
 * @see #andNot(boolean[], boolean[])
 * @see #or(boolean[], boolean[])
 * @see #orNot(boolean[], boolean[])
 * @see #xor(boolean[], boolean[])
 * @see #invert(boolean[])
 */
 public static final String toString(boolean[] array,
         int offset, int len, char separator)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int capacity = array.length;
  capacity = 0;
  if (len > 0)
  {
   boolean value = array[offset];
   value = array[offset + len - 1];
   if ((capacity = len << 2) <= 24)
    capacity = 24;
  }
  StringBuffer sBuf = new StringBuffer(capacity);
  if (len > 0)
   do
   {
    sBuf.append(array[offset++] ? "true" : "false");
    if (--len <= 0)
     break;
    sBuf.append(separator);
   } while (true);
  return new String(sBuf);
 }

/**
 * Produces a hash code value for a given array.
 **
 * This method mixes all the elements of <VAR>array</VAR> to produce
 * a single hash code value. According to JDK1, if an element is
 * <CODE>true</CODE> then <CODE>1231</CODE> (a decimal constant) is
 * used for mixing else <CODE>1237</CODE> is used.
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
 * @see #fill(boolean[], int, int, boolean)
 * @see #reverse(boolean[])
 * @see #countTrue(boolean[])
 * @see #indexOf(boolean[], int, int, int, boolean[])
 * @see #lastIndexOf(boolean[], int, int, int, boolean[])
 * @see #equals(boolean[], boolean[])
 * @see #compare(boolean[], int, int, boolean[], int, int)
 * @see #mismatches(boolean[], int, boolean[], int, int)
 */
 public static final int hashCode(boolean[] array)
  throws NullPointerException
 {
  int code = 0, offset = 0;
  for (int len = array.length; offset < len;
       code = (code << 5) - code)
   code ^= array[offset++] ? 1231 : 1237;
  return code ^ offset;
 }

/**
 * Tests whether or not the specified two arrays are equal.
 **
 * This method returns <CODE>true</CODE> if and only if both of the
 * arrays are of the same length and all the elements of the first
 * array are equal to the corresponding elements of the second
 * array.
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
 * @see #equals(java.lang.Object)
 * @see #fill(boolean[], int, int, boolean)
 * @see #reverse(boolean[])
 * @see #and(boolean[], boolean[])
 * @see #andNot(boolean[], boolean[])
 * @see #or(boolean[], boolean[])
 * @see #orNot(boolean[], boolean[])
 * @see #xor(boolean[], boolean[])
 * @see #invert(boolean[])
 * @see #indexOf(boolean[], int, int, int, boolean[])
 * @see #lastIndexOf(boolean[], int, int, int, boolean[])
 * @see #hashCode(boolean[])
 * @see #compare(boolean[], int, int, boolean[], int, int)
 * @see #mismatches(boolean[], int, boolean[], int, int)
 **
 * @since 2.0
 */
 public static final boolean equals(boolean[] arrayA,
         boolean[] arrayB)
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
 * Count the mismatches of two given array regions.
 **
 * This method returns the count of elements of the first array
 * region which are not equal to the corresponding elements of the
 * second array region. Negative <VAR>len</VAR> is treated as zero.
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
 * @see #fill(boolean[], int, int, boolean)
 * @see #reverse(boolean[])
 * @see #xor(boolean[], boolean[])
 * @see #countTrue(boolean[])
 * @see #hashCode(boolean[])
 * @see #equals(boolean[], boolean[])
 * @see #compare(boolean[], int, int, boolean[], int, int)
 **
 * @since 2.0
 */
 public static final int mismatches(boolean[] arrayA, int offsetA,
         boolean[] arrayB, int offsetB, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int count = arrayA.length - arrayB.length;
  count = 0;
  if (len > 0)
  {
   boolean value = arrayA[offsetA];
   value = arrayA[offsetA + len - 1];
   value = arrayB[offsetB];
   value = arrayB[offsetB + len - 1];
   if (offsetA != offsetB || arrayA != arrayB)
    do
    {
     if (arrayA[offsetA++] != arrayB[offsetB++])
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
 * of <CODE>boolean</CODE> values (the absolute value of the result,
 * in fact, is the distance between the first found mismatch and the
 * end of the bigger-length region). Negative <VAR>lenA</VAR> is
 * treated as zero. Negative <VAR>lenB</VAR> is treated as zero.
 * Important notes: the content of array regions is compared before
 * comparing their length.
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
 * @see #fill(boolean[], int, int, boolean)
 * @see #reverse(boolean[])
 * @see #indexOf(boolean[], int, int, int, boolean[])
 * @see #lastIndexOf(boolean[], int, int, int, boolean[])
 * @see #hashCode(boolean[])
 * @see #equals(boolean[], boolean[])
 * @see #mismatches(boolean[], int, boolean[], int, int)
 */
 public static final int compare(boolean[] arrayA, int offsetA,
         int lenA, boolean[] arrayB, int offsetB, int lenB)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  boolean value = false;
  if (arrayA.length == arrayB.length)
   value = true;
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
   while (lenA > 0)
   {
    if ((value = arrayA[offsetA++]) != arrayB[offsetB++])
    {
     if (lenB <= 0)
      lenB = -lenB;
     lenB += lenA;
     if (value)
      break;
     lenB = -lenB;
     break;
    }
    lenA--;
   }
  return lenB;
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
 * @see BooleanVector#BooleanVector()
 * @see #array()
 * @see #getBooleanAt(int)
 * @see #equals(java.lang.Object)
 */
 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof BooleanVector &&
       obj != this)
   {
    BooleanVector vector = (BooleanVector)obj;
    vector.array = (boolean[])vector.array.clone();
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
 * @see #hashCode(boolean[])
 * @see #array()
 * @see #length()
 * @see #getBooleanAt(int)
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
 * elements of <VAR>obj</VAR> vector.
 **
 * @param obj
 * the object (may be <CODE>null</CODE>) with which to compare.
 * @return
 * <CODE>true</CODE> if and only if <CODE>this</CODE> value is the
 * same as <VAR>obj</VAR> value.
 **
 * @see BooleanVector#BooleanVector()
 * @see #equals(boolean[], boolean[])
 * @see #array()
 * @see #length()
 * @see #getBooleanAt(int)
 * @see #hashCode()
 * @see #greaterThan(java.lang.Object)
 */
 public boolean equals(Object obj)
 {
  return obj == this || obj instanceof BooleanVector &&
   equals(this.array, ((BooleanVector)obj).array);
 }

/**
 * Tests for being semantically greater than the argument.
 **
 * The result is <CODE>true</CODE> if and only if <VAR>obj</VAR> is
 * instance of <CODE>this</CODE> class and <CODE>this</CODE> object
 * is greater than the specified object. Vectors are compared in the
 * element-by-element manner, starting at index <CODE>0</CODE>.
 **
 * @param obj
 * the second compared object (may be <CODE>null</CODE>).
 * @return
 * <CODE>true</CODE> if <VAR>obj</VAR> is comparable with
 * <CODE>this</CODE> and <CODE>this</CODE> object is greater than
 * <VAR>obj</VAR>, else <CODE>false</CODE>.
 **
 * @see #compare(boolean[], int, int, boolean[], int, int)
 * @see #array()
 * @see #length()
 * @see #getBooleanAt(int)
 * @see #equals(java.lang.Object)
 **
 * @since 2.0
 */
 public boolean greaterThan(Object obj)
 {
  if (obj != this && obj instanceof BooleanVector)
  {
   boolean[] array = this.array;
   boolean[] otherArray = ((BooleanVector)obj).array;
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
 * The string representations of <CODE>boolean</CODE> values ("true"
 * or "false") of the wrapped <VAR>array</VAR> are placed into the
 * resulting string in the direct index order, delimited by a single
 * space.
 **
 * @return
 * the string representation (not <CODE>null</CODE>) of
 * <CODE>this</CODE> object.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #toString(boolean[], int, int, char)
 * @see #array()
 * @see #length()
 */
 public String toString()
 {
  boolean[] array = this.array;
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
 * @see BooleanVector#BooleanVector(boolean[])
 * @see #setArray(boolean[])
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
 * @see BooleanVector#BooleanVector(boolean[])
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

/*
 * @(#) src/net/sf/ivmaidns/util/CharVector.java --
 * Class for 'char' array wrappers.
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
 * Class for 'char' array wrappers.
 **
 * This class wraps a primitive <CODE>char</CODE>-type array, and
 * has the possibility to resize (when required) the wrapped array.
 * This class supports cloning, serialization and comparison of its
 * instances. In addition, the class contains <CODE>static</CODE>
 * methods for <CODE>char</CODE> arrays resizing, filling in,
 * reversing, non-zero elements counting, sorting, linear/binary
 * searching in for a value or sequence, case-sensitive or
 * case-insensitive 'less-equal-greater' comparison, mismatches
 * counting and hashing.
 **
 * @see ByteVector
 * @see DoubleVector
 * @see FloatVector
 * @see IntVector
 * @see LongVector
 * @see ShortVector
 * @see BooleanVector
 * @see ObjectVector
 * @see StrComparator
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public final class CharVector
 implements ReallyCloneable, Serializable, Indexable, Sortable,
            Verifiable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.8
 */
 private static final long serialVersionUID = 704428284230945389L;

/**
 * A constant initialized with an instance of empty
 * <CODE>char</CODE> array.
 **
 * @see #array
 */
 protected static final char[] EMPTY = {};

/**
 * The wrapped (encapsulated) custom <CODE>char</CODE> array.
 **
 * <VAR>array</VAR> must be non-<CODE>null</CODE>.
 **
 * @serial
 **
 * @see #EMPTY
 * @see CharVector#CharVector()
 * @see CharVector#CharVector(int)
 * @see CharVector#CharVector(char[])
 * @see #setArray(char[])
 * @see #array()
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, char)
 * @see #getCharAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #integrityCheck()
 */
 protected char[] array;

/**
 * Constructs an empty <CODE>char</CODE> vector.
 **
 * This constructor is used for the creation of a resizable vector.
 * The length of such a vector is changed only by
 * <CODE>resize(int)</CODE> and <CODE>ensureSize(int)</CODE>
 * methods.
 **
 * @see CharVector#CharVector(int)
 * @see CharVector#CharVector(char[])
 * @see #array()
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, char)
 * @see #getCharAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #toString()
 */
 public CharVector()
 {
  this.array = EMPTY;
 }

/**
 * Constructs a new <CODE>char</CODE> vector of the specified
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
 * @see CharVector#CharVector()
 * @see CharVector#CharVector(char[])
 * @see #array()
 * @see #length()
 * @see #setAt(int, char)
 * @see #getCharAt(int)
 * @see #copyAt(int, int, int)
 * @see #fill(char[], int, int, char)
 * @see #clone()
 * @see #toString()
 */
 public CharVector(int size)
 {
  if (size < 0)
   size = -1 >>> 1;
  this.array = new char[size];
 }

/**
 * Constructs a new <CODE>char</CODE> array wrapper.
 **
 * This constructor is used for the creation of a vector which wraps
 * the specified array (without copying it). The wrapped array may
 * be further replaced with another one only by
 * <CODE>setArray(char[])</CODE> and by <CODE>resize(int)</CODE>,
 * <CODE>ensureSize(int)</CODE> methods.
 **
 * @param array
 * the <CODE>char</CODE> array (must be non-<CODE>null</CODE>) to be
 * wrapped.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see CharVector#CharVector()
 * @see CharVector#CharVector(int)
 * @see #setArray(char[])
 * @see #array()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, char)
 * @see #getCharAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #toString()
 **
 * @since 2.0
 */
 public CharVector(char[] array)
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
 * the <CODE>char</CODE> array (must be non-<CODE>null</CODE>) to be
 * wrapped.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see CharVector#CharVector()
 * @see CharVector#CharVector(char[])
 * @see #array()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, char)
 * @see #getCharAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 **
 * @since 2.0
 */
 public void setArray(char[] array)
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
 * the <CODE>char</CODE> array (not <CODE>null</CODE>), which is
 * wrapped.
 **
 * @see CharVector#CharVector(char[])
 * @see #setArray(char[])
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 **
 * @since 1.8
 */
 public final char[] array()
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
 * @see #setArray(char[])
 * @see #array()
 * @see #setAt(int, char)
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #getCharAt(int)
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
 * <CODE>new Character(array()[index])</CODE>.
 **
 * @param index
 * the index (must be in the range) at which to return an element.
 * @return
 * an element (instance of <CODE>Character</CODE>) at
 * <VAR>index</VAR>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #getCharAt(int)
 * @see #array()
 * @see #length()
 */
 public Object getAt(int index)
  throws ArrayIndexOutOfBoundsException
 {
  return new Character(this.array[index]);
 }

/**
 * Returns value of the element at the specified index.
 **
 * The result is the same as of <CODE>array()[index]</CODE>.
 **
 * @param index
 * the index (must be in the range) at which to return an element.
 * @return
 * a <CODE>char</CODE> element at <VAR>index</VAR>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 **
 * @see #array()
 * @see #length()
 * @see #setAt(int, char)
 * @see #resize(int)
 * @see #ensureSize(int)
 */
 public final char getCharAt(int index)
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
 * @see #setArray(char[])
 * @see #array()
 * @see #length()
 * @see #getCharAt(int)
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #copyAt(int, int, int)
 * @see #fill(char[], int, int, char)
 */
 public void setAt(int index, char value)
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
 * @see #setAt(int, char)
 * @see #getCharAt(int)
 * @see #resize(int)
 * @see #ensureSize(int)
 */
 public void copyAt(int srcOffset, int destOffset, int len)
  throws ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   char[] array = this.array;
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
 * @see CharVector#CharVector(int)
 * @see #setArray(char[])
 * @see #array()
 * @see #length()
 * @see #ensureSize(int)
 * @see #resize(char[], int)
 */
 public void resize(int size)
 {
  int len;
  char[] array = this.array;
  if ((len = array.length) != size)
  {
   char[] newArray = EMPTY;
   if (size != 0)
   {
    if (len > size)
     if (size < 0)
      size = -1 >>> 1;
      else len = size;
    System.arraycopy(array, 0, newArray = new char[size], 0, len);
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
 * @see #setAt(int, char)
 * @see #resize(int)
 * @see #ensureSize(char[], int)
 */
 public void ensureSize(int size)
 {
  int len;
  char[] array = this.array, newArray;
  if ((((len = array.length) - size) | size) < 0)
  {
   if (size < 0)
    size = -1 >>> 1;
   if ((len += len >> 1) >= size)
    size = len;
   System.arraycopy(array, 0,
    newArray = new char[size], 0, array.length);
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
 * @see #ensureSize(char[], int)
 * @see #fill(char[], int, int, char)
 */
 public static final char[] resize(char[] array, int size)
  throws NullPointerException
 {
  int len;
  if ((len = array.length) != size)
  {
   char[] newArray = EMPTY;
   if (size != 0)
   {
    if (len > size)
     if (size < 0)
      size = -1 >>> 1;
      else len = size;
    System.arraycopy(array, 0, newArray = new char[size], 0, len);
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
 * @see #resize(char[], int)
 * @see #fill(char[], int, int, char)
 */
 public static final char[] ensureSize(char[] array, int size)
  throws NullPointerException
 {
  int len;
  if ((((len = array.length) - size) | size) < 0)
  {
   if (size < 0)
    size = -1 >>> 1;
   if ((len += len >> 1) >= size)
    size = len;
   char[] newArray;
   System.arraycopy(array, 0,
    newArray = new char[size], 0, array.length);
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
 * @see #indexOf(char[], int, int, int, char[], boolean)
 * @see #lastIndexOf(char[], int, int, int, char[], boolean)
 * @see #quickSort(char[], int, int)
 * @see #binarySearch(char[], int, int, char)
 **
 * @since 2.0
 */
 public static final void fill(char[] array, int offset, int len,
         char value)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int next = array.length, block;
  if (len > 0)
  {
   next = array[(block = offset) + (--len)];
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
 * @see #countNonZero(char[])
 * @see #indexOf(char, int, char[], boolean)
 * @see #lastIndexOf(char, int, char[], boolean)
 * @see #hashCode(char[], boolean)
 * @see #equals(char[], char[])
 * @see #compare(char[], int, int, char[], int, int, boolean)
 * @see #mismatches(char[], int, char[], int, int, boolean)
 */
 public static final void reverse(char[] array)
  throws NullPointerException
 {
  int offset = 0, len = array.length;
  while (--len > offset)
  {
   char value = array[offset];
   array[offset++] = array[len];
   array[len] = value;
  }
 }

/**
 * Count non-zero elements in a given array.
 **
 * This method returns the count of elements of <VAR>array</VAR>
 * which are not equal to zero.
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
 * @see #fill(char[], int, int, char)
 * @see #equals(char[], char[])
 * @see #compare(char[], int, int, char[], int, int, boolean)
 * @see #mismatches(char[], int, char[], int, int, boolean)
 **
 * @since 2.0
 */
 public static final int countNonZero(char[] array)
  throws NullPointerException
 {
  int offset = array.length, count = 0;
  while (offset-- > 0)
   if (array[offset] != 0)
    count++;
  return count;
 }

/**
 * Searches forward for value in a given array.
 **
 * Negative <VAR>index</VAR> is treated as zero, too big
 * <VAR>index</VAR> is treated as <CODE>length</CODE> of
 * <VAR>array</VAR>. Characters case is ignored only if
 * <VAR>ignoreCase</VAR>. If <VAR>value</VAR> is not found then the
 * result is <CODE>-1</CODE>.
 **
 * @param value
 * the character to sequentially search for.
 * @param index
 * the first index, from which to begin forward searching.
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be searched in.
 * @param ignoreCase
 * <CODE>true</CODE> if and only if characters case is ignored.
 * @return
 * the index (non-negative) of the found value or <CODE>-1</CODE>
 * (if not found).
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #lastIndexOf(char, int, char[], boolean)
 * @see #indexOf(char[], int, int, int, char[], boolean)
 * @see #binarySearch(char[], int, int, char)
 * @see #equals(char[], char[])
 * @see #compare(char[], int, int, char[], int, int, boolean)
 */
 public static final int indexOf(char value, int index,
         char[] array, boolean ignoreCase)
  throws NullPointerException
 {
  if (index <= 0)
   index = 0;
  index--;
  int len = array.length;
  if (ignoreCase)
  {
   char temp, upper;
   char lower = Character.toLowerCase(upper =
    Character.toUpperCase(value));
   while (++index < len && (temp = array[index]) != value &&
          (temp = Character.toUpperCase(temp)) != upper &&
          Character.toLowerCase(temp) != lower);
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
 * <VAR>array</VAR> minus one. Characters case is ignored only if
 * <VAR>ignoreCase</VAR>. If <VAR>value</VAR> is not found then the
 * result is <CODE>-1</CODE>.
 **
 * @param value
 * the character to sequentially search for.
 * @param index
 * the first index, from which to begin backward searching.
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be searched in.
 * @param ignoreCase
 * <CODE>true</CODE> if and only if characters case is ignored.
 * @return
 * the index (non-negative) of the found value or <CODE>-1</CODE>
 * (if not found).
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #indexOf(char, int, char[], boolean)
 * @see #lastIndexOf(char[], int, int, int, char[], boolean)
 * @see #binarySearch(char[], int, int, char)
 * @see #reverse(char[])
 * @see #equals(char[], char[])
 * @see #compare(char[], int, int, char[], int, int, boolean)
 */
 public static final int lastIndexOf(char value, int index,
         char[] array, boolean ignoreCase)
  throws NullPointerException
 {
  if (index < 0)
   index = -1;
  int len;
  if ((len = array.length) <= index)
   index = len - 1;
  index++;
  if (ignoreCase)
  {
   char temp, upper;
   char lower = Character.toLowerCase(upper =
    Character.toUpperCase(value));
   while (index-- > 0 && (temp = array[index]) != value &&
          (temp = Character.toUpperCase(temp)) != upper &&
          Character.toLowerCase(temp) != lower);
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
 * treated as <CODE>length</CODE> of <VAR>array</VAR>. Characters
 * case is ignored only if <VAR>ignoreCase</VAR>. If the sequence is
 * not found then the result is <CODE>-1</CODE>.
 **
 * @param subArray
 * the array (must be non-<CODE>null</CODE>) specifying the sequence
 * of characters to search for.
 * @param offset
 * the offset (must be in the range) of the sequence in
 * <VAR>subArray</VAR>.
 * @param len
 * the length of the sequence.
 * @param index
 * the first index, from which to begin forward searching.
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be searched in.
 * @param ignoreCase
 * <CODE>true</CODE> if and only if characters case is ignored.
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
 * @see #indexOf(char, int, char[], boolean)
 * @see #lastIndexOf(char[], int, int, int, char[], boolean)
 * @see #equals(char[], char[])
 * @see #compare(char[], int, int, char[], int, int, boolean)
 */
 public static final int indexOf(char[] subArray, int offset,
         int len, int index, char[] array, boolean ignoreCase)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int curOffset = subArray.length, arrayLen = array.length;
  if (index <= 0)
   index = 0;
  if (len > 0)
  {
   arrayLen -= len;
   char value = subArray[offset];
   curOffset = subArray[len += offset - 1];
   index--;
   if (ignoreCase)
   {
    char temp, upper;
    char lower = Character.toLowerCase(upper =
     Character.toUpperCase(value));
    while (++index <= arrayLen)
     if ((temp = array[index]) == value ||
         (temp = Character.toUpperCase(temp)) == upper ||
         Character.toLowerCase(temp) == lower)
     {
      curOffset = offset;
      int curIndex = index;
      while (++curOffset <= len)
      {
       char curValue = subArray[curOffset];
       if ((temp = array[++curIndex]) != curValue)
       {
        temp = Character.toUpperCase(temp);
        if ((curValue = Character.toUpperCase(curValue)) != temp &&
            Character.toLowerCase(curValue) !=
            Character.toLowerCase(temp))
         break;
       }
      }
      if (curOffset > len)
       break;
     }
   }
    else while (++index <= arrayLen)
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
 * <VAR>array</VAR> minus one. Characters case is ignored only if
 * <VAR>ignoreCase</VAR>. If the sequence is not found then the
 * result is <CODE>-1</CODE>.
 **
 * @param subArray
 * the array (must be non-<CODE>null</CODE>) specifying the sequence
 * of characters to search for.
 * @param offset
 * the offset (must be in the range) of the sequence in
 * <VAR>subArray</VAR>.
 * @param len
 * the length of the sequence.
 * @param index
 * the first index, from which to begin backward searching.
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be searched in.
 * @param ignoreCase
 * <CODE>true</CODE> if and only if characters case is ignored.
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
 * @see #lastIndexOf(char, int, char[], boolean)
 * @see #indexOf(char[], int, int, int, char[], boolean)
 * @see #equals(char[], char[])
 * @see #compare(char[], int, int, char[], int, int, boolean)
 */
 public static final int lastIndexOf(char[] subArray, int offset,
         int len, int index, char[] array, boolean ignoreCase)
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
   char value = subArray[offset];
   curOffset = subArray[len += offset - 1];
   index++;
   if (ignoreCase)
   {
    char temp, upper;
    char lower = Character.toLowerCase(upper =
     Character.toUpperCase(value));
    while (index-- > 0)
     if ((temp = array[index]) == value ||
         (temp = Character.toUpperCase(temp)) == upper ||
         Character.toLowerCase(temp) == lower)
     {
      curOffset = offset;
      arrayLen = index;
      while (++curOffset <= len)
      {
       char curValue = subArray[curOffset];
       if ((temp = array[++arrayLen]) != curValue)
       {
        temp = Character.toUpperCase(temp);
        if ((curValue = Character.toUpperCase(curValue)) != temp &&
            Character.toLowerCase(curValue) !=
            Character.toLowerCase(temp))
         break;
       }
      }
      if (curOffset > len)
       break;
     }
   }
    else while (index-- > 0)
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
 * Produces a hash code value for a given array.
 **
 * This method mixes all the characters (or their lower-case
 * equivalents if <VAR>ignoreCase</VAR>) of <VAR>array</VAR> to
 * produce a single hash code value.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to evaluate hash of.
 * @param ignoreCase
 * <CODE>true</CODE> if and only if characters case is ignored.
 * @return
 * the hash code value for <VAR>array</VAR>.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #hashCode()
 * @see #fill(char[], int, int, char)
 * @see #reverse(char[])
 * @see #countNonZero(char[])
 * @see #indexOf(char, int, char[], boolean)
 * @see #lastIndexOf(char, int, char[], boolean)
 * @see #equals(char[], char[])
 * @see #compare(char[], int, int, char[], int, int, boolean)
 * @see #mismatches(char[], int, char[], int, int, boolean)
 */
 public static final int hashCode(char[] array, boolean ignoreCase)
  throws NullPointerException
 {
  int code = 0, offset = 0, len = array.length;
  if (ignoreCase)
   while (offset < len)
   {
    code ^= Character.toLowerCase(array[offset++]);
    code = (code << 5) - code;
   }
  while (offset < len)
  {
   code ^= array[offset++];
   code = (code << 5) - code;
  }
  return code ^ offset;
 }

/**
 * Tests whether or not the specified two arrays are equal.
 **
 * This method returns <CODE>true</CODE> if and only if both of the
 * arrays are of the same length and all the characters of the first
 * array are equal to the corresponding characters of the second
 * array (matching their case).
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
 * @see #fill(char[], int, int, char)
 * @see #reverse(char[])
 * @see #countNonZero(char[])
 * @see #indexOf(char, int, char[], boolean)
 * @see #lastIndexOf(char, int, char[], boolean)
 * @see #indexOf(char[], int, int, int, char[], boolean)
 * @see #lastIndexOf(char[], int, int, int, char[], boolean)
 * @see #hashCode(char[], boolean)
 * @see #compare(char[], int, int, char[], int, int, boolean)
 * @see #mismatches(char[], int, char[], int, int, boolean)
 **
 * @since 2.0
 */
 public static final boolean equals(char[] arrayA, char[] arrayB)
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
 * This method returns the count of characters of the first array
 * region which are not equal to the corresponding characters of the
 * second array region (matching case or not). Negative
 * <VAR>len</VAR> is treated as zero.
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
 * @param ignoreCase
 * <CODE>true</CODE> if and only if characters case is ignored.
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
 * @see #fill(char[], int, int, char)
 * @see #reverse(char[])
 * @see #countNonZero(char[])
 * @see #hashCode(char[], boolean)
 * @see #equals(char[], char[])
 * @see #compare(char[], int, int, char[], int, int, boolean)
 **
 * @since 2.0
 */
 public static final int mismatches(char[] arrayA, int offsetA,
         char[] arrayB, int offsetB, int len, boolean ignoreCase)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int count = arrayA.length - arrayB.length;
  count = 0;
  if (len > 0)
  {
   char value = arrayA[offsetA];
   value = arrayA[offsetA + len - 1];
   value = arrayB[offsetB];
   value = arrayB[offsetB + len - 1];
   if (offsetA != offsetB || arrayA != arrayB)
    if (ignoreCase)
     do
     {
      char temp = arrayB[offsetB++];
      if ((value = arrayA[offsetA++]) != temp)
      {
       temp = Character.toUpperCase(temp);
       if ((value = Character.toUpperCase(value)) != temp &&
           Character.toLowerCase(value) !=
           Character.toLowerCase(temp))
        count++;
      }
     } while (--len > 0);
     else do
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
 * This method returns a signed integer indicating case-sensitive or
 * case-insensitive 'less-equal-greater' relation between the
 * specified array regions of characters (the absolute value of the
 * result, in fact, is the distance between the first found mismatch
 * and the end of the bigger-length region). Negative
 * <VAR>lenA</VAR> is treated as zero. Negative <VAR>lenB</VAR> is
 * treated as zero. Important notes: the content of array regions is
 * compared before comparing their length.
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
 * @param ignoreCase
 * <CODE>true</CODE> if and only if characters case is ignored.
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
 * @see #fill(char[], int, int, char)
 * @see #reverse(char[])
 * @see #indexOf(char, int, char[], boolean)
 * @see #lastIndexOf(char, int, char[], boolean)
 * @see #hashCode(char[], boolean)
 * @see #equals(char[], char[])
 * @see #mismatches(char[], int, char[], int, int, boolean)
 */
 public static final int compare(char[] arrayA, int offsetA,
         int lenA, char[] arrayB, int offsetB, int lenB,
         boolean ignoreCase)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  char value = (char)(arrayA.length - arrayB.length);
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
   char temp = value;
   while (lenA > 0)
   {
    temp = arrayB[offsetB++];
    if ((value = arrayA[offsetA++]) != temp)
    {
     if (!ignoreCase)
      break;
     temp = Character.toUpperCase(temp);
     if ((value = Character.toUpperCase(value)) != temp)
     {
      temp = Character.toLowerCase(temp);
      if ((value = Character.toLowerCase(value)) != temp)
       break;
     }
    }
    lenA--;
   }
   if (lenA > 0)
   {
    if (lenB <= 0)
     lenB = -lenB;
    lenB += lenA;
    if (value < temp)
     lenB = -lenB;
   }
  }
  return lenB;
 }

/**
 * Sorts the elements in the region of a given array using 'Quick'
 * algorithm.
 **
 * Elements in the region are sorted into ascending natural
 * (unsigned) order. But equal elements may be reordered (since the
 * algorithm is not 'stable'). A small working stack is allocated
 * (since the algorithm is 'in-place' and recursive). The algorithm
 * cost is <CODE>O(log(len) * len)</CODE> typically, but may be of
 * <CODE>O(len * len)</CODE> in the worst case (which is rare, in
 * fact). Negative <VAR>len</VAR> is treated as zero. If an
 * exception is thrown then <VAR>array</VAR> remains unchanged. Else
 * the region content is altered.
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
 * @see #counterSort(char[], int, int)
 * @see #binarySearch(char[], int, int, char)
 * @see #fill(char[], int, int, char)
 */
 public static final void quickSort(char[] array,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   char value = array[offset], temp;
   if (len > 1)
   {
    value = array[len += offset - 1];
    int[] bounds = new int[(JavaConsts.INT_SIZE - 3) << 1];
    int level = 2, index, last;
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
   }
  }
  len = array.length;
 }

/**
 * Sorts the elements in the region of a given array by counting the
 * amount of each possible value in it.
 **
 * Elements in the region are sorted into ascending natural
 * (unsigned) order. A working (counter) buffer of
 * <CODE>(CHAR_MASK + 1)</CODE> (or <CODE>(BYTE_MASK + 1)</CODE> if
 * all characters in the region are bytes) integer values is
 * allocated. The algorithm cost is linear but only for large
 * regions. Negative <VAR>len</VAR> is treated as zero. If an
 * exception is thrown then <VAR>array</VAR> remains unchanged. Else
 * the region content is altered.
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
 * @see #quickSort(char[], int, int)
 * @see #binarySearch(char[], int, int, char)
 * @see #fill(char[], int, int, char)
 **
 * @since 2.0
 */
 public static final void counterSort(char[] array,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   char value = array[offset];
   if (len > 1)
   {
    value = array[len += offset - 1];
    int[] counter = null;
    int count = offset;
    if (JavaConsts.BYTE_MASK < JavaConsts.CHAR_MASK &&
        JavaConsts.BYTE_MASK + 1 > 0)
    {
     counter = new int[JavaConsts.BYTE_MASK + 1];
     do
     {
      if ((value = array[offset]) > JavaConsts.BYTE_MASK)
       break;
      counter[value]++;
     } while (++offset <= len);
    }
    if (offset <= len)
    {
     int[] newCounter = new int[JavaConsts.CHAR_MASK + 1 > 0 ?
      JavaConsts.CHAR_MASK + 1 : -1 >>> 1];
     if (offset > count)
      System.arraycopy(counter, 0,
       newCounter, 0, JavaConsts.BYTE_MASK + 1);
     counter = newCounter;
     do
     {
      counter[array[offset]]++;
     } while (++offset <= len);
    }
    value = 0;
    offset = count;
    do
    {
     for (count = counter[value]; count > 0; count--)
      array[offset++] = value;
     value++;
    } while (offset <= len);
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
 * for <VAR>value</VAR>.
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
 * the case-sensitive value to search for.
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
 * @see #indexOf(char, int, char[], boolean)
 * @see #lastIndexOf(char, int, char[], boolean)
 * @see #quickSort(char[], int, int)
 * @see #counterSort(char[], int, int)
 * @see #fill(char[], int, int, char)
 */
 public static final int binarySearch(char[] array,
         int offset, int len, char value)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   int middle;
   char temp = array[offset];
   temp = array[len += offset - 1];
   do
   {
    if ((temp = array[middle = (offset + len) >>> 1]) > value)
     len = middle - 1;
     else if (temp != value)
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
 * @see CharVector#CharVector()
 * @see #array()
 * @see #getCharAt(int)
 * @see #equals(java.lang.Object)
 */
 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof CharVector && obj != this)
   {
    CharVector vector = (CharVector)obj;
    vector.array = (char[])vector.array.clone();
    return obj;
   }
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * Computes and returns a hash code value for the object.
 **
 * This method mixes lower-case equivalents of all the characters of
 * <CODE>this</CODE> vector to produce a single hash code value.
 **
 * @return
 * a hash code value for <CODE>this</CODE> object.
 **
 * @see #hashCode(char[], boolean)
 * @see #array()
 * @see #length()
 * @see #getCharAt(int)
 * @see #equals(java.lang.Object)
 */
 public int hashCode()
 {
  return hashCode(this.array, true);
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
 * @see CharVector#CharVector()
 * @see #equals(char[], char[])
 * @see #array()
 * @see #length()
 * @see #getCharAt(int)
 * @see #hashCode()
 * @see #greaterThan(java.lang.Object)
 */
 public boolean equals(Object obj)
 {
  return obj == this || obj instanceof CharVector &&
   equals(this.array, ((CharVector)obj).array);
 }

/**
 * Tests for being semantically greater than the argument.
 **
 * The result is <CODE>true</CODE> if and only if <VAR>obj</VAR> is
 * instance of <CODE>this</CODE> class and <CODE>this</CODE> object
 * is greater than the specified object. Vectors are compared in the
 * element-by-element case-sensitive manner, starting at index
 * <CODE>0</CODE>.
 **
 * @param obj
 * the second compared object (may be <CODE>null</CODE>).
 * @return
 * <CODE>true</CODE> if <VAR>obj</VAR> is comparable with
 * <CODE>this</CODE> and <CODE>this</CODE> object is greater than
 * <VAR>obj</VAR>, else <CODE>false</CODE>.
 **
 * @see #compare(char[], int, int, char[], int, int, boolean)
 * @see #array()
 * @see #length()
 * @see #getCharAt(int)
 * @see #equals(java.lang.Object)
 **
 * @since 2.0
 */
 public boolean greaterThan(Object obj)
 {
  if (obj != this && obj instanceof CharVector)
  {
   char[] array = this.array, otherArray = ((CharVector)obj).array;
   if (compare(array, 0, array.length,
       otherArray, 0, otherArray.length, false) > 0)
    return true;
  }
  return false;
 }

/**
 * Converts <CODE>this</CODE> character vector to a string.
 **
 * All the characters of the wrapped array are concatenated together
 * (without spaces) to produce a single string (using
 * <CODE>String(char[])</CODE> constructor).
 **
 * @return
 * the string representation (not <CODE>null</CODE>) of
 * <CODE>this</CODE> object.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #array()
 * @see #length()
 */
 public String toString()
 {
  return new String(this.array);
 }

/**
 * Verifies <CODE>this</CODE> object for its integrity.
 **
 * For debug purpose only.
 **
 * @exception InternalError
 * if integrity violation is detected.
 **
 * @see CharVector#CharVector(char[])
 * @see #setArray(char[])
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
 * @see CharVector#CharVector(char[])
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

/*
 * @(#) src/net/sf/ivmaidns/util/ObjectVector.java --
 * Class for object array wrappers.
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
 * Class for object array wrappers.
 **
 * This class contains an <CODE>Object</CODE>-type array, and has
 * the possibility to resize (when required) the contained array.
 * This class supports serialization (only if all its custom-type
 * elements are serializable), shallow cloning and comparison of its
 * instances. In addition, the class contains <CODE>static</CODE>
 * methods for <CODE>Object</CODE> arrays resizing, filling in,
 * reversing, non-<CODE>null</CODE> elements counting, linear/binary
 * searching in for a value or sequence, equality testing and
 * mismatches counting, hashing and 'to-string' conversion,
 * 'greater-than' comparison and sorting according to the specified
 * comparator, 'less-equal-greater' comparison for
 * <CODE>String</CODE> arrays. As a container, this class is similar
 * to <CODE>ConstVector</CODE>, except that any vector of this class
 * is mutable and the contained array is the exact instance of
 * <CODE>Object[]</CODE>.
 **
 * @see ConstVector
 * @see ByteVector
 * @see CharVector
 * @see DoubleVector
 * @see FloatVector
 * @see IntVector
 * @see LongVector
 * @see ShortVector
 * @see BooleanVector
 * @see GComparator
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public final class ObjectVector
 implements ReallyCloneable, Serializable, Indexable, Sortable,
            Verifiable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.8
 */
 private static final long serialVersionUID = 6672186998322310510L;

/**
 * A constant initialized with an exact instance of empty
 * <CODE>Object</CODE> array.
 **
 * @see #array
 */
 protected static final Object[] EMPTY = {};

/**
 * The encapsulated <CODE>Object</CODE>-type array.
 **
 * <VAR>array</VAR> must be the exact instance of
 * <CODE>Object[]</CODE>.
 **
 * @serial
 **
 * @see #EMPTY
 * @see ObjectVector#ObjectVector()
 * @see ObjectVector#ObjectVector(int)
 * @see ObjectVector#ObjectVector(java.lang.Object[])
 * @see #setArrayCloned(java.lang.Object[])
 * @see #array()
 * @see #length()
 * @see #toArray()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, java.lang.Object)
 * @see #getAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #integrityCheck()
 */
 protected Object[] array;

/**
 * Constructs an empty <CODE>Object</CODE> vector.
 **
 * This constructor is used for the creation of a resizable vector.
 * The length of such a vector is changed only by
 * <CODE>resize(int)</CODE> and <CODE>ensureSize(int)</CODE>
 * methods.
 **
 * @see ObjectVector#ObjectVector(int)
 * @see ObjectVector#ObjectVector(java.lang.Object[])
 * @see #array()
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, java.lang.Object)
 * @see #getAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #toString()
 */
 public ObjectVector()
 {
  this.array = EMPTY;
 }

/**
 * Constructs a new <CODE>Object</CODE> vector of the specified
 * length.
 **
 * This constructor is typically used for the creation of a vector
 * with a fixed size. All the elements of the created vector are set
 * to <CODE>null</CODE>.
 **
 * @param size
 * the initial length (unsigned) of the vector to be created.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ObjectVector#ObjectVector()
 * @see ObjectVector#ObjectVector(java.lang.Object[])
 * @see #array()
 * @see #length()
 * @see #setAt(int, java.lang.Object)
 * @see #getAt(int)
 * @see #copyAt(int, int, int)
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 * @see #clone()
 * @see #toString()
 */
 public ObjectVector(int size)
 {
  if (size < 0)
   size = -1 >>> 1;
  this.array = new Object[size];
 }

/**
 * Constructs a new initialized <CODE>Object</CODE>-type container.
 **
 * This constructor is used for the creation of a vector which
 * contains the copy of the specified array (the component type of
 * the copy is always set to <CODE>Object</CODE> type). The
 * encapsulated array may be further replaced with another one only
 * by <CODE>setArrayCloned(Object[])</CODE> and by
 * <CODE>resize(int)</CODE>, <CODE>ensureSize(int)</CODE> methods.
 **
 * @param array
 * the <CODE>Object</CODE> array (must be non-<CODE>null</CODE>) to
 * be copied and encapsulated.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ObjectVector#ObjectVector()
 * @see ObjectVector#ObjectVector(int)
 * @see #setArrayCloned(java.lang.Object[])
 * @see #array()
 * @see #toArray()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, java.lang.Object)
 * @see #getAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 * @see #toString()
 */
 public ObjectVector(Object[] array)
  throws NullPointerException
 {
  int len = array.length;
  System.arraycopy(array, 0, this.array = new Object[len], 0, len);
 }

/**
 * Sets another array to be encapsulated by <CODE>this</CODE>
 * vector.
 **
 * If an exception is thrown then <CODE>this</CODE> vector remains
 * unchanged. Else this method creates a new <CODE>Object</CODE>
 * array and copies the content of the supplied <VAR>array</VAR>
 * into it.
 **
 * @param array
 * the <CODE>Object</CODE> array (must be non-<CODE>null</CODE>) to
 * be copied and encapsulated.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ObjectVector#ObjectVector()
 * @see ObjectVector#ObjectVector(java.lang.Object[])
 * @see #array()
 * @see #toArray()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #setAt(int, java.lang.Object)
 * @see #getAt(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 **
 * @since 2.0
 */
 public void setArrayCloned(Object[] array)
  throws NullPointerException
 {
  int len = array.length;
  Object[] newArray;
  System.arraycopy(array, 0, newArray = new Object[len], 0, len);
  this.array = newArray;
 }

/**
 * Returns array encapsulated by <CODE>this</CODE> vector.
 **
 * Important notes: this method does not copy <VAR>array</VAR>.
 **
 * @return
 * the encapsulated array (not <CODE>null</CODE>).
 **
 * @see ObjectVector#ObjectVector(java.lang.Object[])
 * @see #setArrayCloned(java.lang.Object[])
 * @see #toArray()
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 **
 * @since 1.8
 */
 public final Object[] array()
 {
  return this.array;
 }

/**
 * Returns a newly created array filled with the elements of
 * <CODE>this</CODE> vector.
 **
 * The result is the exact instance of <CODE>Object[]</CODE> (with
 * the same <CODE>length</CODE> as of this vector).
 **
 * @return
 * the copy (not <CODE>null</CODE>) of the encapsulated array.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ObjectVector#ObjectVector(java.lang.Object[])
 * @see #setArrayCloned(java.lang.Object[])
 * @see #array()
 * @see #length()
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #copyAt(int, int, int)
 * @see #clone()
 */
 public Object[] toArray()
 {
  return (Object[])this.array.clone();
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
 * @see #setArrayCloned(java.lang.Object[])
 * @see #array()
 * @see #setAt(int, java.lang.Object)
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #getAt(int)
 **
 * @since 1.8
 */
 public int length()
 {
  return this.array.length;
 }

/**
 * Returns value of the element at the specified index.
 **
 * The result is the same as of <CODE>array()[index]</CODE>.
 **
 * @param index
 * the index (must be in the range) at which to return an element.
 * @return
 * an element (may be <CODE>null</CODE>) at <VAR>index</VAR>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 **
 * @see #array()
 * @see #length()
 * @see #setAt(int, java.lang.Object)
 * @see #resize(int)
 * @see #ensureSize(int)
 */
 public Object getAt(int index)
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
 * the value (may be <CODE>null</CODE>) to be assigned.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 **
 * @see #setArrayCloned(java.lang.Object[])
 * @see #array()
 * @see #length()
 * @see #getAt(int)
 * @see #resize(int)
 * @see #ensureSize(int)
 * @see #copyAt(int, int, int)
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 */
 public void setAt(int index, Object value)
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
 * @see #setAt(int, java.lang.Object)
 * @see #getAt(int)
 * @see #resize(int)
 * @see #ensureSize(int)
 */
 public void copyAt(int srcOffset, int destOffset, int len)
  throws ArrayIndexOutOfBoundsException
 {
  if (len > 0)
  {
   Object[] array = this.array;
   System.arraycopy(array, srcOffset, array, destOffset, len);
  }
 }

/**
 * Resizes <CODE>this</CODE> vector.
 **
 * This method changes the length of <CODE>this</CODE> vector to the
 * specified one. Important notes: if size (length) of the vector
 * grows then its new elements are set to <CODE>null</CODE>. If an
 * exception is thrown then <CODE>this</CODE> vector remains
 * unchanged.
 **
 * @param size
 * the (unsigned) length of <CODE>this</CODE> vector to set.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ObjectVector#ObjectVector(int)
 * @see #setArrayCloned(java.lang.Object[])
 * @see #array()
 * @see #length()
 * @see #ensureSize(int)
 * @see #resize(java.lang.Object[], int)
 */
 public void resize(int size)
 {
  int len;
  Object[] array = this.array;
  if ((len = array.length) != size)
  {
   Object[] newArray = EMPTY;
   if (size != 0)
   {
    if (len > size)
     if (size < 0)
      size = -1 >>> 1;
      else len = size;
    System.arraycopy(array, 0, newArray = new Object[size], 0, len);
   }
   this.array = newArray;
  }
 }

/**
 * Ensures the size (capacity) of <CODE>this</CODE> vector.
 **
 * This method changes (only if <VAR>size</VAR> is greater than
 * <CODE>length()</CODE>) the length of <CODE>this</CODE> vector to
 * a value not less than <VAR>size</VAR>. Important notes: if size
 * (length) of the vector grows then its new elements are set to
 * <CODE>null</CODE>. If an exception is thrown then
 * <CODE>this</CODE> vector remains unchanged.
 **
 * @param size
 * the (unsigned) length of <CODE>this</CODE> vector to be ensured.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #array()
 * @see #length()
 * @see #setAt(int, java.lang.Object)
 * @see #resize(int)
 * @see #ensureSize(java.lang.Object[], int)
 */
 public void ensureSize(int size)
 {
  int len;
  Object[] array = this.array, newArray;
  if ((((len = array.length) - size) | size) < 0)
  {
   if (size < 0)
    size = -1 >>> 1;
   if ((len += len >> 1) >= size)
    size = len;
   System.arraycopy(array, 0,
    newArray = new Object[size], 0, array.length);
   this.array = newArray;
  }
 }

/**
 * Resizes a given array.
 **
 * This method 'changes' (creates a new <CODE>Object</CODE>-type
 * array and copies the content into it) the length of the specified
 * array to the specified one. Important notes: <VAR>array</VAR>
 * elements are not changed; if <CODE>length</CODE> of
 * <VAR>array</VAR> is the same as <VAR>size</VAR> then
 * <VAR>array</VAR> is returned else <VAR>array</VAR> content is
 * copied into the result (all new elements are set to
 * <CODE>null</CODE> in it, the component type of the new array is
 * always set to <CODE>Object</CODE>).
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
 * @see #ensureSize(java.lang.Object[], int)
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 */
 public static final Object[] resize(Object[] array, int size)
  throws NullPointerException
 {
  int len;
  if ((len = array.length) != size)
  {
   Object[] newArray = EMPTY;
   if (size != 0)
   {
    if (len > size)
     if (size < 0)
      size = -1 >>> 1;
      else len = size;
    System.arraycopy(array, 0, newArray = new Object[size], 0, len);
   }
   array = newArray;
  }
  return array;
 }

/**
 * Ensures the length (capacity) of a given array.
 **
 * This method 'grows' (creates a new appropriate
 * <CODE>Object</CODE>-type array and copies the content into it,
 * but only if <VAR>size</VAR> is greater than <CODE>length</CODE>
 * of <VAR>array</VAR>) the length of <VAR>array</VAR>. Important
 * notes: <VAR>array</VAR> elements are not changed; if
 * <CODE>length</CODE> of <VAR>array</VAR> is greater or the same as
 * <VAR>size</VAR> then <VAR>array</VAR> is returned else
 * <VAR>array</VAR> content is copied into the result (all new
 * elements are set to <CODE>null</CODE> in it, the component type
 * of the new array is always set to <CODE>Object</CODE>).
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
 * @see #resize(java.lang.Object[], int)
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 */
 public static final Object[] ensureSize(Object[] array, int size)
  throws NullPointerException
 {
  int len;
  if ((((len = array.length) - size) | size) < 0)
  {
   if (size < 0)
    size = -1 >>> 1;
   if ((len += len >> 1) >= size)
    size = len;
   Object[] newArray;
   System.arraycopy(array, 0,
    newArray = new Object[size], 0, array.length);
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
 * the value (may be <CODE>null</CODE>) to fill with.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>array</VAR> minus
 * <VAR>len</VAR>).
 * @exception ArrayStoreException
 * if <VAR>value</VAR> could not be stored into <VAR>array</VAR>.
 **
 * @see #array()
 * @see #copyAt(int, int, int)
 * @see #toString(java.lang.Object[], int, int, char)
 * @see #sort(java.lang.Object[], int, int, net.sf.ivmaidns.util.GComparator)
 * @see #binarySearch(java.lang.Object[], int, int,
 * java.lang.Object, net.sf.ivmaidns.util.GComparator)
 **
 * @since 2.0
 */
 public static final void fill(Object[] array, int offset, int len,
         Object value)
  throws NullPointerException, ArrayIndexOutOfBoundsException,
         ArrayStoreException
 {
  int next = array.length, block;
  if (len > 0)
  {
   Object temp;
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
 * @see #countNonNull(java.lang.Object[])
 * @see #indexOf(java.lang.Object, int, java.lang.Object[])
 * @see #lastIndexOf(java.lang.Object, int, java.lang.Object[])
 * @see #hashCode(java.lang.Object[])
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #mismatches(java.lang.Object[], int, java.lang.Object[],
 * int, int)
 */
 public static final void reverse(Object[] array)
  throws NullPointerException
 {
  int offset = 0, len = array.length;
  while (--len > offset)
  {
   Object value = array[offset];
   array[offset++] = array[len];
   array[len] = value;
  }
 }

/**
 * Count non-<CODE>null</CODE> elements in a given array.
 **
 * This method returns the count of elements of <VAR>array</VAR>
 * which are not <CODE>null</CODE>.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to count
 * non-<CODE>null</CODE> elements in.
 * @return
 * the count (non-negative and not greater than <CODE>length</CODE>
 * of <VAR>array</VAR>) of non-<CODE>null</CODE> elements.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #mismatches(java.lang.Object[], int, java.lang.Object[],
 * int, int)
 **
 * @since 2.0
 */
 public static final int countNonNull(Object[] array)
  throws NullPointerException
 {
  int offset = array.length, count = 0;
  while (offset-- > 0)
   if (array[offset] != null)
    count++;
  return count;
 }

/**
 * Searches forward for value in a given array.
 **
 * Negative <VAR>index</VAR> is treated as zero, too big
 * <VAR>index</VAR> is treated as <CODE>length</CODE> of
 * <VAR>array</VAR>. If <VAR>value</VAR> is not found then the
 * result is <CODE>-1</CODE>. Important notes: <VAR>value</VAR> is
 * tested for equality against the elements.
 **
 * @param value
 * the value (may be <CODE>null</CODE>) to sequentially search for.
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
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #lastIndexOf(java.lang.Object, int, java.lang.Object[])
 * @see #indexOf(java.lang.Object[], int, int, int,
 * java.lang.Object[])
 */
 public static final int indexOf(Object value, int index,
         Object[] array)
  throws NullPointerException
 {
  if (index <= 0)
   index = 0;
  index--;
  int len = array.length;
  if (value != null)
   while (++index < len && !value.equals(array[index]));
   else while (++index < len && array[index] != null);
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
 * the result is <CODE>-1</CODE>. Important notes: <VAR>value</VAR>
 * is tested for equality against the elements.
 **
 * @param value
 * the value (may be <CODE>null</CODE>) to sequentially search for.
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
 * @see #reverse(java.lang.Object[])
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #indexOf(java.lang.Object, int, java.lang.Object[])
 * @see #lastIndexOf(java.lang.Object[], int, int, int,
 * java.lang.Object[])
 */
 public static final int lastIndexOf(Object value, int index,
         Object[] array)
  throws NullPointerException
 {
  if (index < 0)
   index = -1;
  int len;
  if ((len = array.length) <= index)
   index = len - 1;
  index++;
  if (value != null)
   while (index-- > 0 && !value.equals(array[index]));
   else while (index-- > 0 && array[index] != null);
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
 * Important notes: the elements of <VAR>subArray</VAR> are tested
 * for equality against the elements of <VAR>array</VAR>.
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
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #indexOf(java.lang.Object, int, java.lang.Object[])
 * @see #lastIndexOf(java.lang.Object[], int, int, int,
 * java.lang.Object[])
 */
 public static final int indexOf(Object[] subArray,
         int offset, int len, int index, Object[] array)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int curOffset = subArray.length, arrayLen = array.length;
  if (index <= 0)
   index = 0;
  if (len > 0)
  {
   arrayLen -= len;
   Object value = subArray[offset];
   Object temp = subArray[len += offset - 1];
   index--;
   while (++index <= arrayLen)
    if (value != null && value.equals(array[index]) ||
        value == null && array[index] == null)
    {
     curOffset = offset;
     int curIndex = index;
     while (++curOffset <= len)
     {
      curIndex++;
      if ((temp = subArray[curOffset]) != null)
      {
       if (!temp.equals(array[curIndex]))
        break;
      }
       else if (array[curIndex] != null)
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
 * result is <CODE>-1</CODE>. Important notes: the elements of
 * <VAR>subArray</VAR> are tested for equality against the elements
 * of <VAR>array</VAR>.
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
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #lastIndexOf(java.lang.Object, int, java.lang.Object[])
 * @see #indexOf(java.lang.Object[], int, int, int,
 * java.lang.Object[])
 */
 public static final int lastIndexOf(Object[] subArray,
         int offset, int len, int index, Object[] array)
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
   Object value = subArray[offset];
   Object temp = subArray[len += offset - 1];
   index++;
   while (index-- > 0)
    if (value != null && value.equals(array[index]) ||
        value == null && array[index] == null)
    {
     curOffset = offset;
     arrayLen = index;
     while (++curOffset <= len)
     {
      arrayLen++;
      if ((temp = subArray[curOffset]) != null)
      {
       if (!temp.equals(array[arrayLen]))
        break;
      }
       else if (array[arrayLen] != null)
        break;
     }
     if (curOffset > len)
      break;
    }
  }
  return index;
 }

/**
 * Converts the region of a given array to its string
 * representation.
 **
 * The string representations of values (if a value is
 * <CODE>null</CODE> then "null") of the specified region of
 * <VAR>array</VAR> are placed into the resulting string in the
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
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 */
 public static final String toString(Object[] array,
         int offset, int len, char separator)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int capacity = array.length;
  Object value;
  capacity = 0;
  if (len > 0)
  {
   value = array[offset];
   value = array[offset + len - 1];
   if ((capacity = len << 2) <= 24)
    capacity = 24;
  }
  StringBuffer sBuf = new StringBuffer(capacity);
  if (len > 0)
   do
   {
    sBuf.append((value = array[offset++]) != null ?
     value.toString() : "null");
    if (--len <= 0)
     break;
    sBuf.append(separator);
   } while (true);
  return new String(sBuf);
 }

/**
 * Produces a hash code value for a given array.
 **
 * This method mixes the hash codes of all the elements of
 * <VAR>array</VAR> to produce a single hash code value. Important
 * notes: if an element is <CODE>null</CODE> then its hash code is
 * assumed to be <CODE>0</CODE>.
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
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 * @see #countNonNull(java.lang.Object[])
 * @see #indexOf(java.lang.Object, int, java.lang.Object[])
 * @see #lastIndexOf(java.lang.Object, int, java.lang.Object[])
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #mismatches(java.lang.Object[], int, java.lang.Object[],
 * int, int)
 */
 public static final int hashCode(Object[] array)
  throws NullPointerException
 {
  int code = 0, offset = 0;
  Object value;
  for (int len = array.length; offset < len;
       code = (code << 5) - code)
   if ((value = array[offset++]) != null)
    code ^= value.hashCode();
  return code ^ offset;
 }

/**
 * Tests whether or not the specified two arrays are equal.
 **
 * This method returns <CODE>true</CODE> if and only if both of the
 * arrays are of the same length and all the elements of the first
 * array are equal to the corresponding elements of the second
 * array. Important notes: the component type of arrays is not
 * compared.
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
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 * @see #indexOf(java.lang.Object, int, java.lang.Object[])
 * @see #lastIndexOf(java.lang.Object, int, java.lang.Object[])
 * @see #hashCode(java.lang.Object[])
 * @see #mismatchRemain(java.lang.Object[], int, java.lang.Object[],
 * int, int)
 */
 public static final boolean equals(Object[] arrayA,
         Object[] arrayB)
  throws NullPointerException
 {
  int offset = arrayA.length;
  Object value;
  if (arrayA != arrayB)
   if (arrayB.length != offset)
    return false;
    else while (offset > 0)
     if ((value = arrayA[--offset]) != null)
     {
      if (!value.equals(arrayB[offset]))
       return false;
     }
      else if (arrayB[offset] != null)
       return false;
  return true;
 }

/**
 * Tests two given array regions for non-equality and returns the
 * distance between the end of the regions and the first found
 * regions mismatch.
 **
 * The search for mismatches is performed in the forward direction
 * starting from the specified offsets. Negative <VAR>len</VAR> is
 * treated as zero. Important notes: if no mismatch is found then
 * zero is returned; the elements of the first array region are
 * tested for equality against the elements of the second one.
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
 * the distance (non-negative) between the first found regions
 * mismatch and the end of the regions.
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
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 * @see #reverse(java.lang.Object[])
 * @see #countNonNull(java.lang.Object[])
 * @see #hashCode(java.lang.Object[])
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #mismatches(java.lang.Object[], int, java.lang.Object[],
 * int, int)
 **
 * @since 2.0
 */
 public static final int mismatchRemain(Object[] arrayA,
         int offsetA, Object[] arrayB, int offsetB, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int count;
  count = arrayA.length - arrayB.length;
  if (len > 0)
  {
   Object value = arrayA[offsetA];
   value = arrayA[offsetA + len - 1];
   value = arrayB[offsetB];
   value = arrayB[offsetB + len - 1];
   if (offsetA != offsetB || arrayA != arrayB)
    do
    {
     if ((value = arrayA[offsetA++]) != null)
     {
      if (!value.equals(arrayB[offsetB]))
       break;
     }
      else if (arrayB[offsetB] != null)
       break;
     offsetB++;
    } while (--len > 0);
  }
   else len = 0;
  return len;
 }

/**
 * Count the mismatches of two given array regions.
 **
 * This method returns the count of elements of the first array
 * region which are not equal to the corresponding elements of the
 * second array region. Negative <VAR>len</VAR> is treated as zero.
 * Important notes: the elements of the first array region are
 * tested for equality against the elements of the second one.
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
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 * @see #reverse(java.lang.Object[])
 * @see #countNonNull(java.lang.Object[])
 * @see #hashCode(java.lang.Object[])
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #greater(java.lang.Object[], java.lang.Object[],
 * net.sf.ivmaidns.util.GComparator)
 **
 * @since 2.0
 */
 public static final int mismatches(Object[] arrayA, int offsetA,
         Object[] arrayB, int offsetB, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int count = arrayA.length - arrayB.length;
  count = 0;
  if (len > 0)
  {
   Object value = arrayA[offsetA];
   value = arrayA[offsetA + len - 1];
   value = arrayB[offsetB];
   value = arrayB[offsetB + len - 1];
   if (offsetA != offsetB || arrayA != arrayB)
    do
    {
     if ((value = arrayA[offsetA++]) != null)
     {
      if (!value.equals(arrayB[offsetB]))
       count++;
     }
      else if (arrayB[offsetB] != null)
       count++;
     offsetB++;
    } while (--len > 0);
  }
  return count;
 }

/**
 * Tests whether or not the first array is greater than the second
 * one according to the supplied comparator.
 **
 * <CODE>Object</CODE> arrays are compared here in the
 * element-by-element manner, starting at index <CODE>0</CODE>, and
 * at each index elements are tested for equality and the first
 * found non-equal elements pair is compared using
 * <VAR>comparator</VAR> (returning the result of this final
 * comparison). Important notes: if there is no non-equal elements
 * pairs then the first array is considered to be greater than the
 * second one only if its length is greater; the component type of
 * arrays is not compared anyway.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>) to be compared.
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>) to compare with.
 * @param comparator
 * the 'greater-than' comparator (must be non-<CODE>null</CODE>) to
 * use.
 * @return
 * <CODE>true</CODE> if <VAR>arrayA</VAR> is semantically greater
 * than <VAR>arrayB</VAR>, else <CODE>false</CODE>.
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>, or <VAR>comparator</VAR> is <CODE>null</CODE>.
 **
 * @see #array()
 * @see #length()
 * @see #greaterThan(java.lang.Object)
 * @see #indexOf(java.lang.Object, int, java.lang.Object[])
 * @see #lastIndexOf(java.lang.Object, int, java.lang.Object[])
 * @see #hashCode(java.lang.Object[])
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #sort(java.lang.Object[], int, int, net.sf.ivmaidns.util.GComparator)
 * @see #greater(net.sf.ivmaidns.util.Sortable[],
 * net.sf.ivmaidns.util.Sortable[])
 * @see #compare(java.lang.String[], int, int, java.lang.String[],
 * int, int)
 **
 * @since 2.0
 */
 public static final boolean greater(Object[] arrayA,
         Object[] arrayB, GComparator comparator)
  throws NullPointerException
 {
  int offset, len = arrayA.length;
  boolean isGreater = false;
  if ((offset = arrayB.length) < len)
  {
   isGreater = true;
   len = offset;
  }
  comparator.greater(comparator, comparator);
  if (arrayA != arrayB)
   for (offset = 0; offset < len; offset++)
   {
    Object value, temp = arrayB[offset];
    if ((value = arrayA[offset]) != null && !value.equals(temp) ||
        value == null && temp != null)
    {
     isGreater = comparator.greater(value, temp);
     break;
    }
   }
  return isGreater;
 }

/**
 * Tests whether or not the first <CODE>Sortable</CODE> array is
 * greater than the second one.
 **
 * <CODE>Sortable</CODE> arrays are compared here in the
 * element-by-element manner, starting at index <CODE>0</CODE>, and
 * at each index elements are tested for equality and the first
 * found non-equal elements pair is compared using the appropriate
 * <CODE>greaterThan(Object)</CODE> method (returning the result of
 * this final comparison). Important notes: if there is no non-equal
 * elements pairs then the first array is considered to be greater
 * than the second one only if its length is greater; the component
 * type of arrays is not compared anyway.
 **
 * @param arrayA
 * the first array (must be non-<CODE>null</CODE>) to be compared.
 * @param arrayB
 * the second array (must be non-<CODE>null</CODE>) to compare with.
 * @return
 * <CODE>true</CODE> if <VAR>arrayA</VAR> is semantically greater
 * than <VAR>arrayB</VAR>, else <CODE>false</CODE>.
 * @exception NullPointerException
 * if <VAR>arrayA</VAR> is <CODE>null</CODE> or <VAR>arrayB</VAR> is
 * <CODE>null</CODE>.
 **
 * @see #array()
 * @see #length()
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #greater(java.lang.Object[], java.lang.Object[],
 * net.sf.ivmaidns.util.GComparator)
 **
 * @since 2.0
 */
 public static final boolean greater(Sortable[] arrayA,
         Sortable[] arrayB)
  throws NullPointerException
 {
  boolean isGreater = false;
  int offset, len = arrayA.length;
  if (arrayA != arrayB)
  {
   if ((offset = arrayB.length) < len)
   {
    isGreater = true;
    len = offset;
   }
   for (offset = 0; offset < len; offset++)
   {
    Sortable value, temp = arrayB[offset];
    if ((value = arrayA[offset]) != null)
    {
     if (!value.equals(temp))
     {
      isGreater = value.greaterThan(temp);
      break;
     }
    }
     else if (temp != null)
     {
      isGreater = !temp.greaterThan(null);
      break;
     }
   }
  }
  return isGreater;
 }

/**
 * Compares two given <CODE>String</CODE> array regions.
 **
 * This method returns a signed integer indicating
 * 'less-equal-greater' case-sensitive relation between the
 * specified array regions of strings (the absolute value of the
 * result, in fact, is the distance between the first found mismatch
 * and the end of the bigger-length region). Negative
 * <VAR>lenA</VAR> is treated as zero. Negative <VAR>lenB</VAR> is
 * treated as zero. Important notes: the content of array regions is
 * compared before comparing their length; any <CODE>null</CODE>
 * element is considered to be greater than a non-<CODE>null</CODE>
 * one.
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
 * @see #length()
 * @see #equals(java.lang.Object[], java.lang.Object[])
 * @see #greater(net.sf.ivmaidns.util.Sortable[],
 * net.sf.ivmaidns.util.Sortable[])
 * @see #greater(java.lang.Object[], java.lang.Object[],
 * net.sf.ivmaidns.util.GComparator)
 **
 * @since 2.0
 */
 public static final int compare(String[] arrayA, int offsetA,
         int lenA, String[] arrayB, int offsetB, int lenB)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  String value;
  int cmp = arrayA.length - arrayB.length;
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
   for (cmp = 0; lenA > 0; lenA--)
   {
    String temp = arrayB[offsetB++];
    if ((value = arrayA[offsetA++]) != temp)
    {
     if (value == null)
      break;
     cmp = -1;
     if (temp == null || (cmp = value.compareTo(temp)) != 0)
      break;
    }
   }
   if (lenA > 0)
   {
    if (lenB <= 0)
     lenB = -lenB;
    lenB += lenA;
    if (cmp < 0)
     lenB = -lenB;
   }
  }
  return lenB;
 }

/**
 * Sorts the objects in the region of a given array according to the
 * supplied comparator.
 **
 * The elements in the region are sorted (using 'Merge' algorithm)
 * into the specified ascending order. Equal elements are not
 * reordered (the algorithm is 'stable'). A temporary buffer of
 * <CODE>((len + 1) / 2)</CODE> objects is allocated. The algorithm
 * cost is <CODE>O(log(len) * len)</CODE> in worst case, but may be
 * near <CODE>O(len)</CODE> for nearly sorted arrays. Negative
 * <VAR>len</VAR> is treated as zero. If an exception is thrown then
 * <VAR>array</VAR> remains unchanged. Else the region content is
 * altered.
 **
 * @param array
 * the array (must be non-<CODE>null</CODE>) to be sorted.
 * @param offset
 * the first index (must be in the range) of the region to sort.
 * @param len
 * the length of the region to sort.
 * @param comparator
 * the 'greater-than' comparator (must be non-<CODE>null</CODE>) to
 * use.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE> or <VAR>comparator</VAR>
 * is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>array</VAR> minus
 * <VAR>len</VAR>).
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #array()
 * @see #quickSort(java.lang.Object[], int, int,
 * net.sf.ivmaidns.util.GComparator)
 */
 public static final void sort(Object[] array, int offset, int len,
         GComparator comparator)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int target = array.length, last;
  comparator.greater(comparator, comparator);
  if (len > 0)
  {
   Object value = array[offset], temp;
   if (len > 1)
   {
    temp = array[(last = offset + len) - 1];
    Object[] buffer = new Object[(len + 1) >> 1];
    int start = offset, total = len, index = 0, degree = 0;
    for (len = ((len >> 1) + len) >> 3; (len >>= 1) > 0; degree++);
    do
    {
     for (target = (len = start) +
          (((total + index) >> degree) >> 1);
          ++start < target; array[offset] = value)
     {
      value = array[offset = start];
      do
      {
       if (!comparator.greater(temp = array[offset - 1], value))
        break;
       array[offset] = temp;
      } while (--offset > len);
     }
     if (start >= last)
      break;
     offset = 1 << degree;
     while (((index ^= offset) & offset) == 0 &&
            (offset >>= 1) > 0);
    } while (true);
    start -= total;
    boolean second = false, unordered = false;
    int half = total >> 1;
    do
    {
     boolean reversed = false;
     target = 0;
     int shift = degree;
     Object[] exchange;
     while (shift-- > 0)
     {
      last = half + start;
      int copy = index = 0;
      do
      {
       len = (half + index) >> shift;
       value = array[offset = (len >> 1) + start];
       start += len;
       if (comparator.greater(array[offset - 1], value))
       {
        temp = array[len = start - len];
        if (copy > 0)
        {
         System.arraycopy(array, len - copy,
          buffer, target - copy, copy);
         copy = 0;
        }
        int middle = offset;
        do
        {
         if (comparator.greater(temp, value))
         {
          buffer[target++] = value;
          if (++offset >= start)
           break;
          value = array[offset];
         }
          else
          {
           buffer[target++] = temp;
           if (++len >= middle)
            break;
           temp = array[len];
          }
        } while (true);
        if (offset < start)
         do
         {
          buffer[target++] = value;
          if (++offset >= start)
           break;
          value = array[offset];
         } while (true);
          else do
          {
           buffer[target++] = temp;
           if (++len >= middle)
            break;
           temp = array[len];
          } while (true);
       }
        else
        {
         copy += len;
         target += len;
        }
       if (start >= last)
        break;
       offset = 1 << (shift - 1);
       while (((index ^= offset) & offset) == 0 &&
              (offset >>= 1) > 0);
      } while (true);
      if (copy <= 0 || (len = (offset = half) - copy) > copy)
      {
       exchange = array;
       array = buffer;
       buffer = exchange;
       offset = start;
       start = target;
       target = offset;
       len = offset = copy;
       reversed = !reversed;
      }
      if (len > 0)
       System.arraycopy(buffer, target - offset,
        array, start - offset, len);
      start -= half;
      target -= half;
     }
     if ((second && (unordered = comparator.greater(reversed ?
         buffer[target - 1] : array[start - 1], array[start]))) !=
         reversed)
      System.arraycopy(array, start, buffer, target, half);
     if (reversed)
     {
      exchange = array;
      array = buffer;
      buffer = exchange;
      offset = start;
      start = target;
      target = offset;
     }
     start += half;
     if (second)
      break;
     half = total - half;
     second = true;
    } while (true);
    if (unordered)
    {
     start = (target = start) - total;
     value = array[offset = (total >> 1) + start - 1];
     temp = buffer[len = (total - 1) >> 1];
     do
     {
      target--;
      if (comparator.greater(value, temp))
      {
       array[target] = value;
       if (offset <= start)
        break;
       value = array[--offset];
      }
       else
       {
        array[target] = temp;
        if (--len < 0)
         break;
        temp = buffer[len];
       }
     } while (true);
     if (len >= 0)
      System.arraycopy(buffer, 0, array, offset, len + 1);
    }
   }
  }
 }

/**
 * Sorts the elements in the region of a given array using 'Quick'
 * algorithm according to the supplied comparator.
 **
 * The elements in the region are sorted into the specified
 * ascending order. But equal elements may be reordered (since the
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
 * @param comparator
 * the 'greater-than' comparator (must be non-<CODE>null</CODE>) to
 * use.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE> or <VAR>comparator</VAR>
 * is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>array</VAR> minus
 * <VAR>len</VAR>).
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #array()
 * @see #sort(java.lang.Object[], int, int, net.sf.ivmaidns.util.GComparator)
 * @see #binarySearch(java.lang.Object[], int, int,
 * java.lang.Object, net.sf.ivmaidns.util.GComparator)
 **
 * @since 2.0
 */
 public static final void quickSort(Object[] array,
         int offset, int len, GComparator comparator)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int level = array.length;
  comparator.greater(comparator, comparator);
  if (len > 0)
  {
   Object value = array[offset], temp;
   if (len > 1)
   {
    value = array[len += offset - 1];
    int[] bounds = new int[(JavaConsts.INT_SIZE - 2) << 1];
    level = 2;
    do
    {
     do
     {
      int index = offset, last;
      if ((last = len) - offset < 6)
      {
       len = offset;
       do
       {
        value = array[offset = ++index];
        do
        {
         if (!comparator.greater(temp = array[offset - 1], value))
          break;
         array[offset--] = temp;
         array[offset] = value;
        } while (offset > len);
       } while (index < last);
       break;
      }
      value = array[len = (offset + len) >>> 1];
      array[len] = array[offset];
      array[offset] = value;
      len = last;
      do
      {
       while (++offset < len &&
              comparator.greater(value, array[offset]));
       len++;
       while (--len >= offset &&
              comparator.greater(array[len], value));
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
 }

/**
 * Searches (fast) for value in a given sorted array.
 **
 * <VAR>array</VAR> (or its specified range) must be sorted
 * ascending (according to the supplied comparator), or the result
 * is undefined. The algorithm cost is of <CODE>O(log(len))</CODE>.
 * The elements are compared against <VAR>value</VAR>. Negative
 * <VAR>len</VAR> is treated as zero. If <VAR>value</VAR> is not
 * found then <CODE>(-result - 1)</CODE> is the offset of the
 * insertion point for <VAR>value</VAR>.
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
 * the value (may be <CODE>null</CODE>) to search for.
 * @param comparator
 * the 'greater-than' comparator (must be non-<CODE>null</CODE>) to
 * use.
 * @return
 * the index (non-negative) of the found value or
 * <CODE>(-insertionOffset - 1)</CODE> (a negative integer) if not
 * found.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE> or <VAR>comparator</VAR>
 * is <CODE>null</CODE>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>len</VAR> is positive and (<VAR>offset</VAR> is negative
 * or is greater than <CODE>length</CODE> of <VAR>array</VAR> minus
 * <VAR>len</VAR>).
 **
 * @see #array()
 * @see #fill(java.lang.Object[], int, int, java.lang.Object)
 * @see #indexOf(java.lang.Object, int, java.lang.Object[])
 * @see #lastIndexOf(java.lang.Object, int, java.lang.Object[])
 * @see #sort(java.lang.Object[], int, int, net.sf.ivmaidns.util.GComparator)
 * @see #quickSort(java.lang.Object[], int, int,
 * net.sf.ivmaidns.util.GComparator)
 */
 public static final int binarySearch(Object[] array,
         int offset, int len, Object value, GComparator comparator)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int middle = array.length;
  comparator.greater(comparator, comparator);
  if (len > 0)
  {
   Object temp = array[offset];
   temp = array[len += offset - 1];
   do
   {
    if (comparator.greater(temp =
        array[middle = (offset + len) >>> 1], value))
     len = middle - 1;
     else if (temp != null)
      if (temp.equals(value))
       break;
       else offset = middle + 1;
      else if (value != null)
       offset = middle + 1;
       else break;
   } while (offset <= len);
   if (offset <= len)
    offset = ~middle;
  }
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
 * @see ObjectVector#ObjectVector()
 * @see #array()
 * @see #toArray()
 * @see #getAt(int)
 * @see #equals(java.lang.Object)
 */
 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof ObjectVector && obj != this)
   {
    ObjectVector vector = (ObjectVector)obj;
    vector.array = (Object[])vector.array.clone();
    return obj;
   }
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * Computes and returns a hash code value for the object.
 **
 * This method mixes the hash codes of all the elements of
 * <CODE>this</CODE> vector to produce a single hash code value.
 **
 * @return
 * a hash code value for <CODE>this</CODE> object.
 **
 * @see #hashCode(java.lang.Object[])
 * @see #array()
 * @see #length()
 * @see #getAt(int)
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
 * <VAR>obj</VAR> is instance of this vector class and all the
 * elements of <CODE>this</CODE> vector are equal to the
 * corresponding elements of <VAR>obj</VAR> vector.
 **
 * @param obj
 * the object (may be <CODE>null</CODE>) with which to compare.
 * @return
 * <CODE>true</CODE> if and only if <CODE>this</CODE> value is the
 * same as <VAR>obj</VAR> value.
 **
 * @see ObjectVector#ObjectVector()
 * @see #array()
 * @see #length()
 * @see #getAt(int)
 * @see #hashCode()
 * @see #greaterThan(java.lang.Object)
 * @see #equals(java.lang.Object[], java.lang.Object[])
 */
 public boolean equals(Object obj)
 {
  return obj == this || obj instanceof ObjectVector &&
   equals(this.array, ((ObjectVector)obj).array);
 }

/**
 * Tests for being semantically greater than the argument.
 **
 * The result is <CODE>true</CODE> if and only if <VAR>obj</VAR> is
 * instance of <CODE>this</CODE> class and <CODE>this</CODE> object
 * is greater than the specified object. <CODE>Object</CODE> vectors
 * are compared in the element-by-element manner, starting at index
 * <CODE>0</CODE> and at each index elements are tested for equality
 * and the first non-equal elements pair is compared using
 * <CODE>INSTANCE</CODE> of <CODE>GComparator</CODE> class.
 **
 * @param obj
 * the second compared object (may be <CODE>null</CODE>).
 * @return
 * <CODE>true</CODE> if <VAR>obj</VAR> is comparable with
 * <CODE>this</CODE> and <CODE>this</CODE> object is greater than
 * <VAR>obj</VAR>, else <CODE>false</CODE>.
 **
 * @see #array()
 * @see #getAt(int)
 * @see #length()
 * @see #equals(java.lang.Object)
 * @see #greater(java.lang.Object[], java.lang.Object[],
 * net.sf.ivmaidns.util.GComparator)
 **
 * @since 2.0
 */
 public boolean greaterThan(Object obj)
 {
  return obj != this && obj instanceof ObjectVector &&
   greater(this.array, ((ObjectVector)obj).array,
   GComparator.INSTANCE);
 }

/**
 * Converts <CODE>this</CODE> vector to its string representation.
 **
 * The string representations of the objects (if a value is
 * <CODE>null</CODE> then "null") of the encapsulated
 * <VAR>array</VAR> are placed into the resulting string in the
 * direct index order, delimited by a single space.
 **
 * @return
 * the string representation (not <CODE>null</CODE>) of
 * <CODE>this</CODE> object.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #toString(java.lang.Object[], int, int, char)
 * @see #getAt(int)
 * @see #length()
 */
 public String toString()
 {
  Object[] array = this.array;
  return toString(array, 0, array.length, ' ');
 }

/**
 * Verifies <CODE>this</CODE> object for its integrity.
 **
 * The array component type and array elements are not checked.
 * For debug purpose only.
 **
 * @exception InternalError
 * if integrity violation is detected.
 **
 * @see ObjectVector#ObjectVector(java.lang.Object[])
 * @see #setArrayCloned(java.lang.Object[])
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
 * internally by <CODE>ObjectInputStream</CODE> class. The component
 * type of the wrapped array is set to <CODE>Object</CODE>.
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
 * @see ObjectVector#ObjectVector(java.lang.Object[])
 * @see #integrityCheck()
 */
 private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
 {
  in.defaultReadObject();
  Object[] array;
  if ((array = this.array) == null)
   throw new InvalidObjectException("array: null");
  int len = array.length;
  System.arraycopy(array, 0, this.array = new Object[len], 0, len);
 }
}

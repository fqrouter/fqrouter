/*
 * @(#) src/net/sf/ivmaidns/util/ConstVector.java --
 * Class for immutable vectors for objects.
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
 * Class for immutable vectors for objects.
 **
 * This <CODE>final</CODE> class is useful for representation of a
 * constant vector (container) of custom objects of any type. An
 * instance of this class encapsulates/wraps (when constructed) a
 * given <CODE>Object</CODE> array. Important notes: the component
 * type of the encapsulated <CODE>Object</CODE> array is hidden; an
 * object of this class may be serialized only if all custom objects
 * it contains are serializable.
 **
 * @see ConstPair
 * @see ObjectVector
 * @see GComparator
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 1.2
 */
public final class ConstVector
 implements Immutable, ReallyCloneable, Serializable, Indexable,
            Sortable, Verifiable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.8
 */
 private static final long serialVersionUID = 3843350134190606883L;

/**
 * The wrapped (encapsulated) custom array of objects.
 **
 * <VAR>array</VAR> must be non-<CODE>null</CODE> (but its elements
 * may be <CODE>null</CODE>). Important notes:
 * <VAR>array[index]</VAR> must not be changed anyhow for every
 * <VAR>index</VAR>; the component type of <VAR>array</VAR> is of no
 * use here.
 **
 * @serial
 **
 * @see ConstVector#ConstVector(java.lang.Object[])
 * @see #length()
 * @see #getAt(int)
 * @see #toArray()
 * @see #integrityCheck()
 */
 protected final Object[] array;

/**
 * Constructs an immutable vector by wrapping a given custom array
 * of objects.
 **
 * Important notes: <VAR>array[index]</VAR> must not be changed
 * anyhow for each index (since no cloning is performed for
 * <VAR>array</VAR> here).
 **
 * @param array
 * the object array (must be non-<CODE>null</CODE>) to be
 * encapsulated.
 * @exception NullPointerException
 * if <VAR>array</VAR> is <CODE>null</CODE>.
 **
 * @see #getAt(int)
 * @see #toArray()
 * @see #equals(java.lang.Object)
 * @see #greaterThan(java.lang.Object)
 * @see #toString()
 */
 public ConstVector(Object[] array)
  throws NullPointerException
 {
  int len;
  len = array.length;
  this.array = array;
 }

/**
 * Returns the number of elements in <CODE>this</CODE> vector.
 **
 * The result is the same as <CODE>length</CODE> of
 * <CODE>toArray()</CODE>.
 **
 * @return
 * amount (non-negative value) of elements.
 **
 * @see #getAt(int)
 * @see #toArray()
 */
 public int length()
 {
  return this.array.length;
 }

/**
 * Returns value of the element at the specified index.
 **
 * The result is the same as of <CODE>toArray()[index]</CODE>.
 **
 * @param index
 * the index (must be in the range) at which to return an element.
 * @return
 * an element (may be <CODE>null</CODE>) at <VAR>index</VAR>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 **
 * @see ConstVector#ConstVector(java.lang.Object[])
 * @see #length()
 * @see #toArray()
 */
 public Object getAt(int index)
  throws ArrayIndexOutOfBoundsException
 {
  return this.array[index];
 }

/**
 * Returns a newly created array filled with the values of the
 * elements of <CODE>this</CODE> vector.
 **
 * Here, the result is the exact instance of <CODE>Object[]</CODE>.
 **
 * @return
 * a new array (not <CODE>null</CODE>), containing values of all the
 * elements of <CODE>this</CODE> vector.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ConstVector#ConstVector(java.lang.Object[])
 * @see #getAt(int)
 * @see #length()
 * @see #equals(java.lang.Object)
 * @see #greaterThan(java.lang.Object)
 * @see #toString()
 */
 public Object[] toArray()
 {
  Object[] array, newArray;
  int len = (array = this.array).length;
  System.arraycopy(array, 0, newArray = new Object[len], 0, len);
  return newArray;
 }

/**
 * Creates and returns a copy of <CODE>this</CODE> object.
 **
 * Important notes: the encapsulated array is not cloned itself.
 **
 * @return
 * a copy (not <CODE>null</CODE> and != <CODE>this</CODE>) of
 * <CODE>this</CODE> instance.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ConstVector#ConstVector(java.lang.Object[])
 * @see #toArray()
 * @see #equals(java.lang.Object)
 */
 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof ConstVector && obj != this)
    return obj;
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * Computes and returns a hash code value for the object.
 **
 * This method hashes all non-<CODE>null</CODE> elements of
 * <CODE>this</CODE> vector and mixes them all to produce a single
 * hash code value.
 **
 * @return
 * a hash code value for <CODE>this</CODE> object.
 **
 * @see #length()
 * @see #getAt(int)
 * @see #equals(java.lang.Object)
 */
 public int hashCode()
 {
  Object[] array = this.array;
  int code = 0, offset = 0;
  Object value;
  for (int len = array.length; offset < len;
       code = (code << 5) - code)
   if ((value = array[offset++]) != null)
    code ^= value.hashCode();
  return code ^ offset;
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
 * @see #length()
 * @see #getAt(int)
 * @see #hashCode()
 * @see #greaterThan(java.lang.Object)
 */
 public boolean equals(Object obj)
 {
  if (obj != this)
  {
   if (!(obj instanceof ConstVector))
    return false;
   int offset;
   Object[] otherArray = ((ConstVector)obj).array, array;
   if ((array = this.array) != otherArray)
   {
    if ((offset = array.length) != otherArray.length)
     return false;
    Object value;
    while (offset-- > 0)
     if ((value = array[offset]) != null)
     {
      if (!value.equals(otherArray[offset]))
       return false;
     }
      else if (otherArray[offset] != null)
       return false;
   }
  }
  return true;
 }

/**
 * Tests for being semantically greater than the argument.
 **
 * The result is <CODE>true</CODE> if and only if <VAR>obj</VAR> is
 * instance of <CODE>this</CODE> class and <CODE>this</CODE> object
 * is greater than the specified object. Vectors are compared in the
 * element-by-element manner, starting at index <CODE>0</CODE> and
 * at each index elements are compared for equality and the first
 * non-equal elements pair is compared through <CODE>INSTANCE</CODE>
 * of <CODE>GComparator</CODE> class.
 **
 * @param obj
 * the second compared object (may be <CODE>null</CODE>).
 * @return
 * <CODE>true</CODE> if <VAR>obj</VAR> is comparable with
 * <CODE>this</CODE> and <CODE>this</CODE> object is greater than
 * <VAR>obj</VAR>, else <CODE>false</CODE>.
 **
 * @see #length()
 * @see #getAt(int)
 * @see #equals(java.lang.Object)
 **
 * @since 1.8
 */
 public boolean greaterThan(Object obj)
 {
  int offset;
  Object[] array = this.array, otherArray;
  boolean isGreater = false;
  if (obj != this && obj instanceof ConstVector &&
      (otherArray = ((ConstVector)obj).array) != array)
  {
   int len = array.length;
   if ((offset = otherArray.length) < len)
   {
    isGreater = true;
    len = offset;
   }
   for (offset = 0; offset < len; offset++)
   {
    Object value, temp = otherArray[offset];
    if ((value = array[offset]) != null && !value.equals(temp) ||
        value == null && temp != null)
    {
     isGreater = GComparator.INSTANCE.greater(value, temp);
     break;
    }
   }
  }
  return isGreater;
 }

/**
 * Converts vector to its 'in-line' string representation.
 **
 * The string representations of the values (if value is
 * <CODE>null</CODE> then "null") of <CODE>this</CODE> vector are
 * placed into the resulting string in the direct index order,
 * delimited by a single space.
 **
 * @return
 * the string representation (not <CODE>null</CODE>) of
 * <CODE>this</CODE> object.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ConstVector#ConstVector(java.lang.Object[])
 * @see #toArray()
 */
 public String toString()
 {
  Object[] array = this.array;
  int offset = 0, len = array.length;
  StringBuffer sBuf =
   new StringBuffer((len << 2) > 24 ? len << 2 : 24);
  if (len > 0)
   do
   {
    Object value;
    sBuf.append((value = array[offset]) != null ?
     value.toString() : "null");
    if (++offset >= len)
     break;
    sBuf.append(' ');
   } while (true);
  return new String(sBuf);
 }

/**
 * Verifies <CODE>this</CODE> object for its integrity.
 **
 * The elements are not checked. For debug purpose only.
 **
 * @exception InternalError
 * if integrity violation is detected.
 **
 * @see ConstVector#ConstVector(java.lang.Object[])
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
 * @see ConstVector#ConstVector(java.lang.Object[])
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

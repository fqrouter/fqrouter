/*
 * @(#) src/net/sf/ivmaidns/util/ConstPair.java --
 * Class for immutable pairs of objects.
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
 * Class for immutable pairs of objects.
 **
 * This instantable class is useful for representation of a constant
 * sortable pair of custom objects of any type (a container for two
 * elements).
 **
 * @see ConstVector
 * @see GComparator
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 1.2
 */
public final class ConstPair
 implements Immutable, ReallyCloneable, Serializable, Indexable,
            Sortable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.8
 */
 private static final long serialVersionUID = 3724364892682440569L;

/**
 * The first custom object of <CODE>this</CODE> pair.
 **
 * @serial
 **
 * @see ConstPair#ConstPair(java.lang.Object, java.lang.Object)
 * @see #getAt(int)
 */
 protected final Object valueA;

/**
 * The second custom object of <CODE>this</CODE> pair.
 **
 * @serial
 **
 * @see ConstPair#ConstPair(java.lang.Object, java.lang.Object)
 * @see #getAt(int)
 */
 protected final Object valueB;

/**
 * Constructs an immutable 'pair' container.
 **
 * @param valueA
 * the first custom object (may be <CODE>null</CODE>).
 * @param valueB
 * the second custom object (may be <CODE>null</CODE>).
 **
 * @see #getAt(int)
 * @see #equals(java.lang.Object)
 * @see #greaterThan(java.lang.Object)
 * @see #toString()
 */
 public ConstPair(Object valueA, Object valueB)
 {
  this.valueA = valueA;
  this.valueB = valueB;
 }

/**
 * Returns the number of elements in <CODE>this</CODE> container.
 **
 * Here, the result is 2.
 **
 * @return
 * amount (non-negative value) of elements.
 **
 * @see #getAt(int)
 */
 public int length()
 {
  return 2;
 }

/**
 * Returns value of the element at the specified index.
 **
 * @param index
 * the index (must be in the range) at which to return an element.
 * @return
 * an element (may be <CODE>null</CODE>) at <VAR>index</VAR>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 **
 * @see ConstPair#ConstPair(java.lang.Object, java.lang.Object)
 * @see #length()
 */
 public Object getAt(int index)
  throws ArrayIndexOutOfBoundsException
 {
  if (index >> 1 != 0)
   throw new ArrayIndexOutOfBoundsException(index);
  Object value = this.valueA;
  if (index > 0)
   value = this.valueB;
  return value;
 }

/**
 * Creates and returns a copy of <CODE>this</CODE> object.
 **
 * The result is the same as of
 * <CODE>new ConstPair(getAt(0), getAt(1))</CODE>.
 **
 * @return
 * a copy (not <CODE>null</CODE> and != <CODE>this</CODE>) of
 * <CODE>this</CODE> instance.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ConstPair#ConstPair(java.lang.Object, java.lang.Object)
 * @see #getAt(int)
 * @see #equals(java.lang.Object)
 */
 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof ConstPair && obj != this)
    return obj;
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * Computes and returns a hash code value for the object.
 **
 * This method hashes all non-<CODE>null</CODE> elements of
 * <CODE>this</CODE> container and mixes them all to produce a
 * single hash code value.
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
  int code = 0;
  Object value;
  if ((value = this.valueA) != null)
  {
   code = value.hashCode();
   code = (code << 5) - code;
  }
  if ((value = this.valueB) != null)
   code ^= value.hashCode();
  return ((code << 5) - code) ^ 2;
 }

/**
 * Indicates whether <CODE>this</CODE> object is equal to the
 * specified one.
 **
 * This method returns <CODE>true</CODE> if and only if
 * <VAR>obj</VAR> is instance of this container class and all
 * elements of <CODE>this</CODE> container are equal to the
 * corresponding elements of <VAR>obj</VAR>.
 **
 * @param obj
 * the object (may be <CODE>null</CODE>) with which to compare.
 * @return
 * <CODE>true</CODE> if and only if <CODE>this</CODE> value is the
 * same as <VAR>obj</VAR> value.
 **
 * @see ConstPair#ConstPair(java.lang.Object, java.lang.Object)
 * @see #length()
 * @see #getAt(int)
 * @see #hashCode()
 * @see #greaterThan(java.lang.Object)
 */
 public boolean equals(Object obj)
 {
  boolean isEqual = true;
  if (obj != this)
  {
   isEqual = false;
   if (obj instanceof ConstPair)
   {
    Object value;
    ConstPair pair = (ConstPair)obj;
    if ((value = this.valueA) != null &&
        value.equals(pair.valueA) ||
        value == null && pair.valueA == null)
     if ((value = this.valueB) != null)
      isEqual = value.equals(pair.valueB);
      else if (pair.valueB == null)
       isEqual = true;
   }
  }
  return isEqual;
 }

/**
 * Tests for being semantically greater than the argument.
 **
 * The result is <CODE>true</CODE> if and only if <VAR>obj</VAR> is
 * instance of <CODE>this</CODE> class and <CODE>this</CODE> object
 * is greater than the specified object. Containers are compared in
 * the element-by-element manner, starting at index <CODE>0</CODE>.
 * So, the first elements pair is tested for equality and then it
 * (if equality test has failed) or (else) the second elements pair
 * is compared through <CODE>INSTANCE</CODE> of
 * <CODE>GComparator</CODE> class (and the result of this comparison
 * is returned).
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
  if (obj == this || !(obj instanceof ConstPair))
   return false;
  ConstPair pair = (ConstPair)obj;
  Object value, pairValue = pair.valueA;
  if ((value = this.valueA) != null && value.equals(pairValue) ||
      value == null && pairValue == null)
  {
   value = this.valueB;
   pairValue = pair.valueB;
  }
  return GComparator.INSTANCE.greater(value, pairValue);
 }

/**
 * Converts container to its 'in-line' string representation.
 **
 * Here, these two values are placed into the resulting string in
 * the direct index order, delimited by a single space.
 **
 * @return
 * the string representation (not <CODE>null</CODE>, with non-zero
 * <CODE>length()</CODE>) of <CODE>this</CODE> object.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ConstPair#ConstPair(java.lang.Object, java.lang.Object)
 * @see #length()
 * @see #getAt(int)
 */
 public String toString()
 {
  Object value;
  return new String((new StringBuffer(24)).
   append((value = this.valueA) != null ?
   value.toString() : "null").append(' ').
   append((value = this.valueB) != null ?
   value.toString() : "null"));
 }
}

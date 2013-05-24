/*
 * @(#) src/net/sf/ivmaidns/util/GComparator.java --
 * Class for 'greater-than' comparators.
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
 * Class for 'greater-than' comparators.
 **
 * A binary predicate, which tests whether the first its argument is
 * greater than the second one, should extend this adapter class and
 * override <CODE>greater(Object, Object)</CODE>, implementing own
 * comparison rules. <CODE>Immutable</CODE> interface is implemented
 * to underline that such comparators must have constant internal
 * state. The comparators may be used in serializable data
 * structures since <CODE>Serializable</CODE> interface is
 * implemented. In addition, this class provides the default
 * comparator for objects of <CODE>Sortable</CODE> interface, and of
 * <CODE>String</CODE> class and of the standard wrappers for the
 * primitive types.
 **
 * @see Sortable
 * @see StrComparator
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 1.8
 */
public class GComparator
 implements Immutable, Serializable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.8
 */
 private static final long serialVersionUID = 1310911848083016948L;

/**
 * The standard comparator for objects.
 **
 * This constant field is initialized with the instantiation of
 * exactly this comparator. The implemented standard comparator
 * correctly orders any two objects of the class which implements
 * <CODE>Sortable</CODE> interface, or both of <CODE>String</CODE>
 * class, or both of the same primary-type value wrapper.
 **
 * @see #greater(java.lang.Object, java.lang.Object)
 */
 public static final GComparator INSTANCE = new GComparator();

/**
 * Constructs a new comparator.
 **
 * This constructor is made <CODE>public</CODE> to allow custom
 * dynamic instantiation of this class.
 **
 * @see #INSTANCE
 */
 public GComparator() {}

/**
 * The body of 'Greater-Than' comparator.
 **
 * Tests whether or not the first specified object is greater than
 * the second one (according to the semantics). This method should
 * be overridden in the adapter subclasses. If any argument is not
 * instance of the expected type then standard comparison should be
 * performed (which is implemented in this method of this class).
 * The standard comparison of two objects is as follows: if
 * <CODE>objA == objB</CODE> then <CODE>false</CODE> is returned
 * else if <VAR>objA</VAR> is instance of <CODE>Sortable</CODE> then
 * <CODE>greaterThan(objB)</CODE> for <VAR>objA</VAR> is returned
 * else if <VAR>objB</VAR> is instance of <CODE>Sortable</CODE> then
 * <CODE>!greaterThan(objA)</CODE> for <VAR>objB</VAR> is returned,
 * else if <VAR>objA</VAR> is <CODE>null</CODE> then
 * <CODE>true</CODE> is returned, else if <VAR>objA</VAR> and
 * <VAR>objB</VAR> are both of the same class (either
 * <CODE>Boolean</CODE>, <CODE>Byte</CODE>, <CODE>Short</CODE>,
 * <CODE>Integer</CODE>, <CODE>Long</CODE>, <CODE>Float</CODE>,
 * <CODE>Double</CODE>, <CODE>Character</CODE> or
 * <CODE>String</CODE>) and the first (primary) value is greater
 * than the second one then <CODE>true</CODE> else
 * <CODE>false</CODE> is returned. Important notes:
 * <CODE>String</CODE> objects are compared here in the
 * case-sensitive manner; <CODE>Float</CODE> and <CODE>Double</CODE>
 * objects are compared here handling their zero and
 * <CODE>NaN</CODE> values specially (<CODE>0</CODE> is greater than
 * <CODE>-0</CODE> and <CODE>NaN</CODE> is greater than
 * non-<CODE>NaN</CODE>).
 **
 * @param objA
 * the first compared argument (may be <CODE>null</CODE>).
 * @param objB
 * the second compared argument (may be <CODE>null</CODE>).
 * @return
 * <CODE>true</CODE> if and only if <VAR>objA</VAR> is greater than
 * <VAR>objB</VAR>.
 **
 * @see #INSTANCE
 */
 public boolean greater(Object objA, Object objB)
 {
  if (objA != objB)
  {
   if (objA instanceof Sortable)
    return ((Sortable)objA).greaterThan(objB);
   if (objB instanceof Sortable)
    return !((Sortable)objB).greaterThan(objA);
   if (objA == null)
    return true;
   if (objB != null)
   {
    if (objA instanceof String)
     return objB instanceof String &&
      ((String)objA).compareTo((String)objB) > 0;
    if (objA instanceof Integer)
     return objB instanceof Integer &&
      ((Integer)objA).intValue() > ((Integer)objB).intValue();
    if (objA instanceof Long)
     return objB instanceof Long &&
      ((Long)objA).longValue() > ((Long)objB).longValue();
    if (objA instanceof Character)
     return objB instanceof Character &&
      ((Character)objA).charValue() > ((Character)objB).charValue();
    if (objA instanceof Short)
     return objB instanceof Short &&
      ((Short)objA).intValue() > ((Short)objB).intValue();
    if (objA instanceof Byte)
     return objB instanceof Byte &&
      ((Byte)objA).intValue() > ((Byte)objB).intValue();
    if (objA instanceof Boolean)
     return objB instanceof Boolean &&
      ((Boolean)objA).booleanValue() &&
      !((Boolean)objB).booleanValue();
    if (objA instanceof Float && objB instanceof Float)
    {
     float floatB = ((Float)objB).floatValue(), floatA;
     return (floatA = ((Float)objA).floatValue()) > floatB ||
      !(floatA < floatB) && Float.floatToIntBits(floatA) >
      Float.floatToIntBits(floatB);
    }
    if (objA instanceof Double && objB instanceof Double)
    {
     double doubleB = ((Double)objB).doubleValue(), doubleA;
     return (doubleA = ((Double)objA).doubleValue()) > doubleB ||
      !(doubleA < doubleB) && Double.doubleToLongBits(doubleA) >
      Double.doubleToLongBits(doubleB);
    }
   }
  }
  return false;
 }
}

/*
 * @(#) src/net/sf/ivmaidns/util/StrComparator.java --
 * Class for string comparators/metrics.
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
 * Class for string comparators/metrics.
 **
 * This comparator class overrides the default semantics of
 * <CODE>GComparator</CODE> class for the case when both of the
 * objects being compared are instances of <CODE>String</CODE>
 * class. This class also implements <CODE>Metricable</CODE>
 * interface to offer some assessment metrics for string objects.
 * Here, case-insensitive comparison of strings is implemented
 * (<CODE>GComparator</CODE> class implementation offers
 * case-sensitive comparison for string objects), and the metrics is
 * string length plus one. In the subclasses
 * <CODE>compare(String, String)</CODE> method should be overridden
 * to implement specific comparison and metrics.
 **
 * @see CharVector
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 1.8
 */
public class StrComparator extends GComparator
 implements Metricable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.8
 */
 private static final long serialVersionUID = 2114110405911422799L;

/**
 * An instance of this comparator.
 **
 * This constant field is initialized with the instantiation of
 * exactly this comparator (hides <CODE>INSTANCE</CODE> of the
 * superclass). The implemented comparator orders correctly strings
 * ignoring letters case, other objects are ordered in the same way
 * as by <CODE>GComparator</CODE> exact instance;
 * <CODE>evaluate(Object)</CODE> method for a string here returns
 * its <CODE>length()</CODE> plus one, for other objects zero is
 * returned.
 **
 * @see #greater(java.lang.Object, java.lang.Object)
 * @see #evaluate(java.lang.Object)
 */
 public static final GComparator INSTANCE = new StrComparator();

/**
 * Constructs a new comparator.
 **
 * This constructor is made <CODE>public</CODE> only to allow custom
 * dynamic instantiation of this class. In other cases,
 * <CODE>INSTANCE</CODE> should be used.
 **
 * @see #INSTANCE
 */
 public StrComparator() {}

/**
 * The body of 'Greater-Than' comparator.
 **
 * Tests whether or not the first specified object is greater than
 * the second one. If both arguments are of <CODE>String</CODE>
 * class then <CODE>(compare(objA, objB) > 0)</CODE> is returned,
 * else <CODE>greater(objA, objB)</CODE> of the superclass is
 * returned.
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
 * @see #compare(java.lang.String, java.lang.String)
 */
 public final boolean greater(Object objA, Object objB)
 {
  boolean isGreater;
  if (objA instanceof String && objB instanceof String)
  {
   isGreater = false;
   if (compare((String)objA, (String)objB) > 0)
    isGreater = true;
  }
   else isGreater = super.greater(objA, objB);
  return isGreater;
 }

/**
 * The body of the metrics.
 **
 * Evaluates the supplied object. If <VAR>obj</VAR> is of
 * <CODE>String</CODE> class then <CODE>compare(null, obj)</CODE> is
 * returned, else this method always returns <CODE>0</CODE>.
 **
 * @param obj
 * the object (may be <CODE>null</CODE>) to evaluate.
 * @return
 * the integer result of the performed evaluation.
 **
 * @see #INSTANCE
 * @see #compare(java.lang.String, java.lang.String)
 */
 public final int evaluate(Object obj)
 {
  int value = 0;
  if (obj instanceof String)
   value = compare(null, (String)obj);
  return value;
 }

/**
 * Compares two given strings.
 **
 * This method returns a signed integer indicating
 * 'less-equal-greater' relation between the specified strings (the
 * absolute value of the result, in fact, is the distance between
 * the first found mismatch and the end of the bigger-length
 * string). This method should be overridden in subclasses to
 * implement specific string comparison rules (if not, this method
 * compares strings in the case-insensitive manner and the metrics
 * (when <CODE>strA == null</CODE>) is <CODE>length()</CODE> of
 * <VAR>strB</VAR> plus one). Important notes: zero is returned if
 * <CODE>strA == strB</CODE>; this function is always asymmetrical.
 **
 * @param strA
 * the first compared string (may be <CODE>null</CODE>).
 * @param strB
 * the second compared string (may be <CODE>null</CODE>).
 * @return
 * a negative integer, zero, or a positive integer as
 * <VAR>strA</VAR> object is less than, equal to, or greater than
 * <VAR>strB</VAR> one.
 **
 * @see #INSTANCE
 * @see #greater(java.lang.Object, java.lang.Object)
 * @see #evaluate(java.lang.Object)
 */
 public int compare(String strA, String strB)
 {
  int lenA, lenB = 0;
  if (strA != strB)
  {
   lenA = 0;
   if (strA != null)
    lenA = strA.length() + 1;
   if (strB != null)
    lenB = strB.length() + 1;
   if ((lenB = lenA - lenB) >= 0)
    lenA -= lenB;
   if (lenA <= 0)
    lenB = -lenB;
   for (int offset = 0; --lenA > 0; offset++)
   {
    char value, temp = strB.charAt(offset);
    if ((value = strA.charAt(offset)) != temp)
    {
     temp = Character.toUpperCase(temp);
     if ((value = Character.toUpperCase(value)) != temp)
     {
      temp = Character.toLowerCase(temp);
      if ((value = Character.toLowerCase(value)) != temp)
      {
       if (lenB <= 0)
        lenB = -lenB;
       lenB += lenA;
       if (value >= temp)
        break;
       lenB = -lenB;
       break;
      }
     }
    }
   }
  }
  return lenB;
 }
}

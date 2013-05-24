/*
 * @(#) src/net/sf/ivmaidns/dns/DNSRecAltCmp.java --
 * Class for DNS record comparator/metrics.
 **
 * Copyright (c) 1999-2001 Ivan Maidanski <ivmai@mail.ru>
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

package net.sf.ivmaidns.dns;

import net.sf.ivmaidns.util.GComparator;
import net.sf.ivmaidns.util.Metricable;

/**
 * Class for DNS record comparator/metrics (alternative).
 **
 * This <CODE>final</CODE> comparator class overrides the default
 * semantics of <CODE>GComparator</CODE> class for the case when
 * both of the objects being compared are instances of
 * <CODE>DNSRecord</CODE> class. This class also implements
 * <CODE>Metricable</CODE> interface to offer records metrics (which
 * is the level of a resource record name plus one). Instead of
 * canonical ordering for records (which is defined in RFC2535 and
 * may be performed through <CODE>GComparator</CODE> instance), this
 * comparator offers an alternative records ordering (more
 * convenient for the end users). The alternative ordering differs
 * from the canonical one in just one thing: records rData is
 * compared not as a sequence of bytes but as a sequence of its
 * decoded fields (each field is compared according to its type).
 **
 * @see DNSRecord
 **
 * @version 3.0
 * @author Ivan Maidanski
 **
 * @since 2.8
 */
public final class DNSRecAltCmp extends GComparator
 implements Metricable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 2.8
 */
 private static final long serialVersionUID = 4428663077680643378L;

/**
 * An instance of this comparator.
 **
 * This constant field is initialized with the instantiation of this
 * comparator (hides <CODE>INSTANCE</CODE> of the superclass).
 **
 * @see #greater(java.lang.Object, java.lang.Object)
 */
 public static final GComparator INSTANCE = new DNSRecAltCmp();

/**
 * Constructs a new comparator.
 **
 * This constructor is made <CODE>public</CODE> only to allow custom
 * dynamic instantiation of this class. In other cases,
 * <CODE>INSTANCE</CODE> should be used.
 **
 * @see #INSTANCE
 */
 public DNSRecAltCmp() {}

/**
 * The body of 'Greater-Than' comparator.
 **
 * Tests whether or not the first specified object is greater than
 * the second one. If both arguments are of <CODE>DNSRecord</CODE>
 * class then <CODE>(objA compareTo(objB, true) > 0)</CODE> is
 * returned, else <CODE>greater(objA, objB)</CODE> of the superclass
 * is returned.
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
 * @see #evaluate(java.lang.Object)
 */
 public boolean greater(Object objA, Object objB)
 {
  boolean isGreater;
  if (objA instanceof DNSRecord && objB instanceof DNSRecord)
  {
   isGreater = false;
   if (((DNSRecord)objA).compareTo((DNSRecord)objB, true) > 0)
    isGreater = true;
  }
   else isGreater = super.greater(objA, objB);
  return isGreater;
 }

/**
 * The body of the metrics.
 **
 * Evaluates the supplied object. If <VAR>obj</VAR> is of
 * <CODE>DNSRecord</CODE> class then
 * <CODE>(obj getLevel() + 1)</CODE> is returned, else this method
 * always returns <CODE>0</CODE>.
 **
 * @param obj
 * the object (may be <CODE>null</CODE>) to evaluate.
 * @return
 * the integer result of the performed evaluation.
 **
 * @see #INSTANCE
 * @see #greater(java.lang.Object, java.lang.Object)
 **
 * @since 3.0
 */
 public int evaluate(Object obj)
 {
  int value = 0;
  if (obj instanceof DNSRecord)
   value = ((DNSRecord)obj).getLevel() + 1;
  return value;
 }
}

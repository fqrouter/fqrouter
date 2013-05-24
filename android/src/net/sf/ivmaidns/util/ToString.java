/*
 * @(#) src/net/sf/ivmaidns/util/ToString.java --
 * Class for 'object-to-string' converters.
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
 * Class for 'object-to-string' converters.
 **
 * All 'object-to-string' converters should extend this adapter
 * class and override <CODE>toString(Object)</CODE>, implementing
 * their own conversion rules. The converters may be used in
 * serializable data structures since <CODE>Serializable</CODE>
 * interface is implemented.
 **
 * @see GComparator
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public class ToString
 implements Serializable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.1
 */
 private static final long serialVersionUID = 6412171811255872037L;

/**
 * The default 'to-string' converter providing the standard Java
 * <CODE>toString()</CODE> functionality.
 **
 * This constant field is initialized with the instantiation of
 * exactly this converter.
 **
 * @see #toString(java.lang.Object)
 **
 * @since 2.0
 */
 public static final ToString INSTANCE = new ToString();

/**
 * Constructs a new 'to-string' converter.
 **
 * This constructor is made <CODE>public</CODE> to allow custom
 * dynamic instantiation of this class.
 **
 * @see #INSTANCE
 **
 * @since 1.1
 */
 public ToString() {}

/**
 * The body of 'Object-to-String' converter.
 **
 * Returns its operand as a string. If the argument is not instance
 * of the expected type then standard Java <CODE>toString()</CODE>
 * method for the specified argument is executed (or "null" is
 * returned if argument is <CODE>null</CODE>). This method should be
 * overridden in adapter subclasses to implement specific
 * 'to-string' conversion (instead of the standard conversion).
 **
 * @param obj
 * the object to be converted (may be <CODE>null</CODE>).
 * @return
 * the string representation (not <CODE>null</CODE>) of the
 * specified object.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #INSTANCE
 */
 public String toString(Object obj)
 {
  String str = "null";
  if (obj != null)
   str = obj.toString();
  return str;
 }
}

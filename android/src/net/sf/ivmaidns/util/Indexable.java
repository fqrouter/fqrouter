/*
 * @(#) src/net/sf/ivmaidns/util/Indexable.java --
 * Read-only interface for indexed containers.
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
 * Read-only interface for indexed containers.
 **
 * This interface should be implemented by a class that contains a
 * group of (typed) elements and allows at least read-only access to
 * these elements by an integer index (from <CODE>0</CODE> to
 * <CODE>length() - 1</CODE>, inclusive).
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 2.0
 */
public interface Indexable
{

/**
 * Returns the number of elements in <CODE>this</CODE> container.
 **
 * This method should not be <CODE>final</CODE>.
 **
 * @return
 * amount (non-negative value) of elements.
 **
 * @see #getAt(int)
 */
 public abstract int length();

/**
 * Returns value of the element at the specified index.
 **
 * If the value to return is primitive then it is being wrapped.
 * This method should not be <CODE>final</CODE>.
 **
 * @param index
 * the index (must be in the range) at which to return an element.
 * @return
 * an element (may be <CODE>null</CODE>) at <VAR>index</VAR>.
 * @exception ArrayIndexOutOfBoundsException
 * if <VAR>index</VAR> is negative or is not less than
 * <CODE>length()</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #length()
 */
 public abstract Object getAt(int index)
  throws ArrayIndexOutOfBoundsException;
}

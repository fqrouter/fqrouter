/*
 * @(#) src/net/sf/ivmaidns/util/Sortable.java --
 * Interface for comparable/orderable objects.
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
 * Interface for comparable/orderable objects.
 **
 * Any class implementing this interface allows semantic ordering
 * (sorting) of its instances using its built-in 'greater-than'
 * comparator method.
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public interface Sortable
{

/**
 * Tests for being semantically greater than the argument.
 **
 * The result is true if and only if <VAR>obj</VAR> is of expected
 * type and <CODE>this</CODE> object is greater than the specified
 * one. Important notes: in a particular <CODE>final</CODE> class
 * <CODE>int compareTo(Type val) throws NullPointerException</CODE>
 * method may be provided for user convenience.
 **
 * @param obj
 * the second compared object (may be <CODE>null</CODE>).
 * @return
 * <CODE>true</CODE> if <VAR>obj</VAR> is comparable with
 * <CODE>this</CODE> and <CODE>this</CODE> object is greater than
 * <VAR>obj</VAR>, else <CODE>false</CODE>.
 **
 * @since 2.0
 */
 public abstract boolean greaterThan(Object obj);
}

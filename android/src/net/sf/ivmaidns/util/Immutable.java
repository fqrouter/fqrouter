/*
 * @(#) src/net/sf/ivmaidns/util/Immutable.java --
 * Tagging interface for immutable objects.
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
 * Tagging interface for immutable objects.
 **
 * This interface should be implemented by a custom class which
 * instances have constant (immutable) internal state. This
 * interface has no methods or fields and serves only to identify
 * the semantics of being immutable. Here are some important notes
 * for developers of the classes for immutable objects: such classes
 * should be declared as <CODE>final</CODE> and should not extend
 * any other classes; they must not have any methods for changing
 * state of <CODE>this</CODE> object; all their variable fields must
 * never be changed even indirectly (all the fields should be
 * <CODE>final</CODE>); methods synchronization is not needed.
 **
 * @see ReallyCloneable
 * @see ConstVector
 * @see UnsignedInt
 * @see GComparator
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 2.0
 */
public interface Immutable
{
}

/*
 * @(#) src/net/sf/ivmaidns/util/ReallyCloneable.java --
 * Interface for cloneable objects.
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
 * Interface for cloneable objects.
 **
 * This interface declares/unhides the <CODE>public</CODE> method
 * for making an instance "exact" copy (unlike
 * <CODE>Cloneable</CODE> interface which is only tagging). The
 * standard <CODE>Cloneable</CODE> interface is extended here to
 * allow the proper usage of native <CODE>clone()</CODE> method of
 * <CODE>Object</CODE> class (without throwing
 * <CODE>CloneNotSupportedException</CODE>) Important notes: the
 * classes which implement this interface should also override the
 * standard <CODE>equals(Object)</CODE> and <CODE>hashCode()</CODE>
 * methods.
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 2.0
 */
public interface ReallyCloneable extends Cloneable
{

/**
 * Creates and returns a copy of <CODE>this</CODE> object.
 **
 * By the <CODE>clone()</CODE> standard definition, this method
 * creates a new instance of the class of this object and
 * initializes all its fields with exactly the contents of the
 * corresponding fields of this object. Typically, native
 * <CODE>clone()</CODE> method of <CODE>Object</CODE> class is used
 * inside this method.
 **
 * @return
 * a copy (not <CODE>null</CODE> and != <CODE>this</CODE>) of
 * <CODE>this</CODE> instance.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 */
 public abstract Object clone();
}

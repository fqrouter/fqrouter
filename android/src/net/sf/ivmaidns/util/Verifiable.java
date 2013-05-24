/*
 * @(#) src/net/sf/ivmaidns/util/Verifiable.java --
 * Interface for object self-integrity check.
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
 * Interface for object self-integrity check.
 **
 * A class implementing this interface gives the programmer (or,
 * mainly, the debugger) the opportunity to check the consistency of
 * internal structures of the object of such class. If there is
 * nothing to check in some class then the class should not
 * implement this interface.
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 2.0
 */
public interface Verifiable
{

/**
 * Verifies <CODE>this</CODE> object for its integrity.
 **
 * This method is mostly used for debug purpose only. By default,
 * deep integrity (internal consistency) check is performed (that
 * is, all variable fields of <CODE>this</CODE> object are verified
 * too). Internal state of object is not altered. If internal
 * consistency is violated somehow (due to an error in Java VM or
 * due to a programmer error) then <CODE>InternalError</CODE> is
 * thrown (with some detailed message). Important notes:
 * <CODE>static final</CODE> fields should not be checked. This
 * method should be synchronized outside (unless the object is not
 * mutable).
 **
 * @exception InternalError
 * if integrity violation is detected.
 */
 public abstract void integrityCheck();
}

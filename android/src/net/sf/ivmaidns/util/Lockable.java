/*
 * @(#) src/net/sf/ivmaidns/util/Lockable.java --
 * Interface for objects synchronized on lock.
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
 * Interface for objects synchronized on lock.
 **
 * A class (typically active) implementing this interface allows an
 * application at any time to choose any custom object on which the
 * instance of the class should perform all its synchronization.
 * This is very useful when several stand-alone active objects (with
 * an internal self-synchronization) need to work in a particular
 * synchronous way (without any possibility of dead-lock).
 **
 * @see ActivityCore
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 2.0
 */
public interface Lockable
{

/**
 * Sets new lock object to be used for synchronization.
 **
 * Important notes: inside the class (implementing this interface),
 * <VAR>lock</VAR> should be <CODE>protected transient</CODE> and
 * non-<CODE>null</CODE>, by default <VAR>lock</VAR> is set to
 * <CODE>this</CODE> (during initialization, deserialization or
 * cloning); all needed synchronization (inside/outside the class)
 * must be done on <VAR>lock</VAR> (as shown below). This method
 * should be implemented as follows:
 **
 * <CODE><PRE>
 * protected transient Object lock = this;
 * public final void setLock(Object newLock)
 * throws NullPointerException
 * { // note: take care on deserializing
 * Object curLock;
 * newLock.equals(newLock); // this is a check for null
 * do
 * {
 * synchronized (curLock = lock)
 * {
 * if (curLock == lock)
 * { // check that synchronized on this lock
 * lock = newLock; // this is a synchronized operation
 * break;
 * }
 * }
 * } while (true);
 * }
 * </PRE></CODE>
 **
 * @param newLock
 * a lock object (must be non-<CODE>null</CODE>) to be used by
 * <CODE>this</CODE> object for doing all its synchronization.
 * @exception NullPointerException
 * if <VAR>newLock</VAR> is <CODE>null</CODE>.
 **
 * @see #getLock()
 */
 public abstract void setLock(Object newLock)
  throws NullPointerException;

/**
 * Just returns the current lock object.
 **
 * This method should be implemented as follows:
 **
 * <CODE><PRE>
 * public final Object getLock()
 * {
 * return lock;
 * }
 * </PRE></CODE>
 **
 * @return
 * the current <VAR>lock</VAR> object, on which <CODE>this</CODE>
 * object performs all its synchronization.
 **
 * @see #setLock(java.lang.Object)
 */
 public abstract Object getLock();
}

/*
 * @(#) src/net/sf/ivmaidns/util/SafeRunnable.java --
 * Interface for safely runnable objects.
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
 * Interface for safely runnable objects.
 **
 * This interface provides the basis safe control over 'active'
 * objects, which start running (single or multiple threads) on the
 * creation and stop when done (or when Java VM terminates or when
 * stop is requested by the user of the object). Active entities may
 * be also interrupted, suspended and resumed safely.
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public interface SafeRunnable
{

/**
 * Interrupts sleeping or waiting inside active object.
 **
 * Interruption means sending a special <VAR>interrupt</VAR> signal
 * to each thread of active object (resulting in throwing of
 * <CODE>InterruptedException</CODE> inside thread when it is
 * waiting or sleeping). This method returns immediately.
 */
 public abstract void interrupt();

/**
 * Initiates safe suspend operation.
 **
 * This method just sets <VAR>suspending</VAR> flag which signals
 * active object that all its threads (only which are alive) must be
 * safely suspended (turned to sleeping state) as soon as possible.
 * This method returns immediately.
 **
 * @see #waitSuspend()
 * @see #resume()
 * @see #isSuspended()
 * @see #stop()
 */
 public abstract void suspend();

/**
 * Initiates and waits for safe suspend.
 **
 * This method sets <VAR>suspending</VAR> flag which signals active
 * object that all its threads (only which are alive) must be safely
 * suspended (turned to sleeping state) as soon as possible. This
 * method returns just after all threads of active object have been
 * suspended (or died).
 **
 * @see #suspend()
 * @see #resume()
 * @see #isSuspended()
 */
 public abstract void waitSuspend();

/**
 * Resumes running after suspend.
 **
 * This method clears <VAR>suspending</VAR> flag, thus telling
 * active object to resume normal running (stop sleeping) after safe
 * suspend for all its still alive threads. This method returns
 * immediately.
 **
 * @see #suspend()
 * @see #waitSuspend()
 * @see #isSuspended()
 */
 public abstract void resume();

/**
 * Initiates safe stop operation.
 **
 * This method just sets <VAR>stopping</VAR> flag which signals
 * active object that all its threads (only which are alive) must be
 * safely terminated (stopped) as soon as possible. This method
 * returns immediately. If active object is suspended then safe stop
 * operation begins just when resuming.
 **
 * @see #suspend()
 * @see #resume()
 * @see #join()
 * @see #isSuspended()
 * @see #isAlive()
 */
 public abstract void stop();

/**
 * Waits while active object is alive.
 **
 * This method infinitely waits for the death (termination) of all
 * threads inside this active object.
 **
 * @see #stop()
 * @see #isAlive()
 * @see #waitSuspend()
 */
 public abstract void join();

/**
 * Tests whether this active object is suspended.
 **
 * This method just immediately returns <VAR>suspended</VAR> flag
 * which is true only when active object is in safe-suspend state
 * (and alive).
 **
 * @return
 * <CODE>true</CODE> if and only if active object is suspended.
 **
 * @see #suspend()
 * @see #waitSuspend()
 * @see #resume()
 * @see #isAlive()
 */
 public abstract boolean isSuspended();

/**
 * Tests whether this active object is still alive.
 **
 * This method just immediately returns the flag indicating that one
 * or more threads (inside this active object) have not died yet.
 **
 * @return
 * <CODE>true</CODE> if and only if active object is alive.
 **
 * @see #stop()
 * @see #join()
 * @see #isSuspended()
 */
 public abstract boolean isAlive();
}

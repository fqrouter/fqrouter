/*
 * @(#) src/net/sf/ivmaidns/util/MultiObservable.java --
 * Interface for observable objects.
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
 * Interface for observable objects.
 **
 * Observable object represents mutable 'data' in the model-view
 * paradigm. Each time an observable object is changed, it calls
 * <CODE>update(this, argument)</CODE> for every registered observer
 * agent (in an unspecified order), where <VAR>argument</VAR>
 * describes the occurred changes (as it must be specified for a
 * particular object).
 **
 * @see Notifiable
 * @see ObservedCore
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public interface MultiObservable
{

/**
 * Registers one more observer.
 **
 * An observer registration means that <VAR>agent</VAR> will be
 * updated (notified) each time <CODE>this</CODE> observable object
 * is changed somehow. Duplicate agents are not added (registered).
 * If an exception is thrown then state of <CODE>this</CODE> object
 * is not changed. Important notes: registered observers should not
 * be accessible for other objects.
 **
 * @param agent
 * the observer agent (must be non-<CODE>null</CODE>) to be
 * registered.
 * @exception NullPointerException
 * if <VAR>agent</VAR> is <CODE>null</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #removeObserver(net.sf.ivmaidns.util.Notifiable)
 */
 public abstract void addObserver(Notifiable agent)
  throws NullPointerException;

/**
 * Unregisters a particular observer.
 **
 * If <VAR>agent</VAR> is not registered (or is <CODE>null</CODE>)
 * then nothing is performed. Else the specified agent is removed
 * from the observers list of <CODE>this</CODE> observable object
 * (this action is just the opposite to the agent registration).
 **
 * @param agent
 * the observer agent (may be <CODE>null</CODE>) to be unregistered.
 **
 * @see #addObserver(net.sf.ivmaidns.util.Notifiable)
 */
 public abstract void removeObserver(Notifiable agent);
}

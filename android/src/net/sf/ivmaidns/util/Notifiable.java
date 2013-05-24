/*
 * @(#) src/net/sf/ivmaidns/util/Notifiable.java --
 * Interface for custom observer agents.
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
 * Interface for custom observer agents.
 **
 * A class should implement (or should have a private class which
 * implements) this interface when it wants to be informed of
 * changes in the observable object. Normally, observer agent serves
 * as an intermediate entity between one or more observed entities
 * and the application entity which wants to be notified (with
 * details) only on the events in these observed objects.
 * <CODE>Verifiable</CODE> interface is extended to allow integrity
 * checking for typical agent objects. Important notes: agents must
 * not modify <VAR>observed</VAR> object anyhow; notification should
 * be performed just after changes; this notification mechanism has
 * nothing to do with threads and is completely separate from the
 * 'wait-notify' mechanism of <CODE>Object</CODE>. Here is the usage
 * example of an observer agent:
 **
 * <CODE><PRE>
 * final class MyAgent // hidden helper class
 * implements Notifiable
 * {
 * private final MyClass myClass; // myClass != null
 * protected MyAgent(MyClass myClass)
 * throws NullPointerException
 * {
 * myClass.equals(myClass);
 * this.myClass = myClass;
 * }
 * public void update(MultiObservable observed, Object argument)
 * {
 * MyClass myClass = this.myClass;
 * if (argument instanceof MyEvent && myClass.observed == observed)
 * // safe (private) actions on myClass
 * }
 * public void integrityCheck()
 * {
 * if (myClass == null)
 * throw new InternalError("myClass: null");
 * }
 * }
 * private transient Notifiable agent = new MyAgent(this);
 * . . .
 * observed.addObserver(agent);
 * . . .
 * observed.removeObserver(agent); // stop observing
 * </PRE></CODE>
 **
 * @see MultiObservable
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public interface Notifiable extends Verifiable
{

/**
 * Notifies <CODE>this</CODE> observer agent on a particular event
 * that just occurred.
 **
 * This method is called whenever <VAR>observed</VAR> object is
 * changed. Important notes: <VAR>observed</VAR> object should not
 * be modified during the notification; types of <VAR>observed</VAR>
 * and <VAR>argument</VAR> should be checked inside; no access
 * should be provided to the instance of this interface (since this
 * method is <CODE>public</CODE>) outside hidden
 * <CODE>notifyObservers</CODE> method of <VAR>observed</VAR>
 * object; <CODE>RuntimeException</CODE> (and
 * <CODE>OutOfMemoryError</CODE>) may be thrown if a notification
 * failure has occurred.
 **
 * @param observed
 * the observed object (may be <CODE>null</CODE>, should be checked
 * inside).
 * @param argument
 * the argument (may be <CODE>null</CODE>, should be checked inside)
 * describing the occurred event.
 * @exception RuntimeException
 * if the notification process has failed (a custom exception).
 * @exception OutOfMemoryError
 * if there is not enough memory (to perform notification).
 */
 public abstract void update(MultiObservable observed,
         Object argument);
}

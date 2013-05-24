/*
 * @(#) src/net/sf/ivmaidns/util/ObservedCore.java --
 * Root class for observable objects.
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
 * Root class for observable objects.
 **
 * This is an implementation of <CODE>MultiObservable</CODE>. An
 * observable object (which class extends or includes a variable of
 * this class) represents mutable 'data' in the model-view paradigm.
 * Each time an observable object is changed, it must call
 * <CODE>notifyObservers(this, argument)</CODE> to notify every
 * registered observer agent (in an unspecified order) about the
 * event, where <VAR>argument</VAR> describes the occurred changes
 * (as it must be specified for a particular object). Important
 * notes: agents must not modify observed object anyhow;
 * notification should be performed just after changes; this
 * notification mechanism has nothing to do with threads and is
 * completely separate from the 'wait-notify' mechanism of
 * <CODE>Object</CODE>.
 **
 * @see Notifiable
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public class ObservedCore
 implements MultiObservable, TrimToSizeable, Verifiable
{

/**
 * An empty <CODE>private</CODE> list of observers.
 **
 * This constant is used to avoid empty array allocation on
 * instantiation of this class (and on trimming to its minimal
 * size).
 **
 * @see #clone()
 * @see #trimToSize()
 */
 private static final Notifiable[] EMPTY_OBSERVERS = {};

/**
 * Array (list) of registered observer agents.
 **
 * Each non-<CODE>null</CODE> agent of this array is notified when
 * <CODE>notifyObservers(observed, argument)</CODE> is called.
 * <VAR>observers</VAR> must be non-<CODE>null</CODE>. This array is
 * set to <VAR>EMPTY_OBSERVERS</VAR> on creation and cloning of
 * <CODE>this</CODE> object. No <CODE>public</CODE> access (even
 * read-only) should be provided to <VAR>observers</VAR> elements
 * (except through notification). In future, <VAR>observers</VAR>
 * may be implemented as an array of weak references.
 **
 * @see #clone()
 * @see #trimToSize()
 */
 private Notifiable[] observers = EMPTY_OBSERVERS;

/**
 * Constructs an observable object.
 **
 * @see #addObserver(net.sf.ivmaidns.util.Notifiable)
 */
 public ObservedCore() {}

/**
 * Frees extra memory.
 **
 * This method re-allocates internal <VAR>observers</VAR> list,
 * setting its length to the current possible minimum. Observer
 * agents are not modified. Observers order is not changed. This
 * method must be synchronized outside.
 **
 * @see #addObserver(net.sf.ivmaidns.util.Notifiable)
 * @see #removeObserver(net.sf.ivmaidns.util.Notifiable)
 */
 public void trimToSize()
 {
  Notifiable[] observers = this.observers;
  int count = 0, index = observers.length, len = index;
  while (index > 0)
   if (observers[--index] != null)
    count++;
  if (count < len)
  {
   Notifiable[] newObservers = EMPTY_OBSERVERS;
   if (count > 0)
   {
    try
    {
     newObservers = new Notifiable[count];
    }
    catch (OutOfMemoryError e)
    {
     return;
    }
    int offset = 0;
    do
    {
     Notifiable agent;
     if ((agent = observers[index]) != null)
     {
      newObservers[offset] = agent;
      if (++offset >= count)
       break;
     }
    } while (++index < len);
   }
   this.observers = newObservers;
  }
 }

/**
 * Registers one more observer.
 **
 * An observer registration means that <VAR>agent</VAR> will be
 * updated (notified) each time <CODE>this</CODE> observable object
 * is changed somehow. If the specified agent is already registered
 * here then the registration of this agent is not performed (no
 * duplicate agents). Internal <VAR>observers</VAR> array may be
 * re-allocated (to have at least enough space for holding all
 * registered agents). If an exception is thrown then state of
 * <CODE>this</CODE> object is not changed. This method must be
 * synchronized outside. Important notes: registered observers are
 * not accessible for other objects, not copied when
 * <CODE>this</CODE> object is cloned, and not serialized.
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
 * @see #notifyObservers(net.sf.ivmaidns.util.MultiObservable,
 * java.lang.Object)
 */
 public void addObserver(Notifiable agent)
  throws NullPointerException
 {
  agent.equals(agent);
  Notifiable[] observers = this.observers;
  int index = observers.length, count = index;
  Notifiable curAgent;
  while (index-- > 0)
   if ((curAgent = observers[index]) != null &&
       curAgent.equals(agent))
    return;
  while (++index < count && observers[index] != null);
  if (index >= count)
  {
   if ((count = (count >> 1) + count + 1) <= index)
    count = -1 >>> 1;
   Notifiable[] newObservers;
   System.arraycopy(observers, 0,
    newObservers = new Notifiable[count], 0, index);
   this.observers = observers = newObservers;
  }
  observers[index] = agent;
 }

/**
 * Unregisters a particular observer.
 **
 * If <VAR>agent</VAR> is <CODE>null</CODE> or
 * <CODE>equals(agent)</CODE> is <CODE>false</CODE> for every
 * registered agent then nothing is performed. Else the specified
 * agent is removed from <VAR>observers</VAR> of <CODE>this</CODE>
 * observable object (this action is just the opposite to the agent
 * registration). Internal <VAR>observers</VAR> array is not
 * re-allocated. This method must be synchronized outside.
 **
 * @param agent
 * the observer agent (may be <CODE>null</CODE>) to be unregistered.
 **
 * @see #addObserver(net.sf.ivmaidns.util.Notifiable)
 */
 public void removeObserver(Notifiable agent)
 {
  if (agent != null)
  {
   Notifiable[] observers = this.observers;
   Notifiable curAgent;
   boolean hasObservers = false;
   for (int index = 0, count = observers.length;
        index < count; index++)
    if ((curAgent = observers[index]) != null)
    {
     if (curAgent.equals(agent))
     {
      observers[index] = null;
      if (hasObservers)
       break;
      while (++index < count)
       if (observers[index] != null)
        return;
      this.observers = EMPTY_OBSERVERS;
      break;
     }
     hasObservers = true;
    }
   hasObservers = false;
  }
 }

/**
 * Tests whether <CODE>this</CODE> observable object has any
 * observers.
 **
 * If the result is <CODE>false</CODE> then there is no observers
 * which must be updated so at this moment it is useless to call
 * <CODE>notifyObservers(MultiObservable, Object)</CODE> method.
 **
 * @return
 * <CODE>false</CODE> only if no observer agents registered.
 **
 * @see #addObserver(net.sf.ivmaidns.util.Notifiable)
 * @see #removeObserver(net.sf.ivmaidns.util.Notifiable)
 * @see #notifyObservers(net.sf.ivmaidns.util.MultiObservable,
 * java.lang.Object)
 */
 public final boolean hasObservers()
 {
  return this.observers.length > 0;
 }

/**
 * Notifies each registered observer agent on the event that just
 * occurred.
 **
 * Agents notification means calling
 * <CODE>update(observed, argument)</CODE> for every agent which is
 * in observers list of <CODE>this</CODE> observable object. The
 * order of notification is undefined. Important notes: notification
 * should be done after committing of the occurred modification of
 * <VAR>observed</VAR>; <VAR>argument</VAR> object should provide
 * minimum yet enough information to effectively find out new state
 * of the object; <CODE>RuntimeException</CODE> (and
 * <CODE>OutOfMemoryError</CODE>) should be handled properly (since
 * some of the agents may have already been notified before the
 * exception is thrown); this method should be called only from the
 * observable object (so, in subclasses this method should be
 * overridden with a <CODE>public</CODE> dummy method).
 **
 * @param observed
 * the observed object (may be <CODE>null</CODE>, but normally
 * <CODE>this</CODE>).
 * @param argument
 * the argument (may be <CODE>null</CODE>), describing the occurred
 * event.
 * @exception RuntimeException
 * if the notification process for some registered agent has failed
 * (a custom exception, not all of the agents may have been
 * notified).
 * @exception OutOfMemoryError
 * if there is not enough memory to complete notification (not all
 * of the agents may have been notified).
 **
 * @see #addObserver(net.sf.ivmaidns.util.Notifiable)
 */
 public void notifyObservers(MultiObservable observed,
         Object argument)
  throws RuntimeException
 {
  int index = 0;
  Notifiable[] observers = this.observers;
  int len = observers.length;
  for (Notifiable agent; index < len; index++)
   if ((agent = observers[index]) != null)
    agent.update(observed, argument);
 }

/**
 * Creates and returns a copy of <CODE>this</CODE> object.
 **
 * This method overrides <CODE>clone()</CODE> of <CODE>Object</CODE>
 * to prevent copying of observers list (it is set empty in the
 * returned observable object). Of course, this method works only
 * for subclasses which implement <CODE>Cloneable</CODE> interface.
 * This method may be overridden and made <CODE>public</CODE> in
 * the subclasses if needed.
 **
 * @return
 * a copy (may be <CODE>null</CODE>) of <CODE>this</CODE> instance.
 * @exception CloneNotSupportedException
 * if <CODE>Cloneable</CODE> interface is not implemented (in a
 * subclass).
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @since 1.1
 */
 protected Object clone()
  throws CloneNotSupportedException
 {
  Object obj;
  if ((obj = super.clone()) instanceof ObservedCore && obj != this)
   ((ObservedCore)obj).observers = EMPTY_OBSERVERS;
  return obj;
 }

/**
 * Verifies <CODE>this</CODE> object for its integrity.
 **
 * Observer agents of <CODE>this</CODE> observable are not checked.
 * For debug purpose only.
 **
 * @exception InternalError
 * if integrity violation is detected.
 **
 * @since 2.0
 */
 public void integrityCheck()
 {
  if (this.observers == null)
   throw new InternalError("observers: null");
 }
}

/*
 * @(#) src/net/sf/ivmaidns/util/ActivityCore.java --
 * Class for active observable entities.
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
 * Class for active observable entities.
 **
 * This is a basis implementation of <CODE>SafeRunnable</CODE>
 * interface, which also extends <CODE>ObservedCore</CODE> class,
 * providing an easy way to create a custom active observable
 * object, which starts running on its creation and stops when done
 * or when safe stop is requested by the user of the object (or when
 * Java Virtual Machine terminates). Active objects may be also
 * interrupted, suspended and resumed. The activity of such object
 * is implemented through the encapsulation of <CODE>Thread</CODE>
 * instance. Created <VAR>thread</VAR> is not daemon (unless current
 * thread is a daemon). Important notes: the Java Virtual Machine
 * exits when the only threads running are all daemons or if
 * <CODE>exit(int)</CODE> method of <CODE>Runtime</CODE> class is
 * called; <CODE>Runnable</CODE> interface is implemented here
 * entirely for the internal purpose.
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public abstract class ActivityCore extends ObservedCore
 implements SafeRunnable, Runnable
{

/**
 * Represents the default idle sleep time in milliseconds.
 **
 * This <CODE>int</CODE> (positive) value is used as a default
 * argument for <CODE>wait(long)</CODE> of <CODE>Object</CODE> class
 * and for <CODE>sleep(long)</CODE> of <CODE>Thread</CODE> class
 * when infinitely waiting for a particular event.
 **
 * @since 2.0
 */
 protected static final int IDLE_SLEEP_MILLIS = 1000;

/**
 * Indicates whether or not <CODE>run()</CODE> method is called.
 **
 * This internal flag is used only to ensure that <CODE>run()</CODE>
 * is called only once. The flag is trigged only once.
 **
 * @see #run()
 */
 private boolean initiated;

/**
 * Indicates whether or not safe suspending is requested for
 * <CODE>this</CODE> active object.
 **
 * This internal flag is used only to safely process the user
 * intention to temporarily suspend running of <CODE>this</CODE>
 * active object. Clearing of this flag means running is immediately
 * resumed.
 **
 * @see #run()
 * @see #suspend()
 * @see #waitSuspend()
 * @see #resume()
 */
 private boolean suspending;

/**
 * Indicates whether or not <CODE>this</CODE> object is safely
 * suspended.
 **
 * This flag may be checked via <CODE>isSuspended()</CODE>.
 **
 * @see #run()
 * @see #waitSuspend()
 * @see #resume()
 * @see #isSuspended()
 */
 private boolean suspended;

/**
 * Indicates whether or not safe stop is requested for
 * <CODE>this</CODE> active object.
 **
 * This internal flag is used only to safely process the user
 * intention to terminate (abort) running of <CODE>this</CODE>
 * active object. The flag is never trigged or trigged only once.
 **
 * @see #run()
 * @see #stop()
 * @see #join()
 * @see #isAlive()
 */
 private boolean stopping;

/**
 * The encapsulated <CODE>Thread</CODE> instance for
 * <CODE>this</CODE> active object.
 **
 * <VAR>thread</VAR> (must be non-<CODE>null</CODE>) is started in
 * the constructor. The target of this thread is <CODE>run()</CODE>
 * method of this class. <VAR>thread</VAR> is not accessible outside
 * the class (even for the subclasses of it).
 **
 * @see ActivityCore#ActivityCore(java.lang.String)
 * @see #run()
 */
 private final Thread thread;

/**
 * Constructs an active observable object with the specified thread
 * name.
 **
 * Created <VAR>thread</VAR> is marked as a daemon only if current
 * thread is a daemon. The thread is started (activated) just after
 * its creation. The user of <CODE>this</CODE> constructed object
 * may safely suspend/resume or stop its activity at any time (while
 * it is running). <VAR>name</VAR> is entirely used to identify the
 * thread in the computer system (for the purpose of the system
 * performance monitoring).
 **
 * @param name
 * the name (must be non-<CODE>null</CODE>) of created
 * <VAR>thread</VAR>.
 * @exception NullPointerException
 * if <VAR>name</VAR> is <CODE>null</CODE>.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 * @exception InternalError
 * if Java VM internal error occurs or if the security manager
 * prohibits the creation of a new thread.
 **
 * @see #run()
 * @see #stop()
 * @see #join()
 * @see #isAlive()
 */
 public ActivityCore(String name)
  throws NullPointerException
 {
  name.equals(name);
  try
  {
   (this.thread = new Thread(this, name)).start();
  }
  catch (SecurityException e)
  {
   throw new InternalError("SecurityException: new Thread()");
  }
  name = null;
 }

/**
 * Custom initialization method which is executed just at the
 * beginning of the execution of <CODE>this</CODE> active object.
 **
 * This method is only called internally once (in the encapsulated
 * thread, not in the constructor) before calling
 * <CODE>loop()</CODE>. If <CODE>OutOfMemoryError</CODE> is thrown
 * then <CODE>loop()</CODE> is skipped and <CODE>done()</CODE> is
 * called. This method should be <CODE>protected final</CODE> in the
 * subclasses if overridden (if not overridden then it is dummy).
 **
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see ActivityCore#ActivityCore(java.lang.String)
 * @see #loop()
 * @see #done()
 * @see #run()
 */
 protected void init() {}

/**
 * Custom action main method which is executed in <CODE>this</CODE>
 * active object as many times as needed.
 **
 * This method is only called internally (in the encapsulated
 * thread) many times (may be none) while soft stop is not requested
 * and result is <CODE>true</CODE>. If <CODE>OutOfMemoryError</CODE>
 * is thrown then <CODE>loop()</CODE> is not called anymore and
 * <CODE>done()</CODE> is called. This method should be
 * <CODE>protected final</CODE> in the subclasses.
 **
 * @return
 * <CODE>true</CODE> only if this method requests to be called
 * again.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #init()
 * @see #done()
 * @see #run()
 * @see #stop()
 */
 protected abstract boolean loop();

/**
 * Custom clean-up method which is executed before normal
 * termination of <CODE>this</CODE> active object.
 **
 * This method is called internally once (in the encapsulated
 * thread) at the end after calling <CODE>loop()</CODE> method. This
 * method is called even if soft stop operation is being performed
 * or even if <CODE>loop()</CODE> (or <CODE>init()</CODE>) method
 * has just thrown <CODE>OutOfMemoryError</CODE>. If this method
 * throws <CODE>OutOfMemoryError</CODE> then it is caught silently.
 * This method should be <CODE>protected final</CODE> in the
 * subclasses if overridden (if not overridden then it is dummy).
 **
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @see #init()
 * @see #loop()
 * @see #run()
 * @see #stop()
 */
 protected void done() {}

/**
 * The thread execution body (an internal method).
 **
 * This method is called internally (only once) by
 * <CODE>Thread</CODE> object when <VAR>thread</VAR> of
 * <CODE>this</CODE> is initialized (when <CODE>this</CODE> active
 * object is constructed). This method first calls
 * <CODE>init()</CODE> method then calls <CODE>loop()</CODE> many
 * times while it returns <CODE>true</CODE>, then calls
 * <CODE>done()</CODE> (semantics of all these three methods is
 * defined by the subclass), and returns. Each time, before calling
 * <CODE>loop()</CODE>, this method analyses the state of
 * <VAR>suspending</VAR> and <VAR>stopping</VAR> to perform safe
 * suspend/resume and stop operations whenever they are requested by
 * the user of <CODE>this</CODE> active object. If
 * <VAR>suspending</VAR> is set then this method waits (before
 * calling <CODE>loop()</CODE>) while <VAR>suspending</VAR> remains
 * set. Setting of <VAR>stopping</VAR> would stop looping, this
 * method calls <CODE>done()</CODE> and returns. If
 * <CODE>OutOfMemoryError</CODE> is thrown during execution of these
 * 'init/loop/done' methods then it is silently ignored and has the
 * same effect as if <VAR>stopping</VAR> is <CODE>true</CODE>.
 * Important notes: on return of this method <VAR>thread</VAR> is
 * terminated; if this method is called outside <CODE>Thread</CODE>
 * then it does nothing.
 **
 * @see ActivityCore#ActivityCore(java.lang.String)
 * @see #suspend()
 * @see #waitSuspend()
 * @see #resume()
 * @see #stop()
 */
 public final void run()
 {
  Thread thread;
  boolean stopping;
  synchronized (thread = this.thread)
  {
   stopping = this.initiated;
   this.initiated = true;
  }
  if (!stopping)
  {
   try
   {
    init();
    do
    {
     if (!(stopping = this.stopping))
      if (!this.suspending)
      {
       try
       {
        Thread.sleep(0);
       }
       catch (InterruptedException e) {}
      }
       else synchronized (thread)
       {
        if (this.suspending)
        {
         this.suspended = true;
         thread.notifyAll();
         do
         {
          try
          {
           thread.wait(IDLE_SLEEP_MILLIS);
          }
          catch (InterruptedException e) {}
         } while (this.suspending);
         this.suspended = false;
        }
        stopping = this.stopping;
       }
    } while (!stopping && loop());
   }
   catch (OutOfMemoryError e) {}
   try
   {
    done();
   }
   catch (OutOfMemoryError e) {}
  }
  thread = null;
 }

/**
 * Frees extra memory.
 **
 * This method re-allocates internal structures (of the object)
 * releasing unused memory occupied by them. This method must be
 * synchronized outside.
 **
 * @see ActivityCore#ActivityCore(java.lang.String)
 */
 public void trimToSize()
 {
  super.trimToSize();
 }

/**
 * Interrupts sleeping or waiting inside active object.
 **
 * Interruption means sending a special <VAR>interrupt</VAR> signal
 * to each thread of active object (resulting in throwing of
 * <CODE>InterruptedException</CODE> inside thread when it is
 * waiting or sleeping). This method returns immediately. This
 * method may be overridden in the subclasses. Important notes:
 * current thread interruption status may be cleared via
 * <CODE>sleep(0)</CODE> of <CODE>Thread</CODE> class.
 **
 * @see #isAlive()
 */
 public void interrupt()
 {
  Thread thread = this.thread;
  try
  {
   thread.interrupt();
  }
  catch (SecurityException e) {}
  thread = null;
 }

/**
 * Initiates safe suspend operation.
 **
 * This method just sets <VAR>suspending</VAR> flag which signals
 * active object that all its threads (only which are alive) must be
 * safely suspended (turned to sleeping state) as soon as possible.
 * This method returns immediately. This method may be overridden in
 * the subclasses.
 **
 * @see #waitSuspend()
 * @see #resume()
 * @see #isSuspended()
 * @see #stop()
 */
 public void suspend()
 {
  this.suspending = true;
 }

/**
 * Initiates and waits for safe suspend.
 **
 * This method sets <VAR>suspending</VAR> flag which signals active
 * object that all its threads (only which are alive) must be safely
 * suspended (turned to sleeping state) as soon as possible. This
 * method returns just after all threads of active object have been
 * suspended (or died). This method may be overridden in the
 * subclasses.
 **
 * @see #suspend()
 * @see #resume()
 * @see #isSuspended()
 */
 public void waitSuspend()
 {
  Thread thread;
  synchronized (thread = this.thread)
  {
   this.suspending = true;
   while (!this.suspended && thread.isAlive())
   {
    try
    {
     thread.wait(IDLE_SLEEP_MILLIS);
    }
    catch (InterruptedException e) {}
   }
  }
  thread = null;
 }

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
 public final void resume()
 {
  Thread thread;
  synchronized (thread = this.thread)
  {
   this.suspending = false;
   if (this.suspended && thread.isAlive())
    thread.notifyAll();
  }
  thread = null;
 }

/**
 * Initiates safe stop operation.
 **
 * This method just sets <VAR>stopping</VAR> flag which signals
 * active object that all its threads (only which are alive) must be
 * safely terminated (stopped) as soon as possible. This method
 * returns immediately. If active object is suspended then safe stop
 * operation begins just when resuming. This method may be
 * overridden in the subclasses.
 **
 * @see #suspend()
 * @see #resume()
 * @see #join()
 * @see #isSuspended()
 * @see #isAlive()
 */
 public void stop()
 {
  this.stopping = true;
 }

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
 public final void join()
 {
  Thread thread;
  synchronized (thread = this.thread)
  {
   while (thread.isAlive())
   {
    try
    {
     thread.wait(IDLE_SLEEP_MILLIS);
    }
    catch (InterruptedException e) {}
   }
  }
  thread = null;
 }

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
 public final boolean isSuspended()
 {
  boolean suspended;
  synchronized (this.thread)
  {
   suspended = this.suspended;
  }
  return suspended;
 }

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
 public final boolean isAlive()
 {
  return this.thread.isAlive();
 }

/**
 * Creates and returns a copy of <CODE>this</CODE> object.
 **
 * The implementation of this method prohibits (since thread object
 * is not cloneable) the usage of standard <CODE>clone()</CODE>
 * method of <CODE>Object</CODE> (even in the subclasses) by
 * throwing <CODE>CloneNotSupportedException</CODE>. But, if needed,
 * this method may be overridden (and made <CODE>public</CODE>) in
 * the subclasses, providing 'pseudo-cloning' (through the
 * constructor of a subclass).
 **
 * @return
 * a copy (not <CODE>null</CODE> and != <CODE>this</CODE>) of
 * <CODE>this</CODE> instance.
 * @exception CloneNotSupportedException
 * if cloning is not implemented.
 * @exception OutOfMemoryError
 * if there is not enough memory.
 **
 * @since 1.1
 */
 protected Object clone()
  throws CloneNotSupportedException
 {
  throw new CloneNotSupportedException();
 }

/**
 * Verifies <CODE>this</CODE> object for its integrity.
 **
 * For debug purpose only.
 **
 * @exception InternalError
 * if integrity violation is detected.
 **
 * @since 2.0
 */
 public void integrityCheck()
 {
  super.integrityCheck();
  if (this.thread == null)
   throw new InternalError("thread: null");
 }
}

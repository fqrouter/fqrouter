/*
 * @(#) src/net/sf/ivmaidns/util/Metricable.java --
 * Interface for integer metrics adapters.
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

import java.io.Serializable;

/**
 * Interface for integer metrics adapters.
 **
 * This interface declares a custom function which may evaluate
 * (according to the semantics) an object value. Metrics may also
 * serve as a predicate (filter), just if a non-zero result is
 * interpreted as <CODE>true</CODE>. Metrics may be used in
 * serializable data structures since <CODE>Serializable</CODE>
 * interface is extended.
 **
 * @see ToString
 * @see GComparator
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 2.0
 */
public interface Metricable extends Serializable
{

/**
 * The body of the metrics.
 **
 * Evaluates the supplied object. If the object is not instance of
 * the expected type then <CODE>0</CODE> is always returned.
 **
 * @param obj
 * the object (may be <CODE>null</CODE>) to evaluate.
 * @return
 * the integer result of the performed evaluation.
 */
 public abstract int evaluate(Object obj);
}

// Debug.java - Print debug messages

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.xml.resolver.helpers;

/**
 * Static debugging/messaging class for Catalogs.
 *
 * <p>This class defines a set of static methods that can be called
 * to produce debugging messages. Messages have an associated "debug
 * level" and messages below the current setting are not displayed.</p>
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class Debug {
  /** The internal debug level. */
  protected int debug = 0;

  /** Constructor */
  public Debug() {
    // nop
  }

  /** Set the debug level for future messages. */
  public void setDebug(int newDebug) {
    debug = newDebug;
  }

  /** Get the current debug level. */
  public int getDebug() {
    return debug;
  }

  /**
   * Print debug message (if the debug level is high enough).
   *
   * <p>Prints "the message"</p>
   *
   * @param level The debug level of this message. This message
   * will only be
   * displayed if the current debug level is at least equal to this
   * value.
   * @param message The text of the message.
   */
  public void message(int level, String message) {
    if (debug >= level) {
      System.out.println(message);
    }
  }

  /**
   * Print debug message (if the debug level is high enough).
   *
   * <p>Prints "the message: spec"</p>
   *
   * @param level The debug level of this message. This message
   * will only be
   * displayed if the current debug level is at least equal to this
   * value.
   * @param message The text of the message.
   * @param spec An argument to the message.
   */
  public void message(int level, String message, String spec) {
    if (debug >= level) {
      System.out.println(message + ": " + spec);
    }
  }

  /**
   * Print debug message (if the debug level is high enough).
   *
   * <p>Prints "the message: spec1" and "spec2" indented on the next line.</p>
   *
   * @param level The debug level of this message. This message
   * will only be
   * displayed if the current debug level is at least equal to this
   * value.
   * @param message The text of the message.
   * @param spec1 An argument to the message.
   * @param spec2 Another argument to the message.
   */
  public void message(int level, String message,
			     String spec1, String spec2) {
    if (debug >= level) {
      System.out.println(message + ": " + spec1);
      System.out.println("\t" + spec2);
    }
  }
}

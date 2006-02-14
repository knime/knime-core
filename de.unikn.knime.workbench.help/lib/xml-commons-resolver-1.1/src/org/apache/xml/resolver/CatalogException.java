// CatalogException.java - Catalog exception

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

package org.apache.xml.resolver;

/**
 * Signal Catalog exception.
 *
 * <p>This exception is thrown if an error occurs loading a
 * catalog file.</p>
 *
 * @see Catalog
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class CatalogException extends Exception {
  /** A wrapper around another exception */
  public static final int WRAPPER = 1;
  /** An invalid entry */
  public static final int INVALID_ENTRY = 2;
  /** An invalid entry type */
  public static final int INVALID_ENTRY_TYPE = 3;
  /** Could not instantiate an XML parser */
  public static final int NO_XML_PARSER = 4;
  /** Unknown XML format */
  public static final int UNKNOWN_FORMAT = 5;
  /** Unparseable XML catalog (not XML)*/
  public static final int UNPARSEABLE = 6;
  /** XML but parse failed */
  public static final int PARSE_FAILED = 7;

  /**
   * The embedded exception if tunnelling, or null.
   */
  private Exception exception = null;
  private int exceptionType = 0;

  /**
   * Create a new CatalogException.
   *
   * @param type The exception type
   * @param message The error or warning message.
   */
  public CatalogException (int type, String message) {
    super(message);
    this.exceptionType = type;
    this.exception = null;
  }

  /**
   * Create a new CatalogException.
   *
   * @param type The exception type
   */
  public CatalogException (int type) {
    super("Catalog Exception " + type);
    this.exceptionType = type;
    this.exception = null;
  }

  /**
   * Create a new CatalogException wrapping an existing exception.
   *
   * <p>The existing exception will be embedded in the new
   * one, and its message will become the default message for
   * the CatalogException.</p>
   *
   * @param e The exception to be wrapped in a CatalogException.
   */
  public CatalogException (Exception e) {
    super();
    this.exceptionType = WRAPPER;
    this.exception = e;
  }

  /**
   * Create a new CatalogException from an existing exception.
   *
   * <p>The existing exception will be embedded in the new
   * one, but the new exception will have its own message.</p>
   *
   * @param message The detail message.
   * @param e The exception to be wrapped in a CatalogException.
   */
  public CatalogException (String message, Exception e) {
    super(message);
    this.exceptionType = WRAPPER;
    this.exception = e;
  }

  /**
   * Return a detail message for this exception.
   *
   * <p>If there is an embedded exception, and if the CatalogException
   * has no detail message of its own, this method will return
   * the detail message from the embedded exception.</p>
   *
   * @return The error or warning message.
   */
  public String getMessage ()
  {
    String message = super.getMessage();

    if (message == null && exception != null) {
      return exception.getMessage();
    } else {
      return message;
    }
  }

  /**
   * Return the embedded exception, if any.
   *
   * @return The embedded exception, or null if there is none.
   */
  public Exception getException ()
  {
    return exception;
  }

  /**
   * Return the exception type
   *
   * @return The exception type
   */
  public int getExceptionType ()
  {
    return exceptionType;
  }

  /**
   * Override toString to pick up any embedded exception.
   *
   * @return A string representation of this exception.
   */
  public String toString ()
  {
    if (exception != null) {
      return exception.toString();
    } else {
      return super.toString();
    }
  }
}

// XParseError.java - An error handler for xparse

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

package org.apache.xml.resolver.apps;

import java.net.URL;
import java.net.MalformedURLException;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * An ErrorHandler for xparse.
 *
 * <p>This class is just the error handler for xparse.</p>
 *
 * @see xparse
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class XParseError implements ErrorHandler {
  /** Show errors? */
  private boolean showErrors = true;

  /** Show warnings? */
  private boolean showWarnings = false;

  /** How many messages should be presented? */
  private int maxMessages = 10;

  /** The number of fatal errors seen so far. */
  private int fatalCount = 0;

  /** The number of errors seen so far. */
  private int errorCount = 0;

  /** The number of warnings seen so far. */
  private int warningCount = 0;

  /** The base URI of the running application. */
  private String baseURI = "";

  /** Constructor */
  public XParseError(boolean errors, boolean warnings) {
    showErrors = errors;
    showWarnings = warnings;

    String dir = System.getProperty("user.dir");
    String file = "";

    if (dir.endsWith("/")) {
      file = "file:" + dir + "file";
    } else {
      file = "file:" + dir + "/" + file;
    }

    try {
      URL url = new URL(file);
      baseURI = url.toString();
    } catch (MalformedURLException mue) {
      // nop;
    }
  }

  /** Return the error count */
  public int getErrorCount() {
    return errorCount;
  }

  /** Return the fatal error count */
  public int getFatalCount() {
    return fatalCount;
  }

  /** Return the warning count */
  public int getWarningCount() {
    return warningCount;
  }

  /** Return the number of messages to display */
  public int getMaxMessages() {
    return maxMessages;
  }

  /** Set the number of messages to display */
  public void setMaxMessages(int max) {
    maxMessages = max;
  }

  /** SAX2 API */
  public void error(SAXParseException exception) {
    if (showErrors) {
      if (errorCount+warningCount < maxMessages) {
	message("Error", exception);
      }
      errorCount++;
    }
  }

  /** SAX2 API */
  public void fatalError(SAXParseException exception) {
    if (showErrors) {
      if (errorCount+warningCount < maxMessages) {
	message("Fatal error", exception);
      }
      errorCount++;
      fatalCount++;
    }
  }

  /** SAX2 API */
  public void warning(SAXParseException exception) {
    if (showWarnings) {
      if ((errorCount+warningCount < maxMessages)) {
	message("Warning", exception);
      }
      warningCount++;
    }
  }

  /** Display a message to the user */
  private void message(String type, SAXParseException exception) {
    String filename = exception.getSystemId();
    if (filename.startsWith(baseURI)) {
      filename = filename.substring(baseURI.length());
    }

    System.out.print(type
		     + ":"
		     + filename
		     + ":"
		     + exception.getLineNumber());

    if (exception.getColumnNumber() > 0) {
      System.out.print(":" + exception.getColumnNumber());
    }

    System.out.println(":" + exception.getMessage());
  }
}

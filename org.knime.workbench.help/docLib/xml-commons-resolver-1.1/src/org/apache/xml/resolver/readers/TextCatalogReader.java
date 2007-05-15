// TextCatalogReader.java - Read text/plain Catalog files

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

package org.apache.xml.resolver.readers;

import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.Vector;
import java.util.Stack;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogEntry;
import org.apache.xml.resolver.CatalogException;
import org.apache.xml.resolver.readers.CatalogReader;

/**
 * Parses plain text Catalog files.
 *
 * <p>This class reads plain text Open Catalog files.</p>
 *
 * @see Catalog
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class TextCatalogReader implements CatalogReader {
  /** The input stream used to read the catalog */
  protected InputStream catfile = null;

  /**
   * Character lookahead stack. Reading a catalog sometimes requires
   * up to two characters of lookahead.
   */
  protected int[] stack = new int[3];

  /**
   * Token stack. Recognizing an unexpected catalog entry requires
   * the ability to "push back" a token.
   */
  protected Stack tokenStack = new Stack();

  /** The current position on the lookahead stack */
  protected int top = -1;

  /** Are keywords in the catalog case sensitive? */
  protected boolean caseSensitive = false;

  /**
   * Construct a CatalogReader object.
   */
  public TextCatalogReader() { }

  public void setCaseSensitive(boolean isCaseSensitive) {
    caseSensitive = isCaseSensitive;
  }

  public boolean getCaseSensitive() {
    return caseSensitive;
  }

  /**
   * Start parsing a text catalog file. The file is
   * actually read and parsed
   * as needed by <code>nextEntry</code>.</p>
   *
   * @param fileUrl  The URL or filename of the catalog file to process
   *
   * @throws MalformedURLException Improper fileUrl
   * @throws IOException Error reading catalog file
   */
  public void readCatalog(Catalog catalog, String fileUrl)
    throws MalformedURLException, IOException {
    URL catURL = null;

    try {
      catURL = new URL(fileUrl);
    } catch (MalformedURLException e) {
      catURL = new URL("file:///" + fileUrl);
    }

    URLConnection urlCon = catURL.openConnection();
    try {
      readCatalog(catalog, urlCon.getInputStream());
    } catch (FileNotFoundException e) {
      catalog.getCatalogManager().debug.message(1, "Failed to load catalog, file not found",
						catURL.toString());
    }
  }

  public void readCatalog(Catalog catalog, InputStream is)
    throws MalformedURLException, IOException {

    catfile = is;

    if (catfile == null) {
      return;
    }

    Vector unknownEntry = null;

    while (true) {
      String token = nextToken();

      if (token == null) {
	if (unknownEntry != null) {
	  catalog.unknownEntry(unknownEntry);
	  unknownEntry = null;
	}
	catfile.close();
	catfile = null;
	return;
      }

      String entryToken = null;
      if (caseSensitive) {
	entryToken = token;
      } else {
	entryToken = token.toUpperCase();
      }

      try {
	int type = CatalogEntry.getEntryType(entryToken);
	int numArgs = CatalogEntry.getEntryArgCount(type);
	Vector args = new Vector();

	if (unknownEntry != null) {
	  catalog.unknownEntry(unknownEntry);
	  unknownEntry = null;
	}

	for (int count = 0; count < numArgs; count++) {
	  args.addElement(nextToken());
	}

	catalog.addEntry(new CatalogEntry(entryToken, args));
      } catch (CatalogException cex) {
	if (cex.getExceptionType() == CatalogException.INVALID_ENTRY_TYPE) {
	  if (unknownEntry == null) {
	    unknownEntry = new Vector();
	  }
	  unknownEntry.addElement(token);
	} else if (cex.getExceptionType() == CatalogException.INVALID_ENTRY) {
	  catalog.getCatalogManager().debug.message(1, "Invalid catalog entry", token);
	  unknownEntry = null;
	}
      }
    }
  }

  /**
     * The destructor.
     *
     * <p>Makes sure the catalog file is closed.</p>
     */
  protected void finalize() {
    if (catfile != null) {
      try {
	catfile.close();
      } catch (IOException e) {
	// whatever...
      }
    }
    catfile = null;
  }

  // -----------------------------------------------------------------

    /**
     * Return the next token in the catalog file.
     *
     * @return The Catalog file token from the input stream.
     * @throws IOException If an error occurs reading from the stream.
     */
  protected String nextToken() throws IOException {
    String token = "";
    int ch, nextch;

    if (!tokenStack.empty()) {
      return (String) tokenStack.pop();
    }

    // Skip over leading whitespace and comments
    while (true) {
      // skip leading whitespace
      ch = catfile.read();
      while (ch <= ' ') {      // all ctrls are whitespace
	ch = catfile.read();
	if (ch < 0) {
	  return null;
	}
      }

      // now 'ch' is the current char from the file
      nextch = catfile.read();
      if (nextch < 0) {
	return null;
      }

      if (ch == '-' && nextch == '-') {
	// we've found a comment, skip it...
	ch = ' ';
	nextch = nextChar();
	while (ch != '-' || nextch != '-') {
	  ch = nextch;
	  nextch = nextChar();
	}

	// Ok, we've found the end of the comment,
	// loop back to the top and start again...
      } else {
	stack[++top] = nextch;
	stack[++top] = ch;
	break;
      }
    }

    ch = nextChar();
    if (ch == '"' || ch == '\'') {
      int quote = ch;
      while ((ch = nextChar()) != quote) {
	char[] chararr = new char[1];
	chararr[0] = (char) ch;
	String s = new String(chararr);
	token = token.concat(s);
      }
      return token;
    } else {
      // return the next whitespace or comment delimited
      // string
      while (ch > ' ') {
	nextch = nextChar();
	if (ch == '-' && nextch == '-') {
	  stack[++top] = ch;
	  stack[++top] = nextch;
	  return token;
	} else {
	  char[] chararr = new char[1];
	  chararr[0] = (char) ch;
	  String s = new String(chararr);
	  token = token.concat(s);
	  ch = nextch;
	}
      }
      return token;
    }
  }

  /**
     * Return the next logical character from the input stream.
     *
     * @return The next (logical) character from the input stream. The
     * character may be buffered from a previous lookahead.
     *
     * @throws IOException If an error occurs reading from the stream.
     */
  protected int nextChar() throws IOException {
    if (top < 0) {
      return catfile.read();
    } else {
      return stack[top--];
    }
  }
}

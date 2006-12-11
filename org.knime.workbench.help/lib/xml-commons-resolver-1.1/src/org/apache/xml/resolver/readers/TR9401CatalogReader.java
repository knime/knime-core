// TR9401CatalogReader.java - Read OASIS Catalog files

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
import java.net.MalformedURLException;
import java.util.Vector;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogEntry;
import org.apache.xml.resolver.CatalogException;

/**
 * Parses OASIS Open Catalog files.
 *
 * <p>This class reads OASIS Open Catalog files, returning a stream
 * of tokens.</p>
 *
 * <p>This code interrogates the following non-standard system properties:</p>
 *
 * <dl>
 * <dt><b>xml.catalog.debug</b></dt>
 * <dd><p>Sets the debug level. A value of 0 is assumed if the
 * property is not set or is not a number.</p></dd>
 * </dl>
 *
 * @see Catalog
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class TR9401CatalogReader extends TextCatalogReader {

  /**
   * Start parsing an OASIS TR9401 Open Catalog file. The file is
   * actually read and parsed
   * as needed by <code>nextEntry</code>.
   *
   * <p>In a TR9401 Catalog the 'DELEGATE' entry delegates public
   * identifiers. There is no delegate entry for system identifiers
   * or URIs.</p>
   *
   * @param fileUrl  The URL or filename of the catalog file to process
   *
   * @throws MalformedURLException Improper fileUrl
   * @throws IOException Error reading catalog file
   */
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

      if (entryToken.equals("DELEGATE")) {
	entryToken = "DELEGATE_PUBLIC";
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
}

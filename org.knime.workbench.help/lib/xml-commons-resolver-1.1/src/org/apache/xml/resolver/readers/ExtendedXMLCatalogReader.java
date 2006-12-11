// ExtendedXMLCatalogReader.java - Read XML Catalog files

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

import java.util.Vector;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.Resolver;
import org.apache.xml.resolver.CatalogEntry;
import org.apache.xml.resolver.CatalogException;

import org.xml.sax.*;
import org.w3c.dom.*;

/**
 * Parse Extended OASIS Entity Resolution Technical Committee 
 * XML Catalog files.
 *
 * @see Catalog
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class ExtendedXMLCatalogReader extends OASISXMLCatalogReader {
  /** The namespace name of extended catalog elements */
  public static final String extendedNamespaceName = "http://nwalsh.com/xcatalog/1.0";

  /**
   * The SAX <code>startElement</code> method recognizes elements
   * from the plain catalog format and instantiates CatalogEntry
   * objects for them.
   *
   * @param namespaceURI The namespace name of the element.
   * @param localName The local name of the element.
   * @param qName The QName of the element.
   * @param atts The list of attributes on the element.
   *
   * @see CatalogEntry
   */
  public void startElement (String namespaceURI,
			    String localName,
			    String qName,
			    Attributes atts)
    throws SAXException {

    // Check before calling the super because super will report our
    // namespace as an extension namespace, but that doesn't count
    // for this element.
    boolean inExtension = inExtensionNamespace();

    super.startElement(namespaceURI, localName, qName, atts);

    int entryType = -1;
    Vector entryArgs = new Vector();

    if (namespaceURI != null && extendedNamespaceName.equals(namespaceURI)
	&& !inExtension) {
      // This is an Extended XML Catalog entry

      if (atts.getValue("xml:base") != null) {
	String baseURI = atts.getValue("xml:base");
	entryType = Catalog.BASE;
	entryArgs.add(baseURI);
	baseURIStack.push(baseURI);

	debug.message(4, "xml:base", baseURI);

	try {
	  CatalogEntry ce = new CatalogEntry(entryType, entryArgs);
	  catalog.addEntry(ce);
	} catch (CatalogException cex) {
	  if (cex.getExceptionType() == CatalogException.INVALID_ENTRY_TYPE) {
	    debug.message(1, "Invalid catalog entry type", localName);
	  } else if (cex.getExceptionType() == CatalogException.INVALID_ENTRY) {
	    debug.message(1, "Invalid catalog entry (base)", localName);
	  }
	}

	entryType = -1;
	entryArgs = new Vector();
      } else {
	baseURIStack.push(baseURIStack.peek());
      }

      if (localName.equals("uriSuffix")) {
	if (checkAttributes(atts, "suffix", "uri")) {
	  entryType = Resolver.URISUFFIX;
	  entryArgs.add(atts.getValue("suffix"));
	  entryArgs.add(atts.getValue("uri"));

	  debug.message(4, "uriSuffix",
			atts.getValue("suffix"),
			atts.getValue("uri"));
	}
      } else if (localName.equals("systemSuffix")) {
	if (checkAttributes(atts, "suffix", "uri")) {
	  entryType = Resolver.SYSTEMSUFFIX;
	  entryArgs.add(atts.getValue("suffix"));
	  entryArgs.add(atts.getValue("uri"));

	  debug.message(4, "systemSuffix",
			atts.getValue("suffix"),
			atts.getValue("uri"));
	}
      } else {
	// This is equivalent to an invalid catalog entry type
	debug.message(1, "Invalid catalog entry type", localName);
      }

      if (entryType >= 0) {
	try {
	  CatalogEntry ce = new CatalogEntry(entryType, entryArgs);
	  catalog.addEntry(ce);
	} catch (CatalogException cex) {
	  if (cex.getExceptionType() == CatalogException.INVALID_ENTRY_TYPE) {
	    debug.message(1, "Invalid catalog entry type", localName);
	  } else if (cex.getExceptionType() == CatalogException.INVALID_ENTRY) {
	    debug.message(1, "Invalid catalog entry", localName);
	  }
	}
      }
    }
  }

  /** The SAX <code>endElement</code> method does nothing. */
  public void endElement (String namespaceURI,
			  String localName,
			  String qName)
    throws SAXException {

    super.endElement(namespaceURI, localName, qName);

    // Check after popping the stack so we don't erroneously think we
    // are our own extension namespace...
    boolean inExtension = inExtensionNamespace();

    int entryType = -1;
    Vector entryArgs = new Vector();

    if (namespaceURI != null
	&& (extendedNamespaceName.equals(namespaceURI))
	&& !inExtension) {

      String popURI = (String) baseURIStack.pop();
      String baseURI = (String) baseURIStack.peek();

      if (!baseURI.equals(popURI)) {
	entryType = catalog.BASE;
	entryArgs.add(baseURI);

	debug.message(4, "(reset) xml:base", baseURI);

	try {
	  CatalogEntry ce = new CatalogEntry(entryType, entryArgs);
	  catalog.addEntry(ce);
	} catch (CatalogException cex) {
	  if (cex.getExceptionType() == CatalogException.INVALID_ENTRY_TYPE) {
	    debug.message(1, "Invalid catalog entry type", localName);
	  } else if (cex.getExceptionType() == CatalogException.INVALID_ENTRY) {
	    debug.message(1, "Invalid catalog entry (rbase)", localName);
	  }
	}
      }
    }
  }
}

// XCatalogReader.java - Read XML Catalog files

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
import org.apache.xml.resolver.CatalogEntry;
import org.apache.xml.resolver.CatalogException;
import org.apache.xml.resolver.helpers.PublicId;

import org.xml.sax.*;

import javax.xml.parsers.*;

/**
 * Parse "xcatalog" XML Catalog files, this is the XML Catalog format
 * developed by John Cowan and supported by Apache.
 *
 * @see Catalog
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class XCatalogReader extends SAXCatalogReader implements SAXCatalogParser {
  /** The catalog object needs to be stored by the object so that
   * SAX callbacks can use it.
   */
  protected Catalog catalog = null;

  /** Set the current catalog. */
  public void setCatalog (Catalog catalog) {
    this.catalog = catalog;
  }

  /** Get the current catalog. */
  public Catalog getCatalog () {
    return catalog;
  }

  /** The constructor */
  public XCatalogReader(SAXParserFactory parserFactory) {
    super(parserFactory);
  }

  // ----------------------------------------------------------------------
  // Implement the SAX DocumentHandler interface

  /** The SAX <code>setDocumentLocator</code> method does nothing. */
  public void setDocumentLocator (Locator locator) {
    return;
  }

  /** The SAX <code>startDocument</code> method does nothing. */
  public void startDocument ()
    throws SAXException {
    return;
  }

  /** The SAX <code>endDocument</code> method does nothing. */
  public void endDocument ()
    throws SAXException {
    return;
  }

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

    int entryType = -1;
    Vector entryArgs = new Vector();

    if (localName.equals("Base")) {
      entryType = catalog.BASE;
      entryArgs.add(atts.getValue("HRef"));

      catalog.getCatalogManager().debug.message(4, "Base", atts.getValue("HRef"));
    } else if (localName.equals("Delegate")) {
      entryType = catalog.DELEGATE_PUBLIC;
      entryArgs.add(atts.getValue("PublicId"));
      entryArgs.add(atts.getValue("HRef"));

      catalog.getCatalogManager().debug.message(4, "Delegate",
		    PublicId.normalize(atts.getValue("PublicId")),
		    atts.getValue("HRef"));
    } else if (localName.equals("Extend")) {
      entryType = catalog.CATALOG;
      entryArgs.add(atts.getValue("HRef"));

      catalog.getCatalogManager().debug.message(4, "Extend", atts.getValue("HRef"));
    } else if (localName.equals("Map")) {
      entryType = catalog.PUBLIC;
      entryArgs.add(atts.getValue("PublicId"));
      entryArgs.add(atts.getValue("HRef"));

      catalog.getCatalogManager().debug.message(4, "Map",
		    PublicId.normalize(atts.getValue("PublicId")),
		    atts.getValue("HRef"));
    } else if (localName.equals("Remap")) {
      entryType = catalog.SYSTEM;
      entryArgs.add(atts.getValue("SystemId"));
      entryArgs.add(atts.getValue("HRef"));

      catalog.getCatalogManager().debug.message(4, "Remap",
		    atts.getValue("SystemId"),
		    atts.getValue("HRef"));
    } else if (localName.equals("XMLCatalog")) {
      // nop, start of catalog
    } else {
      // This is equivalent to an invalid catalog entry type
      catalog.getCatalogManager().debug.message(1, "Invalid catalog entry type", localName);
    }

    if (entryType >= 0) {
      try {
	CatalogEntry ce = new CatalogEntry(entryType, entryArgs);
	catalog.addEntry(ce);
      } catch (CatalogException cex) {
	if (cex.getExceptionType() == CatalogException.INVALID_ENTRY_TYPE) {
	  catalog.getCatalogManager().debug.message(1, "Invalid catalog entry type", localName);
	} else if (cex.getExceptionType() == CatalogException.INVALID_ENTRY) {
	  catalog.getCatalogManager().debug.message(1, "Invalid catalog entry", localName);
	}
      }
    }
    }

    /** The SAX <code>endElement</code> method does nothing. */
    public void endElement (String namespaceURI,
			    String localName,
			    String qName)
      throws SAXException {
      return;
    }

  /** The SAX <code>characters</code> method does nothing. */
  public void characters (char ch[], int start, int length)
    throws SAXException {
    return;
  }

  /** The SAX <code>ignorableWhitespace</code> method does nothing. */
  public void ignorableWhitespace (char ch[], int start, int length)
    throws SAXException {
    return;
  }

  /** The SAX <code>processingInstruction</code> method does nothing. */
  public void processingInstruction (String target, String data)
    throws SAXException {
    return;
  }
}

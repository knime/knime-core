// SAXParserHandler.java - An entity-resolving DefaultHandler

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

import java.io.IOException;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * An entity-resolving DefaultHandler.
 *
 * <p>This class provides a SAXParser DefaultHandler that performs
 * entity resolution.
 * </p>
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 * @version 1.0
 */
public class SAXParserHandler extends DefaultHandler {
  private EntityResolver er = null;
  private ContentHandler ch = null;

  public SAXParserHandler() {
    super();
  }

  public void setEntityResolver(EntityResolver er) {
    this.er = er;
  }

  public void setContentHandler(ContentHandler ch) {
    this.ch = ch;
  }

  // Entity Resolver
  public InputSource resolveEntity(String publicId, String systemId) 
    throws SAXException {

    if (er != null) {
      try {
	return er.resolveEntity(publicId, systemId);
      }	catch (IOException e) {
	  System.out.println("resolveEntity threw IOException!");
	  return null;
      }
    } else {
      return null;
    }
  }

  // Content Handler
  public void characters(char[] ch, int start, int length)
    throws SAXException {
    if (this.ch != null) {
      this.ch.characters(ch, start, length);
    }
  }

  public void endDocument()
    throws SAXException {
    if (ch != null) {
      ch.endDocument();
    }
  }

  public void endElement(String namespaceURI, String localName, String qName)
    throws SAXException {
    if (ch != null) {
      ch.endElement(namespaceURI, localName, qName);
    }
  }

  public void endPrefixMapping(String prefix)
    throws SAXException {
    if (ch != null) {
      ch.endPrefixMapping(prefix);
    }
  }

  public void ignorableWhitespace(char[] ch, int start, int length) 
    throws SAXException {
    if (this.ch != null) {
      this.ch.ignorableWhitespace(ch, start, length);
    }
  }

  public void processingInstruction(String target, String data) 
    throws SAXException {
    if (ch != null) {
      ch.processingInstruction(target, data);
    }
  }

  public void setDocumentLocator(Locator locator) {
    if (ch != null) {
      ch.setDocumentLocator(locator);
    }
  }

  public void skippedEntity(String name)
    throws SAXException {
    if (ch != null) {
      ch.skippedEntity(name);
    }
  }

  public void startDocument()
    throws SAXException {
    if (ch != null) {
      ch.startDocument();
    }
  }

  public void startElement(String namespaceURI, String localName,
			   String qName, Attributes atts)
    throws SAXException {
    if (ch != null) {
      ch.startElement(namespaceURI, localName, qName, atts);
    }
  }

  public void startPrefixMapping(String prefix, String uri)
    throws SAXException {
    if (ch != null) {
      ch.startPrefixMapping(prefix, uri);
    }
  }
}

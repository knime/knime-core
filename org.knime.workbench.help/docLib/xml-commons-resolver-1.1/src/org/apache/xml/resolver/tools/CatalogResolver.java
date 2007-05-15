// CatalogResolver.java - A SAX EntityResolver/JAXP URI Resolver

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

package org.apache.xml.resolver.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;

/**
 * A SAX EntityResolver/JAXP URIResolver that uses catalogs.
 *
 * <p>This class implements both a SAX EntityResolver and a JAXP URIResolver.
 * </p>
 *
 * <p>This resolver understands OASIS TR9401 catalogs, XCatalogs, and the
 * current working draft of the OASIS Entity Resolution Technical
 * Committee specification.</p>
 *
 * @see Catalog
 * @see org.xml.sax.EntityResolver
 * @see javax.xml.transform.URIResolver
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class CatalogResolver implements EntityResolver, URIResolver {
  /** Make the parser Namespace aware? */
  public boolean namespaceAware = true;

  /** Make the parser validating? */
  public boolean validating = false;

  /** The underlying catalog */
  private Catalog catalog = null;

  /** The catalog manager */
  private CatalogManager catalogManager = CatalogManager.getStaticManager();

  /** Constructor */
  public CatalogResolver() {
    initializeCatalogs(false);
  }

  /** Constructor */
  public CatalogResolver(boolean privateCatalog) {
    initializeCatalogs(privateCatalog);
  }

  /** Constructor */
  public CatalogResolver(CatalogManager manager) {
    catalogManager = manager;
    initializeCatalogs(!catalogManager.getUseStaticCatalog());
  }

  /** Initialize catalog */
  private void initializeCatalogs(boolean privateCatalog) {
    catalog = catalogManager.getCatalog();
  }

  /** Return the underlying catalog */
  public Catalog getCatalog() {
    return catalog;
  }

  /**
   * Implements the guts of the <code>resolveEntity</code> method
   * for the SAX interface.
   *
   * <p>Presented with an optional public identifier and a system
   * identifier, this function attempts to locate a mapping in the
   * catalogs.</p>
   *
   * <p>If such a mapping is found, it is returned.  If no mapping is
   * found, null is returned.</p>
   *
   * @param publicId  The public identifier for the entity in question.
   * This may be null.
   *
   * @param systemId  The system identifier for the entity in question.
   * XML requires a system identifier on all external entities, so this
   * value is always specified.
   *
   * @return The resolved identifier (a URI reference).
   */
  public String getResolvedEntity (String publicId, String systemId) {
    String resolved = null;

    if (catalog == null) {
      catalogManager.debug.message(1, "Catalog resolution attempted with null catalog; ignored");
      return null;
    }

    if (systemId != null) {
      try {
	resolved = catalog.resolveSystem(systemId);
      } catch (MalformedURLException me) {
	catalogManager.debug.message(1, "Malformed URL exception trying to resolve",
		      publicId);
	resolved = null;
      } catch (IOException ie) {
	catalogManager.debug.message(1, "I/O exception trying to resolve", publicId);
	resolved = null;
      }
    }

    if (resolved == null) {
      if (publicId != null) {
	try {
	  resolved = catalog.resolvePublic(publicId, systemId);
	} catch (MalformedURLException me) {
	  catalogManager.debug.message(1, "Malformed URL exception trying to resolve",
			publicId);
	} catch (IOException ie) {
	  catalogManager.debug.message(1, "I/O exception trying to resolve", publicId);
	}
      }

      if (resolved != null) {
	catalogManager.debug.message(2, "Resolved public", publicId, resolved);
      }
    } else {
      catalogManager.debug.message(2, "Resolved system", systemId, resolved);
    }

    return resolved;
  }

  /**
   * Implements the <code>resolveEntity</code> method
   * for the SAX interface.
   *
   * <p>Presented with an optional public identifier and a system
   * identifier, this function attempts to locate a mapping in the
   * catalogs.</p>
   *
   * <p>If such a mapping is found, the resolver attempts to open
   * the mapped value as an InputSource and return it. Exceptions are
   * ignored and null is returned if the mapped value cannot be opened
   * as an input source.</p>
   *
   * <p>If no mapping is found (or an error occurs attempting to open
   * the mapped value as an input source), null is returned and the system
   * will use the specified system identifier as if no entityResolver
   * was specified.</p>
   *
   * @param publicId  The public identifier for the entity in question.
   * This may be null.
   *
   * @param systemId  The system identifier for the entity in question.
   * XML requires a system identifier on all external entities, so this
   * value is always specified.
   *
   * @return An InputSource for the mapped identifier, or null.
   */
  public InputSource resolveEntity (String publicId, String systemId) {
    String resolved = getResolvedEntity(publicId, systemId);

    if (resolved != null) {
      try {
	InputSource iSource = new InputSource(resolved);
	iSource.setPublicId(publicId);

	// Ideally this method would not attempt to open the
	// InputStream, but there is a bug (in Xerces, at least)
	// that causes the parser to mistakenly open the wrong
	// system identifier if the returned InputSource does
	// not have a byteStream.
	//
	// It could be argued that we still shouldn't do this here,
	// but since the purpose of calling the entityResolver is
	// almost certainly to open the input stream, it seems to
	// do little harm.
	//
	URL url = new URL(resolved);
	InputStream iStream = url.openStream();
	iSource.setByteStream(iStream);

	return iSource;
      } catch (Exception e) {
	catalogManager.debug.message(1, "Failed to create InputSource", resolved);
	return null;
      }
    }

    return null;
  }

  /** JAXP URIResolver API */
  public Source resolve(String href, String base)
    throws TransformerException {

    String uri = href;
    String fragment = null;
    int hashPos = href.indexOf("#");
    if (hashPos >= 0) {
      uri = href.substring(0, hashPos);
      fragment = href.substring(hashPos+1);
    }

    String result = null;

    try {
      result = catalog.resolveURI(href);
    } catch (Exception e) {
      // nop;
    }

    if (result == null) {
      try {
	URL url = null;

	if (base==null) {
	  url = new URL(uri);
	  result = url.toString();
	} else {
	  URL baseURL = new URL(base);
	  url = (href.length()==0 ? baseURL : new URL(baseURL, uri));
	  result = url.toString();
	}
      } catch (java.net.MalformedURLException mue) {
	// try to make an absolute URI from the current base
	String absBase = makeAbsolute(base);
	if (!absBase.equals(base)) {
	  // don't bother if the absBase isn't different!
	  return resolve(href, absBase);
	} else {
	  throw new TransformerException("Malformed URL "
					 + href + "(base " + base + ")",
					 mue);
	}
      }
    }

    //    if (!href.equals(result)) {
      catalogManager.debug.message(2, "Resolved URI", href, result);
      //    }

    SAXSource source = new SAXSource();
    source.setInputSource(new InputSource(result));
    return source;
  }

  /** Attempt to construct an absolute URI */
  private String makeAbsolute(String uri) {
    if (uri == null) {
      uri = "";
    }

    try {
      URL url = new URL(uri);
      return url.toString();
    } catch (MalformedURLException mue) {
      String dir = System.getProperty("user.dir");
      String file = "";

      if (dir.endsWith("/")) {
	file = "file://" + dir + uri;
      } else {
	file = "file://" + dir + "/" + uri;
      }

      try {
	URL fileURL = new URL(file);
	return fileURL.toString();
      } catch (MalformedURLException mue2) {
	// bail
	return uri;
      }
    }
  }
}

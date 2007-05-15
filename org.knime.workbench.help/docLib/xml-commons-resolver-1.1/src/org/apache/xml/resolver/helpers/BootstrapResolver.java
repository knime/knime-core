// BootstrapResolver.java - Resolve entities and URIs internally

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

import java.util.Hashtable;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;

import javax.xml.transform.URIResolver;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.TransformerException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * A simple bootstrapping resolver.
 *
 * <p>This class is used as the entity resolver when reading XML Catalogs.
 * It searches for the OASIS XML Catalog DTD, Relax NG Grammar and W3C XML Schema
 * as resources (e.g., in the resolver jar file).</p>
 *
 * <p>If you have your own DTDs or schemas, you can extend this class and
 * set the BootstrapResolver in your CatalogManager.</p>
 *
 * @see org.apache.xml.resolver.CatalogManager
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class BootstrapResolver implements EntityResolver, URIResolver {
  /** URI of the W3C XML Schema for OASIS XML Catalog files. */
  public static final String xmlCatalogXSD = "http://www.oasis-open.org/committees/entity/release/1.0/catalog.xsd";

  /** URI of the RELAX NG Grammar for OASIS XML Catalog files. */
  public static final String xmlCatalogRNG = "http://www.oasis-open.org/committees/entity/release/1.0/catalog.rng";

  /** Public identifier for OASIS XML Catalog files. */
  public static final String xmlCatalogPubId = "-//OASIS//DTD XML Catalogs V1.0//EN";

  /** System identifier for OASIS XML Catalog files. */
  public static final String xmlCatalogSysId = "http://www.oasis-open.org/committees/entity/release/1.0/catalog.dtd";

  /** Private hash used for public identifiers. */
  private Hashtable publicMap = new Hashtable();

  /** Private hash used for system identifiers. */
  private Hashtable systemMap = new Hashtable();

  /** Private hash used for URIs. */
  private Hashtable uriMap = new Hashtable();

  /** Constructor. */
  public BootstrapResolver() {
    URL url = this.getClass().getResource("/org/apache/xml/resolver/etc/catalog.dtd");
    if (url != null) {
      publicMap.put(xmlCatalogPubId, url.toString());
      systemMap.put(xmlCatalogSysId, url.toString());
    }

    url = this.getClass().getResource("/org/apache/xml/resolver/etc/catalog.rng");
    if (url != null) {
      uriMap.put(xmlCatalogRNG, url.toString());
    }

    url = this.getClass().getResource("/org/apache/xml/resolver/etc/catalog.xsd");
    if (url != null) {
      uriMap.put(xmlCatalogXSD, url.toString());
    }
  }

  /** SAX resolveEntity API. */
  public InputSource resolveEntity (String publicId, String systemId) {
    String resolved = null;

    if (systemId != null && systemMap.containsKey(systemId)) {
      resolved = (String) systemMap.get(systemId);
    } else if (publicId != null && publicMap.containsKey(publicId)) {
      resolved = (String) publicMap.get(publicId);
    }

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
	// FIXME: silently fail?
	return null;
      }
    }

    return null;
  }

  /** Transformer resolve API. */
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
    if (href != null && uriMap.containsKey(href)) {
      result = (String) uriMap.get(href);
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

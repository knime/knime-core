// CatalogReader.java - An interface for reading catalog files

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
import java.net.MalformedURLException;
import org.apache.xml.resolver.CatalogException;

import java.io.InputStream;
import org.apache.xml.resolver.Catalog;

/**
 * The CatalogReader interface.
 *
 * <p>The Catalog class requires that classes implement this interface
 * in order to be used to read catalogs. Examples of CatalogReaders
 * include the TextCatalogReader, the SAXCatalogReader, and the
 * DOMCatalogReader.</p>
 *
 * @see Catalog
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public interface CatalogReader {
    /**
     * Read a catalog from a file.
     *
     * <p>This class reads a catalog from a URL.</p>
     *
     * @param catalog The catalog for which this reader is called.
     * @param fileUrl The URL of a document to be read.
     * @throws MalformedURLException if the specified URL cannot be
     * turned into a URL object.
     * @throws IOException if the URL cannot be read.
     * @throws UnknownCatalogFormatException if the catalog format is
     * not recognized.
     * @throws UnparseableCatalogException if the catalog cannot be parsed.
     * (For example, if it is supposed to be XML and isn't well-formed.)
     */
    public void readCatalog(Catalog catalog, String fileUrl)
      throws MalformedURLException, IOException, CatalogException;

    /**
     * Read a catalog from an input stream.
     *
     * <p>This class reads a catalog from an input stream.</p>
     *
     * @param catalog The catalog for which this reader is called.
     * @param is The input stream that is to be read.
     * @throws IOException if the URL cannot be read.
     * @throws UnknownCatalogFormatException if the catalog format is
     * not recognized.
     * @throws UnparseableCatalogException if the catalog cannot be parsed.
     * (For example, if it is supposed to be XML and isn't well-formed.)
     */
    public void readCatalog(Catalog catalog, InputStream is)
	throws IOException, CatalogException;
}

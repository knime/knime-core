/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */ 
package embedding.tools;

//Java
import java.io.IOException;
import java.util.Map;

//SAX
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.EntityResolver;

/**
 * This class can be used as base class for XMLReaders that generate SAX 
 * events from Java objects.
 */

public abstract class AbstractObjectReader implements XMLReader {

    private static final String NAMESPACES =
        "http://xml.org/sax/features/namespaces";
    private static final String NS_PREFIXES =
        "http://xml.org/sax/features/namespace-prefixes";
        
    private Map features = new java.util.HashMap();
    private ContentHandler orgHandler;
    
    /** Proxy for easy SAX event generation */
    protected EasyGenerationContentHandlerProxy handler;
    /** Error handler */
    protected ErrorHandler errorHandler;


    /**
     * Constructor for the AbstractObjectReader object
     */
    public AbstractObjectReader() {
        setFeature(NAMESPACES, false);
        setFeature(NS_PREFIXES, false);
    }
    
    /* ============ XMLReader interface ============ */

    /**
     * @see org.xml.sax.XMLReader#getContentHandler()
     */
    public ContentHandler getContentHandler() {
        return this.orgHandler;
    }

    /**
     * @see org.xml.sax.XMLReader#setContentHandler(ContentHandler)
     */
    public void setContentHandler(ContentHandler handler) {
        this.orgHandler = handler;
        this.handler = new EasyGenerationContentHandlerProxy(handler);
    }

    /**
     * @see org.xml.sax.XMLReader#getErrorHandler()
     */
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    /**
     * @see org.xml.sax.XMLReader#setErrorHandler(ErrorHandler)
     */
    public void setErrorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
    }

    /**
     * @see org.xml.sax.XMLReader#getDTDHandler()
     */
    public DTDHandler getDTDHandler() {
        return null;
    }

    /**
     * @see org.xml.sax.XMLReader#setDTDHandler(DTDHandler)
     */
    public void setDTDHandler(DTDHandler handler) {
    }

    /**
     * @see org.xml.sax.XMLReader#getEntityResolver()
     */
    public EntityResolver getEntityResolver() {
        return null;
    }

    /**
     * @see org.xml.sax.XMLReader#setEntityResolver(EntityResolver)
     */
    public void setEntityResolver(EntityResolver resolver) {
    }

    /**
     * @see org.xml.sax.XMLReader#getProperty(String)
     */
    public Object getProperty(java.lang.String name) {
        return null;
    }

    /**
     * @see org.xml.sax.XMLReader#setProperty(String, Object)
     */
    public void setProperty(java.lang.String name, java.lang.Object value) {
    }

    /**
     * @see org.xml.sax.XMLReader#getFeature(String)
     */
    public boolean getFeature(java.lang.String name) {
        return ((Boolean) features.get(name)).booleanValue();
    }

    /**
     * Returns true if the NAMESPACES feature is enabled.
     * @return boolean true if enabled
     */
    protected boolean isNamespaces() {
        return getFeature(NAMESPACES);
    }

    /**
     * Returns true if the MS_PREFIXES feature is enabled.
     * @return boolean true if enabled
     */
    protected boolean isNamespacePrefixes() {
        return getFeature(NS_PREFIXES);
    }

    /**
     * @see org.xml.sax.XMLReader#setFeature(String, boolean)
     */
    public void setFeature(java.lang.String name, boolean value) {
        this.features.put(name, new Boolean(value));
    }

    /**
     * @see org.xml.sax.XMLReader#parse(String)
     */
    public void parse(String systemId) throws IOException, SAXException {
        throw new SAXException(
            this.getClass().getName()
                + " cannot be used with system identifiers (URIs)");
    }

    /**
     * @see org.xml.sax.XMLReader#parse(InputSource)
     */
    public abstract void parse(InputSource input)
        throws IOException, SAXException;

}

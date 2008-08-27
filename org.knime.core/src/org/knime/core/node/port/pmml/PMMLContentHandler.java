/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 */
package org.knime.core.node.port.pmml;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class PMMLContentHandler extends DefaultHandler 
    implements ContentHandler {

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public abstract void characters(final char[] ch, 
            final int start, final int length) throws SAXException;

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public abstract void endDocument() throws SAXException;

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public abstract void endElement(final String uri, final String localName, 
            final String name)throws SAXException;
    

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public abstract void startElement(final String uri, final String localName, 
            final String name, final Attributes atts) throws SAXException;

    
    /* For convenience implement the following methods empty...*/
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void endPrefixMapping(final String prefix) throws SAXException {
        // TODO Auto-generated method stub
        
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void ignorableWhitespace(final char[] ch, final int start, 
            final int length) throws SAXException {
        // TODO Auto-generated method stub
        
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void processingInstruction(final String target, final String data)
            throws SAXException {
        // TODO Auto-generated method stub
        
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void setDocumentLocator(final Locator locator) {
        // TODO Auto-generated method stub
        
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void skippedEntity(final String name) throws SAXException {
        // TODO Auto-generated method stub
        
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void startDocument() throws SAXException {
        // TODO Auto-generated method stub
        
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void startPrefixMapping(final String prefix, final String uri)
            throws SAXException {
        // TODO Auto-generated method stub
        
    }
    
    

}

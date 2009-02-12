/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLMasterContentHandler extends PMMLContentHandler {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            "PMMLErrorHandler");
    
    private Map<String, PMMLContentHandler>m_registeredHandlers 
        = new HashMap<String, PMMLContentHandler>();
    
    private String m_version;
    
    /**
     * 
     * @return the version of the parsed PMML file
     */
    public String getVersion() {
        return m_version;
    }
    
    /**
     * Adds a default PMML content handler, that is able to extract the 
     * newcessary information from the PMML file for the referring model.
     * @param id id in order to retrieve the handler after parsing
     * @param defaultHandler handler that understands a specific model
     * @return false, if the id is already in use, true if the handler was 
     *  successfully registered
     */
    public boolean addContentHandler(final String id, 
            final PMMLContentHandler defaultHandler) {
        if (m_registeredHandlers.get(id) != null) {
            return false;
        }
        m_registeredHandlers.put(id, defaultHandler);
        return true;
    }
    
    /**
     * 
     * @param id the id under which the handler is registered
     * @return true if handler successfully removed, 
     *  false if it wasn't registered
     */
    public boolean removeContentHandler(final String id) {
        return m_registeredHandlers.remove(id) != null ? true : false;
    }
    
    /**
     * 
     * @param id id under which the handler is registered
     * @return the handler if it was found under this id, null otherwise 
     */
    public PMMLContentHandler getDefaultHandler(final String id) {
        return m_registeredHandlers.get(id);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, 
            final int length) throws SAXException {
        for (ContentHandler hdl : m_registeredHandlers.values()) {
            hdl.characters(ch, start, length);
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        for (ContentHandler hdl : m_registeredHandlers.values()) {
            hdl.endDocument();
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName, 
            final String name) throws SAXException {
        for (ContentHandler hdl : m_registeredHandlers.values()) {
            hdl.endElement(uri, localName, name);
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName, 
            final String name, final Attributes atts) throws SAXException {
        if ("PMML".equals(name)) {
            if (atts != null) {
                m_version = atts.getValue("version");
            }
        }
        for (ContentHandler hdl : m_registeredHandlers.values()) {
            hdl.startElement(uri, localName, name, atts);
        }        
    }
    
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void error(final SAXParseException exception) throws SAXException {
        LOGGER.error("Error during validation of PMML port object: ", 
                exception);
//        throw exception;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void fatalError(final SAXParseException exception)
            throws SAXException {
        LOGGER.fatal("Error during validation of PMML port object: ", 
                exception);
//        throw exception;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void warning(final SAXParseException exception)
            throws SAXException {
        LOGGER.warn("Error during validation of PMML port object: ", 
                exception);
//        throw exception;
    }


}

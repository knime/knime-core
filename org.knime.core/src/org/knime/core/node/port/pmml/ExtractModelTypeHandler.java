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
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ExtractModelTypeHandler extends PMMLContentHandler {
    /** Public ID .*/
    public static final String ID = "ExtractModel";
    
    private PMMLModelType m_type = null;
    
    private boolean m_hasNamespace = false;
    
    /**
     * 
     * @return the type of valid PMML models (v2.1)
     */
    public PMMLModelType getModelType() {
        return m_type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        // ignore -> we are only searching for the model type
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        if (m_type == null) {
            m_type = PMMLModelType.None;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName, 
            final String name) throws SAXException {
        for (PMMLModelType t : PMMLModelType.values()) {
            if (t.name().equals(name)) {
                m_type = t;
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String name, final Attributes atts) throws SAXException {
        // leave empty -> we are only searching for the model type
        if (name.equals("PMML")) {
            if (atts.getValue("xmlns") != null
                    && atts.getValue("xmlns").startsWith(
                            "http://www.dmg.org/PMML-3")) {
                m_hasNamespace = true;
            }
        }
    }
    
    /**
     * 
     * @return true if there is a PMML namespace declaration
     */
    public boolean hasNamespace() {
        return m_hasNamespace;
    }

}

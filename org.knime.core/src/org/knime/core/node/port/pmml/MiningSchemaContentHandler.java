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

import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class MiningSchemaContentHandler extends PMMLContentHandler {
    
    private Set<String>m_learningFields;
    private Set<String>m_ignoredFields;
    private Set<String>m_targetFields;
    
    /** ID of this handler. */
    public static final String ID = "MiningSchemaContentHandler";
    
    /**
     * 
     */
    public MiningSchemaContentHandler() {
        m_learningFields = new HashSet<String>();
        m_ignoredFields = new HashSet<String>();
        m_targetFields = new HashSet<String>();
    }
    
    /**
     * 
     * @return the names of the columns used for learning
     */
    public Set<String>getLearningFields() {
        return m_learningFields;
    }
    
    /**
     * 
     * @return the names of the ignored columns
     */
    public Set<String>getIgnoredFields() {
        return m_ignoredFields;
    }
    
    /**
     * 
     * @return the names of the target columns
     */
    public Set<String>getTargetFields() {
        return m_targetFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, 
            final int start, final int length)
            throws SAXException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, 
            final String localName, final String name)
            throws SAXException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, 
            final String localName, final String name,
            final Attributes atts) throws SAXException {
        if ("MiningField".equals(name)) {
            // get attributes
            String colName = atts.getValue("name");
            String usageType = atts.getValue("usageType");
            if ("active".equals(usageType)) {
                m_learningFields.add(colName);
            } else if ("supplementary".equals(usageType)) {
                m_ignoredFields.add(colName);
            } else if ("predicted".equals(usageType)) {
                m_targetFields.add(colName);
            }
        }
    }

}

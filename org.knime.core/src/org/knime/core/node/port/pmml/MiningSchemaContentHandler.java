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

import java.util.LinkedList;
import java.util.List;

import org.knime.core.node.NodeLogger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class MiningSchemaContentHandler extends PMMLContentHandler {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            MiningSchemaContentHandler.class); 
    
    private List<String>m_learningFields;
    private List<String>m_ignoredFields;
    private List<String>m_targetFields;
    
    /** ID of this handler. */
    public static final String ID = "MiningSchemaContentHandler";
    
    /**
     * 
     */
    public MiningSchemaContentHandler() {
        m_learningFields = new LinkedList<String>();
        m_ignoredFields = new LinkedList<String>();
        m_targetFields = new LinkedList<String>();
    }
    
    /**
     * 
     * @return the names of the columns used for learning
     */
    public List<String>getLearningFields() {
        return m_learningFields;
    }
    
    /**
     * 
     * @return the names of the ignored columns
     */
    public List<String>getIgnoredFields() {
        return m_ignoredFields;
    }
    
    /**
     * 
     * @return the names of the target columns
     */
    public List<String>getTargetFields() {
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
            // check for unsupported attributes:
            if (atts.getValue("missingValueReplacement") != null) {
                LOGGER.warn("\"missingValueReplacement\" is not supported and " 
                        + "will be ignored. Skipping it");
            }
            if (atts.getValue("missingValueTreatment") != null) {
                LOGGER.warn("\"missingValueTreatment\" is not supported and " 
                        + "will be ignored. Skipping it");
            }
            if (atts.getValue("outliers") != null) {
                LOGGER.warn("\"outliers\" is not supported and " 
                        + "will be ignored. Skipping it");
            }
            String colName = atts.getValue("name");
            String usageType = atts.getValue("usageType");
            if (usageType == null) {
                usageType = "active";
            }
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

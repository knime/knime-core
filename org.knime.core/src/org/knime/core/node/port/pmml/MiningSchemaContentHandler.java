/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
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

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DataDictionaryContentHandler extends PMMLContentHandler {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            DataDictionaryContentHandler.class);
    
    /** ID to identify registered handler. */ 
    public static final String ID = "DataDictionary";
    
    private String m_currentName;
    private DataType m_currentType;
    private double m_currentMin = Double.NaN;
    private double m_currentMax = Double.NaN;
    private List<String> m_currentValues;
    
    private List<DataColumnSpec>m_colSpecs;
    
    private DataTableSpec m_spec;
    
    private boolean m_read;
    
    private Stack<String>m_elemStack;
    
    /**
     * 
     */
    public DataDictionaryContentHandler() {
        m_elemStack = new Stack<String>();
        m_colSpecs = new ArrayList<DataColumnSpec>();
        m_currentValues = new ArrayList<String>();
        m_read = false;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName, 
            final String name, final Attributes atts) throws SAXException {
        if (name.equals("DataField")) {
            m_read = true;
            if (atts.getValue("usageType") == null 
                    || "active".equals(atts.getValue("usageType"))) {
                m_currentName = atts.getValue("name");
                handleOpType(name, atts);
                handleDataType(name, atts);
            }
        } else if (m_read && m_elemStack.peek().equals("DataField") 
                && name.equals("Interval")) {
            // retrieve left and right margin from attributes
            if (atts.getValue("leftMargin") != null) {
                m_currentMin = Double.parseDouble(atts.getValue("leftMargin"));
            }
            if (atts.getValue("rightMargin") != null) {
                m_currentMax = Double.parseDouble(atts.getValue("rightMargin"));
            }
        } else if (m_read && m_elemStack.peek().equals("DataField") 
                && name.equals("Value")) {
            m_currentValues.add(atts.getValue("value"));
        }
        m_elemStack.push(name);
    }
    
    private void handleOpType(final String name, final Attributes atts) 
        throws SAXException {
        String opType = atts.getValue("optype");
        if (opType == null) {
            throw new SAXException("Invalid PMML!" 
                    + " Attribute \"optype\" not found, " 
                    + "which is required since PMML v.2.0");
        } else if (opType.equals("categorical")) {
            m_currentType = StringCell.TYPE;
        } else if (opType.equals("ordinal")) {
            // TODO: is there a KNIME equivalent for ordinal type??? 
        } else if (opType.equals("continuous")) {
            m_currentType = DoubleCell.TYPE;
        }
    }
    

    private void handleDataType(final String name, final Attributes atts) {
        String typeName = atts.getValue("dataType");
        if (typeName == null) {
            typeName = "unknown";
        }
        if (typeName.equals("string")) {
            m_currentType = StringCell.TYPE;
        } else if (typeName.equals("integer") 
                || typeName.equals("boolean")) {
            m_currentType = IntCell.TYPE;
        } else if (typeName.equals("double") 
                || typeName.equals("float")) {
            m_currentType = DoubleCell.TYPE;
        }
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName, 
            final String name) throws SAXException {
        if (name.equals("DataField") && m_currentName != null 
                && m_currentType != null) {
            DataColumnSpecCreator creator = new DataColumnSpecCreator(
                    m_currentName, m_currentType);
            validateValues(m_currentType);
            DataColumnDomainCreator domainCreator 
                = new DataColumnDomainCreator();
            if (m_currentValues.size() > 0) {
                Set<DataCell>values = new HashSet<DataCell>();
                for (String s : m_currentValues) {
                    values.add(new StringCell(s));
                }
                domainCreator.setValues(values);
            } 
            if (m_currentMin != Double.NaN) {
                domainCreator.setLowerBound(new DoubleCell(m_currentMin));
            } 
            if (m_currentMax != Double.NaN) {
                domainCreator.setUpperBound(new DoubleCell(m_currentMax));
            }
            creator.setDomain(domainCreator.createDomain());
            m_colSpecs.add(creator.createSpec());
            // clean up
            resetDataFieldParsing();
        }
        m_elemStack.pop();
    }
    
    private void resetDataFieldParsing() {
        m_currentMax = Double.NaN;
        m_currentMin = Double.NaN;
        m_currentValues.clear();
        m_currentType = null;
        m_currentName = null;
        m_read = false;
    }
    
    private void validateValues(final DataType type) {
        if (type.isCompatible(DoubleValue.class)) {
            double min = Double.NEGATIVE_INFINITY;
            double max = Double.POSITIVE_INFINITY;
            Collections.sort(m_currentValues);
            try {
                min = Double.parseDouble(m_currentValues.get(0));
            } catch (Exception e) {
                // use default values for min and max
                /*
                LOGGER.warn(
                        "Error while parsing left margin of " 
                        + "Interval. Using negative infinity...");
                        */
            }
            try {
                max = Double.parseDouble(m_currentValues.get(
                        m_currentValues.size() - 1));
            } catch (Exception e) {
                // use default values for min and max
                /*
                LOGGER.warn(
                        "Error while parsing right margin of " 
                        + "Interval. Using positive infinity...");
                        */
            } 
            // TODO: check if neither left nor right margin is defined 
            m_currentMin = min;
            m_currentMax = max;
        }
    }

    /**
     * 
     * @return the loaded dta table spec
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        DataColumnSpec[] array = new DataColumnSpec[m_colSpecs.size()];
        array = m_colSpecs.toArray(array);
        m_spec = new DataTableSpec(array);

        m_colSpecs.clear();
        m_currentMax = Double.NaN;
        m_currentMax = Double.NaN;
        m_currentName = null;
        m_currentType = null;
        m_elemStack.clear();
        m_read = false;
        m_currentValues.clear();
    }

}

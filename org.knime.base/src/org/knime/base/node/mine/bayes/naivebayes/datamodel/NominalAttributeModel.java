/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 * History
 *    25.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.bayes.naivebayes.datamodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;


/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
class NominalAttributeModel extends AttributeModel {

    private static final int HTML_VIEW_SIZE = 1;
    
    /**
     * The unique type of this model used for saving/loading.
     */
    static final String MODEL_TYPE = "NominalModel";

    private static final String MAX_NO_OF_ATTRS = "maxNoOfAttrs";
    private static final String CLASS_VALUE_COUNTER = "noOfClasses";
    private static final String CLASS_VALUE_SECTION = "classValueData_";

    private final class NominalClassValue {

        private static final String CLASS_VALUE = "classValue";
        
        private static final String NO_OF_ROWS = "noOfRows";

        private static final String MISSING_VALUE_COUNTER = "MissingValCounter";
        
        private static final String ATTRIBUTE_VALUES = "attributeValues";

        private static final String ATTR_VAL_COUNTER = "attributeCounter";

        private static final String MISSING_VALUE_NAME = "MissingValue";


        private final String m_classValue;
        
        private int m_noOfRows = 0;
        
        private final  Map<String, ExtendedInteger> m_recsByAttrValue;
        
        private final ExtendedInteger m_missingValueRecs;
        
        
        /**Constructor for class NominalRowValue.NominalClassValue.
         * @param classValue the class value
         */
        NominalClassValue(final String classValue) {
            m_classValue = classValue;
            m_recsByAttrValue =  
                new HashMap<String, ExtendedInteger>(getMaxNoOfAttrVals());
            m_missingValueRecs = new ExtendedInteger();
        }

        /**Constructor for class NominalClassValue.
         * @param config the <code>Config</code> object to read from
         * @throws InvalidSettingsException if the settings are invalid
         */
        NominalClassValue(final Config config)  
            throws InvalidSettingsException {
            m_classValue = config.getString(CLASS_VALUE);
            m_noOfRows = config.getInt(NO_OF_ROWS);
            m_missingValueRecs = 
                new ExtendedInteger(config.getInt(MISSING_VALUE_COUNTER));
            final String[] attrVals = config.getStringArray(ATTRIBUTE_VALUES);
            final int[] recsCounter = config.getIntArray(ATTR_VAL_COUNTER);
            if (attrVals.length != recsCounter.length) {
                throw new InvalidSettingsException(
                        "Attribute and counter array must be of equal size");
            }
            m_recsByAttrValue = 
                new HashMap<String, ExtendedInteger>(attrVals.length);
            for (int i = 0, length = attrVals.length; i < length; i++) {
                m_recsByAttrValue.put(attrVals[i], 
                        new ExtendedInteger(recsCounter[i]));
            }
        }

        /**
         * @param config the config object to write to
         */
        void saveModel(final Config config) {
            config.addString(CLASS_VALUE, m_classValue);
            config.addInt(NO_OF_ROWS, m_noOfRows);
            config.addInt(MISSING_VALUE_COUNTER, m_missingValueRecs.intValue());
            final String[] attrVals = new String[m_recsByAttrValue.size()];
            final int[] recsCounter = new int[m_recsByAttrValue.size()];
            int i = 0;
            for (String classVal : m_recsByAttrValue.keySet()) {
                attrVals[i] = classVal;
                recsCounter[i] = m_recsByAttrValue.get(classVal).intValue();
                i++;
            }
            config.addStringArray(ATTRIBUTE_VALUES, attrVals);
            config.addIntArray(ATTR_VAL_COUNTER, recsCounter);
        }

        /**
         * @param attrVal the attribute value to add to this class
         */
        void addValue(final DataCell attrVal) {
            if (attrVal.isMissing()) {
                m_missingValueRecs.increment();
            } else {
                ExtendedInteger attrRowCounter = 
                    m_recsByAttrValue.get(attrVal.toString());
                if (attrRowCounter == null) {
                    attrRowCounter = new ExtendedInteger();
                    m_recsByAttrValue.put(attrVal.toString(), attrRowCounter);
                }
                attrRowCounter.increment();
            }
            m_noOfRows++;
        }

        /**
         * @return the classValue
         */
        String getClassValue() {
            return m_classValue;
        }
        
        /**
         * @return the total number of rows with this class
         */
        int getNoOfRows() {
            return m_noOfRows;
        }

        /**
         * @param attributeValue the attribute value we wan't the number of
         * rows for
         * @return the number of rows with this class and the given attribute
         */
        ExtendedInteger getNoOfRows4AttributeValue(
                final DataCell attributeValue) {
            if (attributeValue.isMissing()) {
                return m_missingValueRecs;
            }
            return m_recsByAttrValue.get(attributeValue.toString());
        }
        
        /**
         * @param attrVal the attribute value to calculate the probability
         * for
         * @return the probability for the given attribute value
         */
        double getProbability(final DataCell attrVal) {
            final int noOfRows4Class = getNoOfRows();
            if (noOfRows4Class == 0) {
                throw new IllegalStateException("Model for attribute "
                        + getAttributeName() + " contains no rows for class "
                        + m_classValue);
            }
            final ExtendedInteger noOfRows4Attr =
                getNoOfRows4AttributeValue(attrVal);
            if (noOfRows4Attr == null) {
                return 0;
            }
            return (double) noOfRows4Attr.intValue() / noOfRows4Class;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            buf.append(m_classValue);
            buf.append("\n");
            for (String attrVal : m_recsByAttrValue.keySet()) {
                final ExtendedInteger integer = m_recsByAttrValue.get(attrVal);
                buf.append(attrVal);
                buf.append("|");
                buf.append(integer.intValue());
                buf.append("\n");
            }
            return buf.toString();
        }

        /**
         * @return the HTML view of this class model
         */
        String getHTMLView() {
            final Map<String, ExtendedInteger> recsByAttrValue = 
                m_recsByAttrValue;
            if (m_missingValueRecs.intValue() > 0) {
                String missingLabel = MISSING_VALUE_NAME;
                int i = 1;
                while (m_recsByAttrValue.containsKey(missingLabel)) {
                    missingLabel = missingLabel + "(" + i++ + ")";
                }
                recsByAttrValue.put(MISSING_VALUE_NAME, m_missingValueRecs);
            }
            return AttributeModel.createHTMLTable(
                    m_classValue, "Attribute values: ", 
                    "Class counts: ", 10, recsByAttrValue, false);
        }
    }
    
    /**
     * The maximum number of unique attribute values.
     */
    private final int m_maxNoOfAttrVals;
    
    private final Map<String, NominalClassValue> m_classValues;
    
    private final Set<String> m_attributeVals;
    
    /**Constructor for class NominalRowValue.
     * @param attributeName the name of the attribute
     * @param skipMissingVals set to <code>true</code> if the missing values
     * should be skipped during learning and prediction
     * @param maxNoOfNominalVals the maximum number of nominal values
     */
    NominalAttributeModel(final String attributeName, 
            final boolean skipMissingVals, final int maxNoOfNominalVals) {
        super(attributeName, 0, skipMissingVals);
        m_maxNoOfAttrVals = maxNoOfNominalVals;
        m_classValues = 
            new HashMap<String, NominalClassValue>();
        m_attributeVals = new HashSet<String>(m_maxNoOfAttrVals);
    }
    
    
    
    /**Constructor for class NominalAttributeModel.
     * @param attributeName the name of this attribute
     * @param noOfMissingVals the number of missing values
     * @param skipMissingVals set to <code>true</code> if the missing values
     * should be skipped during learning and prediction
     * @param config the <code>Config</code> object to read from
     * @throws InvalidSettingsException if the settings are invalid
     */
    NominalAttributeModel(final String attributeName, 
            final int noOfMissingVals, final boolean skipMissingVals, 
            final Config config) 
        throws InvalidSettingsException {
        super(attributeName, noOfMissingVals, skipMissingVals);
        m_maxNoOfAttrVals = config.getInt(MAX_NO_OF_ATTRS);
        m_attributeVals = new HashSet<String>(m_maxNoOfAttrVals);
        final int noOfClassVals = config.getInt(CLASS_VALUE_COUNTER);
        m_classValues = new HashMap<String, NominalClassValue>(noOfClassVals);
        for (int i = 0; i < noOfClassVals; i++) {
            final Config classConfig = 
                config.getConfig(CLASS_VALUE_SECTION + i);
            final NominalClassValue classVal = 
                new NominalClassValue(classConfig);
            m_classValues.put(classVal.getClassValue(), classVal);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void saveModelInternal(final Config config) {
        config.addInt(MAX_NO_OF_ATTRS, m_maxNoOfAttrVals);
        config.addInt(CLASS_VALUE_COUNTER, m_classValues.size());
        int i = 0;
        for (NominalClassValue classVal : m_classValues.values()) {
            final Config classConfig = 
                config.addConfig(CLASS_VALUE_SECTION + i);
            classVal.saveModel(classConfig);
            i++;
        }
    }
    
    
    /**
     * @return the maximum supported number of unique attribute values
     */
    int getMaxNoOfAttrVals() {
        return m_maxNoOfAttrVals;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    Class<NominalValue> getCompatibleType() {
        return NominalValue.class;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    void addValueInternal(final String classValue, 
            final DataCell attrValue) throws TooManyValuesException {
        NominalClassValue classObject = m_classValues.get(classValue);
        if (classObject == null) {
            classObject = new NominalClassValue(classValue);
            m_classValues.put(classValue, classObject);
        }
        final String attrValString = attrValue.toString();
        if (!m_attributeVals.contains(attrValString)) {
            //check the different number of attribute values
            if (m_attributeVals.size() > m_maxNoOfAttrVals) {
                throw new TooManyValuesException("Attribute value " 
                            + attrValString + " doesn't fit into model");
            }
            m_attributeVals.add(attrValString);
        }
        classObject.addValue(attrValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void validate() throws InvalidSettingsException {
        if (m_attributeVals.size() == 0) {
            setInvalidCause(MODEL_CONTAINS_NO_RECORDS);
            throw new InvalidSettingsException("Model for attribute " 
                    + getAttributeName() + " contains no attribute values");
        }
        if (m_classValues.size() == 0) {
            setInvalidCause(MODEL_CONTAINS_NO_CLASS_VALUES);
            throw new InvalidSettingsException("Model for attribute " 
                    + getAttributeName() + " contains no class values");
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    Collection<String> getClassValues() {
        return m_classValues.keySet();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    Integer getNoOfRecs4ClassValue(final String classValue) {
        final NominalClassValue value = m_classValues.get(classValue);
        if (value == null) {
            return null;
        }
        return new Integer(value.getNoOfRows());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    double getProbabilityInternal(final String classValue, 
            final DataCell attributeValue) {
        final NominalClassValue classVal = m_classValues.get(classValue);
        if (classVal == null) {
            return 0;
        }
        return classVal.getProbability(attributeValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getHTMLViewHeadLine() {
        return "P(class=? | " + getAttributeName() + ")";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    String getType() {
        return MODEL_TYPE;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    String getHTMLView() {
        final List<String> sortedClassVal = 
            AttributeModel.sortCollection(m_classValues.keySet());
        if (sortedClassVal == null) {
            return "";
        }
        final int rowSize = Math.min(HTML_VIEW_SIZE, m_classValues.size());
        final StringBuilder buf = new StringBuilder();
        buf.append("<table border='0' width='100%'>");
        int counter = 0;
        boolean first = true;
        for (String classVal : sortedClassVal) {
            if (counter % rowSize == 0) {
                if (!first) {
                    buf.append("</tr>");
                } else {
                    first = false;
                }
                buf.append("<tr>");
            }
            buf.append("<td valign='top'>");
            buf.append(m_classValues.get(classVal).getHTMLView());
            buf.append("</td>");
            counter++;
        }
        while (counter % rowSize != 0) {
            buf.append("<td>");
            buf.append("&nbsp;");
            buf.append("</td>");
            counter++;
        }
        buf.append("</tr>");
        buf.append("</table>");
        return buf.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(getAttributeName());
        buf.append("\n");
        buf.append(getType());
        buf.append("\n");
        for (NominalClassValue classVal : m_classValues.values()) {
            buf.append(classVal.toString());
            buf.append("\n");
        }
        return buf.toString();
    }
}

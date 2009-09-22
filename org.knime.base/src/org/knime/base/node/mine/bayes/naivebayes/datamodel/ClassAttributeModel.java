/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * -------------------------------------------------------------------
 *
 * History
 *    25.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.mine.bayes.naivebayes.datamodel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.util.MutableInteger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * This {@link AttributeModel} implementation holds the class attribute
 * information like the number of rows per class value.
 * @author Tobias Koetter, University of Konstanz
 */
class ClassAttributeModel extends AttributeModel {
    /**
     * The unique type of this model used for saving/loading.
     */
    static final String MODEL_TYPE = "ClassModel";

    private static final String TOTAL_NO_OF_RECORDS = "totalNoOfRecords";

    private static final String MAX_NO_OF_CLASS_VALUES = "maxNoOfClassValues";

    private static final String CLASS_NAMES = "classValues";

    private static final String CLASS_RECS_COUNTER = "classRecordCounter";


    /**
     *Saves the number of rows per class attribute.
     *<dl>
     *  <dt>Key:</dt>
     *  <dd>The name of the class value as <code>String</code></dd>
     *  <dt>Value:</dt>
     *  <dd>The <code>ExtendedInteger</code> object for this class
     *  value</dd>
     *</dl>
     */
    private final Map<String, MutableInteger> m_recsCounterByClassVal;

    private final int m_maxNoOfClassVals;

    private int m_totalNoOfRecs = 0;


    /**Constructor for class ClassRowValue.
     * @param rowCaption the row caption
     * @param skipMissingVals set to <code>true</code> if the missing values
     * should be skipped during learning and prediction
     * @param maxNoOfClassVals the maximum supported number of class values
     */
    ClassAttributeModel(final String rowCaption,
            final boolean skipMissingVals, final int maxNoOfClassVals) {
        super(rowCaption, 0, skipMissingVals);
        m_maxNoOfClassVals = maxNoOfClassVals;
        m_recsCounterByClassVal =
            new HashMap<String, MutableInteger>(maxNoOfClassVals);
    }

    /**Constructor for class ClassModel.
     * @param attributeName the name of the attribute
     * @param noOfMissingVals the number of missing values
     * @param skipMissingVals set to <code>true</code> if the missing values
     * should be skipped during learning and prediction
     * @param config the <code>Config</code> object to read from
     * @throws InvalidSettingsException if the settings are invalid
     */
    ClassAttributeModel(final String attributeName,
            final int noOfMissingVals, final boolean skipMissingVals,
            final Config config)
        throws InvalidSettingsException {
        super(attributeName, noOfMissingVals, skipMissingVals);
        m_totalNoOfRecs = config.getInt(TOTAL_NO_OF_RECORDS);
        m_maxNoOfClassVals = config.getInt(MAX_NO_OF_CLASS_VALUES);
        final String[] classVals = config.getStringArray(CLASS_NAMES);
        final int[] recsCounter = config.getIntArray(CLASS_RECS_COUNTER);
        if (classVals.length != recsCounter.length) {
            throw new InvalidSettingsException(
                    "Class names and counter must be of equal size");
        }
        m_recsCounterByClassVal =
            new HashMap<String, MutableInteger>(classVals.length);
        for (int i = 0, length = classVals.length; i < length; i++) {
            m_recsCounterByClassVal.put(classVals[i],
                    new MutableInteger(recsCounter[i]));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void saveModelInternal(final Config config) {
        config.addInt(TOTAL_NO_OF_RECORDS, m_totalNoOfRecs);
        config.addInt(MAX_NO_OF_CLASS_VALUES, m_maxNoOfClassVals);
        final String[] classVals = new String[m_recsCounterByClassVal.size()];
        final int[] recsCounter = new int[m_recsCounterByClassVal.size()];
        int i = 0;
        for (final String classVal : m_recsCounterByClassVal.keySet()) {
            classVals[i] = classVal;
            recsCounter[i] = m_recsCounterByClassVal.get(classVal).intValue();
            i++;
        }
        config.addStringArray(CLASS_NAMES, classVals);
        config.addIntArray(CLASS_RECS_COUNTER, recsCounter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void addValueInternal(final String classValue,
            final DataCell attrValue) throws TooManyValuesException {
        if (attrValue.isMissing()) {
            throw new IllegalArgumentException(
                    "Missing value not allowed as class value");
        }
        MutableInteger classCounter = m_recsCounterByClassVal.get(classValue);
        if (classCounter == null) {
            if (m_recsCounterByClassVal.size() > m_maxNoOfClassVals) {
                throw new TooManyValuesException("Class value "
                        + classValue + " doesn't fit into model");
            }
            classCounter = new MutableInteger(0);
            m_recsCounterByClassVal.put(classValue, classCounter);
        }
        classCounter.inc();
        m_totalNoOfRecs++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void validate() throws InvalidSettingsException {
        if (m_totalNoOfRecs == 0) {
            setInvalidCause(MODEL_CONTAINS_NO_RECORDS);
            throw new InvalidSettingsException("Model for attribute "
                    + getAttributeName() + " contains no records");
        }
        if (m_recsCounterByClassVal.size() == 0) {
            setInvalidCause(MODEL_CONTAINS_NO_CLASS_VALUES);
            throw new InvalidSettingsException("Model for attribute "
                    + getAttributeName() + " contains no class values");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Class<? extends DataValue> getCompatibleType() {
        return NominalValue.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Collection<String> getClassValues() {
        return m_recsCounterByClassVal.keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Integer getNoOfRecs4ClassValue(final String classValue) {
        final MutableInteger noOfRecs =
            m_recsCounterByClassVal.get(classValue);
        if (noOfRecs == null) {
            return null;
        }
        return new Integer(noOfRecs.intValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    double getProbabilityInternal(final String classValue,
            final DataCell attributeValue, final double laplaceCorrector) {
        if (attributeValue.isMissing()) {
            throw new IllegalArgumentException(
                    "Missing value not allowed as class value");
        }
        if (m_totalNoOfRecs == 0) {
            throw new IllegalStateException("Model for attribute "
                    + getAttributeName() + " contains no records");
        }
        final MutableInteger noOfRecs =
            m_recsCounterByClassVal.get(classValue);
        if (noOfRecs == null) {
            throw new IllegalStateException(
                    "No record counter object found for attribute "
                    + getAttributeName() + " and class value "
                    + classValue);
        }
        return (double)noOfRecs.intValue() / m_totalNoOfRecs;
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
    String getHTMLViewHeadLine() {
        return "Class counts for " + getAttributeName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getHTMLView(final int totalNoOfRecs) {
        return AttributeModel.createHTMLTable(null, "Class: ",
                "Count: ", 10, m_recsCounterByClassVal, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("Attribute name: ");
        buf.append(getAttributeName());
        buf.append("\t");
        buf.append("No of records: ");
        buf.append(m_totalNoOfRecs);
        buf.append("\n");
        for (final String classVal : m_recsCounterByClassVal.keySet()) {
            final MutableInteger integer =
                m_recsCounterByClassVal.get(classVal);
            buf.append(classVal);
            buf.append("|");
            buf.append(integer.intValue());
            buf.append("\n");
        }
        return buf.toString();
    }

}

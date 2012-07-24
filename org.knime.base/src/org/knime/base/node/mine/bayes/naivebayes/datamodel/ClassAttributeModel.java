/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 * History
 *    25.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.mine.bayes.naivebayes.datamodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.util.MutableInteger;


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
        return noOfRecs.intValue();
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
        final StringBuilder buf = new StringBuilder();
        buf.append(AttributeModel.createHTMLTable(null, "Class: ",
                "Count: ", 10, m_recsCounterByClassVal, true));
        buf.append("<b>Total count: </b>" + totalNoOfRecs + "<br><br>");
        return buf.toString();
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

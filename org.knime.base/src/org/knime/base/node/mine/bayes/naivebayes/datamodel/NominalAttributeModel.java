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

import org.knime.core.data.DataCell;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.util.MutableInteger;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This {@link AttributeModel} implementation calculates the probability for
 * all nominal attributes.
 * @author Tobias Koetter, University of Konstanz
 */
class NominalAttributeModel extends AttributeModel {

//    private static final int HTML_VIEW_SIZE = 1;

    /**
     * The unique type of this model used for saving/loading.
     */
    static final String MODEL_TYPE = "NominalModel";

    private static final String MAX_NO_OF_ATTRS = "maxNoOfAttrs";
    private static final String CLASS_VALUE_COUNTER = "noOfClasses";
    private static final String ATTRIBUTE_VALUES = "attributeValues";
    private static final String CLASS_VALUE_SECTION = "classValueData_";


    private final class NominalClassValue {

        private static final String CLASS_VALUE = "classValue";

        private static final String NO_OF_ROWS = "noOfRows";

        private static final String MISSING_VALUE_COUNTER = "MissingValCounter";

        private static final String ATTRIBUTE_VALS = "attributeValues";

        private static final String ATTR_VAL_COUNTER = "attributeCounter";

        private final String m_classValue;

        private int m_noOfRows = 0;

        private final  Map<String, MutableInteger> m_recsByAttrValue;

        private final MutableInteger m_missingValueRecs;


        /**Constructor for class NominalRowValue.NominalClassValue.
         * @param classValue the class value
         */
        NominalClassValue(final String classValue) {
            m_classValue = classValue;
            m_recsByAttrValue =
                new HashMap<String, MutableInteger>(getMaxNoOfAttrVals());
            m_missingValueRecs = new MutableInteger(0);
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
                new MutableInteger(config.getInt(MISSING_VALUE_COUNTER));
            final String[] attrVals = config.getStringArray(ATTRIBUTE_VALS);
            final int[] recsCounter = config.getIntArray(ATTR_VAL_COUNTER);
            if (attrVals.length != recsCounter.length) {
                throw new InvalidSettingsException(
                        "Attribute and counter array must be of equal size");
            }
            m_recsByAttrValue =
                new HashMap<String, MutableInteger>(attrVals.length);
            for (int i = 0, length = attrVals.length; i < length; i++) {
                m_recsByAttrValue.put(attrVals[i],
                        new MutableInteger(recsCounter[i]));
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
            for (final String classVal : m_recsByAttrValue.keySet()) {
                attrVals[i] = classVal;
                recsCounter[i] = m_recsByAttrValue.get(classVal).intValue();
                i++;
            }
            config.addStringArray(ATTRIBUTE_VALS, attrVals);
            config.addIntArray(ATTR_VAL_COUNTER, recsCounter);
        }

        /**
         * @param attrVal the attribute value to add to this class
         */
        void addValue(final DataCell attrVal) {
            if (attrVal.isMissing()) {
                m_missingValueRecs.inc();
            } else {
                MutableInteger attrRowCounter =
                    m_recsByAttrValue.get(attrVal.toString());
                if (attrRowCounter == null) {
                    attrRowCounter = new MutableInteger(0);
                    m_recsByAttrValue.put(attrVal.toString(), attrRowCounter);
                }
                attrRowCounter.inc();
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
        MutableInteger getNoOfRows4AttributeValue(
                final DataCell attributeValue) {
            if (attributeValue.isMissing()) {
                return m_missingValueRecs;
            }
            return m_recsByAttrValue.get(attributeValue.toString());
        }

        /**
         * @param attributeValue the attribute value we wan't the number of
         * rows for
         * @return the number of rows with this class and the given attribute
         */
        MutableInteger getNoOfRows4AttributeValue(
                final String attributeValue) {
            return m_recsByAttrValue.get(attributeValue);
        }

        /**
         * @param attrVal the attribute value to calculate the probability
         * for
         * @param laplaceCorrector the Laplace corrector to use.
         * A value greater 0 overcomes zero counts.
         * @return the probability for the given attribute value
         */
        double getProbability(final DataCell attrVal,
                final double laplaceCorrector) {
            final int noOfRows4Class = getNoOfRows();
            if (noOfRows4Class == 0) {
                throw new IllegalStateException("Model for attribute "
                        + getAttributeName() + " contains no rows for class "
                        + m_classValue);
            }
            double noOfRows = laplaceCorrector;
            final MutableInteger noOfRows4Attr =
                getNoOfRows4AttributeValue(attrVal);
            if (noOfRows4Attr != null) {
                noOfRows += noOfRows4Attr.intValue();
            }
            return noOfRows / (noOfRows4Class
                    + m_attributeVals.size() * laplaceCorrector);
        }
        /**
         * @return the missingValueRecs
         */
        MutableInteger getMissingValueRecs() {
            return m_missingValueRecs;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            buf.append(m_classValue);
            buf.append("\n");
            for (final String attrVal : m_recsByAttrValue.keySet()) {
                final MutableInteger integer = m_recsByAttrValue.get(attrVal);
                buf.append(attrVal);
                buf.append("|");
                buf.append(integer.intValue());
                buf.append("\n");
            }
            return buf.toString();
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
        final int noOfClassVals = config.getInt(CLASS_VALUE_COUNTER);
        m_attributeVals = new HashSet<String>(m_maxNoOfAttrVals);
        final String[] attrVals = config.getStringArray(ATTRIBUTE_VALUES);
        m_attributeVals.addAll(Arrays.asList(attrVals));
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
        final String[] attrVals = m_attributeVals.toArray(new String[] {});
        config.addStringArray(ATTRIBUTE_VALUES, attrVals);
        int i = 0;
        for (final NominalClassValue classVal : m_classValues.values()) {
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
        if (!attrValue.isMissing()) {
            final String attrValString = attrValue.toString();
            if (!m_attributeVals.contains(attrValString)) {
                //check the different number of attribute values
                if (m_attributeVals.size() >= m_maxNoOfAttrVals) {
                    throw new TooManyValuesException("Attribute value "
                                + attrValString + " doesn't fit into model");
                }
                m_attributeVals.add(attrValString);
            }
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
            final DataCell attributeValue, final double laplaceCorrector) {
        final NominalClassValue classVal = m_classValues.get(classValue);
        if (classVal == null) {
            return 0;
        }
        return classVal.getProbability(attributeValue, laplaceCorrector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getHTMLViewHeadLine() {
        return getAttributeName()
        + ": Number of occurences per attribute and class value";
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
    String getHTMLView(final int totalNoOfRecs) {
        final List<String> sortedClassVal =
            AttributeModel.sortCollection(m_classValues.keySet());
        if (sortedClassVal == null) {
            return "";
        }
        final List<String> sortedAttrValues =
            AttributeModel.sortCollection(m_attributeVals);
        final String classHeading = "Class/" + getAttributeName();
        final String missingHeading = getMissingValueHeader(m_attributeVals);
        final int arraySize;
        if (missingHeading != null) {
            arraySize = sortedAttrValues.size() + 1;
        } else {
            arraySize = sortedAttrValues.size();
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("<table border='1' width='100%'>");
        buf.append(createTableHeader(classHeading , sortedAttrValues,
                missingHeading));
        final int[] rowsPerValCounts = new int[arraySize];
        Arrays.fill(rowsPerValCounts, 0);
        //create the value section
        for (final String classVal : sortedClassVal) {
            final NominalClassValue classValue = m_classValues.get(classVal);
            buf.append("<tr>");
            buf.append("<th>");
            buf.append(classVal);
            buf.append("</th>");
            for (int i = 0, length = sortedAttrValues.size(); i < length; i++) {
                final String attrVal = sortedAttrValues.get(i);
                final MutableInteger rowCounter =
                    classValue.getNoOfRows4AttributeValue(attrVal);
                final int rowCount;
                if (rowCounter != null) {
                    rowCount = rowCounter.intValue();
                } else {
                    rowCount = 0;
                }
                rowsPerValCounts[i] += rowCount;
                buf.append("<td align='center'>");
                buf.append(rowCount);
                buf.append("</td>");
            }
            if (missingHeading != null) {
                final MutableInteger rowCounter =
                    classValue.getMissingValueRecs();
                rowsPerValCounts[arraySize - 1] += rowCounter.intValue();
                buf.append("<td align='center'>");
                buf.append(rowCounter);
                buf.append("</td>");
            }
            buf.append("</tr>");
        }
        //create the value summary section
        buf.append(createSummarySection(totalNoOfRecs, rowsPerValCounts));
        buf.append("</table>");
        return buf.toString();
    }

    private static String createSummarySection(final int totalRowCount,
            final int[] rowsPerValCounts) {
        final NumberFormat nf = NumberFormat.getPercentInstance();
        final StringBuilder buf = new StringBuilder();
        buf.append("<tr>");
        buf.append("<th>");
        buf.append("Rate:");
        buf.append("</th>");
        for (final int rowsPerValCount : rowsPerValCounts) {
            buf.append("<td align='center'>");
            buf.append(nf.format(rowsPerValCount / (double)totalRowCount));
            buf.append("</td>");
        }
        buf.append("</tr>");
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
        for (final NominalClassValue classVal : m_classValues.values()) {
            buf.append(classVal.toString());
            buf.append("\n");
        }
        return buf.toString();
    }
}

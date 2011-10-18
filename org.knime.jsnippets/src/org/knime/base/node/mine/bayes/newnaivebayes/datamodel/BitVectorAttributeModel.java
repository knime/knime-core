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
 */

package org.knime.base.node.mine.bayes.newnaivebayes.datamodel;

import org.knime.core.data.DataCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.util.MutableInteger;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * {@link AttributeModel} implementation that can handle
 * {@link BitVectorValue}s.
 * @author Tobias Koetter, University of Konstanz
 */
public class BitVectorAttributeModel extends AttributeModel {
    /**
     * The unique type of this model used for saving/loading.
     */
    static final String MODEL_TYPE = "BitVectorModel";

    private static final String CLASS_VALUE_COUNTER = "noOfClasses";

    private static final String CLASS_VALUE_SECTION = "classValueData_";

    private final class BitVectorClassValue {

        private static final String CLASS_VALUE = "classValue";

        private static final String MISSING_VALUE_COUNTER = "MissingValCounter";

        private static final String NO_OF_ROWS = "noOfRows";

        private static final String BIT_COUNTS = "bitCounts";

        private final String m_classValue;

        private int m_noOfRows = 0;

        private final MutableInteger m_missingValueRecs;

        private int[] m_bitCounts = null;


        /**Constructor for class BitVectorClassValue.
         * @param classValue
         */
        BitVectorClassValue(final String classValue) {
            m_classValue = classValue;
            m_missingValueRecs = new MutableInteger(0);
        }

        /**Constructor for class NumericalClassValue.
         * @param config the <code>Config</code> object to read from
         * @throws InvalidSettingsException if the settings are invalid
         */
        BitVectorClassValue(final Config config)
            throws InvalidSettingsException {
            m_classValue = config.getString(CLASS_VALUE);
            m_missingValueRecs =
                new MutableInteger(config.getInt(MISSING_VALUE_COUNTER));
            m_noOfRows = config.getInt(NO_OF_ROWS);
            m_bitCounts = config.getIntArray(BIT_COUNTS);
        }

        /**
         * @param config the <code>Config</code> object to write to
         */
        void saveModel(final Config config) {
            config.addString(CLASS_VALUE, m_classValue);
            config.addInt(MISSING_VALUE_COUNTER, m_missingValueRecs.intValue());
            config.addInt(NO_OF_ROWS, m_noOfRows);
            config.addIntArray(BIT_COUNTS, m_bitCounts);
        }

        /**
         * Called after all training rows where added to validate the model.
         * @throws InvalidSettingsException if the model isn't valid
         */
        void validate() throws InvalidSettingsException {
            if (m_noOfRows == 0) {
                setInvalidCause(MODEL_CONTAINS_NO_RECORDS);
                throw new InvalidSettingsException("Model for attribute "
                        + getAttributeName() + " contains no rows.");
            }
        }

        /**
         * @return the classValue
         */
        String getClassValue() {
            return m_classValue;
        }

        /**
         * @return the length of the {@link BitVectorClassValue}
         */
        int getVectorLength() {
            return m_bitCounts.length;
        }

        /**
         * @return the number of rows
         */
        int getNoOfRows() {
            return m_noOfRows;
        }

        /**
         * @param attrValue
         */
        void addValue(final DataCell attrValue) {
            if (attrValue.isMissing()) {
                m_missingValueRecs.inc();
            }
            final BitVectorValue bitVec = (BitVectorValue)attrValue;
            if (m_bitCounts == null) {
                if (bitVec.length() > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException(
                        "BitVector attribute does not support long vectors");
                }
                m_bitCounts  = new int[(int)bitVec.length()];
                Arrays.fill(m_bitCounts, 0);
            } else if (bitVec.length() != m_bitCounts.length) {
                throw new IllegalArgumentException("Illegal bit vector length");
            }
            for (int i = 0; i < m_bitCounts.length; i++) {
                if (bitVec.get(i)) {
                    m_bitCounts[i] += 1;
                }
            }
            m_noOfRows++;
        }

        /**
         * @param attributeValue the attribute value to calculate the
         * probability for
         * @param laplaceCorrector the Laplace corrector to use.
         * A value greater 0 overcomes zero counts.
         * @param useLog <code>true</code> if log sum should be used to
         * combine probabilities
         * @return the probability for the given attribute value
         */
        double getProbability(final DataCell attributeValue,
                final double laplaceCorrector, final boolean useLog) {
            final int noOfRows4Class = getNoOfRows();
            if (noOfRows4Class == 0) {
                throw new IllegalStateException("Model for attribute "
                        + getAttributeName() + " contains no rows for class "
                        + m_classValue);
            }
            final BitVectorValue bitVec = (BitVectorValue)attributeValue;
            if (bitVec.length() != m_bitCounts.length) {
                throw new IllegalArgumentException("Illegal bit vector length");
            }
            double combinedProbability = 1;
            if (useLog) {
                combinedProbability = Math.log(combinedProbability);
            }
            for (int i = 0, length = (int)bitVec.length(); i < length; i++) {
                final double noOfRows = laplaceCorrector
                        + getNoOfRows4AttributeValue(i, bitVec.get(i));
                //we use 2 * laplaceCorrector since the attribute can have only
                //two different values since it is a bit vector
                final double probability = noOfRows
                                    / (noOfRows4Class + 2 * laplaceCorrector);
                if (useLog) {
                    combinedProbability += Math.log(probability);
                } else {
                    combinedProbability *= probability;
                }
            }
            if (useLog) {
                combinedProbability = Math.exp(combinedProbability);
            }
            return combinedProbability;
        }

        /**
         * @param i bit index
         * @param set set or not set
         * @return
         */
        private int getNoOfRows4AttributeValue(final int i,
                final boolean set) {
            if (set) {
                return m_bitCounts[i];
            }
            //return zero count which is the total number of counts minus the
            //count of the ones
            return m_noOfRows - m_bitCounts[i];
        }

        /**
         * @return the missingValueRecs
         */
        MutableInteger getMissingValueRecs() {
            return m_missingValueRecs;
        }

        /**
         * @param idx the index position in the {@link BitVectorClassValue}
         * @return the number of bits that are set for this class value at the
         * specified position
         */
        public int getBitCount(final int idx) {
            return m_bitCounts[idx];
        }


        /**
         * @param idx the index position in the {@link BitVectorClassValue}
         * @return the number of zeros that exist for this class value at the
         * specified position
         */
        public int getZeroCount(final int idx) {
            return getNoOfRows() - m_bitCounts[idx];
        }
    }

    private final Map<String, BitVectorClassValue> m_classValues;

    /**Constructor for class BitVectorAttributeModel.
     * @param attributeName the name of the attribute
     * @param skipMissingVals set to <code>true</code> if the missing values
     * should be skipped during learning and prediction
     */
    BitVectorAttributeModel(final String attributeName,
            final boolean skipMissingVals) {
        super(attributeName, 0, skipMissingVals);
        m_classValues = new HashMap<String, BitVectorClassValue>();
    }


    /**Constructor for class BitVectorAttributeModel.
     * @param attributeName the name of the attribute
     * @param skipMissingVals set to <code>true</code> if the missing values
     * should be skipped during learning and prediction
     * @param noOfMissingVals the number of missing values
     * @param config the <code>Config</code> object to read from
     * @throws InvalidSettingsException if the settings are invalid
     *
     */
    public BitVectorAttributeModel(final String attributeName,
            final boolean skipMissingVals, final int noOfMissingVals,
            final Config config) throws InvalidSettingsException {
        super(attributeName, noOfMissingVals, skipMissingVals);
        final int noOfClasses = config.getInt(CLASS_VALUE_COUNTER);
        m_classValues = new HashMap<String, BitVectorClassValue>(noOfClasses);
        final int noOfClassVals = config.getInt(CLASS_VALUE_COUNTER);
        for (int i = 0; i < noOfClassVals; i++) {
            final Config classConfig =
                config.getConfig(CLASS_VALUE_SECTION + i);
            final BitVectorClassValue classVal =
                new BitVectorClassValue(classConfig);
            m_classValues.put(classVal.getClassValue(), classVal);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void saveModelInternal(final Config config) {
        config.addInt(CLASS_VALUE_COUNTER, m_classValues.size());
        int i = 0;
        for (final BitVectorClassValue classVal : m_classValues.values()) {
            final Config classConfig =
                config.addConfig(CLASS_VALUE_SECTION + i);
            classVal.saveModel(classConfig);
            i++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void validate() throws InvalidSettingsException {
        if (m_classValues.size() == 0) {
            setInvalidCause(MODEL_CONTAINS_NO_CLASS_VALUES);
            throw new InvalidSettingsException("Model for attribute "
                    + getAttributeName() + " contains no class values");
        }
        final Collection<BitVectorClassValue> classVals =
            m_classValues.values();
        for (final BitVectorClassValue value : classVals) {
            value.validate();
        }
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
    Class<BitVectorValue> getCompatibleType() {
        return BitVectorValue.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void addValueInternal(final String classValue, final DataCell attrValue) {
        BitVectorClassValue classObject = m_classValues.get(classValue);
        if (classObject == null) {
            classObject = new BitVectorClassValue(classValue);
            m_classValues.put(classValue, classObject);
        }
        classObject.addValue(attrValue);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    Collection<String> getClassValues() {
        return m_classValues.keySet();
    }

    /**
     * @return the length of the bit vector
     */
    int getVectorLength() {
        if (m_classValues.isEmpty()) {
            return 0;
        }
        return m_classValues.values().iterator().next().getVectorLength();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    Integer getNoOfRecs4ClassValue(final String classValue) {
        final BitVectorClassValue value = m_classValues.get(classValue);
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
            final DataCell attributeValue, final double laplaceCorrector,
            final boolean useLog) {
        final BitVectorClassValue classModel = m_classValues.get(classValue);
        if (classModel == null) {
            return 0;
        }
        return classModel.getProbability(attributeValue, laplaceCorrector,
                useLog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getHTMLViewHeadLine() {
        return "P(" + getAttributeName() + " | class=?)";
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
        final int bitVectorLength = getVectorLength();
        final String classHeading = "Class/" + getAttributeName();
        final LinkedList<String> columnNames = new LinkedList<String>();
        columnNames.add(getAttributeName());
        final String missingHeading = getMissingValueHeader(columnNames);
        final StringBuilder buf = new StringBuilder();
        buf.append("<table border='1' width='100%'>");
        //first table header section
        buf.append("<tr>");
        buf.append("<th rowspan=2>");
        buf.append(classHeading);
        buf.append("</th>");
//first table header row
        for (int i = 0, length = getVectorLength(); i < length; i++) {
            buf.append("<th colspan=2>");
            buf.append("Pos" + i);
            buf.append("</th>");
        }
        if (missingHeading != null) {
            buf.append("<th rowspan = 2>");
            buf.append(missingHeading);
            buf.append("</th>");
        }
        buf.append("</tr>");
//second table header row
        buf.append("<tr>");
        //create the first header row
        for (int i = 0; i < bitVectorLength; i++) {
            buf.append("<th>");
            buf.append("0");
            buf.append("</th>");
            buf.append("<th>");
            buf.append("1");
            buf.append("</th>");
        }
        buf.append("</tr>");
//the value rows
        int totalMissingValCounter = 0;
        final int[][] totalValCounter = new int[2][bitVectorLength];
        Arrays.fill(totalValCounter[0], 0);
        Arrays.fill(totalValCounter[1], 0);
        for (final String classVal : sortedClassVal) {
            final BitVectorClassValue classValue = m_classValues.get(classVal);
            buf.append("<tr>");
            buf.append("<th>");
            buf.append(classVal);
            buf.append("</th>");
            for (int i = 0; i < bitVectorLength; i++) {
                //the bit vector value section
                buf.append("<td align='center'>");
                final int zeroCount = classValue.getZeroCount(i);
                totalValCounter[0][i] += zeroCount;
                buf.append(zeroCount);
                buf.append("</td>");
                buf.append("<td align='center'>");
                final int bitCount = classValue.getBitCount(i);
                totalValCounter[1][i] += bitCount;
                buf.append(bitCount);
                buf.append("</td>");
                //no of missing values for this class value
                if (missingHeading != null) {
                    final MutableInteger missingRowCounter =
                        classValue.getMissingValueRecs();
                    totalMissingValCounter += missingRowCounter.intValue();
                    buf.append("<td align='center'>");
                    buf.append(missingRowCounter);
                    buf.append("</td>");
                }
            }
            buf.append("</tr>");
        }
//the rate row
        final NumberFormat nf = NumberFormat.getPercentInstance();
        buf.append("<tr>");
        buf.append("<th>");
        buf.append("Rate:");
        buf.append("</th>");
        for (int i = 0; i < bitVectorLength; i++) {
            //the bit vector value section
            buf.append("<td align='center'>");
            buf.append(
                    nf.format(totalValCounter[0][i] / (double) totalNoOfRecs));
            buf.append("</td>");
            buf.append("<td align='center'>");
            buf.append(
                    nf.format(totalValCounter[1][i] / (double) totalNoOfRecs));
            buf.append("</td>");
        }
        //no of missing values for this class value
        if (missingHeading != null) {
            buf.append("<td align='center'>");
            buf.append(
                    nf.format(totalMissingValCounter / (double) totalNoOfRecs));
            buf.append("</td>");
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
        for (final BitVectorClassValue classVal : m_classValues.values()) {
            buf.append(classVal.toString());
            buf.append("\n");
        }
        return buf.toString();
    }
}

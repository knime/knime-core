/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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

package org.knime.base.node.mine.bayes.naivebayes.datamodel2;

import java.io.IOException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringEscapeUtils;
import org.dmg.pmml.BayesInputDocument.BayesInput;
import org.dmg.pmml.BayesInputsDocument.BayesInputs;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.util.MutableInteger;


/**
 * {@link AttributeModel} implementation that can handle {@link BitVectorValue}s.
 * @author Tobias Koetter, University of Konstanz
 */
public class BitVectorAttributeModel extends AttributeModel {

    /**
     * The unique type of this model used for saving/loading.
     */
    static final String MODEL_TYPE = "BitVectorModel";

    private static final String CLASS_VALUE_COUNTER = "noOfClasses";

    private static final String CLASS_VALUE_SECTION = "classValueData_";

    private final class BitVectorClassValue implements Serializable {

        private static final long serialVersionUID = 1L;

        private static final String CLASS_VALUE = "classValue";

        private static final String MISSING_VALUE_COUNTER = "MissingValCounter";

        private static final String NO_OF_ROWS = "noOfRows";

        private static final String BIT_COUNTS = "bitCounts";

        private String m_classValue;

        private int m_noOfRows = 0;

        private MutableInteger m_missingValueRecs = new MutableInteger(0);

        private int[] m_bitCounts = null;

        /**Constructor for class BitVectorClassValue.
         * @param classValue
         */
        private BitVectorClassValue(final String classValue) {
            m_classValue = classValue;
        }

        /**Constructor for class NumericalClassValue.
         * @param config the <code>Config</code> object to read from
         * @throws InvalidSettingsException if the settings are invalid
         */
        private BitVectorClassValue(final Config config)
            throws InvalidSettingsException {
            m_classValue = config.getString(CLASS_VALUE);
            m_missingValueRecs.setValue(config.getInt(MISSING_VALUE_COUNTER));
            m_noOfRows = config.getInt(NO_OF_ROWS);
            m_bitCounts = config.getIntArray(BIT_COUNTS);
        }

        /**
         * @param config the <code>Config</code> object to write to
         */
        private void saveModel(final Config config) {
            config.addString(CLASS_VALUE, m_classValue);
            config.addInt(MISSING_VALUE_COUNTER, m_missingValueRecs.intValue());
            config.addInt(NO_OF_ROWS, m_noOfRows);
            config.addIntArray(BIT_COUNTS, m_bitCounts);
        }

        private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
            out.writeObject(m_classValue);
            out.writeObject(m_missingValueRecs);
            out.writeInt(m_noOfRows);
            out.writeObject(m_bitCounts);
        }

        private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            m_classValue = (String)in.readObject();
            m_missingValueRecs = (MutableInteger)in.readObject();
            m_noOfRows = in.readInt();
            m_bitCounts = (int[])in.readObject();
        }

        /**
         * Called after all training rows where added to validate the model.
         * @throws InvalidSettingsException if the model isn't valid
         */
        private void validate() throws InvalidSettingsException {
            if (m_noOfRows == 0) {
                setInvalidCause(MODEL_CONTAINS_NO_RECORDS);
                throw new InvalidSettingsException("Model for attribute "
                        + getAttributeName() + " contains no rows.");
            }
        }

        /**
         * @return the classValue
         */
        private String getClassValue() {
            return m_classValue;
        }

        /**
         * @return the length of the {@link BitVectorClassValue}
         */
        private int getVectorLength() {
            return m_bitCounts.length;
        }

        /**
         * @return the number of rows
         */
        private int getNoOfRows() {
            return m_noOfRows;
        }

        /**
         * @param attrValue
         */
        private void addValue(final DataCell attrValue) {
            if (attrValue.isMissing()) {
                m_missingValueRecs.inc();
            } else {
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
            }
            m_noOfRows++;
        }

        /**
         * @param attributeValue the attribute value to calculate the probability for
         * @param probabilityThreshold the probability to use in lieu of P(Ij | Tk) when count[IjTi] is zero for
         * categorical fields or when the calculated probability of the distribution falls below the threshold for
         * continuous fields.
         * @return the probability for the given attribute value
         */
        private double getProbability(final DataCell attributeValue, final double probabilityThreshold) {
            final int noOfRows4Class = getNoOfRows();
            if (noOfRows4Class == 0) {
                return 0;
            }
            if (attributeValue.isMissing()) {
                return (double)m_missingValueRecs.intValue() / noOfRows4Class;
            }
            final BitVectorValue bitVec = (BitVectorValue)attributeValue;
            if (bitVec.length() != m_bitCounts.length) {
                throw new IllegalArgumentException("Illegal bit vector length");
            }
            double combinedProbability = 1;
            for (int i = 0, length = (int)bitVec.length(); i < length; i++) {
                final double noOfRows = getNoOfRows4AttributeValue(i, bitVec.get(i));
                final double probability = noOfRows / noOfRows4Class;
                combinedProbability += Math.log(probability);
            }
            combinedProbability = Math.exp(combinedProbability);
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
         * @return the number of missing values
         */
        private int getNoOfMissingValueRecs() {
            return m_missingValueRecs.intValue();
        }

        /**
         * @param idx the index position in the {@link BitVectorClassValue}
         * @return the number of bits that are set for this class value at the
         * specified position
         */
        private int getBitCount(final int idx) {
            return m_bitCounts[idx];
        }


        /**
         * @param idx the index position in the {@link BitVectorClassValue}
         * @return the number of zeros that exist for this class value at the
         * specified position
         */
        private int getZeroCount(final int idx) {
            return getNoOfRows() - m_bitCounts[idx] - m_missingValueRecs.intValue();
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
        m_classValues = new LinkedHashMap<>();
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
    BitVectorAttributeModel(final String attributeName,
            final boolean skipMissingVals, final int noOfMissingVals,
            final Config config) throws InvalidSettingsException {
        super(attributeName, noOfMissingVals, skipMissingVals);
        final int noOfClasses = config.getInt(CLASS_VALUE_COUNTER);
        m_classValues = new LinkedHashMap<>(noOfClasses);
        final int noOfClassVals = config.getInt(CLASS_VALUE_COUNTER);
        for (int i = 0; i < noOfClassVals; i++) {
            final Config classConfig =
                config.getConfig(CLASS_VALUE_SECTION + i);
            final BitVectorClassValue classVal =
                new BitVectorClassValue(classConfig);
            m_classValues.put(classVal.getClassValue(), classVal);
        }
    }

    private BitVectorAttributeModel(final String attributeName, final boolean ignoreMissingVals, final int missingVals,
        final Map<String, BitVectorClassValue> classVals) {
        super(attributeName, missingVals, ignoreMissingVals);
        m_classValues = classVals;
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
    void exportToPMMLInternal(final BayesInput bayesInput) {
        throw new UnsupportedOperationException("BitVector not supported by PMML model");
    }

    /**
     * Stores the {@link BitVectorAttributeModel} information as an extension since the PMML standard
     * does not support bit vector columns.
     * @param inputs {@link BayesInputs} to write to
     * @param prefix the prefix to use
     * @see #readExtension(Map, String)
     */
    void writeExtension(final BayesInputs inputs, final String prefix) {
        PMMLNaiveBayesModelTranslator.setStringExtension(inputs.addNewExtension(), prefix + ATTRIBUTE_NAME,
            getAttributeName());
        PMMLNaiveBayesModelTranslator.setBooleanExtension(inputs.addNewExtension(), prefix + IGNORE_MISSING_VALUES,
            ignoreMissingVals());
        PMMLNaiveBayesModelTranslator.setIntExtension(inputs.addNewExtension(), prefix + NO_OF_MISSING_VALUES,
            getNoOfMissingVals());
        PMMLNaiveBayesModelTranslator.setObjectExtension(inputs.addNewExtension(), prefix + CLASS_VALUE_SECTION,
            m_classValues);
    }

    /**
     * Reads the model information from an extension map since the PMML standard does not support bit vector columns.
     * @param extensionMap the map with extension values to read from
     * @param prefix the prefix to use
     * @return the {@link BitVectorAttributeModel}
     * @throws InvalidSettingsException if the given extensions are invalid
     * @see #writeExtension(BayesInputs, String)
     */
    static BitVectorAttributeModel readExtension(final Map<String, String> extensionMap, final String prefix)
            throws InvalidSettingsException {
        final String attributeName =
                PMMLNaiveBayesModelTranslator.getStringExtension(extensionMap, prefix + ATTRIBUTE_NAME);
        final boolean ignoreMissingVals =
                PMMLNaiveBayesModelTranslator.getBooleanExtension(extensionMap, prefix + IGNORE_MISSING_VALUES);
        final int missingVals =
                PMMLNaiveBayesModelTranslator.getIntExtension(extensionMap, prefix + NO_OF_MISSING_VALUES);
        Map<String, BitVectorClassValue> classVals =
                (Map<String, BitVectorClassValue>) PMMLNaiveBayesModelTranslator.getObjectExtension(extensionMap,
                    prefix + CLASS_VALUE_SECTION);
        return new BitVectorAttributeModel(attributeName, ignoreMissingVals, missingVals, classVals);
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
    boolean isCompatible(final DataType type) {
        if (type == null) {
            return false;
        }
        return type.isCompatible(BitVectorValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void addValueInternal(final String classValue, final DataCell attrValue)
            throws TooManyValuesException {
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
        return value.getNoOfRows();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    double getProbabilityInternal(final String classValue, final DataCell attributeValue,
        final double probabilityThreshold) {
        final BitVectorClassValue classModel = m_classValues.get(classValue);
        if (classModel == null) {
            return 0;
        }
        return classModel.getProbability(attributeValue, probabilityThreshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void createDataRows(final ExecutionMonitor exec, final BufferedDataContainer dc, final boolean ignoreMissing,
        final AtomicInteger rowId) throws CanceledExecutionException {
        final List<String> sortedClassVal = AttributeModel.sortCollection(m_classValues.keySet());
        if (sortedClassVal == null) {
            return;
        }
        final StringCell attributeCell = new StringCell(getAttributeName());
        final int vectorLength = getVectorLength();
        for (int pos = 0; pos < vectorLength; pos++) {
            createRowsForPosition(dc, ignoreMissing, sortedClassVal, attributeCell, pos, rowId);
        }
    }

    private void createRowsForPosition(final BufferedDataContainer dc, final boolean ignoreMissing,
        final List<String> sortedClassVal, final StringCell attributeCell, final int pos, final AtomicInteger rowId) {
        boolean[] bitState = new boolean[]{false, true};
        for (boolean set : bitState) {
            for (final String classVal : sortedClassVal) {
                final StringCell classCell = new StringCell(classVal);
                final BitVectorClassValue classValue = m_classValues.get(classVal);
                final List<DataCell> cells = new LinkedList<>();
                cells.add(attributeCell);
                if (set) {
                    cells.add(new StringCell("pos_" + pos + "_set"));
                    cells.add(classCell);
                    cells.add(new IntCell(classValue.getBitCount(pos)));
                } else {
                    cells.add(new StringCell("pos_" + pos + "_notset"));
                    cells.add(classCell);
                    cells.add(new IntCell(classValue.getZeroCount(pos)));
                }
                if (!ignoreMissing) {
                    cells.add(new IntCell(classValue.getNoOfMissingValueRecs()));
                }
                cells.add(DataType.getMissingCell());
                cells.add(DataType.getMissingCell());
                dc.addRowToTable(
                    new DefaultRow(RowKey.createRowKey(rowId.getAndIncrement()), cells.toArray(new DataCell[0])));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getHTMLViewHeadLine() {
        return "P(" + StringEscapeUtils.escapeHtml(getAttributeName()) + " | class=?)";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    String getHTMLView(final int totalNoOfRecs) {
        final List<String> sortedClassVal = AttributeModel.sortCollection(m_classValues.keySet());
        if (sortedClassVal == null) {
            return "";
        }
        final int bitVectorLength = getVectorLength();
        final String classHeading = "Class/" + getAttributeName();
        final LinkedList<String> columnNames = new LinkedList<>();
        columnNames.add(getAttributeName());
        final String missingHeading = getMissingValueHeader(columnNames);
        final StringBuilder buf = new StringBuilder();
        buf.append("<table width='100%'>");
        //first table header section
        buf.append("<tr>");
        buf.append("<th rowspan=2>");
        buf.append(StringEscapeUtils.escapeHtml(classHeading));
        buf.append("</th>");
//first table header row
        for (int i = 0, length = getVectorLength(); i < length; i++) {
            buf.append("<th colspan=2 align='center'>");
            buf.append("Pos" + i);
            buf.append("</th>");
        }
        if (missingHeading != null) {
            buf.append("<th rowspan = 2>");
            buf.append(StringEscapeUtils.escapeHtml(missingHeading));
            buf.append("</th>");
        }
        buf.append("</tr>");
//second table header row
        buf.append("<tr>");
        //create the first header row
        for (int i = 0; i < bitVectorLength; i++) {
            buf.append("<th align='center'>");
            buf.append("0");
            buf.append("</th>");
            buf.append("<th align='center'>");
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
            buf.append(StringEscapeUtils.escapeHtml(classVal));
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
            }
            //no of missing values for this class value
            if (missingHeading != null) {
                totalMissingValCounter += classValue.getNoOfMissingValueRecs();
                buf.append("<td align='center'>");
                buf.append(classValue.getNoOfMissingValueRecs());
                buf.append("</td>");
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
            buf.append(nf.format(totalMissingValCounter / (double) totalNoOfRecs));
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

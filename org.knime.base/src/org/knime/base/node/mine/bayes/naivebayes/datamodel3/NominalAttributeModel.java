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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

package org.knime.base.node.mine.bayes.naivebayes.datamodel3;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.util.FastMath;
import org.dmg.pmml.BayesInputDocument.BayesInput;
import org.dmg.pmml.PairCountsDocument.PairCounts;
import org.dmg.pmml.TargetValueCountDocument.TargetValueCount;
import org.dmg.pmml.TargetValueCountsDocument.TargetValueCounts;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.util.MutableInteger;

/**
 * This {@link AttributeModel} implementation calculates the probability for all nominal attributes.
 *
 * @author Tobias Koetter, University of Konstanz
 * @noreference This class is not intended to be referenced by clients.
 */
final class NominalAttributeModel extends AttributeModel {

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

        private final Map<String, MutableInteger> m_recsByAttrValue = new HashMap<>();

        private final MutableInteger m_missingValueRecs;

        /**
         * Constructor for class NominalRowValue.NominalClassValue.
         *
         * @param classValue the class value
         */
        private NominalClassValue(final String classValue, final int missingValueRecs) {
            m_classValue = classValue;
            m_missingValueRecs = new MutableInteger(missingValueRecs);
            m_noOfRows = missingValueRecs;
        }

        /**
         * Constructor for class NominalClassValue.
         *
         * @param config the <code>Config</code> object to read from
         * @throws InvalidSettingsException if the settings are invalid
         */
        private NominalClassValue(final Config config) throws InvalidSettingsException {
            m_classValue = config.getString(CLASS_VALUE);
            m_noOfRows = (int)config.getLong(NO_OF_ROWS);
            m_missingValueRecs = new MutableInteger((int)config.getLong(MISSING_VALUE_COUNTER));
            final String[] attrVals = config.getStringArray(ATTRIBUTE_VALS);
            final int[] recsCounter = longToIntArr(config.getLongArray(ATTR_VAL_COUNTER));
            if (attrVals.length != recsCounter.length) {
                throw new InvalidSettingsException("Attribute and counter array must be of equal size");
            }
            for (int i = 0, length = attrVals.length; i < length; i++) {
                m_recsByAttrValue.put(attrVals[i], new MutableInteger(recsCounter[i]));
            }
        }

        /**
         * @param config the config object to write to
         */
        private void saveModel(final Config config) {
            config.addString(CLASS_VALUE, m_classValue);
            config.addLong(NO_OF_ROWS, m_noOfRows);
            config.addLong(MISSING_VALUE_COUNTER, getNoOfMissingValueRecs());
            final String[] attrVals = new String[m_recsByAttrValue.size()];
            final long[] recsCounter = new long[m_recsByAttrValue.size()];
            int i = 0;
            for (final String classVal : m_recsByAttrValue.keySet()) {
                attrVals[i] = classVal;
                recsCounter[i] = m_recsByAttrValue.get(classVal).intValue();
                i++;
            }
            config.addStringArray(ATTRIBUTE_VALS, attrVals);
            config.addLongArray(ATTR_VAL_COUNTER, recsCounter);
        }

        /**
         * @param attrVal the attribute value to add to this class
         */
        private void addValue(final DataCell attrVal) {
            if (attrVal.isMissing()) {
                m_missingValueRecs.inc();
            } else {
                MutableInteger attrRowCounter = m_recsByAttrValue.get(attrVal.toString());
                if (attrRowCounter == null) {
                    attrRowCounter = new MutableInteger(0);
                    m_recsByAttrValue.put(attrVal.toString(), attrRowCounter);
                }
                attrRowCounter.inc();
            }
            // no need to check other counter since m_noOfRows always >=
            checkLimits(m_noOfRows);
            m_noOfRows++;
        }

        /**
         * @param attributeValue the attribute value
         * @param rowCount the corresponding row count
         */
        private void addCount(final String attributeValue, final int rowCount) {
            MutableInteger counter = m_recsByAttrValue.get(attributeValue);
            if (counter == null) {
                counter = new MutableInteger(0);
                m_recsByAttrValue.put(attributeValue, counter);
            }
            // no need to check counter since m_noOfRows >= counter
            counter.add(rowCount);
            // TODO: has to be updated once we switch to long
            if (Integer.MAX_VALUE - m_noOfRows < rowCount) {
                // throws an exception
                checkLimits(Integer.MAX_VALUE);
            }
            m_noOfRows += rowCount;
        }

        /**
         * @return the classValue
         */
        private String getClassValue() {
            return m_classValue;
        }

        /**
         * @return the total number of rows with this class
         */
        private int getNoOfRows() {
            return m_noOfRows;
        }

        /**
         * @param attributeValue the attribute value we wan't the number of rows for
         * @return the number of rows with this class and the given attribute
         */
        private int getNoOfRows4AttributeValue(final DataCell attributeValue) {
            if (attributeValue.isMissing()) {
                return getNoOfMissingValueRecs();
            }
            return getNoOfRows4AttributeValue(attributeValue.toString());
        }

        /**
         * @param attributeValue the attribute value we wan't the number of rows for
         * @return the number of rows with this class and the given attribute
         */
        private int getNoOfRows4AttributeValue(final String attributeValue) {
            final MutableInteger counter = m_recsByAttrValue.get(attributeValue);
            if (counter != null) {
                return counter.intValue();
            }
            return 0;
        }

        /**
         * @param attributeValue
         * @param logProbThreshold
         * @return
         */
        private double getLogProbability(final DataCell attributeValue, final double logProbThreshold) {
            final int noOfRows4Class = getNoOfRows();
            if (noOfRows4Class == 0) {
                throw new IllegalStateException(
                    "Model for attribute " + getAttributeName() + " contains no rows for class " + m_classValue);
            }
            final double noOfRows = getNoOfRows4AttributeValue(attributeValue);
            double prob = logProbThreshold;
            if (noOfRows > 0) {
                prob = FastMath.log(noOfRows / noOfRows4Class);
            }
            return prob;
        }

        /**
         * @return the missingValueRecs
         */
        private int getNoOfMissingValueRecs() {
            return m_missingValueRecs.intValue();
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

    private final Map<String, NominalClassValue> m_classValues = new LinkedHashMap<>();

    private final Set<String> m_attributeVals = new LinkedHashSet<>();

    /**
     * Constructor for class NominalRowValue.
     *
     * @param attributeName the name of the attribute
     * @param skipMissingVals set to <code>true</code> if the missing values should be skipped during learning and
     *            prediction
     * @param maxNoOfNominalVals the maximum number of nominal values
     */
    NominalAttributeModel(final String attributeName, final boolean skipMissingVals, final int maxNoOfNominalVals) {
        super(attributeName, 0, skipMissingVals);
        m_maxNoOfAttrVals = maxNoOfNominalVals;
    }

    /**
     * Constructor for class NominalAttributeModel.
     *
     * @param attributeName the name of this attribute
     * @param noOfMissingVals the number of missing values
     * @param skipMissingVals set to <code>true</code> if the missing values should be skipped during learning and
     *            prediction
     * @param config the <code>Config</code> object to read from
     * @throws InvalidSettingsException if the settings are invalid
     */
    NominalAttributeModel(final String attributeName, final int noOfMissingVals, final boolean skipMissingVals,
        final Config config) throws InvalidSettingsException {
        super(attributeName, noOfMissingVals, skipMissingVals);
        m_maxNoOfAttrVals = (int)config.getLong(MAX_NO_OF_ATTRS);
        final int noOfClassVals = (int)config.getLong(CLASS_VALUE_COUNTER);
        final String[] attrVals = config.getStringArray(ATTRIBUTE_VALUES);
        m_attributeVals.addAll(Arrays.asList(attrVals));
        for (int i = 0; i < noOfClassVals; i++) {
            final Config classConfig = config.getConfig(CLASS_VALUE_SECTION + i);
            final NominalClassValue classVal = new NominalClassValue(classConfig);
            m_classValues.put(classVal.getClassValue(), classVal);
        }
    }

    /**
     * Constructor for class NominalAttributeModel.
     *
     * @param attributeName the name of this attribute
     * @param noOfMissingVals the number of missing values
     * @param ignoreMissingVals set to <code>true</code> if the missing values should be ignored during learning and
     *            prediction
     * @param bayesInput the <code>BayesInput</code> object to read from
     * @throws InvalidSettingsException if the settings are invalid
     */
    NominalAttributeModel(final String attributeName, final int noOfMissingVals, final boolean ignoreMissingVals,
        final BayesInput bayesInput) throws InvalidSettingsException {
        super(attributeName, noOfMissingVals, ignoreMissingVals);
        m_maxNoOfAttrVals = Integer.MAX_VALUE;
        final List<PairCounts> pairCounts = bayesInput.getPairCountsList();
        for (final PairCounts pairCount : pairCounts) {
            final String attributeValue = pairCount.getValue();
            m_attributeVals.add(attributeValue);
            final TargetValueCounts targetValueCounts = pairCount.getTargetValueCounts();
            for (TargetValueCount targetCount : targetValueCounts.getTargetValueCountList()) {
                final String classValue = targetCount.getValue();
                NominalClassValue classVal = m_classValues.get(classValue);
                if (classVal == null) {
                    final Map<String, String> extensionMap =
                        PMMLNaiveBayesModelTranslator.convertToMap(targetCount.getExtensionList());
                    int missingValueRecs = 0;
                    if (extensionMap.containsKey(NominalClassValue.MISSING_VALUE_COUNTER)) {
                        missingValueRecs = (int)PMMLNaiveBayesModelTranslator.getLongExtension(extensionMap,
                            NominalClassValue.MISSING_VALUE_COUNTER);
                    }
                    classVal = new NominalClassValue(classValue, missingValueRecs);
                    m_classValues.put(classValue, classVal);
                }
                double count = targetCount.getCount();
                classVal.addCount(attributeValue, (int)count);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void exportToPMMLInternal(final BayesInput bayesInput) {
        for (final String attributeValue : m_attributeVals) {
            PairCounts pairCounts = bayesInput.addNewPairCounts();
            pairCounts.setValue(attributeValue);
            final TargetValueCounts targetValueCounts = pairCounts.addNewTargetValueCounts();
            for (final NominalClassValue classVal : m_classValues.values()) {
                final TargetValueCount targetValueCount = targetValueCounts.addNewTargetValueCount();
                if (!ignoreMissingVals()) {
                    PMMLNaiveBayesModelTranslator.setLongExtension(targetValueCount.addNewExtension(),
                        NominalClassValue.MISSING_VALUE_COUNTER, classVal.getNoOfMissingValueRecs());
                }
                targetValueCount.setValue(classVal.getClassValue());
                final MutableInteger attrCount = classVal.m_recsByAttrValue.get(attributeValue);
                final int count;
                if (attrCount != null) {
                    count = attrCount.intValue();
                } else {
                    count = 0;
                }
                targetValueCount.setCount(count);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void saveModelInternal(final Config config) {
        config.addLong(MAX_NO_OF_ATTRS, getMaxNoOfAttrVals());
        config.addLong(CLASS_VALUE_COUNTER, m_classValues.size());
        final String[] attrVals = m_attributeVals.toArray(new String[]{});
        config.addStringArray(ATTRIBUTE_VALUES, attrVals);
        int i = 0;
        for (final NominalClassValue classVal : m_classValues.values()) {
            final Config classConfig = config.addConfig(CLASS_VALUE_SECTION + i);
            classVal.saveModel(classConfig);
            i++;
        }
    }

    /**
     * @return the maximum supported number of unique attribute values
     */
    private int getMaxNoOfAttrVals() {
        return m_maxNoOfAttrVals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean isCompatible(final DataType type) {
        if (type == null) {
            return false;
        }
        return type.isCompatible(NominalValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void addValueInternal(final String classValue, final DataCell attrValue) throws TooManyValuesException {
        NominalClassValue classObject = m_classValues.get(classValue);
        if (classObject == null) {
            classObject = new NominalClassValue(classValue, 0);
            m_classValues.put(classValue, classObject);
        }
        if (!attrValue.isMissing()) {
            final String attrValString = attrValue.toString();
            if (!m_attributeVals.contains(attrValString)) {
                //check the different number of attribute values
                if (m_attributeVals.size() >= getMaxNoOfAttrVals()) {
                    throw new TooManyValuesException("Attribute value " + attrValString + " doesn't fit into model");
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
            throw new InvalidSettingsException(
                "Model for attribute " + getAttributeName() + " contains no attribute values");
        }
        if (m_classValues.size() == 0) {
            setInvalidCause(MODEL_CONTAINS_NO_CLASS_VALUES);
            throw new InvalidSettingsException(
                "Model for attribute " + getAttributeName() + " contains no class values");
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
    String getHTMLViewHeadLine() {
        return "P(" + getAttributeName() + " | class=?)";
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
    void createDataRows(final ExecutionMonitor exec, final BufferedDataContainer dc, final boolean ignoreMissing,
        final AtomicInteger rowId) throws CanceledExecutionException {
        final List<String> sortedClassVal = AttributeModel.sortCollection(m_classValues.keySet());
        if (sortedClassVal == null) {
            return;
        }
        final List<String> sortedAttrValues = AttributeModel.sortCollection(m_attributeVals);
        final StringCell attributeNameCell = new StringCell(getAttributeName());
        for (final String attrVal : sortedAttrValues) {
            final StringCell attributeValueCell = new StringCell(attrVal);
            for (final String classVal : sortedClassVal) {
                final StringCell classCell = new StringCell(classVal);
                final NominalClassValue classValue = m_classValues.get(classVal);
                final List<DataCell> cells = new LinkedList<>();
                cells.add(attributeNameCell);
                cells.add(attributeValueCell);
                cells.add(classCell);
                cells.add(new IntCell(classValue.getNoOfRows4AttributeValue(attrVal)));
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
    String getHTMLView(final int totalNoOfRecs) {
        final List<String> sortedClassVal = AttributeModel.sortCollection(m_classValues.keySet());
        if (sortedClassVal == null) {
            return "";
        }
        final List<String> sortedAttrValues = AttributeModel.sortCollection(m_attributeVals);
        final String classHeading = "Class/" + getAttributeName();
        final String missingHeading = getMissingValueHeader(m_attributeVals);
        final int arraySize;
        if (missingHeading != null) {
            arraySize = sortedAttrValues.size() + 1;
        } else {
            arraySize = sortedAttrValues.size();
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("<table width='100%'>");
        buf.append(createTableHeader(classHeading, sortedAttrValues, missingHeading));
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
                final int rowCount = classValue.getNoOfRows4AttributeValue(attrVal);
                rowsPerValCounts[i] += rowCount;
                buf.append("<td align='center'>");
                buf.append(rowCount);
                buf.append("</td>");
            }
            if (missingHeading != null) {
                rowsPerValCounts[arraySize - 1] += classValue.getNoOfMissingValueRecs();
                buf.append("<td align='center'>");
                buf.append(classValue.getNoOfMissingValueRecs());
                buf.append("</td>");
            }
            buf.append("</tr>");
        }
        //create the value summary section
        buf.append(createSummarySection(totalNoOfRecs, rowsPerValCounts));
        buf.append("</table>");
        return buf.toString();
    }

    private static String createSummarySection(final int totalRowCount, final int[] rowsPerValCounts) {
        final NumberFormat nf = NumberFormat.getPercentInstance();
        final StringBuilder buf = new StringBuilder();
        buf.append("<tr>");
        buf.append("<th>");
        buf.append("Rate:");
        buf.append("</th>");
        for (int i = 0, length = rowsPerValCounts.length; i < length; i++) {
            buf.append("<td align='center'>");
            buf.append(nf.format(rowsPerValCounts[i] / (double)totalRowCount));
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

    /**
     * {@inheritDoc}
     */
    @Override
    double getLogProbabilityInternal(final String classValue, final DataCell attributeValue,
        final double logProbThreshold) {
        final NominalClassValue classVal = m_classValues.get(classValue);
        if (classVal == null) {
            return 0;
        }
        return classVal.getLogProbability(attributeValue, logProbThreshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean hasRecs4ClassValue(final String classValue) {
        final NominalClassValue classVal = m_classValues.get(classValue);
        if (classVal == null || classVal.getNoOfRows() == 0) {
            return false;
        }
        return true;
    }
}

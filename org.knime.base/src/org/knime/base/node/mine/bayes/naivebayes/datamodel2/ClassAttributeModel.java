/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.dmg.pmml.BayesInputDocument.BayesInput;
import org.dmg.pmml.BayesOutputDocument.BayesOutput;
import org.dmg.pmml.TargetValueCountDocument.TargetValueCount;
import org.dmg.pmml.TargetValueCountsDocument.TargetValueCounts;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
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


    /*
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

    /**The compatible {@link DataValue} classes.*/
    @SuppressWarnings("unchecked")
    public static final Class<? extends DataValue>[] COMPATIBLE_DATA_VALUE_CLASSES = new Class[] {NominalValue.class,
        IntValue.class, LongValue.class, DoubleValue.class, BooleanValue.class};


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
        m_recsCounterByClassVal = new LinkedHashMap<>(maxNoOfClassVals);
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
        m_recsCounterByClassVal = new LinkedHashMap<>(classVals.length);
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


    /**Constructor for class ClassModel.
     * @param attributeName the name of the attribute
     * @param noOfMissingVals the number of missing values
     * @param ignoreMissingVals set to <code>true</code> if the missing values
     * should be ignored during learning and prediction
     * @param out the <code>BayesOutput</code> object to read from
     * @throws InvalidSettingsException if the settings are invalid
     */
    private ClassAttributeModel(final String attributeName, final int noOfMissingVals, final boolean ignoreMissingVals,
            final BayesOutput out) throws InvalidSettingsException {
        super(attributeName, noOfMissingVals, ignoreMissingVals);
        final TargetValueCounts targetValueCounts = out.getTargetValueCounts();
        m_maxNoOfClassVals = Integer.MAX_VALUE;
        final List<TargetValueCount> targetValueCountList = targetValueCounts.getTargetValueCountList();
        m_recsCounterByClassVal = new LinkedHashMap<>(targetValueCountList.size());
        for (TargetValueCount targetValueCount : targetValueCountList) {
            final int count = (int)targetValueCount.getCount();
            m_totalNoOfRecs += count;
            m_recsCounterByClassVal.put(targetValueCount.getValue(), new MutableInteger(count));
        }
    }

    /**
     * @param out the {@link BayesOutput} to read from
     * @return the {@link ClassAttributeModel}
     * @throws InvalidSettingsException if the model could not be generated from the given {@link BayesOutput}
     */
    static ClassAttributeModel loadClassAttributeFromPMML(final BayesOutput out) throws InvalidSettingsException {
        final Map<String, String> extensionMap = PMMLNaiveBayesModelTranslator.convertToMap(out.getExtensionList());
        boolean skipMissing = true;
        int noOfMissing = 0;
        if (extensionMap.containsKey(NO_OF_MISSING_VALUES)) {
            skipMissing = false;
            noOfMissing = PMMLNaiveBayesModelTranslator.getIntExtension(extensionMap, NO_OF_MISSING_VALUES);
        }
        return new ClassAttributeModel(out.getFieldName(), noOfMissing, skipMissing, out);
    }

    /**
     * @param out the PMML {@link BayesOutput} to write the class counts to
     */
    void exportClassAttributeToPMML(final BayesOutput out) {
        out.setFieldName(getAttributeName());
        if (!ignoreMissingVals()) {
            PMMLNaiveBayesModelTranslator.setIntExtension(out.addNewExtension(), NO_OF_MISSING_VALUES,
                getNoOfMissingVals());
        }
        final TargetValueCounts targetValueCounts = out.addNewTargetValueCounts();
        for (final String classVal : m_recsCounterByClassVal.keySet()) {
            final TargetValueCount targetValueCount = targetValueCounts.addNewTargetValueCount();
            targetValueCount.setValue(classVal);
            targetValueCount.setCount(m_recsCounterByClassVal.get(classVal).doubleValue());
        }
    }

    /**
     * {@inheritDoc}
     * @see #exportClassAttributeToPMML(BayesOutput)
     */
    @Override
    void exportToPMMLInternal(final BayesInput in) {
        throw new UnsupportedOperationException("Class model does not write to BayesInput");
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
    boolean isCompatible(final DataType type) {
        if (type == null) {
            return false;
        }
        for (Class<? extends DataValue> classVal : COMPATIBLE_DATA_VALUE_CLASSES) {
            if (type.isCompatible(classVal)) {
                return true;
            }
        }
        return false;
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
        final MutableInteger noOfRecs = m_recsCounterByClassVal.get(classValue);
        if (noOfRecs == null) {
            return null;
        }
        return Integer.valueOf(noOfRecs.intValue());
    }

    /**
     * @return the total number of records
     */
    int getTotalNoOfRecs() {
        return m_totalNoOfRecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    double getProbabilityInternal(final String classValue, final DataCell attributeValue,
        final double probabilityThreshold) {
        if (attributeValue.isMissing()) {
            throw new IllegalArgumentException(
                    "Missing value not allowed as class value");
        }
        if (m_totalNoOfRecs == 0) {
            //this should not happen
            throw new IllegalStateException("Model for attribute " + getAttributeName() + " contains no records");
        }
        final MutableInteger noOfRecs = m_recsCounterByClassVal.get(classValue);
        if (noOfRecs == null) {
            throw new IllegalStateException("No record counter object found for attribute "
                    + getAttributeName() + " and class value " + classValue);
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
        buf.append(AttributeModel.createHTMLTable(null, "Class: ", "Count: ", 10, m_recsCounterByClassVal, true));
        buf.append("<b>Total count: </b>" + totalNoOfRecs + "<br><br>");
        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override void createDataRows(final ExecutionMonitor exec, final BufferedDataContainer dc,
        final boolean ignoreMissing, final AtomicInteger rowId) throws CanceledExecutionException {
        final List<String> sortedClassVal = AttributeModel.sortCollection(m_recsCounterByClassVal.keySet());
        if (sortedClassVal == null) {
            return;
        }
        final StringCell attributeNameCell = new StringCell(getAttributeName());
        for (final String classVal : sortedClassVal) {
            final StringCell classCell = new StringCell(classVal);
            final List<DataCell> cells = new LinkedList<>();
            cells.add(attributeNameCell);
            cells.add(DataType.getMissingCell());
            cells.add(classCell);
            cells.add(new IntCell(getNoOfRecs4ClassValue(classVal)));
            if (!ignoreMissing) {
                cells.add(new IntCell(getNoOfMissingVals()));
            }
            cells.add(DataType.getMissingCell());
            cells.add(DataType.getMissingCell());
            dc.addRowToTable(
                new DefaultRow(RowKey.createRowKey(rowId.getAndIncrement()), cells.toArray(new DataCell[0])));
        }
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

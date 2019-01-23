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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.util.FastMath;
import org.dmg.pmml.BayesInputDocument.BayesInput;
import org.dmg.pmml.GaussianDistributionDocument.GaussianDistribution;
import org.dmg.pmml.TargetValueStatDocument.TargetValueStat;
import org.dmg.pmml.TargetValueStatsDocument.TargetValueStats;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.MutableInteger;

/**
 * This {@link AttributeModel} implementation calculates the probability for numerical attributes by assuming a Gaussian
 * distribution of the data.
 *
 * @author Tobias Koetter, University of Konstanz
 * @noreference This class is not intended to be referenced by clients.
 */
final class NumericalAttributeModel extends AttributeModel {

    /**
     * The unique type of this model used for saving/loading.
     */
    static final String MODEL_TYPE = "NumericalModel";

    private static final String CLASS_VALUE_COUNTER = "noOfClasses";

    private static final String CLASS_VALUE_SECTION = "classValueData_";

    //    private static final int HTML_VIEW_SIZE = 5;

    private final class NumericalClassValue {

        /** The standard deviation is not a number exception text. */
        private static final String SD_NAN_EXCEPTION = "Cannot handle <NaN> standard deviations";

        /** Defines whether the sampling or population variance has to be computed. */
        private static final boolean CALC_SAMPLING_VAR = true;

        private static final double TWO_PI = 2 * FastMath.PI;

        private static final String CLASS_VALUE = "classValue";

        private static final String MISSING_VALUE_COUNTER = "MissingValCounter";

        private static final String NO_OF_ROWS = "noOfRows";

        private static final String MEAN_CFG = "mean";

        //        private static final String VARIANCE_CFG = "variance";

        private static final String SD_CFG = "sd";

        private final String m_classValue;

        private int m_noOfRows = 0;

        private final Mean m_incMean;

        private final Variance m_incVar;

        private double m_mean = Double.NaN;

        private double m_sd = Double.NaN;

        private double m_logSdTwoPi = Double.NaN;

        private final MutableInteger m_missingValueRecs = new MutableInteger(0);

        /**
         * Constructor for class NumericalRowValue.NumericalClassValue.
         *
         * @param classValue the value of this class
         *
         */
        private NumericalClassValue(final String classValue) {
            m_classValue = classValue;
            m_incMean = new Mean();
            m_incVar = new Variance(CALC_SAMPLING_VAR);
        }

        /**
         * Constructor for class NumericalClassValue.
         *
         * @param config the <code>Config</code> object to read from
         * @throws InvalidSettingsException if the settings are invalid
         */
        private NumericalClassValue(final Config config) throws InvalidSettingsException {
            //            throw new RuntimeException("This constructor is forbidden");
            m_classValue = config.getString(CLASS_VALUE);
            m_missingValueRecs.setValue((int)config.getLong(MISSING_VALUE_COUNTER));
            m_noOfRows = (int)config.getLong(NO_OF_ROWS);
            m_mean = config.getDouble(MEAN_CFG);
            m_sd = config.getDouble(SD_CFG);
            CheckUtils.checkArgument(!Double.isNaN(m_sd), SD_NAN_EXCEPTION);
            m_incVar = null;
            m_incMean = null;
        }

        /**
         * Constructor for class NumericalClassValue.
         *
         * @param targetValueStat the <code>TargetValueStat</code> object to read from
         * @throws InvalidSettingsException if the settings are invalid
         */
        private NumericalClassValue(final TargetValueStat targetValueStat) throws InvalidSettingsException {
            m_classValue = targetValueStat.getValue();
            final GaussianDistribution distribution = targetValueStat.getGaussianDistribution();
            m_sd = FastMath.sqrt(distribution.getVariance());
            CheckUtils.checkArgument(!Double.isNaN(m_sd), SD_NAN_EXCEPTION);
            m_mean = distribution.getMean();
            m_logSdTwoPi = FastMath.log(m_sd) + 0.5 * FastMath.log(TWO_PI);
            // load mssing value counters
            final Map<String, String> extensionMap =
                PMMLNaiveBayesModelTranslator.convertToMap(targetValueStat.getExtensionList());
            if (extensionMap.containsKey(MISSING_VALUE_COUNTER)) {
                m_missingValueRecs
                    .setValue((int)PMMLNaiveBayesModelTranslator.getLongExtension(extensionMap, MISSING_VALUE_COUNTER));
                m_noOfRows = (int)PMMLNaiveBayesModelTranslator.getLongExtension(extensionMap, NO_OF_ROWS);
            }
            m_incMean = null;
            m_incVar = null;
        }

        /**
         * @param config the <code>Config</code> object to write to
         */
        private void saveModel(final Config config) {
            config.addString(CLASS_VALUE, m_classValue);
            config.addLong(MISSING_VALUE_COUNTER, getNoOfMissingValueRecs());
            config.addLong(NO_OF_ROWS, m_noOfRows);
            config.addDouble(MEAN_CFG, getMean());
            config.addDouble(SD_CFG, getStdDeviation());
        }

        /**
         * @param targetValueStats
         */
        private void exportToPMML(final TargetValueStat targetValueStat) {
            targetValueStat.setValue(getClassValue());
            if (!ignoreMissingVals()) {
                PMMLNaiveBayesModelTranslator.setLongExtension(targetValueStat.addNewExtension(), MISSING_VALUE_COUNTER,
                    getNoOfMissingValueRecs());
                PMMLNaiveBayesModelTranslator.setLongExtension(targetValueStat.addNewExtension(), NO_OF_ROWS,
                    getNoOfRows());
            }
            final GaussianDistribution distribution = targetValueStat.addNewGaussianDistribution();
            distribution.setMean(getMean());
            distribution.setVariance(getVariance());
        }

        /**
         * @return the classValue
         */
        private String getClassValue() {
            return m_classValue;
        }

        /**
         * @return the noOfRows
         */
        private int getNoOfRows() {
            return m_noOfRows;
        }

        private int getNoOfNotMissingRows() {
            return m_noOfRows - getNoOfMissingValueRecs();
        }

        /**
         * @return the mean
         */
        private double getMean() {
            if (m_incMean != null) {
                return m_incMean.getResult();
            }
            return m_mean;
        }

        /**
         * @return the standard deviation
         */
        private double getStdDeviation() {
            if (m_incVar != null) {
                assert m_minSdValue != Double.NaN : "The minimum standard deviation value havsn't been set";
                final double sd = FastMath.sqrt(m_incVar.getResult());
                return (Double.isNaN(sd) || sd <= m_minSdValue) ? m_minSdValue : sd;
            }
            return m_sd;
        }

        /**
         * @return the variance
         */
        private double getVariance() {
            final double sd = getStdDeviation();
            return sd * sd;
        }

        /**
         * @param attrVal the attribute value to add to this class
         */
        private void addValue(final DataCell attrVal) {
            if (attrVal.isMissing()) {
                m_missingValueRecs.inc();
            } else {
                final double val = ((DoubleValue)attrVal).getDoubleValue();
                m_incMean.increment(val);
                m_incVar.increment(val);
            }
            // no need to check missingValuesRecs since m_noOfRows >= missingValuesRecs
            m_noOfRows = exactInc(m_noOfRows);
        }

        private double getLogProbability(final DataCell attributeValue, final double logProbThreshold) {
            /* This is soo wrong ... but actually we cannot call this method except we have loaded the
             * model from PMML => this should ever happen.
             */
            if (Double.isNaN(m_mean) || m_incMean != null) {
                throw new RuntimeException("Mean hasn't been calculated");
            }
            /* m_noOfRows == 0 & therefore getNoOfNotMissingRows() == 0 if we have read from PMML without missing
             * entries => however, we should not run into this case, since it is ensure by the calling method
             */
            if (attributeValue.isMissing()) {
                if (m_noOfRows == 0) {
                    throw new IllegalStateException(
                        "Model for attribute " + getAttributeName() + " contains no rows for class " + m_classValue);
                }
                return logProbThreshold;
            }

            if (Double.isInfinite(m_sd)) {
                return logProbThreshold;
            }

            final double attrValue = ((DoubleValue)attributeValue).getDoubleValue();
            final double diff = attrValue - m_mean;

            /*
             * Gaussian prob, see https://en.wikipedia.org/wiki/Naive_Bayes_classifier#Gaussian_naive_Bayes
             * p(x = v | C_k) = exp(-((x - mean) * (x -mean) / (2*variance))))  /  sqrt(2 \pi variance)
             * Note that for logs it holds that x * y = exp(log(x) + log(y))
             * denominator: log(sqrt(2 \pi variance)) = log((2 \pi variance)^{1/2}) = 0.5 * log(2 \pi variance)
             * numerator: log(exp(-((x - mean) * (x -mean) / (2*variance)))) = 1/2 * ((x - mean) * (x -mean) / variance)
             *
             * the denominator can be negative infinite if the variance = 0 and positive infinite if TWO_PI * variance
             * leaves the double range.
             * the numerator can be Double.POSITIVE_INFINITY if diff very large and/or variance really small. Note that
             * it holds that variance != 0 if the model has been calculated using KNIME's Naive Bayes Learner.
             * We can substract - Double.NEGATIVE_INFINITY from Double.NEGATIVE_INFINITY without any problems.
             */
            final double frac = diff / m_sd;
            final double prob = -0.5 * (frac * frac) - m_logSdTwoPi;

            return FastMath.max(logProbThreshold, prob);
        }

        /**
         * Called after all training rows where added to validate the model.
         *
         * @throws InvalidSettingsException if the model isn't valid
         */
        private void validate() throws InvalidSettingsException {
            if (m_noOfRows == 0) {
                setInvalidCause(MODEL_CONTAINS_NO_RECORDS);
                throw new InvalidSettingsException("Model for attribute " + getAttributeName() + " contains no rows.");
            }
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
            buf.append("Standard deviation: ");
            buf.append(getStdDeviation());
            buf.append("\n");
            buf.append("Mean: ");
            buf.append(getMean());
            buf.append("\n");
            buf.append("No of rows: ");
            buf.append(getNoOfRows());
            buf.append("\n");
            buf.append("Missing values: ");
            buf.append(getNoOfMissingValueRecs());
            buf.append("\n");
            return buf.toString();
        }

    }

    private final Map<String, NumericalClassValue> m_classValues;

    private double m_minSdValue = Double.NaN;

    /**
     * Constructor for class NumericalRowValue.
     *
     * @param attributeName the row caption
     * @param skipMissingVals set to <code>true</code> if the missing values should be skipped during learning and
     *            prediction
     * @param minSdValue the minimum standard deviation value used when the standard deviation is smaller than this
     *            value
     */
    NumericalAttributeModel(final String attributeName, final boolean skipMissingVals, final double minSdValue) {
        super(attributeName, 0, skipMissingVals);
        m_minSdValue = minSdValue;
        m_classValues = new HashMap<>();
    }

    /**
     * Constructor for class NumericalAttributeModel.
     *
     * @param attributeName the name of the attribute
     * @param skipMissingVals set to <code>true</code> if the missing values should be skipped during learning and
     *            prediction
     * @param noOfMissingVals the number of missing values
     * @param config the <code>Config</code> object to read from
     * @throws InvalidSettingsException if the settings are invalid
     */
    NumericalAttributeModel(final String attributeName, final boolean skipMissingVals, final int noOfMissingVals,
        final Config config) throws InvalidSettingsException {
        super(attributeName, noOfMissingVals, skipMissingVals);
        final int noOfClasses = (int)config.getLong(CLASS_VALUE_COUNTER);
        m_classValues = new HashMap<>(noOfClasses);
        for (int i = 0; i < noOfClasses; i++) {
            final Config classConfig = config.getConfig(CLASS_VALUE_SECTION + i);
            final NumericalClassValue classVal = new NumericalClassValue(classConfig);
            m_classValues.put(classVal.getClassValue(), classVal);
        }
    }

    /**
     * Constructor for class NumericalAttributeModel.
     *
     * @param attributeName the name of the attribute
     * @param ignoreMissingVals set to <code>true</code> if the missing values should be skipped during learning and
     *            prediction
     * @param noOfMissingVals the number of missing values
     * @param bayesInput the <code>BayesInput</code> object to read from
     * @throws InvalidSettingsException if the settings are invalid
     */
    NumericalAttributeModel(final String attributeName, final boolean ignoreMissingVals, final int noOfMissingVals,
        final BayesInput bayesInput) throws InvalidSettingsException {
        super(attributeName, noOfMissingVals, ignoreMissingVals);
        TargetValueStats targetValueStats = bayesInput.getTargetValueStats();
        List<TargetValueStat> targetValueStatList = targetValueStats.getTargetValueStatList();
        m_classValues = new HashMap<>(targetValueStatList.size());
        for (TargetValueStat targetValueStat : targetValueStatList) {
            NumericalClassValue classValue = new NumericalClassValue(targetValueStat);
            m_classValues.put(classValue.getClassValue(), classValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void saveModelInternal(final Config config) {
        config.addLong(CLASS_VALUE_COUNTER, m_classValues.size());
        int i = 0;
        for (final NumericalClassValue classVal : m_classValues.values()) {
            final Config classConfig = config.addConfig(CLASS_VALUE_SECTION + i);
            classVal.saveModel(classConfig);
            i++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void exportToPMMLInternal(final BayesInput bayesInput) {
        final TargetValueStats targetValueStats = bayesInput.addNewTargetValueStats();
        for (final NumericalClassValue classVal : m_classValues.values()) {
            final TargetValueStat targetValueStat = targetValueStats.addNewTargetValueStat();
            classVal.exportToPMML(targetValueStat);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void addValueInternal(final String classValue, final DataCell attrValue) {
        NumericalClassValue classObject = m_classValues.get(classValue);
        if (classObject == null) {
            classObject = new NumericalClassValue(classValue);
            m_classValues.put(classValue, classObject);
        }
        classObject.addValue(attrValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void validate() throws InvalidSettingsException {
        if (m_classValues.size() == 0) {
            setInvalidCause(MODEL_CONTAINS_NO_CLASS_VALUES);
            throw new InvalidSettingsException(
                "Model for attribute " + getAttributeName() + " contains no class values");
        }
        final Collection<NumericalClassValue> classVals = m_classValues.values();
        for (final NumericalClassValue value : classVals) {
            value.validate();
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
        return type.isCompatible(DoubleValue.class);
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
        final NumericalClassValue value = m_classValues.get(classValue);
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
        return "Gaussian distribution for " + getAttributeName() + " per class value";
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
        final StringCell attributeCell = new StringCell(getAttributeName());
        for (final String classVal : sortedClassVal) {
            final List<DataCell> cells = new LinkedList<>();
            cells.add(attributeCell);
            cells.add(DataType.getMissingCell());
            cells.add(new StringCell(classVal));
            final NumericalClassValue classValue = m_classValues.get(classVal);
            cells.add(new IntCell(classValue.getNoOfNotMissingRows()));
            if (!ignoreMissing) {
                cells.add(new IntCell(classValue.getNoOfMissingValueRecs()));
            }
            cells.add(new DoubleCell(classValue.getMean()));
            cells.add(new DoubleCell(classValue.getStdDeviation()));
            dc.addRowToTable(
                new DefaultRow(RowKey.createRowKey(rowId.getAndIncrement()), cells.toArray(new DataCell[0])));
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
        final String missingHeader = getMissingValueHeader(getClassValues());
        final NumberFormat nf = NumberFormat.getPercentInstance();
        //create the value rows
        final StringBuilder countRow = new StringBuilder();
        final StringBuilder meanRow = new StringBuilder();
        final StringBuilder stdDevRow = new StringBuilder();
        final StringBuilder rateRow = new StringBuilder();
        final StringBuilder missingRow = new StringBuilder();
        for (final String classVal : sortedClassVal) {
            final NumericalClassValue classValue = m_classValues.get(classVal);
            countRow.append("<td align='center'>");
            countRow.append(classValue.getNoOfNotMissingRows());
            countRow.append("</td>");

            meanRow.append("<td align='center'>");
            meanRow.append(NaiveBayesModel.HTML_VALUE_FORMATER.format(classValue.getMean()));
            meanRow.append("</td>");

            stdDevRow.append("<td align='center'>");
            stdDevRow.append(NaiveBayesModel.HTML_VALUE_FORMATER.format(classValue.getStdDeviation()));
            stdDevRow.append("</td>");

            rateRow.append("<td align='center'>");
            rateRow.append(nf.format(classValue.getNoOfRows() / (double)totalNoOfRecs));
            rateRow.append("</td>");

            if (missingHeader != null) {
                missingRow.append("<td align='center'>");
                missingRow.append(classValue.getNoOfMissingValueRecs());
                missingRow.append("</td>");
            }
        }

        final StringBuilder buf = new StringBuilder();
        buf.append("<table width='100%'>");
        buf.append(createTableHeader(" ", sortedClassVal, null));
        //append the count row
        buf.append("<tr>");
        buf.append("<th>");
        buf.append("Count:");
        buf.append("</th>");
        buf.append(countRow);
        buf.append("</tr>");
        //append the mean row
        buf.append("<tr>");
        buf.append("<th>");
        buf.append("Mean:");
        buf.append("</th>");
        buf.append(meanRow);
        buf.append("</tr>");
        //append the Std. Deviation row
        buf.append("<tr>");
        buf.append("<th>");
        buf.append("Std. Deviation:");
        buf.append("</th>");
        buf.append(stdDevRow);
        buf.append("</tr>");

        //append the missing val row
        if (missingHeader != null) {
            buf.append("<tr>");
            buf.append("<th>");
            buf.append("Missing values:");
            buf.append("</th>");
            buf.append(missingRow);
            buf.append("</tr>");
        }

        //append the rate row
        buf.append("<tr>");
        buf.append("<th>");
        buf.append("Rate:");
        buf.append("</th>");
        buf.append(rateRow);
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
        for (final NumericalClassValue classVal : m_classValues.values()) {
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
        final NumericalClassValue classModel = m_classValues.get(classValue);
        if (classModel == null) {
            return logProbThreshold;
        }
        return classModel.getLogProbability(attributeValue, logProbThreshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean hasRecs4ClassValue(final String classValue) {
        final NumericalClassValue classModel = m_classValues.get(classValue);
        if (classModel == null) {
            return false;
        }
        return true;
    }
}

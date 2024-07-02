/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 */

package org.knime.core.data.statistics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowReadUtil;
import org.knime.core.data.v2.TableExtractorUtil.Extractor;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.util.Pair;

/**
 * This class serves as a collection of classes implementing {@link Extractor} which extract statistics out of the
 * columns of a {@link BufferedDataTable}.
 *
 * @author Paul Bärnreuther
 * @since 5.1
 * @noextend Not public API, for internal use only.
 * @noreference Not public API, for internal use only.
 */
public final class StatisticsExtractors {

    private StatisticsExtractors() {

    }

    /**
     * This extractor counts the occurrences of values in a given DoubleCell column which are valid, i.e. not missing
     * and not NaN.
     *
     * @author Paul Bärnreuther
     */
    public static final class ValidDoublesCounter implements Extractor {

        private final int m_freqIndex;
        private int m_numObs;

        /**
         * @param freqIndex the column to be read from. It needs to be compatible with {@link DoubleValue}.
         */
        public ValidDoublesCounter(final int freqIndex) {
            m_freqIndex = freqIndex;
        }

        @Override
        public void init(final int size) {
            m_numObs = 0;

        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            final var value = RowReadUtil.readDoubleValue(row, m_freqIndex);
            if (value != null && !Double.isNaN(value)) {
                m_numObs += 1;
            }
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{m_freqIndex};
        }

        /**
         * @return the number of valid values in the given column.
         */
        public int getOutput() {
            return m_numObs;
        }
    }

    /**
     *
     * @author Paul Bärnreuther
     */
    public static final class IntSumExtractor implements Extractor {

        private final int m_colIndex;
        private int m_sum;

        /**
         * @param colIndex the index of the column over which the sum is to be taken.
         */
        public IntSumExtractor(final int colIndex) {
            m_colIndex = colIndex;
        }

        @Override
        public void init(final int size) {
            m_sum = 0;
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            m_sum += RowReadUtil.readPrimitiveIntValue(row, m_colIndex);
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{m_colIndex};
        }

        /**
         * @return the computed sum
         */
        public int getOutput() {
            return m_sum;
        }
    }

    /**
     *
     * @author Juan Diaz Baquero
     */
    public static class DoubleSumExtractor implements Extractor {

        private final int m_colIndex;
        private double m_sum;

        /**
         * @param colIndex the index of the column over which the sum is to be taken.
         */
        public DoubleSumExtractor(final int colIndex) {
            m_colIndex = colIndex;
        }

        @Override
        public void init(final int size) {
            m_sum = 0;
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            m_sum += RowReadUtil.readPrimitiveDoubleValue(row, m_colIndex);
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{m_colIndex};
        }

        /**
         * @return the computed sum
         */
        public double getOutput() {
            return m_sum;
        }
    }

    /**
     * Counts the number of missing values in the column specified in the constructor
     *
     * @author Rupert Ettrich
     */
    public static class CountMissingValuesExtractor implements Extractor {

        private final int m_colIndex;
        private long m_count;

        /**
         *
         * @param colIndex the column for which missing values are to be counted
         */
        public CountMissingValuesExtractor(final int colIndex) {
            m_colIndex = colIndex;
        }

        @Override
        public void init(final int size) {
            m_count = 0;
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            if (row.isMissing(m_colIndex)) {
                m_count += 1;
            }
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{m_colIndex};
        }

        /**
         * @return the number of missing values in the specified column
         */
        public long getOutput() {
            return m_count;
        }

    }

    public static class CountUniqueExtractor implements Extractor {

        private final LinkedHashMap<DataValue, Long> m_groups = new LinkedHashMap<>();

        @Override
        public void init(final int size) {
            // no-op
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            m_groups.merge( //
                row.isMissing(0) ? null : row.getValue(0).materializeDataCell(), //
                1L, //
                (prevValue, value) -> prevValue + 1 //
            );
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{0};
        }

        public long getNumberOfUniqueValues() {
            return m_groups.size();
        }

        @SuppressWarnings("java:S3958") // false positive on stream
        public List<Pair<DataValue, Long>> getMostFrequentValues(final int numValues) {
            return m_groups.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())) //
                .limit(numValues) //
                .map(e -> new Pair<>(e.getKey(), e.getValue())) //
                .toList();
        }
    }

    /**
     * This extractor extracts a quantile from a (already) sorted table without missing values. If the computed rank of
     * the quantile lies between two points in the data, linear interpolation is used.
     *
     * @author Paul Bärnreuther
     */
    public static class QuantileExtractor implements Extractor {

        private final int m_colIndex;

        private final double m_quantile;

        private int m_counter;

        private int m_integerPosition;

        private double m_fractionalPartOfPosition;

        private Double m_result;

        /**
         * The extractor computes the {@code n}th quantile of order {@code p} of a given column. The column needs to be
         * sorted in ascending order and contain only double values for this.
         *
         * Algorithm according to the javadoc of org.apache.commons.math3.stat.descriptive.rank.Percentile.
         *
         * @param colIndex the index of the column from which the quantile is extracted.
         * @param n the index of the quantile
         * @param p the order of the quantile
         */
        public QuantileExtractor(final int colIndex, final int n, final int p) {
            m_colIndex = colIndex;
            m_quantile = n / (double)p;
        }

        @Override
        public void init(final int size) {
            m_result = null;
            if (size == 1) {
                m_integerPosition = 0;
                return;
            }
            var quantilePosition = m_quantile * (size + 1);
            if(quantilePosition < 1) {
                m_integerPosition = 0;
                return;
            }
            if (quantilePosition >= size) {
                m_integerPosition = size - 1;
                return;
            }
            m_fractionalPartOfPosition = quantilePosition - Math.floor(quantilePosition);
            m_integerPosition = (int)Math.floor(quantilePosition);
            m_counter = 1;
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            if (m_counter == m_integerPosition) {
                m_result = RowReadUtil.readPrimitiveDoubleValue(row, m_colIndex);
            } else if (m_counter == m_integerPosition + 1 && m_fractionalPartOfPosition > 0) {
                m_result +=
                    m_fractionalPartOfPosition * (RowReadUtil.readPrimitiveDoubleValue(row, m_colIndex) - m_result);
            }
            m_counter++;
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{m_colIndex};
        }

        /**
         * @return the computed sum
         */
        public Double getOutput() {
            return m_result;
        }
    }

    /**
     * @author Paul Bärnreuther
     */
    public static final class MedianExtractor extends QuantileExtractor {

        /**
         * @param colIndex the index of the column from which the median is extracted.
         */
        public MedianExtractor(final int colIndex) {
            super(colIndex, 1, 2);
        }
    }

    /**
     * @author Paul Bärnreuther
     */
    public static final class FirstQuartileExtractor extends QuantileExtractor {

        /**
         * @param colIndex the index of the column from which the median is extracted.
         */
        public FirstQuartileExtractor(final int colIndex) {
            super(colIndex, 1, 4);
        }
    }

    /**
     * @author Paul Bärnreuther
     */
    public static final class ThirdQuartileExtractor extends QuantileExtractor {

        /**
         * @param colIndex the index of the column from which the median is extracted.
         */
        public ThirdQuartileExtractor(final int colIndex) {
            super(colIndex, 3, 4);
        }
    }

    /**
     * @author Paul Bärnreuther
     */
    public static final class MinimumExtractor extends QuantileExtractor {

        /**
         * @param colIndex the index of the column from which the median is extracted.
         */
        public MinimumExtractor(final int colIndex) {
            super(colIndex, 0, 1);
        }
    }

    /**
     * @author Paul Bärnreuther
     */
    public static final class MaximumExtractor extends QuantileExtractor {

        /**
         * @param colIndex the index of the column from which the median is extracted.
         */
        public MaximumExtractor(final int colIndex) {
            super(colIndex, 1, 1);
        }
    }

    /**
     *
     * @author Juan Diaz Baquero
     */
    public static final class MeanExtractor extends DoubleSumExtractor {

        private int m_nObs;

        /**
         * @param colIndex
         */
        public MeanExtractor(final int colIndex) {
            super(colIndex);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void init(final int size) {
            super.init(size);
            m_nObs = size;
        }

        @Override
        public double getOutput() {
            return super.getOutput() / m_nObs;
        }
    }

    /**
     *
     * @author Juan Diaz Baquero
     */
    public static class MeanAbsoluteDeviation implements Extractor {

        private final int m_colIndex;

        private final double m_mean;

        private double m_sum;

        private int m_nObs;

        /**
         * @param colIndex the index of the column over which the sum is to be taken.
         * @param mean
         */
        public MeanAbsoluteDeviation(final int colIndex, final double mean) {
            m_colIndex = colIndex;
            m_mean = mean;
        }

        @Override
        public void init(final int size) {
            m_nObs = size;
            m_sum = 0;
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            m_sum += Math.abs(RowReadUtil.readPrimitiveDoubleValue(row, m_colIndex) - m_mean);
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{m_colIndex};
        }

        /**
         * @return the computed sum
         */
        public double getOutput() {
            return m_sum / m_nObs;
        }
    }

    /**
     *
     * @author Juan Diaz Baquero
     */
    public static class CentralMomentExtractor implements Extractor {

        private final int m_colIndex;

        private final double m_mean;
        /**
         * Cumulative sum
         */
        protected double m_sum;
        /**
         * sample size
         */
        protected int m_nObs;
        private final int m_moment;

        /**
         * @param colIndex the index of the column over which the central moment is to be taken.
         * @param mean
         * @param moment
         */
        public CentralMomentExtractor(final int colIndex, final double mean, final int moment) {
            m_colIndex = colIndex;
            m_mean = mean;
            m_moment = moment;
        }

        @Override
        public void init(final int size) {
            m_nObs = size;
            m_sum = 0;
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            m_sum += Math.pow(RowReadUtil.readPrimitiveDoubleValue(row, m_colIndex) - m_mean, m_moment);
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{m_colIndex};
        }

        /**
         * @return the computed sum
         */
        public double getOutput() {
            return m_sum / m_nObs;
        }
    }

    /**
     *
     * @author Juan Diaz Baquero
     */
    public static class VarianceExtractor extends CentralMomentExtractor {
        private int m_ddof;

        /**
         * An extractor extracting the sample variance of a column. For changing the ddof of this extractor, use the
         * other constructor.
         *
         * @param colIndex the index of the column over which the central moment is to be taken.
         * @param mean
         */
        public VarianceExtractor(final int colIndex, final double mean) {
            super(colIndex, mean, 2);
            m_ddof = 1;
        }

        /**
         * @param colIndex the index of the column over which the central moment is to be taken.
         * @param mean
         * @param ddof the delta degrees of freedom. See
         *            <a href= "https://numpy.org/doc/stable/reference/generated/numpy.var.html">numpy.var</a> for more
         *            information.
         */
        public VarianceExtractor(final int colIndex, final double mean, final int ddof) {
            this(colIndex, mean);
            m_ddof = ddof;
        }

        /**
         * @return the computed sum
         */
        @Override
        public double getOutput() {
            if (m_nObs <= m_ddof) {
                return Double.NaN;
            }
            return m_sum / (m_nObs - m_ddof);
        }
    }

    /**
     *
     * @author Juan Diaz Baquero
     */
    public static final class StandardDeviationExtractor extends VarianceExtractor {

        /**
         * An extractor extracting the sample standard deviation of a column. For changing the ddof of this extractor,
         * use the other constructor.
         *
         * @param colIndex
         * @param mean
         */
        public StandardDeviationExtractor(final int colIndex, final double mean) {
            super(colIndex, mean);
        }

        /**
         * @param colIndex
         * @param mean
         * @param ddof the delta degrees of freedom. See
         *            <a href= "https://numpy.org/doc/stable/reference/generated/numpy.std.html">numpy.std</a> for more
         *            information.
         */
        public StandardDeviationExtractor(final int colIndex, final double mean, final int ddof) {
            super(colIndex, mean, ddof);
        }

        @Override
        public double getOutput() {
            return Math.sqrt(super.getOutput());
        }
    }

    /**
     *
     * @author Juan Diaz Baquero
     */
    public static class SkewnessExtractor extends CentralMomentExtractor {

        private final double m_stdDeviation;

        /**
         * @param colIndex the index of the column over which the central moment is to be taken.
         * @param mean
         * @param stdDeviation
         */
        public SkewnessExtractor(final int colIndex, final double mean, final double stdDeviation) {
            super(colIndex, mean, 3);
            m_stdDeviation = stdDeviation;
        }

        /**
         * @return the computed sum
         */
        @Override
        public double getOutput() {
            return super.getOutput() / Math.pow(m_stdDeviation, 3);
        }
    }

    /**
     * Estimates unbiased Kurtosis. see https://cran.r-project.org/web/packages/e1071/e1071.pdf
     *
     * @author Juan Diaz Baquero
     */
    public static class KurtosisExtractor extends CentralMomentExtractor {

        private final double m_biasedVariance;

        /**
         * @param colIndex the index of the column over which the central moment is to be taken.
         * @param mean
         * @param biasedVariance
         */
        public KurtosisExtractor(final int colIndex, final double mean, final double biasedVariance) {
            super(colIndex, mean, 4);
            m_biasedVariance = biasedVariance;
        }

        /**
         * @return the computed sum
         */
        @Override
        public double getOutput() {
            var g2 = super.getOutput() / Math.pow(m_biasedVariance, 2) - 3;
            return ((m_nObs + 1) * g2 + 6) * (m_nObs - 1) / ((m_nObs - 2) * (m_nObs - 3));
        }
    }

}

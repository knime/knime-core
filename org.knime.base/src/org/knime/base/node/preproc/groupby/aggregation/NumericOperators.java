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
 *    18.07.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby.aggregation;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;


/**
 * Contains all numerical aggregation operators.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public final class NumericOperators {

    private static NumericOperators instance;

    private NumericOperators() {
        //avoid object creation
    }

    /**
     * Returns the only instance of this class.
     * @return the only instance
     */
    public static NumericOperators getInstance() {
        if (instance == null) {
            instance = new NumericOperators();
        }
        return instance;
    }



    /**
     * Returns the minimum per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class MinOperator extends AggregationOperator {

        private DataCell m_minVal = null;

        /**Constructor for class MinOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        MinOperator(final int maxUniqueValues) {
            super("Minimum", true, "Min(" + PLACE_HOLDER + ")", false, true,
                    maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return origType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(final int maxUniqueValues) {
            return new MinOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            if (m_minVal == null
                    || cell.getType().getComparator().compare(cell, m_minVal)
                        < 0) {
                m_minVal = cell;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            if (m_minVal == null) {
                return DataType.getMissingCell();
            }
            return m_minVal;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_minVal = null;
        }
    }

    /**
     * Returns the maximum per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class MaxOperator extends AggregationOperator {

        private DataCell m_maxVal = null;

        /**Constructor for class MinOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        MaxOperator(final int maxUniqueValues) {
            super("Maximum", true, "Max(" + PLACE_HOLDER + ")", false, true,
                    maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return origType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(final int maxUniqueValues) {
            return new MaxOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            if (m_maxVal == null
                    || cell.getType().getComparator().compare(cell, m_maxVal)
                    > 0) {
                m_maxVal = cell;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            if (m_maxVal == null) {
                return DataType.getMissingCell();
            }
            return m_maxVal;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_maxVal = null;
        }
    }

    /**
     * Returns the mean per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class MeanOperator extends AggregationOperator {

        private final DataType m_type = DoubleCell.TYPE;
        private double m_sum = 0;
        private int m_count = 0;

        /**Constructor for class MinOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        MeanOperator(final int maxUniqueValues) {
            super("Mean", true, "Mean(" + PLACE_HOLDER + ")", false, false,
                    maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return m_type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(final int maxUniqueValues) {
            return new MeanOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            final double d = ((DoubleValue)cell).getDoubleValue();
            m_sum += d;
            m_count++;
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            if (m_count == 0) {
                return DataType.getMissingCell();
            }
            return new DoubleCell(m_sum / m_count);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_sum = 0;
            m_count = 0;
        }
    }

    /**
     * Returns the sum per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class SumOperator extends AggregationOperator {

        private final DataType m_type = DoubleCell.TYPE;
        private boolean m_valid = false;
        private double m_sum = 0;

        /**Constructor for class MinOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        SumOperator(final int maxUniqueValues) {
            super("Sum", true, "Sum(" + PLACE_HOLDER + ")", false, false,
                    maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return m_type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(final int maxUniqueValues) {
            return new SumOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            m_valid = true;
            final double d = ((DoubleValue)cell).getDoubleValue();
            m_sum += d;
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            if (!m_valid) {
                return DataType.getMissingCell();
            }
            return new DoubleCell(m_sum);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_valid = false;
            m_sum = 0;
        }
    }

    /**
     * Returns the variance per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    class VarianceOperator extends AggregationOperator {

        private final DataType m_type = DoubleCell.TYPE;

        private double m_sumSquare = 0;
        private double m_sum = 0;
        private int m_validCount = 0;

        /**Constructor for class VarianceOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        VarianceOperator(final int maxUniqueValues) {
            super("Variance", true, "Variance(" + PLACE_HOLDER + ")",
                    false, false, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return m_type;
        }

        /**Constructor for class NumericOperators.VarianceOperator.
         * @param label user readable label
         * @param numerical <code>true</code> if the operator is only suitable
         * for numerical columns
         * @param columnNamePattern the pattern for the result column name
         * @param usesLimit <code>true</code> if the method checks the number of
         * unique values limit.
         * @param keepColSpec <code>true</code> if the original column
         * specification should be kept if possible
         * @param maxUniqueValues the maximum number of unique values
         */
        VarianceOperator(final String label, final boolean numerical,
                final String columnNamePattern, final boolean usesLimit,
                final boolean keepColSpec, final int maxUniqueValues) {
            super(label, numerical, columnNamePattern, usesLimit, keepColSpec,
                    maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(final int maxUniqueValues) {
            return new VarianceOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            final double d = ((DoubleValue)cell).getDoubleValue();
            m_validCount++;
            m_sum += d;
            m_sumSquare += d * d;
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            if (m_validCount <= 0) {
                return DataType.getMissingCell();
            }
            if (m_validCount == 1) {
                return new DoubleCell(0);
            }
            double variance = (m_sumSquare - ((m_sum * m_sum)
                    / m_validCount)) / (m_validCount - 1);
            // unreported bug fix: in cases in which a column contains
            // almost only one value (for instance 1.0) but one single
            // 'outlier' whose value is, for instance 0.9999998, we get
            // round-off errors resulting in negative variance values
            if (variance < 0.0 && variance > -1.0E8) {
                variance = 0.0;
            }
            return new DoubleCell(variance);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resetInternal() {
            m_sumSquare = 0;
            m_sum = 0;
            m_validCount = 0;
        }

    }

    /**
     * Returns the standard deviation per group.
     *
     * @author Tobias Koetter, University of Konstanz
     */
    final class StdDeviationOperator extends VarianceOperator {

        /**Constructor for class StdDeviationOperator.
         * @param maxUniqueValues the maximum number of unique values
         */
        StdDeviationOperator(final int maxUniqueValues) {
            super("Standard deviation", true,
                    "Standard deviation(" + PLACE_HOLDER + ")",
                    false, false, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(final int maxUniqueValues) {
            return new StdDeviationOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            final DataCell result = super.getResult();
            if (result instanceof DoubleCell) {
                final double value = ((DoubleCell)result).getDoubleValue();
                return new DoubleCell(Math.sqrt(Math.abs(value)));
            }
            return result;
        }
    }
}

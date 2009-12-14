/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *    18.07.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby.aggregation;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
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
        private final DataValueComparator m_comparator;

        /**Constructor for class MinOperator.
         * @param origColSpec the {@link DataColumnSpec} of the original column
         * @param maxUniqueValues the maximum number of unique values
         */
        MinOperator(final DataColumnSpec origColSpec,
                final int maxUniqueValues) {
            super("Minimum", "Min", true, false, true,
                    maxUniqueValues);
            if (origColSpec == null) {
                //this could only happen in the enumeration definition
                m_comparator = DoubleCell.TYPE.getComparator();
            } else {
                m_comparator = origColSpec.getType().getComparator();
            }
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
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            if (origColSpec == null) {
                throw new NullPointerException("origColSpec must not be null");
            }
            return new MinOperator(origColSpec, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            if (m_minVal == null || m_comparator.compare(cell, m_minVal)
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

        private final DataValueComparator m_comparator;

        /**Constructor for class MinOperator.
         * @param origColSpec the {@link DataColumnSpec} of the original column
         * @param maxUniqueValues the maximum number of unique values
         */
        MaxOperator(final DataColumnSpec origColSpec,
                final int maxUniqueValues) {
            super("Maximum", "Max", true, false, true,
                    maxUniqueValues);
            if (origColSpec == null) {
                //this could only happen in the enumeration definition
                m_comparator = DoubleCell.TYPE.getComparator();
            } else {
                m_comparator = origColSpec.getType().getComparator();
            }
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
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            if (origColSpec == null) {
                throw new NullPointerException("origColSpec must not be null");
            }
            return new MaxOperator(origColSpec, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean computeInternal(final DataCell cell) {
            if (cell.isMissing()) {
                return false;
            }
            if (m_maxVal == null || m_comparator.compare(cell, m_maxVal)
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
            super("Mean", true, false, false,
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
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
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
            super("Sum", true, false, false,
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
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
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
            super("Variance", true, false, false, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataType getDataType(final DataType origType) {
            return m_type;
        }

        /**Constructor for class NumericOperators.VarianceOperator.
         * @param label user readable label which is also used for the
         * column name
         * @param numerical <code>true</code> if the operator is only suitable
         * for numerical columns
         * @param usesLimit <code>true</code> if the method checks the number of
         * unique values limit.
         * @param keepColSpec <code>true</code> if the original column
         * specification should be kept if possible
         * @param maxUniqueValues the maximum number of unique values
         */
        VarianceOperator(final String label, final boolean numerical,
                final boolean usesLimit, final boolean keepColSpec,
                final int maxUniqueValues) {
            super(label, numerical, usesLimit, keepColSpec, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
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
            super("Standard deviation", true, false, false, maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AggregationOperator createInstance(
                final DataColumnSpec origColSpec, final int maxUniqueValues) {
            return new StdDeviationOperator(maxUniqueValues);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell getResultInternal() {
            final DataCell result = super.getResultInternal();
            if (result instanceof DoubleCell) {
                final double value = ((DoubleCell)result).getDoubleValue();
                return new DoubleCell(Math.sqrt(Math.abs(value)));
            }
            return result;
        }
    }
}

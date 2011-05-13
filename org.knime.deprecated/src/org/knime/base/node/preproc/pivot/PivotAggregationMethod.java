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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   15.06.2007 (gabriel): created
 */
package org.knime.base.node.preproc.pivot;

import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;

/**
 * Factory holding a set of aggregation functions, such as COUNT, SUM,
 * MEAN, MAX, MIN, VARIANCE, and STD DEVIATION methods. The computation of these
 * values is done by an array reference ({@link #init()} which in modify by next
 * value ({@link #compute(Double[], DataCell)} of one group, and is finally 
 * committed ({@link #done(Double[])}. Each element within the array reference 
 * can be used as a place holder for one value, e.g. sum and number of elements.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
abstract class PivotAggregationMethod {
    
    /**
     * Inits the array used for aggregation. 
     * @return a new array with length needed for aggregation value computation
     */
    abstract Double[] init();
    
    /**
     * Uses the array returned by {@link #init()} to compute the next value.
     * @param agg the array to be modified by this call 
     * @param value the value to apply
     */
    abstract void compute(final Double[] agg, final DataCell value);
    
    /**
     * Finally commits the aggregation value and computes the final output.
     * @param agg the array used for value computation
     * @return the final aggregation value
     */
    abstract DataCell done(final Double[] agg);
    
    /**
     * Method only counts the number of non-empty and non-missing occurrences. 
     */
    static final PivotAggregationMethod COUNT = new PivotAggregationMethod() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Double[] init() {
            return new Double[]{0.0};
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void compute(final Double[] agg, final DataCell value) {
            agg[0] += 1;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell done(final Double[] agg) {
            if (agg == null) {
                return new IntCell(0);
            } else {
                return new IntCell((int) agg[0].doubleValue());
            }
        }
    };

    /**
     * Method computes the sum over all values.
     */
    static final PivotAggregationMethod SUM = new PivotAggregationMethod() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Double[] init() {
            return new Double[]{0.0};
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void compute(final Double[] agg, final DataCell value) {
            if (value == null || value.isMissing()) {
                return;
            }
            if (value.getType().isCompatible(DoubleValue.class)) {
                agg[0] += ((DoubleValue) value).getDoubleValue();
            }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell done(final Double[] agg) {
            if (agg == null) {
                return DataType.getMissingCell();
            } else {
                return new DoubleCell(agg[0]);
            }
        }
    };

    /**
     * Method computes the mean over all values.
     */
    static final PivotAggregationMethod MEAN = new PivotAggregationMethod() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Double[] init() {
            return new Double[]{0.0, 0.0};
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void compute(final Double[] agg, final DataCell value) {
            if (value == null || value.isMissing()) {
                return;
            }
            if (value.getType().isCompatible(DoubleValue.class)) {
                agg[0] += ((DoubleValue) value).getDoubleValue();
                agg[1]++;
            }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell done(final Double[] agg) {
            if (agg != null && agg[1] > 0) {
                return new DoubleCell(agg[0] / agg[1]);
            } else {
                return DataType.getMissingCell();
            }
        }
    };

    /**
     * Method returns the maximum value over all values.
     */
    static final PivotAggregationMethod MAX = new PivotAggregationMethod() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Double[] init() {
            return new Double[]{null};
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void compute(final Double[] agg, final DataCell value) {
            if (value == null || value.isMissing()) {
                return;
            }
            if (value.getType().isCompatible(DoubleValue.class)) {
                double d = ((DoubleValue) value).getDoubleValue();
                if (agg[0] == null) {
                    agg[0] = d;
                } else {
                    if (d > agg[0]) {
                        agg[0] = d;
                    }
                }
            }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell done(final Double[] agg) {
            if (agg == null || agg[0] == null) {
                return DataType.getMissingCell();
            } else {
                return new DoubleCell(agg[0]);
            }
        }
    };

    /**
     * Method returns the minimum over all values.
     */
    static final PivotAggregationMethod MIN = new PivotAggregationMethod() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Double[] init() {
            return new Double[]{null};
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void compute(final Double[] agg, final DataCell value) {
            if (value == null || value.isMissing()) {
                return;
            }
            if (value.getType().isCompatible(DoubleValue.class)) {
                double d = ((DoubleValue) value).getDoubleValue();
                if (agg[0] == null) {
                    agg[0] = d;
                } else {
                    if (d < agg[0]) {
                        agg[0] = d;
                    }
                }
            }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell done(final Double[] agg) {
            if (agg == null || agg[0] == null) {
                return DataType.getMissingCell();
            } else {
                return new DoubleCell(agg[0]);
            }
        }
    };

    /**
     * Method computes the variance over all values.
     */
    static final PivotAggregationMethod VARIANCE = 
            new PivotAggregationMethod() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Double[] init() {
            return new Double[]{0.0, 0.0, 0.0};
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void compute(final Double[] agg, final DataCell value) {
            if (value == null || value.isMissing()) {
                return;
            }
            if (value.getType().isCompatible(DoubleValue.class)) {
                double d = ((DoubleValue) value).getDoubleValue();
                agg[0] += d;
                agg[1] += (d * d);
                agg[2]++;
            }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell done(final Double[] agg) {
            if (agg != null && agg[2] > 0) {
                return new DoubleCell(
                        agg[1] / agg[2] - (agg[0] * agg[0] / agg[2] / agg[2]));
            } else {
                return DataType.getMissingCell();
            }
        }
    };

    /**
     * Method computes the variance over all values.
     */
    static final PivotAggregationMethod STDDEV = 
            new PivotAggregationMethod() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Double[] init() {
            return new Double[]{0.0, 0.0, 0.0};
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void compute(final Double[] agg, final DataCell value) {
            if (value == null || value.isMissing()) {
                return;
            }
            if (value.getType().isCompatible(DoubleValue.class)) {
                double d = ((DoubleValue) value).getDoubleValue();
                agg[0] += d;
                agg[1] += (d * d);
                agg[2]++;
            }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell done(final Double[] agg) {
            if (agg != null && agg[2] > 0) {
                return new DoubleCell(Math.sqrt(
                        agg[1] / agg[2] - (agg[0] * agg[0] / agg[2] / agg[2])));
            } else {
                return DataType.getMissingCell();
            }
        }
    };
    
    /**
     * Set of methods used to compute the aggregation value for one column.
     */
    static final Map<String, PivotAggregationMethod> METHODS =
        new LinkedHashMap<String, PivotAggregationMethod>();
    static {
        METHODS.put("SUM", SUM);
        METHODS.put("MEAN", MEAN);
        METHODS.put("MAXIMUM", MAX);
        METHODS.put("MINIMUM", MIN);
        METHODS.put("VARIANCE", VARIANCE);
        METHODS.put("STD DEVIATION", STDDEV);
    }
    
}

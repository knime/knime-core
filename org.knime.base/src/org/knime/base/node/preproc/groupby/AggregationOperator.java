/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *    29.06.2007 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;


/**
 * Abstract class which has to be extended by all aggregation method operators
 * in the {@link AggregationMethod} enumeration to be used in
 * the {@link GroupByTable} class.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AggregationOperator {

    private final int m_maxUniqueValues;

    private boolean m_skipped;

    /**Constructor for class AggregationOperator.
     * @param maxUniqueValues the maximum number of unique values
     */
    public AggregationOperator(final int maxUniqueValues) {
        m_maxUniqueValues = maxUniqueValues;
    }

    /**
     * Creates a new instance of this operator.
     *
     * @param maxUniqueValues the maximum number of unique values
     * @return a new instance of this operator
     */
    public abstract AggregationOperator createInstance(
            final int maxUniqueValues);

    /**
     * @return the maxUniqueValues
     */
    public int getMaxUniqueValues() {
        return m_maxUniqueValues;
    }

    /**
     * @return <code>true</code> if this operator was skipped
     */
    public boolean isSkipped() {
        return m_skipped;
    }

    /**
     * @param cell the {@link DataCell} to consider during computing
     */
    public void compute(final DataCell cell) {
        if (m_skipped) {
            return;
        }
        m_skipped = computeInternal(cell);
    }

    /**
     * @param cell the {@link DataCell} to consider during computing the cell
     * can't be <code>null</code>.
     * @return <code>true</code> if this column should be skipped in further
     * calculations
     */
    protected abstract boolean computeInternal(final DataCell cell);

    /**
     * @return the result {@link DataCell}
     */
    public DataCell getResult() {
        if (m_skipped) {
            return DataType.getMissingCell();
        }
        return getResultInternal();
    }

    /**
     * @return the result {@link DataCell}
     */
    protected abstract DataCell getResultInternal();

    /**
     * Should reset the operator to the start values.
     */
    public void reset() {
        m_skipped = false;
        resetInternal();
    }

    /**
     * Should reset the operator to the start values.
     */
    protected abstract void resetInternal();
}

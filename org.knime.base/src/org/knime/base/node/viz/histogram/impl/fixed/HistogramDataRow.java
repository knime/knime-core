/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   14.08.2006 (koetter): created
 */
package org.knime.base.node.viz.histogram.impl.fixed;

import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;

/**
 * Saves all information which is needed per row to create a histogram plotter. 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramDataRow {
    private final ColorAttr m_color;

    private final DataCell m_xVal;

    private final DataCell m_aggrVal;

    private final RowKey m_rowKey;

    /**Constructor for class HistogramDataRow.
     * @param key the row key
     * @param color the color attribute
     * @param xVal the value of the selected x column
     * @param aggrVal the value of the selected aggregation column
     */
    public HistogramDataRow(final RowKey key, final ColorAttr color,
            final DataCell xVal, final DataCell aggrVal) {
        m_rowKey = key;
        m_color = color;
        m_xVal = xVal;
        m_aggrVal = aggrVal;
    }

    /**
     * @return the aggrVal
     */
    protected DataCell getAggrVal() {
        return m_aggrVal;
    }

    /**
     * @return the color
     */
    protected ColorAttr getColor() {
        return m_color;
    }

    /**
     * @return the rowKey
     */
    protected RowKey getRowKey() {
        return m_rowKey;
    }

    /**
     * @return the xVal
     */
    protected DataCell getXVal() {
        return m_xVal;
    }
}

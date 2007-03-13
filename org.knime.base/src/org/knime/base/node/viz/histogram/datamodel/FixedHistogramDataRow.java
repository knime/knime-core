/*
 * ------------------------------------------------------------------
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
 *   14.08.2006 (koetter): created
 */
package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;

import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;

/**
 * Saves all information which is needed per row to create a histogram plotter. 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedHistogramDataRow {
    private final Color m_color;

    private final DataCell m_xVal;

    private final DataCell[] m_aggrVal;

    private final RowKey m_rowKey;

    /**Constructor for class HistogramDataRow.
     * @param key the row key
     * @param rowColor the color attribute
     * @param xVal the value of the selected x column
     * @param aggrVal the value of the selected aggregation column
     */
    public FixedHistogramDataRow(final RowKey key, final Color rowColor,
            final DataCell xVal, final DataCell... aggrVal) {
        if (key == null) {
            throw new NullPointerException("Key must not be null");
        }
        if (rowColor == null) {
            throw new NullPointerException("Row color must not be null");
        }
        if (xVal == null) {
            throw new NullPointerException("X value must not be null");
        }
        if (aggrVal == null || aggrVal.length < 0) {
            throw new NullPointerException(
                    "Aggregation value must not be null");
        }
        m_rowKey = key;
        m_color = rowColor;
        m_xVal = xVal;
        m_aggrVal = aggrVal;
    }

    /**
     * @param index the index to retrieve
     * @return the aggrVal at the given index
     */
    public DataCell getAggrVal(final int index) {
        if (index >= m_aggrVal.length) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        return m_aggrVal[index];
    }
    
    /**
     * @return the aggregation values
     */
    public DataCell[] getAggrVals() {
        return m_aggrVal;
    }

    /**
     * @return the color
     */
    public Color getColor() {
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

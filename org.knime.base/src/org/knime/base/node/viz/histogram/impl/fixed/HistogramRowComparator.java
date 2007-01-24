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
 */
package org.knime.base.node.viz.histogram.impl.fixed;

import java.util.Comparator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValueComparator;

/**
 * Comparator used to sort {@link org.knime.core.data.DataRow}s by the value of
 * the row with the given index.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramRowComparator implements Comparator<HistogramDataRow> {

    private int m_o1Greater = 1;

    private int m_o1Smaller = -1;

    private int m_isEquals = 0;

    private final DataValueComparator m_nativeCellComparator;

    /**
     * Constructor for class RowByColumnComparator.
     * 
     * @param cellComp the <code>DataValueComparator</code> used to compare
     *            the cells of the column with the given index
     */
    public HistogramRowComparator(final DataValueComparator cellComp) {
        this.m_nativeCellComparator = cellComp;
    }

    /**
     * @return the <code>DataValueComparator</code> used to compare the cells
     *         with the index set in the constructor
     */
    public DataValueComparator getBasicComparator() {
        return this.m_nativeCellComparator;
    }

    /**
     * @param o1 row 1
     * @param o2 row 2
     * @return the result of the default comparator for the table cell with
     *         index set in the constructor
     */
    public int compare(final HistogramDataRow o1, final HistogramDataRow o2) {
        if (o1 == o2) {
            return m_isEquals;
        }
        if (o1 == null && o2 != null) {
            return m_o1Smaller;
        }
        if (o1 != null && o2 == null) {
            return m_o1Greater;
        }
        DataCell c1 = o1.getXVal();
        DataCell c2 = o2.getXVal();
        int result = this.m_nativeCellComparator.compare(c1, c2);
        return result;
    }
}

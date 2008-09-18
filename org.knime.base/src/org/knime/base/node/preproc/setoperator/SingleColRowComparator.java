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
 *    22.11.2007 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.setoperator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.def.StringCell;

import java.util.Comparator;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class SingleColRowComparator implements Comparator<DataRow> {

    private int m_colIdx = -1;
    private Comparator<DataCell> m_comp;

    /**Constructor for class SingleColRowComparator.
     * @param colIdx the index of the column to compare or -1 if the RowID
     * should be used
     * @param comp the {@link Comparator} to use
     */
    public SingleColRowComparator(final int colIdx,
            final DataValueComparator comp) {
        setColumnIndex(colIdx);
        setComparator(comp);

    }

    /**
     * @param comp the {@link Comparator} to use
     */
    public void setComparator(final Comparator<DataCell> comp) {
        if (comp == null) {
            throw new NullPointerException("Comparator must not be null");
        }
        m_comp = comp;
    }

    /**
     * @return the used {@link Comparator}
     */
    public Comparator<DataCell> getComparator() {
        return m_comp;
    }

    /**
     * @param colIdx the colIdx to set or -1 if the RowID should be used
     */
    public void setColumnIndex(final int colIdx) {
//        if (colIdx < 0) {
//            throw new IllegalArgumentException("Row index must be positive");
//        }
        m_colIdx = colIdx;
    }

    /**
     * {@inheritDoc}
     */
    public int compare(final DataRow r1, final DataRow r2) {
        if (m_colIdx < 0) {
            final String key1 = r1.getKey().getString();
            final String key2 = r2.getKey().getString();
            return compare(key1, key2, m_comp);
        } else {
            final DataCell cell1 = r1.getCell(m_colIdx);
            final DataCell cell2 = r2.getCell(m_colIdx);
            return compare(cell1, cell2, m_comp);
        }
    }

    /**
     * @param key1
     * @param key2
     * @param comp
     * @return
     */
    private int compare(final String c1, final String c2,
            final Comparator<DataCell> comp) {
        if (c1 == c2) {
            return 0;
        }
        if (c1 == null) {
            return 1;
        }
        if (c2 == null) {
            return -1;
        }
        return comp.compare(new StringCell(c1), new StringCell(c2));
    }

    /**
     * @param c1 first cell to compare
     * @param c2 second cell to compare
     * @param comp the comparator to use
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     */
    public static int compare(final DataCell c1, final DataCell c2,
            final Comparator<DataCell> comp) {
        if (c1 == c2) {
            return 0;
        }
        if (c1 == null) {
            return 1;
        }
        if (c2 == null) {
            return -1;
        }
        return comp.compare(c1, c2);
    }
}

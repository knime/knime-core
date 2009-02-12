/*
 * ---------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * ---------------------------------------------------------------------
 */
package org.knime.base.data.filter.column;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;


/**
 * Filter {@link DataRow} which extracts particular cells (columns) from an
 * underlying row.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class FilterColumnRow implements DataRow {

    /**
     * Underlying row.
     */
    private final DataRow m_row;

    /**
     * Array of column indices.
     */
    private final int[] m_columns;

    /**
     * Inits a new filter column {@link DataRow} with the underling row and an
     * array of indices into this row.
     * 
     * @param row the underlying {@link DataRow}
     * @param columns the array of column indices to keep
     */
    public FilterColumnRow(final DataRow row, final int[] columns) {
        m_row = row;
        m_columns = columns;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumCells() {
        return m_columns.length;
    }

    /**
     * {@inheritDoc}
     */
    public RowKey getKey() {
        return m_row.getKey();
    }

    /**
     * Returns the data cell at the given <code>index</code>.
     * 
     * @param index the column index inside the row
     * @return the data cell for index
     * @throws ArrayIndexOutOfBoundsException if the <code>index</code> is out
     *             of range
     */
    public DataCell getCell(final int index) {
        return m_row.getCell(m_columns[index]);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }
}

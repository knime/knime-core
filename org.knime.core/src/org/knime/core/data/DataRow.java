/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   09.01.2006(all): reviewed
 *   02.11.2006 (tm, cs): reviewed
 */
package org.knime.core.data;

/**
 * Container interface for a vector of {@link DataCell}s and a row key for
 * unique identification.
 * 
 * <p>
 * Each <code>DataRow</code> represents one row of a {@link DataTable} and
 * contains a fixed number of {@link DataCell} elements which are directly
 * accessible and read-only. In addition, each <code>DataRow</code> contains a
 * unique identifier key (which is not part of the data vector).
 * <p>
 * A <code>DataRow</code> must not contain a <code>null</code> element or a
 * <code>null</code> key.
 * 
 * <p>
 * This <code>DataRow</code> interface extends the {@link Iterable} interface
 * but does not allow the removal of {@link DataCell}s. Implementors must
 * therefore throw an {@link UnsupportedOperationException} in the Iterators
 * remove method.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see DataTable
 * @see DataCell
 * @see RowIterator
 * @see RowKey
 */
public interface DataRow extends Iterable<DataCell> {

    /**
     * Returns the length of this row, that is the number of columns of the
     * DataTable (not including the row key).
     * 
     * @return length of this row
     */
    int getNumCells();

    /**
     * Returns the row key.
     * 
     * @return the row key
     */
    RowKey getKey();

    /**
     * Returns the {@link DataCell} at the provided index within this row.
     * 
     * @param index the index of the cell to retrieve (indices start from 0)
     * @return the {@link DataCell} at the given index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    DataCell getCell(final int index);
}

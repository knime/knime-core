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
 *   29.10.2006(tm, cs): reviewed
 */
package org.knime.core.data;

/**
 * Most general data interface in table structure with a fixed number of columns
 * and iterable rows (no random access).
 * 
 * <p>
 * Each <code>DataTable</code> is a read-only container of {@link DataRow}
 * elements which are returned by the {@link RowIterator}. Each row must have
 * the same number of {@link DataCell} elements (columns), is read-only, and
 * must provide a unique row identifier. A table also contains a
 * {@link DataTableSpec} member which provides information about the structure
 * of the table. The {@link DataTableSpec} consists of {@link DataColumnSpec}s
 * which contain information about the column, e.g. name, type, and possible
 * values etc.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see DataCell
 * @see DataRow
 * @see RowIterator
 */
public interface DataTable extends Iterable<DataRow> {

    /**
     * Returns the {@link DataTableSpec} object of this table which gives
     * information about the structure of this data table.
     * 
     * @return the DataTableSpec of this table
     */
    DataTableSpec getDataTableSpec();

    /**
     * Returns a row iterator which returns each row one-by-one from the table.
     * 
     * @return row iterator
     * 
     * @see org.knime.core.data.DataRow
     */
    RowIterator iterator();

}

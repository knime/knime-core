/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   09.01.2006(all): reviewed
 */
package org.knime.core.data;

/**
 * Most general data interface in table structure format with a fixed number of
 * columns and ordered rows.
 * 
 * <p>
 * Each <code>DataTable</code> is a container of <code>DataRow</code>
 * elements which are returned by the <code>RowIterator</code>. Each row must
 * have the same number of <code>DataCell</code> elements (columns), is
 * read-only, and must provide a unique row identifier for each row element. A
 * table also contains a <code>DataTableSpec</code> member which provides
 * information about the structure of the table. For each column specifics like
 * name, type, and possible values etc. are kept in a <code>ColumnSpec</code>,
 * which can be retrieved from the <code>DataTableSpec</code> by column index.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see DataCell
 * @see DataRow
 * @see RowIterator
 */
public interface DataTable extends Iterable<DataRow> {

    /**
     * Returns the <code>DataTableSpec</code> object of this table which
     * specifies the structure of this data table.
     * 
     * @return the DataTableSpec of this table.
     */
    DataTableSpec getDataTableSpec();

    /**
     * Returns a row iterator which returns each row one-by-one from the table.
     * 
     * @return row iterator.
     * 
     * @see org.knime.core.data.DataRow
     */
    RowIterator iterator();

}

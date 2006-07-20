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
 *   29.06.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row;

import de.unikn.knime.base.node.filter.row.rowfilter.RowFilter;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.RowIterator;

/**
 * A DataTable which "contains" only rows that don't fall through the specified
 * filter. The table wrapps the original table and forwards only rows that meet
 * the filter criteria. Any <code>RowFilter</code> can be passed. It will
 * decide whether a row is part of this table or not.
 * 
 * @author ohl, University of Konstanz
 */
public class RowFilterTable implements DataTable {

    private final DataTable m_table;

    private final RowFilter m_filter;

    /**
     * Creates a new DataTable which contains only rows that are not filtered
     * out by the specified filter.
     * 
     * @param origTable the table to filter the rows from
     * @param filter a RowFilter that will be consulted for each row to decide
     *            whether to include it or not.
     */
    public RowFilterTable(
            final DataTable origTable, final RowFilter filter) {
        if (origTable == null) {
            throw new NullPointerException("Source table can't be null");
        }
        if (filter == null) {
            throw new NullPointerException("The row filter can't be null");
        }
        m_table = origTable;
        m_filter = filter;
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_table.getDataTableSpec();
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        return new RowFilterIterator(m_table, 
                (RowFilter)m_filter.clone());
    }
}

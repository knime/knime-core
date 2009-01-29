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
 * -------------------------------------------------------------------
 * 
 * History
 *   29.06.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;

/**
 * A {@link org.knime.core.data.DataTable} which "contains" only rows that
 * don't fall through the specified filter. The table wrapps the original table
 * and forwards only rows that meet the filter criteria. Any {@link RowFilter}
 * can be passed. It will decide whether a row is part of this table or not.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class RowFilterTable implements DataTable {

    private final DataTable m_table;

    private final RowFilter m_filter;

    /**
     * Creates a new data table which contains only rows that are not filtered
     * out by the specified filter.
     * 
     * @param origTable the table to filter the rows from
     * @param filter a row filter that will be consulted for each row to decide
     *            whether to include it or not
     */
    public RowFilterTable(final DataTable origTable, final RowFilter filter) {
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
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_table.getDataTableSpec();
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        return new RowFilterIterator(m_table, (RowFilter)m_filter.clone());
    }
}

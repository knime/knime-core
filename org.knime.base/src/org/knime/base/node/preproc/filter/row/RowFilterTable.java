/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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

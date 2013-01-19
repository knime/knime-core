/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 */
package org.knime.base.data.append.column;

import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;


/**
 * A table that appends columns to a given input table. The new columns' values
 * are provided by an
 * {@link AppendedCellFactory}.
 * 
 * <p>
 * This implementation does not verify that the generated cells (from the
 * factory) actually fit to the column spec. Instead, this is checked
 * dynamically in the iterator.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedColumnTable implements DataTable {

    private final DataTable m_table;

    private final DataTableSpec m_spec;

    private final AppendedCellFactory m_factory;

    /**
     * Creates new table.
     * 
     * @param table the underlying table providing the first columns
     * @param cellFactory a factory providing the content of the new columns
     * @param appendColSpec the column specs for the new columns.
     */
    public AppendedColumnTable(final DataTable table,
            final AppendedCellFactory cellFactory,
            final DataColumnSpec... appendColSpec) {
        if (table == null) {
            throw new NullPointerException("Table must not be null.");
        }
        if (appendColSpec == null) {
            throw new NullPointerException("Column spec must not be null.");
        }
        if (cellFactory == null) {
            throw new NullPointerException("Cell factory must not be null.");
        }
        m_spec = getTableSpec(table.getDataTableSpec(), appendColSpec);
        m_table = table;
        m_factory = cellFactory;
    }

    /**
     * Create new table based on an underlying table with a map providing the
     * row key --&gt; new cell mapping. (Thus, this constructor allows only the
     * extension by one column.)
     * 
     * @param table the underlying table
     * @param map tTe map that has to contain <b>all</b> mappings of row key to
     *            new cell. If it does not contain all, an exception is throw
     *            while iterating over the table.
     * @param appendedColSpec the column specs of the new column
     * @throws NullPointerException if any argument is <code>null</code>
     * 
     */
    public AppendedColumnTable(final DataTable table,
            final Map<RowKey, DataCell> map,
            final DataColumnSpec... appendedColSpec) {
        this(table, new DefaultAppendedCellFactory(map), appendedColSpec);
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        return new AppendedColumnRowIterator(this);
    }

    /**
     * Get new iterator over the underlying table.
     * 
     * @return a new iterator
     */
    RowIterator getBaseIterator() {
        return m_table.iterator();
    }

    /**
     * Get reference to the constructor argument.
     * 
     * @return the factory for cells
     */
    AppendedCellFactory getFactory() {
        return m_factory;
    }

    /**
     * Get the class values of the appended columns.
     * 
     * @return those classes
     */
    DataType[] getAppendedColumnClasses() {
        int oldCount = m_table.getDataTableSpec().getNumColumns();
        DataTableSpec mine = getDataTableSpec();
        DataType[] result = new DataType[mine.getNumColumns() - oldCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = mine.getColumnSpec(oldCount + i).getType();
        }
        return result;
    }

    /**
     * Get table spec that is generated when the table is extended by the
     * columns.
     * 
     * @param table the underlying table
     * @param cols the column specs by which <code>table</code> is extended
     * @return the resulting table spec
     */
    public static final DataTableSpec getTableSpec(final DataTableSpec table,
            final DataColumnSpec... cols) {
        return new DataTableSpec(table, new DataTableSpec(cols));
    }
}

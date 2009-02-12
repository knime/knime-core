/*
 * -------------------------------------------------------------------
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

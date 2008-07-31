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
 */
package org.knime.base.data.replace;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;

/**
 * Tables that replaces the values in a given column by other values.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ReplacedColumnsTable implements DataTable {
    private final DataTableSpec m_spec;

    private final DataTable m_table;

    private final int[] m_columns;

    private final ReplacedCellsFactory m_cellFactory;

    /**
     * Creates a new replaced column table with one replaced column.
     * 
     * @param table the table to replace one column in
     * @param spec the column spec for the replaced column
     * @param column the column index to replace
     * @param cellFac the factory to get the replacement cells
     * @throws NullPointerException if the factory is <code>null</code>
     * @throws IndexOutOfBoundsException if the column argument is invalid
     */
    public ReplacedColumnsTable(final DataTable table,
            final DataColumnSpec spec, final int column,
            final ReplacedCellFactory cellFac) {
        this(table, new DataColumnSpec[]{spec}, new int[]{column}, cellFac);
    }

    /**
     * Creates a new replaced column table with several replaced columns.
     * 
     * @param table the table to replace one or more columns in
     * @param specs the column specs for the replaced columns
     * @param columns the column indices to replace
     * @param cellFac the factory to get the replacement cells
     * @throws NullPointerException if the factory is <code>null</code>, or
     *             the arrays contain <code>null</code> elements
     * @throws IndexOutOfBoundsException if the array arguments are invalid
     */
    public ReplacedColumnsTable(final DataTable table,
            final DataColumnSpec[] specs, final int[] columns,
            final ReplacedCellsFactory cellFac) {
        m_spec = createTableSpec(table.getDataTableSpec(), specs, columns);
        if (cellFac == null) {
            throw new NullPointerException("Factory must not be null.");
        }
        m_table = table;
        m_cellFactory = cellFac;
        m_columns = columns;
    }

    /**
     * Creates a new table spec with one replaced column.
     * 
     * @param spec the table to replace one column in
     * @param cspec the new column spec
     * @param column at position
     * @return a new data table spec with the replaced column spec
     * @throws IndexOutOfBoundsException if the column argument is invalid
     * @throws NullPointerException if any argument is <code>null</code>
     */
    public static final DataTableSpec createTableSpec(final DataTableSpec spec,
            final DataColumnSpec cspec, final int column) {
        return createTableSpec(spec, new DataColumnSpec[]{cspec},
                new int[]{column});
    }

    /**
     * Creates a new table spec with several replaced columns.
     * 
     * @param spec the table to replace one or more columns in
     * @param cspecs the new column specs
     * @param columns positions
     * @return a new data table spec with the replaced column spec
     * @throws IndexOutOfBoundsException if any column argument is invalid
     * @throws NullPointerException if any argument is <code>null</code> or
     *             contains <code>null</code> elements
     */
    public static final DataTableSpec createTableSpec(final DataTableSpec spec,
            final DataColumnSpec[] cspecs, final int[] columns) {
        int nrCols = spec.getNumColumns();
        DataColumnSpec[] newColSpec = new DataColumnSpec[nrCols];
        assert (nrCols > 0);
        for (int column = 0; column < nrCols; column++) {
            newColSpec[column] = spec.getColumnSpec(column);
        }
        for (int i = 0; i < columns.length; i++) {
            newColSpec[columns[i]] = cspecs[i];
        }
        return new DataTableSpec(newColSpec);
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
        RowIterator origIt = m_table.iterator();
        DataType[] validateTypes = new DataType[m_columns.length];
        for (int column = 0; column < m_columns.length; column++) {
            validateTypes[column] = getDataTableSpec().getColumnSpec(
                    m_columns[column]).getType();
        }
        return new ReplacedColumnsRowIterator(origIt, m_cellFactory,
                validateTypes, m_columns);
    }
}

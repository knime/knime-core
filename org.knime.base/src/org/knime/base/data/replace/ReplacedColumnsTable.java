/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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

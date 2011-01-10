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
 */
package org.knime.base.data.replace;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;


/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ReplacedColumnsDataRow implements DataRow {
    private final DataRow m_row;

    private final int[] m_columns;

    private final DataCell[] m_newCells;

    /**
     * Creates a new replaced column row.
     * 
     * @param row the row to replace one or more columns in
     * @param newCells the new cells
     * @param columns at positions
     * @throws IndexOutOfBoundsException if one of the column indices is not
     *             inside the row
     * @throws NullPointerException if the replace cell is <code>null</code>
     */
    public ReplacedColumnsDataRow(final DataRow row, final DataCell[] newCells,
            final int[] columns) {
        for (int column : columns) {
            if (column < 0 || column >= row.getNumCells()) {
                throw new IndexOutOfBoundsException("Index invalid: " + column);
            }
        }
        for (DataCell newCell : newCells) {
            if (newCell == null) {
                throw new NullPointerException("New cell must not be null");
            }
        }
        m_row = row;
        m_columns = columns;
        m_newCells = newCells;
    }

    /**
     * Convenience constructor that replaces one cell only. This constructor
     * calls:
     * 
     * <pre>
     * this(row, new DataCell[]{newCell}, new int[]{column});
     * </pre>.
     * 
     * @param row the row to replace one column in
     * @param newCell the new cell
     * @param column the column to be replaced
     */
    public ReplacedColumnsDataRow(final DataRow row, final DataCell newCell,
            final int column) {
        this(row, new DataCell[]{newCell}, new int[]{column});
    }

    /**
     * {@inheritDoc}
     */
    public int getNumCells() {
        return m_row.getNumCells();
    }

    /**
     * {@inheritDoc}
     */
    public RowKey getKey() {
        return m_row.getKey();
    }

    /**
     * {@inheritDoc}
     */
    public DataCell getCell(final int index) {
        for (int i = 0; i < m_columns.length; i++) {
            if (index == m_columns[i]) {
                return m_newCells[i];
            }
        }
        return m_row.getCell(index);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }
}

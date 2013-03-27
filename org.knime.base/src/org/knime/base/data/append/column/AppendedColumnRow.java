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

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;


/**
 * A {@link org.knime.core.data.DataRow} that is extended by one or more
 * cells.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedColumnRow implements DataRow {

    private final DataRow m_baseRow;

    private final DataCell[] m_appendCell;

    private final RowKey m_rowKey;

    /**
     * Creates new Row with <code>baseRow</code> providing the first cells and
     * <code>appendCell</code> as last cells.
     * @param baseRow Containing the first cells
     * @param appendCell The last cells (to be appended).
     */
    public AppendedColumnRow(final DataRow baseRow,
            final DataCell... appendCell) {
        this(baseRow.getKey(), baseRow, appendCell);
    }

    /**
     * Creates new Row with <code>baseRow</code> providing the first cells and
     * <code>appendCell</code> as last cells.
     * @param rowKey new row key for the resulting appended row
     * @param baseRow Containing the first cells
     * @param appendCell The last cells (to be appended).
     */
    public AppendedColumnRow(final RowKey rowKey, final DataRow baseRow,
            final DataCell... appendCell) {
        m_baseRow = baseRow;
        m_appendCell = appendCell;
        m_rowKey = rowKey;
    }

    /**
     * Create a new row with the <code>baseRow</code> providing the first cells
     * and <code>appendedRow</code> providing the following cells. Which cells
     * from the second row should be appended is passed in
     * <code>appendColumn</code> (<code>true</code> for adding the cells at
     * the index, <code>false</code> for not adding it).
     * @param baseRow row with the first cells
     * @param appendedRow row with the cells to append
     * @param appendColumn array with entries set to <code>true</code>, if the
     * corresponding cells from the second row should be added
     */
    public AppendedColumnRow(final DataRow baseRow,
    		final DataRow appendedRow, final boolean[] appendColumn) {
    	this(baseRow.getKey(), baseRow, appendedRow, appendColumn);
    }

    /**
     * Create a new row with the <code>baseRow</code> providing the first cells
     * and <code>appendedRow</code> providing the following cells. Which cells
     * from the second row should be appended is passed in
     * <code>appendColumn</code> (<code>true</code> for adding the cells at
     * the index, <code>false</code> for not adding it).
     * @param rowKey new row key for the resulting appended row
     * @param baseRow row with the first cells
     * @param appendedRow row with the cells to append
     * @param appendColumn array with entries set to <code>true</code>, if the
     * corresponding cells from the second row should be added
     */
    public AppendedColumnRow(final RowKey rowKey, final DataRow baseRow,
    		final DataRow appendedRow, final boolean[] appendColumn) {
        if (appendColumn.length != appendedRow.getNumCells()) {
            throw new IllegalArgumentException("Number of columns to append "
                    + "is unequal to the number of cells in the appended row");
        }
        m_baseRow = baseRow;

        int k = 0;
        for (int i = 0; i < appendColumn.length; i++) {
            if (appendColumn[i]) { k++; }
        }

        m_appendCell = new DataCell[k];
        k = 0;
        for (int i = 0; i < appendColumn.length; i++) {
            if (appendColumn[i]) {
                m_appendCell[k++] = appendedRow.getCell(i);
            }
        }

        m_rowKey = rowKey;
    }
    /**
     * Create a new row with the <code>baseRow</code> providing the first cells
     * and <code>appendedRow</code> providing the following cells.
     * @param rowKey new row key for the resulting appended row
     * @param baseRow row with the first cells
     * @param appendedRow row with the cells to append
     */
    public AppendedColumnRow(final RowKey rowKey, final DataRow baseRow,
    		final DataRow appendedRow) {
        m_baseRow = baseRow;
        m_appendCell = new DataCell[appendedRow.getNumCells()];
        for (int i = 0; i < m_appendCell.length; i++) {
            m_appendCell[i] = appendedRow.getCell(i);
        }
        m_rowKey = rowKey;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumCells() {
        return m_baseRow.getNumCells() + m_appendCell.length;
    }

    /**
     * {@inheritDoc}
     */
    public RowKey getKey() {
        return m_rowKey;
    }

    /**
     * {@inheritDoc}
     */
    public DataCell getCell(final int index) {
        if (index < 0 || index >= getNumCells()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of "
                    + "range [" + 0 + ", " + (getNumCells() - 1) + "]");
        }
        if (index < m_baseRow.getNumCells()) {
            return m_baseRow.getCell(index);
        }
        return m_appendCell[index - m_baseRow.getNumCells()];
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }
}

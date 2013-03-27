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
 * History
 *   Dec 15, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;

/**
 * Special row implementation that supports to access the wrapper cells of
 * {@link BlobDataCell}. Dealing with the wrapper cells ({@link BlobWrapperDataCell})
 * gives the benefit that blobs are not read from the file when passed from one
 * place to another (they will be read on access).
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class BlobSupportDataRow implements DataRow {

    private final RowKey m_key;

    private final DataCell[] m_cells;

    /**
     * @param key Row key
     * @param cells cell array.
     */
    public BlobSupportDataRow(final RowKey key, final DataCell[] cells) {
        m_key = key;
        m_cells = cells;
    }

    /**
     * Creates a new data row with a new row ID.
     *
     * @param key the key with the new row ID
     * @param oldRow container of the cells for the new row
     */
    public BlobSupportDataRow(final RowKey key, final DataRow oldRow) {
        m_key = key;
        if (oldRow instanceof BlobSupportDataRow) {
            m_cells = ((BlobSupportDataRow)oldRow).m_cells;
        } else {
            m_cells = new DataCell[oldRow.getNumCells()];
            for (int i = 0; i < m_cells.length; i++) {
                m_cells[i] = oldRow.getCell(i);
            }
        }

    }

    /**
     * Creates a new data row with a new row ID.
     *
     * @param id the new row ID
     * @param oldRow container of the cells for the new row
     */
    public BlobSupportDataRow(final String id, final DataRow oldRow) {
        this(new RowKey(id), oldRow);
    }

    /**
     * If the cell at index is a blob wrapper cell, it will fetch the content
     * and return it. {@inheritDoc}
     */
    public DataCell getCell(final int index) {
        DataCell c = m_cells[index];
        if (c instanceof BlobWrapperDataCell) {
            return ((BlobWrapperDataCell)c).getCell();
        }
        return c;
    }

    /**
     * Returns the cell at given index. Returns the wrapper cell (if any).
     *
     * @param index Cell index.
     * @return Raw cell.
     */
    public DataCell getRawCell(final int index) {
        return m_cells[index];
    }

    /**
     * {@inheritDoc}
     */
    public RowKey getKey() {
        return m_key;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumCells() {
        return m_cells.length;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }

    /**
     * Get a string representing this row, i.e. "rowkey: (cell1, ..., celln)"
     *
     * @return key + values of this row in a string
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(getKey().toString());
        buffer.append(": (");
        for (int i = 0; i < getNumCells(); i++) {
            buffer.append(getCell(i).toString());
            // separate by ", "
            if (i != getNumCells() - 1) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

}

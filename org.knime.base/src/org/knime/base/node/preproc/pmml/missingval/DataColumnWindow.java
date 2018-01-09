/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.12.2014 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * A window over a single column. The size is equal to lookahead + lookbehind + 1.
 *
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class DataColumnWindow {

    // Ring buffer for the cells in the column
    private DataCell[] m_buffer;

    private int m_lookahead;

    private int m_lookbehind;

    private int m_currentLookahead = 0;

    private int m_currentLookbehind = 0;

    private int m_colIdx;

    // Pointer to the current cell in the ring buffer
    private int m_pointer;

    /**
     * Constructor for a window over a data column.
     * @param columnIdx the column index
     * @param lookahead the size of the lookahead
     * @param lookbehind the size of the lookbehind
     */
    public DataColumnWindow(final int columnIdx, final int lookahead, final int lookbehind) {
        m_buffer = new DataCell[lookahead + lookbehind + 1];
        m_lookahead = lookahead;
        m_lookbehind = lookbehind;
        m_colIdx = columnIdx;
    }

    /**
     * @return the number of cells that are included in the lookahead.
     */
    public int getLookaheadSize() {
        return m_lookahead;
    }

    /**
     * @return the number of cells that are included in the lookbehind.
     */
    public int getLookbehindSize() {
        return m_lookbehind;
    }

    /**
     * @return the index of the column this window is used for.
     */
    public int getColumnIndex() {
        return m_colIdx;
    }

    /**
     * Initializes the window with a buffer that is at least as big as lookahead + lookbehind + 1.
     * @param buffer the data rows used for the initialization
     * @param current the index of the current item in the buffer
     */
    void init(final DataRow[] buffer, final int current) {
        // We have to read enough cells for the lookahead,
        // but the buffer might be smaller because the table is smaller
        int size = Math.min(buffer.length, m_lookahead);

        int c = 0;
        // Fill the buffer
        for (int i = current; i < size + current; i++) {
            DataRow row = buffer[i];
            if (row == null) {
                m_buffer[c++] = null;
            } else {
                m_buffer[c++] = buffer[i].getCell(m_colIdx);
            }
        }

        m_pointer = -1;
        m_currentLookahead = size - 1;
        m_currentLookbehind = -1;
    }

    /**
     * Adds a new cell from a data row read by a WindowedDataTableIterator.
     * @param row the new row to take the cell from
     */
    void addCell(final DataRow row) {
        // Update pointer, buffer and actual lookahead and lookbehind
        m_pointer = (m_pointer + 1) % m_buffer.length;
        m_buffer[(m_pointer + m_lookahead) % m_buffer.length] = row.getCell(m_colIdx);
        m_currentLookbehind = Math.min(m_currentLookbehind + 1, m_lookbehind);
        m_currentLookahead = Math.min(m_currentLookahead + 1, m_lookahead);
    }

    /**
     * @return the data cell that is currently in the middle between the lookbehind and lookahead cells
     */
    public DataCell getCurrentCell() {
        return m_buffer[m_pointer];
    }

    /**
     * Retrieves a cell from the lookbehind.
     * @param n the index of the cell
     * @return the cell from the lookbehind
     */
    public DataCell getNthLookbehind(final int n) {
        if (n > m_currentLookbehind) {
            return null;
        }
        int idx = m_pointer - n;
        if (idx < 0) {
            idx = m_buffer.length - idx;
        }
        return m_buffer[idx % m_buffer.length];
    }

    /**
     * Retrieves a cell from the lookahead.
     * @param n the index of the cell
     * @return the cell from the lookahead
     */
    public DataCell getNthLookahead(final int n) {
        if (n > m_currentLookahead) {
            return null;
        }
        return m_buffer[(m_pointer + n) % m_buffer.length];
    }

    /**
     * Retrieves a cell from the buffer. Index 0 is the current cell,
     * negative indices are the lookbehind, positive ones the lookahead.
     * @param n the index of the cell.
     * @return the data cell at the given index
     */
    public DataCell getNthCell(final int n) {
        if ((n < 0 && -n > m_currentLookbehind) || (n > m_currentLookahead)) {
            return null;
        }
        int idx = m_pointer + n;
        if (idx < 0) {
            idx = m_buffer.length + idx;
        }
        return m_buffer[idx % m_buffer.length];
    }
}

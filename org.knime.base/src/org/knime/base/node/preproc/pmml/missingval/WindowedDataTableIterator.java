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

import java.util.NoSuchElementException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowIterator;

/**
 * Iterator over a data table that provides windows over the individual columns.
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class WindowedDataTableIterator extends RowIterator {

    // Iterator for the data table
    private RowIterator m_iter;

    // Lookahead and lookbehind as set in the constructor
    private int m_lookahead;
    private int m_lookbehind;

    // The actual lookahead and lookbehind
    // (might be different than the set values at the start and end of the table)
    private int m_currentLookahead = 0;
    private int m_currentLookbehind = 0;

    // Pointer to the current row in the buffer
    private int m_pointer = -1;

    // Ring buffer
    private DataRow[] m_buffer;

    private boolean m_hasMore = true;

    // Windows for the individual columns
    private DataColumnWindow[] m_colWindows;

    /**
     * Constructor for WindowedDataTableIterator.
     * @param table the table this iterator is used for,
     * @param lookaheads the size of the lookahead for each column
     * @param lookbehinds the size of the lookbehind for each column
     */
    public WindowedDataTableIterator(final DataTable table, final int[] lookaheads, final int[] lookbehinds) {
        if (lookaheads.length != lookbehinds.length || lookaheads.length != table.getDataTableSpec().getNumColumns()) {
            throw new IllegalArgumentException("There must be one lookahead and lookbehind size value for each column");
        }

        // Initialize column windows and find the maximum lookahead and lookbehind
        int maxLookahead = 0;
        int maxLookbehind = 0;
        m_colWindows = new DataColumnWindow[table.getDataTableSpec().getNumColumns()];
        for (int i = 0; i < lookaheads.length; i++) {
            if (lookaheads[i] > maxLookahead) {
                maxLookahead = lookaheads[i];
            }
            if (lookbehinds[i] > maxLookbehind) {
                maxLookbehind = lookbehinds[i];
            }
            m_colWindows[i] = new DataColumnWindow(i, lookaheads[i], lookbehinds[i]);
        }
        m_lookahead = maxLookahead;
        m_lookbehind = maxLookbehind;
        m_buffer = new DataRow[maxLookahead + maxLookbehind + 1];
        m_iter = table.iterator();
        m_hasMore = m_iter.hasNext();
    }

    /** Initializes the iterator to have enough cells for the lookahead at the start. **/
    private void init() {
        // We need to count how many cells we actually read to handle tables smaller than the lookahead
        int counter = 0;
        // Fill the buffer so we have enough rows for the lookahead of the first row
        for (int i = m_lookbehind; i < m_lookbehind + m_lookahead && m_iter.hasNext(); i++) {
            m_buffer[i] = m_iter.next();
            counter++;
        }
        m_currentLookahead = counter;
        m_pointer = m_lookbehind - 1;

        // Initialize the column windows with the buffer
        for (DataColumnWindow w : m_colWindows) {
            w.init(m_buffer, m_lookbehind);
        }
    }

    /**
     * Returns a data column window for the column at the given index.
     * @param colIdx the column's index
     * @return the window over the data column
     */
    public DataColumnWindow getWindowForColumn(final int colIdx) {
        return m_colWindows[colIdx];
    }

    /**
     * Indicates whether more rows are available.
     * @return true, if more rows can be read from the table
     */
    @Override
    public boolean hasNext() {
        return m_hasMore;
    }

    /**
     * Returns the next data row from the table.
     * @return the row
     */
    @Override
    public DataRow next() {
        if (!m_hasMore) {
            throw new NoSuchElementException("There are no more elements");
        }
        // Check if iterator is initialized and if it is at the end
        if (m_pointer == -1) {
            init();
        } else if (!hasNext()) {
            throw new IndexOutOfBoundsException("Iterator reached the end of the table");
        }

        // Advance pointer and wrap around if necessary (ring buffer)
        m_pointer = (m_pointer + 1) % m_buffer.length;

        // At the beginning the actual lookbehind might be smaller than the desired one, so we update it accordingly
        m_currentLookbehind = Math.min(m_currentLookbehind + 1, m_lookbehind);

        // Add a new row to the buffer
        if (m_iter.hasNext()) {
            // Calculate index where to put the new item and read it from the table
            int idx = (m_pointer + m_lookahead) % m_buffer.length;
            m_buffer[idx] = m_iter.next();
            m_hasMore = m_currentLookahead > 0 || m_iter.hasNext();
        } else {
            m_currentLookahead--;
            m_hasMore = m_currentLookahead > 0;
        }

        // Update all the column windows.
        // They might get an older row if their lookahead is smaller than the maximum lookahead
        for (int i = 0; i < m_colWindows.length; i++) {
            if (m_currentLookahead >= m_colWindows[i].getLookaheadSize()) {
                DataRow next = m_buffer[(m_pointer + m_colWindows[i].getLookaheadSize()) % m_buffer.length];
                m_colWindows[i].addCell(next);
            }
        }

        DataRow row = m_buffer[m_pointer];
        return row;
    }
}

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
 *   Sep 11, 2023 (leonard.woerteler): created
 */
package org.knime.core.data.sort;

import java.util.Arrays;
import java.util.Comparator;

import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;

/**
 * Cursor which receives <i>k</i> non-descending cursors and yields their merged return values in non-descending
 * order. The order in which items are returned is stable in the sense that elements comparing as equal are returned
 * <ol>
 *   <li>in the order in which their cursor appear in the list provided in the constructor, and</li>
 *   <li>in the order in which their cursor returns them.</li>
 * </ol>
 * The algorithm never consumes more than one not-yet-returned element from any input cursor.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 */
public final class KWayMergeCursor implements RowCursor {

    private final Comparator<RowRead> m_comparatorNullsLast;
    private final RowCursor[] m_cursors;
    private final RowRead[] m_values;
    private final int[] m_indexes;
    private final int m_numColumns;

    private int m_currentInput = -1;

    /**
     * @param comparator item comparator
     * @param cursors non-descending input cursors
     * @param numColumns number of columns
     */
    public KWayMergeCursor(final Comparator<RowRead> comparator, final RowCursor[] cursors, final int numColumns) {
        final int n = cursors.length;
        m_comparatorNullsLast = Comparator.nullsLast(comparator);
        m_cursors = cursors.clone();
        m_values = new RowRead[n];
        m_indexes = new int[n];
        Arrays.fill(m_indexes, -1);
        for (var inputIdx = 0; inputIdx < n; inputIdx++) {
            insertNextFrom(inputIdx);
        }
        m_numColumns = numColumns;
    }

    /**
     * Replenishes the <i>Tree of Losers</i> with the next element from the cursor with index {@code start}.
     * If the cursor is drained, {@code null} is added, which compares as larger than all elements.
     *
     * @param inputIdx index of the source iterator
     */
    @SuppressWarnings({"resource"})
    private void insertNextFrom(final int inputIdx) {
        final int n = m_cursors.length;
        final var cursor = m_cursors[inputIdx];
        var currentIndex = inputIdx;
        RowRead currentValue;
        if (cursor != null && cursor.canForward()) {
            currentValue = cursor.forward();
        } else {
            if (cursor != null) {
                cursor.close();
                m_cursors[inputIdx] = null;
            }
            currentValue = null;
        }

        int nodeIdx = (n + inputIdx) / 2;
        while (m_indexes[nodeIdx] >= 0) {
            if (firstSmaller(m_indexes[nodeIdx], m_values[nodeIdx], currentIndex, currentValue)) {
                // we are the loser, so the opponent advances
                final var tmpIdx = currentIndex;
                currentIndex = m_indexes[nodeIdx];
                m_indexes[nodeIdx] = tmpIdx;
                final var tmpValue = currentValue;
                currentValue = m_values[nodeIdx];
                m_values[nodeIdx] = tmpValue;
            }
            nodeIdx /= 2;
        }
        m_indexes[nodeIdx] = currentIndex;
        m_values[nodeIdx] = currentValue;
    }

    /**
     * Compares two elements coming from different input cursors. If the elements compare as equal, the cursors'
     * indexes are compared as a tie breaker. The special value {@code null} is compared as larger than
     * non-{@code null} elements because it indicates that the corresponding cursor has been drained.
     *
     * @param firstIdx index of the first element's cursor
     * @param first first element, may be {@code null}
     * @param secondIdx index of the second element's cursor
     * @param second second element, may be {@code null}
     * @return {@code true} id the first elements is smaller, {@code false} otherwise
     */
    private boolean firstSmaller(final int firstIdx, final RowRead first, final int secondIdx, final RowRead second) {
        final var valueCmp = m_comparatorNullsLast.compare(first, second);
        return valueCmp != 0 ? (valueCmp < 0) : (firstIdx < secondIdx);
    }

    @Override
    public boolean canForward() {
        if (m_values.length == 0) {
            return false;
        }
        if (m_currentInput >= 0 && m_indexes[0] < 0) {
            insertNextFrom(m_currentInput);
        }
        return m_values[0] != null;
    }

    /**
     * @return the index of the input that returned the last {@link RowRead} that was returned by {@link #forward()},
     *         or {@code -1} if this cursor is closed, before the first or after the last {@link RowRead}
     */
    public int currentInputIndex() {
        return m_currentInput;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This cursor is closed if it is forwarded beyond the last {@link RowRead}.
     */
    @Override
    public RowRead forward() {
        if (!canForward()) {
            close();
            return null;
        }

        final var res = m_values[0];
        final var source = m_indexes[0];
        m_values[0] = null;
        m_indexes[0] = -1;
        m_currentInput = source;
        return res;
    }

    @Override
    public int getNumColumns() {
        return m_numColumns;
    }

    @Override
    public void close() {
        for (var i = 0; i < m_cursors.length; i++) {
            @SuppressWarnings("resource")
            final var cursor = m_cursors[i];
            if (cursor != null) {
                cursor.close();
                m_cursors[i] = null;
            }
        }
        m_currentInput = -1;
    }
}

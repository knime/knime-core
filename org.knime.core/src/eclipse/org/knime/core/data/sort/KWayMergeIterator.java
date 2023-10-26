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
import java.util.NoSuchElementException;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;

/**
 * Iterator which receives <i>k</i> non-descending iterators and yields their merged return values in non-descending
 * order. The order in which items are returned is stable in the sense that elements comparing as equal are returned
 * <ol>
 *   <li>in the order in which their iterators appear in the list provided in the constructor, and</li>
 *   <li>in the order in which their iterator returns them.</li>
 * </ol>
 * The algorithm never consumes more than one not-yet-returned element from any input iterator.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
final class KWayMergeIterator extends CloseableRowIterator {

    private final Comparator<DataRow> m_comparatorNullsLast;
    private final CloseableRowIterator[] m_iterators;
    private final DataRow[] m_values;
    private final int[] m_indexes;

    /**
     * @param comparator item comparator
     * @param iterators non-descending input iterators
     */
    KWayMergeIterator(final Comparator<DataRow> comparator, final CloseableRowIterator[] iterators) {
        final int n = iterators.length;
        m_comparatorNullsLast = Comparator.nullsLast(comparator);
        m_iterators = iterators.clone();
        m_values = new DataRow[n];
        m_indexes = new int[n];
        Arrays.fill(m_indexes, -1);
        for (var inputIdx = 0; inputIdx < n; inputIdx++) {
            insertNextFrom(inputIdx);
        }
    }

    /**
     * Replenishes the <i>Tree of Losers</i> with the next element from the iterator with index {@code start}.
     * If the iterator is drained, {@code null} is added, which compares as larger than all elements.
     *
     * @param inputIdx index of the source iterator
     */
    @SuppressWarnings({"resource"})
    private void insertNextFrom(final int inputIdx) {
        final int n = m_iterators.length;
        final var iterator = m_iterators[inputIdx];
        var currentIndex = inputIdx;
        DataRow currentValue;
        if (iterator != null && iterator.hasNext()) {
            currentValue = iterator.next();
        } else {
            if (iterator != null) {
                iterator.close();
                m_iterators[inputIdx] = null;
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
     * Compares two elements coming from different input iterators. If the elements compare as equal, the iterators'
     * indexes are compared as a tie breaker. The special value {@code null} is compared as larger than
     * non-{@code null} elements because it indicates that the corresponding iterator has been drained.
     *
     * @param firstIdx index of the first element's iterator
     * @param first first element, may be {@code null}
     * @param secondIdx index of the second element's iterator
     * @param second second element, may be {@code null}
     * @return {@code true} id the first elements is smaller, {@code false} otherwise
     */
    private boolean firstSmaller(final int firstIdx, final DataRow first, final int secondIdx, final DataRow second) {
        final var valueCmp = m_comparatorNullsLast.compare(first, second);
        return valueCmp != 0 ? (valueCmp < 0) : (firstIdx < secondIdx);
    }

    @Override
    public boolean hasNext() {
        return m_values.length > 0 && m_values[0] != null;
    }

    @Override
    public DataRow next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final var res = m_values[0];
        final var source = m_indexes[0];
        m_values[0] = null;
        m_indexes[0] = -1;
        insertNextFrom(source);
        return res;
    }

    @Override
    public void close() {
        for (var i = 0; i < m_iterators.length; i++) {
            @SuppressWarnings("resource")
            final var iter = m_iterators[i];
            if (iter != null) {
                iter.close();
                m_iterators[i] = null;
            }
        }
    }
}

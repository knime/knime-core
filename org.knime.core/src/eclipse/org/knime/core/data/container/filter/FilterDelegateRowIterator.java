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
 */
package org.knime.core.data.container.filter;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.storage.AbstractTableStoreReader;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.ExecutionMonitor;

/**
 * A {@link CloseableRowIterator} that filters the {@link DataRow DataRows} provided by another delegate
 * {@link CloseableRowIterator} according to a {@link TableFilter}. Used as a fallback for various implementations of
 * {@link KnowsRowCountTable#iteratorWithFilter(TableFilter)} and
 * {@link AbstractTableStoreReader#iteratorWithFilter(TableFilter)}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public final class FilterDelegateRowIterator extends CloseableRowIterator {

    private final CloseableRowIterator m_delegate;

    private final long m_fromIndex;

    private final long m_toIndex;

    private final Optional<ExecutionMonitor> m_exec;

    private long m_index;

    private DataRow m_nextRow;

    // initialization flag to allow for lazy initialization
    private boolean m_initialized;

    /**
     * Constructs a new filter delegate row iterator.
     *
     * @param iterator the iterator to delegate to and filter from
     * @param filter the table filter that specifies the filtering to be performed on the delegate iterator
     * @param exec the execution monitor that shall be updated with progress or null if no progress updates are desired
     */
    public FilterDelegateRowIterator(final CloseableRowIterator iterator, final TableFilter filter,
        final ExecutionMonitor exec) {
        m_delegate = iterator;
        m_fromIndex = filter.getFromRowIndex();
        m_toIndex = filter.getToRowIndex();
        m_exec = Optional.ofNullable(exec);
        m_index = 0;
    }

    private void init() {
        m_nextRow = internalNext();
        m_initialized = true;
    }

    @Override
    public boolean hasNext() {
        if (!m_initialized) {
            init();
        }
        return m_nextRow != null;
    }

    @Override
    public DataRow next() {
        if (!m_initialized) {
            init();
        }
        if (m_nextRow == null) {
            throw new NoSuchElementException();
        }
        final DataRow nextRow = m_nextRow;
        m_nextRow = internalNext();
        return nextRow;
    }

    private DataRow internalNext() {
        // get next row while there is a next row and while we're still at or below the maximum index of rows to keep
        while (m_delegate.hasNext() && m_index <= m_toIndex) {
            final DataRow row = m_delegate.next();

            // update progress if execution monitor is present
            if (m_exec.isPresent()) {
                final long index = m_index + 1;
                final long size = m_toIndex + 1;
                m_exec.get().setProgress(m_index / m_toIndex,
                    () -> String.format("Row %,d/%,d (%s)", index, size, row.getKey()));
            }

            // return the row if we're at or above the minimum index of rows to keep
            // also, increase the index by one
            if (m_index++ >= m_fromIndex) {
                return row;
            }
        }
        return null;
    }

    @Override
    public void close() {
        m_delegate.close();
    }

}
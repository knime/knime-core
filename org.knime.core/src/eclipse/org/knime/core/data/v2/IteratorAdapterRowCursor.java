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
 *   Mar 28, 2024 (leonard.woerteler): created
 */
package org.knime.core.data.v2;

import java.util.Iterator;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;

/**
 * Adapter from an {@link Iterator Iterator&lt;DataRow>} to the more recently introduced {@link RowCursor} API.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
public class IteratorAdapterRowCursor implements RowCursor {

    private final int m_numColumns;

    private CloseableRowIterator m_iterator;

    private DataRow m_current;

    private final RowRead m_read;

    /**
     * Creates a {@link RowRead} which can be used to read the values of the given row iterator. If {@code iterator}
     * is {@link AutoCloseable}, it will be closed by this cursor when it is drained or its {@link #close()} method is
     * called.
     *
     * @param iterator iterator to be adapted
     * @param numColumns number of columns of the rows returned by {code iterator}, will be returned
     *        by {@link #getNumColumns()}
     */
    public IteratorAdapterRowCursor(final Iterator<DataRow> iterator, final int numColumns) {
        m_iterator = CloseableRowIterator.from(iterator);
        m_numColumns = numColumns;
        m_read = RowRead.suppliedBy(() -> m_current, numColumns);
    }

    @Override
    public int getNumColumns() {
        return m_numColumns;
    }

    @Override
    public boolean canForward() {
        return m_iterator != null && m_iterator.hasNext();
    }

    @Override
    public RowRead forward() {
        if (!canForward()) {
            close();
            return null;
        }
        m_current = m_iterator.next();
        return m_read;
    }

    @Override
    public void close() {
        if (m_iterator != null) {
            m_iterator.close();
            m_iterator = null;
            m_current = null;
        }
    }
}

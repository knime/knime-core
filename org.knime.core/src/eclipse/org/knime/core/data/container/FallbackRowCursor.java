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
 */
package org.knime.core.data.container;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.v2.DataRowRowRead;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;

/**
 * Fallback implementation of {@link RowCursor} based on {@link CloseableRowIterator}.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz
 */
final class FallbackRowCursor implements RowCursor, DataRowRowRead {

    private static final class InvalidDataRow implements DataRow {

        private static final InvalidDataRow INSTANCE = new InvalidDataRow();

        private static final String ISE_ERROR_MSG = "This method should not be called.";

        private static final String NSEE_ERROR_MSG = "Cursor at invalid position.";

        private InvalidDataRow() {
            // singleton
        }

        @Override
        public Iterator<DataCell> iterator() {
            throw new IllegalStateException(ISE_ERROR_MSG);
        }

        @Override
        public int getNumCells() {
            throw new IllegalStateException(ISE_ERROR_MSG);
        }

        @Override
        public RowKey getKey() {
            throw new NoSuchElementException(NSEE_ERROR_MSG);
        }

        @Override
        public DataCell getCell(final int index) {
            throw new NoSuchElementException(NSEE_ERROR_MSG);
        }

    }

    private final CloseableRowIterator m_delegate;

    private DataRow m_currentRow;

    private int m_numValues;

    FallbackRowCursor(final CloseableRowIterator delegate, final DataTableSpec spec) {
        m_delegate = delegate;
        m_currentRow = InvalidDataRow.INSTANCE;
        m_numValues = spec.getNumColumns();
    }

    DataRow getCurrentRow() {
        return m_currentRow;
    }

    @Override
    public RowRead forward() {
        if (m_delegate.hasNext()) {
            m_currentRow = m_delegate.next();
            return this;
        }
        if (m_currentRow != InvalidDataRow.INSTANCE) {
            m_currentRow = InvalidDataRow.INSTANCE;
            m_delegate.close();
        }
        return null;
    }

    @Override
    public void close() {
        m_delegate.close();
    }

    @Override
    public RowKeyValue getRowKey() {
        return m_currentRow.getKey();
    }

    @Override
    public <D extends DataValue> D getValue(final int index) {
        @SuppressWarnings("unchecked")
        final D cell = (D)m_currentRow.getCell(index);
        return cell;
    }

    @Override
    public boolean isMissing(final int index) {
        return m_currentRow.getCell(index).isMissing();
    }

    @Override
    public int getNumColumns() {
        return m_numValues;
    }

    @Override
    public boolean canForward() {
        return m_delegate.hasNext();
    }

    @Override
    public DataRow materializeDataRow() {
        return m_currentRow;
    }
}

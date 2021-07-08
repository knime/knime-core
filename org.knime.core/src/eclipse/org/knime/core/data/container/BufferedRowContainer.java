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
 *   Oct 22, 2020 (dietzc): created
 */
package org.knime.core.data.container;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.data.v2.RowKeyReadValue;
import org.knime.core.data.v2.RowKeyWriteValue;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowWrite;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.ValueSchema;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.table.access.BufferedAccesses;
import org.knime.core.table.access.BufferedAccesses.BufferedAccess;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.WriteAccess;

/**
 * Legacy implementation for CustomKeyRowContainer using {@link DataContainer}s as storage backend.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
final class BufferedRowContainer implements RowContainer, RowWriteCursor {

    private static final DataCell MISSING_CELL = DataType.getMissingCell();

    private final BufferedRowWrite m_row;

    private final BufferedDataContainer m_delegate;

    private boolean m_first = true;

    BufferedRowContainer(final BufferedDataContainer delegate, final ValueSchema schema) {
        m_row = new BufferedRowWrite(delegate, schema);
        m_delegate = delegate;
    }

    @Override
    public RowWriteCursor createCursor() {
        return this;
    }

    @Override
    public RowWrite forward() {
        // TODO I don't like the 'm_first' :-(
        if (!m_first) {
            m_row.commit();
        } else {
            m_first = false;
        }
        return m_row;
    }

    @Override
    public boolean canForward() {
        return true;
    }

    @Override
    public BufferedDataTable finish() throws IOException {
        m_delegate.close();
        return m_delegate.getTable();
    }

    @Override
    public void close() {
        // called before finish
        if (!m_delegate.isClosed()) {
            m_row.commit();
            m_delegate.close();
            m_delegate.getBufferedTable().close();
        }
    }

    private static final class BufferedRowWrite implements RowWrite {

        private final BufferedDataContainer m_delegate;

        private final DataCell[] m_cells;

        private final NullableReadValue[] m_readValues;

        private final WriteValue<?>[] m_writeValues;

        private final RowKeyReadValue m_rowKeyReadValue;

        private BufferedRowWrite(final BufferedDataContainer delegate, final ValueSchema schema) {
            m_delegate = delegate;
            final ValueFactory<?, ?>[] factories = schema.getValueFactories();

            m_cells = new DataCell[factories.length - 1];
            m_readValues = new NullableReadValue[factories.length];
            m_writeValues = new WriteValue[factories.length];

            for (int i = 0; i < factories.length; i++) {
                final BufferedAccess access = BufferedAccesses.createBufferedAccess(factories[i].getSpec());
                @SuppressWarnings("unchecked")
                final ValueFactory<ReadAccess, ?> readCast = (ValueFactory<ReadAccess, ?>)factories[i];
                m_readValues[i] = new NullableReadValue(readCast, access);
                @SuppressWarnings("unchecked")
                final ValueFactory<?, WriteAccess> writeCast = (ValueFactory<?, WriteAccess>)factories[i];
                m_writeValues[i] = writeCast.createWriteValue(access);
            }

            m_rowKeyReadValue = (RowKeyReadValue)m_readValues[0].getDelegate();
        }

        @Override
        public <W extends WriteValue<?>> W getWriteValue(final int index) {
            @SuppressWarnings("unchecked")
            final W cast = (W)m_writeValues[index + 1];
            return cast;
        }

        @Override
        public int getNumColumns() {
            return m_readValues.length - 1;
        }

        @Override
        public void setMissing(final int index) {
            m_readValues[index].setMissing();
        }

        @Override
        public void setRowKey(final String rowKey) {
            ((RowKeyWriteValue)m_writeValues[0]).setRowKey(rowKey);
        }

        @Override
        public void setRowKey(final RowKeyValue rowKey) {
            ((RowKeyWriteValue)m_writeValues[0]).setRowKey(rowKey);
        }

        @Override
        public void setFrom(final RowRead row) {
            // TODO performance
            setRowKey(row.getRowKey());
            for (int i = 1; i < m_writeValues.length; i++) {
                m_writeValues[i].setValue(row.getValue(i));
            }
        }

        void commit() {
            // TODO handle case where no row key is required?
            if (m_readValues[0].isMissing()) {
                throw new IllegalStateException("RowKey not set.");
            }

            // We have to loop once to reset our VolatileAccesses after reading
            for (int i = 1; i < m_readValues.length; i++) {
                if (!m_readValues[i].isMissing()) {
                    m_cells[i - 1] = m_readValues[i].getDataCell();

                    // invalidate for next iteration
                    m_readValues[i].setMissing();
                } else {
                    m_cells[i - 1] = MISSING_CELL;
                }
            }
            // cells are copied in row
            m_delegate.addRowToTable(new DefaultRow(new RowKey(m_rowKeyReadValue.getString()), m_cells));
        }

        private static final class NullableReadValue {

            private final ReadValue m_delegate;

            private final BufferedAccess m_access;

            NullableReadValue(final ValueFactory<ReadAccess, ?> factory, final BufferedAccess access) {
                m_delegate = factory.createReadValue(access);
                m_access = access;
            }

            public ReadValue getDelegate() {
                return m_delegate;
            }

            public final DataCell getDataCell() {
                return m_delegate.getDataCell();
            }

            public final boolean isMissing() {
                return m_access.isMissing();
            }

            public final void setMissing() {
                m_access.setMissing();
            }

        }
    }

}

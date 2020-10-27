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
import org.knime.core.data.container.BufferedAccessSpecMapper.BufferedAccess;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.CustomKeyRowContainer;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.ValueSchema;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.ReadAccess;
import org.knime.core.data.v2.access.WriteAccess;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;

/**
 * Legacy implementation for CustomKeyRowContainer using {@link DataContainer}s as storage backend.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
final class CustomKeyBufferedRowContainer implements CustomKeyRowContainer {

    private static final DataCell MISSING_CELL = DataType.getMissingCell();

    private final BufferedDataContainer m_delegate;

    private final DataCell[] m_cells;

    private final NullableReadValue[] m_readValues;

    private final WriteValue<?>[] m_writeValues;

    // volatile thingies
    private RowKey m_rowKey;

    CustomKeyBufferedRowContainer(final BufferedDataContainer delegate, final ValueSchema schema) {
        m_delegate = delegate;
        m_cells = new DataCell[schema.getNumColumns() - 1];
        m_readValues = new NullableReadValue[schema.getNumColumns()];
        m_writeValues = new WriteValue[schema.getNumColumns()];
        for (int i = 0; i < schema.getNumColumns(); i++) {
            final BufferedAccess access = schema.getAccessSpecAt(i).accept(BufferedAccessSpecMapper.INSTANCE);
            final ValueFactory<ReadAccess, WriteAccess> factory = schema.getFactoryAt(i);
            m_readValues[i] = new NullableReadValue(factory, access);
            m_writeValues[i] = factory.createWriteValue(access);
        }

    }

    @Override
    public void push() {
        if (m_rowKey == null) {
            throw new IllegalStateException("Implementation error. RowKey not set before calling push().");
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

        m_delegate.addRowToTable(new DefaultRow(m_rowKey, m_cells));
    }

    @Override
    public <W extends WriteValue<?>> W getWriteValue(final int index) {
        @SuppressWarnings("unchecked")
        final W cast = (W)m_writeValues[index + 1];
        return cast;
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
            m_delegate.close();
            m_delegate.getBufferedTable().close();
        }
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
    public void setRowKey(final String key) {
        m_rowKey = new RowKey(key);
    }

    @Override
    public void setRowKey(final RowKeyValue key) {
        m_rowKey = new RowKey(key.getString());
    }

    private static final class NullableReadValue {

        private final ReadValue m_delegate;

        private final BufferedAccess m_access;

        NullableReadValue(final ValueFactory<ReadAccess, ?> factory, final BufferedAccess access) {
            m_delegate = factory.createReadValue(access);
            m_access = access;
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

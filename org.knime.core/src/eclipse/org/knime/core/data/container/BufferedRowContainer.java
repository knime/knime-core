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
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.schema.ValueSchema;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.table.access.BufferedAccesses;
import org.knime.core.table.access.BufferedAccesses.BufferedAccess;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.WriteAccess;

/**
 * Legacy implementation for {@link RowContainer} using {@link BufferedDataContainer} as storage backend.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
final class BufferedRowContainer implements RowContainer, RowWriteCursor {

    private static final DataCell MISSING_CELL = DataType.getMissingCell();

    private final BufferedDataContainer m_delegate;

    private final ValueSchema m_schema;

    private final int m_numCells;

    /**
     * Lazily initialized {@code DataValue} materialization helpers.
     * <p>
     * The default implementation of {@link DataValue#materializeDataCell()} throws
     * {@code UnsupportedOperationException}. If that ever happens, the value is materialized using the
     * {@link ValueSchema#getValueFactory(int) ValueFactory} provided by the schema.
     */
    private final Materializer<?>[] m_materializers;

    BufferedRowContainer(final BufferedDataContainer delegate, final ValueSchema schema) {
        m_delegate = delegate;
        m_schema = schema;
        m_numCells = m_schema.numFactories() - 1;
        m_materializers = new Materializer[m_numCells];
    }

    @Override
    public ValueSchema getSchema() {
        return m_schema;
    }

    @Override
    public RowWriteCursor createCursor() {
        return this;
    }

    @Override
    public void commit(final RowRead row) {
        if (row instanceof DataRowRead d) {
            m_delegate.addRowToTable(d.getDelegate());
        } else {
            final RowKey rowId = new RowKey(row.getRowKey().getString());
            final DataCell[] cells = new DataCell[m_numCells];

            for (int i = 0; i < cells.length; ++i) {
                if (row.isMissing(i)) {
                    cells[i] = MISSING_CELL;
                } else {
                    try {
                        cells[i] = row.getValue(i).materializeDataCell();
                    } catch (UnsupportedOperationException ex) {
                        if (m_materializers[i] == null) {
                            m_materializers[i] = new Materializer<>(m_schema.getValueFactory(i + 1));
                        }
                        cells[i] = m_materializers[i].materializeDataCell(row.getValue(i));
                    }
                }
            }

            // cells are copied in row, BlobSupportDataRow because it saves one row creation in the Buffer class
            m_delegate.addRowToTable(new BlobSupportDataRow(rowId, cells));
        }
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

    /**
     * @return the delegate container, for unit tests.
     */
    BufferedDataContainer getDelegate() {
        return m_delegate;
    }

    private static class Materializer<D extends DataValue> {
        private final ReadValue m_read;

        private final WriteValue<D> m_write;

        <R extends ReadAccess, W extends WriteAccess> Materializer(final ValueFactory<R, W> factory) {
            BufferedAccess buf = BufferedAccesses.createBufferedAccess(factory.getSpec());
            m_read = factory.createReadValue((R)buf);
            m_write = (WriteValue<D>)factory.createWriteValue((W)buf);
        }

        DataCell materializeDataCell(final D value) {
            m_write.setValue(value);
            return m_read.getDataCell();
        }
    }
}

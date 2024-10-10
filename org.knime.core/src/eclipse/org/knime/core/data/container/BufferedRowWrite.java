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
 *   21 Sept 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.container;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.v2.DataRowRowRead;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.RowKeyReadValue;
import org.knime.core.data.v2.RowKeyWriteValue;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowWrite;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.schema.ValueSchema;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.table.access.BufferedAccesses;
import org.knime.core.table.access.BufferedAccesses.BufferedAccess;
import org.knime.core.table.access.ReadAccess;

/**
 * <p>Implementation of row write that buffers writes and offers to produce {@link DataRow}s with the current
 * buffered contents, invalidating its buffer in the process.
 *
 * <p>This particular implementation allows setting the data row very cheaply via {@link #setFrom(RowRead)},
 * if the passed row read is a {@link DataRowRowRead}, i.e. it is already only a wrapper around a data row.
 *
 * @since 5.4
 */
// extracted from DataRowContainer class in 5.4
public final class BufferedRowWrite implements RowWrite, RowRead {

    private final BufferedRowWrite.NullableReadValue[] m_readValues;

    private final WriteValue<?>[] m_writeValues;

    private final RowKeyReadValue m_rowKeyReadValue;

    /* two fields below were added as part of AP-23029 */

    /** If any field was set to be missing after a row content was set via {@link #setFrom(RowRead)}. */
    private final boolean[] m_forcedMissings;

    /** A row that was set via {@link #setFrom(RowRead)} - in most of the cases that data will come from
     * another table whose cursor is backed by a {@link BufferedDataTable}. */
    private DataRow m_currentRowWhenCallingSetFrom;

    /**
     * Creates a new row write using the given value schema and data row consumer.
     *
     * @param schema schema to use for rows
     */
    public BufferedRowWrite(final ValueSchema schema) {
        int numFactories = schema.numFactories();
        // according to JavaDoc of `numFactories()`, this is "num columns + 1", so at least 1, even if we have no cols
        CheckUtils.checkState(numFactories >= 1,
                "Expected at least one value factory, was: %d".formatted(numFactories));
        m_readValues = new BufferedRowWrite.NullableReadValue[numFactories];
        m_writeValues = new WriteValue[numFactories];
        m_forcedMissings = new boolean[numFactories];

        for (int i = 0; i < numFactories; i++) {//NOSONAR
            var valueFactory = schema.getValueFactory(i);
            final var access = BufferedAccesses.createBufferedAccess(valueFactory.getSpec());
            m_readValues[i] = new NullableReadValue(valueFactory, access);
            m_writeValues[i] = valueFactory.createWriteValue(access);
        }

        m_rowKeyReadValue = (RowKeyReadValue)m_readValues[0].getDelegate(); // NOSONAR we have at least one factory
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
        // +1 to account for the row key
        m_readValues[index + 1].setMissing();
        m_forcedMissings[index + 1] = true;
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
        // reset all read value accesses and m_forceMissing fields in case they were set/called prior
        // 'setFrom' (corner case)
        for (int i = 0; i < m_readValues.length; i++) {
            m_readValues[i].setMissing();
            m_forcedMissings[i] = false;
        }
        if (row instanceof DataRowRowRead dataRowRead) {
            // special case where the new table api is used but the workflow is using row backend,
            // eg. new row filter (in 5.3) copying input to output, see AP-23029
            m_currentRowWhenCallingSetFrom = dataRowRead.getDataRow();
        } else {
            setRowKey(row.getRowKey());
            final var numCells = row.getNumColumns();
            for (var i = 0; i < numCells; i++) {
                if (row.isMissing(i)) {
                    setMissing(i);
                } else {
                    m_writeValues[i + 1].setValue(row.getValue(i));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>Important:</b> After calling this method, the current buffer is reset.
     */
    @Override
    public DataRow materializeDataRow() {
        final var rowViaSetFrom = addRowIfSetViaSetFrom();
        if (rowViaSetFrom != null) {
            return rowViaSetFrom;
        }
        // TODO handle case where no row key is required?
        if (m_readValues[0].isMissing()) {
            throw new IllegalStateException("RowKey not set.");
        }

        DataCell[] cells = new DataCell[m_readValues.length - 1];
        // We have to loop once to reset our VolatileAccesses after reading
        for (int i = 1; i < m_readValues.length; i++) {
            if (!m_readValues[i].isMissing()) {
                cells[i - 1] = m_readValues[i].getDataCell();

                // invalidate for next iteration
                m_readValues[i].setMissing();
                m_forcedMissings[i] = false;
            } else {
                cells[i - 1] = BufferedRowContainer.MISSING_CELL;
            }
        }
        // cells are copied in row, BlobSupportDataRow because it saves one row creation in the Buffer class
        return new BlobSupportDataRow(new RowKey(m_rowKeyReadValue.getString()), cells);
    }

    /**
     * Add a row to the table if it was set via {@link #setFrom(RowRead)}.
     *
     * @return {@code true} if a row was added, {@code false} otherwise (then the individual fields will be set).
     * @throws X
     */
    private DataRow addRowIfSetViaSetFrom() {
        if (m_currentRowWhenCallingSetFrom == null) {
            return null;
        }
        DataCell[] cellCopies = null;
        RowKey rowKey = null;
        if (!m_readValues[0].isMissing()) {
            rowKey = new RowKey(m_rowKeyReadValue.getString());
            cellCopies = copyCells(m_currentRowWhenCallingSetFrom);
        }
        // check for each field if a value was set manually after `setFrom` was called.
        for (var i = 1; i < m_readValues.length; i++) {
            if (!m_readValues[i].isMissing() || m_forcedMissings[i]) {
                if (cellCopies == null) {
                    rowKey = m_currentRowWhenCallingSetFrom.getKey();
                    cellCopies = copyCells(m_currentRowWhenCallingSetFrom);
                }
                if (m_forcedMissings[i]) {
                    cellCopies[i - 1] = BufferedRowContainer.MISSING_CELL;
                } else {
                    cellCopies[i - 1] = m_readValues[i].getDataCell();
                }
            }
        }
        final DataRow row;
        if (cellCopies == null) {
            row = m_currentRowWhenCallingSetFrom;
        } else {
            row = new BlobSupportDataRow(rowKey, cellCopies);
            for (var i = 0; i < m_readValues.length; i++) {
                m_readValues[i].setMissing();
                m_forcedMissings[i] = false;
            }
        }
        m_currentRowWhenCallingSetFrom = null;
        return row;
    }

    private static DataCell[] copyCells(final DataRow currentRowWhenCallingSetFrom) {
        DataCell[] cellCopies;
        cellCopies = new DataCell[currentRowWhenCallingSetFrom.getNumCells()];
        for (var j = 0; j < cellCopies.length; j++) {
            cellCopies[j] = currentRowWhenCallingSetFrom instanceof BlobSupportDataRow bsr
                    ? bsr.getRawCell(j) : currentRowWhenCallingSetFrom.getCell(j);
        }
        return cellCopies;
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

        public DataCell getDataCell() {
            return m_delegate.getDataCell();
        }

        public boolean isMissing() {
            return m_access.isMissing();
        }

        public void setMissing() {
            m_access.setMissing();
        }

    }

    @Override
    public <D extends DataValue> D getValue(final int index) {
        return (D) m_readValues[index + 1].getDelegate().getDataCell();
    }

    @Override
    public boolean isMissing(final int index) {
        return m_readValues[index + 1].isMissing();
    }

    @Override
    public RowKeyValue getRowKey() {
        return m_rowKeyReadValue;
    }
}

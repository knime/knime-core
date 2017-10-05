/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * Created on Mar 18, 2013 by wiswedel
 */
package org.knime.base.node.preproc.columnlag;

import java.util.ArrayList;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.util.UniqueNameGenerator;

/**
 *
 * @author wiswedel
 */
final class LagColumnStreamableOperator extends StreamableOperator {

    private final LagColumnConfiguration m_configuration;
    private final DataTableSpec m_outSpec;
    private final int m_columnIndex;
    private long m_maxRows = -1; // stays -1 if used only during configure or if used in real streaming

    /**
     * @param inSpecs
     *
     */
    LagColumnStreamableOperator(final LagColumnConfiguration configuration, final DataTableSpec inSpec)
            throws InvalidSettingsException {
        m_configuration = configuration;
        if (m_configuration == null) {
            throw new InvalidSettingsException("No configuration available");
        }
        String colName = m_configuration.getColumn();
        m_columnIndex = inSpec.findColumnIndex(colName); // -1 if row id column or column not present
        if (colName != null && m_columnIndex < 0) {
            throw new InvalidSettingsException("Selected column \"" + colName + "\" does not exist");
        }
        String baseName = colName == null ? "RowID" : colName;
        DataType type = colName == null ? StringCell.TYPE : inSpec.getColumnSpec(colName).getType();
        UniqueNameGenerator gen = new UniqueNameGenerator(inSpec);
        int lag = m_configuration.getLag();
        DataColumnSpec[] newCols = new DataColumnSpec[lag];
        int lagInterval = m_configuration.getLagInterval();
        for (int i = 0; i < lag; i++) {
            String p = Integer.toString(lagInterval * (i + 1));
            newCols[i] = gen.newColumn(baseName + "(-" + p + ")", type);
        }
        m_outSpec = new DataTableSpec(inSpec, new DataTableSpec(newCols));
    }

    /**
     * @return the outSpecs
     */
    DataTableSpec getOutSpec() {
        return m_outSpec;
    }

    BufferedDataTable execute(final BufferedDataTable table, final ExecutionContext exec) throws Exception {
        long maxRows = table.size();
        int maxLag = m_configuration.getLag() * m_configuration.getLagInterval();
        if (m_configuration.isSkipInitialIncompleteRows()) {
            maxRows -= maxLag;
        }
        if (!m_configuration.isSkipLastIncompleteRows()) {
            maxRows += maxLag;
        }
        m_maxRows = maxRows;
        BufferedDataContainer output = exec.createDataContainer(m_outSpec);
        RowInput wrappedInput = new DataTableRowInput(table);
        DataContainerPortOutput wrappedOutput = new DataContainerPortOutput(output);
        runFinal(new PortInput[] {wrappedInput}, new PortOutput[] {wrappedOutput}, exec);
        return wrappedOutput.getTable();
    }


    /** {@inheritDoc} */
    @Override
    public void runFinal(final PortInput[] inputs, final PortOutput[] outputs,
                         final ExecutionContext exec) throws Exception {
        long counter = 0;
        int maxLag = m_configuration.getLagInterval() * m_configuration.getLag();
        RingBuffer ringBuffer = new RingBuffer(maxLag);

        RowInput input = (RowInput)inputs[0];
        RowOutput output = (RowOutput)outputs[0];
        int skippedFirstCount = !m_configuration.isSkipInitialIncompleteRows() ? -1
                : m_configuration.getLagInterval() * m_configuration.getLag();
        DataRow row;
        while ((row = input.poll()) != null) {
            if (counter >= skippedFirstCount) {
                DataCell[] newCells = getAdditionalCells(ringBuffer);
                output.push(copyWithNewCells(row, newCells));
            }
            DataCell toBeCached = m_columnIndex < 0 ? new StringCell(row.getKey().toString())
                : row.getCell(m_columnIndex);
            ringBuffer.add(toBeCached);
            setProgress(exec, counter, row);
            counter += 1;
        }

        if (!m_configuration.isSkipLastIncompleteRows()) {
            DataCell[] missings = new DataCell[input.getDataTableSpec().getNumColumns()];
            Arrays.fill(missings, DataType.getMissingCell());
            for (int i = 0; i < maxLag; i++) {
                DataRow missingRow = new DefaultRow("overflow-" + i, missings);
                DataCell[] newCells = getAdditionalCells(ringBuffer);
                output.push(copyWithNewCells(missingRow, newCells));
                ringBuffer.add(DataType.getMissingCell());
            }
        }
        output.close();
    }

    /**
     * @param row
     * @param newCells
     * @return
     */
    private static BlobSupportDataRow copyWithNewCells(final DataRow row, final DataCell[] newCells) {
        int oldCount = row.getNumCells();
        DataCell[] copiedCells = new DataCell[oldCount + newCells.length];
        int i;
        for (i = 0; i < oldCount; i++) {
            copiedCells[i] = row instanceof BlobSupportDataRow ? ((BlobSupportDataRow)row).getRawCell(i)
                    : row.getCell(i);
        }
        for (int j = 0; j < newCells.length; j++) {
            copiedCells[i + j] = newCells[j];
        }
        return new BlobSupportDataRow(row.getKey(), copiedCells);
    }

    private DataCell[] getAdditionalCells(final RingBuffer ringBuffer) {
        int lag = m_configuration.getLag();
        int lagInterval = m_configuration.getLagInterval();
        DataCell[] result = new DataCell[lag];
        for (int i = 0; i < lag; i++) {
            result[i] = ringBuffer.get((i + 1) * lagInterval);
        }
        return result;
    }

    private void setProgress(final ExecutionContext exec, final long counter, final DataRow row)
            throws CanceledExecutionException {
        StringBuilder progMessageBuilder = new StringBuilder("Added row ");
        progMessageBuilder.append(counter + 1);
        if (m_maxRows > 0) {
            progMessageBuilder.append("/").append(m_maxRows);
        }
        progMessageBuilder.append(" (\"").append(row).append("\")");
        if (m_maxRows > 0) {
            exec.setProgress(counter / (double)m_maxRows, progMessageBuilder.toString());
        } else {
            exec.setMessage(progMessageBuilder.toString());
        }
        exec.checkCanceled();
    }

    private static final class DataContainerPortOutput extends RowOutput {
        private final BufferedDataContainer m_container;

        DataContainerPortOutput(final BufferedDataContainer container) {
            m_container = container;
        }

        /** {@inheritDoc} */
        @Override
        public void push(final DataRow row) throws InterruptedException {
            m_container.addRowToTable(row);
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
            m_container.close();
        }

        BufferedDataTable getTable() {
            return m_container.getTable();
        }

    }


    private static final class RingBuffer {

        private final int m_capacity;
        private final ArrayList<DataCell> m_storageList;
        int m_nextIndex = 0;

        RingBuffer(final int capacity) {
            m_storageList = new ArrayList<DataCell>(capacity);
            m_capacity = capacity;
            for (int i = 0; i < capacity; i++) {
                m_storageList.add(DataType.getMissingCell());
            }
        }

        void add(final DataCell cell) {
            m_storageList.set(m_nextIndex, cell);
            m_nextIndex += 1;
            if (m_nextIndex >= m_capacity) {
                m_nextIndex = 0;
            }
        }

        DataCell get(final int requestIndex) {
            int index = m_nextIndex - requestIndex;
            if (index < 0) {
                index += m_capacity;
            }
            return m_storageList.get(index);
        }

    }

}

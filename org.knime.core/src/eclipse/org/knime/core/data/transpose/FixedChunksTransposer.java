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
 *   19 Apr 2022 (leon.wenzler): created
 */
package org.knime.core.data.transpose;

import java.util.stream.IntStream;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;

/**
 * Class to transpose a table on a fixed chunk size. This means, columns of the input table are transposed in chunks of
 * a fixed size to corresponding rows in the output table.
 *
 * @author Leon Wenzler, KNIME AG, Zurich, Switzerland
 */
public class FixedChunksTransposer extends AbstractTableTransposer {

    private final int m_chunkSize;

    private final int m_nrChunks;

    /**
     * @param inputTable table to transpose
     * @param exec for reporting progress and checking cancellation
     * @param chunkSize the number of columns to convert to rows at a time
     * @throws CanceledExecutionException
     */
    public FixedChunksTransposer(final BufferedDataTable inputTable, final ExecutionContext exec, final int chunkSize)
        throws CanceledExecutionException {
        super(inputTable, exec);
        CheckUtils.checkArgument(chunkSize > 0, "Chunk size must be at least 1.");
        m_chunkSize = chunkSize;
        m_nrChunks = (int)Math.ceil(m_dataTableSpec.getNumColumns() / (double)chunkSize);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transpose() throws CanceledExecutionException {
        for (var chunkIdx = 0; chunkIdx < m_nrChunks; chunkIdx++) {
            m_buffer.clear();

            // the column indices in the current chunk
            var colIdx = chunkIdx * m_chunkSize;
            final int colsInChunk = Math.min(m_chunkSize, m_rowsInOutTable - colIdx);
            final var indices = IntStream.range(colIdx, colIdx + colsInChunk).toArray();

            var rowIdx = 0;
            try (CloseableRowIterator iterator = m_inputTable
                .filter(new TableFilter.Builder().withMaterializeColumnIndices(indices).build()).iterator()) {
                while (iterator.hasNext()) {
                    DataRow row = iterator.next();
                    m_exec.setProgress(((rowIdx + 1) * (chunkIdx + 1)) / (double)(m_nrChunks * m_colsInOutTable),
                        () -> "Transpose row \"" + row.getKey().getString() + "\" to column.");
                    // iterate over chunk of columns and collect DataCells
                    m_buffer.storeRowInColumns(row, colIdx, colIdx + colsInChunk);
                    handleIfCanceled();
                    rowIdx++;
                }
            }

            // add chunk of rows to buffer
            for (DataRow row : m_buffer.getRows()) {
                m_exec.setMessage(() -> "Adding row \"" + row.getKey() + "\" to table.");
                m_container.addRowToTable(row);
                handleIfCanceled();
            }
        }
        m_exec.setProgress(1.0, "Finished, closing buffer...");
        m_container.close();
    }
}

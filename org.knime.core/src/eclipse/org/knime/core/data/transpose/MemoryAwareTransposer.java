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

import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;

/**
 * Class to transpose a table with a dynamic chunk size. This means, columns of the input table are transposed in chunks
 * as large as they fit in memory. This brings a significant perfomance increase if the number of table columns is large
 * but the number of rows is small.
 *
 * @author Leon Wenzler, KNIME AG, Zurich, Switzerland
 */
public class MemoryAwareTransposer extends AbstractTableTransposer {

    /** Percentage of table that is temporarily discarded and then retried with smaller chunks. */
    private static final double PERCENTAGE_DISCARDED_TABLE = 0.5;

    private final NodeLogger m_logger = NodeLogger.getLogger(MemoryAwareTransposer.class);

    private final BooleanSupplier m_isMemoryLow;

    /**
     * @param inputTable table to transpose
     * @param exec for reporting progress and checking cancellation
     * @throws CanceledExecutionException
     */
    public MemoryAwareTransposer(final BufferedDataTable inputTable, final ExecutionContext exec)
        throws CanceledExecutionException {
        super(inputTable, exec);
        m_isMemoryLow = MemoryAlertSystem.getInstance()::isMemoryLow;
    }

    /**
     * Constructor for testing. Allows setting a memory alert system that will alert for testing purposes.
     *
     * @param inputTable table to transpose
     * @param exec for reporting progress and checking cancellation
     * @param testMemoryLow for testing purposes
     * @throws CanceledExecutionException
     */
    MemoryAwareTransposer(final BufferedDataTable inputTable, final ExecutionContext exec,
        final BooleanSupplier testMemoryLow) throws CanceledExecutionException {
        super(inputTable, exec);
        m_isMemoryLow = testMemoryLow;
    }

    /**
     * Attempts to construct all output rows at once. If a memory alert is detected, the number of columns to convert to
     * rows at a time (chunk size C) is dynamically reduced.
     *
     * If during a pass over the input table a memory alert is detected, the buffer holds C partially constructed output
     * rows. The chunk size is reduced to C' and the C-C' partially constructed output rows are discarded.
     *
     * {@inheritDoc}
     */
    @Override
    public void transpose() throws CanceledExecutionException {

        final var columnsInputTable = m_rowsInOutTable;

        // the number of columns to convert to rows during a pass over the input table
        var dynamicChunkSize = columnsInputTable;
        var chunkStart = 0;
        var rowProgess = 0;

        // iterate over input table columns
        do {
            // iterate over input table rows
            var transposeStatus = transposeSingleChunk(chunkStart, dynamicChunkSize, rowProgess);
            rowProgess = transposeStatus.getFirst();

            // if chunk transpose has been completed or chunk size has NOT changed
            if (rowProgess == m_colsInOutTable || transposeStatus.getSecond() == -1) {
                // we have transformed dynamicChunkSize columns to rows, so write them out
                for (DataRow row : m_buffer.getRows()) {
                    m_exec.setMessage(() -> "Adding row \"" + row.getKey() + "\" to table.");
                    m_container.addRowToTable(row);
                    handleIfCanceled();
                }
                m_buffer.clear();
                chunkStart += dynamicChunkSize;
                rowProgess = 0;
            } else {
                // updating/ shrinking dynamic chunk size
                dynamicChunkSize = transposeStatus.getSecond();
            }

        } while (chunkStart < columnsInputTable);
        m_exec.setProgress(1.0, "Finished, closing buffer...");
        m_container.close();
    }

    /**
     * Does a single transpose pass over the table. Uses dynamicChunkSize as chunk size of columns to process in one
     * batch. If this chunk size is too large, the memory alert system will notify and the chunk size will be reduced by
     * the factor of {@link MemoryAwareTransposer#PERCENTAGE_DISCARDED_TABLE}.
     *
     * @param chunkStart
     * @param dynamicChunkSize
     * @param rowProgess
     * @return pair (rowProgess, dynamicChunkSize) that encodes the transpose progress made so far
     *
     * @throws CanceledExecutionException
     */
    private Pair<Integer, Integer> transposeSingleChunk(final int chunkStart, int dynamicChunkSize, int rowProgress)
        throws CanceledExecutionException {

        // generates the indices for the current chunk size, from base index (colIdx) to chunk end
        final int colsInChunk = Math.min(m_rowsInOutTable - chunkStart, dynamicChunkSize);
        final int[] indices = IntStream.range(chunkStart, chunkStart + colsInChunk).toArray();

        // load only the columns in the current chunk, starting from the current row index
        final var tableFilter =
            new TableFilter.Builder().withMaterializeColumnIndices(indices).withFromRowIndex(rowProgress).build();

        try (CloseableRowIterator iterator = m_inputTable.filter(tableFilter).iterator()) {
            while (iterator.hasNext()) {
                DataRow row = iterator.next();
                setProgress(chunkStart, colsInChunk, rowProgress, row);
                // column-wise iteration through input data table
                m_buffer.storeRowInColumns(row, chunkStart, chunkStart + colsInChunk);
                rowProgress++;
                handleIfCanceled();
                // if memory is low (too much old columns / new rows collected), shrink chunkSize
                if (m_isMemoryLow.getAsBoolean()) {
                    dynamicChunkSize = (int)Math.ceil(colsInChunk * (1 - PERCENTAGE_DISCARDED_TABLE));
                    m_logger.debug("Memory condition: reduced chunk size by " + PERCENTAGE_DISCARDED_TABLE + " to "
                        + dynamicChunkSize + ", iterations completed: " + rowProgress);
                    m_buffer.truncateRows(dynamicChunkSize);
                    // try again with new iterator and new chunk size, starting from the next row
                    return new Pair<>(rowProgress, dynamicChunkSize);
                }
            }
        }
        // indicates that no changes in chunk size have been made
        return new Pair<>(rowProgress, -1);
    }

    /**
     * Uses current states in execution to determine and display transposing progress.
     *
     * @param chunkStart
     * @param dynamicChunkSize
     * @param rowIdx
     * @param row
     */
    private void setProgress(final int chunkStart, final int dynamicChunkSize, final long rowIdx, final DataRow row) {
        final float totalCells = (float)m_colsInOutTable * m_rowsInOutTable;
        m_exec.setProgress(/*completed cells*/ (chunkStart * m_colsInOutTable + dynamicChunkSize * rowIdx) / totalCells,
            () -> "Transpose row \"" + row.getKey().getString() + "\" to column.");
    }
}

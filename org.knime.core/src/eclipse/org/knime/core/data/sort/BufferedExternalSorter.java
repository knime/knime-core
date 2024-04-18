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
 *   Apr 2, 2024 (leonard.woerteler): created
 */
package org.knime.core.data.sort;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.apache.commons.lang3.Functions.FailableConsumer;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * External sorter for {@link BufferedDataTable}s.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
public final class BufferedExternalSorter extends ExternalSorter<BufferedDataTable> {

    private final MemoryAlertSystem m_memService;
    private final DataTableSpec m_outputSpec;

    /**
     * @param memService memory alert system
     * @param outputSpec table spec of the rows to sort
     * @param comparator comparator deciding the order of the rows in the output
     */
    public BufferedExternalSorter(final MemoryAlertSystem memService, final DataTableSpec outputSpec,
            final Comparator<RowRead> comparator) {
        super(comparator);
        m_memService = memService;
        m_outputSpec = outputSpec;
    }

    /**
     * @param memService memory alert system
     * @param outputSpec table spec of the rows to sort
     * @param comparator comparator deciding the order of the rows in the output
     * @param numRunsPerMerge maximum number of intermediate tables to merge at once
     */
    public BufferedExternalSorter(final MemoryAlertSystem memService, final DataTableSpec outputSpec,
            final Comparator<RowRead> comparator, final int numRunsPerMerge) {
        super(comparator, numRunsPerMerge);
        m_memService = memService;
        m_outputSpec = outputSpec;
    }


    @Override
    protected Optional<BufferedDataTable[]> createInitialRuns(final ExecutionContext exec,
            final BufferedDataTable inputTable, final Progress initialPhaseProgress, final AtomicLong rowsReadCounter)
            throws IOException, CanceledExecutionException {
        if (inputTable.size() < 2) {
            return Optional.empty();
        }

        try (final var rowCursor = inputTable.cursor()) {
            return Optional.of( //
                createInitialRuns(exec, rowCursor, inputTable.size(), initialPhaseProgress, rowsReadCounter));
        }
    }

    @Override
    protected BufferedDataTable[] createInitialRuns(final ExecutionContext exec, final RowCursor input,
            final long optNumRows, final Progress initialPhaseProgress, final AtomicLong rowsReadCounter)
            throws IOException, CanceledExecutionException {
        final BooleanSupplier hasLowMemory = () -> true; // m_memService.newIndicator()::lowMemoryActionRequired;
        return createInitialRunsGreedy(exec, hasLowMemory, 1_000_000, input, optNumRows, initialPhaseProgress,
            rowsReadCounter).toArray(BufferedDataTable[]::new);
    }

    @Override
    protected BufferedDataTable writeRun(final ExecutionContext exec, final Progress progress,
            final List<DataRow> buffer) throws CanceledExecutionException {
        final var numRowsWritten = new AtomicInteger();
        final int totalBufferSize = buffer.size();

        // instantiate the supplier only once
        final var fraction = progress.newFractionBuilder(numRowsWritten::longValue, totalBufferSize);
        Supplier<String> messageSupplier =
            () -> fraction.apply(new StringBuilder("Writing temporary table (row ")).append(")").toString();

        return writeTable(exec, true, container -> {
            for (var i = 0; i < totalBufferSize; i++) {
                progress.checkCanceled();

                // notify the progress monitor that something has changed
                progress.update(messageSupplier);

                // must not use Iterator#remove as it causes array copies
                container.addRowToTable(buffer.set(i, null));
                numRowsWritten.incrementAndGet();
            }

            // reset buffer (contains only `null`s now)
            buffer.clear();
        });
    }

    private BufferedDataTable writeTable(final ExecutionContext exec, final boolean forceOnDisk,
            final FailableConsumer<BufferedDataContainer, CanceledExecutionException> body)
            throws CanceledExecutionException {
        var container = exec.createDataContainer(m_outputSpec, true, forceOnDisk ? 0 : -1);
        try {
            body.accept(container);
            container.close();
            final var run = container.getTable();
            container = null;
            return run;
        } finally {
            if (container != null) {
                // cancellation or error
                container.close();
                exec.clearTable(container.getTable());
            }
        }
    }

    @Override
    protected BufferedDataTable mergeToTable(final ExecutionContext exec, final List<BufferedDataTable> runsToMerge,
            final boolean isLast, final Progress progress, final AtomicLong rowsProcessedCounter,
            final long numOutputRows, final Runnable beforeFinishing) throws CanceledExecutionException, IOException {
        // merge the `k` chunks together and add the combined chunk to the chunks writer
        try (final var mergeCursor = createMergedCursor(exec, runsToMerge)) {
            return writeTable(exec, false, container -> {
                while (mergeCursor.canForward()) {
                    progress.checkCanceled();
                    container.addRowToTable(mergeCursor.forward().materializeDataRow());
                    final var numRowsProcessed = rowsProcessedCounter.incrementAndGet();
                    progress.update(1.0 * numRowsProcessed / numOutputRows);
                }

                beforeFinishing.run();
            });
        }
    }

    @Override
    protected RowCursor createMergedCursor(final ExecutionContext exec, final List<BufferedDataTable> runsToMerge) {
        final var cursors = runsToMerge.stream() //
                .map(run -> new TableClearingRowCursor(exec, run)) //
                .toArray(RowCursor[]::new);
        return cursors.length == 1 ? cursors[0]
                : new KWayMergeCursor(m_comparator, cursors, m_outputSpec.getNumColumns());
    }

    @Override
    protected void clearTable(final ExecutionContext ctx, final BufferedDataTable table) {
        ctx.clearTable(table);
    }

    /**
     * Closeable row iterator that disposes the underlying table when it is closed.
     */
    private static final class TableClearingRowCursor implements RowCursor {

        private final ExecutionContext m_exec;

        private BufferedDataTable m_dataTable;
        private RowCursor m_cursor;

        TableClearingRowCursor(final ExecutionContext exec, final BufferedDataTable dataTable) {
            m_exec = exec;
            m_dataTable = dataTable;
        }

        @Override
        public int getNumColumns() {
            return m_dataTable.getDataTableSpec().getNumColumns();
        }

        @Override
        public boolean canForward() {
            if (m_dataTable == null) {
                // closed
                return false;
            }
            if (m_cursor == null) {
                // lazy initialization
                m_cursor = m_dataTable.cursor();
            }

            if (m_cursor.canForward()) {
                return true;
            }

            close();
            return false;
        }

        @Override
        public RowRead forward() {
            if (!canForward()) {
                return null;
            }
            return m_cursor.forward();
        }

        @Override
        public void close() {
            if (m_dataTable != null) {
                if (m_cursor != null) {
                    m_cursor.close();
                    m_cursor = null;
                }
                m_exec.clearTable(m_dataTable);
                m_dataTable = null;
            }
        }
    }
}

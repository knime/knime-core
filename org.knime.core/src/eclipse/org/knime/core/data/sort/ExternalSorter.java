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
 *   Mar 30, 2024 (leonard.woerteler): created
 */
package org.knime.core.data.sort;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.valueformat.NumberFormatter;

/**
 * Implementation of <a href="https://en.wikipedia.org/wiki/External_sorting">External Sorting</a> on both data tables
 * and row cursors.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 *
 * @param <T> type of tables to sort
 * @since 5.3
 */
public abstract class ExternalSorter<T> {

    /** Comparator determining the order of the rows in the output. */
    protected final Comparator<RowRead> m_comparator;

    /** Maximum number of temporary tables which are merged at once. */
    protected final int m_numRunsPerMerge;

    /**
     * Constructor using {@link AbstractTableSorter#DEF_MAX_OPENCONTAINER} as the maximum number of intermediate tables
     * to merge at once.
     *
     * @param comparator comparator for {@link RowRead}s while sorting the data
     */
    @SuppressWarnings("javadoc")
    protected ExternalSorter(final Comparator<RowRead> comparator) {
        this(comparator, AbstractTableSorter.DEF_MAX_OPENCONTAINER);
    }

    /**
     * @param comparator comparator for {@link RowRead}s while sorting the data
     * @param numRunsPerMerge maximum number of sorted intermediate tables merged at once
     */
    protected ExternalSorter(final Comparator<RowRead> comparator, final int numRunsPerMerge) {
        m_comparator = CheckUtils.checkArgumentNotNull(comparator, "Row comparator can't be `null`");
        CheckUtils.checkArgument(numRunsPerMerge > 1,
            "Minimum number of input tables for a merge is 2, found %d", numRunsPerMerge);
        m_numRunsPerMerge = numRunsPerMerge;
    }


    /* #############################################  Public Interface  ############################################# */

    /**
     * Sorts the given input table and returns a new table containing the same rows, but sorted according to this
     * sorter's comparator.
     *
     * @param inputTable input table to sort
     * @param exec execution context
     * @param optNumRows
     * @return sorted table
     * @throws CanceledExecutionException in case the execution was aborted
     * @throws IOException
     */
    public final Optional<T> sortedTable(final ExecutionContext exec, final T inputTable,
            final OptionalLong optNumRows) throws CanceledExecutionException, IOException { // NOSONAR
        return sortTable(exec, inputTable, optNumRows.orElse(-1L),
            (initialRuns, progress) -> initialRuns.mergeIntoTable(exec, progress, 0.5));
    }

    /**
     * Sorts the given input table and returns a cursor over its rows, sorted according to this sorter's comparator.
     *
     * @param inputTable input table to sort
     * @param exec execution context
     * @param optNumRows
     * @return sorted cursor
     * @throws CanceledExecutionException in case the execution was aborted
     * @throws IOException
     */
    public final Optional<RowCursor> sortedCursor(final ExecutionContext exec, final T inputTable,
            final OptionalLong optNumRows) throws CanceledExecutionException, IOException { // NOSONAR
        return sortTable(exec, inputTable, optNumRows.orElse(-1L),
            (initialRuns, progress) -> initialRuns.mergeIntoCursor(exec, progress, 0.5));
    }

    /**
     * Sorts the given row cursor's rows and returns a new table containing the same rows, but sorted according to this
     * sorter's comparator.
     *
     * @param rowCursor input rows to sort
     * @param exec execution context
     * @param optNumRows
     * @return sorted table
     * @throws CanceledExecutionException in case the execution was aborted
     * @throws IOException
     */
    public final Optional<T> sortedTable(final ExecutionContext exec, final RowCursor rowCursor,
            final OptionalLong optNumRows) throws CanceledExecutionException, IOException { // NOSONAR
        return sortCursor(exec, rowCursor, optNumRows.orElse(-1L),
            (initialRuns, progress) -> initialRuns.mergeIntoTable(exec, progress, 0.5));
    }

    /**
     * Sorts the given row cursor and returns a new cursor, sorted according to this sorter's comparator.
     *
     * @param rowCursor input rows to sort
     * @param exec execution context
     * @param optNumRows
     * @return sorted cursor
     * @throws CanceledExecutionException in case the execution was aborted
     * @throws IOException
     */
    public final Optional<RowCursor> sortedCursor(final ExecutionContext exec, final RowCursor rowCursor,
            final OptionalLong optNumRows) throws CanceledExecutionException, IOException { // NOSONAR
        return sortCursor(exec, rowCursor, optNumRows.orElse(-1L),
            (initialRuns, progress) -> initialRuns.mergeIntoCursor(exec, progress, 0.5));
    }


    /* #############################################  Abstract Methods  ############################################# */

    /**
     * Partitions the input table into an ordered sequence of sorted intermediate tables so that every row of the input
     * table is contained in exactly one initial run and if two rows <i>r<sub>1</sub></i> and <i>r<sub>2</sub></i> are
     * in two different runs so that <i>r<sub>1</sub></i>'s run appears after <i>r<sub>2</sub></i>'s in the output,
     * either <i>r<sub>1</sub></i> appears after <i>r<sub>2</sub></i> in the input table or <i>r<sub>1</sub></i>
     * compares as strictly greater than <i>r<sub>2</sub></i>.
     *
     * @param exec execution context
     * @param inputTable input table to partition into sorted runs
     * @param progress progress reporting
     * @param rowsReadCounter counter for the number of rows processed while creating runs
     * @return list of sorted runs, or {@link Optional#empty()} if the input table is already sorted
     * @throws CanceledExecutionException if execution was canceled
     * @throws IOException from the backend
     */
    protected abstract Optional<T[]> createInitialRuns(ExecutionContext exec, T inputTable, Progress progress,
            AtomicLong rowsReadCounter) throws IOException, CanceledExecutionException;

    /**
     * Partitions the input cursor's rows into an ordered sequence of sorted intermediate tables so that every row of
     * the input cursor's is contained in exactly one initial run and if two rows <i>r<sub>1</sub></i> and
     * <i>r<sub>2</sub></i> are in two different runs so that <i>r<sub>1</sub></i>'s run appears after
     * <i>r<sub>2</sub></i>'s in the output, either <i>r<sub>1</sub></i> appears after <i>r<sub>2</sub></i> in the input
     * table or <i>r<sub>1</sub></i> compares as strictly greater than <i>r<sub>2</sub></i>.
     *
     * @param exec execution context
     * @param input input cursor to partition into sorted runs
     * @param optNumRows number of rows in the input, {@code -1} if unknown
     * @param progress progress reporting
     * @param rowsReadCounter counter for the number of rows processed while creating runs
     * @return list of sorted runs, or {@link Optional#empty()} if the input table is already sorted
     * @throws CanceledExecutionException if execution was canceled
     * @throws IOException from the backend
     */
    protected abstract T[] createInitialRuns(ExecutionContext exec, RowCursor input, long optNumRows, Progress progress,
            AtomicLong rowsReadCounter) throws IOException, CanceledExecutionException;

    /**
     * Writes a single run of {@link DataRow}s out as a run.
     *
     * @param exec execution context
     * @param progress progress reporting, only to update the message and check for cancellation
     * @param buffer in-memory buffer of rows to write out
     * @return created run as a table
     * @throws CanceledExecutionException if execution was canceled
     * @throws IOException from the backend
     */
    protected abstract T writeRun(ExecutionContext exec, Progress progress, List<DataRow> buffer)
            throws CanceledExecutionException, IOException;

    /**
     * Creates a cursor that merges the rows of the given runs into one sorted sequence of rows.
     *
     * @param exec execution context
     * @param runsToMerge runs to be merged
     * @return sorted row cursor
     */
    protected abstract RowCursor createMergedCursor(ExecutionContext exec, List<T> runsToMerge);

    /**
     * Merges the given list of runs into one sorted table.
     *
     * @param exec execution context
     * @param runsToMerge list of runs to be merged
     * @param isLast indicating whether or not this is the last merge, creating the final output table
     * @param progress progress reporting
     * @param rowsProcessedCounter updatable counter for the number of rows that have already been written to the output
     * @param numOutputRows total number of rows in all input runs combined
     * @param beforeFinishing action to be performed right before the output table is closed
     * @return merged table
     * @throws CanceledExecutionException if execution was canceled
     * @throws IOException from the backend
     */
    protected abstract T mergeToTable(ExecutionContext exec, List<T> runsToMerge, boolean isLast, Progress progress,
            AtomicLong rowsProcessedCounter, long numOutputRows, Runnable beforeFinishing)
            throws CanceledExecutionException, IOException;

    /**
     * Clears the table from the execution context and frees all associated resources.
     *
     * @param ctx execution context
     * @param table table to clear
     */
    protected abstract void clearTable(ExecutionContext ctx, T table);


    /* ################################################## Plumbing ################################################## */

    /**
     * Uses the {@link BufferedDataTableSorter}'s greedy strategy for producing initial runs from the input.
     *
     * @param exec execution context
     * @param hasLowMemory observable indicating whether or not memory should be freed to avoid an OOM crash
     * @param minRunSize minimium number of rows in a run
     * @param input input row cursor
     * @param optNumRows number of rows in the input, {@code -1} if unknown
     * @param progress progress reporting
     * @param rowsReadCounter for reporting the number of rows read from the input in real time
     * @return list of runs
     * @throws CanceledExecutionException if execution was canceled
     * @throws IOException from the backend
     */
    protected final List<T> createInitialRunsGreedy(final ExecutionContext exec, final BooleanSupplier hasLowMemory,
            final int minRunSize, final RowCursor input, final long optNumRows, final Progress progress,
            final AtomicLong rowsReadCounter) throws IOException, CanceledExecutionException {

        // adapt the comparator to `DataRow`
        final var readL = new RowReadAdapter();
        final var readR = new RowReadAdapter();
        final Comparator<DataRow> rowComparator =
            (r1, r2) -> m_comparator.compare(readL.withDataRow(r1), readR.withDataRow(r2));

        final var buffer = new ArrayList<DataRow>();
        final var runsContainer = new ArrayList<T>();
        final Supplier<String> fillingBuffer = () -> "Filling in-memory buffer";
        try {

            while (input.canForward()) {
                progress.checkCanceled();

                buffer.add(input.forward().materializeDataRow());
                final var rowNo = rowsReadCounter.incrementAndGet();

                if (optNumRows > 0) {
                    progress.update(1.0 * rowNo / optNumRows, fillingBuffer);
                } else {
                    progress.update(fillingBuffer);
                }

                if (hasLowMemory.getAsBoolean() && buffer.size() >= minRunSize) {
                    flushBuffer(exec, progress, buffer, rowComparator, runsContainer);
                }
            }

            if (!buffer.isEmpty()) {
                // some rows are not yet written to disk
                flushBuffer(exec, progress, buffer, rowComparator, runsContainer);
            }

            // everything went well, extract runs from the container and return them
            final var runs = new ArrayList<>(runsContainer);
            runsContainer.clear();
            return runs;
        } finally {
            for (final var leftover : runsContainer) {
                // can only happen if an exception has been thrown, clean up
                clearTable(exec, leftover);
            }
        }
    }

    private void flushBuffer(final ExecutionContext exec, final Progress initialPhaseProgress,
            final List<DataRow> buffer, final Comparator<DataRow> rowComparator, final List<T> out)
                    throws CanceledExecutionException, IOException {
        // sort buffer
        initialPhaseProgress.update(() -> "Sorting in-memory buffer");
        Collections.sort(buffer, rowComparator);

        // write buffer to disk
        out.add(writeRun(exec, initialPhaseProgress, buffer));
    }

    /**
     * Wrapper for {@link ExecutionMonitor} which is {@link Closeable closeable} to enforce completion of each stage.
     */
    protected static final class Progress implements AutoCloseable {

        /** Pattern matching a single numeric digit. */
        static final Pattern ANY_DIGIT_PATTERN = Pattern.compile("\\d");

        /** Space character that's exactly as wide as a single digit. */
        static final String FIGURE_SPACE = "\u2007";

        private final ExecutionMonitor m_monitor;

        private final NumberFormatter m_progressNumberFormat;

        Progress(final ExecutionMonitor monitor) {
            this(monitor, createNumberFormat());
        }

        private Progress(final ExecutionMonitor monitor, final NumberFormatter formatter) {
            m_monitor = monitor;
            m_progressNumberFormat = formatter;
        }

        private static NumberFormatter createNumberFormat() {
            try {
                return NumberFormatter.builder().setGroupSeparator(",").build();
            } catch (InvalidSettingsException ex) {
                throw new IllegalStateException(ex);
            }
        }

        /**
         * Creates a sub-progress contributing the given fraction towards this monitor's progress.
         *
         * @param fraction fraction of this monitor's contributed by the sub-progress monitor
         * @return the (closeable) sub-monitor
         */
        public Progress createSubProgress(final double fraction) {
            return new Progress(m_monitor.createSubProgress(fraction), m_progressNumberFormat);
        }

        /**
         * Sets the given message supplier for the current level of the progress reporting.
         *
         * @param messageSupplier supplier for the current level's message
         */
        public void update(final Supplier<String> messageSupplier) {
            m_monitor.setMessage(messageSupplier);
        }

        /**
         * Reports the given progress (between 0 and 1) and sets the given message supplier for the current level of the
         * progress reporting.
         *
         * @param progress numeric progress as a fraction of {@code 1.0}
         * @param messageSupplier supplier for the current level's message
         */
        public void update(final double progress, final Supplier<String> messageSupplier) {
            m_monitor.setProgress(progress, messageSupplier);
        }

        /**
         * Reports the given progress (between 0 and 1) for the current level of the progress reporting.
         *
         * @param progress numeric progress as a fraction of {@code 1.0}
         */
        public void update(final double progress) {
            m_monitor.setProgress(progress);
        }

        /**
         * Checks whether or not the execution has been canceled.
         *
         * @throws CanceledExecutionException in case of cancellation
         */
        public void checkCanceled() throws CanceledExecutionException {
            m_monitor.checkCanceled();
        }

        /**
         * Creates a function that adds a nicely formatted, padded fraction of the form {@code " 173/2065"} to a given
         * {@link StringBuilder} that reflects the current value of the given supplier {@code currentValue}. The padding
         * with space characters tries to minimize jumping in the UI.
         *
         * @param currentValue supplier for the current numerator
         * @param total fixed denominator
         * @return function that modified the given {@link StringBuilder} and returns it for convenience
         */
        public UnaryOperator<StringBuilder> newFractionBuilder(final LongSupplier currentValue, final long total) {
            // only computed once
            final var totalStr = m_progressNumberFormat.format(total);
            final var paddingStr = ANY_DIGIT_PATTERN.matcher(totalStr).replaceAll(FIGURE_SPACE).replace(',', ' ');

            return sb -> {
                // computed every time a progress message is requested
                final var currentStr = m_progressNumberFormat.format(currentValue.getAsLong());
                final var padding = paddingStr.substring(0, Math.max(totalStr.length() - currentStr.length(), 0));
                return sb.append(padding).append(currentStr).append("/").append(totalStr);
            };
        }

        /**
         * @return formatter for {@code long} which inserts group separators
         */
        public LongFunction<String> getLongFormatter() {
            return m_progressNumberFormat::format;
        }

        @Override
        public void close() {
            m_monitor.setProgress(1.0, (String)null);
        }
    }

    /* ############################################################################################################## */


    private final MergePhase initialRunsFromTable(final ExecutionContext exec, final T inputTable,
            final long optNumRows, final Progress progress) throws IOException, CanceledExecutionException {
        final var numRowsRead = new AtomicLong();
        if (optNumRows >= 0) {
            if (optNumRows < 2) {
                // nothing to do
                return null;
            }
            final var rowFracBuilder = progress.newFractionBuilder(numRowsRead::get, optNumRows);
            progress.update( //
                () -> rowFracBuilder.apply(new StringBuilder("Reading data (row ")).append(")").toString());
        } else {
            final var formatter = progress.getLongFormatter();
            progress.update(() -> new StringBuilder("Reading data (row ").append(formatter.apply(numRowsRead.get())) //
                .append(")").toString());
        }

        try (final var initialPhaseProgress = progress.createSubProgress(0.5)) {
            final var runs = createInitialRuns(exec, inputTable, initialPhaseProgress, numRowsRead);
            if (runs.isEmpty()) {
                // `createInitialRuns` indicates that the input table is already sorted
                return null;
            }

            return new MergePhase(exec, runs.get(), numRowsRead.get());
        }
    }

    private final MergePhase initialRunsFromCursor(final ExecutionContext exec, final RowCursor rowCursor,
            final long optNumRows, final Progress progress) throws IOException, CanceledExecutionException {
        final var numRowsRead = new AtomicLong();
        if (optNumRows >= 0) {
            if (optNumRows < 2) {
                // nothing to do
                return null;
            }
            final var rowFracBuilder = progress.newFractionBuilder(numRowsRead::get, optNumRows);
            progress.update( //
                () -> rowFracBuilder.apply(new StringBuilder("Reading data (row ")).append(")").toString());
        } else {
            final var formatter = progress.getLongFormatter();
            progress.update(() -> new StringBuilder("Reading data (row ").append(formatter.apply(numRowsRead.get())) //
                .append(")").toString());
        }

        try (final var initialPhaseProgress = progress.createSubProgress(0.5)) {
            final var runs = createInitialRuns(exec, rowCursor, optNumRows, initialPhaseProgress, numRowsRead);
            return new MergePhase(exec, runs, numRowsRead.longValue());
        }
    }

    private interface SortingResultExtractor<T, R> {
        R extract(ExternalSorter<T>.MergePhase mergePhase, Progress progress)
                throws CanceledExecutionException, IOException;
    }

    private <R> Optional<R> sortTable(final ExecutionContext exec, final T inputTable, final long optNumRows,
            final SortingResultExtractor<T, R> extractor) throws CanceledExecutionException, IOException {
        try (final var progress = new Progress(exec);
                final var initialRuns = initialRunsFromTable(exec, inputTable, optNumRows, progress)) {
            return initialRuns == null ? Optional.empty() : Optional.of(extractor.extract(initialRuns, progress));
        }
    }

    private <R> Optional<R> sortCursor(final ExecutionContext exec, final RowCursor rowCursor,
            final long optNumRows, final SortingResultExtractor<T, R> extractor)
            throws CanceledExecutionException, IOException {
        try (final var progress = new Progress(exec);
                final var initialRuns = initialRunsFromCursor(exec, rowCursor, optNumRows, progress)) {
            return initialRuns == null ? Optional.empty() : Optional.of(extractor.extract(initialRuns, progress));
        }
    }

    private static final class RowReadAdapter implements RowRead {

        private DataRow m_currentRow;

        RowRead withDataRow(final DataRow currentRow) {
            m_currentRow = currentRow;
            return this;
        }

        @Override
        public RowKeyValue getRowKey() {
            return m_currentRow.getKey();
        }

        @Override
        public DataCell getAsDataCell(final int index) {
            return m_currentRow.getCell(index);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <D extends DataValue> D getValue(final int index) {
            return (D)getAsDataCell(index);
        }

        @Override
        public int getNumColumns() {
            return m_currentRow.getNumCells();
        }

        @Override
        public boolean isMissing(final int index) {
            return getAsDataCell(index).isMissing();
        }
    }

    /**
     * The merge phase of the on-disk sorting operation consolidates a sorted sequence of pre-sorted temporary tables
     * (the <i>runs</i>) into a single sequence of sorted rows. It is {@link AutoCloseable closeable} because it has
     * to free the disk memory occupied by the runs even if the sorting operation is being aborted.
     */
    final class MergePhase implements AutoCloseable {

        private final ExecutionContext m_exec;
        private final Deque<T> m_runs;
        private final long m_numRows;

        MergePhase(final ExecutionContext exec, final T[] runs, final long numRows) {
            CheckUtils.checkState(runs.length > 0, "Empty list of tables to merge");
            m_exec = exec;
            m_runs = new ArrayDeque<>(List.of(runs));
            m_numRows = numRows;
        }

        public long getNumRows() {
            return m_numRows;
        }

        /**
         * Merges all chunks and returns a cursor over the sorted rows. The results of the last merge are not written
         * back to disk, instead the last merge is deferred until the cursor is used.
         *
         * @param exec execution context
         * @param progress progress for the merge phase
         * @param fraction fraction of the overall progress to be reported here
         * @return cursor over merged rows
         * @throws CanceledExecutionException if execution was canceled
         * @throws IOException from the backend
         */
        public RowCursor mergeIntoCursor(final ExecutionContext exec, final Progress progress, final double fraction)
                throws CanceledExecutionException, IOException {
            progress.update(() -> "Merging temporary tables");

            try (final var mergePhaseProgress = progress.createSubProgress(fraction)) {
                final int numLevels = computeNumLevels(m_numRunsPerMerge);
                mergeToLastLevel(exec, mergePhaseProgress, numLevels);

                // last merge, ownership of the chunks is transferred to the merge cursor
                final var runsToMerge = new ArrayList<>(m_runs);
                m_runs.clear();
                return createMergedCursor(exec, runsToMerge);
            }
        }

        /**
         * Merges all chunks and returns a sorted table.
         *
         * @param exec execution context
         * @param progress progress for the merge phase
         * @param fraction fraction of the overall progress to be reported here
         * @return sorted table
         * @throws CanceledExecutionException if the execution was canceled
         * @throws IOException from the backend
         */
        public T mergeIntoTable(final ExecutionContext exec, final Progress progress, final double fraction)
                throws CanceledExecutionException, IOException {
            progress.update(() -> "Merging temporary tables");

            try (final var mergePhaseProgress = progress.createSubProgress(fraction)) {
                final var numLevels = computeNumLevels(1);
                mergeToLastLevel(exec, mergePhaseProgress, numLevels);

                if (m_runs.size() > 1) {
                    // final merge round, different message
                    try (final var subProgress = mergePhaseProgress.createSubProgress(1.0 / numLevels)) { // NOSONAR
                        performMergeRound(exec, subProgress, true, () -> new StringBuilder("Writing result table"));
                    }
                }

                // make sure that no runs are left behind
                CheckUtils.checkState(m_runs.size() == 1, "Unexpected number of runs after merging: %d", m_runs.size());
                return m_runs.remove();
            }
        }

        /**
         * Performs rounds of merges on the list of runs until at most {@link ExternalSorter#m_numRunsPerMerge} runs
         * remain. those can later be merged into an output table or returned as a merging iterator.
         *
         * @param exec execution context
         * @param progress progress for the merge phase
         * @param numLevels number of levels of the merge tree
         * @throws CanceledExecutionException if the execution was canceled
         * @throws IOException from the backend
         */
        private void mergeToLastLevel(final ExecutionContext exec, final Progress mergePhaseProgress,
                final int numLevels) throws CanceledExecutionException, IOException {
            final var currentLevel = new AtomicInteger();
            final var levelFracBuilder = mergePhaseProgress.newFractionBuilder(currentLevel::get, numLevels);
            while (m_runs.size() > m_numRunsPerMerge) {
                // too many runs, perform one full merge round over the data to ensure stability
                currentLevel.incrementAndGet();
                try (final var subProgress = mergePhaseProgress.createSubProgress(1.0 / numLevels)) { // NOSONAR
                    performMergeRound(exec, subProgress, false,
                        () -> levelFracBuilder.apply(new StringBuilder("Merging level ")));
                }
            }
        }

        /**
         * Performs a single scan over all data, merging groups of {@link ExternalSorter#m_numRunsPerMerge} chunks.
         *
         * @param exec execution context
         * @param progress progress monitor
         * @param prefixSupplier prefix supplier for progress messages
         * @throws CanceledExecutionException if execution was canceled
         * @throws IOException from the backend
         */
        private void performMergeRound(final ExecutionContext exec, final Progress progress, final boolean isLast,
                final Supplier<StringBuilder> prefixSupplier) throws CanceledExecutionException, IOException {
            final var rowsTicker = new AtomicLong();

            // creates messages of the form "<prefix> (row XX/YYY)<additionalInfo>" with optional additional info
            final var additionalInfo = new AtomicReference<>("");
            final var rowsBuilder = progress.newFractionBuilder(rowsTicker::get, m_numRows);
            final Supplier<String> messageSupplier = () -> rowsBuilder.apply(prefixSupplier.get().append(" (row ")) //
                    .append(")").append(additionalInfo.get()).toString();
            progress.update(messageSupplier);

            var remainingInRound = m_runs.size();
            final var runsToMerge = new ArrayList<T>(m_numRunsPerMerge);
            while (remainingInRound > 1) {
                final var mergesRemaining = fractionRoundedUp(remainingInRound, m_numRunsPerMerge);
                final var mergedHere = fractionRoundedUp(remainingInRound, mergesRemaining);
                for (var i = 0; i < mergedHere; i++) {
                    runsToMerge.add(m_runs.removeFirst());
                }

                // merge the `k` runs together and add the combined run back to the queue
                m_runs.addLast(mergeToTable(exec, runsToMerge, isLast, progress, rowsTicker, m_numRows, () -> {
                    additionalInfo.set("; Closing table...");
                    progress.update(messageSupplier);
                }));
                additionalInfo.set("");
                runsToMerge.clear();
                remainingInRound -= mergedHere;
            }

            // it makes no sense to merge a single final run, just copy it over into the next round
            if (remainingInRound == 1) {
                m_runs.addLast(m_runs.removeFirst());
            }
        }

        /**
         * Computes the number of scans over the data the merge phase needs, i.e. the height of the merge tree.
         *
         * @param maxRemaining maximum number of runs allowed to remain after the merge
         * @return number of scans over the input data are performed by the {@link MergePhase}
         */
        private int computeNumLevels(final int maxRemaining) {
            var numLevels = 0;
            var numRuns = m_runs.size();
            while (numRuns > maxRemaining) {
                numRuns = fractionRoundedUp(numRuns, m_numRunsPerMerge);
                numLevels++;
            }
            return numLevels;
        }

        /**
         * Exact version of {@code (int)Math.ceil(1.0 * numerator / denominator)}.
         *
         * @param numerator numerator of the fraction, must be non-negative ({@code >= 0})
         * @param denominator denominator of the fraction, must be strictly positive ({@code > 0})
         * @return the result of dividing {@code numerator} by {@code denominator} and rounding the result up
         */
        private static int fractionRoundedUp(final int numerator, final int denominator) {
            return (numerator + denominator - 1) / denominator;
        }

        @Override
        public void close() {
            while (!m_runs.isEmpty()) {
                clearTable(m_exec, m_runs.poll());
            }
        }
    }
}

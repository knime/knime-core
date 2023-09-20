/*
 *
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   31.08.2011 (wiswedel): created
 */
package org.knime.core.data.sort;

import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.data.util.memory.MemoryAlertSystem.MemoryActionIndicator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * Class to sort a table. See <a href="package.html">package description</a> for details.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
abstract class AbstractTableSorter { // NOSONAR has to be abstract so the class hierarchy doesn't change

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractTableSorter.class);

    /** Representing column spec to sort according to the row key. */
    public static final DataColumnSpec ROWKEY_SORT_SPEC = new DataColumnSpecCreator("-ROWKEY -",
        DataType.getType(StringCell.class)).createSpec();

    /**
     * The maximum number of open containers. See {@link #setMaxOpenContainers(int)} for details.
     */
    @SuppressWarnings("javadoc")
    public static final int DEF_MAX_OPENCONTAINER = 40;

    private MemoryAlertSystem m_memService = MemoryAlertSystem.getInstance();

    private final DataTable m_inputTable;

    private final DataTableSpec m_dataTableSpec;

    private final long m_rowsInInputTable;

    /**
     * The maximal number of open containers. This has an effect when many containers must be merged.
     */
    private int m_maxOpenContainers = DEF_MAX_OPENCONTAINER;

    /**
     * Maximum number of rows. Only changed in unit test. Defaults to {@link Integer#MAX_VALUE}.
     */
    private int m_maxRowsPerChunk = Integer.MAX_VALUE;

    private boolean m_sortInMemory = false;

    /** The RowComparator to compare two DataRows (inner class). */
    private Comparator<DataRow> m_rowComparator;

    /**
     * Private constructor. Assigns input table, checks argument.
     *
     * @param inputTable Table to sort.
     * @param rowsCount The number of rows in the table
     * @throws NullPointerException If arg is null.
     */
    private AbstractTableSorter(final DataTable inputTable, final long rowsCount) {
        if (inputTable == null) {
            throw new NullPointerException("Argument must not be null."); // NOSONAR
        }
        m_dataTableSpec = inputTable.getDataTableSpec();
        // for BDT better use the appropriate derived class (blob handling)
        if (getClass().equals(AbstractTableSorter.class) && inputTable instanceof BufferedDataTable) {
            LOGGER.coding("Do not use a " + AbstractTableSorter.class.getSimpleName() + " to sort" + " a "
                + BufferedDataTable.class.getSimpleName() + " but use a "
                + BufferedDataTableSorter.class.getSimpleName());
        }
        m_inputTable = inputTable;
        m_rowsInInputTable = rowsCount;
    }

    /**
     * Package default constructor for the {@link AbstractColumnTableSorter}.
     *
     * @param rowsCount the row count
     * @param dataTableSpec the data table spec
     * @param rowComparator the comparator
     */
    AbstractTableSorter(final long rowsCount, final DataTableSpec dataTableSpec,
        final Comparator<DataRow> rowComparator) {
        m_dataTableSpec = dataTableSpec;
        m_rowComparator = rowComparator;
        m_inputTable = null;
        m_rowsInInputTable = rowsCount;
    }

    /**
     * Inits sorter on argument table with given row comparator.
     *
     * @param inputTable Table to sort.
     * @param rowsCount The number of rows in the table
     * @param rowComparator Passed to {@link #setRowComparator(Comparator)}.
     */
    @SuppressWarnings("javadoc")
    protected AbstractTableSorter(final DataTable inputTable, final long rowsCount,
            final Comparator<DataRow> rowComparator) {
        this(inputTable, rowsCount);
        setRowComparator(rowComparator);
    }

    /**
     * Inits table sorter using the sorting according to {@link #setSortColumns(Collection, boolean[])}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table
     * @param inclList Passed on to {@link #setSortColumns(Collection, boolean[])}.
     * @param sortAscending Passed on to {@link #setSortColumns(Collection, boolean[])}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     */
    @SuppressWarnings("javadoc")
    protected AbstractTableSorter(final DataTable inputTable, final long rowsCount, final Collection<String> inclList,
            final boolean[] sortAscending) {
        this(inputTable, rowsCount);
        setSortColumns(inclList, sortAscending);
    }

    /**
     * Inits table sorter using the sorting according to {@link #setSortColumns(Collection, boolean[], boolean)}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table
     * @param inclList Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @param sortAscending Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @param sortMissingsToEnd Passed on to {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     * @since 2.6
     */
    @SuppressWarnings("javadoc")
    protected AbstractTableSorter(final DataTable inputTable, final long rowsCount, final Collection<String> inclList,
            final boolean[] sortAscending, final boolean sortMissingsToEnd) {
        this(inputTable, rowsCount);
        setSortColumns(inclList, sortAscending, sortMissingsToEnd);
    }

    /** @param rowComparator the rowComparator to set */
    public final void setRowComparator(final Comparator<DataRow> rowComparator) {
        if (rowComparator == null) {
            throw new NullPointerException("Argument must not be null."); // NOSONAR
        }
        m_rowComparator = rowComparator;
    }

    /**
     * Sets sorting columns and order.
     *
     * @param inclList the list with the columns to sort; the first column name represents the first sort criteria, the
     *            second the second criteria and so on.
     *
     * @param sortAscending the sort order; each field corresponds to the column in the list of included columns. true:
     *            ascending false: descending
     */
    public final void setSortColumns(final Collection<String> inclList, final boolean[] sortAscending) {
        setSortColumns(inclList, sortAscending, false);
    }

    /**
     * Sets sorting columns and order.
     *
     * @param inclList the list with the columns to sort; the first column name represents the first sort criteria, the
     *            second the second criteria and so on.
     *
     * @param sortAscending the sort order; each field corresponds to the column in the list of included columns. true:
     *            ascending false: descending
     * @param sortMissingsToEnd Whether to sort missing values always to the end independent to the sort oder (if false
     *            missing values are always smaller than non-missings).
     * @since 2.6
     */
    public final void setSortColumns(final Collection<String> inclList, final boolean[] sortAscending,
        final boolean sortMissingsToEnd) {
        if (sortAscending == null || inclList == null) {
            throw new IllegalArgumentException("Argument must not be null.");
        }
        if (inclList.size() != sortAscending.length) {
            throw new IllegalArgumentException("Length of arguments vary: " + inclList.size() + " vs. "
                + sortAscending.length);
        }
        if (inclList.contains(null)) {
            throw new IllegalArgumentException("Argument array must not contain null: " + inclList);
        }
        if (!(inclList instanceof Set)) {
            Set<String> noDuplicatesSet = new HashSet<>(inclList);
            if (noDuplicatesSet.size() != inclList.size()) {
                throw new IllegalArgumentException("Argument collection must " + "not contain duplicates: " + inclList);
            }
        }
        final var spec = m_inputTable.getDataTableSpec();
        final var rc = RowComparator.on(spec);
        var i = 0;
        for (final String name : inclList) {
            final int index = spec.findColumnIndex(name);
            final boolean ascending = sortAscending[i];
            if (index == -1) {
                if (!ROWKEY_SORT_SPEC.getName().equals(name)) {
                    throw new IllegalArgumentException("Could not find column name:" + name);
                }
                rc.thenComparingRowKey(rk -> rk.withDescendingSortOrder(!ascending));
            } else {
                rc.thenComparingColumn(index,
                    col -> col.withDescendingSortOrder(!ascending).withMissingsLast(sortMissingsToEnd));
            }
            i++;
        }
        setRowComparator(rc.build());
    }

    /**
     * Get the number of maximum open containers. See {@link #setMaxOpenContainers(int)} for details.
     *
     * @return the maxOpenContainers
     */
    @SuppressWarnings("javadoc")
    public int getMaxOpenContainers() {
        return m_maxOpenContainers;
    }

    /**
     * Changes the number of maximum open containers (=files) during the sorting. Containers are used in the k-way merge
     * sort, the higher the number the fewer iterations in the final merge need to be done.
     *
     * <p>
     * The default is {@value #DEF_MAX_OPENCONTAINER}.
     *
     * @param value the maxOpenContainers to number of maximal open containers.
     * @throws IllegalArgumentException If argument is smaller or equal to 2.
     */
    @SuppressWarnings("javadoc")
    public void setMaxOpenContainers(final int value) {
        if (value <= 2) {
            throw new IllegalArgumentException("Invalid open container count: " + value);
        }
        m_maxOpenContainers = value;
    }

    /**
     * Set the maximum number of rows per chunk, defaults to {@link Integer#MAX_VALUE}. This field is modified from the
     * testing framework.
     *
     * @param maxRows the maxRows to set
     */
    void setMaxRows(final int maxRows) {
        m_maxRowsPerChunk = maxRows;
    }

    /**
     * Set memory service. Used in unit test.
     *
     * @param memService the memService to set
     */
    void setMemService(final MemoryAlertSystem memService) {
        m_memService = memService;
    }

    DataTable getInputTable() {
        return m_inputTable;
    }

    /**
     * @return the sortInMemory field, see {@link #setSortInMemory(boolean)} for details.
     */
    @SuppressWarnings("javadoc")
    public boolean getSortInMemory() { // NOSONAR name is fine
        return m_sortInMemory;
    }

    /**
     * Forces the sorting to happen in memory (if argument is true). This is not advisable as tables can be large. Note,
     * the sorting may also take place in memory if the table is small (see class description for details).
     *
     * <p>
     * This option is merely to ensure backward compatibility and should not be used anymore.
     *
     * <p>
     * The default value for this option is <b>false</b>.
     *
     * @param sortInMemory <code>true</code> if sorting should be done in memory, <code>false</code> if sorting should
     *            be done in a hybrid way as described in the class description.
     */
    public void setSortInMemory(final boolean sortInMemory) {
        m_sortInMemory = sortInMemory;
    }

    ChunksWriter newChunksWriter(final TableIOHandler tableIOHandler) {
        return new ChunksWriter(m_dataTableSpec, tableIOHandler);
    }

    /**
     * Sorts the table passed in the constructor according to the settings and returns the sorted output table.
     *
     * @param exec To report progress
     * @return The sorted output.
     * @throws CanceledExecutionException If canceled.
     */
    DataTable sortInternal(final ExecutionMonitor exec, final TableIOHandler dataHandler)
            throws CanceledExecutionException {
        DataTable result;
        final var fitsIntoJavaArray = m_rowsInInputTable <= Integer.MAX_VALUE;
        if (m_sortInMemory && fitsIntoJavaArray) {
            result = sortInMemory(exec, dataHandler);
        } else {
            if (m_sortInMemory) {
                LOGGER.info("Not sorting table in memory, because it has more than " + Integer.MAX_VALUE + " rows.");
            }
            result = sortOnDisk(exec, dataHandler);
        }
        exec.setProgress(1.0);
        return result;
    }

    private DataTable sortInMemory(final ExecutionMonitor exec, final TableIOHandler dataHandler)
            throws CanceledExecutionException {

        final var optSorted = memSort(exec);
        if (optSorted.isEmpty()) {
            // input table has fewer than two rows, so it is trivially sorted
            return m_inputTable;
        }

        exec.setMessage("Creating sorted table");

        final var dc = dataHandler.createDataContainer(m_inputTable.getDataTableSpec(), false);
        final var writeExec = exec.createSubProgress(0.5);
        var progress = 0;
        for (DataRow r : optSorted.get()) {
            exec.checkCanceled();
            if (m_rowsInInputTable > 0) {
                writeExec.setProgress(progress / (double)m_rowsInInputTable, r.getKey()::getString);
            } else {
                final var rowNo = progress;
                writeExec.setMessage(() -> r.getKey() + " (row " + rowNo + ")");
            }
            dc.addRowToTable(r);
            progress++;
        }
        dc.close();
        return dc.getTable();
    }

    private final Optional<List<DataRow>> memSort(final ExecutionMonitor exec) throws CanceledExecutionException {
        var progress = 0;
        exec.setMessage("Reading data");
        final var rowList = new ArrayList<DataRow>();
        ExecutionMonitor readExec = exec.createSubProgress(0.5);
        for (final DataRow r : m_inputTable) {
            readExec.checkCanceled();
            if (m_rowsInInputTable > 0) {
                readExec.setProgress(progress / (double)m_rowsInInputTable, r.getKey()::getString);
            } else {
                final var rowNo = progress;
                readExec.setMessage(() -> r.getKey() + " (row " + rowNo + ")");
            }
            rowList.add(r);
            progress++;
        }
        // if there is 0 or 1 row only, return immediately (can't rely on "rowCount" as it might not be set)
        if (rowList.size() <= 1) {
            return Optional.empty();
        }

        exec.setMessage("Sorting");
        Collections.sort(rowList, m_rowComparator);
        return Optional.of(rowList);
    }

    /**
     * Sorts the given data table using a disk-based k-way merge sort.
     *
     * @param dataTable the data table that sgetRowCounthould be sorted
     * @param exec an execution context for reporting progress and creating BufferedDataContainers
     * @param dataHandler
     * @throws CanceledExecutionException if the user has canceled execution
     */
    private DataTable sortOnDisk(final ExecutionMonitor exec, final TableIOHandler tableIOHandler)
            throws CanceledExecutionException {
        if (m_rowsInInputTable <= 0) {
            // potentially unknown input size
            exec.setProgress(-1);
        }

        final var ticker = new AtomicLong();
        final var total = Long.toString(m_rowsInInputTable);
        if (m_rowsInInputTable <= 0) {
            exec.setMessage("Reading data");
        } else {
            // current row number is padded to the length of the total number of rows to minimize jumping
            final var template = "Reading data (row % " + total.length() + "d/" + total + ")";
            exec.setMessage(() -> template.formatted(ticker.get()));
        }

        final var initialPhaseExec = exec.createSubProgress(0.5);
        try (final var mergePhase = createInitialChunks(initialPhaseExec, tableIOHandler, m_inputTable, ticker)) {
            long numRows = mergePhase.getNumRows();
            // no or one row only in input table, can exit immediately (can't rely on global rowCount, might not be set)
            if (numRows <= 1) {
                return m_inputTable;
            }

            exec.setMessage("Merging temporary tables");
            long numLevels = Math.max(mergePhase.computeNumLevels(false), 1);
            final var mergePhaseExec = exec.createSubProgress(0.5 * numLevels / (numLevels + 1));
            try (final var mergeIterator = mergePhase.mergeIntoIterator(mergePhaseExec)) {

                // current row number is padded to the length of the total number of rows to minimize jumping
                final var template = "Writing output table (row % " + total.length() + "d/" + total + ")";
                ticker.set(0L);
                exec.setMessage(() -> template.formatted(ticker.get()));

                // The final output container, leave it to the system to do the caching (bug 1809)
                final var resultContainer = tableIOHandler.createDataContainer(m_dataTableSpec, false);
                final var outputPhaseExec = exec.createSubProgress(0.5 / (numLevels + 1));
                while (mergeIterator.hasNext()) {
                    exec.checkCanceled();
                    resultContainer.addRowToTable(mergeIterator.next());
                    outputPhaseExec.setProgress(1.0 * ticker.incrementAndGet() / numRows);
                }
                outputPhaseExec.setProgress(1.0);

                exec.setMessage("Closing output table");
                resultContainer.close();
                exec.setProgress(1.0);
                return resultContainer.getTable();
            }
        }
    }

    CloseableRowIterator sortedIteratorInternal(final ExecutionContext exec, final TableIOHandler dataHandler)
            throws CanceledExecutionException {
        if (m_rowsInInputTable <= 0) {
            // potentially unknown input size
            exec.setProgress(-1);
        }

        final var fitsIntoJavaArray = m_rowsInInputTable <= Integer.MAX_VALUE;
        try {
            if (m_sortInMemory && fitsIntoJavaArray) {
                final var optSorted = memSort(exec);
                return CloseableRowIterator.from(optSorted.map(List::iterator).orElse(m_inputTable.iterator()));
            } else {
                if (m_sortInMemory) {
                    LOGGER.info("Not sorting table in memory, because it has more than "
                            + Integer.MAX_VALUE + " rows.");
                }

                final var ticker = new AtomicLong();
                final var total = Long.toString(m_rowsInInputTable);
                if (m_rowsInInputTable <= 0) {
                    exec.setMessage("Reading data");
                } else {
                    final var template = "Reading data (row % " + total.length() + "d/" + total + ")";
                    exec.setMessage(() -> template.formatted(ticker.get()));
                }

                final var initialPhaseExec = exec.createSubProgress(0.5);
                try (final var mergePhase = createInitialChunks(initialPhaseExec, dataHandler, m_inputTable, ticker)) {
                    // no or one row only in input table, can exit immediately (can't rely on global rowCount)
                    if (mergePhase.getNumRows() <= 1) { // NOSONAR
                        return CloseableRowIterator.from(m_inputTable.iterator());
                    }

                    // The final output container, merge chunks until there are at most `m_maxOpenContainers` left
                    exec.setMessage("Merging temporary tables");
                    final var mergePhaseExec = exec.createSubProgress(0.5);
                    return mergePhase.mergeIntoIterator(mergePhaseExec);
                }
            }
        } finally {
            exec.setProgress(1.0);
        }
    }

    private MergePhase createInitialChunks(final ExecutionMonitor initialPhaseExec, final TableIOHandler tableIOHandler,
            final DataTable dataTable, final AtomicLong rowsRead) throws CanceledExecutionException {
        final var buffer = new ArrayList<DataRow>();
        long chunkStartRow = 0;
        var rowsInCurrentChunk = 0;

        MemoryActionIndicator memObservable = m_memService.newIndicator();

        final Deque<Iterable<DataRow>> chunksContainer;
        try (final var chunksWriter = newChunksWriter(tableIOHandler);
                final var inputIter = CloseableRowIterator.from(dataTable.iterator())) {
            while (inputIter.hasNext()) {
                final var rowNo = rowsRead.incrementAndGet();
                rowsInCurrentChunk++;
                initialPhaseExec.checkCanceled();
                if (m_rowsInInputTable > 0) {
                    initialPhaseExec.setProgress(1.0 * rowNo / m_rowsInInputTable, "Filling in-memory buffer");
                } else {
                    initialPhaseExec.setMessage(() -> "Reading table, %d rows read".formatted(rowNo));
                }
                buffer.add(inputIter.next());

                if ((memObservable.lowMemoryActionRequired() && (rowsInCurrentChunk >= m_maxOpenContainers))
                        || (rowNo % m_maxRowsPerChunk == 0)) {
                    LOGGER.debug("Writing chunk [" + chunkStartRow + ":" + rowNo + "] - mem usage: " + getMemUsage());
                    initialPhaseExec.setMessage("Sorting in-memory buffer");
                    // sort buffer
                    Collections.sort(buffer, m_rowComparator);
                    // write buffer to disk
                    writeChunk(initialPhaseExec, chunksWriter, buffer);
                    LOGGER.debug("Wrote chunk [" + chunkStartRow + ":" + rowNo + "] - mem usage: " + getMemUsage());
                    chunkStartRow = rowNo + 1;
                    rowsInCurrentChunk = 0;
                }
            }

            chunksContainer = new ArrayDeque<>();
            chunksWriter.finish(chunksContainer::addAll);
        }

        // Add buffer to the chunks
        if (!buffer.isEmpty()) {
            // sort buffer
            Collections.sort(buffer, m_rowComparator);
            chunksContainer.add(buffer);
        }

        initialPhaseExec.setProgress(1.0);

        return createMergePhase(tableIOHandler, chunksContainer, rowsRead.get());
    }

    private static void writeChunk(final ExecutionMonitor exec, final ChunksWriter chunksWriter,
            final ArrayList<DataRow> buffer) throws CanceledExecutionException {
        try (final var chunk = chunksWriter.openChunk(true)) {
            final int totalBufferSize = buffer.size();
            // current row number is padded to the length of the total number of rows to minimize jumping
            final var numStr = Integer.toString(totalBufferSize);
            final var template = "Writing temporary table (row % " + (numStr.length()) + "d/" + numStr + ")";
            for (var i = 0; i < totalBufferSize; i++) {
                exec.setMessage(template.formatted(i));
                // must not use Iterator#remove as it causes array copies
                final var next = buffer.set(i, null);
                chunk.addRow(next);
                exec.checkCanceled();
            }
            buffer.clear();
        }
    }

    /**
     * Creates a merge phase configured for this sorter. Mostly needed for {@link AbstractColumnTableSorter}.
     *
     * @param tableIOHandler table I/O handler
     * @param chunks chunks to be merged
     * @param numRows number of rows in the input
     * @return configured merge phase
     */
    MergePhase createMergePhase(final TableIOHandler tableIOHandler, final Deque<Iterable<DataRow>> chunks,
            final long numRows) {
        return new MergePhase(m_dataTableSpec, tableIOHandler, m_rowComparator, m_maxOpenContainers, chunks, numRows);
    }

    /**
     * The merge phase of the on-disk sorting operation consolidates a sorted sequence of pre-sorted temporary tables
     * (the <i>chunks</i>) into a single sequence of sorted rows. It is {@link AutoCloseable closeable} because it has
     * to free the disk memory occupied by the chunks even if the sorting operation is being aborted.
     */
    static final class MergePhase implements AutoCloseable {

        private final DataTableSpec m_tableSpec;
        private final TableIOHandler m_dataHandler;
        private final Comparator<DataRow> m_rowComparator;
        private final int m_maxOpenContainers;
        private final Deque<Iterable<DataRow>> m_chunks;
        private final long m_numRows;

        MergePhase(final DataTableSpec tableSpec, final TableIOHandler dataHandler,
                final Comparator<DataRow> rowComparator, final int maxOpenContainers,
                final Deque<Iterable<DataRow>> chunks, final long numRows) {
            m_tableSpec = tableSpec;
            m_dataHandler = dataHandler;
            m_rowComparator = rowComparator;
            m_maxOpenContainers = maxOpenContainers;
            m_chunks = chunks;
            m_numRows = numRows;
        }

        public long getNumRows() {
            return m_numRows;
        }

        /**
         * Computes the number of scans over the data the merge phase needs, i.e. the height of the merge tree.
         *
         * @param mergeCompletely flag indicating whether the last round is also written back to disk
         * @return number of scans over the input data are performed by the {@link MergePhase}
         */
        public int computeNumLevels(final boolean mergeCompletely) {
            // this loop is a bit strange because it mirrors the actual execution logic
            var numLevels = 0;
            var numChunks = m_chunks.size();
            while (numChunks != 0) {
                if (numChunks == 1 || (!mergeCompletely && numChunks <= m_maxOpenContainers)) {
                    // can be merged in one scan, last merge is accounted for elsewhere
                    return numLevels;
                }
                // exact version of `(int) Math.ceil(1.0 * numChunks / m_maxOpenContainers)`
                numChunks = (numChunks + m_maxOpenContainers - 1) / m_maxOpenContainers;
                numLevels++;
            }
            return 0;
        }

        /**
         * Merges the chunks of this merge phase int o a single sorted iterator.
         *
         * @param mergePhaseExec execution monitor
         * @param mergeCompletely if <code>true</code> the chunks are merged until only one chunk is left, otherwise the
         *            algorithm returns after at most {@link #m_maxOpenContainers} chunks are used
         * @return an iterator returning the sorted, merged result
         * @throws CanceledExecutionException if the algorithm has been canceled
         */
        private CloseableRowIterator mergeChunks(final ExecutionMonitor mergePhaseExec, final boolean mergeCompletely)
                throws CanceledExecutionException {

            final int numRounds = computeNumLevels(mergeCompletely);
            for (var round = 0; !m_chunks.isEmpty(); round++) {
                final var numChunks = m_chunks.size();
                if (numChunks == 1 || (!mergeCompletely && numChunks <= m_maxOpenContainers)) {
                    // can be merged in one scan, ownership of the chunks is transferred to the merge iterator
                    final var chunksToMerge = new ArrayList<>(m_chunks);
                    m_chunks.clear();
                    mergePhaseExec.setProgress(1.0);
                    return createMergeIterator(chunksToMerge);
                }

                // `numChunks` > 1, `numRounds` > 0.0
                final var subProgress = mergePhaseExec.createSubProgress(numRounds == 0 ? 1.0 : (1.0 / numRounds));
                performMergeRound(subProgress, round, numRounds);
                subProgress.setProgress(1.0);
            }
            mergePhaseExec.setProgress(1.0);
            return CloseableRowIterator.empty();
        }

        /**
         * Performs a single scan over all data, merging groups of {@link #m_maxOpenContainers} chunks.
         *
         * @param exec execution monitor
         * @param round number of the current merge round
         * @param numRounds total number of merge rounds
         * @throws CanceledExecutionException if the algorithm has been canceled
         */
        private void performMergeRound(final ExecutionMonitor exec, final int round, final int numRounds)
                throws CanceledExecutionException {
            final var messageStart = "Merging level %d/%d".formatted(round + 1, numRounds);
            final var message = messageStart + " (row % " + Long.toString(m_numRows).length() + "d/%d)";
            try (final var chunksWriter = new ChunksWriter(m_tableSpec, m_dataHandler)) {
                final var chunksToMerge = new ArrayList<Iterable<DataRow>>(m_maxOpenContainers);
                long numRowsProcessed = 0;
                while (m_chunks.size() > 1) {
                    // remove the next `k` chunks from the last round
                    final var k = Math.min(m_maxOpenContainers, m_chunks.size());
                    for (var i = 0; i < k; i++) {
                        chunksToMerge.add(m_chunks.poll());
                    }

                    // merge the `k` chunks together and add the combined chunk to the chunks writer
                    try (final var mergeIterator = createMergeIterator(chunksToMerge);
                            final var chunk = chunksWriter.openChunk(true)) {
                        while (mergeIterator.hasNext()) { // NOSONAR
                            exec.checkCanceled();
                            chunk.addRow(mergeIterator.next());
                            numRowsProcessed++;
                            final var currentRowNo = numRowsProcessed;
                            final var progress = 1.0 * numRowsProcessed / m_numRows;
                            exec.setProgress(progress, () -> message.formatted(currentRowNo, m_numRows));
                        }
                    }
                    chunksToMerge.clear();
                }

                // it makes no sense to merge a single final chunk, just copy it over into the next round
                final var last = m_chunks.poll();
                chunksWriter.finish(m_chunks::addAll);
                if (last != null) {
                    m_chunks.add(last);
                }
                exec.setProgress(1.0, messageStart + " - Finishing...");
            }
        }

        /**
         * Creates a new iterator that merges the given chunks into one stably sorted sequence.
         *
         * @param chunksToMergechunks to be merged
         * @return sorted iterator
         */
        @SuppressWarnings("resource")
        private CloseableRowIterator createMergeIterator(final Collection<Iterable<DataRow>> chunksToMerge) {
            if (chunksToMerge.isEmpty()) {
                return CloseableRowIterator.empty();
            }

            final var deletingIters = new CloseableRowIterator[chunksToMerge.size()];
            final var chunkIter = chunksToMerge.iterator();
            for (var i = 0; i < deletingIters.length; i++) {
                final var chunk = chunkIter.next();
                deletingIters[i] = chunk instanceof DataTable dt ? new TableClearingIterator(m_dataHandler, dt)
                    : CloseableRowIterator.from(chunk.iterator());
            }
            return deletingIters.length == 1 ? deletingIters[0] : new KWayMergeIterator(m_rowComparator, deletingIters);
        }

        /**
         * Merges all chunks and returns an iterator over the sorted rows. The results of the last merge are not written
         * back to disk, instead the last merge is deferred until the iterator is used.
         *
         * @param exec execution monitor for the merge phase
         * @return iterator over merged rows
         * @throws CanceledExecutionException if the execution was canceled
         */
        public CloseableRowIterator mergeIntoIterator(final ExecutionMonitor exec) throws CanceledExecutionException {
            return mergeChunks(exec, false);
        }

        /**
         * Merges all chunks and returns an iterator over the sorted rows. The results of the last merge have been
         * written back to disk, so only one temporary table is open in the background.
         *
         * @param exec execution monitor for the merge phase
         * @return iterator over merged rows
         * @throws CanceledExecutionException if the execution was canceled
         */
        public CloseableRowIterator mergeIntoMaterializedIterator(final ExecutionMonitor exec)
                throws CanceledExecutionException {
            return mergeChunks(exec, true);
        }

        @Override
        public void close() {
            while (!m_chunks.isEmpty()) {
                if (m_chunks.poll() instanceof DataTable dt) {
                    m_dataHandler.clearTable(dt);
                }
            }
        }
    }

    private static String getMemUsage() {
        final var runtime = Runtime.getRuntime();
        final var free = runtime.freeMemory();
        final var total = runtime.totalMemory();
        final var avail = runtime.maxMemory();
        final var freeD = free / (double)(1024 * 1024);
        final var totalD = total / (double)(1024 * 1024);
        final var availD = avail / (double)(1024 * 1024);
        final var freeS = NumberFormat.getInstance().format(freeD);
        final var totalS = NumberFormat.getInstance().format(totalD);
        final var availS = NumberFormat.getInstance().format(availD);
        return "avail: " + availS + "MB, total: " + totalS + "MB, free: " + freeS + "MB";
    }

    /**
     * Closeable row iterator that disposes the underlying iterable when it is closed.
     */
    private static final class TableClearingIterator extends CloseableRowIterator {

        private final TableIOHandler m_tableIOHandler;

        private DataTable m_dataTable;
        private CloseableRowIterator m_iterator;

        TableClearingIterator(final TableIOHandler dataHandler, final DataTable dataTable) {
            m_tableIOHandler = dataHandler;
            m_dataTable = dataTable;
        }

        @Override
        public boolean hasNext() {
            if (m_dataTable == null) {
                // closed
                return false;
            }
            if (m_iterator == null) {
                // lazy initialization
                m_iterator = CloseableRowIterator.from(m_dataTable.iterator());
            }
            return m_iterator.hasNext();
        }

        @Override
        public DataRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return m_iterator.next();
        }

        @Override
        public void close() {
            if (m_dataTable != null) {
                if (m_iterator != null) {
                    m_iterator.close();
                    m_iterator = null;
                }
                m_tableIOHandler.clearTable(m_dataTable);
                m_dataTable = null;
            }
        }
    }
}

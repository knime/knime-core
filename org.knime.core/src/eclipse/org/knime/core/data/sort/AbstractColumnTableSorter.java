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
 *   02.05.2014 (Marcel Hanser): created
 */
package org.knime.core.data.sort;

import static org.knime.core.node.util.CheckUtils.checkArgument;
import static org.knime.core.node.util.CheckUtils.checkNotNull;
import static org.knime.core.node.util.CheckUtils.checkSettingNotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.sort.ChunksWriter.ChunkHandle;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.data.util.memory.MemoryAlertSystem.MemoryActionIndicator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.ThreadPool;

/**
 * Sorts a data table, in a fine configurable way. It uses the disk based k-way merge sort like e.g. the
 * {@link BufferedDataTableSorter}, but with the additional functionality to sort only parts of the data table.
 *
 * @author Marcel Hanser
 * @since 2.10
 */
abstract class AbstractColumnTableSorter {

    private final ThreadPool m_executor =
            KNIMEConstants.GLOBAL_THREAD_POOL.createSubPool(Runtime.getRuntime().availableProcessors());

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractColumnTableSorter.class);

    /**
     * Default memory threshold. If this relative amount of memory is filled, the chunk is sorted and flushed to disk.
     */
    public static final double DEF_MEM_THRESHOLD = 0.8;

    /**
     * The maximum number of open containers.
     */
    public static final int DEF_MAX_OPENCONTAINER = 500;

    private MemoryActionIndicator m_memActionIndicator = MemoryAlertSystem.getInstance().newIndicator();

    private final Map<SortingDescription, List<DataRow>> m_buffer;

    private final SortingDescription[] m_sortDescriptions;

    private int m_maxOpenContainers = DEF_MAX_OPENCONTAINER;

    private long m_rowCount;

    private final DataTableSpec m_dataTableSpec;

    /**
     * Sorts the columns of the given table separately and ascending. <b>Attention</b> this means that the data rows are
     * split.
     *
     * @param spec the spec
     * @param rowsCount the amount of rows of the data table, if known -1 otherwise
     * @param columnToSort the columns to be sorted
     * @throws InvalidSettingsException if arguments are inconsistent.
     * @throws NullPointerException if any argument is null.
     */
    AbstractColumnTableSorter(final DataTableSpec spec, final long rowsCount, final String... columnToSort)
            throws InvalidSettingsException {
        this(spec, rowsCount, toSortDescriptions(spec, columnToSort));
    }

    /**
     * Sorts the columns of the given table as described in the provided {@link SortingDescription}s.
     *
     * @param spec the spec
     * @param rowsCount The number of rows in the table, if known -1 otherwise
     * @param descriptions the defined descriptions
     * @throws InvalidSettingsException If arguments are inconsistent.
     */
    AbstractColumnTableSorter(final DataTableSpec spec, final long rowsCount, final SortingDescription... descriptions)
            throws InvalidSettingsException {
        checkArgument(!ArrayUtils.contains(descriptions, null), "Null values are not permitted.");
        m_dataTableSpec = checkNotNull(spec);
        m_sortDescriptions = descriptions;
        m_rowCount = rowsCount;
        m_buffer = new LinkedHashMap<>();
        for (SortingDescription desc : descriptions) {
            m_buffer.put(desc, new ArrayList<>());
            desc.init(spec);
        }
    }

    /**
     * @param rowCount the row count
     * @param spec the spec
     * @param comparator the comparator to use
     * @return a concrete TableSorter
     */
    abstract AbstractTableSorter createTableSorter(long rowCount, DataTableSpec spec, Comparator<DataRow> comparator);

    /**
     * @param dataTable the table to sort
     * @param exec the execution context
     * @param resultListener the result listener
     * @throws CanceledExecutionException if the user cancels the execution
     */
    void sort(final DataTable dataTable, final ExecutionMonitor exec, final TableIOHandler dataHandler,
            final SortingConsumer resultListener) throws CanceledExecutionException {

        if (m_sortDescriptions.length <= 0) {
            for (DataRow r : dataTable) {
                resultListener.consume(new DefaultRow(r.getKey(), new DataCell[0])); // NOSONAR
            }
        } else {
            for (List<DataRow> i : m_buffer.values()) {
                i.clear();
            }
            sortOnDisk(dataTable, exec, dataHandler, resultListener);
        }
    }

    /**
     * Changes the number of maximum open containers (=files) during the sorting. Containers are used in the k-way merge
     * sort, the higher the number the fewer iterations in the final merge need to be done.
     *
     * <p>
     * The default is 500.
     *
     * @param value the maxOpenContainers to number of maximal open containers.
     * @throws IllegalArgumentException If argument is smaller or equal to 2.
     */
    public void setMaxOpenContainers(final int value) {
        checkArgument(value > 2 && value >= m_sortDescriptions.length, "At least '%d' containers are necessary.",
            Math.max(3, m_sortDescriptions.length));
        m_maxOpenContainers = value;
    }

    /**
     * Package visible due test issues.
     *
     * @param memIndicator the memService to set
     */
    void setMemActionIndicator(final MemoryActionIndicator memIndicator) {
        m_memActionIndicator = memIndicator;
    }

    /**
     * Sorts the given data table using a disk-based k-way merge sort.
     *
     * @param dataTable input table
     * @param exec execution monitor
     * @param dataHandler table I/O handler
     * @param resultListener result consumer
     * @throws CanceledExecutionException if the operation was cancelled
     */
    private void sortOnDisk(final DataTable dataTable, final ExecutionMonitor exec, final TableIOHandler dataHandler,
            final SortingConsumer resultListener) throws CanceledExecutionException {

        List<CloseableRowIterator> partitionRowIterators = null;
        try {
            // merge all selected columns into closeable iterators
            partitionRowIterators = sortSingleColumns(exec, dataHandler, dataTable);
            if (partitionRowIterators.isEmpty()) {
                return;
            }

            // assemble and publish the results to the listener
            final var currentRow = new ArrayList<DataRow>();
            @SuppressWarnings("resource")
            final var firstPartitionIterator = partitionRowIterators.get(0);
            for (var rowNo = 0L; firstPartitionIterator.hasNext(); rowNo++) {
                for (var i = 0; i < partitionRowIterators.size(); i++) {
                    @SuppressWarnings("resource")
                    final var colIterator = partitionRowIterators.get(i);
                    currentRow.add(colIterator.next());
                }
                resultListener.consume(new BlobSupportDataRow(new RowKey("AutoGenerated" + rowNo), currentRow));
                currentRow.clear();
            }
        } finally {
            if (partitionRowIterators != null) {
                partitionRowIterators.forEach(CloseableRowIterator::close);
            }
        }
    }

    /**
     * Merges the selected columns into single-column tables and returns closeable iterators over the sorted tables.
     *
     * @param exec execution monitor
     * @param dataHandler table I/O handler
     * @param dataTable input table
     * @return list of iterators, which have to be closed by the caller in all cases
     * @throws CanceledExecutionException if the operation was canceled
     */
    private List<CloseableRowIterator> sortSingleColumns(final ExecutionMonitor exec, final TableIOHandler dataHandler,
        final DataTable dataTable) throws CanceledExecutionException {
        // Each sorting description is done as a single external merge sort of their parts of the data
        final var columnPartitions = new AbstractTableSorter[m_sortDescriptions.length];
        for (var i = 0; i < m_sortDescriptions.length; i++) {
            final var colTableSpec = m_sortDescriptions[i].createDataTableSpec(m_dataTableSpec);
            columnPartitions[i] = createTableSorter(m_rowCount, colTableSpec, m_sortDescriptions[i]);
        }

        final List<Deque<Iterable<DataRow>>> chunks = new ArrayList<>();
        try {
            // phase one: create as big chunks as possible from the given input table for each sort description
            exec.setMessage("Reading table");
            final var chunksAndRows = createInitialChunks(exec, dataHandler, dataTable, columnPartitions, chunks);
            final var chunkCount = chunksAndRows[0];
            final var currentTotalRows = chunksAndRows[1];

            // phase 2: merge the temporary tables
            exec.setMessage("Merging temporary tables.");
            final var mergingProgress = exec.createSubProgress(0.3);
            return mergePartitions(columnPartitions, chunks, mergingProgress, dataHandler,
                chunkCount, currentTotalRows);

        } finally {
            // should be empty in normal operation, clean up temporary data in case of cancellation
            for (final var chunkDeque : chunks) {
                if (chunkDeque != null) {
                    for (final var chunk : chunkDeque) {
                        if (chunk instanceof DataTable dt) { // NOSONAR
                            dataHandler.clearTable(dt);
                        }
                    }
                    chunkDeque.clear();
                }
            }
        }
    }

    /**
     * Creates initial chunks for all selected columns from the input table.
     *
     * @param exec execution monitor
     * @param dataHandler table I/O handler
     * @param dataTable input table
     * @param columnPartitions column sorters
     * @param chunks output parameter for the chunks
     * @return two-element arrays containing the number of chunks per column and the number of rows overall
     * @throws CanceledExecutionException if the operation was canceled
     */
    @SuppressWarnings("resource")
    private long[] createInitialChunks(final ExecutionMonitor exec, final TableIOHandler dataHandler,
            final DataTable dataTable, final AbstractTableSorter[] columnPartitions,
            final List<Deque<Iterable<DataRow>>> chunks) throws CanceledExecutionException {
        final var readProgress = exec.createSubProgress(0.7);
        List<ChunksWriter> chunksWriters = List.of();
        try (final var iterator = CloseableRowIterator.from(dataTable.iterator())) {
            chunksWriters = Arrays.stream(columnPartitions) //
                    .map(sorter -> sorter.newChunksWriter(dataHandler)) //
                    .collect(Collectors.toCollection(ArrayList<ChunksWriter>::new));

            long chunkCount = 0;
            long currentTotalRows = 0;
            while (iterator.hasNext()) {
                LOGGER.debugWithFormat("Reading temporary tables -- (chunk %d)", chunkCount);
                assert m_buffer.values().stream().allMatch(l -> l.isEmpty());
                long bufferedRows = fillBuffer(iterator, exec);
                LOGGER.debugWithFormat("Writing temporary tables -- (chunk %d with %d rows)", chunkCount, bufferedRows);
                currentTotalRows += bufferedRows;
                readProgress.setProgress(currentTotalRows / (double)m_rowCount,
                    String.format("Writing temporary tables (chunk %d with %d rows)", chunkCount, bufferedRows));
                chunkCount++;
                LOGGER.debugWithFormat("Sorting temporary tables -- (chunk %d with %d rows)", chunkCount, bufferedRows);
                sortBufferInParallel();

                LOGGER.debugWithFormat("Writing temporary tables (chunk %d with %d rows)", chunkCount, bufferedRows);
                for (var i = 0; i < m_sortDescriptions.length; i++) {
                    final var sortingDescription = m_sortDescriptions[i];
                    LOGGER.debugWithFormat("Writing temporary table (chunk %d, column %d)", chunkCount, i);
                    ListIterator<DataRow> rowIterator = m_buffer.get(sortingDescription).listIterator();
                    try (ChunkHandle chunk = chunksWriters.get(i).openChunk(true)) { // NOSONAR
                        while (rowIterator.hasNext()) { // NOSONAR
                            exec.checkCanceled();

                            chunk.addRow(rowIterator.next());
                            // release the row as early as possible
                            rowIterator.set(null);
                        }
                    }
                }
                for (List<DataRow> i : m_buffer.values()) {
                    i.clear();
                }
            }

            for (final var writer : chunksWriters) {
                try (writer) {
                    // drain and close all writers
                    final var chunkQueue = new ArrayDeque<Iterable<DataRow>>();
                    chunks.add(chunkQueue);
                    writer.finish(chunkQueue::addAll);
                }
            }
            chunksWriters.clear();

            readProgress.setProgress(1.0);
            return new long[] { chunkCount, currentTotalRows };

        } finally {
            // should be empty in normal operation, clean up temporary data in case of cancellation
            chunksWriters.forEach(ChunksWriter::close);
        }
    }

    /**
     * Merges all partitions.
     *
     * @param columnPartitions column sorters
     * @param chunks collections of chunks grouped by column
     * @param exec execution monitor
     * @param dataHandler table I/O handler
     * @param chunkCount number of chunks per column table
     * @param numRows number of rows per column table
     * @return list of closeable iterators over the column tables, must be closed by the caller in all circumstances
     * @throws CanceledExecutionException
     */
    @SuppressWarnings("resource")
    private List<CloseableRowIterator> mergePartitions(final AbstractTableSorter[] columnPartitions,
            final List<Deque<Iterable<DataRow>>> chunks, final ExecutionMonitor exec, final TableIOHandler dataHandler,
            final long chunkCount, final long numRows) throws CanceledExecutionException {
        LOGGER.debug("Merging tables");
        final var partitionRowIterators = new ArrayList<CloseableRowIterator>();
        try {
            final long numberOfNecessaryContainers = chunkCount * m_sortDescriptions.length;
            var partIdx = 0;
            if (numberOfNecessaryContainers > m_maxOpenContainers && chunkCount > 1) {
                // AP-12179: if chunkCount == 1, numberOfNecessaryContainers can still exceed maxOpenContainers if we
                // have many sortDescriptions but in this case we can't reduce the number of containers by merging

                // we have to merge some of the partitions completely before returning the final containers.
                long tmp = numberOfNecessaryContainers;
                final long numExcessContainers = numberOfNecessaryContainers - m_maxOpenContainers;
                final double numExcessPartitions = numExcessContainers / chunkCount;
                // merging a partition reduces the number of containers for this partition from chunkCount to 1
                // thus after merging numExcessPartitions, we still have numExcessPartitions files more than allowed
                // consequently we have to reduce more partitions to fall below maxOpenContainers
                // AP-12179: In case of chunkCount == 1 this formula led to a division by zero but we now ensure
                // in the if condition above that chunkCount is greater than 1.
                final double numPartitionsToMergeSeparately = numExcessPartitions
                        + Math.ceil(numExcessPartitions / (chunkCount - 1));
                var index = 0;
                while (tmp > m_maxOpenContainers && partIdx < columnPartitions.length) {
                    final var subProgress = exec.createSubProgress(index / numPartitionsToMergeSeparately);
                    final Deque<Iterable<DataRow>> mergeQueue = chunks.set(partIdx, null);
                    final var columnSorter = columnPartitions[partIdx];
                    try (final var mergePhase = // NOSONAR
                            columnSorter.createMergePhase(dataHandler, mergeQueue, numRows)) {
                        partitionRowIterators.add(mergePhase.mergeIntoMaterializedIterator(subProgress));
                    }
                    index++;
                    // if we merge a run completely we save chunkCount of open file handles
                    // but need obviously one for opening the result file.
                    tmp = tmp - chunkCount + 1;
                    partIdx++;
                }
            }
            exec.setProgress(1, "Merging Done.");
            // we can now open enough containers to merge all runs at one time.
            // Hence the remaining containers don't need to be merged separately
            while (partIdx < columnPartitions.length) {
                final var subProgress = exec.createSubProgress(0);
                final Deque<Iterable<DataRow>> mergeQueue = chunks.set(partIdx, null);
                try (final var mergePhase =
                        columnPartitions[partIdx].createMergePhase(dataHandler, mergeQueue, numRows)) {
                    partitionRowIterators.add(mergePhase.mergeIntoIterator(subProgress));
                }
                partIdx++;
            }

            final var result = new ArrayList<>(partitionRowIterators);
            partitionRowIterators.clear();
            return result;
        } finally {
            // should be empty in normal operation, clean up temporary data in case of cancellation
            partitionRowIterators.forEach(CloseableRowIterator::close);
        }
    }

    private static SortingDescription[] toSortDescriptions(final DataTableSpec dataTableSpec, final String[] toSort)
            throws InvalidSettingsException {
        checkArgument(!ArrayUtils.contains(toSort, null), "Null values are not permitted.");

        final var descriptions = new SortingDescription[toSort.length];
        for (var i = 0; i < toSort.length; i++) {
            final var so = toSort[i];
            DataColumnSpec columnSpec = checkSettingNotNull( //
                dataTableSpec.getColumnSpec(so), "Column: '%s' does not exist in input table.", so);
            final DataValueComparator comparator = columnSpec.getType().getComparator();
            descriptions[i] = new SortingDescription(so) {
                @Override
                public int compare(final DataRow o1, final DataRow o2) {
                    return comparator.compare(o1.getCell(0), o2.getCell(0));
                }
            };
        }
        return descriptions;
    }

    private long fillBuffer(final RowIterator iterator, final ExecutionMonitor readExec)
            throws CanceledExecutionException {
        long count = 0;
        while (iterator.hasNext()) {
            count += 1;
            readExec.checkCanceled();
            DataRow r = iterator.next();
            for (Entry<SortingDescription, List<DataRow>> descr : m_buffer.entrySet()) {
                descr.getValue().add(descr.getKey().createSubRow(r));
            }
            // read at least two rows, otherwise we won't make any progress
            if ((count >= 2) &&  m_memActionIndicator.lowMemoryActionRequired()) {
                break;
            }
        }
        return count;
    }

    private void sortBufferInParallel() {
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (final Entry<SortingDescription, List<DataRow>> descr : m_buffer.entrySet()) {
                futures.add(m_executor.enqueue(() -> Collections.sort(descr.getValue(), descr.getKey())));
            }
            // wait until the inserting is finished
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (ExecutionException | InterruptedException e) { // NOSONAR
            futures.stream().forEach(f -> f.cancel(true));
            throw new RuntimeException("Execution has been interrupted!", e); // NOSONAR
        }
    }
}

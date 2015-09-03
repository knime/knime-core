/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * History
 *   02.05.2014 (Marcel Hanser): created
 */
package org.knime.core.data.sort;

import static org.knime.core.node.util.CheckUtils.checkArgument;
import static org.knime.core.node.util.CheckUtils.checkNotNull;
import static org.knime.core.node.util.CheckUtils.checkSettingNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobSupportDataRow;
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

    private final ThreadPool m_executor = KNIMEConstants.GLOBAL_THREAD_POOL.createSubPool(Runtime.getRuntime()
        .availableProcessors());

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

    private int m_rowCount;

    private final DataTableSpec m_dataTableSpec;

    /**
     * The constructor is identical to {@link #AbstractColumnTableSorter(DataTableSpec, int, String...)} with
     * {@link DataTableSpec#getColumnNames()} as the last input.
     *
     * @param spec the spec
     * @param rowsCount the amount of rows of the data table, if known -1 otherwise
     * @throws InvalidSettingsException if arguments are inconsistent.
     * @throws NullPointerException if any argument is null.
     */
    AbstractColumnTableSorter(final DataTableSpec spec, final int rowsCount) throws InvalidSettingsException {
        this(spec, rowsCount, spec.getColumnNames());
    }

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
    AbstractColumnTableSorter(final DataTableSpec spec, final int rowsCount, final String... columnToSort)
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
    AbstractColumnTableSorter(final DataTableSpec spec, final int rowsCount, final SortingDescription... descriptions)
        throws InvalidSettingsException {
        checkNotNullAndNotEmpty(descriptions);
        m_dataTableSpec = checkNotNull(spec);
        m_sortDescriptions = descriptions;
        m_rowCount = rowsCount;
        m_buffer = new LinkedHashMap<>();
        for (SortingDescription desc : descriptions) {
            m_buffer.put(desc, new ArrayList<DataRow>(20000));
        }
        validateAndInit(spec, descriptions);
    }

    /**
     * @param rowCount the row count
     * @param spec the spec
     * @param rowComparator the comparator to use
     * @return a concrete TableSorter
     */
    abstract AbstractTableSorter createTableSorter(int rowCount, DataTableSpec spec, Comparator<DataRow> rowComparator);

    /**
     * @param dataTable the table to sort
     * @param exec the execution context
     * @param resultListener the result listener
     * @throws CanceledExecutionException if the user cancels the execution
     */
    void sort(final DataTable dataTable, final ExecutionMonitor exec, final SortingConsumer resultListener)
        throws CanceledExecutionException {

        clearBuffer();
        sortOnDisk(dataTable, exec, resultListener);
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
     * @param dataTable the data table that sgetRowCounthould be sorted
     * @param exec an execution context for reporting progress and creating BufferedDataContainers
     * @throws CanceledExecutionException if the user has canceled execution
     */
    private void
        sortOnDisk(final DataTable dataTable, final ExecutionMonitor exec, final SortingConsumer resultListener)
            throws CanceledExecutionException {

        final List<AbstractTableSorter> columnPartitions =
            new ArrayList<AbstractTableSorter>(m_sortDescriptions.length);

        // Each sorting description is done as a single external merge sort of their parts of the data
        for (int i = 0; i < m_sortDescriptions.length; i++) {
            AbstractTableSorter tableSorter =
                createTableSorter(m_rowCount, m_sortDescriptions[i].createDataTableSpec(m_dataTableSpec),
                    m_sortDescriptions[i]);
            columnPartitions.add(tableSorter);
        }

        exec.setMessage("Reading table.");
        RowIterator iterator = dataTable.iterator();

        ExecutionMonitor readProgress = exec.createSubProgress(0.7);

        // phase one: create as big chunks as possible from the given input table
        // for each sort description
        int chunkCount = 0;
        int currentTotalRows = 0;
        while (iterator.hasNext()) {
            LOGGER.debugWithFormat("Reading temporary tables -- ", chunkCount);
            fillBuffer(iterator, exec);
            int bufferedRows = m_buffer.entrySet().iterator().next().getValue().size();
            LOGGER.debugWithFormat("Writing temporary tables -- %d/%d ", chunkCount, bufferedRows);
            currentTotalRows += bufferedRows;
            readProgress.setProgress(currentTotalRows / (double)m_rowCount, "Writing temporary tables -- "
                + chunkCount++ + "/" + bufferedRows);
            LOGGER.debugWithFormat("Sorting temporary tables -- %d/%d ", chunkCount, bufferedRows);
            sortBufferInParallel();

            for (AbstractTableSorter tableSorter : columnPartitions) {
                tableSorter.openChunk();
            }

            LOGGER.debugWithFormat("Writing temporary tables -- %d/%d ", chunkCount, bufferedRows);
            for (int i = 0; i < m_sortDescriptions.length; i++) {
                SortingDescription sortingDescription = m_sortDescriptions[i];
                LOGGER.debugWithFormat("Writing temporary table -- %d/%d ", chunkCount, i);
                AbstractTableSorter tableSorter = columnPartitions.get(i);
                ListIterator<DataRow> rowIterator = m_buffer.get(sortingDescription).listIterator();
                while (rowIterator.hasNext()) {
                    tableSorter.addRowToChunk(rowIterator.next());
                    // release the row as early as possible
                    rowIterator.set(null);
                }
                exec.checkCanceled();
            }

            for (AbstractTableSorter tableSorter : columnPartitions) {
                tableSorter.closeChunk();
            }

            clearBuffer();
        }

        clearBuffer();

        // if the table is empty or has only one row, we can stop here
        if (currentTotalRows <= 1) {
            return;
        }

        readProgress.setProgress(1.0);

        // phase 2: merge the temporary tables
        exec.setMessage("Merging temporary tables.");
        ExecutionMonitor mergingProgress = exec.createSubProgress(0.3);
        List<Iterator<DataRow>> partitionRowIterators = mergePartitions(columnPartitions, mergingProgress, chunkCount);

        // publish the results to the listener
        List<DataRow> currentRow = new ArrayList<>();
        int rowNo = 0;
        while (partitionRowIterators.get(0).hasNext()) {
            for (int i = 0; i < partitionRowIterators.size(); i++) {
                currentRow.add(partitionRowIterators.get(i).next());
            }
            resultListener.consume(aggregateRows(new RowKey("AutoGenerated" + rowNo++), currentRow));
            currentRow.clear();
        }
    }

    private List<Iterator<DataRow>> mergePartitions(final List<AbstractTableSorter> columnPartitions,
        final ExecutionMonitor exec, final int chunkCount) throws CanceledExecutionException {
        LOGGER.debug("Merging tables");
        List<Iterator<DataRow>> partitionRowIterators = new ArrayList<Iterator<DataRow>>();

        int numberOfNeccessaryContainers = chunkCount * m_sortDescriptions.length;
        if (numberOfNeccessaryContainers <= m_maxOpenContainers) {
            exec.setProgress(1, "Merging Done.");
            // we can open enough containers to merge all runs at one time.
            // So there is no need to merge them separately
            for (AbstractTableSorter clMs : columnPartitions) {
                partitionRowIterators.add(clMs.mergeChunks(exec.createSubProgress(0), false));
            }
        } else {
            int tmp = numberOfNeccessaryContainers;
            double noOfColumnMergeSortToSortCompletely =
                Math.ceil((numberOfNeccessaryContainers - m_maxOpenContainers) / (chunkCount - 1));

            //we have to merge some of the partitions completely before returning the final containers.
            Iterator<AbstractTableSorter> i = columnPartitions.iterator();
            int index = 0;
            while (tmp > m_maxOpenContainers && i.hasNext()) {
                partitionRowIterators.add(i.next().mergeChunks(
                    exec.createSubProgress(index++ / noOfColumnMergeSortToSortCompletely), true));
                // if we merge a run completely we save chunkCount of open files handles
                // but need obviously one for opening the result file.
                tmp = tmp - chunkCount + 1;
            }
            exec.setProgress(1, "Merging Done.");
            while (i.hasNext()) {
                partitionRowIterators.add(i.next().mergeChunks(exec.createSubProgress(0), false));
            }
        }
        return partitionRowIterators;
    }

    /**
     * @param next
     * @param rowToAggregate
     * @return
     */
    private static DataRow aggregateRows(final RowKey next, final List<DataRow> rowToAggregate) {
        return new BlobSupportDataRow(next, rowToAggregate);
    }

    private static void validateAndInit(final DataTableSpec dataTableSpec, final SortingDescription[] toSort)
        throws InvalidSettingsException {
        for (SortingDescription desc : toSort) {
            desc.init(dataTableSpec);
        }
    }

    private static SortingDescription[] toSortDescriptions(final DataTableSpec dataTableSpec, final String[] toSort)
        throws InvalidSettingsException {
        checkNotNullAndNotEmpty(toSort);

        SortingDescription[] toReturn = new SortingDescription[toSort.length];
        int index = 0;
        for (String so : toSort) {
            DataColumnSpec columnSpec = checkSettingNotNull(//
                dataTableSpec.getColumnSpec(so), "Column: '%s' does not exist in input table.", so);
            final DataValueComparator comparator = columnSpec.getType().getComparator();
            toReturn[index++] = new SortingDescription(so) {

                @Override
                public int compare(final DataRow o1, final DataRow o2) {
                    return comparator.compare(o1.getCell(0), o2.getCell(0));
                }
            };
        }
        return toReturn;
    }

    private static <T> void checkNotNullAndNotEmpty(final T[] toSort) {
        checkArgument(ArrayUtils.isNotEmpty(toSort), "Array cannot be empty.");
        checkArgument(!ArrayUtils.contains(toSort, null), "Null values are not permitted.");
    }

    /**
     * @param m_buffer
     * @param iterator
     * @param checkMemory
     * @param exec
     * @throws CanceledExecutionException
     */
    private void fillBuffer(final RowIterator iterator, final ExecutionMonitor readExec)
        throws CanceledExecutionException {

        // read at least two rows, otherwise we won't make any progress
        long count = 0;
        while (iterator.hasNext()) {
            readExec.checkCanceled();
            DataRow r = iterator.next();
            for (Entry<SortingDescription, List<DataRow>> descr : m_buffer.entrySet()) {
                descr.getValue().add(descr.getKey().createSubRow(r));
            }
            if ((++count >= 2) &&  m_memActionIndicator.lowMemoryActionRequired()) {
                break;
            }
        }
    }

    private void clearBuffer() {
        for (List<DataRow> i : m_buffer.values()) {
            i.clear();
        }
    }

    private void sortBufferInParallel() {
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (final Entry<SortingDescription, List<DataRow>> descr : m_buffer.entrySet()) {
                futures.add(m_executor.enqueue(new Runnable() {

                    @Override
                    public void run() {
                        Collections.sort(descr.getValue(), descr.getKey());
                    }
                }));
            }
            //wait until the inserting is finished
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Execution has been interrupted!", e);
        }
    }
}
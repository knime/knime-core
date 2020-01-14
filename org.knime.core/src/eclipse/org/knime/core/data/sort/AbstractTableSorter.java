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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.data.util.memory.MemoryAlertSystem.MemoryActionIndicator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * Class to sort a table. See <a href="package.html">package description</a> for details.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
abstract class AbstractTableSorter {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractTableSorter.class);

    /** Representing column spec to sort according to the row key. */
    public static final DataColumnSpec ROWKEY_SORT_SPEC = new DataColumnSpecCreator("-ROWKEY -",
        DataType.getType(StringCell.class)).createSpec();

    /**
     * The maximum number of open containers. See {@link #setMaxOpenContainers(int)} for details.
     */
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

    private DataContainer m_currentContainer;

    private Queue<Iterable<DataRow>> m_chunksContainer = new LinkedList<Iterable<DataRow>>();

    private double m_progress;

    private double m_incProgress;

    private long m_itemCount;

    /**
     * Private constructor. Assigns input table, checks argument.
     *
     * @param inputTable Table to sort.
     * @param rowsCount The number of rows in the table
     * @throws NullPointerException If arg is null.
     */
    private AbstractTableSorter(final DataTable inputTable, final long rowsCount) {
        if (inputTable == null) {
            throw new NullPointerException("Argument must not be null.");
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
     * @deprecated use {@link #AbstractTableSorter(long, DataTableSpec, Comparator)} instead which supports more than
     *             {@link Integer#MAX_VALUE} rows
     */
    @Deprecated
    public AbstractTableSorter(final DataTable inputTable, final int rowsCount,
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
     * @deprecated use {@link #AbstractTableSorter(DataTable, long, Collection, boolean[])} instead which supports more
     *             than {@link Integer#MAX_VALUE} rows
     */
    @Deprecated
    public AbstractTableSorter(final DataTable inputTable, final int rowsCount, final Collection<String> inclList,
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
     * @deprecated use {@link #AbstractTableSorter(DataTable, long, Collection, boolean[], boolean)} instead which
     *             supports more than {@link Integer#MAX_VALUE} rows
     */
    @Deprecated
    public AbstractTableSorter(final DataTable inputTable, final int rowsCount, final Collection<String> inclList,
        final boolean[] sortAscending, final boolean sortMissingsToEnd) {
        this(inputTable, rowsCount);
        setSortColumns(inclList, sortAscending, sortMissingsToEnd);
    }


    /**
     * Inits sorter on argument table with given row comparator.
     *
     * @param inputTable Table to sort.
     * @param rowsCount The number of rows in the table
     * @param rowComparator Passed to {@link #setRowComparator(Comparator)}.
     */
    public AbstractTableSorter(final DataTable inputTable, final long rowsCount,
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
    public AbstractTableSorter(final DataTable inputTable, final long rowsCount, final Collection<String> inclList,
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
    public AbstractTableSorter(final DataTable inputTable, final long rowsCount, final Collection<String> inclList,
        final boolean[] sortAscending, final boolean sortMissingsToEnd) {
        this(inputTable, rowsCount);
        setSortColumns(inclList, sortAscending, sortMissingsToEnd);
    }

    /** @param rowComparator the rowComparator to set */
    public void setRowComparator(final Comparator<DataRow> rowComparator) {
        if (rowComparator == null) {
            throw new NullPointerException("Argument must not be null.");
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
    public void setSortColumns(final Collection<String> inclList, final boolean[] sortAscending) {
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
    public void setSortColumns(final Collection<String> inclList, final boolean[] sortAscending,
        final boolean sortMissingsToEnd) {
        if (sortAscending == null || inclList == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        if (inclList.size() != sortAscending.length) {
            throw new IllegalArgumentException("Length of arguments vary: " + inclList.size() + " vs. "
                + sortAscending.length);
        }
        if (inclList.contains(null)) {
            throw new IllegalArgumentException("Argument array must not contain null: " + inclList);
        }
        if (!(inclList instanceof Set)) {
            Set<String> noDuplicatesSet = new HashSet<String>(inclList);
            if (noDuplicatesSet.size() != inclList.size()) {
                throw new IllegalArgumentException("Argument collection must " + "not contain duplicates: " + inclList);
            }
        }
        int[] indices = new int[sortAscending.length];
        final DataTableSpec spec = m_inputTable.getDataTableSpec();
        int curIndex = 0;
        for (String name : inclList) {
            int index = spec.findColumnIndex(name);
            if (index == -1 && !name.equals(ROWKEY_SORT_SPEC.getName())) {
                throw new IllegalArgumentException("Could not find column name:" + name.toString());
            }
            indices[curIndex++] = index;
        }
        setRowComparator(new RowComparator(indices, sortAscending, sortMissingsToEnd, spec));
    }

    /**
     * Get the number of maximum open containers. See {@link #setMaxOpenContainers(int)} for details.
     *
     * @return the maxOpenContainers
     */
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
    public boolean getSortInMemory() {
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

    /**
     * Sorts the table passed in the constructor according to the settings and returns the sorted output table.
     *
     * @param exec To report progress
     * @return The sorted output.
     * @throws CanceledExecutionException If canceled.
     */
    DataTable sortInternal(final ExecutionMonitor exec) throws CanceledExecutionException {
        DataTable result;
        if (m_sortInMemory && (m_rowsInInputTable <= Integer.MAX_VALUE)) {
            result = sortInMemory(exec);
        } else {
            if (m_rowsInInputTable > Integer.MAX_VALUE) {
                LOGGER.info("Not sorting table in memory, because it has more than " + Integer.MAX_VALUE + " rows.");
            }
            result = sortOnDisk(exec);
        }
        exec.setProgress(1.0);
        return result;
    }

    private DataTable sortInMemory(final ExecutionMonitor exec) throws CanceledExecutionException {
        final DataTable dataTable = m_inputTable;
        List<DataRow> rowList = new ArrayList<DataRow>();

        int progress = 0;
        final long rowCount = m_rowsInInputTable;
        exec.setMessage("Reading data");
        ExecutionMonitor readExec = exec.createSubProgress(0.5);
        for (final DataRow r : dataTable) {
            readExec.checkCanceled();
            if (rowCount > 0) {
                readExec.setProgress(progress / (double)rowCount, r.getKey().getString());
            } else {
                readExec.setMessage(r.getKey() + " (row " + progress + ")");
            }
            rowList.add(r);
            progress++;
        }
        // if there is 0 or 1 row only, return immediately (can't rely on
        // "rowCount" as it might not be set)
        if (rowList.size() <= 1) {
            return m_inputTable;
        }

        exec.setMessage("Sorting");
        Collections.sort(rowList, m_rowComparator);

        exec.setMessage("Creating sorted table");

        final DataContainer dc = createDataContainer(dataTable.getDataTableSpec(), false);
        ExecutionMonitor writeExec = exec.createSubProgress(0.5);
        progress = 0;
        for (DataRow r : rowList) {
            exec.checkCanceled();
            if (rowCount > 0) {
                writeExec.setProgress(progress / (double)rowCount, r.getKey().getString());
            } else {
                writeExec.setMessage(r.getKey() + " (row " + progress + ")");
            }
            dc.addRowToTable(r);
            progress++;
        }
        dc.close();
        return dc.getTable();
    }

    /**
     * Creates data container, either a buffered data container or a plain one.
     *
     * @param spec The spec of the container/table.
     * @param forceOnDisk false to use default, true to flush data immediately to disk. It's true when used in the
     *            #sortOnDisk(ExecutionMonitor) method and the container is only used temporarily.
     * @return A new fresh container.
     */
    abstract DataContainer createDataContainer(final DataTableSpec spec, final boolean forceOnDisk);

    /**
     * Clears the temporary table that was used during the execution but is no longer needed.
     *
     * @param table The table to be cleared.
     */
    abstract void clearTable(final DataTable table);

    /**
     * Sorts the given data table using a disk-based k-way merge sort.
     *
     * @param dataTable the data table that sgetRowCounthould be sorted
     * @param exec an execution context for reporting progress and creating BufferedDataContainers
     * @throws CanceledExecutionException if the user has canceled execution
     */
    private DataTable sortOnDisk(final ExecutionMonitor exec) throws CanceledExecutionException {
        final DataTable dataTable = m_inputTable;

        m_progress = 0.0;
        m_incProgress = m_rowsInInputTable <= 0 ? -1.0 : 1.0 / (2.0 * m_rowsInInputTable);
        long counter = createInitialChunks(exec, dataTable);
        // no or one row only in input table, can exit immediately
        // (can't rely on global rowCount - might not be set)
        if (counter <= 1) {
            return m_inputTable;
        }

        exec.setMessage("Merging temporary tables");
        // The final output container
        // merge chunks until there are only so much left, as m_maxopencontainers
        Iterator<DataRow> result = mergeChunks(exec, false);

        // add results to the final container
        // The final output container, leave it to the
        // system to do the caching (bug 1809)
        DataContainer resultContainer = createDataContainer(dataTable.getDataTableSpec(), false);
        while (result.hasNext()) {
            resultContainer.addRowToTable(result.next());
        }
        resultContainer.close();
        return resultContainer.getTable();
    }

    /**
     * @param exec execution context
     * @param mergeCompletely if <code>true</code> the chunks are merged until only one chunk is left, otherwise the
     *            algorithm returns after at most {@link #m_maxOpenContainers} chunks are used
     * @return an iterator returning the sorted, merged result
     * @throws CanceledExecutionException if the algorithm has been canceled
     */
    Iterator<DataRow> mergeChunks(final ExecutionMonitor exec, final boolean mergeCompletely)
        throws CanceledExecutionException {
        while (!m_chunksContainer.isEmpty()) {
            exec.setMessage("Merging temporary tables, " + m_chunksContainer.size() + " remaining");
            if (m_chunksContainer.size() < m_maxOpenContainers) {
                if (m_rowsInInputTable > 0) {
                    m_incProgress = (1.0 - m_progress) / m_rowsInInputTable;
                }
            } else {
                if (m_rowsInInputTable > 0) {
                    double estimatedReads =
                        Math.ceil(m_chunksContainer.size() / (double)m_maxOpenContainers) * m_rowsInInputTable;
                    m_incProgress = (1.0 - m_progress) / estimatedReads;
                }
            }

            Queue<MergeEntry> containersToMerge = new ArrayDeque<>();

            for (int i = 0; !m_chunksContainer.isEmpty() && i < m_maxOpenContainers; i++) {
                containersToMerge.add(new MergeEntry(m_chunksContainer.poll(), i, m_rowComparator));
            }

            MergingIterator mergingIterator = new MergingIterator(containersToMerge);

            if (m_chunksContainer.isEmpty() && (!mergeCompletely || containersToMerge.size() == 1)) {
                return mergingIterator;
            } else {
                if (m_rowsInInputTable > 0) {
                    // increment progress
                    m_progress += m_incProgress;
                    exec.setProgress(m_progress);
                }
                openChunk();

                try {
                    while (mergingIterator.hasNext()) {
                        addRowToChunk(mergingIterator.next());
                        exec.checkCanceled();
                    }
                } finally {
                    closeChunk();
                }
            }
        }
        return Collections.<DataRow>emptyList().iterator();
    }

    private long createInitialChunks(final ExecutionMonitor exec, final DataTable dataTable)
        throws CanceledExecutionException {
        long outerCounter;
        long counter = 0;
        ArrayList<DataRow> buffer = new ArrayList<DataRow>();
        long chunkStartRow = 0;
        int rowsInCurrentChunk = 0;

        MemoryActionIndicator memObservable = m_memService.newIndicator();

        exec.setMessage("Reading table");
        for (Iterator<DataRow> iter = dataTable.iterator(); iter.hasNext();) {
            counter++;
            rowsInCurrentChunk++;
            exec.checkCanceled();
            String message = "Reading table, " + counter + " rows read";
            if (m_rowsInInputTable > 0) {
                m_progress += m_incProgress;
                exec.setProgress(m_progress, message);
            } else {
                exec.setMessage(message);
            }
            DataRow row = iter.next();
            buffer.add(row);
            if ((memObservable.lowMemoryActionRequired() && (rowsInCurrentChunk >= m_maxOpenContainers))
                || (counter % m_maxRowsPerChunk == 0)) {
                LOGGER.debug("Writing chunk [" + chunkStartRow + ":" + counter + "] - mem usage: " + getMemUsage());
                if (m_rowsInInputTable > 0) {
                    long estimatedIncrements = m_rowsInInputTable - counter + buffer.size();
                    m_incProgress = (0.5 - m_progress) / estimatedIncrements;
                }
                exec.setMessage("Sorting temporary buffer");
                // sort buffer
                Collections.sort(buffer, m_rowComparator);
                // write buffer to disk
                openChunk();
                final int totalBufferSize = buffer.size();
                for (int i = 0; i < totalBufferSize; i++) {
                    exec.setMessage("Writing temporary table -- " + i + "/" + totalBufferSize);
                    // must not use Iterator#remove as it causes
                    // array copies
                    DataRow next = buffer.set(i, null);
                    addRowToChunk(next);
                    exec.checkCanceled();
                    if (m_rowsInInputTable > 0) {
                        m_progress += m_incProgress;
                        exec.setProgress(m_progress);
                    }
                }
                buffer.clear();
                closeChunk();

                LOGGER.debug("Wrote chunk [" + chunkStartRow + ":" + counter + "] - mem usage: " + getMemUsage());
                chunkStartRow = counter + 1;
                rowsInCurrentChunk = 0;
            }
        }
        // Add buffer to the chunks
        if (!buffer.isEmpty()) {
            // sort buffer
            Collections.sort(buffer, m_rowComparator);
            m_chunksContainer.add(buffer);
        }
        outerCounter = counter;
        return outerCounter;
    }

    /**
     * Opens a chunk data container to accept rows using {@link #addRowToChunk(DataRow)}, {@link #closeChunk()} closes
     * the current container and adds it to the chunk list.
     */
    void openChunk() {
        m_currentContainer = createDataContainer(m_dataTableSpec, true);
        m_currentContainer.setMaxPossibleValues(0);
    }

    /**
     * Adds a row to the current chunk.
     *
     * @param dataRow the row
     */
    void addRowToChunk(final DataRow dataRow) {
        m_itemCount++;
        m_currentContainer.addRowToTable(dataRow);
    }

    /**
     * Closes the chunk.
     */
    void closeChunk() {
        if (m_currentContainer != null) {
            m_currentContainer.close();
            if (m_itemCount > 0) {
                m_chunksContainer.offer(m_currentContainer.getTable());
            } else {
                clearTable(m_currentContainer.getTable());
            }
            m_itemCount = 0;
        }
    }

    private String getMemUsage() {
        Runtime runtime = Runtime.getRuntime();
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        long avail = runtime.maxMemory();
        double freeD = free / (double)(1024 * 1024);
        double totalD = total / (double)(1024 * 1024);
        double availD = avail / (double)(1024 * 1024);
        String freeS = NumberFormat.getInstance().format(freeD);
        String totalS = NumberFormat.getInstance().format(totalD);
        String availS = NumberFormat.getInstance().format(availD);
        return "avail: " + availS + "MB, total: " + totalS + "MB, free: " + freeS + "MB";
    }

    private final class MergeEntry implements Comparable<MergeEntry>, Iterator<DataRow> {
        private DataRow m_row;

        private Iterable<DataRow> m_iterable;

        private Iterator<DataRow> m_iterator;

        private int m_index;

        private Comparator<DataRow> m_comparator;

        /**
         * @param iterator
         * @param index
         * @param comparator
         */
        MergeEntry(final Iterable<DataRow> iterable, final int index, final Comparator<DataRow> comparator) {
            m_iterable = iterable;
            m_index = index;
            m_comparator = comparator;
        }

        private void open() {
            if (m_iterator == null) {
                m_iterator = m_iterable.iterator();
                if (m_iterator.hasNext()) {
                    m_row = m_iterator.next();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            // open the file lazily
            if (m_row == null) {
                if (m_iterable instanceof DataTable) {
                    clearTable((DataTable)m_iterable);
                }
                return false;
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            // open the file lazily
            if (m_row == null) {
                throw new NoSuchElementException();
            }
            DataRow toReturn = m_row;
            m_row = m_iterator.hasNext() ? m_iterator.next() : null;
            return toReturn;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final MergeEntry that) {
            int value = m_comparator.compare(this.m_row, that.m_row);
            if (value == 0) {
                return this.m_index - that.m_index;
            } else {
                return value;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + m_index;
            result = prime * result + ((m_row == null) ? 0 : m_row.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MergeEntry other = (MergeEntry)obj;
            return (this.compareTo(other) == 0);
        }
    }

    /**
     * Lazily opens the given MergeEntry's (The runs of this merging step) and returns the rows.
     *
     * @author Marcel Hanser
     */
    private static final class MergingIterator implements Iterator<DataRow> {
        private Queue<MergeEntry> m_containerToMerge;

        private boolean m_opened = false;

        /**
         * @param containerToMerge
         */
        private MergingIterator(final Queue<MergeEntry> containerToMerge) {
            super();
            m_containerToMerge = containerToMerge;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            if (!m_opened) {
                Queue<MergeEntry> sortedEntries = new PriorityQueue<>();
                for (MergeEntry entry : m_containerToMerge) {
                    entry.open();
                    sortedEntries.add(entry);
                }
                m_containerToMerge = sortedEntries;
                m_opened = true;
            }
            return !m_containerToMerge.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            if (hasNext()) {
                MergeEntry first = m_containerToMerge.poll();
                DataRow currentCell = first.next();
                if (first.hasNext()) {
                    m_containerToMerge.offer(first);
                }
                return currentCell;
            } else {
                throw new NoSuchElementException();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}

/*
 *
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 * History
 *   31.08.2011 (wiswedel): created
 */
package org.knime.core.data.sort;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * Class to sort a table. See <a href="package.html">package description</a>
 * for details.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
abstract class TableSorter {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(TableSorter.class);

    /** Representing column spec to sort according to the row key. */
    public static final DataColumnSpec ROWKEY_SORT_SPEC =
        new DataColumnSpecCreator("-ROWKEY -",
                DataType.getType(StringCell.class)).createSpec();

    /** Default memory threshold. If this relative amount of memory is filled,
     * the chunk is sorted and flushed to disk. */
    public static final double DEF_MEM_THRESHOLD = 0.8;
    /** The maximum number of open containers. See
     * {@link #setMaxOpenContainers(int)} for details. */
    public static final int DEF_MAX_OPENCONTAINER = 40;

    private MemoryService m_memService = new MemoryService(DEF_MEM_THRESHOLD);

    private final DataTable m_inputTable;
    private final int m_rowsInInputTable;

    /**
     * The maximal number of open containers. This has an effect when many
     * containers must be merged.
     */
    private int m_maxOpenContainers = DEF_MAX_OPENCONTAINER;

    /** Maximum number of rows. Only used in unit test.
     * Defaults to {@link Integer#MAX_VALUE}. */
    private int m_maxRows = Integer.MAX_VALUE;

    private boolean m_sortInMemory = false;

    /** The RowComparator to compare two DataRows (inner class). */
    private Comparator<DataRow> m_rowComparator;

    /** Private constructor. Assigns input table, checks argument.
     * @param inputTable Table to sort.
     * @param rowsCount The number of rows in the table
     * @throws NullPointerException If arg is null. */
    private TableSorter(final DataTable inputTable, final int rowsCount) {
        if (inputTable == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        // for BDT better use the appropriate derived class (blob handling)
        if (getClass().equals(TableSorter.class)
                && inputTable instanceof BufferedDataTable) {
            LOGGER.coding("Do not use a "
                    + TableSorter.class.getSimpleName() + " to sort"
                    + " a " + BufferedDataTable.class.getSimpleName()
                    + " but use a "
                    + BufferedDataTableSorter.class.getSimpleName());
        }
        m_inputTable = inputTable;
        m_rowsInInputTable = rowsCount;
    }

    /** Inits sorter on argument table with given row comparator.
     * @param inputTable Table to sort.
     * @param rowsCount The number of rows in the table
     * @param rowComparator Passed to {@link #setRowComparator(Comparator)}. */
    public TableSorter(final DataTable inputTable, final int rowsCount,
            final Comparator<DataRow> rowComparator) {
        this(inputTable, rowsCount);
        setRowComparator(rowComparator);
    }

    /** Inits table sorter using the sorting according to
     * {@link #setSortColumns(Collection, boolean[])}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table
     * @param inclList Passed on to
     * {@link #setSortColumns(Collection, boolean[])}.
     * @param sortAscending Passed on to
     * {@link #setSortColumns(Collection, boolean[])}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     */
    public TableSorter(final DataTable inputTable,
            final int rowsCount, final Collection<String> inclList,
            final boolean[] sortAscending) {
        this(inputTable, rowsCount);
        setSortColumns(inclList, sortAscending);
    }

    /** Inits table sorter using the sorting according to
     * {@link #setSortColumns(Collection, boolean[], boolean)}.
     *
     * @param inputTable The table to sort
     * @param rowsCount The number of rows in the table
     * @param inclList Passed on to
     * {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @param sortAscending Passed on to
     * {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @param sortMissingsToEnd Passed on to
     * {@link #setSortColumns(Collection, boolean[], boolean)}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     * @since 2.6
     */
    public TableSorter(final DataTable inputTable,
            final int rowsCount, final Collection<String> inclList,
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

    /** Sets sorting columns and order.
     * @param inclList the list with the columns to sort; the first column name
     *            represents the first sort criteria, the second the second
     *            criteria and so on.
     *
     * @param sortAscending the sort order; each field corresponds to the column
     *            in the list of included columns. true: ascending false:
     *            descending
     */
    public void setSortColumns(final Collection<String> inclList,
            final boolean[] sortAscending) {
        setSortColumns(inclList, sortAscending, false);
    }

    /** Sets sorting columns and order.
    * @param inclList the list with the columns to sort; the first column name
    *            represents the first sort criteria, the second the second
    *            criteria and so on.
    *
    * @param sortAscending the sort order; each field corresponds to the column
    *            in the list of included columns. true: ascending false:
    *            descending
    * @param sortMissingsToEnd Whether to sort missing values always to the
    *            end independent to the sort oder (if false missing values
    *            are always smaller than non-missings).
    * @since 2.6
    */
    public void setSortColumns(final Collection<String> inclList,
            final boolean[] sortAscending, final boolean sortMissingsToEnd) {
        if (sortAscending == null || inclList == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        if (inclList.size() != sortAscending.length) {
            throw new IllegalArgumentException("Length of arguments vary: "
                    + inclList.size() + " vs. " + sortAscending.length);
        }
        if (inclList.contains(null)) {
            throw new IllegalArgumentException(
                    "Argument array must not contain null: " + inclList);
        }
        if (!(inclList instanceof Set)) {
            Set<String> noDuplicatesSet = new HashSet<String>(inclList);
            if (noDuplicatesSet.size() != inclList.size()) {
                throw new IllegalArgumentException("Argument collection must "
                        + "not contain duplicates: " + inclList);
            }
        }
        int[] indices = new int[sortAscending.length];
        final DataTableSpec spec = m_inputTable.getDataTableSpec();
        int curIndex = 0;
        for (String name : inclList) {
            int index = spec.findColumnIndex(name);
            if (index == -1 && !name.equals(ROWKEY_SORT_SPEC.getName())) {
                throw new IllegalArgumentException(
                        "Could not find column name:" + name.toString());
            }
            indices[curIndex++] = index;
        }
        setRowComparator(new RowComparator(
                indices, sortAscending, sortMissingsToEnd, spec));
    }

    /** Get the number of maximum open containers. See
     * {@link #setMaxOpenContainers(int)} for details.
     * @return the maxOpenContainers */
    public int getMaxOpenContainers() {
        return m_maxOpenContainers;
    }

    /** Changes the number of maximum open containers (=files) during the
     * sorting. Containers are used in the k-way merge sort, the higher the
     * number the fewer iterations in the final merge need to be done.
     *
     * <p>The default is {@value #DEF_MAX_OPENCONTAINER}.
     * @param value the maxOpenContainers to number of maximal open containers.
     * @throws IllegalArgumentException If argument is smaller or equal to 2.
     */
    public void setMaxOpenContainers(final int value) {
        if (value <= 2) {
            throw new IllegalArgumentException(
                    "Invalid open container count: " + value);
        }
        m_maxOpenContainers = value;
    }

    /** Set the maximum number of rows per chunk, defaults to
     * {@link Integer#MAX_VALUE}. This field is modified from the testing
     * framework.
     * @param maxRows the maxRows to set */
    void setMaxRows(final int maxRows) {
        m_maxRows = maxRows;
    }

    /** Set memory service. Used in unit test.
     * @param memService the memService to set */
    void setMemService(final MemoryService memService) {
        m_memService = memService;
    }

    /** @return the sortInMemory field, see {@link #setSortInMemory(boolean)}
     * for details. */
    public boolean getSortInMemory() {
        return m_sortInMemory;
    }

    /** Forces the sorting to happen in memory (if argument is true). This is
     * not advisable as tables can be large. Note, the sorting may also take
     * place in memory if the table is small (see class description for
     * details).
     *
     * <p>This option is merely to ensure backward compatibility and should
     * not be used anymore.
     *
     * <p>The default value for this option is <b>false</b>.
     *
     * @param sortInMemory <code>true</code> if sorting should be done in
     * memory, <code>false</code> if sorting should be done in a hybrid way
     * as described in the class description.
     */
    public void setSortInMemory(final boolean sortInMemory) {
        m_sortInMemory = sortInMemory;
    }

    /** Sorts the table passed in the constructor according to the settings
     * and returns the sorted output table.
     * @param exec To report progress
     * @return The sorted output.
     * @throws CanceledExecutionException If canceled. */
    DataTable sortInternal(final ExecutionMonitor exec)
        throws CanceledExecutionException {
        DataTable result;
        if (m_sortInMemory) {
            result = sortInMemory(exec);
        } else {
            result = sortOnDisk(exec);
        }
        exec.setProgress(1.0);
        return result;
    }

    private DataTable sortInMemory(final ExecutionMonitor exec)
    throws CanceledExecutionException {
        final DataTable dataTable = m_inputTable;
        List<DataRow> rowList = new ArrayList<DataRow>();

        int progress = 0;
        final int rowCount = m_rowsInInputTable;
        exec.setMessage("Reading data");
        ExecutionMonitor readExec = exec.createSubProgress(0.5);
        for (final DataRow r : dataTable) {
            readExec.checkCanceled();
            if (rowCount > 0) {
                readExec.setProgress(progress / (double)rowCount,
                        r.getKey().getString());
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

        final DataContainer dc = createDataContainer(
                dataTable.getDataTableSpec(), false);
        ExecutionMonitor writeExec = exec.createSubProgress(0.5);
        progress = 0;
        for (DataRow r : rowList) {
            exec.checkCanceled();
            if (rowCount > 0) {
                writeExec.setProgress(progress / (double)rowCount,
                        r.getKey().getString());
            } else {
                writeExec.setMessage(r.getKey() + " (row " + progress + ")");
            }
            dc.addRowToTable(r);
            progress++;
        }
        dc.close();
        return dc.getTable();
    }

    /** Creates data container, either a buffered data container or a plain
     * one.
     * @param spec The spec of the container/table.
     * @param forceOnDisk false to use default, true to flush data immediately
     * to disk. It's true when used in the
     * #sortOnDisk(ExecutionMonitor) method and the container is only
     * used temporarily.
     * @return A new fresh container. */
    abstract DataContainer createDataContainer(final DataTableSpec spec,
            final boolean forceOnDisk);

    /** Clears the temporary table that was used during the execution but is
     * no longer needed.
     * @param table The table to be cleared. */
    abstract void clearTable(final DataTable table);

    /**
     * Sorts the given data table using a disk-based k-way merge sort.
     *
     * @param dataTable the data table that sgetRowCounthould be sorted
     * @param exec an execution context for reporting progress and creating
     *            BufferedDataContainers
     * @throws CanceledExecutionException if the user has canceled execution
     */
    private DataTable sortOnDisk(final ExecutionMonitor exec)
    throws CanceledExecutionException {
        final DataTable dataTable = m_inputTable;

        // cont will hold the sorted chunks
        List<Iterable<DataRow>> chunksCont =
            new LinkedList<Iterable<DataRow>>();

        ArrayList<DataRow> buffer = new ArrayList<DataRow>();

        double progress = 0.0;
        double incProgress = m_rowsInInputTable <= 0
            ? -1.0 : 1.0 / (2.0 * m_rowsInInputTable);
        int counter = 0;
        int cf = 0;
        int chunkStartRow = 0;

        exec.setMessage("Reading table");
        for (Iterator<DataRow> iter = dataTable.iterator(); iter.hasNext();) {
            cf++;
            exec.checkCanceled();
            if (!m_memService.isMemoryLow()
                    && (cf % m_maxRows != 0 || cf == 0)) {
                counter++;
                exec.checkCanceled();
                String message = "Reading table, " + counter + " rows read";
                if (m_rowsInInputTable > 0) {
                    progress += incProgress;
                    exec.setProgress(progress, message);
                } else {
                    exec.setMessage(message);
                }
                DataRow row = iter.next();
                buffer.add(row);
            } else {
                LOGGER.debug("Writing chunk [" + chunkStartRow + ":"
                        + counter + "] - mem usage: " + getMemUsage());
                if (m_rowsInInputTable > 0) {
                    int estimatedIncrements =
                        m_rowsInInputTable - counter + buffer.size();
                    incProgress = (0.5 - progress) / estimatedIncrements;
                }
                exec.setMessage("Sorting temporary buffer");
                // sort buffer
                Collections.sort(buffer, m_rowComparator);
                // write buffer to disk
                DataContainer diskCont = createDataContainer(
                        dataTable.getDataTableSpec(), true);
                diskCont.setMaxPossibleValues(0);
                final int totalBufferSize = buffer.size();
                for (int i = 0; i < totalBufferSize; i++) {
                    exec.setMessage("Writing temporary table -- "
                            + i + "/" + totalBufferSize);
                    // must not use Iterator#remove as it causes
                    // array copies
                    DataRow next = buffer.set(i, null);
                    diskCont.addRowToTable(next);
                    exec.checkCanceled();
                    if (m_rowsInInputTable > 0) {
                        progress += incProgress;
                        exec.setProgress(progress);
                    }
                }
                buffer.clear();
                diskCont.close();
                chunksCont.add(diskCont.getTable());

                // Force full gc to be sure that there is not too much
                // garbage
                LOGGER.debug("Wrote chunk [" + chunkStartRow + ":"
                        + counter + "] - mem usage: " + getMemUsage());
                Runtime.getRuntime().gc();

                LOGGER.debug("Forced gc() when reading rows, new mem usage: "
                        + getMemUsage());
                chunkStartRow = counter + 1;
            }
        }
        // no or one row only in input table, can exit immediately
        // (can't rely on global rowCount - might not be set)
        if (counter <= 1) {
            return m_inputTable;
        }
        // Add buffer to the chunks
        if (!buffer.isEmpty()) {
            // sort buffer
            Collections.sort(buffer, m_rowComparator);
            chunksCont.add(buffer);
        }

        exec.setMessage("Merging temporary tables");
        // The final output container
        DataContainer cont = null;
        // merge chunks until there is one left
        while (chunksCont.size() > 1 || cont == null) {
            exec.setMessage("Merging temporary tables, " + chunksCont.size()
                    + " remaining");
            if (chunksCont.size() < m_maxOpenContainers) {
                // The final output container, leave it to the
                // system to do the caching (bug 1809)
                cont = createDataContainer(
                        dataTable.getDataTableSpec(), false);
                if (m_rowsInInputTable > 0) {
                    incProgress = (1.0 - progress) / m_rowsInInputTable;
                }
            } else {
                cont = createDataContainer(dataTable.getDataTableSpec(), true);
                if (m_rowsInInputTable > 0) {
                    double estimatedReads =
                        Math.ceil(chunksCont.size() / (double) m_maxOpenContainers)
                        * m_rowsInInputTable;
                    incProgress = (1.0 - progress) / estimatedReads;
                }
            }
            // isolate lists to merge
            List<Iterable<DataRow>> toMergeCont =
                new ArrayList<Iterable<DataRow>>();
            int c = 0;
            for (Iterator<Iterable<DataRow>> iter = chunksCont.iterator();
            iter.hasNext();) {
                c++;
                if (c > m_maxOpenContainers) {
                    break;
                }
                toMergeCont.add(iter.next());
                // remove container from chunksCont
                iter.remove();
            }
            // merge container in toMergeCont into cont
            PriorityQueue<MergeEntry> currentRows =
                new PriorityQueue<MergeEntry>(toMergeCont.size());
            for (int i = 0; i < toMergeCont.size(); i++) {
                Iterator<DataRow> iter = toMergeCont.get(i).iterator();
                if (iter.hasNext()) {
                    currentRows.add(new MergeEntry(iter, i, m_rowComparator));
                }
            }
            while (currentRows.size() > 0) {
                MergeEntry first = currentRows.poll();
                DataRow least = first.poll();
                cont.addRowToTable(least);
                exec.checkCanceled();
                if (m_rowsInInputTable > 0) {
                    // increment progress
                    progress += incProgress;
                    exec.setProgress(progress);
                }
                // read next row in first
                if (null != first.peek()) {
                    currentRows.add(first);
                }
            }
            cont.close();
            // Add cont to the pending containers
            chunksCont.add(0, cont.getTable());
            // toMergeCont may contain DataTable. These DatatTables can be
            // cleared now.
            for (Iterable<DataRow> merged : toMergeCont) {
                if (merged instanceof DataTable) {
                    clearTable((DataTable)merged);
                }
            }
        }
        return cont.getTable();
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
        return "avail: " + availS + "MB, total: " + totalS
        + "MB, free: " + freeS + "MB";
    }

    private static final class MergeEntry implements Comparable<MergeEntry> {
        private DataRow m_row;
        private Iterator<DataRow> m_iterator;
        private int m_index;
        private Comparator<DataRow> m_comparator;

        /**
         * @param iterator
         * @param index
         * @param comparator
         */
        MergeEntry(final Iterator<DataRow> iterator, final int index,
                final Comparator<DataRow> comparator) {
            m_iterator = iterator;
            m_row = m_iterator.hasNext() ? m_iterator.next() : null;
            m_index = index;
            m_comparator = comparator;
        }

        /**
         * @return the row
         */
        public DataRow peek() {
            return m_row;
        }

        /**
         * @return the row
         */
        public DataRow poll() {
            DataRow row = m_row;
            m_row = m_iterator.hasNext() ? m_iterator.next() : null;
            return row;
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
     * The private class RowComparator is used to compare two DataRows. It
     * implements the Comparator-interface, so we can use the Arrays.sort method
     * to sort an array of DataRows. */
    private static final class RowComparator implements Comparator<DataRow> {

        /**
         * The included column indices.
         */
        private final int[] m_indices;

        /** The comparators for the different columns (value in array is null
         * if sorted according to row key). Fetched at constructor time to
         * reduce number of DataType accesses during compare() call*/
        private final DataValueComparator[] m_colComparators;
        /**
         * Array containing information about the sort order for each column.
         * true: ascending false: descending
         */
        private final boolean[] m_sortAscending;

        /** Missing vals always at end (if not then they just smaller than
         * any non-missing). */
        private final boolean m_sortMissingsToEnd;

        /**
         * @param indices Array of sort column indices.
         * @param sortAscending Sort order.
         * @param sortMissingsToEnd Missing at bottom.
         * @param spec The spec to the table. */
        RowComparator(final int[] indices, final boolean[] sortAscending,
                final boolean sortMissingsToEnd, final DataTableSpec spec) {
            m_indices = indices;
            m_colComparators = new DataValueComparator[indices.length];
            for (int i = 0; i < m_indices.length; i++) {
                // only if the cell is in the includeList
                // -1 is RowKey!
                if (m_indices[i] == -1) {
                    m_colComparators[i] = null;
                } else {
                    m_colComparators[i] = spec.getColumnSpec(
                            m_indices[i]).getType().getComparator();
                }
            }
            m_sortAscending = sortAscending;
            m_sortMissingsToEnd = sortMissingsToEnd;
        }

        /** {@inheritDoc} */
        @Override
        public int compare(final DataRow dr1, final DataRow dr2) {

            if (dr1 == dr2) {
                return 0;
            }
            if (dr1 == null) {
                return 1;
            }
            if (dr2 == null) {
                return -1;
            }

            assert (dr1.getNumCells() == dr2.getNumCells());

            for (int i = 0; i < m_indices.length; i++) {

                // only if the cell is in the includeList
                // -1 is RowKey!
                int cellComparison;
                if (m_indices[i] == -1) {
                    String k1 = dr1.getKey().getString();
                    String k2 = dr2.getKey().getString();
                    cellComparison = k1.compareTo(k2);
                } else {
                    final DataCell c1 = dr1.getCell(m_indices[i]);
                    final DataCell c2 = dr2.getCell(m_indices[i]);
                    final boolean c1Missing = c1.isMissing();
                    final boolean c2Missing = c2.isMissing();
                    if (m_sortMissingsToEnd && (c1Missing || c2Missing)) {
                        if (c1Missing && c2Missing) {
                            cellComparison = 0;
                        } else if (c1Missing) {
                            cellComparison = m_sortAscending[i] ? +1 : -1;
                        } else { // c2.isMissing()
                            cellComparison = m_sortAscending[i] ? -1 : +1;
                        }
                    } else {
                        final DataValueComparator comp = m_colComparators[i];
                        cellComparison = comp.compare(c1, c2);
                    }
                }
                if (cellComparison != 0) {
                    return (m_sortAscending[i] ? cellComparison
                            : -cellComparison);
                }
            }
            return 0; // all cells in the DataRow have the same value
        }
    }

}

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * Class to sort a table. The sorting is done in a disk-based k-way merge sort
 * using a MemoryService that observes the JVM's memory consumption.
 * The implementation reads in the input table sequentially into chunks,
 * whereby the chunk size is determined at runtime based on available memory.
 * Each chunk is then sorted in memory and flushed out into a temporary
 * container. The final step is to compose the output table by merging the
 * temporary containers.
 *
 * <p>Usage: Client implementations will initialize this object with the table
 * to be sorted, set properties using the varies set-methods (defaults are
 * generally fine) and finally call the {@link #sort(ExecutionContext)} method.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TableSorter {

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

    private final BufferedDataTable m_inputTable;
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
     * @throws NullPointerException If arg is null. */
    private TableSorter(final BufferedDataTable inputTable) {
        if (inputTable == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_inputTable = inputTable;
    }

    /** Inits sorter on argument table with given row comparator.
     * @param inputTable Table to sort.
     * @param rowComparator Passed to {@link #setRowComparator(Comparator)}. */
    public TableSorter(final BufferedDataTable inputTable,
            final Comparator<DataRow> rowComparator) {
        this(inputTable);
        setRowComparator(rowComparator);
    }

    /** Inits table sorter using the sorting according to
     * {@link #setSortColumns(Collection, boolean[])}.
     *
     * @param inputTable The table to sort
     * @param inclList Passed on to
     * {@link #setSortColumns(Collection, boolean[])}.
     * @param sortAscending Passed on to
     * {@link #setSortColumns(Collection, boolean[])}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     */
    public TableSorter(final BufferedDataTable inputTable,
            final Collection<String> inclList, final boolean[] sortAscending) {
        this(inputTable);
        setSortColumns(inclList, sortAscending);
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
        setRowComparator(new RowComparator(indices, sortAscending, spec));
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
     * @param ctx The execution context to create temporary buffers and to
     * report progress
     * @return The sorted output.
     * @throws CanceledExecutionException If canceled. */
    public BufferedDataTable sort(final ExecutionContext ctx)
        throws CanceledExecutionException {
        BufferedDataTable result;
        if (m_inputTable.getRowCount() < 2) {
            result = m_inputTable;
        } else if (m_sortInMemory) {
            result = sortInMemory(ctx);
        } else {
            result = sortOnDisk(ctx);
        }
        ctx.setProgress(1.0);
        return result;
    }

    private BufferedDataTable sortInMemory(final ExecutionContext exec)
    throws CanceledExecutionException {
        final BufferedDataTable dataTable = m_inputTable;
        final DataRow[] rows = new DataRow[dataTable.getRowCount()];

        final double max = 2 * dataTable.getRowCount();
        int progress = 0;
        exec.setMessage("Reading data");
        int i = 0;
        for (final DataRow r : dataTable) {
            exec.checkCanceled();
            exec.setProgress(progress / max);
            rows[i++] = r;
            progress++;
        }

        exec.setMessage("Sorting");
        Arrays.sort(rows, m_rowComparator);

        exec.setMessage("Creating sorted table");

        final BufferedDataContainer dc =
            exec.createDataContainer(dataTable.getDataTableSpec(), true);
        for (i = 0; i < rows.length; i++) {
            exec.checkCanceled();
            exec.setProgress(progress / max);
            dc.addRowToTable(rows[i]);
            progress++;
        }
        dc.close();
        return dc.getTable();
    }

    /**
     * Sorts the given data table using a disk-based k-way merge sort.
     *
     * @param dataTable the data table that should be sorted
     * @param exec an execution context for reporting progress and creating
     *            BufferedDataContainers
     * @throws CanceledExecutionException if the user has canceled execution
     */
    private BufferedDataTable sortOnDisk(final ExecutionContext exec)
    throws CanceledExecutionException {
        final BufferedDataTable dataTable = m_inputTable;
        assert dataTable.getRowCount() > 0;

        // cont will hold the sorted chunks
        List<Iterable<DataRow>> chunksCont =
            new LinkedList<Iterable<DataRow>>();

        ArrayList<DataRow> buffer = new ArrayList<DataRow>();

        double progress = 0;
        double incProgress = 0.5 / dataTable.getRowCount();
        int counter = 0;
        int cf = 0;
        int chunkStartRow = 0;

        exec.setMessage("Reading table");
        for (Iterator<DataRow> iter = dataTable.iterator(); iter.hasNext();) {
            cf++;
            exec.checkCanceled();
            if (!m_memService.isMemoryLow(exec)
                    && (cf % m_maxRows != 0 || cf == 0)) {
                counter++;
                progress += incProgress;
                if (counter % 100 == 0) {
                    exec.checkCanceled();
                    exec.setProgress(progress, "Reading table, " + counter
                            + " rows read");
                }
                DataRow row = iter.next();
                buffer.add(row);
            } else {
                LOGGER.debug("Writing chunk [" + chunkStartRow + ":"
                        + counter + "] - mem usage: " + getMemUsage());
                int estimatedIncrements = dataTable.getRowCount() - counter
                + buffer.size();
                incProgress = (0.5 - progress) / estimatedIncrements;
                exec.setMessage("Sorting temporary buffer");
                // sort buffer
                Collections.sort(buffer, m_rowComparator);
                // write buffer to disk
                BufferedDataContainer diskCont =
                    exec.createDataContainer(dataTable.getDataTableSpec(),
                            true, 0);
                diskCont.setMaxPossibleValues(0);
                final int totalBufferSize = buffer.size();
                for (int i = 0; i < totalBufferSize; i++) {
                    exec.setMessage("Writing temporary table -- "
                            + i + "/" + totalBufferSize);
                    // must not use Iterator#remove as it causes
                    // array copies
                    DataRow next = buffer.set(i, null);
                    diskCont.addRowToTable(next);
                    progress += incProgress;
                    exec.setProgress(progress);
                    exec.checkCanceled();
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
        // Add buffer to the chunks
        if (!buffer.isEmpty()) {
            // sort buffer
            Collections.sort(buffer, m_rowComparator);
            chunksCont.add(buffer);
        }

        exec.setMessage("Merging temporary tables");
        // The final output container
        BufferedDataContainer cont = null;
        // merge chunks until there is one left
        while (chunksCont.size() > 1 || cont == null) {
            exec.setMessage("Merging temporary tables, " + chunksCont.size()
                    + " remaining");
            if (chunksCont.size() < m_maxOpenContainers) {
                // The final output container, leave it to the
                // system to do the caching (bug 1809)
                cont = exec.createDataContainer(dataTable.getDataTableSpec(),
                        true);
                incProgress = (1.0 - progress) / dataTable.getRowCount();
            } else {
                cont = exec.createDataContainer(dataTable.getDataTableSpec(),
                        true, 0);
                double estimatedReads =
                    Math.ceil(chunksCont.size() / m_maxOpenContainers)
                    * dataTable.getRowCount();
                incProgress = (1.0 - progress) / estimatedReads;
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
                // increment progress
                progress += incProgress;
                exec.setProgress(progress);
                // read next row in first
                if (null != first.peek()) {
                    currentRows.add(first);
                }
            }
            cont.close();
            // Add cont to the pending containers
            chunksCont.add(0, cont.getTable());
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

        /**
         * Array containing information about the sort order for each column.
         * true: ascending false: descending
         */
        private final boolean[] m_sortAscending;

        /** The spec. Set shortly before the comparison starts, used to get
         * column compartor. */
        private final DataTableSpec m_spec;

        /**
         * @param indices Array of sort column indices.
         * @param sortAscending Sort order.
         * @param spec The spec to the table. */
        RowComparator(final int[] indices, final boolean[] sortAscending,
                final DataTableSpec spec) {
            m_indices = indices;
            m_sortAscending = sortAscending;
            m_spec = spec;
        }

        /**
         * This method compares two DataRows based on a comparison for each
         * DataCell and the sorting order (m_sortOrder) for each column.
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         * @param dr1 one data row
         * @param dr2 another datarow to be compared with dr1
         * @return -1 if dr1 < dr2, 0 if dr1 == dr2 and 1 if dr1 > dr2
         */
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
                int cellComparison = 0;
                if (m_indices[i] == -1) {
                    String k1 = dr1.getKey().getString();
                    String k2 = dr2.getKey().getString();
                    cellComparison = k1.compareTo(k2);
                } else {
                    final DataValueComparator comp =
                        m_spec.getColumnSpec(m_indices[i]).getType()
                        .getComparator();
                    // same column means that they have the same type
                    cellComparison =
                        comp.compare(dr1.getCell(m_indices[i]), dr2
                                .getCell(m_indices[i]));
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

/* @(#)$RCSfile$
 * $Revision$ $Date$ $Author$
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
 *   23.10.2006 (sieb): created
 */
package org.knime.base.data.sort;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.knime.base.node.preproc.sorter.SorterNodeDialogPanel2;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * A data table that sorts a given data table according to the passed sorting
 * parameters.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class SortedTable implements DataTable {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SortedTable.class);

    private BufferedDataTable m_sortedTable;

    private int m_maxRows;
    private MemoryService m_memService;

    /**
     * The maximal number of open containers. This has an effect when many
     * containers must be merged.
     */
    private int m_maxOpenContainers = 40;

    /**
     * Hold the table spec to be used by the comparator inner class.
     */
    private final DataTableSpec m_spec;

    /**
     * The included column indices.
     */
    private int[] m_indices;

    /**
     * The RowComparator to compare two DataRows (inner class).
     */
    private final Comparator<DataRow> m_rowComparator;

    /**
     * Array containing information about the sort order for each column. true:
     * ascending false: descending
     */
    private boolean[] m_sortAscending;

    /**
     * Creates a new sorted table. Sorting is done with the given comparator.
     *
     * @param dataTable any data table
     * @param rowComparator the comparator that should be used for sorting
     * @param sortInMemory <code>true</code> if sorting should be done in
     *            memory, <code>false</code> if sorting should be done on disk
     * @param exec an execution context for reporting progress and creating
     *            temporary table files
     * @throws CanceledExecutionException if the user canceled execution
     */
    public SortedTable(final BufferedDataTable dataTable,
            final Comparator<DataRow> rowComparator,
            final boolean sortInMemory, final ExecutionContext exec)
            throws CanceledExecutionException {
        this(dataTable, rowComparator, sortInMemory, 40, exec);
    }

    /**
     * Creates a new sorted table. Sorting is done with the given comparator.
     *
     * @param dataTable any data table
     * @param rowComparator the comparator that should be used for sorting
     * @param sortInMemory <code>true</code> if sorting should be done in
     *            memory, <code>false</code> if sorting should be done on disk
     * @param exec an execution context for reporting progress and creating
     *            temporary table files
     * @throws CanceledExecutionException if the user canceled execution
     */
    public SortedTable(final BufferedDataTable dataTable,
            final Comparator<DataRow> rowComparator,
            final boolean sortInMemory, final int maxOpenContainer,
            final ExecutionContext exec)
            throws CanceledExecutionException {
        m_rowComparator = rowComparator;
        m_spec = dataTable.getDataTableSpec();
        m_maxRows = Integer.MAX_VALUE;
        m_memService = new MemoryService(0.9);
        m_maxOpenContainers = maxOpenContainer;
        if (dataTable.getRowCount() < 2) {
            m_sortedTable = dataTable;
        } else if (sortInMemory) {
            sortInMemory(dataTable, exec);
        } else {
            sortOnDisk(dataTable, exec);
        }
    }

    /**
     * Creates a sorted table from the given table and the sorting parameters.
     * The table is sorted in the constructor.
     *
     * @param dataTable the buffered data table to sort
     * @param inclList the list with the columns to sort; the first column name
     *            represents the first sort criteria, the second the second
     *            criteria and so on.
     *
     * @param sortAscending the sort order; each field corresponds to the column
     *            in the list of included columns. true: ascending false:
     *            descending
     *
     * @param exec the execution context used to create the the buffered data
     *            table and indicate the progress
     * @throws CanceledExecutionException the user has canceled this operation
     *
     */
    public SortedTable(final BufferedDataTable dataTable,
            final List<String> inclList, final boolean[] sortAscending,
            final ExecutionContext exec) throws CanceledExecutionException {
        this(dataTable, inclList, sortAscending, false, exec);
    }

    /**
     * Creates a sorted table from the given table and the sorting parameters.
     * The table is sorted in the constructor.
     *
     * @param dataTable the buffered data table to sort
     * @param inclList the list with the columns to sort; the first column name
     *            represents the first sort criteria, the second the second
     *            criteria and so on.
     *
     * @param sortAscending the sort order; each field corresponds to the column
     *            in the list of included columns. true: ascending false:
     *            descending
     * @param sortInMemory <code>true</code> if the table should be sorted in
     *            memory, <code>false</code> if it should be sorted in disk.
     *            Sorting in memory is much faster but may fail if the data
     *            table is too big.
     *
     * @param exec the execution context used to create the the buffered data
     *            table and indicate the progress
     * @throws CanceledExecutionException the user has canceled this operation
     */
    public SortedTable(final BufferedDataTable dataTable,
            final List<String> inclList, final boolean[] sortAscending,
            final boolean sortInMemory, final ExecutionContext exec)
            throws CanceledExecutionException {
        this(dataTable, inclList, sortAscending, sortInMemory, 40, exec);
    }

    /**
     * Creates a sorted table from the given table and the sorting parameters.
     * The table is sorted in the constructor.
     *
     * @param dataTable the buffered data table to sort
     * @param inclList the list with the columns to sort; the first column name
     *            represents the first sort criteria, the second the second
     *            criteria and so on.
     *
     * @param sortAscending the sort order; each field corresponds to the column
     *            in the list of included columns. true: ascending false:
     *            descending
     * @param sortInMemory <code>true</code> if the table should be sorted in
     *            memory, <code>false</code> if it should be sorted in disk.
     *            Sorting in memory is much faster but may fail if the data
     *            table is too big.
     *
     * @param exec the execution context used to create the the buffered data
     *            table and indicate the progress
     * @throws CanceledExecutionException the user has canceled this operation
     */
    public SortedTable(final BufferedDataTable dataTable,
            final List<String> inclList, final boolean[] sortAscending,
            final boolean sortInMemory, final int maxOpenContainer,
            final ExecutionContext exec)
            throws CanceledExecutionException {
        m_rowComparator = new RowComparator();
        m_spec = dataTable.getDataTableSpec();
        m_maxRows = Integer.MAX_VALUE;
        double memThreshold = MemoryService.DISABLE_SORT_MIN_MEMORY ? 0.85 : 0.75;
        m_memService = new MemoryService(memThreshold);
        m_maxOpenContainers = maxOpenContainer;

        // get the column indices of the columns that will be sorted
        // also make sure that m_inclList and m_sortOrder both exist
        if (inclList == null) {
            throw new IllegalArgumentException(
                    "List of colums to include (incllist) is "
                            + "not set in the model");
        } else {
            m_indices = new int[inclList.size()];
        }

        m_sortAscending = sortAscending;
        if (m_sortAscending == null) {
            throw new IllegalArgumentException("Sortorder array is "
                    + "not set in the model");
        }

        int pos = -1;
        for (int i = 0; i < inclList.size(); i++) {
            final String dc = inclList.get(i);
            pos = m_spec.findColumnIndex(dc);
            if (pos == -1
                    && !(dc.compareTo(SorterNodeDialogPanel2.
                            ROWKEY.getName()) == 0)) {
                throw new IllegalArgumentException(
                        "Could not find column name:" + dc.toString());
            }
            m_indices[i] = pos;
        }

        if (dataTable.getRowCount() < 2) {
            m_sortedTable = dataTable;
        } else if (sortInMemory) {
            sortInMemory(dataTable, exec);
        } else {
            sortOnDisk(dataTable, exec);
        }
    }

    private void sortInMemory(final BufferedDataTable dataTable,
            final ExecutionContext exec) throws CanceledExecutionException {
        final DataRow[] rows = new DataRow[dataTable.getRowCount()];

        final double max = 2 * dataTable.getRowCount();
        int progress = 0;
        exec.setMessage("Reading data");
        long time = System.currentTimeMillis();
        int i = 0;
        for (final DataRow r : dataTable) {
            exec.checkCanceled();
            exec.setProgress(progress / max);
            rows[i++] = r;
            progress++;
        }
        LOGGER.debug("Read data time: " + (System.currentTimeMillis() - time));

        exec.setMessage("Sorting");
        time = System.currentTimeMillis();
        Arrays.sort(rows, m_rowComparator);
        LOGGER.debug("Sort time: " + (System.currentTimeMillis() - time));

        exec.setMessage("Creating sorted table");

        final BufferedDataContainer dc =
                exec.createDataContainer(dataTable.getDataTableSpec(), true);
        time = System.currentTimeMillis();
        for (i = 0; i < rows.length; i++) {
            exec.checkCanceled();
            exec.setProgress(progress / max);
            dc.addRowToTable(rows[i]);
            progress++;
        }
        dc.close();

        m_sortedTable = dc.getTable();
        LOGGER.debug("Write time: " + (System.currentTimeMillis() - time));
    }

    /**
     * Sorts the given data table using a disk-based k-way merge sort.
     *
     * @param dataTable the data table that should be sorted
     * @param exec an execution context for reporting progress and creating
     *            BufferedDataContainers
     * @throws CanceledExecutionException if the user has canceled execution
     */
    private void sortOnDisk(final BufferedDataTable dataTable,
            final ExecutionContext exec) throws CanceledExecutionException {
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

        m_sortedTable = cont.getTable();
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

    private static class MergeEntry implements Comparable<MergeEntry> {
        private DataRow m_row;
        private Iterator<DataRow> m_iterator;
        private int m_index;
        private Comparator<DataRow> m_comparator;

        /**
         * @param row
         * @param iterator
         */
        public MergeEntry(final Iterator<DataRow> iterator, final int index,
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
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_sortedTable.getDataTableSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowIterator iterator() {
        return m_sortedTable.iterator();
    }

    /**
     * The private class RowComparator is used to compare two DataRows. It
     * implements the Comparator-interface, so we can use the Arrays.sort method
     * to sort an array of DataRows. If both DataRows are null they are
     * considered as equal. A null DataRow is considered as 'less than' an
     * initialized DataRow. On each position, the DataCells of the two DataRows
     * are compared with their compareTo-method.
     *
     * @author Nicolas Cebron, University of Konstanz
     */
    private class RowComparator implements Comparator<DataRow> {

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

    /**
     * @return the sorted table as buffered data table
     */
    public BufferedDataTable getBufferedDataTable() {
        return m_sortedTable;
    }

    /**
     * @return the number of rows of this table
     */
    public int getRowCount() {
        return m_sortedTable.getRowCount();
    }
}

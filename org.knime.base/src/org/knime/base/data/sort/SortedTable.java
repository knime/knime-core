/* @(#)$RCSfile$
 * $Revision$ $Date$ $Author$
 *
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   23.10.2006 (sieb): created
 */
package org.knime.base.data.sort;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.knime.base.node.preproc.sorter.SorterNodeDialogPanel2;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DataContainer;
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

    private static final int MAX_OPEN_CONTAINERS = 40;

    private static final int MAX_CELLS_PER_CONTAINER =
            Math.max(DataContainer.MAX_CELLS_IN_MEMORY, 10000);

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

    private long m_counter = 0;

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
        m_rowComparator = rowComparator;
        m_spec = dataTable.getDataTableSpec();
        if (sortInMemory) {
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
        m_rowComparator = new RowComparator();
        m_spec = dataTable.getDataTableSpec();

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

        if (dataTable.getRowCount() == 0) {
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
        final double maxRowsPerContainer =
                MAX_CELLS_PER_CONTAINER
                        / (double)dataTable.getSpec().getNumColumns();
        final int contCount =
                (int)Math.ceil(dataTable.getRowCount() / maxRowsPerContainer);

        // split the input table into chunks
        BufferedDataContainer[] cont = new BufferedDataContainer[contCount];
        DataRow[] rows = new DataRow[(int)Math.ceil(maxRowsPerContainer)];
        // if only one container left (final output container), leave it to the
        // system to do the caching (bug 1809)
        cont[0] =
                exec.createDataContainer(dataTable.getDataTableSpec(), true,
                        (contCount == 1) ? -1 : 0);
        m_counter = 0;
        int k = 0, j = 0;
        double max =
                dataTable.getRowCount()
                        + dataTable.getRowCount()
                        * Math.ceil(Math.log(contCount)
                                / Math.log(MAX_OPEN_CONTAINERS));
        exec.setMessage("Reading table");
        for (DataRow row : dataTable) {
            rows[j++] = row;
            if (++m_counter % rows.length == 0) {
                Arrays.sort(rows, m_rowComparator);
                for (int i = 0; i < rows.length; i++) {
                    cont[k].addRowToTable(rows[i]);
                    rows[i] = null;
                }
                cont[k].close();
                j = 0;
                if (m_counter < dataTable.getRowCount()) {
                    cont[++k] =
                            exec.createDataContainer(dataTable
                                    .getDataTableSpec(), true, 0);
                }
            }
            if (m_counter % 1000 == 0) {
            	exec.checkCanceled();
            	exec.setProgress(m_counter / max, "Reading table, " + m_counter
            		+ " rows read");
            }
        }
        if (j > 0) {
            Arrays.sort(rows, 0, j, m_rowComparator);
            for (int i = 0; i < j; i++) {
                cont[k].addRowToTable(rows[i]);
                rows[i] = null;
            }
        }
        cont[k].close();

        // merge containers until only one is left
        while (k > 0) {
            exec.setMessage("Merging temporary tables, " + (k + 1)
                    + " remaining");
            int l = 0;
            // only merge at most MAX_OPEN_CONTAINERS at the same time
            // to avoid too many open file handles
            for (int i = 0; i <= k; i += MAX_OPEN_CONTAINERS) {
                cont[l++] =
                        merge(cont, i,
                                Math.min(i + MAX_OPEN_CONTAINERS, k + 1), exec,
                                max, (l == 1) && (k < MAX_OPEN_CONTAINERS));
            }
            k = l - 1;
        }

        m_sortedTable = cont[0].getTable();
    }

    private static class MergeIterator extends RowIterator {
        private DataRow m_nextRow;

        private final CloseableRowIterator m_it;

        MergeIterator(final BufferedDataTable table) {
            m_it = table.iterator();
            if (m_it.hasNext()) {
                m_nextRow = m_it.next();
            } else {
                m_it.close();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return (m_nextRow != null);
        }

        /**
         * Returns the next row without advancing the iterator.
         *
         * @return the next row
         */
        public DataRow showNext() {
            return m_nextRow;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            DataRow r = m_nextRow;
            if (m_it.hasNext()) {
                m_nextRow = m_it.next();
            } else {
                m_nextRow = null;
                m_it.close();
            }
            return r;
        }
    }

    /**
     * Merges the data containers in the given array into one data container.
     *
     * @param cont an array with data containers
     * @param left the first container in the array to merge (inclusive)
     * @param right the last container in the array to merge (exclusive)
     * @param exec an execution context
     * @param max the maximum progress (number of processed rows)
     * @param isLastContainer <code>true</code> if this created container is the
     *            last one and should obey the memory settings from the
     *            execution context
     * @return a merged and sorted data container
     * @throws CanceledExecutionException if execution has been canceled by the
     *             user
     */
    private BufferedDataContainer merge(final BufferedDataContainer[] cont,
            final int left, final int right, final ExecutionContext exec,
            final double max, final boolean isLastContainer)
            throws CanceledExecutionException {
        BufferedDataContainer out =
                exec.createDataContainer(cont[left].getTableSpec(), true,
                        isLastContainer ? -1 : 0);

        // open iterators for all containers
        MergeIterator[] it = new MergeIterator[right - left];
        for (int i = left; i < right; i++) {
            it[i - left] = new MergeIterator(cont[i].getTable());
        }

        // we make a linear search through all open iterators to find the
        // smallest row which we then add to the output container and advance
        // the corresponding iterator one further
        HashSet<MergeIterator> finished = new HashSet<MergeIterator>();
        while (finished.size() < it.length) {
            MergeIterator min = it[0];
            for (int i = 1; i < it.length; i++) {
                if (!min.hasNext()
                        || (it[i].hasNext() && (m_rowComparator.compare(min
                                .showNext(), it[i].showNext()) > 0))) {
                    min = it[i];
                }
            }

            out.addRowToTable(min.next());
            if (++m_counter % 1000 == 0) {
                exec.checkCanceled();
                exec.setProgress(m_counter / max);
            }
            if (!min.hasNext()) {
                finished.add(min);
            }
        }

        out.close();
        return out;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_sortedTable.getDataTableSpec();
    }

    /**
     * {@inheritDoc}
     */
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

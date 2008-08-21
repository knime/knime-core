/* @(#)$RCSfile$
 * $Revision$ $Date$ $Author$
 *
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
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

    /**
     * Number of cells for each container.
     */
    private static final int CONTAINERSIZE = 100000;

    private BufferedDataTable m_sortedTable;

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
            if (pos == -1) {
                throw new IllegalArgumentException("Could not find column name:"
                        + dc.toString());
            }
            m_indices[i] = pos;
        }

        if (sortInMemory) {
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

    private void sortOnDisk(final BufferedDataTable dataTable,
            final ExecutionContext exec) throws CanceledExecutionException {
        final ArrayList<BufferedDataContainer> containerVector =
                new ArrayList<BufferedDataContainer>();
        // Initialize RowIterator
        final RowIterator rowIt = dataTable.iterator();
        final int nrRows = dataTable.getRowCount();
        int currentRowNr = 0;

        final int nrContainerRows = CONTAINERSIZE
            / Math.max(1, m_spec.getNumColumns());
        // wrap all DataRows in Containers of size containerSize
        // sort each container before it is'stored'.
        BufferedDataContainer newContainer =
            exec.createDataContainer(m_spec, true, 0);
        int nrRowsinContainer = 0;
        // TODO: can be omitted due to new buffered table with known row size
        final ArrayList<DataRow> containerrowlist = new ArrayList<DataRow>();
        final ExecutionMonitor subexec = exec.createSubProgress(.5);
        int chunkCounter = 1;
        final int numChunks = (int)Math.ceil((double)nrRows / nrContainerRows);
        while (rowIt.hasNext()) {
            subexec.setProgress((double)currentRowNr / (double)nrRows,
                    "Reading in data-chunk " + chunkCounter + "...");
            exec.checkCanceled();
            if (newContainer.isClosed()) {
                newContainer = exec.createDataContainer(m_spec, true);
                nrRowsinContainer = 0;
            }
            final DataRow row = rowIt.next();
            currentRowNr++;
            nrRowsinContainer++;
            containerrowlist.add(row);
            if (nrRowsinContainer == nrContainerRows) {
                exec.checkCanceled();
                // sort list
                subexec.setMessage("Presorting chunk " + chunkCounter + " of "
                        + numChunks);
                Collections.sort(containerrowlist, m_rowComparator);
                // write in container
                for (final DataRow row2 : containerrowlist) {
                    newContainer.addRowToTable(row2);
                }
                newContainer.close();
                containerVector.add(newContainer);
                chunkCounter++;
                containerrowlist.clear();
            }
        }
        if (nrRowsinContainer % nrContainerRows != 0) {
            exec.checkCanceled();
            // sort list
            subexec.setMessage("Presorting chunk " + chunkCounter + " of "
                    + numChunks);
            Collections.sort(containerrowlist, m_rowComparator);
            // write in container
            for (final DataRow row2 : containerrowlist) {
                newContainer.addRowToTable(row2);
            }
            newContainer.close();
            containerVector.add(newContainer);
            containerrowlist.clear();
        }

        // merge all sorted containers together
        final BufferedDataContainer mergeContainer =
                exec.createDataContainer(m_spec, true, 0);

        // an array of RowIterators gives access to all (sorted) containers
        final RowIterator[] currentRowIterators =
                new RowIterator[containerVector.size()];
        final DataRow[] currentRowValues = new DataRow[containerVector.size()];

        // Initialise both arrays
        for (int c = 0; c < containerVector.size(); c++) {
            final BufferedDataContainer tempContainer = containerVector.get(c);
            final DataTable tempTable = tempContainer.getTable();
            currentRowIterators[c] = tempTable.iterator();
        }
        for (int c = 0; c < containerVector.size(); c++) {
            currentRowValues[c] = currentRowIterators[c].next();
        }
        int position = -1;

        // find the smallest/biggest element of all, put it in
        // mergeContainer
        final ExecutionMonitor subexec2 = exec.createSubProgress(.5);
        for (int i = 0; i < currentRowNr; i++) {
            subexec2.setProgress((double)i / (double)currentRowNr, "Merging");
            exec.checkCanceled();
            position = findNext(currentRowValues);
            mergeContainer.addRowToTable(currentRowValues[position]);
            if (currentRowIterators[position].hasNext()) {
                currentRowValues[position] =
                        currentRowIterators[position].next();
            } else {
                currentRowIterators[position] = null;
                currentRowValues[position] = null;
            }
        }
        // Everything should be written out in the MergeContainer
        for (int i = 0; i < currentRowIterators.length; i++) {
            assert (currentRowValues[i] == null);
            assert (currentRowIterators[i] == null);
        }
        mergeContainer.close();
        final BufferedDataTable dt = mergeContainer.getTable();
        assert (dt != null);
        m_sortedTable = dt;
    }

    /**
     * This method finds the next DataRow (position) that should be inserted in
     * the MergeContainer.
     */
    private int findNext(final DataRow[] currentValues) {
        int min = 0;
        while (currentValues[min] == null) {
            min++;
        }

        for (int i = min + 1; i < currentValues.length; i++) {
            if (currentValues[i] != null) {
                if (m_rowComparator.compare(currentValues[i],
                        currentValues[min]) < 0) {
                    min = i;
                }
            }
        }
        return min;
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
                // same column means that they have the same type
                final DataValueComparator comp =
                        m_spec.getColumnSpec(m_indices[i]).getType()
                                .getComparator();
                int cellComparison =
                        comp.compare(dr1.getCell(m_indices[i]), dr2
                                .getCell(m_indices[i]));

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

/* @(#)$RCSfile$
 * $Revision$ $Date$ $Author$
 *
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

import java.util.Comparator;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * A data table that sorts a given data table according to the passed sorting
 * parameters.
 *
 * <p><b>Note</b>: This class is only wrapping the class
 * {@link BufferedDataTableSorter}. It is recommend to use the
 * {@link BufferedDataTableSorter} class directly rather than this
 * class (which is kept anyway to retain backward compatibility).
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class SortedTable implements DataTable {

    private final BufferedDataTable m_sortedTable;

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
        BufferedDataTableSorter sorter =
            new BufferedDataTableSorter(dataTable, rowComparator);
        sorter.setSortInMemory(sortInMemory);
        sorter.setMaxOpenContainers(maxOpenContainer);
        m_sortedTable = sorter.sort(exec);
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
        BufferedDataTableSorter sorter =
            new BufferedDataTableSorter(dataTable, inclList, sortAscending);
        sorter.setMaxOpenContainers(maxOpenContainer);
        sorter.setSortInMemory(sortInMemory);
        m_sortedTable = sorter.sort(exec);
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

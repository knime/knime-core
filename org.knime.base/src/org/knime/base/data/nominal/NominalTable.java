/*
 * ---------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 */
package org.knime.base.data.nominal;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;


/**
 * This class wraps a {@link DataTable} into a new one by computing all possible
 * values for one particular column. This is then returned by the
 * {@link org.knime.core.data.DataTableSpec#getColumnSpec(int)} method. All
 * binned columns are then handled as String-valued columns.
 * <p>
 * Note, this computation can be time consuming, since it is necessary to
 * iterate to the full data table and checking each value.
 * 
 * @see DataColumnSpec
 * @see DataTableSpec
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class NominalTable implements DataTable {
    /** The wrapped table. */
    private final BufferedDataTable m_table;

    /** The input table spec with all poss. values for the selected column. */
    private final DataTableSpec m_spec;

    /**
     * Wraps the given table into a new table by computing all possible values
     * for the given <code>column</code>.
     * 
     * @param table the data table to work on
     * @param columns the column to find nominal values for
     * @param exec object to check with if user canceled
     * @throws CanceledExecutionException if user canceled execution
     * @throws NullPointerException if the table or <code>column</code> is
     *             <code>null</code>
     * @throws IllegalArgumentException if the <code>column</code> does not
     *             appear in the <code>table</code>
     * @throws IllegalStateException if the <code>column</code> appears at
     *             least twice in <code>table</code>
     * 
     * @see #computeValues(BufferedDataTable,ExecutionMonitor,String...)
     */
    public NominalTable(final BufferedDataTable table, 
            final ExecutionMonitor exec, final String... columns) 
            throws CanceledExecutionException {
        m_spec = computeValues(table, exec, columns);
        m_table = table;
    }

    /**
     * Wraps the given table into a new table by computing all possible values
     * for all columns.
     * 
     * @param table the data table to work on
     * @param exec object to check with if user canceled
     * @throws CanceledExecutionException if user canceled execution
     * @throws NullPointerException if the table is <code>null</code>
     */
    public NominalTable(final BufferedDataTable table, 
            final ExecutionMonitor exec) throws CanceledExecutionException {
        m_spec = computeValues(table, exec);
        m_table = table;
    }

    /**
     * Wraps the given table into a new table by computing all possible values
     * for all columns.
     * 
     * @param table the data table to work on
     * @param exec object to check with if user canceled
     * @return a new spec containing all possible value for all columns
     * @throws CanceledExecutionException if user canceled execution
     * @throws NullPointerException if the table is <code>null</code>
     */
    public static final DataTableSpec computeValues(
            final BufferedDataTable table, final ExecutionMonitor exec) 
            throws CanceledExecutionException {
        DataTableSpec spec = table.getDataTableSpec();
        int[] all = new int[spec.getNumColumns()];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        return computeValues(table, exec, all);
    }

    /**
     * Wraps the given table into a new table by computing all possible values
     * for the given column indices.
     * 
     * @param table the data table to work on
     * @param columnIndex the selected columns
     * @param exec object to check with if user canceled
     * @throws CanceledExecutionException if user canceled execution
     * @throws NullPointerException if the table is <code>null</code>
     * @throws IndexOutOfBoundsException if the column index is not in the range
     *             of the table
     * @throws IllegalArgumentException if the array of column indices is not
     *             sorted
     */
    public NominalTable(final BufferedDataTable table, 
            final ExecutionMonitor exec, final int... columnIndex) 
            throws CanceledExecutionException {
        m_spec = computeValues(table, exec, columnIndex);
        m_table = table;
    }

    /**
     * The table spec which contains at least all possible values for one
     * particular column.
     * 
     * @return the data table spec
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @return the original table's row iterator
     * @see org.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        return m_table.iterator();
    }

    /**
     * Computes all possible values based in the given table and column name.
     * 
     * @param table the underlying table
     * @param columns the columns to retrieve all possible values for
     * @param exec object to check with if user canceled operation
     * @return a new table spec
     * @throws NullPointerException if either the <code>table</code> or the
     *             <code>columns</code> array is <code>null</code>
     * @throws IllegalArgumentException if the <code>column</code> does not
     *             appear in the data
     * @throws IllegalStateException if the <code>column</code> appears at
     *             least twice in the table
     * @throws CanceledExecutionException if user canceled execution
     * 
     * @see #computeValues(BufferedDataTable,ExecutionMonitor,int...)
     */
    public static final DataTableSpec computeValues(
            final BufferedDataTable table, final ExecutionMonitor exec, 
            final String... columns) throws CanceledExecutionException {
        int[] colIndices = new int[columns.length];
        DataTableSpec spec = table.getDataTableSpec();
        for (int i = 0; i < columns.length; i++) {
            colIndices[i] = spec.findColumnIndex(columns[i]);
        }
        return computeValues(table, exec, colIndices);
    }

    /**
     * Finds all possible values based on a table and a number of given column
     * indices by iterating through the table.
     * 
     * @param table ihe table to get values from
     * @param columnIndex an array of sorted column indices
     * @param exec an object to check if user canceled
     * @return a modified table spec containing all possible values
     * @throws NullPointerException if the table is <code>null</code>
     * @throws IllegalArgumentException if column indices are not sorted
     * @throws IndexOutOfBoundsException if a column index is out of range
     * @throws CanceledExecutionException if user canceled operation
     */
    public static final DataTableSpec computeValues(
            final BufferedDataTable table, final ExecutionMonitor exec, 
            final int... columnIndex) throws CanceledExecutionException {
        DataTableSpec oldSpec = table.getDataTableSpec();
        // keep all possible values for each column (index)
        @SuppressWarnings("unchecked")
        Set<DataCell>[] set = new Set[columnIndex.length];
        HashSet<Integer> hash = new HashSet<Integer>();
        for (int c = 0; c < columnIndex.length; c++) {
            if (columnIndex[c] == -1) {
                throw new IllegalArgumentException("Column " + columnIndex[c]
                        + " not found.");
            }
            if (hash.contains(columnIndex[c])) {
                throw new IllegalArgumentException("Column indices "
                        + " contain duplicates: " + c);
            }
            if (c > 0 && columnIndex[c - 1] >= columnIndex[c]) {
                throw new IllegalArgumentException("Column indices are "
                        + "not sorted.");
            }
            hash.add(columnIndex[c]);
            set[c] = new HashSet<DataCell>();
        }
        // overall rows in the table
        int rowCount = 0;
        for (DataRow row : table) {
            // get value for column indices
            for (int c = 0; c < columnIndex.length; c++) {
                DataCell cell = row.getCell(columnIndex[c]);
                // adds only each value once
                set[c].add(cell);
            }
            if (exec != null) {
                exec.checkCanceled(); // throws exception if user canceled
                exec.setProgress((double) ++rowCount / table.getRowCount(), 
                        "" + row.getKey());
            }
        }
        DataColumnSpec[] newColSpecs = new DataColumnSpec[oldSpec
                .getNumColumns()];
        // index within the set of possible values
        int idx = 0;
        for (int i = 0; i < newColSpecs.length; i++) {
            DataColumnSpec oldColSpec = oldSpec.getColumnSpec(i);
            if (hash.contains(i)) {
                DataColumnSpecCreator creator = new DataColumnSpecCreator(
                        oldColSpec);
                DataCell lower = null;
                DataCell upper = null;
                if (oldColSpec.getDomain().hasBounds()) {
                    lower = oldColSpec.getDomain().getLowerBound();
                    upper = oldColSpec.getDomain().getUpperBound();
                } else {
                    // TODO DoubleValue is to restrict
                    if (oldColSpec.getType().isCompatible(DoubleValue.class)) {
                        TreeSet<DataCell> tSet = new TreeSet<DataCell>(
                                oldColSpec.getType().getComparator());
                        tSet.addAll(set[idx]);
                        lower = tSet.first();
                        upper = tSet.last();
                    }
                }
                DataColumnDomain dom = new DataColumnDomainCreator(set[idx],
                        lower, upper).createDomain();
                creator.setDomain(dom);
                newColSpecs[i] = creator.createSpec();
                idx++;
            } else {
                newColSpecs[i] = oldColSpec;
            }
        }
        // create new table spec along with all column specs
        return new DataTableSpec(newColSpecs);
    }
}

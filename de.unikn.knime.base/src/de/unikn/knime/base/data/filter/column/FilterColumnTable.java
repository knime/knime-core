/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.base.data.filter.column;

import java.util.LinkedHashSet;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DataValue;
import de.unikn.knime.core.data.RowIterator;

/**
 * This <code>DataTable</code> filters (includes or excludes) a specified
 * number of columns from a given table by just wrapping the underlying data
 * table.
 * <p>
 * These columns can be either filtered by a particular column
 * <code>DataType</code>, by a list of column names, or indices whereby the
 * ordering is maintained. Note, it is not possible to select a column twice
 * from the given table.
 * <p>
 * The ordering of the final table is depending on the order of the column names
 * resp. column indices.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class FilterColumnTable implements DataTable {

    /**
     * Underlying table to wrap.
     */
    private final DataTable m_data;

    /**
     * Column indices within the underlying table.
     */
    private final int[] m_columns;

    /**
     * The new spec derived from the underlying data given the included columns.
     */
    private final DataTableSpec m_tableSpec;

    /**
     * Inits a new filter column table based on a <code>DataTable</code> and
     * an unique, unordered number of column indices.
     * 
     * @param data The underlying data table.
     * @param columns The column indices to INCLUDE or EXCLUDE from the table.
     * @param include If <code>true</code> the columns are INCLUDE otherwise
     *            EXCLUDED.
     * @throws NullPointerException If one of the args is <code>null</code>.
     * @throws ArrayIndexOutOfBoundsException If one of the column indices lies
     *             not within the table's column range.
     * @throws IllegalArgumentException If the column indices are empty or one
     *             of the column indices is used twice.
     */
    public FilterColumnTable(final DataTable data, final boolean include,
            final int... columns) {
        // keep input data
        m_data = data;
        DataTableSpec spec = data.getDataTableSpec();
        // keep indices
        if (include) {
            m_columns = columns; // will be checked later
        } else {
            int cols = spec.getNumColumns();
            m_columns = inverse(columns, cols);
        }
        m_tableSpec = createFilterTableSpec(spec, m_columns);
        assert (m_tableSpec != null);
    }

    /**
     * Inits a new filter column table based on a <code>DataTable</code> and
     * an unique, unordered number of column indices.
     * 
     * @param data The underlying data table.
     * @param columns The column indices to INCLUDE in the new table.
     * @throws NullPointerException If one of the args is <code>null</code>.
     * @throws ArrayIndexOutOfBoundsException If one of the column indices lies
     *             not within the table's column range.
     * @throws IllegalArgumentException If the column indices are empty or one
     *             of the column indices is used twice.
     */
    public FilterColumnTable(final DataTable data, final int... columns) {
        this(data, true, columns);
    }

    /**
     * Calls <code>createFilterTableSpec(DataTableSpec, int[])</code>
     * arguments with the correct values in the int[] argument, i.e. it will
     * locate the columns and assign the "right" values.
     * 
     * @param spec The input spec.
     * @param columns The names of the columns that should survive.
     * @return A new spec with extracted columns.
     * @throws IndexOutOfBoundsException If columns not available.
     * @throws NullPointerException If either argument is null or contains null
     *             values.
     */
    public static final DataTableSpec createFilterTableSpec(
            final DataTableSpec spec, final DataCell... columns) {
        int[] cols = new int[columns.length];
        for (int i = 0; i < cols.length; i++) {
            cols[i] = spec.findColumnIndex(columns[i]);
        }
        return createFilterTableSpec(spec, cols);
    }

    /**
     * This function constructs a spec for this filter table. From the given
     * table spec it extracts the specified indicies and arranges them
     * accordingly. It stores references in the new table spec pointing to
     * objects referenced to by the passed table spec.
     * 
     * @param spec The input spec.
     * @param include Whether the column indices are to include or exclude.
     * @param columns The output column indices to extract from the input spec.
     * @return A new spec with extracted columns.
     * @throws IndexOutOfBoundsException If columns not available.
     * @throws NullPointerException If either argument is null or contains null
     *             values.
     */
    public static final DataTableSpec createFilterTableSpec(
            final DataTableSpec spec, final boolean include, 
            final int... columns) {
        // if column indices refer to the include list
        if (include) {
            return createFilterTableSpec(spec, columns);
        } else { // create the complement list
            int cols = spec.getNumColumns();
            int[] icolumns = inverse(columns, cols);
            return createFilterTableSpec(spec, icolumns);
        }
    }

    private static int[] inverse(final int[] columns, final int len) {
        boolean[] ins = new boolean[len];
        for (int i = 0; i < columns.length; i++) {
            ins[columns[i]] = true; // exclude it
        }
        // inverse column indice list
        int[] icolumns = new int[len - columns.length];
        int j = 0; // count the includes
        // via all column keep included ones
        for (int i = 0; i < ins.length; i++) {
            if (!ins[i]) {
                icolumns[j] = i;
                j++;
            }
        }
        return icolumns;
    }

    /**
     * This function constructs a spec for this filter table. From the given
     * table spec it extracts the specified indicies and arranges them
     * accordingly. It stores references in the new table spec pointing to
     * objects referenced to by the passed table spec.
     * 
     * @param spec The input spec.
     * @param columns The output column indices to extract from the input spec.
     * @return A new spec with extracted columns.
     * @throws IndexOutOfBoundsException If columns not available.
     * @throws NullPointerException If either argument is null or contains null
     *             values.
     */
    public static final DataTableSpec createFilterTableSpec(
            final DataTableSpec spec, final int... columns) {

        assert (spec != null);

        for (int i = 0; i < columns.length; i++) {
            if (columns[i] < 0 || columns[i] >= spec.getNumColumns()) {
                throw new ArrayIndexOutOfBoundsException(
                        "Column index is out of range: 0 <= " + columns[i]
                                + " < " + spec.getNumColumns() + ".");
            } else {
                // test if column already in use
                for (int j = 0; j < i; j++) {
                    if (columns[i] == columns[j]) {
                        throw new IllegalArgumentException(
                                "Column index is already is use: [" + i
                                        + "] = [" + j + "] = " + columns[i]
                                        + ".");
                    }
                }
            }
        }

        DataColumnSpec[] cspecs = new DataColumnSpec[columns.length];
        for (int i = 0; i < columns.length; i++) {
            cspecs[i] = spec.getColumnSpec(columns[i]);
        }

        return new DataTableSpec(cspecs);
    } // createFilterColumnTableSpec(DataTableSpec,int...)

    /**
     * Inits a new filter column table based on a <code>DataTable</code> and a
     * unique, unordered number of column names.
     * 
     * @param data The underlying data table.
     * @param columns The column name to filter.
     * @throws NullPointerException If one of the args is <code>null</code>.
     * @throws IllegalArgumentException if one of the column name is already is
     *             use or the column name does not exist in the table.
     */
    public FilterColumnTable(final DataTable data, final DataCell... columns) {
        this(data, findColumnIndices(data.getDataTableSpec(), columns));
    }

    /**
     * Find all column indices in the spec.
     * 
     * @param spec The spec to search in.
     * @param columns The column indices.
     * @return A sorted array of indices.
     */
    private static int[] findColumnIndices(final DataTableSpec spec,
            final DataCell... columns) {
        int[] ret = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (columns[i] == null) {
                throw new NullPointerException("Column name at index " + i
                        + " must not be null!");
            } else {
                ret[i] = spec.findColumnIndex(columns[i]);
                if (ret[i] < 0 || ret[i] >= spec.getNumColumns()) {
                    throw new IllegalArgumentException("Column name "
                            + columns[i].toString() + " not found.");
                }
                // check uniqueness
                for (int k = 0; k < i; k++) {
                    if (ret[i] == ret[k]) {
                        throw new IllegalArgumentException(
                                "Column index is already in use: [" + i
                                        + "] = [" + k + "] = " + ret[k] + ".");
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Inits a new filter column table based on a <code>DataTable</code> and
     * one type (<code>DataCell</code>) using <code>java.lang.Class</code>
     * to extract these columns.
     * 
     * @param data The underlying table.
     * @param type The column type to be extracted.
     * @throws NullPointerException if one of the args is <code>null</code>.
     * @throws ClassCastException If the column type is not assignable from
     *             <code>DataCell</code>.
     * @throws IllegalArgumentException If the given type does not appear in the
     *             table.
     */
    public FilterColumnTable(final DataTable data, final DataType type) {

        // retrieve input spec and keep data table
        DataTableSpec spec = data.getDataTableSpec();
        m_data = data;

        LinkedHashSet<Integer> columnList = new LinkedHashSet<Integer>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataType c = m_data.getDataTableSpec().getColumnSpec(i).getType();
            if (type.isASuperTypeOf(c)) {
                columnList.add(i);
            }
        }

        // int array with column indices
        m_columns = new int[columnList.size()];
        int idx = -1;
        for (int i : columnList) {
            m_columns[++idx] = i;
        }

        // create new output spec
        m_tableSpec = createFilterTableSpec(data.getDataTableSpec(), m_columns);
        assert (m_tableSpec != null);
    } // FilterColumnTable(DataTable,DataType)

    /**
     * Inits a new filter column table based on a <code>DataTable</code> and a
     * <code>DataValue</code> class as value. This table will contain all
     * columns from <code>data</code> which are compatible to
     * <code>value</code>.
     * 
     * @param data The underlying table.
     * @param value The compatible value type to be included.
     * @throws NullPointerException If any argument is <code>null</code>.
     */
    public FilterColumnTable(final DataTable data,
            final Class<? extends DataValue> value) {
        this(data, true, findCompatibleColums(data.getDataTableSpec(), value));
    }

    private static int[] findCompatibleColums(final DataTableSpec spec,
            final Class<? extends DataValue> value) {
        LinkedHashSet<Integer> columnList = new LinkedHashSet<Integer>();
        for (int c = 0; c < spec.getNumColumns(); c++) {
            if (spec.getColumnSpec(c).getType().isCompatible(value)) {
                columnList.add(c);
            }
        }
        // int array with column indices
        int[] columns = new int[columnList.size()];
        int idx = -1;
        for (int i : columnList) {
            columns[++idx] = i;
        }
        return columns;
    }

    /**
     * @return A copy of the used column indices from the filtered data table.
     */
    public int[] getColumnIndices() {
        int[] copiedIndices = new int[m_columns.length];
        System.arraycopy(m_columns, 0, copiedIndices, 0, m_columns.length);
        return copiedIndices;
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_tableSpec;
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        return new FilterColumnRowIterator(m_data.iterator(), m_columns);
    }

} // FilterColumnTable

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
package org.knime.base.data.filter.column;

import java.util.LinkedHashSet;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CloseableRowIterator;


/**
 * This {@link DataTable} filters (includes or excludes) a specified number of
 * columns from a given table by just wrapping the underlying data table.
 * <p>
 * These columns can be either filtered by a particular column {@link DataType},
 * by a list of column names, or indices whereby the ordering is maintained.
 * Note, it is not possible to select a column twice from the given table.
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
     * Inits a new filter column table based on a {@link DataTable} and an
     * unique, unordered number of column indices.
     *
     * @param data the underlying data table
     * @param columns the column indices to INCLUDE or EXCLUDE from the table
     * @param include if <code>true</code> the columns are INCLUDEd otherwise
     *            EXCLUDEd
     * @throws ArrayIndexOutOfBoundsException if one of the column indices lies
     *             not within the table's column range
     * @throws IllegalArgumentException if the <code>data</code> or
     *             <code>columns</code> are <code>null</code>, or column
     *             indices are out of range or appear as duplicates in the array
     */
    public FilterColumnTable(final DataTable data, final boolean include,
            final int... columns) {
        if (data == null) {
            throw new IllegalArgumentException("Table must not be null");
        }
        if (columns == null) {
            throw new IllegalArgumentException("Indices must not be null.");
        }
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
     * Inits a new filter column table based on a {@link DataTable} and an
     * unique, unordered number of column indices.
     *
     * @param data the underlying data table
     * @param columns the column indices to INCLUDE in the new table
     * @throws NullPointerException if one of the args is <code>null</code>
     * @throws ArrayIndexOutOfBoundsException if one of the column indices lies
     *             not within the table's column range
     * @throws IllegalArgumentException if the column indices are empty or one
     *             of the column indices is used twice
     */
    public FilterColumnTable(final DataTable data, final int... columns) {
        this(data, true, columns);
    }

    /**
     * Calls {@link #createFilterTableSpec(DataTableSpec, int[])} arguments with
     * the correct values in the <code>int[]</code> argument, i.e. it will
     * locate the columns and assign the "right" values.
     *
     * @param spec the input spec
     * @param columns the names of the columns that should survive
     * @return a new spec with extracted columns
     * @throws IndexOutOfBoundsException if columns are no available
     * @throws NullPointerException if either argument is <code>null</code> or
     *             contains <code>null</code> values
     */
    public static final DataTableSpec createFilterTableSpec(
            final DataTableSpec spec, final String... columns) {
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
     * @param spec the input spec
     * @param include whether the column indices are to include or exclude
     * @param columns the output column indices to extract from the input spec
     * @return a new spec with extracted columns
     * @throws IndexOutOfBoundsException if columns not available
     * @throws NullPointerException if either argument is <code>null</code> or
     *             contains <code>null</code> values
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
        checkIndices(len, columns);
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
     * @param spec the input spec
     * @param columns The output column indices to extract from the input spect
     * @return a new spec with extracted columns
     * @throws IndexOutOfBoundsException if columns are not availablea
     * @throws NullPointerException if either argument is null or contains null
     *             values
     */
    public static final DataTableSpec createFilterTableSpec(
            final DataTableSpec spec, final int... columns) {
        checkIndices(spec.getNumColumns(), columns);
        DataColumnSpec[] cspecs = new DataColumnSpec[columns.length];
        for (int i = 0; i < columns.length; i++) {
            cspecs[i] = spec.getColumnSpec(columns[i]);
        }
        return new DataTableSpec(cspecs);
    }

    private static void checkIndices(final int ncols, final int... indices) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < 0 || indices[i] >= ncols) {
                throw new ArrayIndexOutOfBoundsException("index out of range: "
                        + indices[i]);
            }
            for (int j = i + 1; j < indices.length; j++) {
                if (indices[i] == indices[j]) {
                    sb.append(" (" + i + "," + j + ")");
                }
            }
        }
        if (sb.length() > 0) {
            throw new IllegalArgumentException("column duplicates:"
                    + sb.toString());
        }
    }

    /**
     * Inits a new filter column table based on a {@link DataTable} and a
     * unique, unordered number of column names.
     *
     * @param data the underlying data table
     * @param columns the column name to filter
     * @throws NullPointerException if one of the args is <code>null</code>
     * @throws IllegalArgumentException if one of the column name is already is
     *             use or the column name does not exist in the table.
     */
    public FilterColumnTable(final DataTable data, final String... columns) {
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
            final String... columns) {
        if (columns == null) {
            throw new IllegalArgumentException("Columns must not be null.");
        }
        int[] ret = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (columns[i] == null) {
                throw new NullPointerException("Column name at index " + i
                        + " must not be null!");
            } else {
                ret[i] = spec.findColumnIndex(columns[i]);
                if (ret[i] < 0 || ret[i] >= spec.getNumColumns()) {
                    throw new IllegalArgumentException("Column name "
                            + columns[i] + " not found.");
                }
            }
        }
        checkIndices(spec.getNumColumns(), ret);
        return ret;
    }

    /**
     * Inits a new filter column table based on a {@link DataTable} and one
     * type ({@link org.knime.core.data.DataCell}))
     * using {@link java.lang.Class} to extract these columns.
     *
     * @param data the underlying table
     * @param type the column type to be extractedt
     * @throws IllegalArgumentException if the given type does not appear in the
     *             table
     */
    public FilterColumnTable(final DataTable data, final DataType type) {
        if (data == null) {
            throw new IllegalArgumentException("Table must not be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("DataType must not be null.");
        }
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
     * Inits a new filter column table based on a {@link DataTable} and a
     * {@link DataValue} class as value. This table will contain all columns
     * from <code>data</code> which are compatible to <code>value</code>.
     *
     * @param data the underlying table
     * @param value the compatible value type to be included
     * @throws NullPointerException if any argument is <code>null</code>
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
     * @return a copy of the used column indices from the filtered data table
     */
    public int[] getColumnIndices() {
        int[] copiedIndices = new int[m_columns.length];
        System.arraycopy(m_columns, 0, copiedIndices, 0, m_columns.length);
        return copiedIndices;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_tableSpec;
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        RowIterator it = m_data.iterator();
        if (it instanceof CloseableRowIterator) {
            return new CloseableFilterColumnRowIterator(
                    (CloseableRowIterator) it, m_columns);
        } else {
            return new FilterColumnRowIterator(it, m_columns);
        }
    }
}

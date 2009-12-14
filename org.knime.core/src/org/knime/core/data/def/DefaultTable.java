/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * --------------------------------------------------------------------- *
 * History
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.util.ObjectToDataCellConverter;

/**
 * Deprecated(!) default implementation of a {@link DataTable} object. This
 * implementation keeps the data in memory all the time. Don't use it. We have
 * better containers: rather use the
 * {@link org.knime.core.data.container.DataContainer DataContainer}.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @deprecated DefaultTable will hold the entire data in main memory and should
 *             therefore not be used anymore. Instead, you should consider to
 *             use a {@link org.knime.core.data.container.DataContainer} (or in
 *             a Node's {@link org.knime.core.node.NodeModel#execute(
 *             org.knime.core.node.BufferedDataTable[],
 *             org.knime.core.node.ExecutionContext) execute method} a
 *             {@link org.knime.core.node.BufferedDataTable} to create
 *             DataTables).
 */
public class DefaultTable implements DataTable {

    /**
     * Keeps the data, elements of type DataRow.
     */
    private final ArrayList<DataRow> m_rowList;

    /**
     * Keeps the information specifying the structure of the table, also serving
     * as meta info object passed before execution of a node. It stores the
     * names of the columns, if not given in constructor, automatically
     * generated ("Column_"+i). And the Types of the columns.
     */
    private final DataTableSpec m_tableSpec;

    /**
     * Creates a new table object from an array of <code>DataRow</code>
     * objects, and an array of column names and types.
     * 
     * @param rows The list of <code>DataRow</code> objects.
     * @param spec The Spec of this table.
     * @throws NullPointerException If one of the arguments is 
     *              <code>null</code>, or if the array contains 
     *              <code>null</code> values.
     * @throws IllegalArgumentException If any runtime class in the row's cells
     *             does not comply with the settings in the spec.
     */
    public DefaultTable(final DataRow[] rows, final DataTableSpec spec) {
        // check row array
        if (rows == null) {
            throw new NullPointerException("DataRow array must not be null!");
        }

        // sanity check for valid column type information
        for (int r = 0; r < rows.length; r++) {
            DataRow row = rows[r];
            int numCells = row.getNumCells();
            if (numCells != spec.getNumColumns()) {
                throw new IllegalArgumentException("Cell count in row " + r
                        + " is not equal to length of column names array: "
                        + numCells + " vs. " + spec.getNumColumns());
            }
            for (int c = 0; c < numCells; c++) {
                DataType columnType = spec.getColumnSpec(c).getType();
                DataType cellType = row.getCell(c).getType();
                if (!columnType.isASuperTypeOf(cellType)) {
                    throw new IllegalArgumentException(
                            "Runtime class of object \""
                                    + row.getCell(c).toString()
                                    + "\" at index (" + r + "; " + c + ") is "
                                    + cellType
                                    + " and does not comply with its supposed "
                                    + "superclass " + columnType + "!");
                }
            } // for all cells
        }
        // everything is valid, copy data
        m_rowList = new ArrayList<DataRow>(Arrays.asList(rows));
        m_tableSpec = spec;
    }

    /**
     * Creates a new table object from an array of <code>DataRow</code>
     * objects, and an array of column names and types.
     * 
     * @param rows The list of <code>DataRow</code> objects.
     * @param columnNames The names of the columns.
     * @param columnTypes The column types.
     * @throws NullPointerException If one of the arguments is 
     *              <code>null</code>, or if any array contains 
     *              <code>null</code> values.
     * @throws IllegalStateException If redundant column names are found.
     * @throws IllegalArgumentException If any element in
     *             <code>columnTypes</code> is not a (sub-)class of
     *             <code>DataCell.class</code> or the runtime class for an
     *             element of the corresponding cells in <code>rows</code> is
     *             a supertype of its <code>columnTypes</code> counterpart.
     */
    public DefaultTable(final DataRow[] rows, final String[] columnNames,
            final DataType[] columnTypes) {
        this(rows, new DataTableSpec(columnNames, columnTypes));
    }

    /**
     * Private constructor that is used by all constructors below. It inits the
     * data structure from the ObjectSupplier.
     * 
     * @param data Get the data from, in whatever way.
     * @param rowHeader Containing row header information.
     * @param colHeader Containing column header information.
     * @param conv Used to get DataCell objects from the ObjectSupplier.
     */
    private DefaultTable(final ObjectSupplier data, final String[] rowHeader,
            final String[] colHeader, final ObjectToDataCellConverter conv) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null.");
        }
        // check row count
        final int rowCount = data.getRowCount();
        // check row count, a valid rowCount must have been given
        if (rowHeader != null && rowHeader.length != rowCount) {
            throw new IllegalArgumentException("Row count inconsistent:"
                    + rowHeader.length + " vs. " + rowCount + ".");
        }

        /*
         * the following lines define the correct column count, this looks a bit
         * messy, but there is no obvious way to shorten it
         */

        // figure out column count ... somehow. If fails, use 0
        int colCount = -1;
        if (rowCount > 0) {
            colCount = data.getColumnCount(0);
        } else if (colHeader != null) {
            colCount = colHeader.length;
        } else {
            colCount = 0;
        }

        // sanity check if column count is consistent
        if (colCount >= 0 && colHeader != null 
                && colHeader.length != colCount) {
            throw new IllegalArgumentException("Column count inconsistent: "
                    + colHeader.length + " vs. " + colCount + ".");
        }

        // create column header and types
        String[] myHeaders = new String[colCount];
        for (int c = 0; c < colCount; c++) {
            String head = (colHeader != null) ? colHeader[c] : "Column_" + c;
            myHeaders[c] = head;
        } // for-loop: all columns

        // try to find meaningful column types
        DataType[] myTypes = new DataType[colCount];

        // copy data into own structure
        m_rowList = new ArrayList<DataRow>(rowCount);
        for (int r = 0; r < rowCount; r++) {
            if (data.getColumnCount(r) != colCount) {
                throw new IllegalArgumentException(
                        "Unmatching column count in row " + r + ": " + colCount
                                + " vs. " + data.getColumnCount(r) + ".");
            }
            // name of the row ... if provided
            String rowHead = (rowHeader != null ? rowHeader[r] : "Row_" + r);
            DataCell[] rowContent = new DataCell[colCount];
            // traverse columns in row
            for (int c = 0; c < colCount; c++) {
                // some overhead, hope this is ok.
                Object o = data.getObject(r, c);
                rowContent[c] = conv.createDataCell(o);
                DataType cellType = rowContent[c].getType();
                if (r == 0) { // for first row simply assign class
                    myTypes[c] = cellType;
                } else { // for any other row get base class
                    myTypes[c] =
                            DataType.getCommonSuperType(myTypes[c], cellType);
                }
            } // for all columns in row
            DataRow row = new DefaultRow(rowHead, rowContent);
            m_rowList.add(row);
        } // for all rows

        // if no rows available: assume StringType as column type
        if (rowCount == 0) {
            Arrays.fill(myTypes, StringCell.TYPE);
        }
        m_tableSpec = new DataTableSpec(myHeaders, myTypes);
    } // DefaultTable(ObjectSupplier, String[], String[], ...)

    /**
     * Generates a new instance from an <code>Object[][]</code>. The data is
     * provided in a parameter array containing <code>Object</code>s that are
     * known by the given <code>converter</code>'s
     * <code>createDataCell(Object)</code> method. The column type is
     * determined by the most specific <code>DataCell</code> class for all
     * cells in a column.
     * 
     * @param data Content of the table, all rows must have same length,
     *            <code>null</code> values are ok to indicate missing values.
     * @param rowHeader The name of the rows in an array. May be
     *            <code>null</code>. The length of the array (if given) must
     *            match <code>data.length</code>.
     * @param colHeader The name of the columns in an array. May be
     *            <code>null</code>. The length of the array (if given) must
     *            match <code>data[r].length</code> for any given
     *            <code>r</code>.
     * @param conv Used to transform any object in <code>data</code> to a
     *            <code>DataCell</code>
     * @throws IllegalArgumentException In the following cases:
     *             <ul>
     *             <li> <code>data</code> is <code>null</code>
     *             <li> <code>data.length</code> !=
     *             <code>rowHeader.length</code> if <code>rowHeader</code>
     *             is given (non-<code>null</code>)
     *             <li> if length of <code>colHeader</code> (if not
     *             <code>null</code>) is unequal to the length of any row in
     *             <code>data</code>
     *             <li> if any object in <code>data</code> cannot be handled
     *             by <code>converter</code>.
     *             </ul>
     * @see DataType#getCommonSuperType(DataType, DataType)
     */
    public DefaultTable(final Object[][] data, final String[] rowHeader,
            final String[] colHeader, final ObjectToDataCellConverter conv) {
        this(new ObjectSupplier(data), rowHeader, colHeader, conv);
    }

    /**
     * Generates a new instance from an <code>Object[][]</code> using the
     * default <code>DataCell</code> factory. That is, <code>data</code> may
     * only contain objects of type <code>String</code>, <code>Integer</code>,
     * <code>Byte</code>, <code>Double</code>, <code>Float</code> or
     * <code>null</code> to identify missing values. See the factory
     * implementation, {@link ObjectToDataCellConverter#createDataCell(Object)},
     * for details. The column type is determined by the most specific
     * <code>DataCell</code> class for all cells in a column.
     * 
     * @param data Content of the table, all rows must have same length,
     *            <code>null</code> values are ok to indicate missing values.
     * @param rowHeader The name of the rows in an array. May be
     *            <code>null</code>. The length of the array (if given) must
     *            match <code>data.length</code>.
     * @param colHeader The name of the columns in an array. May be
     *            <code>null</code>. The length of the array (if given) must
     *            match <code>data[r].length</code> for any given
     *            <code>r</code>.
     * @throws IllegalArgumentException In the following cases:
     *             <ul>
     *             <li> <code>data</code> is <code>null</code>
     *             <li> <code>data.length</code> !=
     *             <code>rowHeader.length</code> if <code>rowHeader</code>
     *             is given (non-<code>null</code>)
     *             <li> if length of <code>colHeader</code> (if not
     *             <code>null</code>) is unequal to the length of any row in
     *             <code>data</code>
     *             <li> if any object in <code>data</code> cannot be handled
     *             by <code>converter</code>.
     *             </ul>
     * @see DataType#getCommonSuperType(DataType, DataType)
     */
    public DefaultTable(final Object[][] data, final String[] rowHeader,
            final String[] colHeader) {
        this(new ObjectSupplier(data), rowHeader, colHeader,
                ObjectToDataCellConverter.INSTANCE);
    }

    /**
     * Calls <code>this(data, (String[])null, (String[])null);</code>.
     * 
     * @param data Data to be set in this table. For further details see other
     *            constructor
     * @see #DefaultTable(Object[][], String[], String[])
     */
    public DefaultTable(final Object[][] data) {
        this(data, (String[])null, (String[])null);
    }

    /**
     * Generates a new instance from an <code>int[][]</code> using the default
     * <code>DataCell</code> factory. All entries in this parameter array are
     * wrapped by an <code>IntCell</code> by calling
     * <code>ObjectToDataCellConverter.createDataCell(new Integer(data[i][j])
     * </code>
     * 
     * @param data Content of the table, all rows must have same length.
     * @param rowHeader The name of the rows in an array. May be
     *            <code>null</code>. The length of the array (if given) must
     *            match <code>data.length</code>.
     * @param colHeader The name of the columns in an array. May be
     *            <code>null</code>. The length of the array (if given) must
     *            match <code>data[r].length</code> for any given
     *            <code>r</code>.
     * @throws IllegalArgumentException In the following cases:
     *             <ul>
     *             <li> <code>data</code> is <code>null</code>
     *             <li> <code>data.length</code> !=
     *             <code>rowHeader.length</code> if <code>rowHeader</code>
     *             is given (non-<code>null</code>)
     *             <li> if length of <code>colHeader</code> (if not
     *             <code>null</code>) is unequal to the length of any row in
     *             <code>data</code>
     *             <li> if any object in <code>data</code> cannot be handled
     *             by <code>converter</code>.
     *             </ul>
     * @see ObjectToDataCellConverter#createDataCell(Object)
     */
    public DefaultTable(final int[][] data, final String[] rowHeader,
            final String[] colHeader) {
        this(new ObjectSupplier(data), rowHeader, colHeader,
                ObjectToDataCellConverter.INSTANCE);
    }

    /**
     * Calls <code>this(data, (String[])null, (String[])null);</code>.
     * 
     * @param data Data to be set in this table. For further details see other
     *            constructor
     * @see #DefaultTable(int[][], String[], String[])
     */
    public DefaultTable(final int[][] data) {
        this(data, (String[])null, (String[])null);
    }

    /**
     * Generates a new instance from an <code>double[][]</code> using the
     * default <code>DataCell</code> factory. All entries in this parameter
     * array are wrapped by an <code>DoubleCell</code> by calling
     * <code>ObjectToDataCellConverter.createDataCell(new Double(data[i][j])
     * </code>
     * 
     * @param data Content of the table, all rows must have same length.
     * @param rowHeader The name of the rows in an array. May be
     *            <code>null</code>. The length of the array (if given) must
     *            match <code>data.length</code>.
     * @param colHeader The name of the columns in an array. May be
     *            <code>null</code>. The length of the array (if given) must
     *            match <code>data[r].length</code> for any given
     *            <code>r</code>.
     * @throws IllegalArgumentException In the following cases:
     *             <ul>
     *             <li> <code>data</code> is <code>null</code>
     *             <li> <code>data.length</code> !=
     *             <code>rowHeader.length</code> if <code>rowHeader</code>
     *             is given (non-<code>null</code>)
     *             <li> if length of <code>colHeader</code> (if not
     *             <code>null</code>) is unequal to the length of any row in
     *             <code>data</code>
     *             <li> if any object in <code>data</code> cannot be handled
     *             by <code>converter</code>.
     *             </ul>
     */
    public DefaultTable(final double[][] data, final String[] rowHeader,
            final String[] colHeader) {
        this(new ObjectSupplier(data), rowHeader, colHeader,
                ObjectToDataCellConverter.INSTANCE);
    }

    /**
     * Calls <code>this(data, (String[])null, (String[])null);</code>.
     * 
     * @param data Data to be set in this table. For further details see other
     *            constructor
     * @see #DefaultTable(double[][], String[], String[])
     */
    public DefaultTable(final double[][] data) {
        this(data, (String[])null, (String[])null);
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
        return new DefaultRowIterator(m_rowList);
    }

    /**
     * Get a reference to underlying data container. The returned
     * <code>ArrayList</code> contains objects of type <code>DataRow</code>.
     * This method never returns <code>null</code>, even though the returned
     * list can be empty if there are no rows in the table.
     * 
     * @return reference to internal data container.
     */
    protected final List<DataRow> getRowsInList() {
        return Collections.unmodifiableList(m_rowList);
    }

    /**
     * Get the number of rows in this table.
     * 
     * @return The number of rows.
     */
    public int getRowCount() {
        return m_rowList.size();
    }

    /**
     * This class serves as some kind of template. It is used in the different
     * constructors to retrieve an object at position (row, column). The
     * DefaultTable has constructors expecting int[][], double[][], or Object[],
     * thus we would have to duplicate code in order to get the cell at (row,
     * column). May be the template mechanism in Java 1.5 helps to avoid this
     * inner class.
     */
    private static class ObjectSupplier {

        /**
         * For each row an element in this array. Each element is instance of
         * either int[], double[], or Object[].
         */
        private final Object[] m_rows;

        /**
         * Constructor handling int values.
         * 
         * @param data the data in an int array
         */
        public ObjectSupplier(final int[][] data) {
            if (data == null) {
                throw new IllegalArgumentException("Data must not be null.");
            }
            m_rows = data;
        }

        /**
         * Constructor handling double values.
         * 
         * @param data the data in a double array
         */
        public ObjectSupplier(final double[][] data) {
            if (data == null) {
                throw new IllegalArgumentException("Data must not be null.");
            }
            m_rows = data;
        }

        /**
         * Constructor handling arbitray Objects.
         * 
         * @param data the data in an array
         */
        public ObjectSupplier(final Object[][] data) {
            if (data == null) {
                throw new IllegalArgumentException("Data must not be null.");
            }
            m_rows = data;
        }

        /**
         * Get the row count, that is the length of m_rows.
         * 
         * @return number of rows
         */
        public int getRowCount() {
            return m_rows.length;
        }

        /**
         * Get the column count for a particular row. In general, the column
         * count is the same for all rows. The outer class will use these return
         * values for sanity checks.
         * 
         * @param row the row of interest
         * @return the length of the row
         */
        public int getColumnCount(final int row) {
            if (m_rows instanceof int[][]) {
                return ((int[])m_rows[row]).length;
            }
            if (m_rows instanceof double[][]) {
                return ((double[])m_rows[row]).length;
            }
            return ((Object[])m_rows[row]).length;
        }

        /**
         * Get the object at position (row, column). If the the constructor
         * expecting an Object[][] was called, it simply returns the value at
         * the proper position, otherwise it will do a return new Double(..) or
         * new Integer(..), resp.
         * 
         * @param row the row of interest
         * @param column the column of interest
         * @return the object at this position, or a new Number if the generic
         *         constructors were used
         */
        public Object getObject(final int row, final int column) {
            if (m_rows instanceof int[][]) {
                int[] allInts = (int[])m_rows[row];
                return new Integer(allInts[column]);
            }
            if (m_rows instanceof double[][]) {
                double[] allDoubles = (double[])m_rows[row];
                return new Double(allDoubles[column]);
            }
            return ((Object[])m_rows[row])[column];
        }
    } // private class ObjectSupplier
}

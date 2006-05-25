/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 */
package de.unikn.knime.base.data.container;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnDomainCreator;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataColumnSpecCreator;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.def.DoubleCell;
import de.unikn.knime.core.data.def.StringCell;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;

/**
 * Buffer that collects <code>DataRow</code> objects and creates a 
 * <code>DataTable</code> on request. This data structure is useful if the 
 * number of rows is not known in advance. 
 * 
 * <p>Usage: Once created, the container needs to be initialized by calling
 * <code>open(DataTableSpec)</code>. Add the data using the 
 * <code>addRowToTable(DataRow)</code> method and finally close it with
 * <code>close()</code>. You can access the table by <code>getTable()</code>.
 * 
 * <p>Note regarding the column domain: This implementation updates the column
 * domain while new rows are added to the table. It will keep the lower and 
 * upper bound for all columns that are numeric, i.e. whose column type is
 * a sub type of <code>DoubleCell.TYPE</code>. For categorical columns,
 * it will keep the list of possible values if the number of different values
 * does not exceed 60. (If there are more, the values are forgotten and 
 * therefore not available in the final table.) A categorical column is 
 * a column whose type is a sub type of <code>StringCell.TYPE</code>, 
 * i.e. <code>StringCell.TYPE.isSuperTypeOf(yourtype)</code> where 
 * yourtype is the given column type.
 * 
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DataContainer implements RowAppender {
    
    /** 
     * Number of cells that are cached without being written to the 
     * temp file (see Buffer implementation).
     * TODO: We need to find a way to figure out how much memory a row occupies
     */
    private static final int MAX_CELLS_IN_MEMORY = 0; //2000000;
    
    /**
     * The number of possible values being kept at most. If the number of
     * possible values in a column exceeds this values, no values will
     * be memorized.
     */
    private static final int MAX_POSSIBLE_VALUES = 60;

    /** The object that saves the rows. */
    private Buffer m_buffer;
    
    private final int m_maxCellsInMemory; 
    
    /** Holds the keys of the added rows to check for duplicates. */
    private HashSet<RowKey> m_keySet;
    
    /** The tablespec of the return table. */
    private DataTableSpec m_spec;
    
    /** Table to return. Not null when close() is called. */
    private DataTable m_table;
    
    /** The number of possible values to be memorized. */
    private int m_maxPossibleValues;
    
    /** For each column, memorize the possible values. For detailed information
     * regarding the possible values and range determination, refer to the
     * class description.
     */
    private LinkedHashSet<DataCell>[] m_possibleValues;
    
    /** The min values in each column, we keep these values only for numerical
     * column (i.e. double, int).  For non-numerical columns the respective
     * entries will be null.
     */
    private DataCell[] m_minCells;

    /** The max values in each column, similar to m_minCells.
     */
    private DataCell[] m_maxCells;
    
    
    /** Creates a new container with at most 2000000 cells that 
     * are kept in memory. If more cells are added, rows are buffered to disk.
     */
    public DataContainer() {
        this(MAX_CELLS_IN_MEMORY);
    }
    
    /** 
     * Creates a new container with at most <code>maxCellsInMemory</code> 
     * cells that are kept in memory. If more cells are added, 
     * rows are buffered to disk.
     * @param maxCellsInMemory Maximum count of cells in memory before swapping.
     * @throws IllegalArgumentException If <code>maxCellsInMemory</code> &lt; 0.
     */
    public DataContainer(final int maxCellsInMemory) {
        if (maxCellsInMemory < 0) {
            throw new IllegalArgumentException(
                    "Cell count must be positive: " + maxCellsInMemory); 
        }
        m_maxCellsInMemory = maxCellsInMemory;
        m_maxPossibleValues = MAX_POSSIBLE_VALUES;
    }
    
    /** Get the number of possible values that are being kept.
     * @return This number.
     */
    public int getMaxPossibleValues() {
        return m_maxPossibleValues;
    }
    
    /** Define a new threshold for number of possible values to memorize.
     * It makes sense to call this method before any rows are added.
     * @param maxPossibleValues The new number.
     * @throws IllegalArgumentException If the value < 0
     */
    public void setMaxPossibleValues(final int maxPossibleValues) {
        if (maxPossibleValues < 0) {
            throw new IllegalArgumentException(
                    "number < 0: " + maxPossibleValues);
        }
        m_maxPossibleValues = maxPossibleValues;
    }
    
    /**
     * Returns <code>true</code> if the container has been initialized with 
     * <code>DataTableSpec</code> and is ready to accept rows.
     * 
     * <p>This is <code>true</code> after <code>open</code> an there was
     * no <code>close</code> invocation.
     * @return <code>true</code> if container is accepting rows.
     */
    public boolean isOpen() {
        return m_buffer != null && m_table == null;
    }

    /**
     * Opens the container so that rows can be added by
     * <code>addRowToTable(DataRow)</code>. The table spec of the resulting
     * table (the one being returned by <code>getTable()</code>) will have a
     * valid column domain. That means, while rows are added to the container,
     * the domain of each column is adjusted.
     * <p>
     * If you prefer to stick with the domain as passed in the argument, use the
     * method <code>open(DataTableSpec, true)</code> instead.
     * 
     * @param spec Table spec of the final table. Rows that are added to the
     *            container must comply with this spec.
     * @throws NullPointerException If <code>spec</code> is <code>null</code>.
     */
    public void open(final DataTableSpec spec) {
        open(spec, false);
    }

    /**
     * Opens the container so that rows can be added by 
     * <code>addRowToTable(DataRow)</code>. 
     * @param spec Table spec of the final table. Rows that are added to the
     *        container must comply with this spec.
     * @param initDomain if set to true, the column domains in the 
     *        container are initialized with the domains from spec. 
     * @throws NullPointerException If <code>spec</code> is <code>null</code>.
     */
    @SuppressWarnings ("unchecked")
    public void open(final DataTableSpec spec, final boolean initDomain) {
        if (spec == null) {
            throw new NullPointerException("Spec must not be null!");
        }
        DataTableSpec oldSpec = m_spec;
        m_spec = spec;
        m_keySet = new HashSet<RowKey>();
        if (m_buffer != null) {
            m_buffer.close(oldSpec);
        }
        
        // figure out for which columns it's worth to keep the list of possible
        // values and min/max ranges
        m_possibleValues = new LinkedHashSet[m_spec.getNumColumns()];
        m_minCells = new DataCell[m_spec.getNumColumns()];
        m_maxCells = new DataCell[m_spec.getNumColumns()];
        for (int i = 0; i < m_spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = m_spec.getColumnSpec(i);
            DataType colType = colSpec.getType();
            if (StringCell.TYPE.isASuperTypeOf(colType)) {
                if (initDomain) {
                    Set<DataCell> values = colSpec.getDomain().getValues();
                    m_possibleValues[i] = values != null 
                        ? new LinkedHashSet(values) : null;
                } else {
                    m_possibleValues[i] = new LinkedHashSet<DataCell>();
                }
            }
            if (DoubleCell.TYPE.isASuperTypeOf(colType)) {
                if (initDomain) {
                    m_minCells[i] = colSpec.getDomain().getLowerBound();
                    m_maxCells[i] = colSpec.getDomain().getUpperBound();
                } else {
                    m_minCells[i] = DataType.getMissingCell();
                    m_maxCells[i] = DataType.getMissingCell();
                }
            }
            
        }
        // how many rows will occupy MAX_CELLS_IN_MEMORY
        final int colCount = spec.getNumColumns();
        int rowsInMemory = m_maxCellsInMemory / ((colCount > 0) ? colCount : 1);
        m_buffer = new Buffer(rowsInMemory);
    }

    /**
     * Returns <code>true</code> if table has been closed and 
     * <code>getTable()</code> will return a <code>DataTable</code> object.
     * 
     * <p>This method does return <code>false</code> has not even been opened
     * yet.
     * @return <code>true</code> if table is available, <code>false</code>
     *         otherwise.
     */
    public boolean isClosed() {
        return m_table != null;
    }

    /**
     * Closes container and creates table that can be accessed by 
     * <code>getTable()</code>. Successive calls of <code>addRowToTable</code>
     * will fail with an exception.
     * @throws IllegalStateException If container is not open.
     */
    public void close() {
        if (isClosed()) {
            return;
        }
        if (!isOpen()) {
            throw new IllegalStateException("Cannot close table: container has"
                    + " not been initialized (opened).");
        }
        DataTableSpec finalSpec = createTableSpecWithRange();
        m_buffer.close(finalSpec);
        m_table = new BufferedTable(m_buffer);
        m_buffer = null;
        m_spec = null;
        m_keySet = null;
        m_possibleValues = null;
        m_minCells = null;
        m_maxCells = null;
    }
    
    /** Get the number of rows that have been added so far.
     * (How often has <code>addRowToTable</code> been called since the last
     * <code>openTable</code> request)
     * @return The number of rows in the container (independent of closed or
     *         not closed).
     * @throws IllegalStateException If container is not open.
     */
    public int size() {
        if (isClosed()) {
            return ((BufferedTable)m_table).getBuffer().size();
        }
        if (!isOpen()) {
            throw new IllegalStateException("Container is not open.");
        }
        return m_buffer.size();
    }

    /**
     * Get reference to table. This method throws an excpetion unless the 
     * container is closed and has therefore a table available.
     * @return Reference to the table that has been built up.
     * @throws IllegalStateException If <code>isClosed()</code> returns
     *         <code>false</code>
     */
    public DataTable getTable() {
        if (!isClosed()) {
            throw new IllegalStateException("Cannot get table: container is"
                    + " not closed.");
        }
        return m_table;
    }
    
    /** 
     * Get the currently set DataTableSpec.
     * @return The current spec.
     */
    public DataTableSpec getTableSpec() {
        if (isClosed()) {
            return m_table.getDataTableSpec();
        } else if (isOpen()) {
            return m_buffer.getTableSpec();
        }
        throw new IllegalStateException("Cannot get spec: container not open.");
    }

    /** 
     * @see RowAppender#addRowToTable(DataRow)
     */
    public void addRowToTable(final DataRow row) {
        if (!isOpen()) {
            throw new IllegalStateException("Cannot add row: container has"
                    + " not been initialized (opened).");
        }
        // let's do every possible sanity check
        int numCells = row.getNumCells();
        RowKey key = row.getKey();
        if (numCells != m_spec.getNumColumns()) {
            throw new IllegalArgumentException("Cell count in row \"" + key
                    + "\" is not equal to length of column names " + "array: "
                    + numCells + " vs. " + m_spec.getNumColumns());
        }
        for (int c = 0; c < numCells; c++) {
            DataType columnClass = m_spec.getColumnSpec(c).getType();
            DataCell value = row.getCell(c);
            DataType runtimeClass = value.getType();
            if (!columnClass.isASuperTypeOf(runtimeClass)) {
                throw new IllegalArgumentException("Runtime class of object \""
                        + value.toString() + "\" (index " + c
                        + ") in " + "row \"" + key + "\" is "
                        + runtimeClass.toString()
                        + " and does not comply with its supposed superclass "
                        + columnClass.toString());
            }
            // keep the list of possible values and the range updated
            updatePossibleValues(c, value);
            updateMinMax(c, value);
            
        } // for all cells
        if (m_keySet.contains(key)) {
            throw new IllegalArgumentException("Container contains already a"
                    + " row with key \"" + key + "\".");
        }

        // all test passed, add row
        m_keySet.add(key);
        m_buffer.addRow(row);
    } // addRowToTable(DataRow)
    
    /** Adds another value to the list of possible values in a certain column.
     * This method does nothing if the values don't need to be stored for the
     * respective column.
     * @param col The column of interest.
     * @param value The (maybe) new value.
     */
    private void updatePossibleValues(final int col, final DataCell value) {
        if (m_possibleValues[col] == null || value.isMissing()) {
            return;
        } 
        m_possibleValues[col].add(value);
        if (m_possibleValues[col].size() > m_maxPossibleValues) {
            // forget possible values
            m_possibleValues[col] = null;
        }
    }
    
    /** Updates the min and max value for an respective column. This method 
     * does nothing if the min and max values don't need to be stored, e.g.
     * the column at hand contains string values.
     * @param col The column of interest.
     * @param value The new value to check.
     */
    private void updateMinMax(final int col, final DataCell value) {
        if (m_minCells[col] == null || value.isMissing()) {
            return;
        }
        if (m_minCells[col].isMissing()) {
            assert (m_maxCells[col].isMissing());
            m_minCells[col] = value;
            m_maxCells[col] = value;
        } else {
            DataType colType = m_spec.getColumnSpec(col).getType();
            Comparator<DataCell> comparator = colType.getComparator();
            if (comparator.compare(value, m_minCells[col]) < 0) {
                m_minCells[col] = value;
            }
            if (comparator.compare(value, m_maxCells[col]) > 0) {
                m_maxCells[col] = value;
            }
        }
    }
    
    /** Creates the final data table spec. It also includes the column domain
     * information (if any)
     * @return The final data table spec to be used.
     */
    private DataTableSpec createTableSpecWithRange() {
        DataColumnSpec[] colSpec = new DataColumnSpec[m_spec.getNumColumns()];
        for (int i = 0; i < colSpec.length; i++) {
            DataColumnSpec original = m_spec.getColumnSpec(i);
            DataCell[] possVal = m_possibleValues[i] != null 
                ? m_possibleValues[i].toArray(new DataCell[0]) : null;
            DataCell min = m_minCells[i] != null && !m_minCells[i].isMissing()
                ? m_minCells[i] : null;
            DataCell max = m_maxCells[i] != null && !m_maxCells[i].isMissing()
                ? m_maxCells[i] : null;
            DataColumnDomainCreator domainCreator = 
                new DataColumnDomainCreator(possVal, min, max);
            DataColumnSpecCreator specCreator = 
                new DataColumnSpecCreator(original);
            specCreator.setDomain(domainCreator.createDomain());
            colSpec[i] = specCreator.createSpec();
        }
        return new DataTableSpec(colSpec);
    }
    
    /** Convenience method that will buffer the entire argument table. This is
     * usefull if you have a wrapper table at hand and want to make sure that 
     * all calculations are done here 
     * @param table The table to cache.
     * @param exec The execution monitor to report progress to and to check
     * for the cancel status.
     * @param maxCellsInMemory The number of cells to be kept in memory before
     * swapping to disk.
     * @return A cache table containing the data from the argument.
     * @throws NullPointerException If the argument is <code>null</code>.
     * @throws CanceledExecutionException If the process has been canceled.
     */
    public static DataTable cache(final DataTable table, 
            final ExecutionMonitor exec, final int maxCellsInMemory) 
        throws CanceledExecutionException {
        DataContainer buf = new DataContainer(maxCellsInMemory);
        buf.open(table.getDataTableSpec());
        int row = 0;
        try {
            for (RowIterator it = table.iterator(); it.hasNext(); row++) {
                DataRow next = it.next();
                exec.setMessage("Caching row " + row + " (\"" 
                        + next.getKey() + "\")");
                exec.checkCanceled();
                buf.addRowToTable(next);
            }
        } finally {
            buf.close();
        }
        return buf.getTable();
    }
    
    /** Convenience method that will buffer the entire argument table. This is
     * usefull if you have a wrapper table at hand and want to make sure that 
     * all calculations are done here 
     * @param table The table to cache.
     * @param exec The execution monitor to report progress to and to check
     * for the cancel status.
     * @return A cache table containing the data from the argument.
     * @throws NullPointerException If the argument is <code>null</code>.
     * @throws CanceledExecutionException If the process has been canceled.
     */
    public static DataTable cache(final DataTable table, 
            final ExecutionMonitor exec) throws CanceledExecutionException {
        return cache(table, exec, MAX_CELLS_IN_MEMORY);
    }
    
    /** Writes a given DataTable permanently to a zip file. 
     * @param table The table to write.
     * @param outFile The file to write to. Will be created or overwritten.
     * @param exec For progress info.
     * @throws IOException If writing fails.
     * @throws CanceledExecutionException If canceled.
     */
    public static void writeToZip(final DataTable table, final File outFile,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        Buffer buf = new Buffer(outFile);
        int rowCount = 0;
        try {
            for (DataRow row : table) {
                rowCount++;
                exec.setMessage("Writing row " + rowCount + " (\"" 
                        + row.getKey() + "\")");
                exec.checkCanceled();
                buf.addRow(row);
            }
        } finally {
            buf.close(table.getDataTableSpec());
        }
    }
    
    /** 
     * Reads a table from a zip file that has been written using the 
     * <code>writeToZip</code> method.
     * @param zipFile To read from.
     * @return The table contained in the zip file.
     * @throws IOException If that fails.
     */
    public static DataTable readFromZip(final File zipFile) throws IOException {
        Buffer buffer = new Buffer(zipFile, /*ignore*/false);
        return new BufferedTable(buffer);
    }
}

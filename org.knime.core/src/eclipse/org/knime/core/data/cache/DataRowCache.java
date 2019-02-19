/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   31 Jan 2019 (albrecht): created
 */
package org.knime.core.data.cache;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowIteratorBuilder;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 *
 * @author Christian Albrecht, Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 3.8
 */
public class DataRowCache {

    /** Possible hilite filter incarnations. */
    public static enum TableContentFilter {
        /** Show all available rows (default). */
        All,
        /** Show only rows, which are currently hilited. */
        HiliteOnly,
        /** Show only rows, which are currently not hilited. */
        UnHiliteOnly;

        /** @param isHilit If row is hilit.
         * @return whether the row with the given hilite status is to be
         * filtered out.
         */
        public boolean matches(final boolean isHilit) {
            switch (this) {
            case All: return true;
            case HiliteOnly: return isHilit;
            case UnHiliteOnly: return !isHilit;
            default: throw new InternalError("Unknown constant: " + this);
            }
        }

        /** @return Whether filter is limiting the scope. */
        public boolean performsFiltering() {
            switch (this) {
            case All: return false;
            default: return true;
            }
        }
    }

    /**
     * Default size of the ring buffer (500).
     */
    public static final int CACHE_SIZE = 500;

    /**
     * Number of rows being read at a time (50). Once a "new" row is accessed,
     * the system will use the {@link org.knime.core.data.DataTable}'s
     * iterator to get <code>CHUNK_SIZE</code> new rows which are added to the
     * cache.
     * Suppose you have a table with more than 100 rows: Those are then accessed
     */
    public static final int CHUNK_SIZE = 50;

    /** underlying data; may be null to indicate invalid status. */
    private DataTable m_data;

    /** Iterator in {@link #m_data} to get content, newly instantiated when user
     * scrolls up. */
    private RowIterator m_iterator;

    /**
     * Ring buffer. Size determined by {@link #CACHE_SIZE}.
     */
    private DataRow[] m_cachedRows;

    /** storing for each row in the cache if it has been hilited,
     * using the same ring buffer strategy as {@link #m_cachedRows}. */
    private BitSet m_hilitSet;

    /**
     * Number of rows seen in current iterator that are of interest, i.e.
     * hilited rows when only hilited rows should be shown, all
     * rows otherwise. This field is set to 0 when a new iterator is
     * instantiated.
     */
    private int m_rowCountOfInterestInIterator;

    /**
     * Number of rows of interest that have been seen so far. If
     * only hilited rows should be shown this field is equal to
     * {@link #m_maxRowCount}.
     */
    private int m_rowCountOfInterest;

    /**
     * Is the current value of {@link #m_rowCountOfInterest} final? If
     * only hilited rows should be shown this field
     * is equal to {@link #m_isMaxRowCountFinal}.
     */
    private boolean m_isRowCountOfInterestFinal;

    /** Counter of rows in current iterator. If only hilited rows should be
     * shown, this field is equal to
     * {@link #m_rowCountOfInterestInIterator}. This field is
     * incremented with each <code>m_iterator.next()</code> and reset to 0 with
     * <code>m_iterator = new ...</code>
     */
    private int m_rowCountInIterator;

    /** lower bound for overall number of rows in {@link #m_data}, updated when
     * new rows are encountered.
     */
    private int m_maxRowCount;

    /** flag if all rows in the underlying DataTable have been seen,
     * i.e. the iterator ran at least once to the very end of the table.
     * This field is set to <code>true</code> the first time
     * <code>m_iterator.atEnd()</code> returns <code>true</code>.
     */
    private boolean m_isMaxRowCountFinal;

    /** Policy as to which rows to show. */
    private TableContentFilter m_tableFilter = TableContentFilter.All;

    /** number of rows that are read at a time, defaults to
     * {@link #CHUNK_SIZE}. */
    private int m_chunkSize;

    /** size of ring buffer, if <code>m_cachedRows.length == m_cacheSize</code>
     * (if m_cachedRows not <code>null</code>). */
    private int m_cacheSize;

    /** The original table that was set by the user. It may not be displayed
     * if the user chose to sort the table in the view (m_data is then the
     * sorted table). */
    private DataTable m_originalUnsortedTable;

    private Set<String> m_includedColumnIndices;

    /**
     * Creates a new DataRowCache instance with empty content. Call
     * {@link #setDataTable(DataTable, ExecutionMonitor)} to set a valid data table. No
     * HiLiting is available.
     *
     * @see #setDataTable(DataTable, ExecutionMonitor)
     * @see #setHiLiteHandler(HiLiteHandler)
     */
    public DataRowCache() {
        m_maxRowCount = 0;
        m_isMaxRowCountFinal = true; // no data seen, assume final row count
        m_chunkSize = CHUNK_SIZE;
        m_cacheSize = CACHE_SIZE;
    }

    /**
     * Creates a new DataRowCache instance displaying <code>table</code>.
     * No HiLiting is available.
     *
     * @param table the table to be displayed. May be <code>null</code> to indicate that there is no data available.
     * @param exec an {@link ExecutionMonitor} to track progress or check for cancellation
     */
    public DataRowCache(final DataTable table, final ExecutionMonitor exec) {
        this();
        setDataTable(table, exec);
    }

    /**
     * Sets new data for this table. The argument may be <code>null</code> to
     * indicate invalid data (nothing displayed).
     *
     * @param data the new data being displayed or <code>null</code>
     * @param exec an {@link ExecutionMonitor} to track progress or check for cancellation
     */
    public void setDataTable(final DataTable data, final ExecutionMonitor exec) {
        setDataTableIntern(data, data, exec);
    }

    /**
     * Sets new data for this table. The argument may be <code>null</code> to indicate invalid data (nothing displayed).
     * Additionally an array of column indices to be included in the cache may be given to take advantage of faster
     * data access through columnar storage.
     *
     * @param data the new data being displayed or <code>null</code>
     * @param exec an {@link ExecutionMonitor} to track progress or check for cancellation
     * @param includedColumns optional column names to be used by the cache, if no column names are given all
     * columns are included. Accessing columns which are not included here may lead to an exception.
     */
    public void setDataTable(final DataTable data, final ExecutionMonitor exec, final String... includedColumns) {
        setIncludedColumns(includedColumns);
        setDataTableIntern(data, data, exec);
    }

    /**
     * Sets an array of column names of which columns from the underlying {@link DataTable} are of interest for the
     * purpose of this cache. By default all columns are included. Accessing cells from columns excluded after calling
     * this method may lead to an exception.
     *
     * @param includedColumns the column names of the columns to include in the row cache
     */
    public void setIncludedColumns(final String... includedColumns) {
        if (includedColumns == null || includedColumns.length <= 0) {
            return;
        }
        Set<String> colSet = Arrays.stream(includedColumns).collect(Collectors.toSet());
        if (!colSet.equals(m_includedColumnIndices)) {
            clearCache();
            m_includedColumnIndices = colSet;
        }
    }

    /**
     * Sets new data for this table. The table argument may be
     * <code>null</code> to indicate invalid data (nothing displayed).
     */
    private void setDataTableIntern(final DataTable originalData,
            final DataTable data, final ExecutionMonitor exec) {
        if (m_data == data) {
            return;  // do not start event storm
        }
        //TODO: fill
        boolean clearOldTable = false/*m_tableSortOrder != null*/;
        //TODO: enable
        //cancelRowCountingInBackground();
        int oldColCount = getColumnCount();
        int newColCount =
            data != null ? data.getDataTableSpec().getNumColumns() : 0;
        int oldRowCount = getRowCount();
        DataTable oldData = m_data;
        m_originalUnsortedTable = originalData;
        m_data = data;
        m_cachedRows = null;
        m_hilitSet = null;
        if (m_iterator instanceof CloseableRowIterator) {
            ((CloseableRowIterator)m_iterator).close();
        }
        m_iterator = null;
        m_rowCountOfInterestInIterator = 0;
        m_rowCountOfInterest = 0;
        m_maxRowCount = 0;
        //TODO: enable
        //cancelRowCountingInBackground();
        m_isMaxRowCountFinal = true;
        m_isRowCountOfInterestFinal = true;
        boolean structureChanged = oldColCount != newColCount;
        if (oldColCount == newColCount) {
            if (oldRowCount > 0) {
                //TODO: enable
                //fireTableRowsDeleted(0, oldRowCount - 1);
            }
            if (newColCount > 0) {
                structureChanged = !data.getDataTableSpec().equalStructure(
                        oldData.getDataTableSpec());
            }
        }
        if (data != null) { // new data available, release old stuff
            // assume that there are rows, may change in cacheNextRow() below
            m_isMaxRowCountFinal = false;
            m_isRowCountOfInterestFinal = false;
            final long rowCountFromTable;
            if (data instanceof BufferedDataTable) {
                rowCountFromTable = ((BufferedDataTable)data).size();
            } else if (data instanceof ContainerTable) {
                rowCountFromTable = ((ContainerTable)data).size();
            } else {
                rowCountFromTable = -1; // unknown
            }
            if (rowCountFromTable >= 0) {
                m_isMaxRowCountFinal = true;
                if (rowCountFromTable > Integer.MAX_VALUE) {
                    NodeLogger.getLogger(getClass()).warn("Table view will show only the first " + Integer.MAX_VALUE
                        + " rows of " + rowCountFromTable + ".");
                    m_maxRowCount = Integer.MAX_VALUE;
                } else {
                    m_maxRowCount = (int) rowCountFromTable;
                }
                if (!m_tableFilter.performsFiltering()) {
                    m_rowCountOfInterest = m_maxRowCount;
                    m_isRowCountOfInterestFinal = true;
                }
            }

            int cacheSize = getCacheSize();
            m_cachedRows = new DataRow[cacheSize];
            m_hilitSet = new BitSet(cacheSize);
            clearCache();  // will instantiate a new iterator.
            // will also set m_isRowCountOfInterestFinal etc. accordingly
            cacheNextRow(exec);
        }
        if (structureChanged) {
            // notify listeners
            //TODO: enable
            //fireTableStructureChanged();
        } else {
            int newRowCount = getRowCount();
            if (newRowCount > 0) {
                //TODO:enable
                //fireTableRowsInserted(0, newRowCount);
            }
        }
        //TODO: enable
        //m_propertySupport.firePropertyChange(PROPERTY_DATA, oldData, m_data);
        if (clearOldTable && oldData instanceof ContainerTable) {
            ((ContainerTable)oldData).clear();
        }
    }

    /**
     * Is there valid data to show?
     *
     * @return <code>true</code> if underlying <code>DataTable</code> is not
     * <code>null</code>
     */
    public final boolean hasData() {
        return m_data != null;
    }

    /** Get the name of the current data table (if any) or <code>null</code>.
     * @return The table name or <code>null</code>.
     */
    public String getTableName() {
        return hasData() ? getDataTableSpec().getName() : null;
    }

    /**
     * Get the column count.
     *
     * @return the number of columns in the underlying <code>DataTable</code>
     *         or 0 if {@link #hasData()} returns <code>false</code>.
     */
    public int getColumnCount() {
        return hasData() ? m_data.getDataTableSpec().getNumColumns() : 0;
    } // getColumnCount()

    /**
     * Get the number of rows in this model. This number may be less than
     * the actual row count in the underlying <code>DataTable</code>. (Note,
     * the final size of a DataTable is not necessarily known in advance.) The
     * row count is updated each time a new row is requested. The return value
     * is:
     * <ul>
     * <li> The correct row count of the <code>DataTable</code>, if the table
     *      was traversed completely at least once.
     * <li> The rows that have been seen (returned by iterator) plus 1 (to
     *      indicate that there are more rows to come), if the table has not
     *      been traversed completely.
     * </ul>
     * Keep in mind that this call may not return the same value for two
     * successive calls as it is possible that new rows have been seen in the
     * meantime. As new rows are seen an event is fired.
     *
     * @return a lower bound for the number of rows in this model
     */
    public int getRowCount() {
        return m_rowCountOfInterest;
    } // getRowCount()

    /**
     * Get the DataCell at a specific location. If the requested row is in the
     * cache, it will be returned. Otherwise the ring buffer is updated
     * (iterator pushed forward or reset) until the row is in the cache.
     *
     * @param row the row index
     * @param column the column index
     * @return the <code>DataCell</code> in the underlying
     *         <code>DataTable</code> at position [<code>row, column</code>]
     * @throws IndexOutOfBoundsException if either argument violates its range
     */
    public DataCell getValueAt(final int row, final int column, final ExecutionMonitor exec)
            throws IndexOutOfBoundsException, CanceledExecutionException {
        boundColumn(column);
        DataRow result = getRow(row, exec);
        return result.getCell(column);
    } // getValueAt(int, int)

    /**
     * @param row
     * @return
     */
    public RowKey getRowKey(final int row, final ExecutionMonitor exec)
            throws IndexOutOfBoundsException, CanceledExecutionException {
        DataRow result = getRow(row, exec);
        return result.getKey();
    }

    /**
     * Get the column header for some specific column index. The final column
     * name is generated by using the column header's <code>DataCell</code>
     * <code>toString()</code> method.
     *
     * @param colIndex the column's index
     * @return header of column <code>colIndex</code>
     * @see DataColumnSpec#getName()
     * @throws IndexOutOfBoundsException if <code>colIndex</code> out of range
     */
    public String getColumnName(final int colIndex) {
        boundColumn(colIndex);
        DataColumnSpec colSpec =
            m_data.getDataTableSpec().getColumnSpec(colIndex);
        return colSpec.getName().toString();
    } // getColumnName(int)

    /**
     * Returns DataCell.class.
     * @param column
     * @return
     */
    public Class<DataCell> getColumnClass(final int column) {
        boundColumn(column);
        return DataCell.class;
    } // getColumnClass(int)

    /**
     * Get reference to underlying <code>DataTable</code> as it was passed
     * in the constructor or changed by successive calls of
     * {@link #setDataTable(DataTable)}.
     *
     * @return reference to table or <code>null</code> if {@link #hasData()}
     *         returns <code>false</code>
     * @see #TableContentModel(DataTable)
     */
    public final DataTable getDataTable() {
        return m_originalUnsortedTable;
    } // getDataTable()

    /**
     * Returns <code>true</code> if the iterator has traversed the whole
     * <code>DataTable</code> at least once. The value returned by
     * {@link #getRowCount()} is therefore final. It returns
     * <code>false</code> if the value can still increase.
     *
     * <p>Note: The row count may not increase even if
     * {@link #isRowCountFinal()} returns <code>false</code>. This unlikely
     * case occurs when - by chance - <code>getRowCount() + 1</code> is indeed
     * the final row count. See the {@link #getRowCount()} method
     * for further details.</p>
     *
     * @return if there are no more unknown rows
     */
    public boolean isRowCountFinal() {
        if (m_tableFilter.performsFiltering()) {
            return m_isRowCountOfInterestFinal;
        }
        return m_isMaxRowCountFinal;
    } // isRowCountFinal()

    /**
     * Changes the size of the ring buffer so that <code>size</code> rows
     * can be cached. This method will clear the current cache. The cache should
     * have at least a size so that all rows that are displayed at a time can
     * be buffered (however, no further checking is carried out).
     *
     * <p>
     * If <code>size</code> is less than twice the chunk size, i.e.
     * <pre><code>size &lt; 2*getChunkSize()</code></pre>, it will be set to it.
     * So when a "new" row is requested, at least <code>getChunkSize()</code>
     * rows after (including that row) and before the new row will be also in
     * the cache.
     *
     * @param size size of the new cache.
     * @return the new cache size (may differ from <code>size</code>, see above)
     * @throws IllegalArgumentException if <code>size</code> <= 0.
     */
    protected final int setCacheSize(final int size) {
        if (size == getCacheSize()) { // cool, nothing changed
            return size;
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must not <= 0: " + size);
        }
        m_cacheSize = Math.max(2 * getChunkSize(), size);
        m_cachedRows = new DataRow[m_cacheSize];
        m_hilitSet = new BitSet();
        clearCache();
        return m_cacheSize;
    } // setCacheSize(int)

    /**
     * Get the size of the cache.
     *
     * @return number of rows that fit in cache
     * @see #CACHE_SIZE
     */
    public final int getCacheSize() {
        return m_cacheSize;
    } // getCacheSize()

    /**
     * Set a new chunk size. This value is defaulted to {@link #CHUNK_SIZE}
     * on start-up. The new value can be at most <code>getCacheSize()/2</code>.
     * If it is bigger, it's value is reduced to <code>getCacheSize()/2</code>.
     *
     * @param newSize the new value
     * @return the new chunk size (may differ from passed argument, see above)
     * @throws IllegalArgumentException if <code>newSize</code> is &lt;= 0.
     * @see #getCacheSize()
     */
    protected final int setChunkSize(final int newSize) {
        if (newSize <= 0) {
            throw new IllegalArgumentException(
                "Chunks must not be <= 0: " + newSize);
        }
        // avoid too small chunks, round up when odd chunk size
        m_chunkSize = Math.min(newSize, (getCacheSize() + 1) / 2);
        return m_chunkSize;
    } // setChunkSize(int)

    /**
     * Get the chunk size.
     *
     * @return The current value of a chunk being read (default:
     *         {@link #CHUNK_SIZE}.
     */
    public final int getChunkSize() {
        return m_chunkSize;
    } // getChunkSize()

    /**
     * Get the table spec of the current DataTable. It returns <code>null</code>
     * if the model is currently not having a table.
     *
     * @return The spec of the DataTable.
     */
    public DataTableSpec getDataTableSpec() {
        if (hasData()) {
            return getDataTable().getDataTableSpec();
        }
        return null;
    }

    /**
     * Get the color information for a row.
     *
     * @param row The row of interest
     * @return The color information for that row
     */
    public ColorAttr getColorAttr(final int row, final ExecutionMonitor exec)
            throws IndexOutOfBoundsException, CanceledExecutionException {
        // makes also sure row is cached
        DataRow r = getRow(row, exec);
        assert (row >= (m_rowCountOfInterestInIterator - getCacheSize())
                && row < m_rowCountOfInterestInIterator);
        return m_data.getDataTableSpec().getRowColor(r);
    }

    /**
     * Gets a row with a specified index. Either the row is in the cache (just
     * take it) or the row must be got from the DataTable (uses iterator and
     * puts row into cache).
     *
     * @param row row index of interest
     * @param exec
     * @return the row at a specific position
     * @throws IndexOutOfBoundsException if <code>row</code> violates its range
     * @throws CanceledExecutionException
     */
    public DataRow getRow(final int row, final ExecutionMonitor exec)
            throws IndexOutOfBoundsException, CanceledExecutionException {
        boundRow(row);
        final int cacheSize = getCacheSize();
        final int oldRowCount = getRowCount();

        // the iterator goes further when the last known row is requested
        boolean pushIterator = !isRowCountFinal() && (row == oldRowCount - 1);
        if (row >= (m_rowCountOfInterestInIterator - cacheSize)
                && (row < m_rowCountOfInterestInIterator) && !pushIterator) {
            return getRowFromCache(row);
        }

        /* row is not in cache */
        // row already released from cache
        if (row < (m_rowCountOfInterestInIterator - cacheSize)) {
            // clear cache, init new iterator
            clearCache();
        }
        assert (row >= m_rowCountOfInterestInIterator - 1);

        boolean wasRowCountFinal = isRowCountFinal();

        // push iterator forward to index row+m_chunkSize (ensures when
        // getRow(row+1) is called the iterator is NOT used again)
        double rowDiffInitial = Math.max(1, row - m_rowCountOfInterestInIterator + m_chunkSize);
        int newRowsCached = 0;
        boolean mayHaveNext;
        do {
            // changes also m_rowCountOfInterestInIterator
            mayHaveNext = cacheNextRow(exec);
            if (exec != null) {
                exec.checkCanceled();
                exec.setProgress(++newRowsCached / rowDiffInitial);
            }
        } while ((m_rowCountOfInterestInIterator - 1) != (row + m_chunkSize)
                && mayHaveNext);
        // is it the first time that we see the last row? (fire event)
        boolean isFinalSwap = !wasRowCountFinal && !mayHaveNext;
        // block contains rows that we haven't seen before
        if (isFinalSwap || (m_rowCountOfInterestInIterator > oldRowCount)) {
            if (isFinalSwap) {
                boundRow(row); // row count is final, we have an upper bound!
            }
            //TODO: enable
            //fireTableRowsInserted(oldRowCount, getRowCount() - 1);
        }
        if (exec != null) {
            exec.setProgress(1.0);
        }
        return getRowFromCache(row);
    } // getRow(int)

    /**
     * Pushes iterator one step further and caches the next element at the
     * proper position in the ring buffer.
     *
     * @return <code>true</code> if that was successful, <code>false</code>
     *         if the iterator went to the end.
     */
    private boolean cacheNextRow(final ExecutionMonitor exec) {
        assert (hasData());
        DataRow currentRow;
        boolean isHiLit;
        do {
            if (!m_iterator.hasNext()) {
                // set to false with new data
                m_isMaxRowCountFinal = true;
                // set to false with new highlight event or new data
                m_isRowCountOfInterestFinal = true;
                return false;
            }
            currentRow = m_iterator.next();
            m_rowCountInIterator++;
            m_maxRowCount = Math.max(m_maxRowCount, m_rowCountInIterator);
            // TODO: enable this for selected only...
            isHiLit = /* m_hiLiteHdl != null
                ? m_hiLiteHdl.isHiLit(currentRow.getKey()) : */ false;
            // ignore row if we filter for hilit rows and this one is not hilit
        } while (!m_tableFilter.matches(isHiLit));
        // index of row in cache
        int indexInCache = m_rowCountOfInterestInIterator % getCacheSize();
        m_cachedRows[indexInCache] = currentRow;
        m_hilitSet.set(indexInCache, isHiLit);
        m_rowCountOfInterestInIterator++;
        m_rowCountOfInterest =
            Math.max(m_rowCountOfInterest, m_rowCountOfInterestInIterator);
        return true;
    } // cacheNextRow()

    /** Get new iterator, only to be called when data is set. Gets an
     * {@link BufferedDataTable#iteratorFailProve() fail prove iterator} if
     * the table is an instance of {@link BufferedDataTable}. */
    private RowIterator getNewDataIterator() {
        assert hasData();
        // TODO: find a different solution for the Swing table view, can't use the fail prove iterator with the iterator
        // builder
        /* if (m_data instanceof BufferedDataTable) {
            return ((BufferedDataTable)m_data).iteratorFailProve();
        } */

        RowIteratorBuilder<? extends RowIterator> iteratorBuilder = m_data.iteratorBuilder();
        if (m_includedColumnIndices != null) {
            iteratorBuilder.filterColumns(m_includedColumnIndices.stream().toArray(String[]::new));
        }
        return iteratorBuilder.build();
    }

    /**
     * Clears cache, instantiates new Iterator.
     */
    protected void clearCache() {
        if (!hasData()) {
            return;
        }
        if (m_iterator instanceof CloseableRowIterator) {
            ((CloseableRowIterator)m_iterator).close();
        }
        m_iterator = getNewDataIterator();
        m_rowCountInIterator = 0;
        // all updated in nextBlock()
        m_rowCountOfInterestInIterator = 0;
        // clear cache
        Arrays.fill(m_cachedRows, null);
        m_hilitSet.clear();
    } // clearCache()

    /**
     * Returns a row with a given index from the cache. It is mandatory to give
     * a row index which is certainly in the cache, i.e.
     * <code>(row >= (m_rowCountOfInterestInIterator - cS)
     * && row < m_rowCountOfInterestInIterator)</code> must hold.
     *
     * @param  row index of the row in the underlying <code>DataTable</code>
     * @return the row with the given index
     */
    private DataRow getRowFromCache(final int row) {
        return m_cachedRows[indexForRow(row)];
    } // getRowFromCache(int)

    /**
     * Get the index in the cache where row with index "row" is located.
     * The row MUST be in the cache.
     *
     * @param row row to access
     * @return internal index of the row
     */
    private int indexForRow(final int row) {
        final int cS = getCacheSize();
        assert (row >= (m_rowCountOfInterestInIterator - cS)
                && row < m_rowCountOfInterestInIterator) : "Row is not cached";
        // index of row in ring buffer
        int indexInCache = (row % cS);
        return indexInCache;
    } // indexForRow(int)

    /**
     * Counterpart for {@link #indexForRow(int)}.
     *
     * @param index an index in the cache.
     * @return the row id for <code>index</code>
     */
    private int rowForIndex(final int index) {
        final int cS = getCacheSize();
        final int lastRowIndex = ((m_rowCountOfInterestInIterator - 1) % cS);
        if (index <= lastRowIndex) {
            return m_rowCountOfInterestInIterator - 1 - (lastRowIndex - index);
        }
        return m_rowCountOfInterestInIterator - 1 - (lastRowIndex + cS - index);
    } // rowForIndex(int)

    /** @return index in the cache hosting the first row in the
     *  table that's cached or -1 if none is cached */
    private int firstRowCached() {
        final int lastRow = lastRowCached();
        if (lastRow < 0) {
            return -1;
        }
        final int cS = getCacheSize();
        int next = (lastRow + 1) % cS;
        return m_cachedRows[next] != null ? next : 0;
    }

    /** @return index in cache hosting the last row in the table that's
     *  cached or -1 if none is cached. */
    private int lastRowCached() {
        final int cS = getCacheSize();
        return m_rowCountOfInterestInIterator >= 0
            ? (m_rowCountOfInterestInIterator - 1) % cS : -1;
    }

    /**
     * Used by the row counter thread to inform about new rows. An event will
     * be fired to inform registered listeners.
     *
     * @param newCount the new row count
     * @param isFinal if there are possibly more rows to count
     */
    synchronized void setRowCount(final int newCount, final boolean isFinal) {
        final int oldCount = m_maxRowCount;
        if (oldCount >= newCount) {
            return;
        }
        m_isMaxRowCountFinal = isFinal;
        m_maxRowCount = newCount;
        if (!m_tableFilter.performsFiltering()) {
            m_rowCountOfInterest = m_maxRowCount;
            m_isRowCountOfInterestFinal = isFinal;
        }
        //TODO: enable
        /* ViewUtils.invokeLaterInEDT(new Runnable() {
            @Override
            public void run() {
                fireTableRowsInserted(oldCount, newCount - 1);
            }
        }); */
    }

    /**
     * Checks if given argument is in range and throws an exception
     * if it is not.
     *
     * @param rowIndex row index to check
     * @throws IndexOutOfBoundsException if argument violates range
     */
    protected final void boundRow(final int rowIndex) {
        if (rowIndex < 0) {
            throw new IndexOutOfBoundsException(
                "Row index must not be < 0: " + rowIndex);
        }
        // reasonable check only possible when table was seen completely,
        // method is called again when m_isMaxRowCountFinal switches to true
        if (isRowCountFinal() && (rowIndex >= getRowCount())) {
            throw new IndexOutOfBoundsException(
                "Row index " + rowIndex + " invalid: "
                + rowIndex + " >= " +  m_maxRowCount);
        }
    } // boundRow(int)

    /**
     * Checks if given argument is in range and throws an exception
     * if it is not.
     *
     * @param columnIndex column index to check
     * @throws IndexOutOfBoundsException if argument violates range
     */
    protected final void boundColumn(final int columnIndex) {
        if (columnIndex < 0) {
            throw new IndexOutOfBoundsException(
                "Column index must not be < 0: " + columnIndex);
        }
        if (columnIndex >= getColumnCount()) {
            throw new IndexOutOfBoundsException(
                "Column index must not be >= "
                    + getColumnCount()
                    + ": "
                    + columnIndex);
        }
    } // boundColumn(int)

}

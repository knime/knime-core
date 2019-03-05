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
 *   12 Feb 2019 (albrecht): created
 */
package org.knime.core.data.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DirectAccessTable;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowIteratorBuilder;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * Wrapper around a {@link DataTable} which supports caching a window of contiguous {@link DataRow}s in a ring buffer.
 * This functionality is mainly needed for table views which support scrolling or paging so that rows in the area of
 * currently visible rows can be accessed quickly.
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @since 3.8
 *
 * @noreference This class is not intended to be referenced by clients. Pending API
 * @noextend This class is not intended to be subclassed by clients. Pending API
 * @noinstantiate This class is not intended to be instantiated by clients. Pending API
 */
public class WindowCacheTable implements DirectAccessTable {

    /** Default size of the ring buffer (500). */
    public static final int DEFAULT_CACHE_SIZE = 500;

    /** Default number of rows being read at a time (50) ahead of the requested row. Once a "new" row is accessed,
     * the system will use the {@link org.knime.core.data.DataTable}'s iterator to get <code>DEFAULT_LOOK_AHEAD</code>
     * new rows which are added to the cache.
     */
    public static final int DEFAULT_LOOK_AHEAD = 50;

    /** Underlying iterable data table. May be null to indicate invalid status. */
    private final DataTable m_table;

    /** Iterator in {@link #m_table} to get content, newly instantiated when rows before cache are requested. */
    private RowIterator m_tableIterator;

    /**
     * Ring buffer. Size determined by {@link #DEFAULT_CACHE_SIZE}.
     */
    private DataRow[] m_cachedRows;

    /**
     * Number of rows seen in current iterator that are of interest, i.e. hilited rows when only hilited rows should be
     * shown, all rows otherwise. This field is set to 0 when a new iterator is instantiated.
     */
    private long m_rowCountOfInterestInIterator;

    /**
     * Number of rows of interest that have been seen so far. If only hilited rows should be shown this field is equal
     * to {@link #m_maxRowCount}.
     */
    private long m_rowCountOfInterest;

    /** Counter of rows in current iterator. If only hilited rows should be shown, this field is equal to
     * {@link #m_rowCountOfInterestInIterator}. This field is incremented with each <code>m_iterator.next()</code>
     * and reset to 0 with <code>m_iterator = new ...</code>
     */
    private long m_rowCountInIterator;

    /** Lower bound for overall number of rows in {@link #m_data}, updated when new rows are encountered.
     */
    private long m_maxRowCount;

    /** Flag if all rows in the underlying DataTable have been seen, i.e. the iterator ran at least once to the very
     * end of the table. This field is set to <code>true</code> the first time <code>m_iterator.atEnd()</code>
     * returns <code>true</code>.
     */
    private boolean m_isMaxRowCountFinal;

    /** Number of rows that are read ahead of the last requested row, defaults to {@link #DEFAULT_CHUNK_SIZE}. */
    private int m_lookAheadSize;

    /** Total size of ring buffer, if <code>m_cachedRows.length == m_cacheSize</code>
     * (if m_cachedRows not <code>null</code>). */
    private int m_cacheSize;

    /** Set of column names to be included in the returned rows. Accessing columns outside of this set may cause
     * exceptions to be thrown. An empty or uninitialized set indicates that all columns should be accessible. */
    private Set<String> m_includedColumnIndices;

    private WindowCacheTable(final DataTable table, final boolean init) {
        m_maxRowCount = 0;
        m_isMaxRowCountFinal = true; // no data seen, assume final row count
        m_lookAheadSize = DEFAULT_LOOK_AHEAD;
        m_cacheSize = DEFAULT_CACHE_SIZE;
        m_table = table;
        if (init) {
            initCache();
        }
    }

    /**
     * @param table
     */
    public WindowCacheTable(final DataTable table) {
        this(table, true);
    }

    /**
     * @param table
     * @param includedColumns
     */
    public WindowCacheTable(final DataTable table, final String... includedColumns) {
        this(table, false);
        setIncludedColumns(includedColumns);
        initCache();
    }

    public Set<String> getIncludedColumns() {
        return m_includedColumnIndices;
    }

    /**
     * Sets an array of column names of which columns from the underlying {@link DataTable} are of interest for the
     * purpose of this cache. By default all columns are included. Accessing cells from columns excluded after calling
     * this method may lead to an exception.
     *
     * @param includedColumns the column names of the columns to include in the row cache
     */
    private void setIncludedColumns(final String... includedColumns) {
        if (includedColumns == null || includedColumns.length <= 0) {
            return;
        }
        Set<String> colSet = Arrays.stream(includedColumns).collect(Collectors.toSet());
        if (!colSet.equals(m_includedColumnIndices)) {
            m_includedColumnIndices = colSet;
        }
    }

    private void initCache() {
        m_cachedRows = null;
        m_tableIterator = null;
        m_rowCountOfInterestInIterator = 0;
        m_rowCountOfInterest = 0;
        m_maxRowCount = 0;
        m_isMaxRowCountFinal = true;
        if (m_table != null) {
            // assume that there are rows, may change in cacheNextRow() below
            m_isMaxRowCountFinal = false;
            final long rowCountFromTable;
            if (m_table instanceof BufferedDataTable) {
                rowCountFromTable = ((BufferedDataTable)m_table).size();
            } else if (m_table instanceof ContainerTable) {
                rowCountFromTable = ((ContainerTable)m_table).size();
            } else {
                rowCountFromTable = -1; // unknown
            }
            if (rowCountFromTable >= 0) {
                m_isMaxRowCountFinal = true;
                m_maxRowCount = rowCountFromTable;
                m_rowCountOfInterest = m_maxRowCount;
            }
            int cacheSize = getCacheSize();
            m_cachedRows = new DataRow[cacheSize];
            clearCacheAndInitIterator();  // will instantiate a new iterator.
            // will also set m_isRowCountOfInterestFinal etc. accordingly
            cacheNextRow();
        }
    }

    /**
     * Is there valid data to show?
     *
     * @return <code>true</code> if underlying <code>DataTable</code> is not <code>null</code>
     */
    public final boolean hasData() {
        return m_table != null;
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
        return hasData() ? m_table.getDataTableSpec().getNumColumns() : 0;
    }

    /**
     * {@inheritDoc}
     * Returns null when {@link #hasData()} is false.
     */
    @Override
    public final DataTableSpec getDataTableSpec() {
        return hasData() ? m_table.getDataTableSpec() : null;
    }

    /**
     * Get reference to underlying <code>DataTable</code> as it was passed in the constructor
     *
     * @return reference to table or <code>null</code> if {@link #hasData()} returns <code>false</code>
     * @see #WindowCacheTable(DataTable)
     */
    public final DataTable getDataTable() {
        return m_table;
    }

    /**
     * Returns <code>true</code> if the row count of the table is known. That is if the underlying {@link DataTable}
     * knows its row count, if the iterator has traversed the whole <code>DataTable</code> at least once or if
     * {@link #setRowCount(long, boolean)} has been called.<br>
     * In this case also {@link #getRowCount()} will return a value and otherwise throw an exception.
     *
     * @return if there are no more unknown rows
     */
    public boolean hasRowCount() {
        return m_isMaxRowCountFinal;
    }

    /**
     * Changes the size of the ring buffer so that <code>size</code> rows can be cached. This method will clear the
     * current cache. The cache should have at least a size so that all rows that are displayed at a time can be
     * buffered (however, no further checking is carried out).
     *
     * <p>
     * If <code>size</code> is less than twice the look ahead size, i.e.
     * <pre><code>size &lt; 2*getChunkSize()</code></pre>, it will be set to it. So when a "new" row is requested,
     * at least <code>getLookAheadSize()</code> rows after (including that row) and before the new row will be also in
     * the cache.
     *
     * @param size size of the new cache.
     * @return the new cache size (may differ from <code>size</code>, see above)
     * @throws IllegalArgumentException if <code>size</code> <= 0.
     */
    public final int setCacheSize(final int size) {
        if (size == getCacheSize()) { // cool, nothing changed
            return size;
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must not <= 0: " + size);
        }
        m_cacheSize = Math.max(2 * getLookAheadSize(), size);
        m_cachedRows = new DataRow[m_cacheSize];
        clearCacheAndInitIterator();
        return m_cacheSize;
    }

    /**
     * Get the size of the cache.
     *
     * @return number of rows that fit in cache
     * @see #DEFAULT_CACHE_SIZE
     */
    public final int getCacheSize() {
        return m_cacheSize;
    }

    /**
     * Set a new chunk size. This value is defaulted to {@link #DEFAULT_LOOK_AHEAD} on start-up. The new value can be
     * at most <code>getCacheSize()/2</code>. If it is bigger, it's value is reduced to <code>getCacheSize()/2</code>.
     *
     * @param newSize the new value
     * @return the new look ahead size (may differ from passed argument, see above)
     * @throws IllegalArgumentException if <code>newSize</code> is &lt;= 0.
     * @see #getCacheSize()
     */
    public final int setLookAheadSize(final int newSize) {
        if (newSize <= 0) {
            throw new IllegalArgumentException(
                "Chunks must not be <= 0: " + newSize);
        }
        // avoid too small chunks, round up when odd chunk size
        m_lookAheadSize = Math.min(newSize, (getCacheSize() + 1) / 2);
        return m_lookAheadSize;
    }

    /**
     * Get the look ahead size.
     *
     * @return The current value of a look ahead being read (default: {@link #DEFAULT_LOOK_AHEAD}.
     */
    public final int getLookAheadSize() {
        return m_lookAheadSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataRow> getRows(final long start, final int length, final ExecutionMonitor exec)
            throws IndexOutOfBoundsException, CanceledExecutionException {
        checkRowIndex(start);
        if (length < 0 || start + length < 0) {
            throw new IndexOutOfBoundsException("Length can not be negative.");
        }
        if (length > m_cacheSize - m_lookAheadSize) {
            throw new IndexOutOfBoundsException("Length exceeds maximum value of rows that can be returned ("
                + (m_cacheSize - m_lookAheadSize) + "), but requested was " + length
                + ". Consider increasing the cache size or decreasing the amount of requested rows.");
        }
        final int cacheSize = getCacheSize();
        final long oldRowCount = m_rowCountOfInterest;
        final long lastRow = start + length - 1;

        // the iterator goes further when the last known row is requested
        boolean pushIterator = !hasRowCount() && (lastRow >= oldRowCount - 1);
        if (start >= (m_rowCountOfInterestInIterator - cacheSize)
                && (lastRow < m_rowCountOfInterestInIterator) && !pushIterator) {
            return getRowsFromCache(start, length, exec);
        }

        /* not all rows in cache */
        // some rows already released from cache
        if (start < (m_rowCountOfInterestInIterator - cacheSize)) {
            // clear cache, init new iterator
            clearCacheAndInitIterator();
        }
        assert (start + length >= m_rowCountOfInterestInIterator - 1);

        /* push iterator forward to index lastRow + m_lookAheadSize
         * (ensures when getRows(lastRow+1,...) is called the iterator does not need to be used again) */
        double rowDiffInitial = Math.max(1, lastRow - m_rowCountOfInterestInIterator + m_lookAheadSize);
        int newRowsCached = 0;
        boolean mayHaveNext;
        do {
            // changes also m_rowCountOfInterestInIterator
            mayHaveNext = cacheNextRow();
            if (exec != null) {
                exec.checkCanceled();
                exec.setProgress(++newRowsCached / rowDiffInitial);
            }
        } while ((m_rowCountOfInterestInIterator - 1) != (lastRow + m_lookAheadSize) && mayHaveNext);

        if (exec != null) {
            exec.setProgress(1.0);
        }
        return getRowsFromCache(start, length, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRowCount() throws UnknownRowCountException {
        if (!m_isMaxRowCountFinal) {
            throw new UnknownRowCountException();
        }
        return m_rowCountOfInterest;
    }

    /**
     * Pushes iterator one step further and caches the next element at the proper position in the ring buffer.
     *
     * @return <code>true</code> if that was successful, <code>false</code>
     *         if the iterator went to the end.
     */
    private boolean cacheNextRow() {
        assert (hasData());
        DataRow currentRow;
        if (!m_tableIterator.hasNext()) {
            // set to false with new data
            m_isMaxRowCountFinal = true;
            return false;
        }
        currentRow = m_tableIterator.next();
        m_rowCountInIterator++;
        m_maxRowCount = Math.max(m_maxRowCount, m_rowCountInIterator);
        // TODO: skip rows if rows need to be filtered by some other predicate (e.g. show selected only...)

        // index of row in cache
        int indexInCache = (int)m_rowCountOfInterestInIterator % getCacheSize();
        m_cachedRows[indexInCache] = currentRow;
        m_rowCountOfInterestInIterator++;
        m_rowCountOfInterest = Math.max(m_rowCountOfInterest, m_rowCountOfInterestInIterator);
        return true;
    }

    /**
     * Get new iterator, only to be called when data is set. If predicates are set those are tried to push down to
     * the iterator.
     */
    private RowIterator getNewDataIterator() {
        assert hasData();
        RowIteratorBuilder<? extends RowIterator> iteratorBuilder = m_table.iteratorBuilder();
        if (m_includedColumnIndices != null) {
            iteratorBuilder.filterColumns(m_includedColumnIndices.stream().toArray(String[]::new));
        }
        return iteratorBuilder.build();
    }

    /**
     * Clears cache, instantiates a new iterator.
     */
    private void clearCacheAndInitIterator() {
        if (!hasData()) {
            return;
        }
        if (m_tableIterator instanceof CloseableRowIterator) {
            ((CloseableRowIterator)m_tableIterator).close();
        }
        m_tableIterator = getNewDataIterator();
        m_rowCountInIterator = 0;
        // all updated in nextBlock()
        m_rowCountOfInterestInIterator = 0;
        // clear cache
        Arrays.fill(m_cachedRows, null);
    }

    /**
     * Returns a window of rows with a given index and length from the cache.
     * It is mandatory to give a row index which is certainly in the cache, i.e.
     * <code>(row >= (m_rowCountOfInterestInIterator - cS) && row < m_rowCountOfInterestInIterator)</code>
     * must hold.
     *
     * @param row index of the row in the underlying <code>DataTable</code>
     * @param length the length of the window to return
     * @param exec an optional execution monitor to set progress
     * @return the row with the given index
     */
    private List<DataRow> getRowsFromCache(final long start, final int length, final ExecutionMonitor exec) {
        List<DataRow> rowList = new ArrayList<DataRow>(length);
        for (long row = start; row < start + length; row++) {
            if (row >= m_rowCountOfInterest) {
                break; // row outside of table, may return an empty or partially filled list
            }
            rowList.add(m_cachedRows[indexForRow(row)]);
        }
        if (exec != null) {
            exec.setProgress(1);
        }
        return rowList;
    }

    /**
     * Get the index in the cache where row with index "row" is located.
     * The row MUST be in the cache.
     *
     * @param row row to access
     * @return internal index of the row
     */
    private int indexForRow(final long row) {
        final int cS = getCacheSize();
        assert (row >= (m_rowCountOfInterestInIterator - cS)
                && row < m_rowCountOfInterestInIterator) : "Row is not cached";
        // index of row in ring buffer
        int indexInCache = (int)(row % cS);
        return indexInCache;
    }

    /**
     * If the row count can be determined outside of this cache a caller can set the row count explicitly. Calling
     * this method with {@code isFinal == true} will lead to {@link #getRowCount()} returning this new count instead
     * of throwing an exception.
     *
     * @param newCount the new row count
     * @param isFinal if there are possibly more rows to count
     */
    public synchronized void setRowCount(final long newCount, final boolean isFinal) {
        final long oldCount = m_maxRowCount;
        if (oldCount >= newCount) {
            return;
        }
        m_isMaxRowCountFinal = isFinal;
        m_maxRowCount = newCount;
        m_rowCountOfInterest = m_maxRowCount;
    }

    /**
     * Checks if given argument is in range and throws an exception if it is not.
     *
     * @param rowIndex row index to check
     * @throws IndexOutOfBoundsException if argument violates range
     */
    private final void checkRowIndex(final long rowIndex) {
        if (rowIndex < 0) {
            throw new IndexOutOfBoundsException(
                "Row index must not be < 0: " + rowIndex);
        }
        // reasonable check only possible when table was seen completely,
        // method is called again when m_isMaxRowCountFinal switches to true
        long rowCount = -1;
        try {
            rowCount = getRowCount();
        } catch (UnknownRowCountException e) { }
        if (hasRowCount() && (rowIndex >= rowCount)) {
            throw new IndexOutOfBoundsException(
                "Row index " + rowIndex + " invalid: "
                + rowIndex + " >= " +  m_maxRowCount);
        }
    }
}

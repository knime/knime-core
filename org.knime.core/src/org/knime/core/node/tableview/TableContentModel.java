/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 * 
 * 2006-06-08 (tm): reviewed 
 */
package org.knime.core.node.tableview;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;


/** 
 * Proprietary implementation of a model for a table view. Input of the 
 * model is a {@link org.knime.core.data.DataTable} that is to be displayed
 * in the table view. 
 * <p>
 * This class uses a ring buffer to cache the rows being displayed. 
 * As new rows are read from the {@link org.knime.core.data.DataTable}
 * (using the the table's {@link org.knime.core.data.RowIterator}) they are
 * added to the ring buffer (and "old" rows 
 * are deleted). Each time a row is requested that resides before the cursor
 * of the current iterator and is not in the cache (default size: 500), the 
 * cache is cleared and a new iterator is instantiated. Thus, this class will
 * have some performance problems when the user scrolls up in the table view. 
 * However, when scrolling down, the data flow is somewhat "fluent".</p>
 * 
 * <p>This class also supports hiliting of rows (even though it is a view
 * property). We do store the hilite status of the rows in here as it 
 * complies nicely with the caching strategy.</p>
 * 
 * @see org.knime.core.data.DataTable
 * @see TableContentModel#setCacheSize(int)
 *   
 * @author Bernd Wiswedel, University of Konstanz
 */
public class TableContentModel extends AbstractTableModel 
    implements HiLiteListener, TableContentInterface {
    
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
    
    private static final long serialVersionUID = 8413295641103391635L;

    /**
     * Property name of the event when the data table has changed.
     */
    public static final String PROPERTY_DATA = "data changed";
    
    /** Property name of the event when hilite handler changes. */
    public static final String PROPERTY_HILITE = "hilite changed";
    
    /** 
     * Default size of the ring buffer (500).
     * 
     * @see #setCacheSize(int)
     */
    public static final int CACHE_SIZE = 500;
    
    /** 
     * Number of rows being read at a time (50). Once a "new" row is accessed,
     * the system will use the {@link org.knime.core.data.DataTable}'s
     * iterator to get <code>CHUNK_SIZE</code> new rows which are added to the
     * cache. 
     * Suppose you have a table with more than 100 rows: Those are then accessed
     * chunk-wise, i.e. 0-49, 50-99, and so on.
     * 
     * @see #setChunkSize(int)
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
     * hilited rows when {@link #m_showOnlyHiLit} is <code>true</code>, all
     * rows otherwise. This field is set to 0 when a new iterator is
     * instantiated. 
     */
    private int m_rowCountOfInterestInIterator;
    
    /**
     * Number of rows of interest that have been seen so far. If
     * {@link #m_showOnlyHiLit} is <code>false</code> this field is equal to
     * {@link #m_maxRowCount}.
     */
    private int m_rowCountOfInterest;
    
    /**
     * Is the current value of {@link #m_rowCountOfInterest} final? If
     * {@link #m_showOnlyHiLit} is <code>false</code> this field is equal to
     * {@link #m_isMaxRowCountFinal}.
     */
    private boolean m_isRowCountOfInterestFinal;
    
    /** Counter of rows in current iterator. If {@link #m_showOnlyHiLit} is
     * <code>false</code>, this field is equal to
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
    
    /** Handler to get the hilite status of the rows from and to send 
     * hilite requests to. Is <code>null</code> when no hilite available.
     */
    private HiLiteHandler m_hiLiteHdl;
    
    /** property handler that is used for events when data or 
     * hilite handler has changed.
     */
    private final PropertyChangeSupport m_propertySupport;
    
    /** A thread executed in background to count the rows, is triggered by
     * {@link #countRowsInBackground()}.
     */
    private RowCounterThread m_rowCounterThread;
    
    
    /** 
     * Creates a new TableContentModel with empty content. Call 
     * {@link #setDataTable(DataTable)} to set a valid data table. No 
     * HiLiting is available.
     * 
     * @see #setDataTable(DataTable)
     * @see #setHiLiteHandler(HiLiteHandler)
     */
    public TableContentModel() {
        m_maxRowCount = 0;
        m_isMaxRowCountFinal = true; // no data seen, assume final row count
        m_chunkSize = CHUNK_SIZE;
        m_cacheSize = CACHE_SIZE;
        m_propertySupport = new PropertyChangeSupport(this);
    } // TableContentModel()
    
    /** 
     * Creates a new TableContentModel displaying <code>data</code>.
     * No HiLiting is available.
     * 
     * @param data the table to be displayed. May be <code>null</code> to 
     *        indicate that there is no data available. 
     */
    public TableContentModel(final DataTable data) {
        this();
        setDataTable(data);
    } // TableContentModel(DataTable)
    
    /** 
     * Creates a new TableContentModel displaying <code>data</code>.
     * If <code>prop</code> is not <code>null</code>, its 
     * <code>HiLiteHandler</code> is used to do hilite synchronization.
     * 
     * @param data the table to be displayed. May be <code>null</code> to 
     *        indicate that there is no data available.
     * @param prop the <code>HiLiteHandler</code> However, may
     *        also be <code>null</code> to disable any hiliting. 
     */
    public TableContentModel(final DataTable data, final HiLiteHandler prop) {
        this(data);
        setHiLiteHandler(prop);
    } // TableContentModel(DataTable, HiLiteHandler)
    
    /**
     * Sets new data for this table. The argument may be <code>null</code> to
     * indicate invalid data (nothing displayed).
     * 
     * @param data the new data being displayed or <code>null</code>
     */
    public void setDataTable(final DataTable data) {
        // TODO: setDataIntern should be in own thread, but is now in 
        // EventDispatchThread (even invokeAndWait). This causes OutportView to 
        // freeze while data is loading (since it is loaded in EDT)
        if (SwingUtilities.isEventDispatchThread()) {
            setDataTableIntern(data);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                   public void run() {
                       setDataTableIntern(data);
                   } 
                });
            } catch (InterruptedException ie) {
                NodeLogger.getLogger(getClass()).warn(
                        "Exception while setting new table.", ie);
            } catch (InvocationTargetException ite) {
                NodeLogger.getLogger(getClass()).warn(
                        "Exception while setting new table.", ite);
            }
        }
    }

    /**
     * Sets new data for this table. The argument may be <code>null</code> to
     * indicate invalid data (nothing displayed).
     * 
     * @param data the new data being displayed or <code>null</code>
     */
    private synchronized void setDataTableIntern(final DataTable data) {
        if (m_data == data) {
            return;  // do not start event storm
        }
        cancelRowCountingInBackground();
        int oldColCount = getColumnCount();
        int newColCount = 
            data != null ? data.getDataTableSpec().getNumColumns() : 0;
        int oldRowCount = getRowCount();
        DataTable oldData = m_data;
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
        m_rowCounterThread = null;
        m_isMaxRowCountFinal = true;
        m_isRowCountOfInterestFinal = true;
        boolean structureChanged = oldColCount != newColCount;
        if (oldColCount == newColCount) {
            if (oldRowCount > 0) {
                fireTableRowsDeleted(0, oldRowCount - 1);
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
            if (data instanceof BufferedDataTable) {
                BufferedDataTable bData = (BufferedDataTable)data;
                m_isMaxRowCountFinal = true;
                m_maxRowCount = bData.getRowCount();
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
            cacheNextRow();
        }
        if (structureChanged) {
            // notify listeners
            fireTableStructureChanged();
        } else {
            int newRowCount = getRowCount();
            if (newRowCount > 0) {
                fireTableRowsInserted(0, newRowCount);
            }
        }
        m_propertySupport.firePropertyChange(PROPERTY_DATA, oldData, m_data);
    }
    
    /** 
     * Starts a new internal thread that will iterate over the table
     * and count the rows in it. Successive calls of this method are ignored.
     * Also if the final row count is known (either by consecutive calls of
     * this method or because the table is so small that the cache is filled 
     * with all rows) this method has no effect.<br />
     * 
     * The row counting process can be aborted by calling 
     * {@link #cancelRowCountingInBackground()}.
     */ 
    public synchronized void countRowsInBackground() {
        if (m_rowCounterThread != null || m_isMaxRowCountFinal) {
            return;
        }
        m_rowCounterThread = new RowCounterThread(this);
        m_rowCounterThread.start();
    }
    
    /** Cancels the potential row counter thread invoked by 
     * {@link #countRowsInBackground()}. If this method has not been called
     * yet, this method does nothing.
     */
    public synchronized void cancelRowCountingInBackground() {
        if (m_rowCounterThread != null) {
            m_rowCounterThread.interrupt();
            m_rowCounterThread = null;
        }
    }
    
    /** 
     * Set a new <code>HiLiteHandler</code>. If the argument is 
     * <code>null</code> hiliting is disabled.
     * 
     * @param hiliter the new handler to use.
     */
    public synchronized void setHiLiteHandler(final HiLiteHandler hiliter) {
        // nothing has changed, we come away easy
        if (hiliter == m_hiLiteHdl) {
            return;
        } 
        HiLiteHandler oldHandler = m_hiLiteHdl;
        if (m_hiLiteHdl != null) { // unregister from old handler 
            m_hiLiteHdl.removeHiLiteListener(this);
        }
        m_hiLiteHdl = hiliter;
        if (hiliter != null) { // register at new one
            hiliter.addHiLiteListener(this);
        }
        
        // check for rows whose hilite status has changed
        if (hasData()) {
            if (m_tableFilter.performsFiltering()) {
                m_isRowCountOfInterestFinal = false;
                m_rowCountOfInterest = 0;
                clearCache();
                cacheNextRow();
                fireTableDataChanged();
            } else {
                final int cacheSize = getCacheSize();
                // init with nonsense values - will change soon
                final int firstRowCached = firstRowCached();
                int firstRow = -1; // remember first and last changed index
                int lastRow = -1;
                // traverse all rows in cache and check if some are hilighted
                for (int i = 0; i < cacheSize; i++) {
                    final int indexInCache = (firstRowCached + i) % cacheSize; 
                    final DataRow current = indexInCache >= 0 
                        ? m_cachedRows[indexInCache] : null;
                    if (current == null) { // rows haven't been cached yet
                        break;             // everything after is also null
                    }
                    final RowKey key = current.getKey();
                    // do the hilite sync
                    final boolean wasHiLit = m_hilitSet.get(indexInCache);
                    final boolean isHiLit = (hiliter != null
                        ? hiliter.isHiLit(key) : false);
                    
                    // either hilite or color changed
                    if (wasHiLit != isHiLit) {
                        if (firstRow == -1) {
                            firstRow = indexInCache;
                        }
                        lastRow = indexInCache;
                        m_hilitSet.set(indexInCache, isHiLit);
                    }
                }
                // will swallow the event when firstRow > lastRow
                fireRowsInCacheUpdated(firstRow, lastRow);
            }
        }
        m_propertySupport.firePropertyChange(
                PROPERTY_HILITE, oldHandler, m_hiLiteHdl);
    } // setHiLiteHandler(HiLiteHandler)
    
    /** 
     * Return a reference to the hilite handler currently being used.
     * 
     * @return the current HiLiteHandler or <code>null</code> if none is set
     */
    public HiLiteHandler getHiLiteHandler() {
        return m_hiLiteHdl;
    }
    
    /** 
     * Is there valid data to show?
     * 
     * @return <code>true</code> if underlying <code>DataTable</code> is not
     * <code>null</code>
     */
    public final boolean hasData() {
        return m_data != null;
    } // hasData()
    
    /** Get the name of the current data table (if any) or <code>null</code>.
     * @return The table name or <code>null</code>.
     */
    public String getTableName() {
        return hasData() ? getDataTableSpec().getName() : null;
    }
    
    /** 
     * Is there a HiLiteHandler connected?
     * 
     * @return <code>true</code> if global hiliting is possible
     */
    public final boolean hasHiLiteHandler() {
        return m_hiLiteHdl != null;
    }

    /**
     * Control behaviour to show only hilited rows.
     * @param showOnlyHilite <code>true</code> Filter and display only rows
     *            whose hilite status is set.
     * @deprecated This method has been replaced by 
     * {@link #setTableContentFilter(TableContentFilter)}.
     */
    @Deprecated
    public final void showHiLitedOnly(final boolean showOnlyHilite) {
        setTableContentFilter(showOnlyHilite 
                ? TableContentFilter.HiliteOnly : TableContentFilter.All);
    }

    /** Get status of filtering for hilited rows.
    * @return <code>true</code> if only hilited rows are shown,
    *         <code>false</code> if all rows are shown.
    * @deprecated This method has been replaced by 
    *   {@link #getTableContentFilter()}
    */
    @Deprecated
   public boolean showsHiLitedOnly() {
       return TableContentFilter.HiliteOnly.equals(m_tableFilter);
   }

    
    /** Set new filter instance. If changed, the table will be refreshed.
     * @param newFilter The new filter to be applied.
     * @throws NullPointerException If argument is null.
     */
    public final void setTableContentFilter(
            final TableContentFilter newFilter) {
        if (newFilter.equals(m_tableFilter)) {
            return;
        }
        m_tableFilter = newFilter;
        if (m_tableFilter.performsFiltering()) {
            // don't know how many rows are being filtered
            m_rowCountOfInterest = 0;
            m_isRowCountOfInterestFinal = false;
        } else {
            assert m_tableFilter.equals(TableContentFilter.All);
            // row count may be known when it shows all rows
            m_rowCountOfInterest = m_maxRowCount;
            m_isRowCountOfInterestFinal = m_isMaxRowCountFinal;
        }
        // assume that there are rows, may change in cacheNextRow() below
        clearCache();  // will instantiate a new iterator.
        // will also set m_isRowCountOfInterestFinal etc. accordingly 
        cacheNextRow();
        fireTableDataChanged();
    }
    
    /** @return the currently set filter. */
    public TableContentFilter getTableContentFilter() {
        return m_tableFilter;
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
    public DataCell getValueAt(final int row, final int column) {
        boundColumn(column);
        DataRow result = getRow(row);
        return result.getCell(column);
    } // getValueAt(int, int)

    /**
     * {@inheritDoc}
     */
    public RowKey getRowKey(final int row) {
        DataRow result = getRow(row);
        return result.getKey();
    }

    /** 
     * Get the column header for some specific column index. The final column
     * name is generated by using the column header's <code>DataCell</code>
     * <code>toString()</code> method.
     * 
     * @param colIndex the column's index
     * @return header of column <code>colIndex</code>
     * @see AbstractTableModel#getColumnName(int)
     * @see DataColumnSpec#getName() 
     * @throws IndexOutOfBoundsException if <code>colIndex</code> out of range
     */
    @Override
    public String getColumnName(final int colIndex) {
        boundColumn(colIndex);
        DataColumnSpec colSpec = 
            m_data.getDataTableSpec().getColumnSpec(colIndex); 
        return colSpec.getName().toString();
    } // getColumnName(int)

    /** 
     * Returns DataCell.class.
     * {@inheritDoc}
     */
    @Override
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
        return m_data;
    } // getDataTable()
    
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
     * <p>If <code>size</code> is less than twice the chunk size, i.e.
     * <pre><code>size < 2*getChunkSize()</code></pre>, it will be set to it.
     * So when a "new" row is requested, at least <code>getChunkSize()</code>
     * rows after (including that row) and before the new row will be also in 
     * the cache.</p>
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
     * @see #setCacheSize(int)
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
     * @throws IllegalArgumentException if <code>newSize</code> is <= 0.
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
     * @see #setChunkSize(int)
     */
    public final int getChunkSize() {
        return m_chunkSize;
    } // getChunkSize()
    
    /**
     * {@inheritDoc}
     */
    public boolean isHiLit(final int row) {
        // ensure row is cached
        getRow(row);
        assert (row >= (m_rowCountOfInterestInIterator - getCacheSize()) 
                && row < m_rowCountOfInterestInIterator);
        return getHiLiteFromCache(row);
    } // isHiLit(int)
    
    /**
     * Get the color information for a row.
     *  
     * @param row The row of interest
     * @return The color information for that row
     */
    public ColorAttr getColorAttr(final int row) {
        // makes also sure row is cached
        DataRow r = getRow(row);
        assert (row >= (m_rowCountOfInterestInIterator - getCacheSize()) 
                && row < m_rowCountOfInterestInIterator);
        return m_data.getDataTableSpec().getRowColor(r);
    }
    
    /** 
     * HiLites all rows that are selected according to the given 
     * selection model. This method does nothing if no handler is connected.
     * 
     * @param selModel To get selection status from
     * @throws NullPointerException 
     *         if <code>selModel</code> is <code>null</code>
     */
    protected void requestHiLite(final ListSelectionModel selModel) {
        processHiLiteRequest(selModel, true);
    } // hilite(ListSelectionModel)
    
    /** 
     * "Unhilites" all rows that are selected according to the given 
     * selection model. This method does nothing if no handler is connected.
     * 
     * @param selModel To get selection status from
     * @throws NullPointerException 
     *         if <code>selModel</code> is <code>null</code>
     */
    protected void requestUnHiLite(final ListSelectionModel selModel) {
        processHiLiteRequest(selModel, false);
    } // unHilite(ListSelectionModel)
    
    /** 
     * Resets the hiliting of all keys by invoking the reset method in
     * the <code>HiLiteHandler</code>. This method does nothing if no handler
     * is connected.
     * 
     * @see HiLiteHandler#fireClearHiLiteEvent()
     */
    protected void requestResetHiLite() {
        if (m_hiLiteHdl != null) {
            m_hiLiteHdl.fireClearHiLiteEvent();
        }
    } // clearHilite()
    
    /**
     * {@inheritDoc}
     */
    public void hiLite(final KeyEvent e) {
        processHiLiteEvent(e, true);
    } // hiLite(KeyEvent)
    
    /**
     * {@inheritDoc}
     */
    public void unHiLite(final KeyEvent e) {
        processHiLiteEvent(e, false);
    } // unHiLite(KeyEvent)
    

    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll(final KeyEvent event) {
        if (!hasData()) {
            return;
        }
        final int cacheSize = getCacheSize();
        final int oldRowCount = getRowCount();
        // if it shows only the hilited ones, remove all rows.
        switch (m_tableFilter) {
        case HiliteOnly:
            m_rowCountOfInterest = 0;
            clearCache(); // clears also hilite
            fireTableRowsDeleted(0, oldRowCount);
            break;
        case UnHiliteOnly:
            // there may be more rows now (can't get fewer)
            boolean wasRowCountOfInterestFinal = m_isRowCountOfInterestFinal;
            m_isRowCountOfInterestFinal = false;
            clearCache(); // clears also hilite
            if (oldRowCount > 0) {
                fireTableRowsUpdated(0, oldRowCount - 1);
                if (wasRowCountOfInterestFinal) {
                    // go to last known (which will
                    // try to read further and send rows-inserted events)
                    getRow(oldRowCount - 1);
                }
            } else if (cacheNextRow()) {
                // row count was zero, notify listeners of first row 
                fireTableRowsInserted(oldRowCount, getRowCount() - 1);
            }
            break;
        case All:
            /* process event if it shows all rows */
            final int firstRowCached = firstRowCached();
            int firstI = -1; // remember first and last changed "i" (for event)
            int lastI = -1;
            // traverse all rows in cache and check if some are hilighted
            for (int i = 0; i < cacheSize; i++) {
                final int indexInCache = (firstRowCached + i) % cacheSize; 
                final DataRow current = indexInCache >= 0 
                    ? m_cachedRows[indexInCache] : null;
                if (current == null) { // last row, everything after is null
                    break;
                }
                boolean isHiLit = m_hilitSet.get(indexInCache);
                if (isHiLit) {
                    if (firstI == -1) {
                        firstI = indexInCache;
                    }
                    lastI = indexInCache;
                    m_hilitSet.set(indexInCache, false);
                }
            }
            if (lastI != -1) { // something has changed -> fire event
                assert (firstI != -1);
                fireRowsInCacheUpdated(firstI, lastI);
            }
            break;
        default: throw new InternalError("Unknown filter: " + m_tableFilter);
        }
    } // unHiLiteAll()
    
    /** 
     * Gets a row with a specified index. Either the row is in the cache (just
     * take it) or the row must be got from the DataTable (uses iterator and 
     * puts row into cache).
     * 
     * @param row row index of interest
     * @return the row at a specific position
     * @throws IndexOutOfBoundsException if <code>row</code> violates its range
     */
    protected DataRow getRow(final int row) {
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
        boolean mayHaveNext;
        do {
            // changes also m_rowCountOfInterestInIterator
            mayHaveNext = cacheNextRow();  
        } while ((m_rowCountOfInterestInIterator - 1) != (row + m_chunkSize) 
                && mayHaveNext);
        // is it the first time that we see the last row? (fire event)
        boolean isFinalSwap = !wasRowCountFinal && !mayHaveNext;
        // block contains rows that we haven't seen before
        if (isFinalSwap || (m_rowCountOfInterestInIterator > oldRowCount)) {
            if (isFinalSwap) {
                boundRow(row); // row count is final, we have an upper bound!
            }
            fireTableRowsInserted(oldRowCount, getRowCount() - 1);
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
    private boolean cacheNextRow() {
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
            isHiLit = m_hiLiteHdl != null 
                ? m_hiLiteHdl.isHiLit(currentRow.getKey()) : false;
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
        m_iterator = m_data.iterator();
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
     * Get Hilight status from a particular row index. The row must be 
     * in the cache (mandatory).
     *  
     * @param row index of the row in the underlying <code>DataTable</code>
     * @return the Hilight status of that row 
     */
    private boolean getHiLiteFromCache(final int row) {
        return m_hilitSet.get(indexForRow(row));
    } // getHiLiteFromCache(int) 

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
     * Translates hilight event and sets properties accordingly.
     * 
     * @param e the event to evaluate. 
     * @param isHiLite <code>true</code> for highlight request, 
     *        <code>false</code> for an unhilight
     */
    private void processHiLiteEvent(
            final KeyEvent e, final boolean isHiLite) {
        if (!hasData()) {
            return;
        }
        final int cacheSize = getCacheSize();
        final int oldRowCount = getRowCount();
        if (m_tableFilter.performsFiltering()) {
            /* what follows: run through the DataTable to the last 
             * cached row, count the number of rows that have been 
             * changed and add (or subtract, resp.) them from the global 
             * row count
             */
            // counter - runs from 0 to m_rowCountOfInterest
            int c = 0;
            // #rows that changed up to m_rowCountOfInterest
            int changedCount = 0;
            Set<RowKey> keySet = e.keys();
            for (RowIterator it = m_data.iterator(); it.hasNext() 
                && c < m_rowCountOfInterest;) {
                RowKey currentRowKey = it.next().getKey();
                boolean isNowOfInterest = 
                    m_tableFilter.matches(m_hiLiteHdl.isHiLit(currentRowKey));
                boolean hasChanged = keySet.contains(currentRowKey);
                changedCount += hasChanged ? 1 : 0;
                // was previously of interest?
                c += (hasChanged != isNowOfInterest) ? 1 : 0;
            }
            m_isRowCountOfInterestFinal = false;
            clearCache();
            boolean newRowsAdded;
            switch (m_tableFilter) {
            case HiliteOnly: newRowsAdded = isHiLite;
                break;
            case UnHiliteOnly: newRowsAdded = !isHiLite;
                break;
            default: throw new InternalError(
                    "Unexpected filter: " + m_tableFilter);
            }
            if (changedCount > 0) {
                assert (oldRowCount > 0);
                if (newRowsAdded) {
                    fireTableRowsUpdated(0, oldRowCount - 1);
                    m_rowCountOfInterest += changedCount;
                    fireTableRowsInserted(oldRowCount, getRowCount() - 1);
                } else {
                    m_rowCountOfInterest -= changedCount;
                    m_rowCountOfInterest = Math.max(m_rowCountOfInterest, 0);
                    fireTableRowsDeleted(m_rowCountOfInterest, oldRowCount - 1);
                    fireTableRowsUpdated(0, getRowCount());
                }
            }
            // there may be more rows after the last known one when 
            // newRowsAdded is true (only more rows are added - check for that)
            if (oldRowCount == getRowCount() && newRowsAdded) {
                if (oldRowCount > 0) { // that would fail if there were 0 rows
                    getRow(oldRowCount - 1); // move it to the last known
                }
                // are there new rows now?
                if (oldRowCount == getRowCount() && cacheNextRow()) {
                    fireTableRowsInserted(oldRowCount, oldRowCount);
                }
            }
            return;
        }
        
        /* process event if it shows all rows */
        final Set<RowKey> s = e.keys();
        final int firstRowCached = firstRowCached();
        int firstI = -1; // remember first and last changed "i" (for event)
        int lastI = -1;
        // traverse all rows in cache and check if the rows' key is hilighted
        for (int i = 0; i < cacheSize; i++) {
            final int indexInCache = (firstRowCached + i) % cacheSize; 
            final DataRow current = indexInCache >= 0 
                ? m_cachedRows[indexInCache] : null;
            if (current == null) { // last row, everything after is null
                break;
            }
            RowKey key = current.getKey();
            if (s.contains(key)) { // is newly hilighted
                if (firstI == -1) {
                    firstI = indexInCache;
                }
                lastI = indexInCache;
                // wasn't previously set
                assert (isHiLite != m_hilitSet.get(indexInCache));
                m_hilitSet.set(indexInCache, isHiLite);
            }
        }
        if (lastI != -1) { // something has changed -> fire event
            assert (firstI != -1);
            fireRowsInCacheUpdated(firstI, lastI);
        }
    } // processHiLiteEvent(KeyEvent, boolean)
    
    /** 
     * Propagates the selection status of the <code>ListSelectionModel</code> 
     * parameter to the <code>HiLiteHandler</code>. This method does nothing if
     * hilighting is disabled (according to the 
     * <code>hasHiLiteHandler()</code> method) or nothing is selected in 
     * <code>selModel</code>.
     * 
     * @param selModel To get selection status from
     * @param isHiLite Flag to tell if selected rows are to highlight or
     *        unhilight
     * @throws NullPointerException if <code>selModel</code> is null
     */
    private void processHiLiteRequest(
            final ListSelectionModel selModel, final boolean isHiLite) {
        if (selModel == null) {
            throw new NullPointerException("No selection model specified");
        }
        if (!hasHiLiteHandler() || selModel.isSelectionEmpty()) {
            return;
        }
        final int firstSelected = selModel.getMinSelectionIndex();
        final int lastSelected = selModel.getMaxSelectionIndex();
        final HashSet<RowKey> selectedSet = new HashSet<RowKey>();
        // if all selected rows are in cache
        if ((firstSelected >=  m_rowCountOfInterestInIterator - getCacheSize())
            && (lastSelected < m_rowCountOfInterestInIterator)) {
            // no new iteration necessary, simply traverse cache
            final int length = lastSelected - firstSelected;
            for (int i = 0; i <= length; i++) {
                int k = firstSelected + i;
                if (selModel.isSelectedIndex(k)) {
                    DataRow row = getRow(k);
                    selectedSet.add(row.getKey());
                }
            }
        } else { // iteration necessary: use new (private) iterator
            // TODO: check for correctness when m_showOnlyHilited is set
            final RowIterator it = m_data.iterator();
            for (int i = 0; it.hasNext() && i <= lastSelected; i++) {
                RowKey key = it.next().getKey();
                if (i >= firstSelected && selModel.isSelectedIndex(i)) {
                    selectedSet.add(key);
                }
            }
        } // end if-else
        assert (!selectedSet.isEmpty());
        // fire event according to mode
        if (isHiLite) {
            m_hiLiteHdl.fireHiLiteEvent(selectedSet);
        } else {
            m_hiLiteHdl.fireUnHiLiteEvent(selectedSet);
        }
    } //processHiLiteRequest(ListSelectionModel, boolean)
    
    /**
     * Fires a new {@link javax.swing.event.TableModelEvent} to inform
     * listeners that the rows between <code>rowForIndex(i1)</code> and 
     * <code>rowForIndex(i1)</code> have changed. This method returns 
     * immediately if <code>firstRow > lastRow</code>.
     * 
     * @param i1 First index in cache that changed
     * @param i2 Last index, respectively.
     */
    private void fireRowsInCacheUpdated(final int i1, final int i2) {
        // translate "i" to row id's
        int firstRow = rowForIndex(i1);
        int lastRow = rowForIndex(i2);
        if (firstRow > lastRow) {
            int swap = lastRow;
            lastRow = firstRow;
            firstRow = swap;
        }
        fireTableRowsUpdated(firstRow, lastRow);
    } // fireRowsInCacheUpdated(final int i1, final int i2)
    
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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireTableRowsInserted(oldCount, newCount - 1);
            }
        });
    }
    
    
    /**
     * Adds property change listener to this instance.
     * 
     * @param listener A new listener to register.
     * @see PropertyChangeSupport
     *      #addPropertyChangeListener(PropertyChangeListener)
     */
    public void addPropertyChangeListener(
            final PropertyChangeListener listener) {
        m_propertySupport.addPropertyChangeListener(listener);
    }
    
    /**
     * Adds property change listener to this instance.
     * 
     * @param propertyName only events with that id are fired to the listener.
     * @param listener the listener to register here
     * @see PropertyChangeSupport
     *      #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public void addPropertyChangeListener(final String propertyName,
            final PropertyChangeListener listener) {
        m_propertySupport.addPropertyChangeListener(propertyName, listener);
    }
    
    /**
     * Are there listeners registered to this class?
     * 
     * @param propertyName property name of interest
     * @return <code>true</code> if there are known listeners,
     * <code>false</code> otherwise
     * @see PropertyChangeSupport#hasListeners(java.lang.String)
     */
    public boolean hasListeners(final String propertyName) {
        return m_propertySupport.hasListeners(propertyName);
    }
    
    /**
     * Removes a listener from this instance.
     * 
     * @param listener listener to be removed
     * @see PropertyChangeSupport
     *      #removePropertyChangeListener(PropertyChangeListener)
     */
    public void removePropertyChangeListener(
            final PropertyChangeListener listener) {
        m_propertySupport.removePropertyChangeListener(listener);
    }
    
    /**
     * Removes a listener from this instance.
     * 
     * @param propertyName only if it listens to this property id 
     * @param listener listener to be removed
     * @see PropertyChangeSupport
     *      #removePropertyChangeListener(String, PropertyChangeListener)
     */
    public void removePropertyChangeListener(final String propertyName,
            final PropertyChangeListener listener) {
        m_propertySupport.removePropertyChangeListener(propertyName, listener);
    }
    
    /** 
     * Get reference to <code>PropertyChangeSupport</code> to allow subclasses
     * to fire customized events.
     * 
     * @return reference to change support object, never <code>null</code>.
     */
    protected final PropertyChangeSupport getPropertyChangeSupport() {
        return m_propertySupport;
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

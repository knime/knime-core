/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */
package org.knime.core.data.container;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.IDataTableDomainCreator;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.WorkflowDataRepository;
import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.DuplicateKeyException;
import org.knime.core.util.FileUtil;
import org.knime.core.util.ThreadSafeDuplicateChecker;

/**
 * Buffer that collects <code>DataRow</code> objects and creates a <code>DataTable</code> on request. This data
 * structure is useful if the number of rows is not known in advance.
 *
 * <p>
 * Usage: Create a container with a given spec (matching the rows being added later on, add the data using the
 * <code>addRowToTable(DataRow)</code> method and finally close it with <code>close()</code>. You can access the table
 * by <code>getTable()</code>.
 *
 * <p>
 * Note regarding the column domain: This implementation updates the column domain while new rows are added to the
 * table. It will keep the lower and upper bound for all columns that are numeric, i.e. whose column type is a sub type
 * of <code>DoubleCell.TYPE</code>. For categorical columns, it will keep the list of possible values if the number of
 * different values does not exceed 60. (If there are more, the values are forgotten and therefore not available in the
 * final table.) A categorical column is a column whose type is a sub type of <code>StringCell.TYPE</code>, i.e.
 * <code>StringCell.TYPE.isSuperTypeOf(yourtype)</code> where yourtype is the given column type.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DataContainer implements RowAppender {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataContainer.class);

    /**
     * Number of cells that are cached without being written to the temp file (see Buffer implementation); It defaults
     * value can be changed using the java property {@link #PROPERTY_CELLS_IN_MEMORY}.
     *
     * @deprecated access via {@link DataContainerSettings#getDefault()}
     */
    @Deprecated
    public static final int MAX_CELLS_IN_MEMORY;

    /**
     * The actual number of possible values being kept at most. See {@link #DEF_MAX_POSSIBLE_VALUES}.
     *
     * @since 2.10
     * @deprecated access via {@link DataContainerSettings#getDefault()}
     */
    @Deprecated
    public static final int MAX_POSSIBLE_VALUES;

    /**
     * The cache size for asynchronous table writing. It's the number of rows that are kept in memory before handing it
     * to the writer routines. The default value can be changed using the java property
     * {@link KNIMEConstants#PROPERTY_ASYNC_WRITE_CACHE_SIZE}.
     *
     * @deprecated access via {@link DataContainerSettings#getDefault()}
     */
    @Deprecated
    static final int ASYNC_CACHE_SIZE;

    /**
     * Whether to use synchronous IO while adding rows to a buffer or reading from an file iterator. The default value
     * can be changed by setting the appropriate java property {@link KNIMEConstants#PROPERTY_SYNCHRONOUS_IO} at
     * startup.
     *
     * @deprecated access via {@link DataContainerSettings#getDefault()}
     */
    @Deprecated
    static final boolean SYNCHRONOUS_IO;

    /**
     * The maximum number of asynchronous write threads, each additional container will switch to synchronous mode.
     *
     * @deprecated access via {@link DataContainerSettings#getDefault()}
     */
    @Deprecated
    static final int MAX_ASYNC_WRITE_THREADS;

    /**
     * The default value for initializing the domain.
     *
     * @deprecated access via {@link DataContainerSettings#getDefault()}
     */
    @Deprecated
    static final boolean INIT_DOMAIN;

    /** The executor, which runs the IO tasks. Currently used only while writing rows. */
    static final ThreadPoolExecutor ASYNC_EXECUTORS;

    static {
        final DataContainerSettings defaults = DataContainerSettings.getDefault();
        MAX_CELLS_IN_MEMORY = defaults.getMaxCellsInMemory();
        ASYNC_CACHE_SIZE = defaults.getAsyncCacheSize();
        SYNCHRONOUS_IO = defaults.useSyncIO();
        MAX_ASYNC_WRITE_THREADS = defaults.getMaxAsyncWriteThreads();
        MAX_POSSIBLE_VALUES = defaults.getMaxDomainValues();
        INIT_DOMAIN = defaults.getInitializeDomain();
        // see also Executors.newCachedThreadPool(ThreadFactory)
        ASYNC_EXECUTORS = new ThreadPoolExecutor(MAX_ASYNC_WRITE_THREADS, MAX_ASYNC_WRITE_THREADS, 60L,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                private final AtomicLong m_threadCount = new AtomicLong();

                /** {@inheritDoc} */
                @Override
                public Thread newThread(final Runnable r) {
                    return new Thread(r, "KNIME-Container-Thread-" + m_threadCount.getAndIncrement());
                }
            });
    }

    /**
     * The object that instantiates the buffer, may be set right after constructor call before any rows are added.
     */
    private BufferCreator m_bufferCreator;

    /** The object that saves the rows. */
    private Buffer m_buffer;

    /**
     * The current number of objects added to this container. In a synchronous case this number is equal to
     * m_buffer.size() but it may be larger if the data is written asynchronously.
     */
    private int m_size;

    /** The write throwable indicating that asynchronous writing failed. */
    private AtomicReference<Throwable> m_writeThrowable;

    /**
     * Whether this container writes synchronously, i.e. when rows come in they get written immediately. If true the
     * fields {@link #m_asyncAddFuture} and {@link #m_writeThrowable} are null. This field coincides most of times with
     * the {@link #SYNCHRONOUS_IO}, but may be true if there are too many concurrent write threads (more than
     * {@value #MAX_ASYNC_WRITE_THREADS}).
     */
    private final boolean m_isSynchronousWrite;

    /**
     * The index of the pending batch, i.e., the index of the next batch that has to be forwarded to the {@link Buffer}.
     */
    private final AtomicLong m_pendingBatch;

    /** The queue storing the {@link IDataTableDomainCreator} used by the {@link ContainerDomainRunnable}. */
    private final BlockingQueue<IDataTableDomainCreator> m_queue;

    /** The index of the current batch. */
    private long m_curBatch;

    /** Map storing those rows that still need to be forwarded to the {@link Buffer}. */
    private final Map<Long, List<BlobSupportDataRow>> m_blobRowMap;

    /** The batch. */
    private List<DataRow> m_batch;

    /** The size of each batch submitted to the {@link #ASYNC_EXECUTORS} service. */
    private final int m_batchSize;

    /** The maximum number of threads used by this container. */
    final int m_nThreads;

    /** Semaphore used to block the container until all {@link ContainerDomainRunnable} are finished. */
    private final Semaphore m_semaphore;

    /** The maximum number of rows kept in memory. */
    private int m_maxRowsInMemory;

    /** Holds the keys of the added rows to check for duplicates. */
    private ThreadSafeDuplicateChecker m_duplicateChecker;

    /** The tablespec of the return table. */
    private DataTableSpec m_spec;

    /** Table to return. Not null when close() is called. */
    private ContainerTable m_table;

    private IDataTableDomainCreator m_domainCreator;

    /** Local repository map, created lazily. */
    private Map<Integer, ContainerTable> m_localMap;

    /** repository for blob and filestore (de)serialization and table id handling */
    private IDataRepository m_dataRepository;

    /**
     * A file store handler. It's lazy initialized in this class. The buffered data container sets the FSH of the
     * corresponding node. A plain data container will copy file store cells.
     */
    private IWriteFileStoreHandler m_fileStoreHandler;

    /**
     * Whether to force a copy of any added blob. See {@link #setForceCopyOfBlobs(boolean)} for details.
     */
    private boolean m_forceCopyOfBlobs;

    /**
     * Opens the container so that rows can be added by <code>addRowToTable(DataRow)</code>. The table spec of the
     * resulting table (the one being returned by <code>getTable()</code>) will have a valid column domain. That means,
     * while rows are added to the container, the domain of each column is adjusted.
     * <p>
     * If you prefer to stick with the domain as passed in the argument, use the constructor
     * <code>DataContainer(DataTableSpec, true,
     * DataContainer.MAX_CELLS_IN_MEMORY)</code> instead.
     *
     * @param spec Table spec of the final table. Rows that are added to the container must comply with this spec.
     * @throws NullPointerException If <code>spec</code> is <code>null</code>.
     */
    public DataContainer(final DataTableSpec spec) {
        this(spec, false);
    }

    /**
     * Opens the container so that rows can be added by <code>addRowToTable(DataRow)</code>.
     *
     * @param spec Table spec of the final table. Rows that are added to the container must comply with this spec.
     * @param initDomain if set to true, the column domains in the container are initialized with the domains from spec.
     * @throws NullPointerException If <code>spec</code> is <code>null</code>.
     */
    public DataContainer(final DataTableSpec spec, final boolean initDomain) {
        this(spec, DataContainerSettings.getDefault().withInitializedDomain(initDomain));
    }

    /**
     * Opens the container so that rows can be added by <code>addRowToTable(DataRow)</code>.
     *
     * @param spec Table spec of the final table. Rows that are added to the container must comply with this spec.
     * @param initDomain if set to true, the column domains in the container are initialized with the domains from spec.
     * @param maxCellsInMemory Maximum count of cells in memory before swapping.
     * @throws IllegalArgumentException If <code>maxCellsInMemory</code> &lt; 0.
     * @throws NullPointerException If <code>spec</code> is <code>null</code>.
     */
    public DataContainer(final DataTableSpec spec, final boolean initDomain, final int maxCellsInMemory) {
        this(spec, DataContainerSettings.getDefault().withInitializedDomain(initDomain)
            .withMaxCellsInMemory(maxCellsInMemory));
    }

    /**
     * Opens the container so that rows can be added by <code>addRowToTable(DataRow)</code>.
     *
     * @param spec Table spec of the final table. Rows that are added to the container must comply with this spec.
     * @param initDomain if set to true, the column domains in the container are initialized with the domains from spec.
     * @param maxCellsInMemory Maximum count of cells in memory before swapping.
     * @param forceSynchronousIO Whether to force synchronous IO. If this property is false, it's using the default
     *            (which is false unless specified otherwise through {@link KNIMEConstants#PROPERTY_SYNCHRONOUS_IO})
     * @throws IllegalArgumentException If <code>maxCellsInMemory</code> &lt; 0 or the spec is null
     */
    protected DataContainer(final DataTableSpec spec, final boolean initDomain, final int maxCellsInMemory,
        final boolean forceSynchronousIO) {
        this(spec,
            DataContainerSettings.getDefault().withInitializedDomain(initDomain).withMaxCellsInMemory(maxCellsInMemory)
                .withSyncIO(forceSynchronousIO || DataContainerSettings.getDefault().useSyncIO()));
    }

    /**
     * Opens the container so that rows can be added by <code>addRowToTable(DataRow)</code>.
     *
     * @param spec Table spec of the final table. Rows that are added to the container must comply with this spec.
     * @param settings the container settings
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public DataContainer(final DataTableSpec spec, final DataContainerSettings settings) {
        CheckUtils.checkArgumentNotNull(spec, "Spec must not be null!");
        CheckUtils.checkArgument(settings.getMaxCellsInMemory() >= 0, "Cell count must be positive: %s",
            settings.getMaxCellsInMemory());
        m_spec = spec;
        m_duplicateChecker = settings.createDuplicateChecker();
        m_isSynchronousWrite = settings.useSyncIO();
        m_batchSize = settings.getAsyncCacheSize();
        if (m_isSynchronousWrite) {
            m_semaphore = null;
            m_pendingBatch = null;
            m_blobRowMap = null;
            m_writeThrowable = null;
            m_nThreads = 0;
            m_queue = null;
        } else {
            m_nThreads = Math.min(settings.getMaxContainerThreads(), ASYNC_EXECUTORS.getMaximumPoolSize());
            m_blobRowMap = new ConcurrentHashMap<>();
            m_semaphore = new Semaphore(m_nThreads);
            m_queue = new ArrayBlockingQueue<>(m_nThreads);
            m_batch = new ArrayList<>(m_batchSize);
            m_pendingBatch = new AtomicLong();
            m_writeThrowable = new AtomicReference<Throwable>();
            m_curBatch = 0;
        }
        m_domainCreator = settings.createDomainCreator(m_spec);
        m_size = 0;
        // how many rows will occupy MAX_CELLS_IN_MEMORY
        final int colCount = spec.getNumColumns();
        m_maxRowsInMemory = settings.getMaxCellsInMemory() / ((colCount > 0) ? colCount : 1);
        m_bufferCreator = new BufferCreator(settings.getBufferSettings());
    }

    private void addRowToTableWrite(final DataRow row) {
        // let's do every possible sanity check
        validateSpecCompatiblity(row);
        m_domainCreator.updateDomain(row);
        addRowKeyForDuplicateCheck(row.getKey());
        m_buffer.addRow(row, false, m_forceCopyOfBlobs);
    }

    /**
     * Validates that the given {@link DataRow} complies with the given {@link DataTableSpec}.
     *
     * @param row the row to validate
     *
     */
    private void validateSpecCompatiblity(final DataRow row) {
        int numCells = row.getNumCells();
        RowKey key = row.getKey();
        if (numCells != m_spec.getNumColumns()) {
            throw new IllegalArgumentException("Cell count in row \"" + key
                + "\" is not equal to length of column names array: " + numCells + " vs. " + m_spec.getNumColumns());
        }
        for (int c = 0; c < numCells; c++) {
            DataType columnClass = m_spec.getColumnSpec(c).getType();
            DataCell value;
            DataType runtimeType;
            if (row instanceof BlobSupportDataRow) {
                BlobSupportDataRow bsvalue = (BlobSupportDataRow)row;
                value = bsvalue.getRawCell(c);
            } else {
                value = row.getCell(c);
            }
            if (value instanceof BlobWrapperDataCell) {
                BlobWrapperDataCell bw = (BlobWrapperDataCell)value;
                runtimeType = bw.getBlobDataType();
            } else {
                runtimeType = value.getType();
            }

            if (!columnClass.isASuperTypeOf(runtimeType)) {
                String valString = value.toString();
                // avoid too long string representations
                if (valString.length() > 30) {
                    valString = valString.substring(0, 30) + "...";
                }
                throw new IllegalArgumentException("Runtime class of object \"" + valString + "\" (index " + c
                    + ") in row \"" + key + "\" is " + runtimeType.toString() + " and does "
                    + "not comply with its supposed superclass " + columnClass.toString());
            }
        } // for all cells
    }

    /**
     * Checks if any of the {@link ContainerDomainRunnable} threw an exception.
     *
     * @param waitForRunnables if set to {@code true} and an exception occured this call will block until all runnables
     *            are finished
     */
    private void checkAsyncWriteThrowable(final boolean waitForRunnables) {
        Throwable t = m_writeThrowable.get();
        if (t != null) {
            m_blobRowMap.clear();
            if (waitForRunnables) {
                try {
                    m_semaphore.acquire(m_nThreads);
                } catch (InterruptedException ex) {
                }
            }
            StringBuilder error = new StringBuilder();
            if (t.getMessage() != null) {
                error.append(t.getMessage());
            } else {
                error.append("Writing to table process threw \"");
                error.append(t.getClass().getSimpleName()).append("\"");
            }
            if (t instanceof DuplicateKeyException) {
                // self-causation not allowed
                throw new DuplicateKeyException((DuplicateKeyException)t);
            } else {
                throw new DataContainerException(error.toString(), t);
            }
        }
    }

    /**
     * Set a buffer creator to be used to initialize the buffer. This method must be called before any rows are added.
     *
     * @param bufferCreator To be used.
     * @throws NullPointerException If the argument is <code>null</code>.
     * @throws IllegalStateException If the buffer has already been created.
     */
    protected void setBufferCreator(final BufferCreator bufferCreator) {
        if (m_buffer != null) {
            throw new IllegalStateException("Buffer has already been created.");
        }
        if (bufferCreator == null) {
            throw new NullPointerException("BufferCreator must not be null.");
        }
        m_bufferCreator = bufferCreator;
    }

    /**
     * If true any blob that is not owned by this container, will be copied and this container will take ownership. This
     * option is true for loop end nodes, which need to aggregate the data generated in the loop body.
     *
     * @param forceCopyOfBlobs this above described property
     * @throws IllegalStateException If this buffer has already added rows, i.e. this method must be called right after
     *             construction.
     */
    protected final void setForceCopyOfBlobs(final boolean forceCopyOfBlobs) {
        if (size() > 0) {
            throw new IllegalStateException("Container already has rows;  invocation of this method is only permitted"
                + " immediately after constructor call.");
        }
        m_forceCopyOfBlobs = forceCopyOfBlobs;
    }

    /**
     * Get the property, which has possibly been set by {@link #setForceCopyOfBlobs(boolean)}.
     *
     * @return this property.
     */
    protected final boolean isForceCopyOfBlobs() {
        return m_forceCopyOfBlobs;
    }

    /**
     * Define a new threshold for number of possible values to memorize. It makes sense to call this method before any
     * rows are added.
     *
     * @param maxPossibleValues The new number.
     * @throws IllegalArgumentException If the value &lt; 0
     */
    public void setMaxPossibleValues(final int maxPossibleValues) {
        m_domainCreator.setMaxPossibleValues(maxPossibleValues);
    }

    /**
     * Returns <code>true</code> if the container has been initialized with <code>DataTableSpec</code> and is ready to
     * accept rows.
     *
     * <p>
     * This implementation returns <code>!isClosed()</code>;
     *
     * @return <code>true</code> if container is accepting rows.
     */
    public boolean isOpen() {
        return !isClosed();
    }

    /**
     * Returns <code>true</code> if table has been closed and <code>getTable()</code> will return a
     * <code>DataTable</code> object.
     *
     * @return <code>true</code> if table is available, <code>false</code> otherwise.
     */
    public boolean isClosed() {
        return m_table != null;
    }

    /**
     * Closes container and creates table that can be accessed by <code>getTable()</code>. Successive calls of
     * <code>addRowToTable</code> will fail with an exception.
     *
     * @throws IllegalStateException If container is not open.
     * @throws DuplicateKeyException If the final check for duplicate row keys fails.
     * @throws DataContainerException If the duplicate check fails for an unknown IO problem
     */
    public void close() {
        if (isClosed()) {
            return;
        }
        if (m_buffer == null) {
            m_buffer = m_bufferCreator.createBuffer(m_spec, m_maxRowsInMemory, createInternalBufferID(),
                getDataRepository(), getLocalTableRepository(), getFileStoreHandler(), m_isSynchronousWrite);
        }
        if (!m_isSynchronousWrite) {
            try {
                if (!m_batch.isEmpty()) {
                    submit();
                }
                // wait for all threads to stop
                m_semaphore.acquire(m_nThreads);
            } catch (final InterruptedException ie) {
                m_writeThrowable.compareAndSet(null, ie);
            }
            for (final IDataTableDomainCreator domainCreator : m_queue) {
                m_domainCreator.merge(domainCreator);
            }
            checkAsyncWriteThrowable(false);
        }
        // create table spec _after_ all_ rows have been added (i.e. wait for
        // asynchronous write thread to finish)
        DataTableSpec finalSpec = m_domainCreator.createSpec();
        m_buffer.close(finalSpec);
        try {
            m_duplicateChecker.checkForDuplicates();
        } catch (IOException ioe) {
            throw new DataContainerException("Failed to check for duplicate row IDs", ioe);
        } catch (DuplicateKeyException dke) {
            String key = dke.getKey();
            throw new DuplicateKeyException("Found duplicate row ID \"" + key + "\" (at unknown position)", key);
        }
        m_table = new ContainerTable(m_buffer);
        getLocalTableRepository().put(m_table.getBufferID(), m_table);
        m_buffer = null;
        m_spec = null;
        m_duplicateChecker.clear();
        m_duplicateChecker = null;
        m_domainCreator = null;
        m_size = -1;
    }

    /**
     * Get the number of rows that have been added so far. (How often has <code>addRowToTable</code> been called.)
     *
     * @return The number of rows in the container.
     * @throws IllegalStateException If container is not open.
     * @since 3.0
     */
    public long size() {
        if (isClosed()) {
            return m_table.getBuffer().size();
        }
        return m_size;
    }

    /**
     * Get reference to table. This method throws an exception unless the container is closed and has therefore a table
     * available.
     *
     * @return Reference to the table that has been built up.
     * @throws IllegalStateException If <code>isClosed()</code> returns <code>false</code>
     */
    public DataTable getTable() {
        return getBufferedTable();
    }

    /**
     * Disposes this container and all underlying resources.
     *
     * @since 3.1
     */
    public void dispose() {
        m_table.clear();
    }

    /**
     * Returns the table holding the data. This method is identical to the getTable() method but is more specific with
     * respec to the return type. It's used in derived classes.
     *
     * @return The table underlying this container.
     * @throws IllegalStateException If <code>isClosed()</code> returns <code>false</code>
     */
    protected final ContainerTable getBufferedTable() {
        if (!isClosed()) {
            throw new IllegalStateException("Cannot get table: container is not closed.");
        }
        return m_table;
    }

    /**
     * Used in tests.
     *
     * @return underlying buffer (or null if not initialized after restore).
     * @noreference This method is not intended to be referenced by clients.
     */
    public final Buffer getBuffer() {
        return m_buffer;
    }

    /**
     * Get the currently set DataTableSpec.
     *
     * @return The current spec.
     */
    public DataTableSpec getTableSpec() {
        if (isClosed()) {
            return m_table.getDataTableSpec();
        } else if (isOpen()) {
            return m_spec;
        }
        throw new IllegalStateException("Cannot get spec: container not open.");
    }

    /** {@inheritDoc} */
    @Override
    public void addRowToTable(final DataRow row) {
        if (!isOpen()) {
            throw new IllegalStateException("Cannot add row: container has not been initialized (opened).");
        }
        if (row == null) {
            throw new NullPointerException("Can't add null rows to container");
        }
        if (m_buffer == null) {
            int bufID = createInternalBufferID();
            Map<Integer, ContainerTable> localTableRep = getLocalTableRepository();
            IWriteFileStoreHandler fileStoreHandler = getFileStoreHandler();
            m_buffer = m_bufferCreator.createBuffer(m_spec, m_maxRowsInMemory, bufID, getDataRepository(),
                localTableRep, fileStoreHandler, m_isSynchronousWrite);
            if (m_buffer == null) {
                throw new NullPointerException("Implementation error, must not return a null buffer.");
            }
        }
        if (m_isSynchronousWrite) {
            if (MemoryAlertSystem.getInstance().isMemoryLow()) {
                m_buffer.flushBuffer();
            }
            addRowToTableWrite(row);
        } else {
            m_batch.add(row);
            try {
                if (MemoryAlertSystem.getInstance().isMemoryLow()) {
                    // flush the buffer
                    m_buffer.flushBuffer();
                    // write all keys to disk
                    m_duplicateChecker.writeToDisk();
                    // submit the pending batch
                    submit();
                    // wait until all runnables are finished
                    m_semaphore.acquire(m_nThreads);
                    m_semaphore.release(m_nThreads);
                } else {
                    if (m_batch.size() == m_batchSize) {
                        submit();
                    }
                }
            } catch (final IOException | InterruptedException e) {
                m_writeThrowable.compareAndSet(null, e);
            }
            checkAsyncWriteThrowable(true);
        }
        m_size += 1;
    } // addRowToTable(DataRow)

    /**
     * Submits the current batch to the {@link #ASYNC_EXECUTORS} service.
     *
     * @throws InterruptedException if an interrupted occured
     */
    private void submit() throws InterruptedException {
        // wait until we are allowed to submit a new runnable
        m_semaphore.acquire();
        // re-use an domainCreator if possible and submit the job
        IDataTableDomainCreator domainCreator = m_queue.poll();
        if (domainCreator == null) {
            domainCreator = new DataTableDomainCreator(m_spec, false);
            domainCreator.setMaxPossibleValues(m_domainCreator.getMaxPossibleVals());
        }
        ASYNC_EXECUTORS.execute(new ContainerDomainRunnable(this, domainCreator, m_batch, m_curBatch++));
        // reset batch
        m_batch = new ArrayList<>(m_batchSize);
    }

    /** @return size of buffer temp file in bytes, -1 if not set. Only for debugging/test purposes. */
    long getBufferFileSize() {
        Buffer b = m_table != null ? m_table.getBuffer() : m_buffer;
        if (b != null) {
            return b.getBufferFileSize();
        }
        return -1L;
    }

    /**
     * ID for buffers, which are not part of the workflow (no BufferedDataTable).
     *
     * @since 3.7
     */
    public static final int NOT_IN_WORKFLOW_BUFFER = -1;

    /**
     * Get an internal id for the buffer being used. This ID is used in conjunction with blob serialization to locate
     * buffers. Blobs that belong to a Buffer (i.e. they have been created in a particular Buffer) will write this ID
     * when serialized to a file. Subsequent Buffers that also need to serialize Blob cells (which, however, have
     * already been written) can then reference to the respective Buffer object using this ID.
     *
     * <p>
     * An ID of -1 denotes the fact, that the buffer is not intended to be used for sophisticated blob serialization.
     * All blob cells that are added to it will be newly serialized as if they were created for the first time.
     *
     * <p>
     * This implementation returns -1 ({@link #NOT_IN_WORKFLOW_BUFFER}.
     *
     * @return -1 or a unique buffer ID.
     */
    protected int createInternalBufferID() {
        return NOT_IN_WORKFLOW_BUFFER;
    }

    /**
     * Method being called when {@link #addRowToTable(DataRow)} is called. This method will add the given row key to the
     * internal row key hashing structure, which allows for duplicate checking.
     *
     * <p>
     * This method may be overridden to disable duplicate checks. The overriding class must ensure that there are no
     * duplicates being added whatsoever.
     *
     * @param key Key being added. This implementation extracts the string representation from it and adds it to an
     *            internal {@link DuplicateChecker} instance.
     * @throws DataContainerException This implementation may throw a <code>DataContainerException</code> when
     *             {@link DuplicateChecker#addKey(String)} throws an {@link IOException}.
     * @throws DuplicateKeyException If a duplicate is encountered.
     */
    protected void addRowKeyForDuplicateCheck(final RowKey key) {
        //        synchronized (m_duplicateChecker) {
        try {
            m_duplicateChecker.addKey(key.toString());
        } catch (IOException ioe) {
            throw new DataContainerException(
                ioe.getClass().getSimpleName() + " while checking for duplicate row IDs: " + ioe.getMessage(), ioe);
        } catch (DuplicateKeyException dke) {
            throw new DuplicateKeyException(
                "Encountered duplicate row ID  \"" + dke.getKey() + "\" at row number " + (m_buffer.size() + 1),
                dke.getKey());
        }
        //        }
    }

    /**
     * @return The file store handler for this container (either initialized lazy or previously set by the node).
     * @since 2.6
     * @nooverride
     * @noreference This method is not intended to be referenced by clients.
     */
    protected IWriteFileStoreHandler getFileStoreHandler() {
        if (m_fileStoreHandler == null) {
            m_fileStoreHandler = NotInWorkflowWriteFileStoreHandler.create();
            m_fileStoreHandler.addToRepository(getDataRepository());
        }
        return m_fileStoreHandler;
    }

    /**
     * @param handler the fileStoreHandler to set
     * @nooverride
     * @noreference This method is not intended to be referenced by clients.
     */
    protected final void setFileStoreHandler(final IWriteFileStoreHandler handler) {
        if (m_fileStoreHandler != null) {
            throw new IllegalStateException("File store handler already assigned");
        }
        if (handler == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_fileStoreHandler = handler;
    }

    /**
     * Get the data repository. Overridden in {@link org.knime.core.node.BufferedDataContainer}.
     *
     * @return A data repository for deserializing blobs and file stores and for handling table ids
     * @since 3.7
     */
    protected IDataRepository getDataRepository() {
        if (m_dataRepository == null) {
            m_dataRepository = NotInWorkflowDataRepository.newInstance();
        }
        return m_dataRepository;
    }

    /**
     * Get the local repository. Overridden in {@link org.knime.core.node.BufferedDataContainer}
     *
     * @return A local repository to which tables are added that have been created during the node's execution.
     */
    protected Map<Integer, ContainerTable> getLocalTableRepository() {
        if (m_localMap == null) {
            m_localMap = new HashMap<Integer, ContainerTable>();
        }
        return m_localMap;
    }

    /**
     * @return the isSynchronousWrite whether the data is written in the same thread that calls addRow. Property depends
     *         on system property {@link #SYNCHRONOUS_IO} and the number of concurrent writes.
     */
    boolean isSynchronousWrite() {
        return m_isSynchronousWrite;
    }

    /**
     * Convenience method that will buffer the entire argument table. This is useful if you have a wrapper table at hand
     * and want to make sure that all calculations are done here
     *
     * @param table The table to cache.
     * @param exec The execution monitor to report progress to and to check for the cancel status.
     * @param maxCellsInMemory The number of cells to be kept in memory before swapping to disk.
     * @return A cache table containing the data from the argument.
     * @throws NullPointerException If the argument is <code>null</code>.
     * @throws CanceledExecutionException If the process has been canceled.
     */
    public static DataTable cache(final DataTable table, final ExecutionMonitor exec, final int maxCellsInMemory)
        throws CanceledExecutionException {
        DataContainer buf = new DataContainer(table.getDataTableSpec(), true, maxCellsInMemory);
        int row = 0;
        try {
            for (RowIterator it = table.iterator(); it.hasNext(); row++) {
                DataRow next = it.next();
                exec.setMessage("Caching row #" + (row + 1) + " (\"" + next.getKey() + "\")");
                exec.checkCanceled();
                buf.addRowToTable(next);
            }
        } finally {
            buf.close();
        }
        return buf.getTable();
    }

    /**
     * Convenience method that will buffer the entire argument table. This is useful if you have a wrapper table at hand
     * and want to make sure that all calculations are done here
     *
     * @param table The table to cache.
     * @param exec The execution monitor to report progress to and to check for the cancel status.
     * @return A cache table containing the data from the argument.
     * @throws NullPointerException If the argument is <code>null</code>.
     * @throws CanceledExecutionException If the process has been canceled.
     */
    public static DataTable cache(final DataTable table, final ExecutionMonitor exec)
        throws CanceledExecutionException {
        return cache(table, exec, DataContainerSettings.getDefault().getMaxCellsInMemory());
    }

    /** Used in write/readFromZip: Name of the zip entry containing the spec. */
    static final String ZIP_ENTRY_SPEC = "spec.xml";

    /** Used in write/readFromZip: Config entry: The spec of the table. */
    static final String CFG_TABLESPEC = "table.spec";

    /**
     * Writes a given DataTable permanently to a zip file. This includes also all table spec information, such as color,
     * size, and shape properties.
     *
     * @param table The table to write.
     * @param zipFile The file to write to. Will be created or overwritten.
     * @param exec For progress info.
     * @throws IOException If writing fails.
     * @throws CanceledExecutionException If canceled.
     * @see #readFromZip(File)
     */
    public static void writeToZip(final DataTable table, final File zipFile, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        try (OutputStream out = new FileOutputStream(zipFile)) {
            writeToStream(table, out, exec);
        }
    }

    /**
     * Writes a given DataTable permanently to an output stream. This includes also all table spec information, such as
     * color, size, and shape properties.
     *
     * <p>
     * The content is saved by instantiating a {@link ZipOutputStream} on the argument stream, saving the necessary
     * information in respective zip entries. The stream is not closed by this method.
     *
     * @param table The table to write.
     * @param out The stream to save to. It does not have to be buffered.
     * @param exec For progress info.
     * @throws IOException If writing fails.
     * @throws CanceledExecutionException If canceled.
     * @see #readFromStream(InputStream)
     */
    public static void writeToStream(final DataTable table, final OutputStream out, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        Buffer buf;
        ExecutionMonitor e = exec;
        boolean canUseBuffer = table instanceof ContainerTable;
        if (canUseBuffer) {
            Buffer b = ((ContainerTable)table).getBuffer();
            if (b.containsBlobCells() && b.getBufferID() != -1) {
                canUseBuffer = false;
            }
        }
        if (canUseBuffer) {
            buf = ((ContainerTable)table).getBuffer();
        } else {
            exec.setMessage("Archiving table");
            e = exec.createSubProgress(0.8);
            buf = new Buffer(table.getDataTableSpec(), 0, -1, NotInWorkflowDataRepository.newInstance(),
                new HashMap<Integer, ContainerTable>(), NotInWorkflowWriteFileStoreHandler.create(), true);
            int rowCount = 0;
            for (DataRow row : table) {
                rowCount++;
                e.setMessage("Writing row #" + rowCount + " (\"" + row.getKey() + "\")");
                e.checkCanceled();
                buf.addRow(row, false, false);
            }
            buf.close(table.getDataTableSpec());
            exec.setMessage("Closing zip file");
            e = exec.createSubProgress(0.2);
        }
        final boolean originalOutputIsBuffered =
            ((out instanceof BufferedOutputStream) || (out instanceof ByteArrayOutputStream));
        OutputStream os = originalOutputIsBuffered ? out : new BufferedOutputStream(out);

        ZipOutputStream zipOut = new ZipOutputStream(os);
        // (part of) bug fix #1141: spec must be put as first entry in order
        // for the table reader to peek it
        zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_SPEC));
        NodeSettings settings = new NodeSettings("Table Spec");
        NodeSettingsWO specSettings = settings.addNodeSettings(CFG_TABLESPEC);
        buf.getTableSpec().save(specSettings);
        settings.saveToXML(new NonClosableOutputStream.Zip(zipOut));
        buf.addToZipFile(zipOut, e);
        zipOut.finish();
        if (!originalOutputIsBuffered) {
            os.flush();
        }
    }

    /**
     * Reads a table from a zip file that has been written using the
     * {@link #writeToZip(DataTable, File, ExecutionMonitor)} method.
     *
     * @param zipFile To read from.
     * @return The table contained in the zip file.
     * @throws IOException If that fails.
     * @see #writeToZip(DataTable, File, ExecutionMonitor)
     */
    public static ContainerTable readFromZip(final File zipFile) throws IOException {
        return readFromZip(new ReferencedFile(zipFile), new BufferCreator());
    }

    /**
     * Reads a table from an input stream. This is the reverse operation of
     * {@link #writeToStream(DataTable, OutputStream, ExecutionMonitor)}.
     *
     * <p>
     * The argument stream will be closed. If this is not desired, consider to use a {@link NonClosableInputStream} as
     * argument.
     *
     * @param in To read from, Stream will be closed finally.
     * @return The table contained in the stream.
     * @throws IOException If that fails.
     * @see #writeToStream(DataTable, OutputStream, ExecutionMonitor)
     */
    public static ContainerTable readFromStream(final InputStream in) throws IOException {
        // mimic the behavior of readFromZip(ReferencedFile)
        CopyOnAccessTask coa = new CopyOnAccessTask(/*File*/null, null, -1, NotInWorkflowDataRepository.newInstance(),
            new BufferCreator());
        // executing the createBuffer() method will start the copying process
        Buffer buffer = coa.createBuffer(in);
        return new ContainerTable(buffer);
    }

    /**
     * Factory method used to restore table from zip file.
     *
     * @param zipFileRef To read from.
     * @param creator Factory object to create a buffer instance.
     * @return The table contained in the zip file.
     * @throws IOException If that fails.
     * @see #readFromZip(File)
     */
    // This method is used from #readFromZip(File) or from a
    // RearrangeColumnsTable when it reads a table that has been written
    // with KNIME 1.1.x or before.
    static ContainerTable readFromZip(final ReferencedFile zipFileRef, final BufferCreator creator) throws IOException {
        /*
         * Ideally, the entire functionality of reading the zip file should take
         * place in the Buffer class (as that is also the place where save is
         * implemented). However, that is not that obvious to implement since
         * the buffer needs to be created using the DataTableSpec
         * (DataContainer.writeToZip stores the DTS in the zip file), which we
         * need to extract beforehand. Using a ZipFile here and extracting only
         * the DTS is not possible, as that may crash (OutOfMemoryError), see
         * bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4705373
         */
        // bufferID = -1: all blobs are contained in buffer, no fancy
        // reference handling to other buffer objects
        CopyOnAccessTask coa =
            new CopyOnAccessTask(zipFileRef, null, -1, NotInWorkflowDataRepository.newInstance(), creator);
        // executing the createBuffer() method will start the copying process
        Buffer buffer = coa.createBuffer();
        return new ContainerTable(buffer);
    }

    /**
     * Used in {@link org.knime.core.node.BufferedDataContainer} to read the tables from the workspace location.
     *
     * @param zipFile To read from (is going to be copied to temp on access)
     * @param spec The DTS for the table.
     * @param bufferID The buffer's id used for blob (de)serialization
     * @param dataRepository Workflow global data repository for blob and file store resolution.
     * @return Table contained in <code>zipFile</code>.
     * @noreference This method is not intended to be referenced by clients.
     */
    protected static ContainerTable readFromZipDelayed(final ReferencedFile zipFile, final DataTableSpec spec,
        final int bufferID, final WorkflowDataRepository dataRepository) {
        CopyOnAccessTask t = new CopyOnAccessTask(zipFile, spec, bufferID, dataRepository, new BufferCreator());
        return readFromZipDelayed(t, spec);
    }

    /**
     * Used in {@link org.knime.core.node.BufferedDataContainer} to read the tables from the workspace location.
     *
     * @param c The factory that create the Buffer instance that the returned table reads from.
     * @param spec The DTS for the table.
     * @return Table contained in <code>zipFile</code>.
     */
    static ContainerTable readFromZipDelayed(final CopyOnAccessTask c, final DataTableSpec spec) {
        return new ContainerTable(c, spec);
    }

    /** the temp file will have a time stamp in its name. */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /**
     * Creates a temp file called "knime_container_<i>date</i>_xxxx.bin.gz" and marks it for deletion upon exit. This
     * method is used to init the file when the data container flushes to disk. It is also used when the nodes are read
     * back in to copy the data to the tmp-directory.
     *
     * @return A temp file to use. The file is empty.
     * @throws IOException If that fails for any reason.
     * @deprecated use {@link #createTempFile(String)} -- in fact, method should only be used withing the core
     * @noreference This method is not intended to be referenced by clients.
     */
    @Deprecated
    public static final File createTempFile() throws IOException {
        return createTempFile(".bin.gz");
    }

    /**
     * Creates a temp file called "knime_container_<i>date</i>_xxxx.&lt;suffix>" and marks it for deletion upon exit.
     * This method is used to init the file when the data container flushes to disk. It is also used when the nodes are
     * read back in to copy the data to the tmp-directory.
     *
     * @param suffix The file suffix (e.g. ".bin.gz")
     * @return A temp file to use. The file is empty.
     * @throws IOException If that fails for any reason.
     * @noreference This method is not intended to be referenced by clients.
     * @since 3.6
     */
    public static final File createTempFile(final String suffix) throws IOException {
        String date;
        synchronized (DATE_FORMAT) {
            date = DATE_FORMAT.format(new Date());
        }
        String fileName = "knime_container_" + date + "_";
        File f = FileUtil.createTempFile(fileName, suffix);
        f.deleteOnExit();
        return f;
    }

    /**
     * Returns <code>true</code> if the given argument table has been created by the DataContainer, <code>false</code>
     * otherwise.
     *
     * @param table The table to check.
     * @return If the given table was created by a DataContainer.
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    public static final boolean isContainerTable(final DataTable table) {
        return table instanceof ContainerTable;
    }

    /**
     * Implements a runnable that validates the row against the defined spec, tests for key duplicates, transforms a
     * {@link DataRow} to a {@link BlobSupportDataRow} and finally forwards the rows, in proper order, to the buffer.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private static final class ContainerDomainRunnable implements Runnable {

        /** Weak reference to the container creating the runnable. */
        private final WeakReference<DataContainer> m_containerRef;

        /** The data table domain creator. */
        private final IDataTableDomainCreator m_dataTableDomainCreator;

        /** The batch of rows to be processed. */
        private final List<DataRow> m_rows;

        /** The current batch index. */
        private final long m_batchIdx;

        /**
         * Constructor.
         *
         * @param container the container
         * @param domainCreator the domain creator
         * @param rows the batch of rows to be processed
         * @param batchIdx the batch index
         */
        ContainerDomainRunnable(final DataContainer container, final IDataTableDomainCreator domainCreator,
            final List<DataRow> rows, final long batchIdx) {
            m_containerRef = new WeakReference<>(container);
            m_dataTableDomainCreator = domainCreator;
            m_rows = rows;
            m_batchIdx = batchIdx;
        }

        @Override
        public void run() {
            final DataContainer container = m_containerRef.get();
            if (container != null && container.m_writeThrowable.get() == null) {
                try {
                    final List<BlobSupportDataRow> blobRows = new ArrayList<>(m_rows.size());
                    for (final DataRow row : m_rows) {
                        container.validateSpecCompatiblity(row);
                        m_dataTableDomainCreator.updateDomain(row);
                        container.addRowKeyForDuplicateCheck(row.getKey());
                        blobRows.add(container.m_buffer.saveBlobsAndFileStores(row, container.m_forceCopyOfBlobs));
                    }
                    boolean addRows;
                    synchronized (container.m_pendingBatch) {
                        addRows = m_batchIdx == container.m_pendingBatch.get();
                        if (!addRows) {
                            container.m_blobRowMap.put(m_batchIdx, blobRows);
                        }
                    }
                    if (addRows) {
                        addRows(container, blobRows);
                        while (hasEntry(container)) {
                            addRows(container, container.m_blobRowMap.remove(container.m_pendingBatch.get()));
                        }
                    }
                } catch (final IOException | DuplicateKeyException | DataContainerException | IllegalArgumentException
                        | SecurityException e) {
                    container.m_writeThrowable.compareAndSet(null, e);
                } finally {
                    container.m_queue.add(m_dataTableDomainCreator);
                    container.m_semaphore.release();
                }
            } else if (container != null) {
                container.m_queue.add(m_dataTableDomainCreator);
                container.m_semaphore.release();
            }
        }

        /**
         * Forwards the given list of {@link BlobSupportDataRow} to the buffer
         *
         * @param container the container holding the buffer
         * @param blobRows the rows to be forwarded to the buffer
         * @throws IOException - if the buffer cannot write the rows to disc
         */
        private static void addRows(final DataContainer container, final List<BlobSupportDataRow> blobRows)
            throws IOException {
            for (final BlobSupportDataRow row : blobRows) {
                container.m_buffer.addBlobSupportDataRow(row);
            }
        }

        /**
         * Checks whether there is another batch of {@link BlobSupportDataRow} exists that has to be forwarded to the
         * buffer.
         *
         * @param container the container holding the buffer
         * @return {@code true} if another batch has to be forwarded to the buffer, {@code false} otherwise
         */
        private static boolean hasEntry(final DataContainer container) {
            synchronized (container.m_pendingBatch) {
                return container.m_blobRowMap.containsKey(container.m_pendingBatch.incrementAndGet());
            }
        }

    }

    /**
     * Helper class to create a Buffer instance given a binary file and the data table spec.
     */
    static class BufferCreator {

        /** The settings informing about the output format used by the writing buffer. */
        final BufferSettings m_bufferSettings;

        /**
         * Constructor.
         */
        BufferCreator() {
            this(BufferSettings.getDefault());
        }

        /**
         * Constructor.
         *
         * @param settings the {@link BufferSettings}
         */
        BufferCreator(final BufferSettings settings) {
            m_bufferSettings = settings;
        }

        /**
         * Creates buffer for reading.
         *
         * @param binFile the binary temp file.
         * @param blobDir temp directory containing blobs (may be null).
         * @param fileStoreDir temp dir containing file stores (mostly null)
         * @param spec The spec.
         * @param metaIn Input stream containing meta information.
         * @param bufID The buffer's id used for blob (de)serialization
         * @param dataRepository repository for blob and filestore (de)serialization and table id handling
         * @return A buffer instance.
         * @throws IOException If parsing fails.
         */
        Buffer createBuffer(final File binFile, final File blobDir, final File fileStoreDir, final DataTableSpec spec,
            final InputStream metaIn, final int bufID, final IDataRepository dataRepository) throws IOException {
            return new Buffer(binFile, blobDir, fileStoreDir, spec, metaIn, bufID, dataRepository, m_bufferSettings);
        }

        /**
         * Creates buffer for writing (adding of rows).
         *
         * @param spec Write spec -- used to initialize output stream for some non-KNIME formats
         * @param rowsInMemory The number of rows being kept in memory.
         * @param bufferID The buffer's id used for blob (de)serialization.
         * @param dataRepository repository for blob and filestore (de)serialization and table id handling
         * @param localTableRep Table repository for blob (de)serialization.
         * @param fileStoreHandler the file store handler
         * @param forceSynchronousWrite whether to force the buffer disk IO thread to write synchronously
         *
         * @return A newly created buffer.
         */
        Buffer createBuffer(final DataTableSpec spec, final int rowsInMemory, final int bufferID,
            final IDataRepository dataRepository, final Map<Integer, ContainerTable> localTableRep,
            final IWriteFileStoreHandler fileStoreHandler, final boolean forceSynchronousWrite) {
            return new Buffer(spec, rowsInMemory, bufferID, dataRepository, localTableRep, fileStoreHandler,
                forceSynchronousWrite, m_bufferSettings);
        }

    }
}
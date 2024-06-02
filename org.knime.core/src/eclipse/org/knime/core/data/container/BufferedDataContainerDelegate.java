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
 *   Apr 30, 2020 (dietzc): created
 */
package org.knime.core.data.container;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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

import org.apache.commons.lang3.mutable.MutableLong;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.storage.TableStoreFormat;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowDataRepository;
import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.DuplicateKeyException;

/**
 * {@link DataContainerDelegate} implementation using {@link Buffer} and {@link TableStoreFormat}.
 * Previously implemented in {@link DataContainer}. Separated out to make it exchangeable.
 *
 * @author Bernd Wiswedel, KNIME GmbH
 * @author Mark Ortmann, KNIME GmbH
 * @author Christian Dietz, KNIME GmbH
 * @since 4.2
 */
class BufferedDataContainerDelegate implements DataContainerDelegate {

    /** The executor, which runs the IO tasks. Currently used only while writing rows. */
    static final ThreadPoolExecutor ASYNC_EXECUTORS;

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
     * The default value for initializing the domain.
     *
     * @deprecated access via {@link DataContainerSettings#getDefault()}
     */
    @Deprecated
    static final boolean INIT_DOMAIN;

    static {
        final DataContainerSettings defaults = DataContainerSettings.getDefault();
        ASYNC_CACHE_SIZE = defaults.getRowBatchSize();
        SYNCHRONOUS_IO = defaults.isForceSequentialRowHandling();
        INIT_DOMAIN = defaults.isInitializeDomain();
        // see also {@link Executors#fixedThradPool(ThreadFactory)}
        ASYNC_EXECUTORS = new ThreadPoolExecutor(defaults.getMaxContainerThreads(), defaults.getMaxContainerThreads(),
            10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                private final AtomicLong m_threadCount = new AtomicLong();

                @Override
                public Thread newThread(final Runnable r) {
                    return new Thread(r, "KNIME-Container-Thread-" + m_threadCount.getAndIncrement());
                }
            });
        ASYNC_EXECUTORS.allowCoreThreadTimeOut(true);
    }

    /**
     * Whether to force a copy of any added blob. See {@link #setForceCopyOfBlobs(boolean)} for details.
     */
    private boolean m_forceCopyOfBlobs;

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
     * Whether this container handles rows sequentially, i.e. one row after another. Handling a row encompasses (1)
     * validation against a given table spec, (2) updating the table's domain, (3) checking for duplicates among row
     * keys, and (4) handling of blob and file store cells. Independent of this setting, the underlying {@link Buffer}
     * class always writes rows to disk sequentially, yet potentially asynchronously.
     */
    private final boolean m_forceSequentialRowHandling;

    /** Flag indicating the memory state. */
    boolean m_memoryLowState;

    /**
     * The index of the pending batch, i.e., the index of the next batch that has to be forwarded to the {@link Buffer}.
     */
    private final MutableLong m_pendingBatchIdx;

    /** The queue storing the {@link DataTableDomainCreator} used by the {@link ContainerRunnable}. */
    private final BlockingQueue<DataTableDomainCreator> m_domainUpdaterPool;

    /** The index of the current batch. */
    private long m_curBatchIdx;

    /** Map storing those rows that still need to be forwarded to the {@link Buffer}. */
    private final Map<Long, List<BlobSupportDataRow>> m_pendingBatchMap;

    /**
     * The current batch, i.e., a list of rows that have not yet been been verified nor added to the buffer. A
     * {@link DataRow} will be added to the current batch list, whenever {@link #addRowToTable(DataRow)} gets invoked.
     * Once the size of the current batch reaches {@link #m_batchSize} it is submitted to the {@link #ASYNC_EXECUTORS},
     * where each row is getting verified and finally are added to the buffer.
     */
    private List<DataRow> m_curBatch;

    /** The size of each batch submitted to the {@link #ASYNC_EXECUTORS} service. */
    private final int m_batchSize;

    /** The maximum number of threads used by this container. */
    private final int m_maxNumThreads;

    /**
     * Semaphore used to block the container until all {@link #m_maxNumThreads} {@link ContainerRunnable} are finished.
     */
    private final Semaphore m_numActiveContRunnables;

    /**
     * Semaphore used to block the producer in case that we have more than {@link #m_maxNumThreads} batches waiting to
     * be handed over to the {@link Buffer}.
     */
    private final Semaphore m_numPendingBatches;

    /** The maximum number of rows kept in memory. */
    private int m_maxRowsInMemory;

    /** Holds the keys of the added rows to check for duplicates. */
    private DuplicateChecker m_duplicateChecker;

    /** The tablespec of the return table. */
    private DataTableSpec m_spec;

    private DataTableDomainCreator m_domainCreator;

    /** repository for blob and filestore (de)serialization and table id handling */
    private IDataRepository m_repository;

    // TODO
    private final ILocalDataRepository m_localRepository;

    /**
     * A file store handler. It's lazy initialized in this class. The buffered data container sets the FSH of the
     * corresponding node. A plain data container will copy file store cells.
     */
    private IWriteFileStoreHandler m_fileStoreHandler;

    private BufferedContainerTable m_table;

    /**
     * @param spec
     * @param withForceSequentialRowHandling
     * @param repository
     */
    BufferedDataContainerDelegate(final DataTableSpec spec, final DataContainerSettings settings,
        final IDataRepository repository, final ILocalDataRepository localRepository,
        final IWriteFileStoreHandler fileStoreHandler) {
        CheckUtils.checkArgumentNotNull(spec, "Spec must not be null!");
        final int maxCellsInMemory = settings.getMaxCellsInMemory().orElse(DataContainerSettings.MAX_CELLS_IN_MEMORY);
        m_spec = spec;
        m_duplicateChecker = settings.createDuplicateChecker();
        m_forceSequentialRowHandling = settings.isForceSequentialRowHandling();
        m_batchSize = settings.getRowBatchSize();
        m_memoryLowState = false;
        if (m_forceSequentialRowHandling) {
            m_numActiveContRunnables = null;
            m_numPendingBatches = null;
            m_pendingBatchIdx = null;
            m_pendingBatchMap = null;
            m_writeThrowable = null;
            m_maxNumThreads = 0;
            m_domainUpdaterPool = null;
        } else {
            m_maxNumThreads = Math.min(settings.getMaxThreadsPerContainer(), ASYNC_EXECUTORS.getMaximumPoolSize());
            m_pendingBatchMap = new ConcurrentHashMap<>();
            m_numActiveContRunnables = new Semaphore(m_maxNumThreads);
            m_domainUpdaterPool = new ArrayBlockingQueue<>(m_maxNumThreads);
            m_numPendingBatches = new Semaphore(m_maxNumThreads);
            m_curBatch = new ArrayList<>(m_batchSize);
            m_pendingBatchIdx = new MutableLong();
            m_writeThrowable = new AtomicReference<Throwable>();
            m_curBatchIdx = 0;
        }
        m_domainCreator = settings.createDomainCreator(m_spec);
        m_size = 0;
        // how many rows will occupy MAX_CELLS_IN_MEMORY
        final int colCount = spec.getNumColumns();
        m_maxRowsInMemory = maxCellsInMemory / ((colCount > 0) ? colCount : 1);
        m_bufferCreator = settings.isEnableRowKeys() ? new BufferCreator(settings.getBufferSettings()) : new NoKeyBufferCreator();
        m_forceCopyOfBlobs = settings.isForceCopyOfBlobs();
        m_repository = repository;
        m_localRepository = localRepository;
        m_fileStoreHandler = fileStoreHandler;
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
     * Checks if any of the {@link ContainerRunnable} threw an exception.
     *
     */
    private void checkAsyncWriteThrowable() {
        Throwable t = m_writeThrowable.get();
        if (t != null) {
            m_pendingBatchMap.clear();
            try {
                waitForRunnableTermination();
            } catch (InterruptedException ie) {
                // nothing to do we already have t != null
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
    @Override
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
    boolean isOpen() {
        return m_table == null;
    }

    @Override
    public void flushRows() {
        if (m_table != null) {
            return;
        }
        if (!m_forceSequentialRowHandling) {
            try {
                if (m_writeThrowable.get() == null && !m_curBatch.isEmpty()) {
                    submit();
                }
                waitForRunnableTermination();
            } catch (final InterruptedException ie) {//NOSONAR
                m_writeThrowable.compareAndSet(null, ie);
            }
            checkAsyncWriteThrowable();
        }
    }

    /**
     * Closes container and creates table that can be accessed by <code>getTable()</code>. Successive calls of
     * <code>addRowToTable</code> will fail with an exception.
     *
     * @throws IllegalStateException If container is not open.
     * @throws DuplicateKeyException If the final check for duplicate row keys fails.
     * @throws DataContainerException If the duplicate check fails for an unknown IO problem
     */
    @Override
    public void close() {
        if (m_table!=null) {
            return;
        }
        if (m_buffer == null) {
            m_buffer = m_bufferCreator.createBuffer(m_spec, m_maxRowsInMemory, createInternalBufferID(), m_repository,
                m_localRepository, m_fileStoreHandler);
        }
        if (!m_forceSequentialRowHandling) {
            flushRows();
            for (final DataTableDomainCreator domainCreator : m_domainUpdaterPool) {
                m_domainCreator.merge(domainCreator);
            }
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
        m_table = new BufferedContainerTable(m_buffer);
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
    @Override
    public long size() {
        if (m_table!=null) {
            return m_table.getBuffer().size();
        }
        return m_size;
    }

    /**
     * Obtain a reference to the table that has been built up. This method throws an exception unless the container is
     * closed and therefore has a table available. This method is susceptible to resource leaks. Consider invoking
     * {@link DataContainer#getCloseableTable() getCloseableTable} instead. Alternatively, make sure to cast the table
     * to a {@link ContainerTable} and {@link ContainerTable#clear() clear} it to dispose underlying resources once it
     * is no longer needed.
     *
     * @return reference to the table that has been built up
     * @throws IllegalStateException if the container has not been closed yet or has already been disposed
     */
    @Override
    public ContainerTable getTable() {
        return m_table;
    }

    /**
     * Get the currently set DataTableSpec.
     *
     * @return The current spec.
     */
    @Override
    public DataTableSpec getTableSpec() {
        if (m_table!=null) {
            return m_table.getDataTableSpec();
        } else if (isOpen()) {
            return m_spec;
        }
        throw new IllegalStateException("Cannot get spec: container not open.");
    }

    @Override
    public void addRowToTable(final DataRow row) {
        if (!isOpen()) {
            throw new IllegalStateException("Cannot add row: container has not been initialized (opened).");
        }
        if (row == null) {
            throw new NullPointerException("Can't add null rows to container");
        }
        initBufferIfRequired();
        if (m_forceSequentialRowHandling) {
            addRowToTableSynchronously(row);
        } else {
            addRowToTableAsynchronously(row);
        }
        m_size += 1;
    }

    @Override
    public void clear() {
        if (m_table != null) {
            // also clears buffer
            m_table.clear();
        } else if (m_buffer != null) {
            m_buffer.clear();
        }
    }

    /**
     * Initializes the buffer if required.
     */
    private void initBufferIfRequired() {
        if (m_buffer == null) {
            final int bufID = createInternalBufferID();
            m_buffer = m_bufferCreator.createBuffer(m_spec, m_maxRowsInMemory, bufID, m_repository, m_localRepository,
                m_fileStoreHandler);
            if (m_buffer == null) {
                throw new NullPointerException("Implementation error, must not return a null buffer.");
            }
        }
    }

    /**
     * Adds the row to the table in a synchronous manner and reacts to memory alerts.
     *
     * @param row the row to be synchronously processed
     */
    private void addRowToTableSynchronously(final DataRow row) {
        if (MemoryAlertSystem.getInstanceUncollected().isMemoryLow()) {
            m_buffer.flushBuffer();
        }
        addRowToTableWrite(row);
    }

    /**
     * Adds the row to the table in a asynchronous/synchronous manner depending on the current memory state, see
     * {@link #m_memoryLowState}. Whenever we change into a low memory state we flush everything to disk and wait for
     * all {@link ContainerRunnable} to finish their execution, while blocking the data producer.
     *
     * @param row the row to be asynchronously processed
     */
    private void addRowToTableAsynchronously(final DataRow row) {
        checkAsyncWriteThrowable();
        try {
            if (MemoryAlertSystem.getInstanceUncollected().isMemoryLow()) {
                // write all keys to disk if necessary
                m_duplicateChecker.flushIfNecessary();
                // if we witness a low memory state the first time we flush everything to disk.
                // In case we are already in this state we switch to synchronous |writing", to ensure that
                // we release the memory as fast as possible.
                if (!m_memoryLowState) {
                    // flush the buffer, forces the buffer to synchronously write to disc from here on
                    m_buffer.flushBuffer();
                    // submit the pending batch
                    if (!m_curBatch.isEmpty()) {
                        submit();
                    }
                    // wait until all runnables are finished
                    waitForRunnableTermination();
                    // adjust the memory state
                    m_memoryLowState = true;
                    // we change to synchronous write so we need to ensure that the domain values order stays correct
                    // and that our ContainerRunnables can continue their work once we leave the low memory state
                    m_domainCreator.setBatchId(m_curBatchIdx++);
                    m_pendingBatchIdx.increment();
                }
                // write synchronously
                addRowToTableWrite(row);
            } else {
                m_memoryLowState = false;
                m_curBatch.add(row);
                if (m_curBatch.size() == m_batchSize) {
                    submit();
                }
            }
        } catch (final IOException | InterruptedException e) {
            m_writeThrowable.compareAndSet(null, e);
        }
    }

    private void waitForRunnableTermination() throws InterruptedException {
        m_numActiveContRunnables.acquire(m_maxNumThreads);
        m_numActiveContRunnables.release(m_maxNumThreads);
    }

    /**
     * Submits the current batch to the {@link #ASYNC_EXECUTORS} service.
     *
     * @throws InterruptedException if an interrupted occured
     */
    private void submit() throws InterruptedException {
        // wait until we are allowed to submit a new runnable
        m_numPendingBatches.acquire();
        m_numActiveContRunnables.acquire();
        // poll can only return null if we never had #nThreads ContainerRunnables at the same
        // time queued for execution or none of the already submitted Runnables has already finished
        // it's computation
        DataTableDomainCreator domainCreator = m_domainUpdaterPool.poll();
        if (domainCreator == null) {
            domainCreator = new DataTableDomainCreator(m_domainCreator);
            domainCreator.setMaxPossibleValues(m_domainCreator.getMaxPossibleValues());
        }
        ASYNC_EXECUTORS.execute(new ContainerRunnable(domainCreator, m_curBatch, m_curBatchIdx++));
        // reset batch
        m_curBatch = new ArrayList<>(m_batchSize);
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
     * This implementation returns -1 ({@link #DataContainer.NOT_IN_WORKFLOW_BUFFER}.
     *
     * @return -1 or a unique buffer ID.
     */
    private int createInternalBufferID() {
        return m_repository instanceof NotInWorkflowDataRepository ? DataContainer.NOT_IN_WORKFLOW_BUFFER
            : m_repository.generateNewID();
    }

    /**
     * Used in tests.
     *
     * @return underlying buffer (or null if not initialized after restore).
     * @noreference This method is not intended to be referenced by clients.
     */
    final Buffer getBuffer() {
        return m_buffer;
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
    private void addRowKeyForDuplicateCheck(final RowKey key) {
        try {
            m_duplicateChecker.addKey(key.toString());
        } catch (IOException ioe) {
            throw new DataContainerException(
                ioe.getClass().getSimpleName() + " while checking for duplicate row IDs: " + ioe.getMessage(), ioe);
        } catch (DuplicateKeyException dke) {
            throw new DuplicateKeyException("Encountered duplicate row ID \"" + dke.getKey() + "\"", dke.getKey());
        }
    }

    /**
     * Returns <code>true</code> if the given argument table has been created by a DataContainer, <code>false</code>
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
    private final class ContainerRunnable implements Runnable {

        /** The data table domain creator. */
        private final DataTableDomainCreator m_dataTableDomainCreator;

        /** The batch of rows to be processed. */
        private final List<DataRow> m_rows;

        /** The current batch index. */
        private final long m_batchIdx;

        private final NodeContext m_nodeContext;

        /**
         * Constructor.
         *
         * @param domainCreator the domain creator
         * @param rows the batch of rows to be processed
         * @param batchIdx the batch index
         */
        ContainerRunnable(final DataTableDomainCreator domainCreator, final List<DataRow> rows, final long batchIdx) {
            m_rows = rows;
            m_batchIdx = batchIdx;
            m_dataTableDomainCreator = domainCreator;
            m_dataTableDomainCreator.setBatchId(m_batchIdx);
            /**
             * The node context may be null if the DataContainer has been created outside of a node's context (e.g., in
             * unit tests). This is also the reason why this class does not extend the RunnableWithContext class.
             */
            m_nodeContext = NodeContext.getContext();
        }

        @Override
        public final void run() {
            NodeContext.pushContext(m_nodeContext);
            try {
                if (m_writeThrowable.get() == null) {
                    final List<BlobSupportDataRow> blobRows = new ArrayList<>(m_rows.size());
                    for (final DataRow row : m_rows) {
                        validateSpecCompatiblity(row);
                        m_dataTableDomainCreator.updateDomain(row);
                        addRowKeyForDuplicateCheck(row.getKey());
                        blobRows.add(m_buffer.saveBlobsAndFileStores(row, m_forceCopyOfBlobs));
                    }
                    boolean addRows;
                    synchronized (m_pendingBatchIdx) {
                        addRows = m_batchIdx == m_pendingBatchIdx.longValue();
                        if (!addRows) {
                            m_pendingBatchMap.put(m_batchIdx, blobRows);
                        }
                    }
                    if (addRows) {
                        addRows(blobRows);
                        m_numPendingBatches.release();
                        while (isNextPendingBatchExistent()) {
                            addRows(m_pendingBatchMap.remove(m_pendingBatchIdx.longValue()));
                            m_numPendingBatches.release();
                        }
                    }

                }
            } catch (final Throwable t) {
                m_writeThrowable.compareAndSet(null, t);
                // Potential deadlock cause by the following scenario.
                // Initial condition:
                //      1. ContainerRunnables:
                //          1.1 The max number of container runnables are submitted to the pool
                //          1.2 No batch has been handed over to the buffer (m_numPendingBatches no available permits)
                //      2. Producer:
                //          2.1. Hasn't seen an exception so far wants to submit another batch and tries to acquire a
                //              permit from m_numPendingBatches
                //      3. The container runnable whose index equals the current pending batch index crashes before
                //          it can hand anything to the buffer and finally give its permit back to m_numPendingBatches
                // Result: The producer waits forever to acquire a permit from m_numPendingBatches. Hence, we have to
                //          release at least one permit here
                // Note: The producer will submit another ContainerRunnable, however this will do nothing as
                //          m_writeThrowable.get != null
                if (m_batchIdx == m_pendingBatchIdx.longValue()) {
                    m_numPendingBatches.release();
                }
            } finally {
                m_domainUpdaterPool.add(m_dataTableDomainCreator);
                m_numActiveContRunnables.release();
                NodeContext.removeLastContext();
            }
        }

        /**
         * Forwards the given list of {@link BlobSupportDataRow} to the buffer
         *
         * @param blobRows the rows to be forwarded to the buffer
         * @throws IOException - if the buffer cannot write the rows to disc
         */
        private void addRows(final List<BlobSupportDataRow> blobRows) throws IOException {
            for (final BlobSupportDataRow row : blobRows) {
                m_buffer.addBlobSupportDataRow(row);
            }
        }

        /**
         * Checks whether there is another batch of {@link BlobSupportDataRow} existent that has to be forwarded to the
         * buffer.
         *
         * @return {@code true} if another batch has to be forwarded to the buffer, {@code false} otherwise
         *
         */
        private boolean isNextPendingBatchExistent() {
            synchronized (m_pendingBatchIdx) {
                m_pendingBatchIdx.increment();
                return m_pendingBatchMap.containsKey(m_pendingBatchIdx.longValue());
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
         *
         * @return A newly created buffer.
         */
        Buffer createBuffer(final DataTableSpec spec, final int rowsInMemory, final int bufferID,
            final IDataRepository dataRepository, final ILocalDataRepository localTableRep,
            final IWriteFileStoreHandler fileStoreHandler) {
            return new Buffer(spec, rowsInMemory, bufferID, dataRepository, localTableRep, fileStoreHandler,
                m_bufferSettings);
        }
    }

    /** Creates NoKeyBuffer objects rather then Buffer objects. */
    static class NoKeyBufferCreator extends BufferCreator {

        /** {@inheritDoc} */
        @Override
        Buffer createBuffer(final DataTableSpec spec, final int rowsInMemory, final int bufferID,
            final IDataRepository dataRepository, final ILocalDataRepository localTableRep,
            final IWriteFileStoreHandler fileStoreHandler) {
            return new NoKeyBuffer(spec, rowsInMemory, bufferID, dataRepository, localTableRep, fileStoreHandler);
        }

        /** {@inheritDoc} */
        @Override
        Buffer createBuffer(final File binFile, final File blobDir, final File fileStoreDir, final DataTableSpec spec,
            final InputStream metaIn, final int bufID, final IDataRepository dataRepository) throws IOException {
            return new NoKeyBuffer(binFile, blobDir, spec, metaIn, bufID, dataRepository);
        }
    }

    /** Used in write/readFromZip: Name of the zip entry containing the spec. */
    static final String ZIP_ENTRY_SPEC = "spec.xml";

    /** Used in write/readFromZip: Config entry: The spec of the table. */
    static final String CFG_TABLESPEC = "table.spec";

    /**
     * @param in
     * @return
     * @throws IOException
     */
    public static ContainerTable readFromStream(final InputStream in) throws IOException {
        // mimic the behavior of readFromZip(ReferencedFile)
        CopyOnAccessTask coa =
            new CopyOnAccessTask(/*File*/null, null, -1, NotInWorkflowDataRepository.newInstance(), true);
        // executing the createBuffer() method will start the copying process
        Buffer buffer = coa.createBuffer(in);
        return new BufferedContainerTable(buffer);
    }

    /**
     * @param zipFileRef
     * @param rowKeys
     * @return
     * @throws IOException
     */
    public static ContainerTable readFromZip(final ReferencedFile zipFileRef, final boolean rowKeys)
        throws IOException {
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
            new CopyOnAccessTask(zipFileRef, null, -1, NotInWorkflowDataRepository.newInstance(), rowKeys);
        // executing the createBuffer() method will start the copying process
        Buffer buffer = coa.createBuffer();
        return new BufferedContainerTable(buffer);
    }

    /**
     * @param zipFile
     * @param spec
     * @param bufferID
     * @param dataRepository
     * @return
     */
    public static ContainerTable readFromZipDelayed(final ReferencedFile zipFile, final DataTableSpec spec,
        final int bufferID, final WorkflowDataRepository dataRepository) {
        CopyOnAccessTask t = new CopyOnAccessTask(zipFile, spec, bufferID, dataRepository, true);
        return readFromZipDelayed(t, spec);
    }

    /**
     * @param c
     * @param spec
     * @return
     */
    public static ContainerTable readFromZipDelayed(final CopyOnAccessTask c, final DataTableSpec spec) {
        return new BufferedContainerTable(c, spec);
    }

    /**
     * @param table
     * @param out
     * @param exec
     * @return
     * @throws IOException
     * @throws CanceledExecutionException
     */
    public static void writeToStream(final DataTable table, final OutputStream out, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        Buffer buf;
        ExecutionMonitor e = exec;
        boolean canUseBuffer = table instanceof ContainerTable;
        if (canUseBuffer) {
            Buffer b = ((BufferedContainerTable)table).getBuffer();
            if (b.containsBlobCells() && b.getBufferID() != -1) {
                canUseBuffer = false;
            }
        }
        if (canUseBuffer) {
            buf = ((BufferedContainerTable)table).getBuffer();
        } else {
            exec.setMessage("Archiving table");
            e = exec.createSubProgress(0.8);
            buf = new Buffer(table.getDataTableSpec(), 0, -1, NotInWorkflowDataRepository.newInstance(),
                new DefaultLocalDataRepository(), NotInWorkflowWriteFileStoreHandler.create());
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
}

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.WorkflowDataRepository;
import org.knime.core.util.DuplicateKeyException;
import org.knime.core.util.FileUtil;

/**
 * Buffer that collects <code>DataRow</code> objects and creates a <code>DataTable</code> on request. This data
 * structure is useful if the number of rows is not known in advance.
 *
 * <p>
 * Usage: Create a container with a given spec (matching the rows being added later on, add the data using the
 * <code>addRowToTable(DataRow)</code> method and finally close it with <code>close()</code>. You can access the table
 * via <code>getCloseableTable()</code>.
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
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class DataContainer implements RowAppender {

    /**
     * ID for buffers, which are not part of the workflow (no BufferedDataTable).
     *
     * @since 3.7
     */
    public static final int NOT_IN_WORKFLOW_BUFFER = -1;

    /**
     * Number of cells that are cached without being written to the temp file (see Buffer implementation); It defaults
     * value can be changed using the java property {@link KNIMEConstants#PROPERTY_CELLS_IN_MEMORY}.
     *
     * @deprecated access via {@link DataContainerSettings#getDefault()}
     */
    @Deprecated
    public static final int MAX_CELLS_IN_MEMORY;

    /**
     * The actual number of possible values being kept at most. Its default value can be changed using the java property
     * {@link KNIMEConstants#PROPERTY_DOMAIN_MAX_POSSIBLE_VALUES}.
     *
     * @since 2.10
     * @deprecated access via {@link DataContainerSettings#getDefault()}
     */
    @Deprecated
    public static final int MAX_POSSIBLE_VALUES;

    static {
        final DataContainerSettings defaults = DataContainerSettings.getDefault();
        MAX_CELLS_IN_MEMORY = defaults.getMaxCellsInMemory();
        MAX_POSSIBLE_VALUES = defaults.getMaxDomainValues();
    }

    private RowContainer m_rowContainer;

    private DataTableSpec m_spec;

    private final Map<Integer, ContainerTable> m_localRepository;
    /**
     * Consider using {@link ExecutionContext#createDataContainer(DataTableSpec)} instead of invoking this constructor
     * directly.
     * <p>
     * Opens the container so that rows can be added by <code>addRowToTable(DataRow)</code>. The table spec of the
     * resulting table (the one being returned by <code>getCloseableTable()</code>) will have a valid column domain.
     * That means, while rows are added to the container, the domain of each column is adjusted.
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
     * Consider using {@link ExecutionContext#createDataContainer(DataTableSpec, boolean)} instead of invoking this
     * constructor directly.
     * <p>
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
     * Consider using {@link ExecutionContext#createDataContainer(DataTableSpec, boolean, int)} instead of invoking this
     * constructor directly.
     * <p>
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
     * @param repository the data repository
     * @param localTableRepository the local table repository
     * @param fileStoreHandler a filestore handler
     * @param forceCopyOfBlobs true, if blobs should be copied
     * @param rowKeys if <code>true</code>, {@link RowKey}s are expected to be part of a {@link DataRow}.
     * @throws IllegalArgumentException If <code>maxCellsInMemory</code> &lt; 0 or the spec is null
     */
    protected DataContainer(final DataTableSpec spec, final boolean initDomain, final int maxCellsInMemory,
        final boolean forceSynchronousIO, final IDataRepository repository,
        final Map<Integer, ContainerTable> localTableRepository, final IWriteFileStoreHandler fileStoreHandler,
        final boolean forceCopyOfBlobs, final boolean rowKeys) {
        this(spec,
            DataContainerSettings.getDefault().withInitializedDomain(initDomain).withMaxCellsInMemory(maxCellsInMemory)
                .withForceSequentialRowHandling(
                    forceSynchronousIO || DataContainerSettings.getDefault().isForceSequentialRowHandling()),
            repository, localTableRepository, fileStoreHandler, forceCopyOfBlobs, rowKeys);
    }

    /**
     * Opens the container so that rows can be added by <code>addRowToTable(DataRow)</code>.
     *
     * @param spec Table spec of the final table. Rows that are added to the container must comply with this spec.
     * @param settings the container settings
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public DataContainer(final DataTableSpec spec, final DataContainerSettings settings) {
        this(spec, settings, NotInWorkflowDataRepository.newInstance(), new HashMap<>(), null, false, true);
    }

    /**
     * Used for testing purposes
     *
     * @param spec Table spec of the final table. Rows that are added to the container must comply with this spec.
     * @param initDomain if set to true, the column domains in the container are initialized with the domains from spec.
     * @param maxCellsInMemory Maximum count of cells in memory before swapping.
     * @param forceSynchronousIO Whether to force synchronous IO. If this property is false, it's using the default
     *            (which is false unless specified otherwise through {@link KNIMEConstants#PROPERTY_SYNCHRONOUS_IO})
     */
    protected DataContainer(final DataTableSpec spec, final boolean initDomain, final int maxCellsInMemory,
        final boolean forceSynchronousIO) {
        this(spec,
            DataContainerSettings.getDefault().withInitializedDomain(initDomain).withMaxCellsInMemory(maxCellsInMemory)
                .withForceSequentialRowHandling(
                    forceSynchronousIO || DataContainerSettings.getDefault().isForceSequentialRowHandling()));
    }

    /**
     * @param spec
     * @param withForceSequentialRowHandling
     * @param repository
     */
    private DataContainer(final DataTableSpec spec, final DataContainerSettings settings,
        final IDataRepository repository, final Map<Integer, ContainerTable> localRepository,
        final IWriteFileStoreHandler fileStoreHandler, final boolean forceCopyOfBlobs, final boolean rowKeys) {
        m_spec = spec;
        m_localRepository = localRepository;

        m_rowContainer = new BufferedRowContainer(spec, settings, repository, localRepository,
            initFileStoreHandler(fileStoreHandler, repository), forceCopyOfBlobs, rowKeys);
    }

    /**
     * Obtain a one-time-use table that should be used in a <code>try</code>-with-resources block. The resources
     * underlying the table and the data container are disposed when exiting the <code>try</code>-with-resources block.
     * This method throws an exception unless the container is closed and therefore has a table available. It also
     * throws an exception if the container or its underlying resources have already been disposed. If you wish to
     * obtain the table multiple times, invoke {@link DataContainer#getTable() getTable} instead.
     *
     * @return reference to a one-time-use table that, after use, disposes the resources underlying this container
     * @throws IllegalStateException if the container has not been closed yet or has already been disposed
     * @since 4.2
     */
    public CloseableTable getCloseableTable() {
        final ContainerTable delegate = getBufferedTable();
        return new CloseableTable() {
            @Override
            public void close() {
                delegate.clear();
            }

            @Override
            public RowIterator iterator() {
                return delegate.iterator();
            }

            @Override
            public DataTableSpec getDataTableSpec() {
                return delegate.getDataTableSpec();
            }
        };
    }

    /**
     * Disposes this container and all underlying resources.
     *
     * @since 3.1
     * @deprecated After the container is {@link #close() closed} and its table has been {@link #getTable() obtained},
     *             it has done its job and you should not dispose its resources directly. Instead, cast the table to a
     *             {@link ContainerTable} and {@link ContainerTable#clear() clear} it to dispose underlying resources
     *             once the table is no longer needed.
     */
    @Deprecated
    public void dispose() {
        m_rowContainer.clear();
    }

    @Override
    public void addRowToTable(final DataRow row) {
        m_rowContainer.addRowToTable(row);
    }

    /**
     * Returns the table holding the data. This method is identical to the getTable() method but is more specific with
     * respec to the return type. It's used in derived classes.
     *
     * @return The table underlying this container.
     * @throws IllegalStateException If <code>isClosed()</code> returns <code>false</code>
     */
    protected final ContainerTable getBufferedTable() {
        return m_rowContainer.getTable();
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
    public DataTable getTable() {
        if (!isClosed()) {
            throw new IllegalStateException("Cannot get table: container is not closed.");
        }
        return m_rowContainer.getTable();
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
        m_rowContainer.close();
        ContainerTable table = m_rowContainer.getTable();
        m_localRepository.put(table.getTableId(), table);
    }

    /**
     * Returns <code>true</code> if table has been closed and <code>getTable()</code> will return a
     * <code>DataTable</code> object.
     *
     * @return <code>true</code> if table is available, <code>false</code> otherwise.
     */
    public boolean isClosed() {
        return m_rowContainer.isClosed();
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
     * Get the number of rows that have been added so far. (How often has <code>addRowToTable</code> been called.)
     *
     * @return The number of rows in the container.
     * @throws IllegalStateException If container is not open.
     * @since 3.0
     */
    public long size() {
        return m_rowContainer.size();
    }

    /* Used in tests */
    RowContainer getRowContainer() {
        return m_rowContainer;
    }

    /**
     * Define a new threshold for number of possible values to memorize. It makes sense to call this method before any
     * rows are added.
     *
     * @param maxPossibleValues The new number.
     * @throws IllegalArgumentException If the value &lt; 0
     */
    public void setMaxPossibleValues(final int maxPossibleValues) {
        m_rowContainer.setMaxPossibleValues(maxPossibleValues);
    }

    /**
     * Get the currently set DataTableSpec.
     *
     * @return The current spec.
     */
    public DataTableSpec getTableSpec() {
        if (isClosed()) {
            return m_rowContainer.getTableSpec();
        } else if (isOpen()) {
            return m_spec;
        }
        throw new IllegalStateException("Cannot get spec: container not open.");
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
        DataContainer container = new DataContainer(table.getDataTableSpec(), true, maxCellsInMemory);
        int row = 0;
        try {
            for (RowIterator it = table.iterator(); it.hasNext(); row++) {
                DataRow next = it.next();
                exec.setMessage("Caching row #" + (row + 1) + " (\"" + next.getKey() + "\")");
                exec.checkCanceled();
                container.addRowToTable(next);
            }
        } finally {
            container.close();
        }
        return container.getTable();
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

    private static IWriteFileStoreHandler initFileStoreHandler(final IWriteFileStoreHandler fileStoreHandler,
        final IDataRepository repository) {
        IWriteFileStoreHandler nonNull = fileStoreHandler;
        if (nonNull == null) {
            nonNull = NotInWorkflowWriteFileStoreHandler.create();
            nonNull.addToRepository(repository);
        }
        return nonNull;
    }

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
        // TODO switch according to table implementation.
       BufferedRowContainer.writeToStream(table, out, exec);
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
        // TODO use file ending to determine format?
        return readFromZip(new ReferencedFile(zipFile), true);
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
        // TODO how do we know which deserializer to use?
        return BufferedRowContainer.readFromStream(in);
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
    static ContainerTable readFromZip(final ReferencedFile zipFileRef, final boolean rowKeys) throws IOException {
        return BufferedRowContainer.readFromZip(zipFileRef, rowKeys);
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
        return BufferedRowContainer.readFromZipDelayed(zipFile, spec, bufferID, dataRepository);
    }

    /**
     * Used in {@link org.knime.core.node.BufferedDataContainer} to read the tables from the workspace location.
     *
     * @param c The factory that create the Buffer instance that the returned table reads from.
     * @param spec The DTS for the table.
     * @return Table contained in <code>zipFile</code>.
     */
    static ContainerTable readFromZipDelayed(final CopyOnAccessTask c, final DataTableSpec spec) {
        return BufferedRowContainer.readFromZipDelayed(c, spec);
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
        return createTempFile(FileUtil.getWorkflowTempDir(), suffix);
    }

    static final File createTempFile(final File dir, final String suffix) throws IOException {
        String date;
        synchronized (DATE_FORMAT) {
            date = DATE_FORMAT.format(new Date());
        }
        String fileName = "knime_container_" + date + "_";
        File f = FileUtil.createTempFile(fileName, suffix, dir, true);
        f.deleteOnExit();
        return f;
    }
}
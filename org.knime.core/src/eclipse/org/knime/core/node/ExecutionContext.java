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
 *
 */
package org.knime.core.node;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.RowKey;
import org.knime.core.data.TableBackend;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.container.DefaultLocalDataRepository;
import org.knime.core.data.container.ILocalDataRepository;
import org.knime.core.data.container.VoidTable;
import org.knime.core.data.container.WrappedTable;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.filestore.internal.ROWriteFileStoreHandler;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.KNIMEJob;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.node.workflow.WorkflowTableBackendSettings;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeOutputNodeModel;
import org.knime.core.table.row.Selection;
import org.knime.core.util.DuplicateKeyException;

/**
 * An <code>ExecutionContext</code> provides storage capacities during a
 * {@link org.knime.core.node.NodeModel#execute( BufferedDataTable[],
 * ExecutionContext) NodeModel's execution}. Furthermore it allows to report
 * progress of the execution and to check for cancellation events.
 *
 * Any derived class of <code>NodeModel</code> that has at least one data
 * output will need to create a <code>BufferedDataTable</code> as return value
 * of the execute method. These <code>BufferedDataTable</code> can only be
 * created by means of an <code>ExecutionContext</code> using one of the
 * <code>create...</code> methods. There are basically three different ways to
 * create the output table:
 * <dl>
 * <dt><a name="new_data"></a><strong>New data</strong></dt>
 * <dd>Use the {@link #createDataContainer(DataTableSpec)} method to create a
 * container to which rows are sequentially added. The final result will be
 * available through the container's {@link BufferedDataContainer#getTable()}
 * method. Alternatively you can also use the
 * {@link #createBufferedDataTable(DataTable, ExecutionMonitor)} method which
 * will traverse the argument table and cache everything. These method shall be
 * used when the entire output must be cached (thus also resulting in using more
 * disc space when the workflow is saved). </dd>
 * <dt><a name="new_column"></a><strong>Some columns of the input have changed</strong></dt>
 * <dd>This is the case, for instance when you just append a single column to
 * the input table (or filter/replace existing columns from it). The method to
 * use here is {@link #createColumnRearrangeTable(BufferedDataTable,
 * ColumnRearranger, ExecutionMonitor)}. When the workflow is saved, only the
 * columns that have changed are stored to disc.</dd>
 * <dt><a name="new_spec"></a><strong>The table spec of the input changes</strong></dt>
 * <dd>This happens for nodes that rename a column or add some properties to
 * the table spec. The input data itself is left untouched. Use the
 * {@link #createSpecReplacerTable(BufferedDataTable, DataTableSpec)} here.</dd>
 * </dl>
 *
 * <p>Apart from creating BufferedDataTable, objects of this class are also
 * responsible to report progress information. See the super class for more
 * information.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ExecutionContext extends ExecutionMonitor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExecutionContext.class);

    private final Node m_node;
    private final MemoryPolicy m_memoryPolicy;
    private final ILocalDataRepository m_localTableRepository;
    private final IWriteFileStoreHandler m_fileStoreHandler;
    private final IDataRepository m_dataRepository;

    /** Creates new object based on a progress monitor and a node as parent
     * of any created buffered data table.
     * @param progMon To report progress to.
     * @param node The parent of any BufferedDataTable being created.
     * themselves; used internally to identify tables that serialize blob cells.
     * @deprecated Use the constructor with a table repository argument instead.
     *             This constructor potentially does not support serialization
     *             of blobs.
     */
    @Deprecated
    public ExecutionContext(final NodeProgressMonitor progMon, final Node node) {
        // as it is deprecated we don't introduce an argument for mem policy
        this(progMon, node, MemoryPolicy.CacheSmallInMemory, NotInWorkflowDataRepository.newInstance());
    }

    /**
     * Creates new object based on a progress monitor and a node as parent of any created buffered data table.
     *
     * @param progMon To report progress to.
     * @param node The parent of any BufferedDataTable being created.
     * @param policy the policy according to which created BufferedDataTables behave
     * @param tableRepository ignored
     * @deprecated This constructor ignores the last argument and calls
     *             {@link #ExecutionContext(NodeProgressMonitor, Node, MemoryPolicy, IDataRepository)}
     */
    @Deprecated
    public ExecutionContext(final NodeProgressMonitor progMon, final Node node,
        final MemoryPolicy policy,
        final HashMap<Integer, ContainerTable> tableRepository) {
        this(progMon, node, policy, NotInWorkflowDataRepository.newInstance());
        CheckUtils.checkArgument(tableRepository.isEmpty(),
            "Not to be called with non-empty table repository (%d element(s))", tableRepository.size());
    }

    /**
     * Creates new object based on a progress monitor and a node as parent of
     * any created buffered data table.
     *
     * @param progMon To report progress to.
     * @param node The parent of any BufferedDataTable being created.
     * @param policy the policy according to which created BufferedDataTables
     *            behave
     * @param dataRepository for workflow global blob, filestore and table handling/lookup
     * @since 3.7
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public ExecutionContext(final NodeProgressMonitor progMon, final Node node,
            final MemoryPolicy policy, final IDataRepository dataRepository) {
        this(progMon, node, policy, dataRepository, new DefaultLocalDataRepository(),
            (node.getFileStoreHandler() instanceof IWriteFileStoreHandler
                ? (IWriteFileStoreHandler)node.getFileStoreHandler() : null));
    }

    /** Creates execution context with all required arguments. It's used
     * internally to also provided the execution context local table repository.
     *
     * @param progMon see other constructor.
     * @param node see other constructor.
     * @param policy see other constructor.
     * @param dataRepository see other constructor
     * @param localTableRepository execution context local table. This argument
     * is non-null only if this is a sub execution context (inheriting table
     * repository from parent).
     * @param tableRepository see other constructor.
     */
    private ExecutionContext(final NodeProgressMonitor progMon, final Node node, final MemoryPolicy policy,
        final IDataRepository dataRepository, final ILocalDataRepository localTableRepository,
        final IWriteFileStoreHandler fileStoreHandler) {
        super(progMon);
        m_node = CheckUtils.checkArgumentNotNull(node);
        if (fileStoreHandler == null) {
            LOGGER.debug("No file store handler set on \"" + m_node.getName()
                    + "\" (possibly running in 3rd party executor)");
            NotInWorkflowDataRepository repo = NotInWorkflowDataRepository.newInstance();
            m_fileStoreHandler = new ROWriteFileStoreHandler(repo);
        } else {
            m_fileStoreHandler = fileStoreHandler;
        }
        m_dataRepository = CheckUtils.checkArgumentNotNull(dataRepository);
        m_memoryPolicy = policy;
        m_localTableRepository = localTableRepository;
    }

    /** Creates a FileStore handle during execution that can be used to
     * instantiate a {@link FileStoreCell} and fill a table.
     * @param relativePath Name of the file/directory. The file object will
     * not be created. The name must not start with a '.' and it must not be
     * a nested directory (though clients can create sub directories in the
     * file represented by the returned file store object).
     * @return a new file store object
     * @throws IOException if the name is invalid (e.g. starts with a dot)
     * @throws DuplicateKeyException If the name was already used in a previous
     * indication.
     * @since 2.6
     * @noreference Pending API. Feel free to use the method but keep in mind
     * that it might change in a future version of KNIME.
     */
    public FileStore createFileStore(final String relativePath)
        throws IOException {
        return m_fileStoreHandler.createFileStore(relativePath);
    }

    /**
     * Caches the table argument and returns a reference to a BufferedDataTable wrapping the content. When saving the
     * workflow, the entire data is written to disc. This method is provided for convenience. (All it does is to create
     * a BufferedDataContainer, adding the rows to it and returning a handle to it.) <br />
     * <br />
     * Note: If table is already a BufferedDataTable it is simply returned.
     * <p>
     * This method refers to the first way of storing data, see <a href="#new_data">here</a>.
     *
     * @param table The table to cache.
     * @param subProgressMon The execution monitor to report progress to. In most cases this is the object on which this
     *            method is invoked. It may however be an sub progress monitor.
     * @return A table ready to be returned in the execute method.
     * @throws CanceledExecutionException If canceled.
     */
    public BufferedDataTable createBufferedDataTable(final DataTable table, final ExecutionMonitor subProgressMon)
        throws CanceledExecutionException {
        if (table instanceof BufferedDataTable) {
            return (BufferedDataTable)table;
        }
        final var numRows = table instanceof KnowsRowCountTable rowCountTable ? rowCountTable.size() : 0;
        final var c = createDataContainer(table.getDataTableSpec(), true);
        var idx = 0l;
        try {
            for (final var row : table) {
                final var finalIdx = idx + 1;
                if (numRows > 0) {
                    subProgressMon.setProgress(finalIdx / (double)numRows,
                        () -> String.format("Caching row %d of %d (\"%s\")", finalIdx, numRows, row.getKey()));
                } else {
                    subProgressMon.setMessage(() -> String.format("Caching row %d (\"%s\")", finalIdx, row.getKey()));
                }
                subProgressMon.checkCanceled();
                c.addRowToTable(row);
                idx++;
            }
        } finally {
            c.close();
        }
        final var out = c.getTable();
        out.setOwnerRecursively(m_node);
        return out;
    }

    /** Performs the creation of buffered datatables for an array of DataTables.
     * @param tables The tables to cache.
     * @param exec The execution monitor for progress, cancel
     * @return The cached array of tables.
     * @throws CanceledExecutionException If canceled.
     * @see #createBufferedDataTable(DataTable, ExecutionMonitor)
     */
    public BufferedDataTable[] createBufferedDataTables(
            final DataTable[] tables, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        BufferedDataTable[] temp = new BufferedDataTable[tables.length];
        for (int i = 0; i < tables.length; i++) {
            temp[i] = createBufferedDataTable(tables[i],
                    exec.createSubProgress(1.0 / tables.length));
        }
        return temp;

    }

    /**
     * Creates a container to which rows can be added. Use this method if
     * you sequentially generate new rows. Add those by using the
     * <code>addRow(DataRow)</code> method and finally close the container and
     * get the result by invoking <code>getTable()</code>. All rows will be
     * cached.
     * <p>This method refers to the first way of storing data,
     * see <a href="#new_data">here</a>.
     * @param spec The spec to open the container.
     * @return A container to which rows can be added and which provides
     * the <code>BufferedDataTable</code>.
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    public BufferedDataContainer createDataContainer(final DataTableSpec spec) {
        return createDataContainer(spec, true);
    }

    /**
     * Creates a container to which rows can be added. Use this method if
     * you sequentially generate new rows. Add those by using the
     * <code>addRow(DataRow)</code> method and finally close the container and
     * get the result by invoking <code>getTable()</code>. All rows will be
     * cached.
     * <p>This method refers to the first way of storing data,
     * see <a href="#new_data">here</a>.
     * @param spec The spec to open the container.
     * @param initDomain If the domain information from the argument shall
     * be used to initialize the domain (min, max, possible values). If false,
     * the domain will be determined on the fly.
     * @return A container to which rows can be added and which provides
     * the <code>BufferedDataTable</code>.
     * @throws NullPointerException If the spec argument is <code>null</code>.
     */
    public BufferedDataContainer createDataContainer(final DataTableSpec spec,
            final boolean initDomain) {
        return createDataContainer(spec, initDomain, -1);
    }

    /**
     * Creates a container to which rows can be added, overwriting the nodes {@link IWriteFileStoreHandler}.
     * This method has the same behavior as {@link #createDataContainer(DataTableSpec)} except that the provided
     * {@link IWriteFileStoreHandler} is used. This is useful e.g. if the container is created to serve as a preview
     * even while this nodes' fileStoreHandler is closed.
     *
     * @param spec The spec to open the container.
     * @param writeFileStoreHandler The {@link IWriteFileStoreHandler} to use instead of the one associated with this
     *            {@link ExecutionContext}, useful e.g. if the container is only used as a preview.
     * @return A container to which rows can be added and which provides the <code>BufferedDataTable</code>.
     * @throws NullPointerException If the spec argument is <code>null</code>.
     * @since 5.2
     * @noreference This method is not intended to be referenced by clients.
     */
    BufferedDataContainer createDataContainer(final DataTableSpec spec,
        final IWriteFileStoreHandler writeFileStoreHandler) {
        return createDataContainer(spec, true, -1, true, writeFileStoreHandler);
    }

    /**
     * Creates a container to which rows can be added, overwriting the node's memory policy. This method has the same
     * behavior as {@link #createDataContainer(DataTableSpec, boolean)} except for the last argument
     * <code>maxCellsInMemory</code>. It controls the memory policy of the data container (which is otherwise controlled
     * by a user setting in the dialog).
     *
     * <p>
     * <b>Note:</b> It's strongly advised to use {@link #createDataContainer(DataTableSpec, boolean)} instead of this
     * method as the above method realizes the memory policy specified by the user. Use this method only if you have
     * good reasons to do so (for instance if you create many containers, whose default memory options would yield a
     * high accumulated memory consumption).
     *
     * @param spec The spec to open the container.
     * @param initDomain If the domain information from the argument shall be used to initialize the domain (min, max,
     *            possible values). If false, the domain will be determined on the fly.
     * @param maxCellsInMemory Number of cells to be kept in memory, especially 0 forces the table to write to disk
     *            immediately. A value smaller than 0 will respect the user setting (as defined by the accompanying
     *            node).
     * @return A container to which rows can be added and which provides the <code>BufferedDataTable</code>.
     * @throws NullPointerException If the spec argument is <code>null</code>.
     */
    public BufferedDataContainer createDataContainer(final DataTableSpec spec, final boolean initDomain,
        final int maxCellsInMemory) {
        return createDataContainer(spec, initDomain, maxCellsInMemory, true);
    }

    /**
     * Creates a container to which rows can be added, overwriting the
     * node's memory policy. This method has the same behavior as
     * {@link #createDataContainer(DataTableSpec, boolean)} except for the
     * last argument <code>maxCellsInMemory</code>. It controls the memory
     * policy of the data container (which is otherwise controlled by a user
     * setting in the dialog).
     *
     * <p>
     * <b>Note:</b> It's strongly advised to use
     * {@link #createDataContainer(DataTableSpec, boolean)} instead of this
     * method as the above method realizes the memory policy specified by the
     * user. Use this method only if you have good reasons to do so
     * (for instance if you create many containers, whose default memory
     * options would yield a high accumulated memory consumption).
     * @param spec The spec to open the container.
     * @param initDomain If the domain information from the argument shall
     * be used to initialize the domain (min, max, possible values). If false,
     * the domain will be determined on the fly.
     * @param maxCellsInMemory Number of cells to be kept in memory, especially
     * 0 forces the table to write to disk immediately. A value smaller than 0
     * will respect the user setting (as defined by the accompanying node).
     * @param rowKeys if true, {@link RowKey}s are expected to be part of a {@link DataRow}.
     * @return A container to which rows can be added and which provides
     * the <code>BufferedDataTable</code>.
     * @throws NullPointerException If the spec argument is <code>null</code>.
     */
    BufferedDataContainer createDataContainer(final DataTableSpec spec, final boolean initDomain,
        final int maxCellsInMemory, final boolean rowKeys) {
        return createDataContainer(spec, initDomain, maxCellsInMemory, rowKeys, m_fileStoreHandler);
    }

    /**
     * Create a {@link BufferedDataContainer} using the provide {@link IWriteFileStoreHandler}. For all other
     * parameters, see the method above.
     *
     * @param spec The spec to open the container.
     * @param initDomain If the domain information from the argument shall be used to initialize the domain (min, max,
     *            possible values). If false, the domain will be determined on the fly.
     * @param maxCellsInMemory Number of cells to be kept in memory, especially 0 forces the table to write to disk
     *            immediately. A value smaller than 0 will respect the user setting (as defined by the accompanying
     *            node).
     * @param rowKeys if true, {@link RowKey}s are expected to be part of a {@link DataRow}.
     * @param writeFileStoreHandler The {@link IWriteFileStoreHandler} to use instead of the one associated with this
     *            {@link ExecutionContext}, useful e.g. if the container is only used as a preview.
     * @return A container to which rows can be added and which provides the <code>BufferedDataTable</code>.
     * @throws NullPointerException If the spec argument is <code>null</code>.
     */
    private BufferedDataContainer createDataContainer(final DataTableSpec spec, final boolean initDomain,
        final int maxCellsInMemory, final boolean rowKeys, final IWriteFileStoreHandler writeFileStoreHandler) {


        final boolean forceCopyOfBlobs = m_node.isModelCompatibleTo(LoopEndNode.class)
            || m_node.isModelCompatibleTo(VirtualSubNodeOutputNodeModel.class);

        final DataContainerSettings containerSettings = DataContainerSettings.internalBuilder() //
            .withInitializedDomain(initDomain) //
            .withMaxCellsInMemory(maxCellsInMemory < 0 ? getMaxCellsInMemory(m_memoryPolicy) : maxCellsInMemory) //
            /*
             * Force sequential handling of rows when the node is a loop end: At a loop end, rows containing blobs need
             * to be written instantly as their owning buffer is discarded in the next loop iteration, see bug 2935. To
             * be written instantly, they have to be handled sequentially.
             */
            .withForceSequentialRowHandling(
                m_node.isForceSychronousIO() || DataContainerSettings.getDefault().isForceSequentialRowHandling()) //
            .withForceCopyOfBlobs(forceCopyOfBlobs) //
            .withRowKeysEnabled(rowKeys).build();

        /*
         * "in theory" the data repository should never be null for non-cleared file store handlers. However...
         * resetting nodes in fully executed loops causes the loop start to be reset first and then the loop
         * body+end, see also WorkflowManager.resetAndConfigureAffectedLoopContext() (can be reproduced using unit
         * test Bug4409_inactiveInnerLoop
         */
        final IDataRepository dataRepository =
            Objects.requireNonNullElseGet(m_dataRepository, NotInWorkflowDataRepository::newInstance);
        return new BufferedDataContainer(spec, m_node, containerSettings,
            dataRepository, m_localTableRepository, writeFileStoreHandler, getTableBackend());
    }

    /**
     * Returns the number of cells to be kept in memory according to the
     * passed policy.
     * @param memPolicy the policy to apply
     * @return number of cells to be kept in memory
     */
    private static int getMaxCellsInMemory(final MemoryPolicy memPolicy) {
        return switch (memPolicy) {
            case CacheInMemory -> Integer.MAX_VALUE;
            case CacheSmallInMemory -> DataContainerSettings.getDefault().getMaxCellsInMemory();
            default -> 0;
        };
    }

    private static TableBackend getTableBackend() {
        // THIS IF CODE PATH NEEDS TO BE REMOVED AS SOON AS WE HAVE FEATURE PARITY for new backend
        final TableBackend backend = WorkflowTableBackendSettings.getTableBackendForCurrentContext();
        LOGGER.debugWithFormat("Using Table Backend \"%s\".", backend.getClass().getSimpleName());
        return backend;
    }

    /**
     * Creates a new <code>BufferedDataTable</code> based on a given input table
     * (<code>in</code>) whereby only some of the columns of <code>in</code>
     * have changed.
     * <p>When the workflow is saved, only the columns that changed will be
     * stored to disc, see also the class description for
     * <a href="#new_column">details</a>.
     * @param in The input table, i.e. reference table.
     * @param rearranger The object which performs the reassembling of columns.
     * @param subProgressMon Typically the object on which this method isgetType
     * performed unless the processing is only a part of the total work.
     * @return A new table which can be returned in the execute method.
     * @throws CanceledExecutionException If canceled.
     */
    public BufferedDataTable createColumnRearrangeTable(
            final BufferedDataTable in, final ColumnRearranger rearranger,
            final ExecutionMonitor subProgressMon)
            throws CanceledExecutionException {
        var t = getTableBackend().rearrange(subProgressMon, m_dataRepository::generateNewID, rearranger, in, this);
        return wrapTableFromBackend(t);
    }

    /**
     * Creates a new <code>BufferedDataTable</code> based on a given input table
     * (<code>in</code>) whereby only the table spec of it has changed.
     * <p>When the workflow is saved, only the spec needs to be saved, see also
     * the class description for <a href="#new_spec">details</a>.
     * @param in The input table, i.e. reference table.
     * @param newSpec The new table spec of <code>in</code>.
     * @return A new table which can be returned in the execute method.
     */
    public BufferedDataTable createSpecReplacerTable(
            final BufferedDataTable in, final DataTableSpec newSpec) {
        var t = getTableBackend().replaceSpec(this, in, newSpec, m_dataRepository::generateNewID);
        return wrapTableFromBackend(t);
    }

   /**
    * Creates a new <code>BufferedDataTable</code> that simply wraps the
    * argument table. This is useful when a node just passes on the input table,
    * for example. If the implementation of NodeModel does not use this method
    * (but simply returns the input table directy), the framework will perform
    * the wrapping operation.
    * @param in The input table to wrap.
    * @return A new table which can be returned in the execute method.
    * @throws NullPointerException If the argument is null.
    */
    public BufferedDataTable createWrappedTable(final BufferedDataTable in) {
        WrappedTable t = new WrappedTable(in);
        BufferedDataTable out = new BufferedDataTable(t, getDataRepository());
        out.setOwnerRecursively(m_node);
        return out;
    }

    /** Create new "void" table. It's a framework method that is used by the streaming executor to populate the output
     * of a node. It has a table specification but no data (which is also indicated in the view).
     *
     * <p>This method is hardly ever interesting for client implementations.
     * @param spec The meta data.
     * @return a new void table.
     * @since 3.1
     */
    public BufferedDataTable createVoidTable(final DataTableSpec spec) {
        VoidTable voidTable = VoidTable.create(spec);
        BufferedDataTable out = new BufferedDataTable(voidTable, getDataRepository());
        out.setOwnerRecursively(m_node);
        return out;
    }

    /** Creates a new {@link BufferedDataTable}, which is  row-wise
     * concatenation of the argument tables. The order of the rows in the
     * returned table is defined through the order of the argument array
     * <code>tables</code> (the <code>BufferedDataTable</code> at index 0
     * provides the first set of rows.
     *
     * <p> The column domains (min, max and possible values) will be
     * merged.
     *
     * <p>Property handlers (such as
     * {@link org.knime.core.data.property.ColorHandler Color},
     * {@link org.knime.core.data.property.ShapeHandler Shape}, and
     * {@link org.knime.core.data.property.SizeHandler}) are taken from the
     * first table in the given array.
     *
     * <p>
     * If tables don't match structurally, e.g. a column is present in one table but not the other,
     * missing values are inserted accordingly.
     *
     * <p>The {@link org.knime.core.data.RowKey RowKeys} must be unique, other
     * wise this method throws an exception.
     * @param exec For cancel checks (this method iterates all rows to
     * ensure uniqueness) and progress.
     * @param tables An array of tables to concatenate,
     * must not be <code>null</code> or empty.
     * @return The concatenated table.
     * @throws CanceledExecutionException If canceled.
     * @throws IllegalArgumentException If the table specs violate any
     * constraint mentioned above, the row keys are not unique, or the array
     * is empty.
     * @throws NullPointerException If any argument is <code>null</code>.
     */
    public BufferedDataTable createConcatenateTable(final ExecutionMonitor exec, final BufferedDataTable... tables)
        throws CanceledExecutionException {
        final KnowsRowCountTable table =
            getTableBackend().concatenate(this, exec, m_dataRepository::generateNewID, null, true, tables);
        return wrapTableFromBackend(table);
    }

    /**
     * Creates a new {@link BufferedDataTable}, which is row-wise concatenation of the argument tables. The order of the
     * rows in the returned table is defined through the order of the argument array <code>tables</code> (the
     * <code>BufferedDataTable</code> at index 0 provides the first set of rows.
     *
     * <p>
     * The column domains (min, max and possible values) will be merged.
     *
     * <p>
     * Property handlers (such as {@link org.knime.core.data.property.ColorHandler Color},
     * {@link org.knime.core.data.property.ShapeHandler Shape}, and {@link org.knime.core.data.property.SizeHandler})
     * are taken from the first table in the given array.
     * <p>
     * If tables don't match structurally, e.g. a column is present in one table but not the other, missing values are
     * inserted accordingly.
     *
     * <p>
     * If the pre-check for duplicates is NOT desired, the {@link org.knime.core.data.RowKey RowKeys} must either be
     * unique or a suffix needs to be appended. Otherwise this method throws an exception
     *
     *
     * @param exec For cancel checks (this method iterates all rows to ensure uniqueness) and progress.
     * @param rowKeyDuplicateSuffix if set, the given suffix will be appended to row key duplicates.
     * @param duplicatesPreCheck if for duplicates should be checked BEFORE creating the result table. If
     *            <code>false</code> the row keys of the input tables MUST either be unique over all tables or
     *            a suffix appended.
     * @param tables An array of tables to concatenate, must not be <code>null</code> or empty.
     * @return The concatenated table.
     * @throws CanceledExecutionException If canceled.
     * @throws IllegalArgumentException If the table specs violate any constraint mentioned above, the row keys are not
     *             unique, or the array is empty.
     * @throws NullPointerException If any argument is <code>null</code>.
     * @since 3.1
     */
    public BufferedDataTable createConcatenateTable(final ExecutionMonitor exec,
        final Optional<String> rowKeyDuplicateSuffix, final boolean duplicatesPreCheck,
        final BufferedDataTable... tables) throws CanceledExecutionException {
        final KnowsRowCountTable concatenated = getTableBackend().concatenate(this, exec,
            m_dataRepository::generateNewID, rowKeyDuplicateSuffix.orElse(null), duplicatesPreCheck, tables);
        return wrapTableFromBackend(concatenated);
    }

    BufferedDataTable createConcatenateTableWithNewRowIDs(final BufferedDataTable... tables) {
        // the use of "this" twice is intended because this is a new method that does not accept an ExecutionMonitor
        // instead clients should create a SubExecutionContext with the appropriate progress
        return wrapTableFromBackend(
            getTableBackend().concatenateWithNewRowIDs(this, m_dataRepository::generateNewID, tables));
    }

    /**
     * Creates a new {@link BufferedDataTable} that is a column based join of
     * the argument tables. The <code>left</code> table argument contributes
     * the first set of columns and the <code>right</code> table argument the
     * second set of columns. The tables must not contain duplicate columns
     * (i.e. columns with the same name). They do need to contain the same set
     * of rows though, i.e. the same row count and equal row keys in identical
     * order. If any of these constraints is not met, this method throws and
     * <code>IllegalArgumentException</code>.
     *
     * <p>
     * This method will traverse both tables once to ensure that the row keys
     * are identical and are returned in the same order. It reports progress for
     * this sanity check to the <code>exec</code> argument.
     *
     * <p>
     * The returned table is only a view on both argument tables, i.e. any
     * subsequent iteration is carried out on the argument tables. This also
     * means that the returned table does only acquire little main memory and no
     * disc memory at all.
     *
     * @param left The table contributing the first set of columns.
     * @param right The table contributing the second set of columns.
     * @param exec For progress information and cancel checks, consider to use a
     *            {@link ExecutionMonitor#createSubProgress(double) sub
     *            execution monitor} when joining two tables is only part of the
     *            whole work.
     * @return A buffered data table as join of the two argument tables.
     * @throws CanceledExecutionException If progress has been canceled.
     * @throws NullPointerException If any argument is <code>null</code>.
     * @throws IllegalArgumentException If the tables contain duplicate columns
     *             or non-matching rows.
     * @see DataTableSpec#DataTableSpec(DataTableSpec, DataTableSpec)
     */
    public BufferedDataTable createJoinedTable(final BufferedDataTable left, final BufferedDataTable right,
        final ExecutionMonitor exec) throws CanceledExecutionException {
        KnowsRowCountTable jt = getTableBackend().append(this, exec, m_dataRepository::generateNewID, left, right);
        return wrapTableFromBackend(jt);
    }

    BufferedDataTable appendTables(final TableBackend.AppendConfig config, final BufferedDataTable left,
        final BufferedDataTable right) throws CanceledExecutionException {
        var appendedTable = getTableBackend().append(this, m_dataRepository::generateNewID, config, left, right);
        return wrapTableFromBackend(appendedTable);
    }

    /**
     * Creates a sliced table according to the provided {@link TableFilter slice}. This {@link ExecutionContext} will be
     * used for progress reporting. Use {@code exec.createSubExecutionContext(0.2).createSlicedTable(table, slice)} if
     * only a part of the nodes progress should be covered by this operation.
     *
     * @param table to slice
     * @param slice the slice to extract from the provided table
     * @return the sliced table
     * @noreference This method is not intended to be referenced by clients.
     */
    BufferedDataTable createSlicedTable(final BufferedDataTable table, final Selection slice) {
        var slicedTable = getTableBackend().slice(this, table, slice, m_dataRepository::generateNewID);
        return wrapTableFromBackend(slicedTable);
    }

    private BufferedDataTable wrapTableFromBackend(final KnowsRowCountTable table) {
        registerAsLocalTableIfContainerTable(table);
        var out = BufferedDataTable.wrapTableFromTableBackend(table, getDataRepository());
        out.setOwnerRecursively(m_node);
        return out;
    }

    /** Allows node implementations to clear temporary tables. This is useful
     * for nodes that need to create temp tables during their execution, e.g.
     * a sorter implementation swaps out temporary data to disk for later
     * merging. Most node implementations will not use this method as they
     * create the final output tables directly.
     *
     * There a couple of side-constraints:
     * <ul>
     * <li>This method is only to be called during a node's execution.
     * <li>The argument table needs to be created using the current node's
     * execution context (either this object or a derived/parent execution
     * context).
     * <li>Only the argument table will be cleared, any referenced table (also
     * created by the same node) will not be cleared.
     * <li>The table argument must not be returned by the execute method.
     * <li>As this table is supposed to be temporary storage the table should
     * not contain any individually defined {@link
     * org.knime.core.data.container.BlobDataCell} as these might be referenced
     * by other tables created during execution.
     * </ul>
     *
     * @param table The table to be cleared.
     * @throws NullPointerException If the argument is null.
     * @throws IllegalStateException If the table is not created by this node
     *         or this method is not called during execution.
     * @since v2.5
     */
    public void clearTable(final BufferedDataTable table) {
        if (table.getOwner() != m_node) {
            String oOwner = table.getOwner().getName();
            throw new IllegalStateException("Can't clear table that was "
                    + "created by another node (\"" + oOwner + "\")");
        }
        int id = table.getBufferedTableId();
        if (m_dataRepository.getTable(id).isPresent()) {
            throw new IllegalStateException("Clearing table not allowed - can "
                    + "only clear a table during execution");
        }
        table.clearSingle(m_node);
        // this is a bit dirty: the local table repository enables us to
        // reference blob cells that are created in a different table but
        // on the same node (other table not yet in global repository - and
        // maybe never will). The arg table is added to the local rep during
        // DataContainer#close (or by one of the methods delegating to the TableBackend)
        // but it can remove itself during clear()...
        // that's why we do it here.
        m_localTableRepository.removeTable(id);
    }

    /**
     * Creates a new execution context with a different max progress value.
     * This method is the counterpart to {@link #createSubProgress(double)}
     * {@link ExecutionMonitor}. A sub execution contexts has the same
     * properties as this object but it only reports progress to a limited value
     * of <code>maxProg</code>. It can therefore be used in, e.g. utility
     * classes which report progress in [0, 1], but whose progress is only
     * a small contribution to the overall progress.
     * @param maxProg The maximum progress, must be in [0,1]
     * @return A new execution context.
     */
    public ExecutionContext createSubExecutionContext(final double maxProg) {
        NodeProgressMonitor subProgress = createSubProgressMonitor(maxProg);
        return new ExecutionContext(subProgress, m_node, m_memoryPolicy, m_dataRepository, m_localTableRepository,
            m_fileStoreHandler);
    }

    /**
     * Creates a new execution context with a different max progress value and
     * swallowing any report messages. This method is the counterpart to
     * {@link #createSilentSubProgress(double)} in {@link ExecutionMonitor}. A
     * sub execution contexts has the same properties as this object but it only
     * reports progress to a limited value of <code>maxProg</code>. It will
     * also ignore any message, which is set using the
     * {@link #setMessage(String)} method. It can therefore be used in, e.g.
     * utility classes which report progress in [0, 1], but whose progress is
     * only a small contribution to the overall progress.
     *
     * @param maxProg The maximum progress, must be in [0,1]
     * @return A new execution context.
     */
    public ExecutionContext createSilentSubExecutionContext(
            final double maxProg) {
        NodeProgressMonitor subProgress = createSilentSubProgressMonitor(maxProg);
        return new ExecutionContext(subProgress, m_node, m_memoryPolicy, m_dataRepository, m_localTableRepository,
            m_fileStoreHandler);
    }

    /**
     * Create a new {@link RowContainer} for direct write access to storage. This is a new write API added in version
     * 4.3, allowing tables to be created without cell creation overhead. An implementation would look as follows:
     *
     * <pre>
     *     final long nrRows = ...
     *     final DataTableSpec spec = ...
     *     BufferedDataTable table;
     *     try (final CustomKeyRowContainer cursor = exec.createRowContainer(spec)) {
     *         for (long i = 0; i < nrRows; i++) {
     *             final String key = "Row" + i;
     *             exec.setProgress((double) i / nrRows, () -> ...);
     *             exec.checkCanceled();
     *             cursor.setRowKey(key);
     *             cursor.&lt;DoubleWriteValue>getWriteValue(j).setValue(Math.random());
     *             cursor.&lt;IntWriteValue>getWriteValue(j).setValue((int)(100 * Math.random()));
     *             cursor.push();
     *         }
     *         table = cursor.finish();
     *     }
     * </pre>
     *
     * @param spec the spec to create the RowContainer
     * @return a new RowContainer with CustomKeys.
     *
     * @noreference This method is not intended to be referenced by clients.
     * @implNote Highly experimental API added with KNIME AP 4.3 - Can change with future released of KNIME AP.
     * @since 4.3
     */
    public final RowContainer createRowContainer(final DataTableSpec spec) {
        return createRowContainer(spec, false);
    }

    /**
     * Create a new RowContainer for direct write access to storage. This method is analogous to
     * {@link #createRowContainer(DataTableSpec)} with the option to set the initialization of the domain.
     *
     * @param spec the spec to create the RowContainer
     * @param initDomains <source>true</source> if domains should be initialized with domains of spec.
     * @return a new RowContainer with CustomKeys.
     *
     * @noreference This method is not intended to be referenced by clients.
     * @implNote Highly experimental API added with KNIME AP 4.3 - Can change with future released of KNIME AP.
     * @since 4.3
     */
    @SuppressWarnings("resource")// the RowContainer is closed by the LocalRepoAwareRowContainer
    public final RowContainer createRowContainer(final DataTableSpec spec, final boolean initDomains) {
        final TableBackend backend = WorkflowTableBackendSettings.getTableBackendForCurrentContext();
        LOGGER.debugWithFormat("Using Table Backend \"%s\".", backend.getClass().getSimpleName());
        var container = backend.create(this, spec,
            DataContainerSettings.internalBuilder().withInitializedDomain(initDomains).build(), getDataRepository(),
            getFileStoreHandler());
        return new LocalRepoAwareRowContainer(container, m_localTableRepository);
    }

    /** @return the fileStoreHandler the handler set at construction time (possibly null if run in 3rd party exec) */
    IWriteFileStoreHandler getFileStoreHandler() {
        return m_fileStoreHandler;
    }

    /**
     * @return the dataRepository set at construction time, not null.
     */
    IDataRepository getDataRepository() {
        return m_dataRepository;
    }

    /**
     * Get reference to the local table repository. It contains
     * <code>ContainerTable</code> objects that have been created during the
     * execution of a node. Some of which will be put into the global
     * repository after execution.
     * @return The local table repository.
     */
    Map<Integer, ContainerTable> getLocalTableRepository() {
        return m_localTableRepository.toMap();
    }

    /** @return the node */
    Node getNode() {
        return m_node;
    }

    /** Called when node was canceled. */
    void onCancel() {
        m_localTableRepository.onCancel();
    }

    private void registerAsLocalTableIfContainerTable(final KnowsRowCountTable table) {
        if (table instanceof ContainerTable) {
            m_localTableRepository.addTable((ContainerTable)table);
        }
    }

    /**
     * Submits a job to an executor, which can be a threaded one, a cluster
     * executor or anything else.
     *
     * @param input the input data for the job
     * @param settings the settings for the job
     * @param jobClass the job's class that is
     * @param exec the execution monitor
     * @return a future holding the job's results
     * @throws NoSuchMethodException of the job class does not have
     * a default constructor
     */
    public Future<PortObject[]> submitJob(final PortObject[] input,
            final NodeSettingsRO settings,
            final Class<? extends KNIMEJob> jobClass,
            final ExecutionMonitor exec) throws NoSuchMethodException {
        final Constructor<? extends KNIMEJob> cons = jobClass.getConstructor();

        Callable<PortObject[]> task = new Callable<PortObject[]>() {
            @Override
            public PortObject[] call() throws Exception {
                return cons.newInstance().run(input, settings,
                        ExecutionContext.this);
            }
        };

        return KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(task);
    }

    private static final class LocalRepoAwareRowContainer implements RowContainer {

        private final RowContainer m_backendDelegate;

        private final ILocalDataRepository m_localDataRepo;

        LocalRepoAwareRowContainer(final RowContainer backendDelegate, final ILocalDataRepository localDataRepo) {
            m_backendDelegate = backendDelegate;
            m_localDataRepo = localDataRepo;
        }

        @Override
        public void close() throws IOException {
            m_backendDelegate.close();
        }

        @Override
        public RowWriteCursor createCursor() {
            return m_backendDelegate.createCursor();
        }

        @Override
        public BufferedDataTable finish() throws IOException {
            var table = m_backendDelegate.finish();
            var delegate = table.getDelegate();
            if (delegate instanceof ContainerTable container) {
                m_localDataRepo.addTable(container);
            }
            return table;
        }

    }
}

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
 * History
 *   Jul 5, 2006 (wiswedel): created
 */
package org.knime.core.node;

import static org.knime.core.data.container.DefaultTableStoreFormat.CompressionFormat.GZIP;
import static org.knime.core.data.container.DefaultTableStoreFormat.CompressionFormat.NONE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.RowKey;
import org.knime.core.data.TableBackend;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.data.container.BufferedContainerTable;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ConcatenateTable;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.DefaultTableStoreFormat;
import org.knime.core.data.container.JoinedTable;
import org.knime.core.data.container.RearrangeColumnsTable;
import org.knime.core.data.container.TableSpecReplacerTable;
import org.knime.core.data.container.VoidTable;
import org.knime.core.data.container.WrappedTable;
import org.knime.core.data.container.filter.CloseableDataRowIterable;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.container.storage.TableStoreFormat;
import org.knime.core.data.container.storage.TableStoreFormatRegistry;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.SizeAwareDataTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.BufferedDataTableView;
import org.knime.core.node.workflow.WorkflowDataRepository;
import org.knime.core.util.MutableBoolean;

/**
 * DataTable implementation that is passed along the KNIME workflow. This
 * implementation is provided in a NodeModel's
 * {@link org.knime.core.node.NodeModel#execute(
 * BufferedDataTable[], ExecutionContext)} method as input data and
 * must also be returned as output data.
 *
 * <p><code>BufferedDataTable</code> are not created directly (via a
 * constructor, for instance) but they are rather instantiated using the
 * {@link ExecutionContext} that is provided in the execute method.
 *
 * <p>Implementation note: The iterator returned by this class is a
 * {@link CloseableRowIterator}, meaning that if your implementation is likely
 * to open many iterators without pushing them to the end of the table, you
 * should consider to close them when done in order to free system resources.

 * @author Bernd Wiswedel, University of Konstanz
 */
public final class BufferedDataTable implements SizeAwareDataTable, PortObject {

    /** Define port type of objects of this class when used as PortObjects.
     */
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(BufferedDataTable.class);

    /** A port type representing an optional input table (as used, for instance
     * in the Concatenate node).
     * @since 2.6 */
    public static final PortType TYPE_OPTIONAL =
        PortTypeRegistry.getInstance().getPortType(BufferedDataTable.class, true);

    /**
     * Method that is used internally while the workflow is being loaded. Not
     * intended to be used directly by node implementations.
     * @param tblRep The table repository
     * @param tableID The table ID
     * @param dataRepository The data repository (needed for blobs, file stores, and table ids).
     * @return The table from the repository.
     * @throws InvalidSettingsException If no such table exists.
     * @since 3.7
     */
    public static BufferedDataTable getDataTable(final Map<Integer, BufferedDataTable> tblRep, final Integer tableID,
        final WorkflowDataRepository dataRepository) throws InvalidSettingsException {
        if (tblRep == null) {
            throw new NullPointerException("Table repository must not be null");
        }
        BufferedDataTable result = tblRep.get(tableID);
        if (result == null) {
            throw new InvalidSettingsException("No BufferedDataTable "
                    + " with ID " + tableID);
        }
        // update the lastID counter!
        assert result.m_tableID == tableID;
        dataRepository.updateLastId(tableID);
        return result;
    }

    /**
     * Method that is used internally while the workflow is being loaded. Not
     * intended to be used directly by node implementations.
     * @param tblRep The table repository
     * @param t The table to put into the repository.
     */
    public static void putDataTable(
            final Map<Integer, BufferedDataTable> tblRep,
            final BufferedDataTable t) {
        tblRep.put(t.getBufferedTableId(), t);
    }

    /** Throws <code>IllegalStateException</code> as this method is not
     * supposed to be called; refer to the API of {@link PortObject} for details
     * on this method. The KNIME engine treats objects of this kind differently.
     * @return Nothing as an exception is being thrown.
     */
    static PortObjectSerializer<BufferedDataTable> getPortObjectSerializer() {
        throw new IllegalStateException("No access on BufferedDataTables "
                + "via generic PortObjectSerializer");
    }

    private final MutableBoolean m_isCleared = new MutableBoolean(false);
    private final KnowsRowCountTable m_delegate;
    private int m_tableID;
    private Node m_owner;

    /**
     * Creates a new buffered data table based on a container table (caching everything).
     *
     * @param table The reference.
     * @param bufferID The buffer ID.
     */
    BufferedDataTable(final ContainerTable table, final int bufferID) {
        this(table, bufferID, null);
    }

    /**
     * Creates a new buffered data table based on a changed columns table (only memorize columns that changed).
     *
     * @param table The reference.
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     */
    BufferedDataTable(final RearrangeColumnsTable table, final IDataRepository dataRepository) {
        this(table,
            table.getAppendTable() != null ? table.getAppendTable().getTableId() : dataRepository.generateNewID(),
            dataRepository);
    }

    /**
     * Creates a new buffered data table based on a changed spec table (only keep new spec).
     *
     * @param table The reference.
     * @param dataRepository the data repository needed for blobs, file stores, and table ids)
     */
    BufferedDataTable(final TableSpecReplacerTable table, final IDataRepository dataRepository) {
        this(table, dataRepository.generateNewID(), dataRepository);
    }

    /**
     * Creates a new buffered data table based on a wrapped table.
     *
     * @param table The reference.
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     */
    BufferedDataTable(final WrappedTable table, final IDataRepository dataRepository) {
        this(table, dataRepository.generateNewID(), dataRepository);
    }

    /**
     * Creates a new buffered data table based on a concatenation of BufferedDataTables.
     *
     * @param table The reference.
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     */
    BufferedDataTable(final ConcatenateTable table, final IDataRepository dataRepository) {
        this(table, dataRepository.generateNewID(), dataRepository);
    }

    /**
     * Creates a new buffered data table based on a join of BufferedDataTables.
     *
     * @param table The reference.
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     */
    BufferedDataTable(final JoinedTable table, final IDataRepository dataRepository) {
        this(table, dataRepository.generateNewID(), dataRepository);
    }

    /**
     * Creates a new buffered data table based on a "void" table.
     *
     * @param table The reference.
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     */
    BufferedDataTable(final VoidTable table, final IDataRepository dataRepository) {
        this(table, dataRepository.generateNewID(), dataRepository);
    }

    /**
     * Creates a new BufferedDataTable for an extended table type.
     *
     * @param table The extended table
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     */
    BufferedDataTable(final ExtensionTable table, final IDataRepository dataRepository) {
        this(table, dataRepository.generateNewID(), dataRepository);
    }

    /**
     * Wraps the provided table into a BufferedDataTable.<br>
     * This method is to be used when a table is created by one of the table transformations in {@link TableBackend}.
     *
     * @param table to wrap
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     * @return a BufferedDataTable that is backed by the provided table
     */
    static BufferedDataTable wrapTableFromTableBackend(final KnowsRowCountTable table,
        final IDataRepository dataRepository) {
        if (table instanceof RearrangeColumnsTable rct) {
            return new BufferedDataTable(rct, dataRepository);
        }
        if (table instanceof ContainerTable ct) {
            return new BufferedDataTable(ct,  ct.getTableId(), dataRepository);
        }
        return new BufferedDataTable(table, dataRepository.generateNewID(), dataRepository);
    }

    private BufferedDataTable(final KnowsRowCountTable table, final int id,
        final IDataRepository dataRepository) {
        m_delegate = table;
        if (dataRepository != null) {
            // table ID -1 is used for old workflows (1.1.x) - no notion of blobs at that time
            // see also DataContainer.readFromZip(ReferencedFile, BufferCreator)
            assert id == -1 || Integer.toUnsignedLong(id) <= Integer.toUnsignedLong(dataRepository.getLastId()) //
                    : "Table identifiers not unique " + id;
        }
        m_tableID = id;
    }

    /** Package scope getter for underlying table implementation. Only needed
     * if underlying table is of special kind and can be treated differently/
     * more efficiently by individual node implementations.
     * @return underlying table.
     */
    /*package*/ KnowsRowCountTable getDelegate() {
        return m_delegate;
    }

    /** Called after execution of node has finished to put the tables that
     * are returned from the execute method into a global table repository.
     * @param dataRepository The repository from the workflow
     */
    void putIntoTableRepository(final WorkflowDataRepository dataRepository) {
        m_delegate.putIntoTableRepository(dataRepository);
        BufferedDataTable[] references = m_delegate.getReferenceTables();
        for (BufferedDataTable reference : references) {
            reference.putIntoTableRepository(dataRepository);
        }
    }

    /** Remove this table and all of its delegates from the table repository,
     * if and only if its owner is the argument node.
     * @param dataRepository The repository to be removed from.
     * @param owner The dedicated owner.
     * @return The number of tables effectively removed, used for assertions.
     */
    int removeFromTableRepository(final WorkflowDataRepository dataRepository,
            final Node owner) {
        if (getOwner() != owner) { // can safely test for hard references here
            return 0;
        }
        int result = 0;
        BufferedDataTable[] references = m_delegate.getReferenceTables();
        for (BufferedDataTable reference : references) {
            result += reference.removeFromTableRepository(dataRepository, owner);
        }
        if (m_delegate.removeFromTableRepository(dataRepository)) {
            result += 1;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_delegate.getDataTableSpec();
    }

    /**
     * {@inheritDoc}
     * @see #getDataTableSpec()
     */
    @Override
    public DataTableSpec getSpec() {
        return getDataTableSpec();
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return "Rows: " + size() + ", Cols: " + getSpec().getNumColumns();
    }

    /** {@inheritDoc} */
    @Override
    public CloseableRowIterator iterator() {
        return m_delegate.iterator();
    }

    /**
     * Provides a {@link RowCursor} to access rows of this table.
     *
     * @return {@link RowCursor} to access rows of a table.
     * @apiNote Must not to be called by clients. Experimental API.
     * @since 4.2.2
     */
    @Override
    public RowCursor cursor(){
        return m_delegate.cursor();
    }

    /**
     * Provides a {@link RowCursor} to access rows of this table that is filtered according to a given
     * {@link TableFilter} and can be iterated over. The filtering won't change this BufferedDataTable or impact
     * subsequent calls of this method with other filters.
     *
     * @param filter to filter rows and columns.
     *
     * @return {@link RowCursor} to access rows of a table.
     * @apiNote Must not to be called by clients. Experimental API.
     * @since 4.2.2
     */
    public RowCursor cursor(final TableFilter filter) {
        return m_delegate.cursor(filter);
    }

    /**
     * Provides a {@link CloseableDataRowIterable} that is filtered according to a given {@link TableFilter} and can be
     * iterated over. The filtering won't change this BufferedDataTable or impact subsequent calls of this method with
     * other filters.
     *
     * @param filter the filter to be applied
     * @return a filtered iterable
     * @since 4.0
     */
    public CloseableDataRowIterable filter(final TableFilter filter) {
        return filter(filter, null);
    }

    /**
     * Provides a {@link CloseableDataRowIterable} that is filtered according to a given {@link TableFilter} and can be
     * iterated over. During iteration, a given {@link ExecutionMonitor} will update its progress. Note that the
     * progress updates will not be accurate if you iterate over the returned iterable multiple times. It is therefore
     * suggested to use the returned iterable only once. The filtering won't change this BufferedDataTable or impact
     * subsequent calls of this method with other filters.
     *
     * @param filter the filter to be applied
     * @param exec the execution monitor that shall be updated with progress or null if no progress updates are desired
     * @return a filtered iterable
     * @since 4.0
     */
    public CloseableDataRowIterable filter(final TableFilter filter, final ExecutionMonitor exec) {
        CheckUtils.checkArgumentNotNull(filter);
        filter.validate(getSpec(), m_delegate.size());
        return new CloseableDataRowIterable() {
            @Override
            public CloseableRowIterator iterator() {
                return m_delegate.iteratorWithFilter(filter, exec);
            }
        };
    }

    /**
     * Get an iterator instance that will return missing values when the table
     * is cleared as part of a node reset.
     *
     * <p>
     * In general, node implementations are guaranteed not to be active
     * (executing none of the abstract methods) when a node is reset. However,
     * if the data is displayed in a view using an iterator (e.g. a table view),
     * there is no guarantee when the data is accessed. Reading the data and
     * clearing the table may occur concurrently, specifically if used in fast
     * executing loops. An iterator returned by this method will ensure that
     * (invalid = missing) data is returned and that there are as many rows as
     * suggested by the {@link #size()} method.
     *
     * <p>
     * Client implementations should generally use the {@link #iterator()}
     * method to read the data unless the data is potentially accessed in a node
     * or port view outside the
     * {@link NodeModel#execute(BufferedDataTable[], ExecutionContext)} method.
     *
     * @return A new iterator instance that will return missing values and
     *          fake row ids in case the table is cleared (either before this
     *          method is called or while the iteration is in progress).
     */
    public CloseableRowIterator iteratorFailProve() {
        synchronized (m_isCleared) {
            CloseableRowIterator baseIterator;
            if (m_isCleared.booleanValue()) {
                baseIterator = null;
            } else {
                baseIterator = iterator();
            }
            return new CloseableFailProveRowIterator(baseIterator);
        }

    }

    /**
     * Get the row count of the this table.
     * @return Number of rows in the table.
     * @since 3.0
     * @deprecated use {@link #size()} instead which supports more than {@link Integer#MAX_VALUE} rows
     */
    @Deprecated
    public int getRowCount() {
        return KnowsRowCountTable.checkRowCount(size());
    }

    /**
     * Returns the number of rows in this table.
     *
     * @return the number of rows
     * @since 3.0
     */
    @Override
    public long size() {
        return m_delegate.size();
    }


    /** Method being used internally, not interesting for the implementor of
     * a new node model. It will return a unique ID to identify the table
     * while loading.
     * @return The unique ID.
     */
    public Integer getBufferedTableId() {
        return m_tableID;
    }

    private final class CloseableFailProveRowIterator
        extends CloseableRowIterator {

        private final int m_cellCount;
        private final long m_maxRows;
        private final CloseableRowIterator m_it;
        private long m_rowIndex;

        private CloseableFailProveRowIterator(final CloseableRowIterator it) {
            m_it = it;
            m_cellCount = getDataTableSpec().getNumColumns();
            m_maxRows = size();
        }

        @Override
        public DataRow next() {
            synchronized (m_isCleared) {
                DataRow result;
                if (m_isCleared.booleanValue()) {
                    DataCell[] cells = new DataCell[m_cellCount];
                    Arrays.fill(cells, DataType.getMissingCell());
                    result = new BlobSupportDataRow(new RowKey(
                            "Cleared_row_" + m_rowIndex), cells);
                } else {
                    result = m_it.next();
                }
                m_rowIndex += 1;
                return result;
            }
        }

        @Override
        public boolean hasNext() {
            return m_rowIndex < m_maxRows;
        }

        @Override
        public void close() {
            if (m_it != null) {
                m_it.close();
            }
        }

    }

    private static final String CFG_TABLE_META = "table_meta_info";
    private static final String CFG_TABLE_REFERENCE = "table_references";
    private static final String CFG_TABLE_TYPE = "table_type";
    private static final String CFG_TABLE_ID = "table_ID";
    private static final String CFG_TABLE_FILE_NAME = "table_file_name";
    private static final String CFG_TABLE_CONTAINER_FORMAT = "table_format";
    private static final String CFG_TABLE_CONTAINER_FORMAT_VERSION = "table_format_version";
    private static final String CFG_TABLE_COMPRESSION_FORMAT = "table_compression_format";

    private static final String TABLE_TYPE_CONTAINER = "container_table";
    /**
     * As of 3.6 KNIME saves container tables written with a format other than the {@link DefaultTableStoreFormat} under
     * this value in order to make KNIME <= 3.5 fail when loading such a workflow.
     */
    private static final String TABLE_TYPE_CONTAINER_CUSTOM = "container_table_custom";
    /**
     * As of 4.0 KNIME saves container tables written with the {@link DefaultTableStoreFormat}, but compressed with a
     * format other than GZIP under this value in order to make KNIME <= 3.7 fail when loading such a workflow.
     */
    private static final String TABLE_TYPE_CONTAINER_COMPRESS = "container_table_compressed";
    private static final String TABLE_TYPE_REARRANGE_COLUMN = "rearrange_columns_table";
    /**
     * Similar to the container table (see above), we have to make sure that earlier versions of KNIME complain when
     * loading a workflow that has been written with a custom table store format (e.g., Parquet) or custom compression
     * formats (e.g., Snappy).
     */
    private static final String TABLE_TYPE_REARRANGE_COLUMN_CUSTOM = "rearrange_columns_table_custom";
    private static final String TABLE_TYPE_REARRANGE_COLUMN_COMPRESS = "rearrange_columns_table_compressed";
    private static final String TABLE_TYPE_NEW_SPEC = "new_spec_table";
    private static final String TABLE_TYPE_WRAPPED = "wrapped_table";
    private static final String TABLE_TYPE_CONCATENATE = "concatenate_table";
    private static final String TABLE_TYPE_JOINED = "joined_table";
    private static final String TABLE_TYPE_VOID = "void_table";
    /** The table is referenced multiple times in a node, e.g. provided at
     * different outputs (possibly wrapped) or it is used as output-port table
     * and as internally held table. See bug 2117.
     * @since 2.6 */
    private static final String TABLE_TYPE_REFERENCE_IN_SAME_NODE = "reference_from_same_node_table";
    private static final String TABLE_TYPE_EXTENSION = "extension_table";
    private static final String TABLE_FILE = "data.zip";
    private static final String TABLE_DESCRIPTION_FILE = "data.xml";
    private static final String TABLE_SPEC_FILE = "spec.xml";


    /** Saves the table to a directory and writes some settings to the argument
     * NodeSettingsWO object. It will also write the reference table in case
     * this node is responsible for it (i.e. this node created the reference
     * table).
     * @param dir The directory to write to.
     * @param savedTableIDs Ids of tables that were previously saved, used to identify
     * tables that are referenced by the same nodes multiple times.
     * @param exec The progress monitor for cancellation.
     * @throws IOException If writing fails.
     * @throws CanceledExecutionException If canceled.
     */
    void save(final File dir, final Set<Integer> savedTableIDs, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        NodeSettings s = new NodeSettings(CFG_TABLE_META);
        Integer bufferedTableID = getBufferedTableId();
        s.addInt(CFG_TABLE_ID, bufferedTableID);
        File outFile = new File(dir, TABLE_FILE);
        if (!savedTableIDs.add(bufferedTableID)) {
            s.addString(CFG_TABLE_TYPE, TABLE_TYPE_REFERENCE_IN_SAME_NODE);
        } else if (m_delegate instanceof BufferedContainerTable) {
            final TableStoreFormat format = ((BufferedContainerTable)m_delegate).getTableStoreFormat();
            if (!DefaultTableStoreFormat.class.equals(format.getClass())) {
                // use different identifier to cause old versions of KNIME to fail loading newer workflows
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_CONTAINER_CUSTOM);
                s.addString(CFG_TABLE_CONTAINER_FORMAT, format.getClass().getName());
                s.addString(CFG_TABLE_CONTAINER_FORMAT_VERSION, format.getVersion());
            } else {
                final DefaultTableStoreFormat defaultFormat = (DefaultTableStoreFormat)format;
                if (!Arrays.asList(NONE, GZIP).contains(defaultFormat.getCompressionFormat())) {
                    s.addString(CFG_TABLE_TYPE, TABLE_TYPE_CONTAINER_COMPRESS);
                    s.addString(CFG_TABLE_COMPRESSION_FORMAT, defaultFormat.getCompressionFormat().toString());
                } else {
                    s.addString(CFG_TABLE_TYPE, TABLE_TYPE_CONTAINER);
                }
            }
            m_delegate.saveToFile(outFile, s, exec);
        } else {
            if (m_delegate instanceof RearrangeColumnsTable) {
                final ContainerTable containerTable = ((RearrangeColumnsTable)m_delegate).getAppendTable();
                if (containerTable != null && containerTable instanceof BufferedContainerTable) {
                    final BufferedContainerTable appendTable = (BufferedContainerTable)containerTable;
                    final TableStoreFormat format = appendTable.getTableStoreFormat();
                    if (!DefaultTableStoreFormat.class.equals(format.getClass())) {
                        // use different identifier to cause old versions of KNIME to fail loading newer workflows
                        s.addString(CFG_TABLE_TYPE, TABLE_TYPE_REARRANGE_COLUMN_CUSTOM);
                        s.addString(CFG_TABLE_CONTAINER_FORMAT, appendTable.getTableStoreFormat().getClass().getName());
                        s.addString(CFG_TABLE_CONTAINER_FORMAT_VERSION, appendTable.getTableStoreFormat().getVersion());
                    } else {
                        final DefaultTableStoreFormat defaultFormat = (DefaultTableStoreFormat)format;
                        if (!Arrays.asList(NONE, GZIP).contains(defaultFormat.getCompressionFormat())) {
                            s.addString(CFG_TABLE_TYPE, TABLE_TYPE_REARRANGE_COLUMN_COMPRESS);
                            s.addString(CFG_TABLE_COMPRESSION_FORMAT, defaultFormat.getCompressionFormat().toString());
                        } else {
                            s.addString(CFG_TABLE_TYPE, TABLE_TYPE_REARRANGE_COLUMN);
                        }
                    }
                } else {
                    s.addString(CFG_TABLE_TYPE, TABLE_TYPE_REARRANGE_COLUMN);
                }
            } else if (m_delegate instanceof TableSpecReplacerTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_NEW_SPEC);
            } else if (m_delegate instanceof WrappedTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_WRAPPED);
            } else if (m_delegate instanceof JoinedTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_JOINED);
            } else if (m_delegate instanceof VoidTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_VOID);
            } else if (m_delegate instanceof ConcatenateTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_CONCATENATE);
            } else {
                assert m_delegate instanceof ExtensionTable;
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_EXTENSION);
            }
            BufferedDataTable[] references = m_delegate.getReferenceTables();
            ArrayList<String> referenceDirs = new ArrayList<String>();
            for (BufferedDataTable reference : references) {
                if (reference.getOwner() == getOwner()
                        && !savedTableIDs.contains(reference.getBufferedTableId())) {
                    int index = referenceDirs.size();
                    String dirName = "r" + index;
                    File subDir = new File(dir, dirName);
                    if (!subDir.mkdir() && !subDir.isDirectory()) {
                        throw new IOException("Could not create directory " + subDir.getAbsolutePath());
                    }
                    if (!subDir.canWrite()) {
                        throw new IOException("Unable to write directory " + subDir.getAbsolutePath());
                    }
                    referenceDirs.add(dirName);
                    reference.save(subDir, savedTableIDs, exec);
                }
            }
            s.addStringArray(CFG_TABLE_REFERENCE,
                    referenceDirs.toArray(new String[referenceDirs.size()]));
            m_delegate.saveToFile(outFile, s, exec);
        }
        // only write the data file to the settings if it has been created
        if (outFile.exists()) {
            s.addString(CFG_TABLE_FILE_NAME, TABLE_FILE);
        } else {
            s.addString(CFG_TABLE_FILE_NAME, null);
        }
        saveSpec(getDataTableSpec(), dir);
        File dataXML = new File(dir, TABLE_DESCRIPTION_FILE);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dataXML))) {
            s.saveToXML(out);
        }
    }

    /**
     * Utility method that is used when the node saves its state. It saves
     * it to a file spec.xml.
     * @param spec To save
     * @param dataPortDir destination directory
     * @throws IOException if that fails for any reason
     */
    static void saveSpec(final DataTableSpec spec, final File dataPortDir)
        throws IOException {
        // do not write file, if spec is null (may be the case when node
        // is configured but can't calculate output, e.g. transpose node)
        if (spec == null) {
            return;
        }
        File specFile = new File(dataPortDir, TABLE_SPEC_FILE);
        Config c = new NodeSettings(TABLE_SPEC_FILE);
        spec.save(c);
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(specFile))) {
            c.saveToXML(os);
        }
    }

    /**
     * Utility method used in the node's load method. It reads the spec from
     * a file spec.xml in <code>dataPortDir</code>.
     * @param dataPortDir To load from.
     * @return The spec contained in this directory.
     * @throws IOException If that fails.
     * @throws InvalidSettingsException If the settings in the spec.xml can't
     * be parsed.
     */
    static DataTableSpec loadSpec(final ReferencedFile dataPortDir)
        throws IOException, InvalidSettingsException {
        File specFile = new File(dataPortDir.getFile(), TABLE_SPEC_FILE);
        if (specFile.exists()) {
            if (specFile.length() > 10 * 1024 * 1024) { // 10MB
                NodeLogger.getLogger(BufferedDataTable.class).warn(String.format(
                    "Table spec file is %s large - this may result in increased memory consumption (path '%s')",
                        FileUtils.byteCountToDisplaySize(specFile.length()), specFile.getAbsolutePath()));
            }
            try (InputStream in = new BufferedInputStream(new FileInputStream(specFile))) {
                ConfigRO c = NodeSettings.loadFromXML(in);
                return DataTableSpec.load(c);
            }
        } else {
            throw new IOException("No such file \""
                    + specFile.getAbsolutePath() + "\"");
        }
    }

    /** Factory method to restore a table that has been written using
     * the save method.
     * @param dirRef The directory to load from.
     * @param settings The settings to load from.
     * @param exec The exec mon for progress/cancel
     * @param tblRep The table repository
     * @param dataRepository The data repository (needed for blobs and file stores).
     * @return The table as written by save.
     * @throws IOException If reading fails.
     * @throws CanceledExecutionException If canceled.
     * @throws InvalidSettingsException If settings are invalid.
     */
    static BufferedDataTable loadFromFile(final ReferencedFile dirRef,
            final NodeSettingsRO settings, final ExecutionMonitor exec,
            final Map<Integer, BufferedDataTable> tblRep,
            final WorkflowDataRepository dataRepository)
            throws IOException, CanceledExecutionException,
            InvalidSettingsException {
        File dir = dirRef.getFile();
        NodeSettingsRO s;
        // in version 1.1.x and before, the information was stored in
        // an external data.xml (directly in the node dir)
        boolean isVersion11x;
        File dataXML = new File(dir, TABLE_DESCRIPTION_FILE);
        // no xml file present and no settings passed in method:
        // loading an exported workflow without data
        if (!dataXML.exists() && settings == null) {
            throw new IOException("No such data file: "
                    + dataXML.getAbsolutePath());
        }
        DataTableSpec spec;
        if (dataXML.exists()) { // version 1.2.0 and later
            try (InputStream input = new BufferedInputStream(new FileInputStream(dataXML))) {
                s = NodeSettings.loadFromXML(input);
            }
            spec = loadSpec(dirRef);
            isVersion11x = false;
        } else { // version 1.1.x
            s = settings.getNodeSettings(CFG_TABLE_META);
            spec = null; // needs to be read from zip file!
            isVersion11x = true;
        }
        int id = s.getInt(CFG_TABLE_ID);
        dataRepository.updateLastId(id);
        String fileName = s.getString(CFG_TABLE_FILE_NAME);
        ReferencedFile fileRef;
        if (fileName != null) {
            fileRef = new ReferencedFile(dirRef, fileName);
            File file = fileRef.getFile();
            if (!file.exists()) {
                throw new IOException("No such data file: " + fileRef);
            }
            if (!file.isFile() || !file.canRead()) {
                throw new IOException("Cannot read file " + fileRef);
            }
        } else {
            // for instance for a column filter node this is null.
            fileRef = null;
        }
        String tableType = CheckUtils.checkSettingNotNull(s.getString(CFG_TABLE_TYPE), "Table type must not be null");
        BufferedDataTable t;

        if (Arrays.asList(TABLE_TYPE_CONTAINER_CUSTOM, TABLE_TYPE_REARRANGE_COLUMN_CUSTOM).contains(tableType)) {
            checkFormat(s);
        }
        if (Arrays.asList(TABLE_TYPE_CONTAINER_COMPRESS, TABLE_TYPE_REARRANGE_COLUMN_COMPRESS).contains(tableType)) {
            checkCompression(s);
        }

        switch (tableType) {
            case TABLE_TYPE_REFERENCE_IN_SAME_NODE:
                return CheckUtils.checkSettingNotNull(tblRep.get(id),
                    "Table reference with ID %d not found in load map", id);
            case TABLE_TYPE_CONTAINER:
                if (isVersion11x) {
                    if (fileRef == null) {
                        throw new NullPointerException("Reference on file to load from has not been set.");
                    }
                    final ContainerTable cont = DataContainer.readFromZip(fileRef.getFile());
                    t = new BufferedDataTable(cont, id);
                    break;
                }
            case TABLE_TYPE_CONTAINER_CUSTOM: // added in 3.6
            case TABLE_TYPE_CONTAINER_COMPRESS: // added in 4.0
                final ContainerTable cont = BufferedDataContainer.readFromZipDelayed(fileRef, spec, id, dataRepository);
                t = new BufferedDataTable(cont, id);
                break;
            case TABLE_TYPE_REARRANGE_COLUMN_CUSTOM:
            case TABLE_TYPE_REARRANGE_COLUMN_COMPRESS:
            case TABLE_TYPE_REARRANGE_COLUMN:
            case TABLE_TYPE_JOINED:
            case TABLE_TYPE_VOID:
            case TABLE_TYPE_CONCATENATE:
            case TABLE_TYPE_WRAPPED:
            case TABLE_TYPE_NEW_SPEC:
            case TABLE_TYPE_EXTENSION:
                String[] referenceDirs;
                // in version 1.2.x and before there was one reference table at most
                // (no concatenate table in those versions)
                if (s.containsKey("table_reference")) {
                    String refDir = s.getString("table_reference");
                    referenceDirs = refDir == null ? new String[0] : new String[]{refDir};
                } else {
                    referenceDirs = s.getStringArray(CFG_TABLE_REFERENCE);
                }
                for (String reference : referenceDirs) {
                    CheckUtils.checkSettingNotNull(reference, "Reference dir is \"null\"");
                    ReferencedFile referenceDirRef = new ReferencedFile(dirRef, reference);
                    loadFromFile(referenceDirRef, s, exec, tblRep, dataRepository);
                }
                if (Arrays.asList(TABLE_TYPE_REARRANGE_COLUMN, TABLE_TYPE_REARRANGE_COLUMN_CUSTOM,
                    TABLE_TYPE_REARRANGE_COLUMN_COMPRESS).contains(tableType)) {
                    t = new BufferedDataTable(new RearrangeColumnsTable(fileRef, s, tblRep, spec, id, dataRepository, exec),
                        dataRepository);
                } else if (tableType.equals(TABLE_TYPE_JOINED)) {
                    JoinedTable jt = JoinedTable.load(s, spec, tblRep, dataRepository);
                    t = new BufferedDataTable(jt, dataRepository);
                } else if (tableType.equals(TABLE_TYPE_VOID)) {
                    VoidTable jt = VoidTable.load(spec);
                    t = new BufferedDataTable(jt, dataRepository);
                } else if (tableType.equals(TABLE_TYPE_CONCATENATE)) {
                    ConcatenateTable ct = ConcatenateTable.load(s, spec, tblRep, dataRepository);
                    t = new BufferedDataTable(ct, dataRepository);
                } else if (tableType.equals(TABLE_TYPE_WRAPPED)) {
                    WrappedTable wt = WrappedTable.load(s, tblRep, dataRepository);
                    t = new BufferedDataTable(wt, dataRepository);
                } else if (tableType.equals(TABLE_TYPE_NEW_SPEC)) {
                    TableSpecReplacerTable replTable;
                    if (isVersion11x) {
                        if (fileRef == null) {
                            throw new NullPointerException("Reference on file to load from has not been set.");
                        }
                        replTable = TableSpecReplacerTable.load11x(fileRef.getFile(), s, tblRep, dataRepository);
                    } else {
                        replTable = TableSpecReplacerTable.load(s, spec, tblRep, dataRepository);
                    }
                    t = new BufferedDataTable(replTable, dataRepository);
                } else if (tableType.equals(TABLE_TYPE_EXTENSION)) {
                    ExtensionTable et = ExtensionTable.loadExtensionTable(fileRef, spec, s, tblRep, exec, dataRepository);
                    t = new BufferedDataTable(et, dataRepository);
                } else {
                    assert false : "Insufficent case switch: " + tableType;
                    throw new InvalidSettingsException("Unknown table identifier: " + tableType);
                }
                break;
            default:
                throw new InvalidSettingsException("Unknown table identifier: " + tableType);
        }
        t.m_tableID = id;
        tblRep.put(id, t);
        return t;
    }

    private static void checkFormat(final NodeSettingsRO settings) throws InvalidSettingsException {
        String formatFQN =
            CheckUtils.checkSettingNotNull(settings.getString(CFG_TABLE_CONTAINER_FORMAT), "Container format is null");
        TableStoreFormat format = TableStoreFormatRegistry.getInstance().getTableStoreFormat(formatFQN);
        String versionString = CheckUtils.checkSettingNotNull(settings.getString(CFG_TABLE_CONTAINER_FORMAT_VERSION),
            "Version string is null");
        CheckUtils.checkSetting(format.validateVersion(versionString),
            "Unsupported version \"%s\" for table format \"%s\"", versionString, format.getClass().getName());
    }

    private static void checkCompression(final NodeSettingsRO settings) throws InvalidSettingsException {
        String compressionName = CheckUtils.checkSettingNotNull(settings.getString(CFG_TABLE_COMPRESSION_FORMAT),
            "Compression format is null");
        CheckUtils.checkSetting(DefaultTableStoreFormat.validateCompressionFormat(compressionName),
            "Unsupported compression format \"%s\"", compressionName);
    }

    /**
     * @return Returns the owner.
     */
    Node getOwner() {
        return m_owner;
    }

    /**
     * @param owner The owner to set.
     */
    void setOwnerRecursively(final Node owner) {
        if (m_owner == null) {
            m_owner = owner;
            BufferedDataTable[] references = m_delegate.getReferenceTables();
            for (BufferedDataTable reference : references) {
                reference.setOwnerRecursively(owner);
            }
        }
    }

    /** Finds all tables owned by the argument node, which are directly
     * reachable (including this table).
     * @param dataOwner The owner.
     * @param result The set to add to. */
    synchronized void collectTableAndReferencesOwnedBy(
            final Node dataOwner, final Collection<BufferedDataTable> result) {
        if (dataOwner != getOwner()) {
            return;
        }
        result.add(this);
        BufferedDataTable[] references = m_delegate.getReferenceTables();
        for (BufferedDataTable reference : references) {
            reference.collectTableAndReferencesOwnedBy(dataOwner, result);
        }
    }

    /** Clears any associated storage, for instance temp files. This call does
     * not clear referenced tables owned by the same node.
     * @param dataOwner The owner of the tables. Used for assertion (table
     * must be owned by argument node!)
     */
    synchronized void clearSingle(final Node dataOwner) {
        // only take responsibility for our data tables
        if (dataOwner != getOwner()) {
            // this really is an assertion
            throw new IllegalStateException("Table not created by owner node");
        }
        synchronized (m_isCleared) {
            if (m_isCleared.booleanValue()) {
                return;
            }
            m_isCleared.setValue(true);
            m_delegate.clear();
        }
    }

    /** Reads table from its saved location (usually the workspace). Used
     * to allow for later re-saving in a cleared workspace (used for
     * version hop) */
    void ensureOpen() {
        BufferedDataTable[] references = m_delegate.getReferenceTables();
        for (BufferedDataTable reference : references) {
            reference.ensureOpen();
        }
        m_delegate.ensureOpen();
    }

    /**
     * Internally used interface. You won't have any benefit by implementing
     * this interface! It's used for selected classes in the KNIME core.
     *
     * @noimplement This interface is not intended to be implemented by clients.
     */
    public interface KnowsRowCountTable extends SizeAwareDataTable {
        /**
         * Row count of the table.
         * @return The row count.
         * @since 3.0
         * @deprecated use {@link #size()} instead which supports more than {@link Integer#MAX_VALUE} rows
         */
        @Deprecated
        int getRowCount();

        /**
         * Returns the number of rows in this table.
         *
         * @return the number of rows
         * @since 3.0
         */
        @Override
        long size();

        /** Save the table to a file.
         * @param f To write to.
         * @param settings To add meta information to.
         * @param exec For progress/cancel.
         * @throws IOException If writing fails.
         * @throws CanceledExecutionException If canceled.
         */
        void saveToFile(final File f, final NodeSettingsWO settings,
                final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException;

        /** Clears any allocated temporary files. The table won't be used
         * anymore.
         */
        void clear();

        /** Implementation of link BufferedDataTable#ensureOpen(). */
        void ensureOpen();

        /** Overridden to narrow return type to closeable iterator.
         * {@inheritDoc} */
        @Override
        CloseableRowIterator iterator();

        /**
         * @return {@link RowCursor} to access rows of a table.
         * @apiNote Must not to be called by clients. Experimental API.
         * @since 4.3
         */
        @Override
        RowCursor cursor();

        /**
         * @param filter to filter rows and columns.
         *
         * @return {@link RowCursor} to access rows of a table.
         * @apiNote Must not to be called by clients. Experimental API.
         * @since 4.3
         */
        RowCursor cursor(final TableFilter filter);

        /**
         * Provides a {@link CloseableRowIterator} that is filtered according to a given {@link TableFilter}. The
         * filtering won't change this KnowsRowCountTable or impact subsequent calls of this method with other filters.
         *
         * @param filter the filter to be applied
         * @return a filtered iterator
         * @since 4.0
         */
        default CloseableRowIterator iteratorWithFilter(final TableFilter filter) {
            return iteratorWithFilter(filter, null);
        }

        /**
         * Provides a {@link CloseableRowIterator} that is filtered according to a given {@link TableFilter}. During
         * iteration, a given {@link ExecutionMonitor} will update its progress. The filtering won't change this
         * KnowsRowCountTable or impact subsequent calls of this method with other filters.
         *
         * @param filter the filter to be applied
         * @param exec the execution monitor that shall be updated with progress or null if no progress updates are
         *            desired
         * @return a filtered iterator
         * @since 4.0
         */
        CloseableRowIterator iteratorWithFilter(TableFilter filter, ExecutionMonitor exec);

        /** Reference to the underlying tables, if any. A reference
         * table exists if this object is just a wrapper, such as a
         * RearrangeColumnsTable or if this table concatenates a set of
         * other tables.
         * @return The reference table or <code>null</code>.
         */
        BufferedDataTable[] getReferenceTables();

        /** Put this table into the global table repository. Called when
         * execution finished.
         * @param dataRepository The workflow table repository.
         * @since 3.7
         */
        void putIntoTableRepository(final WorkflowDataRepository dataRepository);

        /** Remove this table from global table repository. Called when
         * node is reset.
         * @param dataRepository The workflow table repository.
         * @return If this table was indeed removed from the table repository
         *         (true for ordinary container tables but false for wrappers
         *         such as concatenate or spec replacer)
         * @since 3.7
         */
        boolean removeFromTableRepository(final WorkflowDataRepository dataRepository);

        /**
         * Checks if the row count is greater than {@link Integer#MAX_VALUE}. If this is the case an exception is
         * thrown, otherwise the number of rows is returned as an int.
         *
         * @param count the number of rows
         * @return the number of rows
         * @throws IllegalStateException if the number of rows is greater than {@link Integer#MAX_VALUE}
         * @since 3.0
         */
        static int checkRowCount(final long count) {
            if (count > Integer.MAX_VALUE) {
                throw new IllegalStateException("Row count is greater than " + Integer.MAX_VALUE + ". The current node "
                    + "cannot handle more than this number. Please ask the vendor to update the implementation.");
            }
            return (int) count;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        if (!SwingUtilities.isEventDispatchThread()) {
            // this method is usually not called in the EDT (see OutPortView
            // class), but setting the table into the TableView will queue into
            // the EDT. First time access on the table might be expensive when
            // the table is restored from disk, so let's help here and do the
            // copy in this non-UI thread
            ensureOpen();
        }
        if (m_delegate instanceof ExtensionTable) {
            JComponent[] views = ((ExtensionTable)m_delegate).getViews(this);
            if (views != null && views.length > 0) {
                return views;
            }
        }
        return new JComponent[] {new BufferedDataTableView(this)};
    }
}


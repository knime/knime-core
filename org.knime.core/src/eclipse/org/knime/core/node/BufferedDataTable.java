/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 * History
 *   Jul 5, 2006 (wiswedel): created
 */
package org.knime.core.node;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ConcatenateTable;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.JoinedTable;
import org.knime.core.data.container.RearrangeColumnsTable;
import org.knime.core.data.container.TableSpecReplacerTable;
import org.knime.core.data.container.WrappedTable;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.BufferedDataTableView;
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
public final class BufferedDataTable implements DataTable, PortObject {

    /** Define port type of objects of this class when used as PortObjects.
     */
    public static final PortType TYPE = new PortType(BufferedDataTable.class);

    /** A port type representing an optional input table (as used, for instance
     * in the Concatenate node).
     * @since 2.6 */
    public static final PortType TYPE_OPTIONAL =
        new PortType(BufferedDataTable.class, true);

    /** internal ID for any generated table. */
    private static final AtomicInteger LAST_ID = new AtomicInteger(0);

    /**
     * Method that is used internally while the workflow is being loaded. Not
     * intended to be used directly by node implementations.
     * @param tblRep The table repository
     * @param tableID The table ID
     * @return The table from the repository.
     * @throws InvalidSettingsException If no such table exists.
     */
    public static BufferedDataTable getDataTable(
            final Map<Integer, BufferedDataTable> tblRep,
            final Integer tableID) throws InvalidSettingsException {
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
        LAST_ID.set(Math.max(tableID, LAST_ID.get()));
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

    /** Returns a table identifier and increments the internal counter.
     * @return Table identifier.
     */
    static int generateNewID() {
        return LAST_ID.incrementAndGet();
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

    /** Creates a new buffered data table based on a container table
     * (caching everything).
     * @param table The reference.
     * @param bufferID The buffer ID.
     */
    BufferedDataTable(final ContainerTable table, final int bufferID) {
        this((KnowsRowCountTable)table, bufferID);
    }

    /** Creates a new buffered data table based on a changed columns table
     * (only memorize columns that changed).
     * @param table The reference.
     */
    BufferedDataTable(final RearrangeColumnsTable table) {
        this(table, table.getAppendTable() != null
                ? table.getAppendTable().getBufferID() : generateNewID());
    }

    /** Creates a new buffered data table based on a changed spec table
     * (only keep new spec).
     * @param table The reference.
     */
    BufferedDataTable(final TableSpecReplacerTable table) {
        this(table, generateNewID());
    }

    /** Creates a new buffered data table based on a wrapped table.
     * @param table The reference.
     */
    BufferedDataTable(final WrappedTable table) {
        this(table, generateNewID());
    }

    /** Creates a new buffered data table based on a concatenation of
     * BufferedDataTables.
     * @param table The reference.
     */
    BufferedDataTable(final ConcatenateTable table) {
        this(table, generateNewID());
    }

    /** Creates a new buffered data table based on a join of
     * BufferedDataTables.
     * @param table The reference.
     */
    BufferedDataTable(final JoinedTable table) {
        this(table, generateNewID());
    }

    /**
     * Creates a new BufferedDataTable for an extended table type.
     * @param table The extended table
     */
    BufferedDataTable(final ExtensionTable table) {
        this(table, generateNewID());
    }

    private BufferedDataTable(final KnowsRowCountTable table, final int id) {
        m_delegate = table;
        assert id <= LAST_ID.get() : "Table identifiers not unique";
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
     * @param rep The repository from the workflow
     */
    void putIntoTableRepository(final HashMap<Integer, ContainerTable> rep) {
        m_delegate.putIntoTableRepository(rep);
        BufferedDataTable[] references = m_delegate.getReferenceTables();
        for (BufferedDataTable reference : references) {
            reference.putIntoTableRepository(rep);
        }
    }

    /** Remove this table and all of its delegates from the table repository,
     * if and only if its owner is the argument node.
     * @param rep The repository to be removed from.
     * @param owner The dedicated owner.
     * @return The number of tables effectively removed, used for assertions.
     */
    int removeFromTableRepository(final HashMap<Integer, ContainerTable> rep,
            final Node owner) {
        if (getOwner() != owner) { // can safely test for hard references here
            return 0;
        }
        int result = 0;
        BufferedDataTable[] references = m_delegate.getReferenceTables();
        for (BufferedDataTable reference : references) {
            result += reference.removeFromTableRepository(rep, owner);
        }
        if (m_delegate.removeFromTableRepository(rep)) {
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
        return "Rows: " + getRowCount()
            + ", Cols: " + getSpec().getNumColumns();
    }

    /** {@inheritDoc} */
    @Override
    public CloseableRowIterator iterator() {
        return m_delegate.iterator();
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
     * suggested by the {@link #getRowCount()} method.
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
     */
    public int getRowCount() {
        return m_delegate.getRowCount();
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
        private final int m_maxRows;
        private final CloseableRowIterator m_it;
        private int m_rowIndex;

        private CloseableFailProveRowIterator(final CloseableRowIterator it) {
            m_it = it;
            m_cellCount = getDataTableSpec().getNumColumns();
            m_maxRows = getRowCount();
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
    private static final String TABLE_TYPE_CONTAINER = "container_table";
    private static final String TABLE_TYPE_REARRANGE_COLUMN =
        "rearrange_columns_table";
    private static final String TABLE_TYPE_NEW_SPEC = "new_spec_table";
    private static final String TABLE_TYPE_WRAPPED = "wrapped_table";
    private static final String TABLE_TYPE_CONCATENATE = "concatenate_table";
    private static final String TABLE_TYPE_JOINED = "joined_table";
    /** The table is referenced multiple times in a node, e.g. provided at
     * different outputs (possibly wrapped) or it is used as output-port table
     * and as internally held table. See bug 2117.
     * @since 2.6 */
    private static final String TABLE_TYPE_REFERENCE_IN_SAME_NODE = "reference_from_same_node_table";
    private static final String TABLE_TYPE_EXTENSION = "extension_table";
    private static final String TABLE_SUB_DIR = "reference";
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
        } else if (m_delegate instanceof ContainerTable) {
            s.addString(CFG_TABLE_TYPE, TABLE_TYPE_CONTAINER);
            m_delegate.saveToFile(outFile, s, exec);
        } else {
            if (m_delegate instanceof RearrangeColumnsTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_REARRANGE_COLUMN);
            } else if (m_delegate instanceof TableSpecReplacerTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_NEW_SPEC);
            } else if (m_delegate instanceof WrappedTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_WRAPPED);
            } else if (m_delegate instanceof JoinedTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_JOINED);
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
                    String dirName = TABLE_SUB_DIR + "_" + index;
                    File subDir = new File(dir, dirName);
                    subDir.mkdir();
                    if (!subDir.exists() || !subDir.canWrite()) {
                        throw new IOException("Unable to write directory "
                                + subDir.getAbsolutePath());
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
        s.saveToXML(new BufferedOutputStream(new FileOutputStream(dataXML)));
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
        c.saveToXML(new BufferedOutputStream(new FileOutputStream(specFile)));
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
            ConfigRO c = NodeSettings.loadFromXML(new BufferedInputStream(
                    new FileInputStream(specFile)));
            return DataTableSpec.load(c);
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
     * @param bufferRep The buffer repository (needed for blobs).
     * @param fileStoreHandlerRepository ...
     * @return The table as written by save.
     * @throws IOException If reading fails.
     * @throws CanceledExecutionException If canceled.
     * @throws InvalidSettingsException If settings are invalid.
     */
    static BufferedDataTable loadFromFile(final ReferencedFile dirRef,
            final NodeSettingsRO settings, final ExecutionMonitor exec,
            final Map<Integer, BufferedDataTable> tblRep,
            final HashMap<Integer, ContainerTable> bufferRep,
            final FileStoreHandlerRepository fileStoreHandlerRepository)
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
            s = NodeSettings.loadFromXML(
                    new BufferedInputStream(new FileInputStream(dataXML)));
            spec = loadSpec(dirRef);
            isVersion11x = false;
        } else { // version 1.1.x
            s = settings.getNodeSettings(CFG_TABLE_META);
            spec = null; // needs to be read from zip file!
            isVersion11x = true;
        }
        int id = s.getInt(CFG_TABLE_ID);
        LAST_ID.set(Math.max(LAST_ID.get(), id + 1));
        String fileName = s.getString(CFG_TABLE_FILE_NAME);
        ReferencedFile fileRef;
        if (fileName != null) {
            fileRef = new ReferencedFile(dirRef, fileName);
            File file = fileRef.getFile();
            if (!file.exists()) {
                throw new IOException("No such data file: " + fileRef);
            }
            if (!file.isFile() || !file.canRead()) {
                throw new IOException("Can not read file " + fileRef);
            }
        } else {
            // for instance for a column filter node this is null.
            fileRef = null;
        }
        String tableType = s.getString(CFG_TABLE_TYPE);
        BufferedDataTable t;
        if (tableType.equals(TABLE_TYPE_REFERENCE_IN_SAME_NODE)) {
            t = tblRep.get(id);
            if (t == null) {
                throw new InvalidSettingsException("Table reference with ID "
                        + id + " not found in load map");
            }
            return t;
        } else if (tableType.equals(TABLE_TYPE_CONTAINER)) {
            ContainerTable fromContainer;
            if (isVersion11x) {
                fromContainer =
                    DataContainer.readFromZip(fileRef.getFile());
            } else {
                fromContainer =
                    BufferedDataContainer.readFromZipDelayed(fileRef, spec,
                            id, bufferRep, fileStoreHandlerRepository);
            }
            t = new BufferedDataTable(fromContainer, id);
        } else {
            String[] referenceDirs;
            // in version 1.2.x and before there was one reference table at most
            // (no concatenate table in those versions)
            if (s.containsKey("table_reference")) {
                String refDir = s.getString("table_reference");
                referenceDirs = refDir == null
                    ? new String[0] : new String[]{refDir};
            } else {
                referenceDirs = s.getStringArray(CFG_TABLE_REFERENCE);
            }
            for (String reference : referenceDirs) {
                if (reference == null) {
                    throw new InvalidSettingsException(
                            "Reference dir is \"null\"");
                }
                ReferencedFile referenceDirRef =
                    new ReferencedFile(dirRef, reference);
                loadFromFile(referenceDirRef, s, exec, tblRep, bufferRep,
                        fileStoreHandlerRepository);
            }
            if (tableType.equals(TABLE_TYPE_REARRANGE_COLUMN)) {
                t = new BufferedDataTable(
                        new RearrangeColumnsTable(fileRef, s, tblRep, spec,
                                id, bufferRep, fileStoreHandlerRepository));
            } else if (tableType.equals(TABLE_TYPE_JOINED)) {
                JoinedTable jt = JoinedTable.load(s, spec, tblRep);
                t = new BufferedDataTable(jt);
            } else if (tableType.equals(TABLE_TYPE_CONCATENATE)) {
                ConcatenateTable ct = ConcatenateTable.load(s, spec, tblRep);
                t = new BufferedDataTable(ct);
            } else if (tableType.equals(TABLE_TYPE_WRAPPED)) {
                WrappedTable wt = WrappedTable.load(s, tblRep);
                t = new BufferedDataTable(wt);
            } else if (tableType.equals(TABLE_TYPE_NEW_SPEC)) {
                TableSpecReplacerTable replTable;
                if (isVersion11x) {
                    replTable = TableSpecReplacerTable.load11x(
                            fileRef.getFile(), s, tblRep);
                } else {
                    replTable = TableSpecReplacerTable.load(s, spec, tblRep);
                }
                t = new BufferedDataTable(replTable);
            } else if (tableType.equals(TABLE_TYPE_EXTENSION)) {
                ExtensionTable et = ExtensionTable.loadExtensionTable(
                        fileRef, spec, s, tblRep, exec);
                t = new BufferedDataTable(et);
            } else {
                throw new InvalidSettingsException("Unknown table identifier: "
                        + tableType);
            }
        }
        t.m_tableID = id;
        tblRep.put(id, t);
        return t;
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

    /** Clears any associated storage, for instance temp files. This call also
     * clears all referenced tables (if they are owned by the same node).
     * @param dataOwner The owner of the tables. If
     * getOwner() != dataOwner, we return immediately.
     */
    synchronized void clear(final Node dataOwner) {
        // only take responsibility for our data tables
        if (dataOwner != getOwner()) {
            return;
        }
        synchronized (m_isCleared) {
            if (m_isCleared.booleanValue()) {
                return;
            }
            BufferedDataTable[] references = m_delegate.getReferenceTables();
            for (BufferedDataTable reference : references) {
                reference.clear(dataOwner);
            }
            m_isCleared.setValue(true);
            m_delegate.clear();
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

    /** Internally used interface. You won't have any benefit by implementing
     * this interface! It's used for selected classes in the KNIME core.
     */
    public static interface KnowsRowCountTable extends DataTable {
        /** Row count of the table.
         * @return The row count.
         */
        int getRowCount();

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

        /** Implementation of {@link BufferedDataTable#ensureOpen()}. */
        void ensureOpen();

        /** Overridden to narrow return type to closeable iterator.
         * {@inheritDoc} */
        @Override
        public CloseableRowIterator iterator();

        /** Reference to the underlying tables, if any. A reference
         * table exists if this object is just a wrapper, such as a
         * RearrangeColumnsTable or if this table concatenates a set of
         * other tables.
         * @return The reference table or <code>null</code>.
         */
        BufferedDataTable[] getReferenceTables();

        /** Put this table into the global table repository. Called when
         * execution finished.
         * @param rep The workflow table repository.
         */
        void putIntoTableRepository(final HashMap<Integer, ContainerTable> rep);

        /** Remove this table from global table repository. Called when
         * node is reset.
         * @param rep The workflow table repository.
         * @return If this table was indeed removed from the table repository
         *         (true for ordinary container tables but false for wrappers
         *         such as concatenate or spec replacer)
         */
        boolean removeFromTableRepository(
                final HashMap<Integer, ContainerTable> rep);

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


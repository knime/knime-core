/*
 * -------------------------------------------------------------------
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ConcatenateTable;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.JoinedTable;
import org.knime.core.data.container.RearrangeColumnsTable;
import org.knime.core.data.container.TableSpecReplacerTable;
import org.knime.core.data.container.WrappedTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;


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
     * (only memorize rows that changed).
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
    
    private BufferedDataTable(final KnowsRowCountTable table, final int id) {
        m_delegate = table;
        assert id <= LAST_ID.get() : "Table identifiers not unique";
        m_tableID = id;
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
    
    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_delegate.getDataTableSpec();
    }
    
    /**
     * {@inheritDoc}
     * @see #getDataTableSpec()
     */
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
    public CloseableRowIterator iterator() {
        return m_delegate.iterator();
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
    private static final String TABLE_SUB_DIR = "reference";
    private static final String TABLE_FILE = "data.zip";
    private static final String TABLE_DESCRIPTION_FILE = "data.xml";
    private static final String TABLE_SPEC_FILE = "spec.xml";
    
    
    /** Saves the table to a directory and writes some settings to the argument
     * NodeSettingsWO object. It will also write the reference table in case
     * this node is responsible for it (i.e. this node created the reference
     * table).
     * @param dir The directory to write to.
     * @param exec The progress monitor for cancellation.
     * @throws IOException If writing fails.
     * @throws CanceledExecutionException If canceled.
     */
    void save(final File dir, final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        NodeSettings s = new NodeSettings(CFG_TABLE_META);
        s.addInt(CFG_TABLE_ID, getBufferedTableId());
        if (m_delegate instanceof ContainerTable) {
            s.addString(CFG_TABLE_TYPE, TABLE_TYPE_CONTAINER);
        } else { 
            if (m_delegate instanceof RearrangeColumnsTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_REARRANGE_COLUMN);
            } else if (m_delegate instanceof TableSpecReplacerTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_NEW_SPEC);
            } else if (m_delegate instanceof WrappedTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_WRAPPED);
            } else if (m_delegate instanceof JoinedTable) {
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_JOINED);
            } else {
                assert m_delegate instanceof ConcatenateTable;
                s.addString(CFG_TABLE_TYPE, TABLE_TYPE_CONCATENATE);
            }
            BufferedDataTable[] references = m_delegate.getReferenceTables();
            ArrayList<String> referenceDirs = new ArrayList<String>();
            for (BufferedDataTable reference : references) {
                if (reference.getOwner() == getOwner()) {
                    int index = referenceDirs.size();
                    String dirName = TABLE_SUB_DIR + "_" + index;
                    File subDir = new File(dir, dirName);
                    subDir.mkdir();
                    if (!subDir.exists() || !subDir.canWrite()) {
                        throw new IOException("Unable to write directory "
                                + subDir.getAbsolutePath());
                    }
                    referenceDirs.add(dirName);
                    reference.save(subDir, exec);
                }
            }
            s.addStringArray(CFG_TABLE_REFERENCE, 
                    referenceDirs.toArray(new String[referenceDirs.size()]));
        }
        File outFile = new File(dir, TABLE_FILE);
        m_delegate.saveToFile(outFile, s, exec);
        // only write the data file to the spec if it has been created
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
        }
        return null;
    }
    
    /** Factory method to restore a table that has been written using
     * the save method.
     * @param dirRef The directory to load from.
     * @param settings The settings to load from.
     * @param exec The exec mon for progress/cancel
     * @param tblRep The table repository
     * @param bufferRep The buffer repository (needed for blobs).
     * @return The table as written by save.
     * @throws IOException If reading fails.
     * @throws CanceledExecutionException If canceled.
     * @throws InvalidSettingsException If settings are invalid.
     */
    static BufferedDataTable loadFromFile(final ReferencedFile dirRef,
            final NodeSettingsRO settings, final ExecutionMonitor exec,
            final Map<Integer, BufferedDataTable> tblRep, 
            final HashMap<Integer, ContainerTable> bufferRep) 
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
        if (tableType.equals(TABLE_TYPE_CONTAINER)) {
            ContainerTable fromContainer;
            if (isVersion11x) {
                fromContainer = 
                    BufferedDataContainer.readFromZip(fileRef.getFile()); 
            } else {
                fromContainer = 
                    BufferedDataContainer.readFromZipDelayed(
                            fileRef, spec, id, bufferRep);
            }
            t = new BufferedDataTable(fromContainer, id);
        } else if (tableType.equals(TABLE_TYPE_REARRANGE_COLUMN)
                || (tableType.equals(TABLE_TYPE_NEW_SPEC))
                || (tableType.equals(TABLE_TYPE_WRAPPED))
                || (tableType.equals(TABLE_TYPE_JOINED))
                || (tableType.equals(TABLE_TYPE_CONCATENATE))) {
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
                loadFromFile(referenceDirRef, s, exec, tblRep, bufferRep);
            }
            if (tableType.equals(TABLE_TYPE_REARRANGE_COLUMN)) {
                t = new BufferedDataTable(
                        new RearrangeColumnsTable(
                                fileRef, s, tblRep, spec, id, bufferRep));
            } else if (tableType.equals(TABLE_TYPE_JOINED)) {
                JoinedTable jt = JoinedTable.load(s, spec, tblRep);
                t = new BufferedDataTable(jt);
            } else if (tableType.equals(TABLE_TYPE_CONCATENATE)) {
                ConcatenateTable ct = ConcatenateTable.load(s, spec, tblRep);
                t = new BufferedDataTable(ct);
            } else if (tableType.equals(TABLE_TYPE_WRAPPED)) {
                WrappedTable wt = WrappedTable.load(s, tblRep);
                t = new BufferedDataTable(wt);
            } else {
                TableSpecReplacerTable replTable;
                if (isVersion11x) {
                    replTable = TableSpecReplacerTable.load11x(
                            fileRef.getFile(), s, tblRep);
                } else {
                    replTable = TableSpecReplacerTable.load(s, spec, tblRep);
                }
                t = new BufferedDataTable(replTable);
            }
        } else {
            throw new InvalidSettingsException("Unknown table identifier: "
                    + tableType);
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
    
    /** Clears any associated storage, for instance temp files.
     * @param dataOwner The owner of the tables. If 
     * getOwner() != dataOwner, we return immediately.
     */
    synchronized void clear(final Node dataOwner) {
        // only take responsibility for our data tables
        if (dataOwner != getOwner()) {
            return;
        }
        BufferedDataTable[] references = m_delegate.getReferenceTables();
        for (BufferedDataTable reference : references) {
            reference.clear(dataOwner);
        }
        m_delegate.clear();
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
         */
        void removeFromTableRepository(
                final HashMap<Integer, ContainerTable> rep);
    }
}


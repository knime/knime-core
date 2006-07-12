/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Jul 5, 2006 (wiswedel): created
 */
package de.unikn.knime.core.node;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.container.BufferedTable;
import de.unikn.knime.core.data.container.DataContainer;
import de.unikn.knime.core.data.container.RearrangeColumnsTable;
import de.unikn.knime.core.util.FileUtil;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public final class BufferedDataTable implements DataTable {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(BufferedDataTable.class);
    
    static final HashMap<Integer, HashMap<Integer, BufferedDataTable>>
        LOADER_HASH =  
            new HashMap<Integer, HashMap<Integer, BufferedDataTable>>();
    
    public static void initRepository(final int loadID) {
        LOGGER.debug("Adding new table repository for id " + loadID + ", ("
                + LOADER_HASH.size() + " in total)");
        LOADER_HASH.put(loadID, new HashMap<Integer, BufferedDataTable>());
    }
    
    public static void clearRepository(final int loadID) {
        Object removed = LOADER_HASH.remove(loadID);
        if (removed == null) {
            LOGGER.warn("No table repository for id " + loadID);
        } else {
            LOGGER.debug("Removed table repository for id " + loadID + ", ("
                    + LOADER_HASH.size() + " left)");
        }
    }
    
    public static BufferedDataTable getDataTable(final int loadID, 
            final Integer tableID) throws InvalidSettingsException {
        HashMap<Integer, BufferedDataTable> hash = LOADER_HASH.get(loadID);
        if (hash == null) {
            throw new InvalidSettingsException("No BufferedDataTable " 
                    + "repository for load ID " + loadID);
        }
        BufferedDataTable result = hash.get(tableID);
        if (result == null) {
            throw new InvalidSettingsException("No BufferedDataTable " 
                    + " with ID " + tableID);
        }
        // update the LASTID counter!
        assert result.m_tableID == tableID;
        LASTID = Math.max(tableID, LASTID);
        return result;
    }
    
    public static void putDataTable(final int loadID, 
            final BufferedDataTable t) throws InvalidSettingsException {
        HashMap<Integer, BufferedDataTable> hash = LOADER_HASH.get(loadID);
        if (hash == null) {
            throw new InvalidSettingsException("No BufferedDataTable " 
                    + "repository for load ID " + loadID);
        }
        hash.put(t.getBufferedTableId(), t);
    }
    
    static int LASTID = 0;
    private final KnowsRowCountTable m_delegate;
    private int m_tableID;
    private Node m_owner;
    
    public BufferedDataTable(BufferedTable table) {
        this((KnowsRowCountTable)table);
    }
    
    public BufferedDataTable(RearrangeColumnsTable table) {
        this((KnowsRowCountTable)table);
    }
    
    
    public static BufferedDataTable[] createBufferedDataTables(
            final DataTable[] tables, final ExecutionMonitor exec)
    throws CanceledExecutionException {
        BufferedDataTable[] temp = new BufferedDataTable[tables.length];
        for (int i = 0; i < tables.length; i++) {
            createBufferedDataTable(tables[i], exec);
        }
        return temp;
    }
    
    public static BufferedDataTable createBufferedDataTable(
            final DataTable table, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        boolean isKnown = false;
        if (table instanceof BufferedDataTable) {
            isKnown = true;
        } else if (table instanceof RearrangeColumnsTable) {
            isKnown = true;
        }
        if (isKnown) {
            LOGGER.coding("Attempted to create a BufferedDataTable "
                    + "with a known table implemenation (" 
                    + table.getClass().getSimpleName() + "), keeping only " 
                    + "reference. Use one of the constructors in the future!");
            return new BufferedDataTable((KnowsRowCountTable)table);
        } 
        return DataContainer.cache(table, exec);
    }
    
    private BufferedDataTable(KnowsRowCountTable table) {
        m_delegate = table;
        m_tableID = LASTID++;
    }
    
    BufferedDataTable getReferenceTable() {
        if (m_delegate instanceof RearrangeColumnsTable) {
            return ((RearrangeColumnsTable)m_delegate).getReferenceTable();
        } 
        return null;
    }
    
    /**
     * @see de.unikn.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_delegate.getDataTableSpec();
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        return m_delegate.iterator();
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#getRowCount()
     */
    public int getRowCount() {
        return m_delegate.getRowCount();
    }
    
    public Integer getBufferedTableId() {
        return m_tableID;
    }
    
    private static final String CFG_TABLE_META = "table_meta_info";
    private static final String CFG_TABLE_REFERENCE = "table_reference";
    private static final String CFG_TABLE_TYPE = "table_type";
    private static final String CFG_TABLE_ID = "table_ID";
    private static final String CFG_TABLE_FILE_NAME = "table_file_name";
    private static final String TABLE_TYPE_CONTAINER = "container_table";
    private static final String TABLE_TYPE_REARRANGE_COLUMN = 
        "rearrange_columns_table";
    private static final String TABLE_SUB_DIR = "reference";
    private static final String TABLE_FILE = "data.zip";
    
    void save(final File dir, final NodeSettings settings, 
            final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        NodeSettings s = settings.addConfig(CFG_TABLE_META);
        s.addInt(CFG_TABLE_ID, getBufferedTableId());
        File outFile = new File(dir, TABLE_FILE);
        s.addString(CFG_TABLE_FILE_NAME, TABLE_FILE);
        m_delegate.saveToFile(outFile, s, exec);
        if (m_delegate instanceof BufferedTable) {
            s.addString(CFG_TABLE_TYPE, TABLE_TYPE_CONTAINER);
        } else if (m_delegate instanceof RearrangeColumnsTable) {
            s.addString(CFG_TABLE_TYPE, TABLE_TYPE_REARRANGE_COLUMN);
            BufferedDataTable reference = 
                ((RearrangeColumnsTable)m_delegate).getReferenceTable();
            if (reference.getOwner() == getOwner()) {
                File subDir = new File(dir, TABLE_SUB_DIR);
                subDir.mkdir();
                if (!subDir.exists() || !subDir.canWrite()) {
                    throw new IOException("Unable to write directory "
                            + subDir.getAbsolutePath());
                }
                s.addString(CFG_TABLE_REFERENCE, TABLE_SUB_DIR);
                reference.save(subDir, s, exec);
            } else {
                s.addString(CFG_TABLE_REFERENCE, null);
            }
        }
    }
    
    static BufferedDataTable loadFromFile(final File dir,
            final NodeSettings settings, final ExecutionMonitor exec,
            final int loadID) throws IOException, CanceledExecutionException,
            InvalidSettingsException {
        HashMap<Integer, BufferedDataTable> hash =
            LOADER_HASH.get(loadID);
        if (hash == null) {
            throw new IOException(
                    "There is no table repository with ID " + loadID + "\n"
                    + "(Valid are " 
                    + Arrays.toString(LOADER_HASH.keySet().toArray())
                    + ")");
        }
        NodeSettings s = settings.getConfig(CFG_TABLE_META);
        int id = s.getInt(CFG_TABLE_ID);
        String fileName = s.getString(CFG_TABLE_FILE_NAME);
        File file = new File(dir, fileName);
        File dest = DataContainer.createTempFile();
        dest.deleteOnExit();
        FileUtil.copy(file, dest, exec);
        String tableType = s.getString(CFG_TABLE_TYPE);
        BufferedDataTable t;
        if (tableType.equals(TABLE_TYPE_CONTAINER)) {
            t = DataContainer.readFromZip(dest);
        } else if (tableType.equals(TABLE_TYPE_REARRANGE_COLUMN)) {
            String reference = s.getString(CFG_TABLE_REFERENCE, null);
            if (reference != null) {
                File referenceDir = new File(dir, reference);
                loadFromFile(referenceDir, s, exec, loadID);
            }
            t = new BufferedDataTable(
                    new RearrangeColumnsTable(dest, s, loadID));
        } else {
            throw new InvalidSettingsException("Unknown table identifier: "
                    + tableType);
        }
        t.m_tableID = id;
        LASTID = Math.max(id, LASTID);
        hash.put(id, t);
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
            if (m_delegate instanceof RearrangeColumnsTable) {
                BufferedDataTable reference = 
                    ((RearrangeColumnsTable)m_delegate).getReferenceTable();
                reference.setOwnerRecursively(owner);
            }
        }
    }

    public static interface KnowsRowCountTable extends DataTable {
        int getRowCount();
        void saveToFile(final File f, final NodeSettings settings, final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException;
    }
}




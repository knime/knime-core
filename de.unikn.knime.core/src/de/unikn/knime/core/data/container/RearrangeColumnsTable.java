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
package de.unikn.knime.core.data.container;

import java.io.File;
import java.io.IOException;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.BufferedDataTable.KnowsRowCountTable;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class RearrangeColumnsTable implements DataTable, KnowsRowCountTable {
    
    private static final RowKey DUMMY_KEY = new RowKey("non-existing");
    private static final DataRow DUMMY_ROW = 
        new DefaultRow(DUMMY_KEY, new DataCell[0]);
    
    private static final RowIterator EMPTY_ITERATOR = new RowIterator() {
        public boolean hasNext() {
            return true;
        }
        public DataRow next() {
            return DUMMY_ROW;
        };
    };

    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_ID = "table_reference_ID";
    private static final String CFG_MAP = "table_internal_map";
    private static final String CFG_FLAGS = "table_internal_flags";
    
    private final DataTableSpec m_spec;
    private final BufferedDataTable m_reference;
    private final int[] m_map;
    private final boolean[] m_isFromRefTable;
    private final Buffer m_appendBuffer;

    /**
     * 
     */
    RearrangeColumnsTable(final BufferedDataTable reference, final int[] map, 
            final boolean[] isFromRefTable, final DataColumnSpec[] newSpecs, 
            final CellFactory cellFactory, final ExecutionMonitor exec) 
            throws CanceledExecutionException {
        m_spec = ColumnRearranger.createSpec(
                reference.getDataTableSpec(), map, isFromRefTable, newSpecs);
        m_reference = reference;
        // for a pure filter (a table that just hides some columns from
        // "reference" but does not add any new column we avoid to scan
        // the entire table (nothing is written anyway)
        boolean hasNewColumns = false;
        for (int i = 0; i < isFromRefTable.length && !hasNewColumns; i++) {
            hasNewColumns |= !isFromRefTable[i];
        }
        if (hasNewColumns) {
            DataContainer container = new DataContainer(
                    new DataTableSpec(newSpecs), true) {
                protected Buffer newBuffer(final int rowsInMemory) {
                    return new NoKeyBuffer(rowsInMemory);
                }
            };
            double finalRowCount = reference.getRowCount();
            int r = 0;
            try {
                for (RowIterator it = reference.iterator(); it.hasNext(); r++) {
                    DataRow row = it.next();
                    DataCell[] cells = cellFactory.getCells(row);
                    DataRow appendix = new DefaultRow(row.getKey(), cells);
                    container.addRowToTable(appendix);
                    exec.setProgress(r / finalRowCount);
                    exec.checkCanceled();
                }
            } finally {
                container.close();
            }
            m_appendBuffer = container.getBuffer();
        } else {
            m_appendBuffer = null;
        }
        m_map = map;
        m_isFromRefTable = isFromRefTable;
    }
    
    public RearrangeColumnsTable(final File f, final NodeSettings settings,
            final int loadID) throws IOException, InvalidSettingsException {
        NodeSettings subSettings = settings.getConfig(CFG_INTERNAL_META);
        int tableID = subSettings.getInt(CFG_REFERENCE_ID);
        m_reference = BufferedDataTable.getDataTable(loadID, tableID);
        m_map = subSettings.getIntArray(CFG_MAP);
        m_isFromRefTable = subSettings.getBooleanArray(CFG_FLAGS);
        DataColumnSpec[] colSpecs;
        boolean containsFalse = false;
        for (int i = 0; !containsFalse && i < m_isFromRefTable.length; i++) {
            if (!m_isFromRefTable[i]) {
                containsFalse = true;
            }
        }
        if (containsFalse) {
            m_appendBuffer = new NoKeyBuffer(f, loadID);
            DataTableSpec appendSpec = m_appendBuffer.getTableSpec();
            colSpecs = new DataColumnSpec[appendSpec.getNumColumns()];
            for (int i = 0; i < appendSpec.getNumColumns(); i++) {
                colSpecs[i] = appendSpec.getColumnSpec(i);
            }
        } else {
            m_appendBuffer = null;
            colSpecs = new DataColumnSpec [0];
        }
        m_spec = ColumnRearranger.createSpec(m_reference.getDataTableSpec(), 
                m_map, m_isFromRefTable, colSpecs);
    }
    
    public BufferedDataTable getReferenceTable() {
        return m_reference;
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        RowIterator appendIt;
        if (m_appendBuffer != null) {
            appendIt = m_appendBuffer.iterator();
        } else {
            appendIt = EMPTY_ITERATOR;
        }
        return new JoinTableIterator(
                m_reference.iterator(), appendIt, m_map, m_isFromRefTable);
    }
    
    
    /**
     * @see de.unikn.knime.core.data.DataTable#getRowCount()
     */
    public int getRowCount() {
        return m_reference.getRowCount();
    }
    
    /**
     * @see KnowsRowCountTable#saveToFile(File, NodeSettings, ExecutionMonitor)
     */
    public void saveToFile(
            final File f, final NodeSettings s, final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        NodeSettings subSettings = s.addConfig(CFG_INTERNAL_META);
        subSettings.addInt(CFG_REFERENCE_ID, m_reference.getBufferedTableId());
        subSettings.addIntArray(CFG_MAP, m_map);
        subSettings.addBooleanArray(CFG_FLAGS, m_isFromRefTable);
        // create it anyway, even if size is 0
        f.createNewFile();
        if (m_appendBuffer != null) {
            m_appendBuffer.saveToFile(f, exec);
        }
    }
    
    
    /**
     * 
     * @author wiswedel, University of Konstanz
     */
    private class NoKeyBuffer extends Buffer {
        
        private static final String VERSION = "noRowKeyContainer_1.0.0";
        int m_loadID;
        
        NoKeyBuffer(final int maxRowsInMemory) {
            super(maxRowsInMemory);
        }
        
        /**
         * For writing.
         * @see Buffer#Buffer(File)
         */
        NoKeyBuffer(final File outFile, final NodeSettings additionalMeta) 
            throws IOException {
            super(outFile);
        }
        
        /**
         * For reading.
         * @see Buffer#Buffer(File, boolean)
         */
        NoKeyBuffer(final File inFile, final int loadID) 
            throws IOException {
            super(inFile, false);
        }
        
        /**
         * @see de.unikn.knime.core.data.container.Buffer#getVersion()
         */
        @Override
        public String getVersion() {
            return VERSION;
        }

        /**
         * @see Buffer#validateVersion(String)
         */
        @Override
        public void validateVersion(final String version) throws IOException {
            if (!VERSION.equals(version)) {
                throw new IOException("Unsupported version: \"" + version 
                        + "\" (expected \"" + VERSION + "\")");
            }
        }
        
        @Override
        public void addRow(final DataRow row) {
            if (row.getNumCells() > 0) {
                super.addRow(row);
            } else {
                incrementSize();
            }
        }
        
        @Override
        public RowIterator iterator() {
            if (getTableSpec().getNumColumns() > 0) {
                return super.iterator();
            } else {
                return new RowIterator() {
                    private int m_count = 0;
                    public boolean hasNext() {
                        return m_count < size();
                    }
                    public DataRow next() {
                        m_count++;
                        return DUMMY_ROW;
                    };
                };
            }
        }
        
        /**
         * Does nothing as row keys are not stored.
         * @see Buffer#writeRowKey(RowKey)
         */
        @Override
        void writeRowKey(final RowKey key) throws IOException {
            // left empty, uses always the same key
        }
        
        /**
         * Returns always the same key, does nothing to the stream.
         * @see Buffer#readRowKey(DCObjectInputStream)
         */
        @Override
        RowKey readRowKey(final DCObjectInputStream inStream) throws IOException {
            return DUMMY_KEY;
        }
    }
}

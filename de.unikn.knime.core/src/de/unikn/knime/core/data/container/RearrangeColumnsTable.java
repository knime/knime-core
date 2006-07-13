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
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;
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
    private final NoKeyBuffer m_appendBuffer;

    /**
     * 
     */
    RearrangeColumnsTable(final BufferedDataTable reference, final int[] map, 
            final boolean[] isFromRefTable, final DataTableSpec spec, 
            NoKeyBuffer appendBuffer) {
        m_spec = spec;
        m_reference = reference;
        m_appendBuffer = appendBuffer;
        m_map = map;
        m_isFromRefTable = isFromRefTable;
    }
    
    public RearrangeColumnsTable(final File f, final NodeSettingsRO settings,
            final int loadID) throws IOException, InvalidSettingsException {
        NodeSettingsRO subSettings = settings.getNodeSettings(CFG_INTERNAL_META);
        int tableID = subSettings.getInt(CFG_REFERENCE_ID);
        m_reference = BufferedDataTable.getDataTable(loadID, tableID);
        m_map = subSettings.getIntArray(CFG_MAP);
        m_isFromRefTable = subSettings.getBooleanArray(CFG_FLAGS);
        DataColumnSpec[] appendColSpecs;
        boolean containsFalse = false;
        for (int i = 0; !containsFalse && i < m_isFromRefTable.length; i++) {
            if (!m_isFromRefTable[i]) {
                containsFalse = true;
            }
        }
        if (containsFalse) {
            m_appendBuffer = new NoKeyBuffer(f, loadID);
            DataTableSpec appendSpec = m_appendBuffer.getTableSpec();
            appendColSpecs = new DataColumnSpec[appendSpec.getNumColumns()];
            for (int i = 0; i < appendSpec.getNumColumns(); i++) {
                appendColSpecs[i] = appendSpec.getColumnSpec(i);
            }
        } else {
            m_appendBuffer = null;
            appendColSpecs = new DataColumnSpec [0];
        }
        DataTableSpec refSpec = m_reference.getDataTableSpec();
        DataColumnSpec[] colSpecs = new DataColumnSpec[m_isFromRefTable.length];
        for (int i = 0; i < colSpecs.length; i++) {
            if (m_isFromRefTable[i]) {
                colSpecs[i] = refSpec.getColumnSpec(m_map[i]);
            } else {
                colSpecs[i] = appendColSpecs[m_map[i]];
            }
        }
        m_spec = new DataTableSpec(colSpecs);
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
            final File f, final NodeSettingsWO s, final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addInt(CFG_REFERENCE_ID, m_reference.getBufferedTableId());
        subSettings.addIntArray(CFG_MAP, m_map);
        subSettings.addBooleanArray(CFG_FLAGS, m_isFromRefTable);
        // create it anyway, even if size is 0
        f.createNewFile();
        if (m_appendBuffer != null) {
            m_appendBuffer.saveToFile(f, exec);
        }
    }
    
    
}

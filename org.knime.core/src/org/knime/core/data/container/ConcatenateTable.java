/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   Jan 5, 2007 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ConcatenateTable implements KnowsRowCountTable {
    
    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_IDS = "table_reference_IDS";
    private static final String CFG_ROW_COUNT = "table_rowcount";

    private final BufferedDataTable[] m_tables;
    private final DataTableSpec m_spec;
    private final int m_rowCount;
    
    private ConcatenateTable(final BufferedDataTable[] tables, 
            final DataTableSpec spec, final int rowCount) {
        m_tables = tables;
        m_spec = spec;
        m_rowCount = rowCount;
    }

    /** Internal use.
     * {@inheritDoc} */
    public void clear() {
        // left empty, it's up to the node to clear our underlying tables.
    }
    
    /** Internal use.
     * {@inheritDoc} */
    public void ensureOpen() {
        for (BufferedDataTable t : m_tables) {
            // use public method in class Node to do it
            // (ensureOpen() has only package scope in class BDT)
            Node.invokeEnsureOpen(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public BufferedDataTable[] getReferenceTables() {
        return m_tables;
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return m_rowCount;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /** {@inheritDoc} */
    public CloseableRowIterator iterator() {
        return new MyIterator();
    }

    /** {@inheritDoc} */
    public void putIntoTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
    }

    /** {@inheritDoc} */
    public void removeFromTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
    }

    /** {@inheritDoc} */
    public void saveToFile(final File f, final NodeSettingsWO s,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        int[] referenceIDs = new int[m_tables.length];
        for (int i = 0; i < m_tables.length; i++) {
            referenceIDs[i] = m_tables[i].getBufferedTableId();
        }
        subSettings.addIntArray(CFG_REFERENCE_IDS, referenceIDs);
        subSettings.addInt(CFG_ROW_COUNT, m_rowCount);
    }
    
    /** Restore table form node settings object.
     * @param s Containing information.
     * @param spec Associated spec.
     * @param tblRep For table lookup
     * @return The newly instantiated table.
     * @throws InvalidSettingsException If information is invalid.
     */
    public static ConcatenateTable load(final NodeSettingsRO s, 
            final DataTableSpec spec, 
            final Map<Integer, BufferedDataTable> tblRep) 
        throws InvalidSettingsException {
        NodeSettingsRO subSettings = s.getNodeSettings(CFG_INTERNAL_META);
        int[] referenceIDs = subSettings.getIntArray(CFG_REFERENCE_IDS);
        int rowCount = subSettings.getInt(CFG_ROW_COUNT);
        BufferedDataTable[] tables = new BufferedDataTable[referenceIDs.length];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = BufferedDataTable.getDataTable(tblRep, referenceIDs[i]);
        }
        return new ConcatenateTable(tables, spec, rowCount);
    }

    /** Creates a new table from argument tables.
     * @param mon for progress info/cancellation
     * @param tables Tables to put together.
     * @return The new table.
     * @throws CanceledExecutionException If cancelled.
     */
    public static ConcatenateTable create(
            final ExecutionMonitor mon, final BufferedDataTable... tables) 
            throws CanceledExecutionException {
        DataTableSpec[] specs = new DataTableSpec[tables.length];
        int rowCount = 0;
        for (int i = 0; i < tables.length; i++) {
            specs[i] = tables[i].getDataTableSpec();
            rowCount += tables[i].getRowCount();
        }
        DataTableSpec finalSpec = createSpec(specs);
        HashSet<RowKey> hash = new HashSet<RowKey>();
        int r = 0;
        for (int i = 0; i < tables.length; i++) {
            for (DataRow row : tables[i]) {
                RowKey key = row.getKey();
                if (!hash.add(key)) {
                    throw new IllegalArgumentException("Duplicate row key \"" 
                            + key + "\" in table with index " + i);
                }
                r++;
                mon.setProgress(r / (double)rowCount, "Checking tables, row " 
                        + r + "/" + rowCount + " (\"" + row.getKey() + "\")");
            }
            mon.checkCanceled();
        }
        return new ConcatenateTable(tables, finalSpec, rowCount);
    }
    
    /** Creates merged table spec.
     * @param specs the argument tables.
     * @return the new spec
     * @see DataTableSpec#mergeDataTableSpecs(DataTableSpec...)
     */
    public static DataTableSpec createSpec(final DataTableSpec... specs) {
        return DataTableSpec.mergeDataTableSpecs(specs);
    }
    
    private class MyIterator extends CloseableRowIterator {
        private int m_tableIndex;
        private CloseableRowIterator m_curIterator;
        private DataRow m_next;
        
        /** Creates new iterator. */
        public MyIterator() {
            m_tableIndex = 0;
            m_curIterator = m_tables[m_tableIndex].iterator();
            m_next = internalNext();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_next != null;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            DataRow next = m_next;
            m_next = internalNext();
            return next;
        }
        
        private DataRow internalNext() {
            if (m_curIterator.hasNext()) {
                return m_curIterator.next();
            } 
            if (m_tableIndex < m_tables.length - 1) {
                m_tableIndex++;
                m_curIterator = m_tables[m_tableIndex].iterator();
                return internalNext();
            } 
            return null;
        }
        
        /** {@inheritDoc} */
        @Override
        public void close() {
            m_curIterator.close();
            m_tableIndex = m_tables.length;
        }
        
    }

}

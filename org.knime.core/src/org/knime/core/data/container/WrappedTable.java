/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Aug 12, 2008 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;

/**
 * Special table implementation that simply wraps a given 
 * {@link BufferedDataTable}. This class is used by the framework and should not
 * be of public interest.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class WrappedTable implements KnowsRowCountTable {
    
    private final BufferedDataTable m_table;
    
    /** Creates new table wrapping the argument.
     * @param table Table to wrap
     * @throws NullPointerException If argument is null.
     */
    public WrappedTable(final BufferedDataTable table) {
        if (table == null) {
            throw new NullPointerException("Table must not be null.");
        }
        m_table = table;
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
    }

    /** {@inheritDoc} */
    @Override
    public void ensureOpen() {
        
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getReferenceTables() {
        return new BufferedDataTable[]{m_table};
    }

    /** {@inheritDoc} */
    @Override
    public int getRowCount() {
        return m_table.getRowCount();
    }

    /** {@inheritDoc} */
    @Override
    public CloseableRowIterator iterator() {
        return m_table.iterator();
    }

    /** {@inheritDoc} */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_table.getDataTableSpec();
    }
    
    /** {@inheritDoc} */
    @Override
    public void putIntoTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
    }

    /** {@inheritDoc} */
    @Override
    public void removeFromTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
    }
    
    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_ID = "table_reference_ID";

    /** {@inheritDoc} */
    @Override
    public void saveToFile(final File f, final NodeSettingsWO s,
            final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addInt(CFG_REFERENCE_ID, m_table.getBufferedTableId());
    }
    
    /** Restore table, reverse operation to 
     * {@link #saveToFile(File, NodeSettingsWO, ExecutionMonitor) save}.
     * @param s To load from
     * @param tblRep Global table loader map.
     * @return A freshly created wrapped table.
     * @throws InvalidSettingsException If settings are invalid.
     */
    public static WrappedTable load(final NodeSettingsRO s, 
            final Map<Integer, BufferedDataTable> tblRep) 
        throws InvalidSettingsException {
        NodeSettingsRO subSettings = s.getNodeSettings(CFG_INTERNAL_META);
        int refID = subSettings.getInt(CFG_REFERENCE_ID);
        BufferedDataTable reference = 
            BufferedDataTable.getDataTable(tblRep, refID);
        return new WrappedTable(reference);
    }

}

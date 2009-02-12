/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Jul 14, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;


/**
 * Table that only replaces the data table spec of an underlying table. This
 * class is not intended for subclassing or to be used in a node model 
 * implementation. Instead, use the methods provided through the execution
 * context.
 * @see org.knime.core.node.ExecutionContext#createSpecReplacerTable(
 *       BufferedDataTable, DataTableSpec)
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class TableSpecReplacerTable implements KnowsRowCountTable {
    
    private final BufferedDataTable m_reference;
    private final DataTableSpec m_newSpec;
    
    /** Creates new table. Not intended to be used directly for node 
     * implementations.
     * @param table The reference table.
     * @param newSpec Its new spec.
     * @throws IllegalArgumentException If the spec doesn't match the data
     * (number of columns)
     */
    public TableSpecReplacerTable(
            final BufferedDataTable table, final DataTableSpec newSpec) {
        DataTableSpec oldSpec = table.getDataTableSpec();
        if (oldSpec.getNumColumns() != newSpec.getNumColumns()) {
            throw new IllegalArgumentException("Table specs have different " 
                    + "lengths: " + oldSpec.getNumColumns() + " vs. " 
                    + newSpec.getNumColumns());
            // I don't think we can make more assertions here.
        }
        m_reference = table;
        m_newSpec = newSpec;
    }

    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_ID = "table_reference_ID";
    private static final String ZIP_ENTRY_SPEC = "newspec.xml";
    
    /**
     * {@inheritDoc}
     */
    public void saveToFile(final File f, final NodeSettingsWO s,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addInt(CFG_REFERENCE_ID, m_reference.getBufferedTableId());
    }
    
    /**
     * Does nothing.
     * {@inheritDoc}
     */
    public void putIntoTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
    }
    
    /**
     * Does nothing.
     * {@inheritDoc}
     */
    public void removeFromTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
    }
    
    /**
     * Restores table from a file that has been written using KNIME 1.1.x 
     * or before. Not intended to be used by node implementations.
     * @param f The file to read from.
     * @param s The settings to get meta information from.
     * @param tblRep The table repository
     * @return The resulting table.
     * @throws IOException If reading the file fails.
     * @throws InvalidSettingsException If reading the settings fails.
     */
    public static TableSpecReplacerTable load11x(
            final File f, final NodeSettingsRO s, 
            final Map<Integer, BufferedDataTable> tblRep) 
        throws IOException, InvalidSettingsException {
        ZipFile zipFile = new ZipFile(f);
        InputStream in = new BufferedInputStream(
                zipFile.getInputStream(new ZipEntry(ZIP_ENTRY_SPEC)));
        NodeSettingsRO specSettings = NodeSettings.loadFromXML(in);
        DataTableSpec newSpec = DataTableSpec.load(specSettings);
        return load(s, newSpec, tblRep);
    }

    /**
     * Restores table from a file that has been written using KNIME 1.2.0 
     * or later. Not intended to be used by node implementations.
     * @param s The settings to get meta information from.
     * @param newSpec The new table spec.
     * @param tblRep The table repository
     * @return The resulting table.
     * @throws InvalidSettingsException If reading the settings fails.
     */
    public static TableSpecReplacerTable load(final NodeSettingsRO s, 
            final DataTableSpec newSpec, 
            final Map<Integer, BufferedDataTable> tblRep) 
        throws InvalidSettingsException {
        NodeSettingsRO subSettings = s.getNodeSettings(CFG_INTERNAL_META);
        int refID = subSettings.getInt(CFG_REFERENCE_ID);
        BufferedDataTable reference = 
            BufferedDataTable.getDataTable(tblRep, refID);
        return new TableSpecReplacerTable(reference, newSpec);
    }
    
    /**
     * Do not call this method! It's used internally. 
     * {@inheritDoc}
     */
    public void clear() {
    }
    
    /** {@inheritDoc} */
    public void ensureOpen() {
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_newSpec;
    }

    /**
     * {@inheritDoc}
     */
    public CloseableRowIterator iterator() {
        return m_reference.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return m_reference.getRowCount();
    }
    
    /**
     * Get handle to reference table in an array of length 1.
     * @return Reference to that table.
     */
    public BufferedDataTable[] getReferenceTables() {
        return new BufferedDataTable[]{m_reference};
    }
}

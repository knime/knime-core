/* 
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
 *   Jul 14, 2006 (wiswedel): created
 */
package de.unikn.knime.core.data.container;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;
import de.unikn.knime.core.node.BufferedDataTable.KnowsRowCountTable;

/**
 * Table that only replaces the data table spec of an underlying table. This
 * class is not intended for subclassing or to be used in a node model 
 * implementation. Instead, use the method provided through the execution
 * context.
 * @see de.unikn.knime.core.node.ExecutionContext#createSpecReplacerTable(
 *       BufferedDataTable, DataTableSpec)
 * @author wiswedel, University of Konstanz
 */
public class TableSpecReplacerTable implements KnowsRowCountTable {
    
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

    /**
     * Restores table from a file. Not intended to be used by node 
     * implementations.
     * @param f The file to read from.
     * @param s The settings to get meta information from.
     * @param loadID The internal node id (to get the reference table from).
     * @throws IOException If reading the file fails.
     * @throws InvalidSettingsException If reading the settings fails.
     */
    public TableSpecReplacerTable(final File f, final NodeSettingsRO s, 
            final int loadID) throws IOException, InvalidSettingsException {
        NodeSettingsRO subSettings = s.getNodeSettings(CFG_INTERNAL_META);
        int refID = subSettings.getInt(CFG_REFERENCE_ID);
        m_reference = BufferedDataTable.getDataTable(loadID, refID);
        ZipFile zipFile = new ZipFile(f);
        InputStream in = new BufferedInputStream(
                zipFile.getInputStream(new ZipEntry(ZIP_ENTRY_SPEC)));
        NodeSettingsRO specSettings = NodeSettings.loadFromXML(in);
        m_newSpec = DataTableSpec.load(specSettings);
    }

    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_ID = "table_reference_ID";
    private static final String CFG_SPEC = "table_changed_spec";
    private static final String ZIP_ENTRY_SPEC = "newspec.xml";
    
    /**
     * @see de.unikn.knime.core.node.BufferedDataTable.KnowsRowCountTable
     * #saveToFile(File, NodeSettingsWO, ExecutionMonitor)
     */
    public void saveToFile(final File f, final NodeSettingsWO s,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addInt(CFG_REFERENCE_ID, m_reference.getBufferedTableId());
        ZipOutputStream zipOut = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(f)));
        zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_SPEC));
        NodeSettings specWriteSettings = new NodeSettings(CFG_SPEC);
        m_newSpec.save(specWriteSettings);
        // will also close the stream.
        specWriteSettings.saveToXML(zipOut);
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_newSpec;
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        return m_reference.iterator();
    }

    /**
     * @see de.unikn.knime.core.node.BufferedDataTable.KnowsRowCountTable
     * #getRowCount()
     */
    public int getRowCount() {
        return m_reference.getRowCount();
    }
    
    /**
     * Get handle to reference table.
     * @return Reference to that table.
     */
    public BufferedDataTable getReferenceTable() {
        return m_reference;
    }
}

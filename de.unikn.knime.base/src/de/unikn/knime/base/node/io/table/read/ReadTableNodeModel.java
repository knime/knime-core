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
 *   May 19, 2006 (wiswedel): created
 */
package de.unikn.knime.base.node.io.table.read;

import java.io.File;
import java.io.IOException;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.container.DataContainer;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;

/**
 * NodeMode for table that reads the file as written from the
 * Write table node.
 * @author wiswedel, University of Konstanz
 */
public class ReadTableNodeModel extends NodeModel {
    /** Identifier for the node settings object. */
    static final String CFG_FILENAME = "filename";
    
    /** The extension of the files to store, \".knime\". */
    public static final String PREFERRED_FILE_EXTENSION = ".table";
    
    private String m_fileName;

    /**
     * Creates new model with no inputs, one output.
     */
    public ReadTableNodeModel() {
        super(0, 1);
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    @Override
    protected void saveSettingsTo(final NodeSettings settings) {
        if (m_fileName != null) {
            settings.addString(CFG_FILENAME, m_fileName);
        }

    }

    /**
     * @see NodeModel#validateSettings(NodeSettings)
     */
    @Override
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        settings.getString(CFG_FILENAME);
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettings)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        m_fileName = settings.getString(CFG_FILENAME);
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionMonitor)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionMonitor exec) throws Exception {
        File f = new File(m_fileName);
        DataTable outTable = DataContainer.readFromZip(f);
        return new DataTable[]{outTable};
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_fileName == null) {
            throw new InvalidSettingsException("No file set.");
        }
        try {
            File f = new File(m_fileName);
            // doesn't hurt to read the table here. It will only parse
            // the spec, not the data content.
            DataTable outTable = DataContainer.readFromZip(f);
            return new DataTableSpec[]{outTable.getDataTableSpec()};
        } catch (IOException ioe) {
            String message = ioe.getMessage();
            if (message == null) {
                message = "Unable to read spec from file, "
                    + "no detailed message available.";
            }
            throw new InvalidSettingsException(message);
            
        }
    }

    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #loadInternals(java.io.File, de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }

    
    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #saveInternals(java.io.File, de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not internals to save
    }
}

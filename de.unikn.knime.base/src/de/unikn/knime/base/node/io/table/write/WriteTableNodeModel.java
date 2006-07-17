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
package de.unikn.knime.base.node.io.table.write;

import java.io.File;
import java.io.IOException;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.container.DataContainer;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionContext;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * NodeModel for the node to write arbitrary tables to a file. It only shows
 * a file chooser dialog.
 * @author wiswedel, University of Konstanz
 */
public class WriteTableNodeModel extends NodeModel {
    
    /** Config identifier for the settings object. */
    static final String CFG_FILENAME = "filename";
    
    private String m_fileName;
    
    /** Creates new NodeModel with one input, no output ports. */
    public WriteTableNodeModel() {
        super(1, 0);
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_fileName != null) {
            settings.addString(CFG_FILENAME, m_fileName);
        }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(CFG_FILENAME);
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName = settings.getString(CFG_FILENAME);
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, 
            final ExecutionContext exec) throws Exception {
        BufferedDataTable in = inData[0];
        DataContainer.writeToZip(in, new File(m_fileName), exec);
        return new BufferedDataTable[0];
    }

    /**
     * @see NodeModel#reset()
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
            throw new InvalidSettingsException("No file given.");
        }
        return new DataTableSpec[0];
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
        // no internals to save
    }

    
}

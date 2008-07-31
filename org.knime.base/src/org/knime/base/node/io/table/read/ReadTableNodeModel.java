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
 *   May 19, 2006 (wiswedel): created
 */
package org.knime.base.node.io.table.read;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


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
    
    private final SettingsModelString m_fileName = 
        new SettingsModelString(CFG_FILENAME, null);

    /**
     * Creates new model with no inputs, one output.
     */
    public ReadTableNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_fileName.getStringValue() != null) {
            m_fileName.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        File f = new File(m_fileName.getStringValue());
        DataTable table = DataContainer.readFromZip(f);
        BufferedDataTable out = exec.createBufferedDataTable(table, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_fileName.getStringValue() == null) {
            throw new InvalidSettingsException("No file set.");
        }
        try {
            File f = new File(m_fileName.getStringValue());
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
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not internals to save
    }
}

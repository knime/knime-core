/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   21.08.2005 (gabriel): created
 */
package org.knime.base.node.io.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.util.KnimeEncryption;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class DBReaderNodeModel extends DBReaderConnectionNodeModel {
    
    private DataTableSpec m_lastSpec = null;
    
    /**
     * Creates a new DB reader.
     */
    protected DBReaderNodeModel() {
        super(0, 1, 0, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        exec.setProgress(-1, "Opening database connection...");
        DBReaderConnection load = null;
        try {
            DBDriverLoader.registerDriver(getDriver());
            String password = KnimeEncryption.decrypt(getPassword());
            load = new DBReaderConnection(
                    getDatabaseName(), getUser(), password, getQuery());
            m_lastSpec = load.getDataTableSpec();
            return new BufferedDataTable[]{exec.createBufferedDataTable(load,
                exec)};
        } finally {
            if (load != null) {
                load.close();
            }
        }
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
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        File specFile = null;
        specFile = new File(nodeInternDir, "spec.xml");
        if (!specFile.exists()) {
            IOException ioe = new IOException("Spec file (\"" 
                    + specFile.getAbsolutePath() + "\") does not exist "
                    + "(node may have been saved by an older version!)");
            throw ioe;
        }
        NodeSettingsRO specSett = 
            NodeSettings.loadFromXML(new FileInputStream(specFile));
        try {
            m_lastSpec = DataTableSpec.load(specSett);
        } catch (InvalidSettingsException ise) {
            IOException ioe = new IOException("Could not read output spec.");
            ioe.initCause(ise);
            throw ioe;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        assert (m_lastSpec != null) : "Spec must not be null!";
        NodeSettings specSett = new NodeSettings("spec.xml");
        m_lastSpec.save(specSett);
        File specFile = new File(nodeInternDir, "spec.xml");
        specSett.saveToXML(new FileOutputStream(specFile));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void connectionChanged() {
        m_lastSpec = null;
    }
 
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (getDatabaseName() == null || getUser() == null 
                || getPassword() == null) {
            throw new InvalidSettingsException("No settings available "
                    + "to create database connection.");
        }
        if (m_lastSpec != null) {
            return new DataTableSpec[]{m_lastSpec};
        }
        DBDriverLoader.registerDriver(getDriver());
        try { 
            String password = KnimeEncryption.decrypt(getPassword());
            DBReaderConnection conn = new DBReaderConnection(
                    getDatabaseName(), getUser(), password, getQuery());
            m_lastSpec = conn.getDataTableSpec();
        } catch (Exception e) {
            throw new InvalidSettingsException("Could not establish "
                    + "connection to database: " + getDatabaseName());
        }
        return new DataTableSpec[]{m_lastSpec};
    }
}

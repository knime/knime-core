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
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBReaderNodeModel extends NodeModel {
    
    private DataTableSpec m_lastSpec = null;
    
    private String m_query = null;
    
    private final DBConnection m_conn;
    
    /**
     * Creates a new database reader.
     * @param nrDataIns number of data inputs
     * @param nrDataOuts number of data outputs
     */
    DBReaderNodeModel(final int nrDataIns, final int nrDataOuts) {
        super(nrDataIns, nrDataOuts);
        m_conn = new DBConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) 
            throws CanceledExecutionException, Exception {
        exec.setProgress("Opening database connection...");
        try {
            DBReaderConnection load = new DBReaderConnection(
                    new DBQueryConnection(m_conn, m_query, Integer.MAX_VALUE));
            m_lastSpec = load.getDataTableSpec();
            exec.setProgress("Reading data from database...");
            return new BufferedDataTable[]{load.createTable(exec)};
        } catch (Throwable t) {
            m_lastSpec = null;
            throw new RuntimeException(t);
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) 
            throws InvalidSettingsException {
        if (m_lastSpec != null) {
            return new DataTableSpec[]{m_lastSpec};
        }
        try {
            DBReaderConnection conn = new DBReaderConnection(
                    new DBQueryConnection(m_conn, m_query, Integer.MAX_VALUE));
            m_lastSpec = conn.getDataTableSpec();
            return new DataTableSpec[]{m_lastSpec};
        } catch (InvalidSettingsException e) {
            m_lastSpec = null;
            throw e;
        } catch (Throwable t) {
            m_lastSpec = null;
            throw new InvalidSettingsException(t);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String query = settings.getString(DBConnection.CFG_STATEMENT);
        if (query != null 
                && query.contains(DBQueryConnection.TABLE_PLACEHOLDER)) {
            throw new InvalidSettingsException(
                    "Database table place holder (" 
                    + DBQueryConnection.TABLE_PLACEHOLDER + ") not replaced.");
        }
        m_conn.validateConnection(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String query = settings.getString(DBConnection.CFG_STATEMENT);
        if (m_conn.loadValidatedConnection(settings)
                || query == null || !query.equals(m_query)) {
            m_lastSpec = null;
        }
        m_query = query;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_conn.saveConnection(settings);
        settings.addString(DBConnection.CFG_STATEMENT, m_query);
    }
}

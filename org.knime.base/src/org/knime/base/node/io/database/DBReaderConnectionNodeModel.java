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
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DatabasePortObject;
import org.knime.core.node.DatabasePortObjectSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class DBReaderConnectionNodeModel extends GenericNodeModel {
    
    private final DBQueryConnection m_conn;

    /**
     * Creates a new database reader.
     */
    DBReaderConnectionNodeModel() {
        super(new PortType[0], new PortType[]{DatabasePortObject.TYPE});
        m_conn = new DBQueryConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_conn.saveConnection(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_conn.loadValidatedConnection(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_conn.loadValidatedConnection(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        Exception e = m_conn.execute(m_conn.getQuery());
        if (e != null) {
            throw e;
        }
        ModelContentRO cont = m_conn.createConnectionModel();
        DBReaderConnection load = 
            new DBReaderConnection(m_conn, m_conn.getQuery(), 10);
        BufferedDataTable data = exec.createBufferedDataTable(load, exec);
        DatabasePortObject dbObj = new DatabasePortObject(data, cont);
        return new PortObject[]{dbObj};
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

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec spec = null;
        // try to create database connection
        try {
            m_conn.createConnection();
            DBReaderConnection reader = 
                new DBReaderConnection(m_conn, m_conn.getQuery());
            spec = reader.getDataTableSpec();
        } catch (Exception e) {
            throw new InvalidSettingsException(e.getMessage());
        }
        ModelContentRO cont = m_conn.createConnectionModel();
        DatabasePortObjectSpec dbSpec = new DatabasePortObjectSpec(spec, cont);
        return new PortObjectSpec[]{dbSpec};
    }

}

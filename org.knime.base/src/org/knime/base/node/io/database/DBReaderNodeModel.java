/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.KnimeEncryption;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class DBReaderNodeModel extends DBReaderConnectionNodeModel {

    private DBReaderConnection m_load = null;
    
    /**
     * Creates a new DB reader.
     */
    protected DBReaderNodeModel() {
        super(0, 1, 0, 0);
    }

    /**
     * @see org.knime.base.node.io.database.DBReaderConnectionNodeModel#execute(
     * org.knime.core.node.BufferedDataTable[], 
     * org.knime.core.node.ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        exec.setProgress(-1, "Opening database connection...");
        try {
            String password = KnimeEncryption.decrypt(getPassword());
            m_load = new DBReaderConnection(
                    getDatabaseName(), getUser(), password, getQuery());
            return new BufferedDataTable[]{exec.createBufferedDataTable(m_load,
                exec)};
        } finally {
            reset();
        }
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        if (m_load != null) {
            m_load.close();
            m_load = null;
        }
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {

    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {

    }

 
    /**
     * @see org.knime.base.node.io.database.DBReaderConnectionNodeModel
     *  #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        registerDriver();
        
        DataTableSpec spec = null;
        try { 
            String password = KnimeEncryption.decrypt(getPassword());
            DBReaderConnection conn = new DBReaderConnection(
                    getDatabaseName(), getUser(), password, getQuery());
            conn.getDataTableSpec();
        } catch (Exception e) {
            throw new InvalidSettingsException("Could not establish connection"
                    + " to database: " + getDatabaseName());
        }
        
        return new DataTableSpec[]{spec};
    }
}

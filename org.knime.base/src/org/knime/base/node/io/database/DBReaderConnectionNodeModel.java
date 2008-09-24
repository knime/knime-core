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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseReaderConnection;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBReaderConnectionNodeModel extends DBNodeModel {
    
    private final DatabaseReaderConnection m_load = 
        new DatabaseReaderConnection(); 
    
    /**
     * Creates a new database reader.
     */
    DBReaderConnectionNodeModel() {
        super(new PortType[0], new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        DatabaseQueryConnectionSettings conn = m_load.getQueryConnection();
        if (conn != null) {
            conn.saveConnection(settings);
        }
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new DatabaseQueryConnectionSettings(settings);
        super.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        DatabaseQueryConnectionSettings conn = 
            new DatabaseQueryConnectionSettings(settings, getNumCachedRows());
        try {
            m_load.setDBQueryConnection(conn);
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        }
        super.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) 
            throws CanceledExecutionException, Exception {
        DatabaseQueryConnectionSettings conn = m_load.getQueryConnection();
        DataTableSpec spec = m_load.getDataTableSpec();
        DatabasePortObject dbObj = new DatabasePortObject(
                new DatabasePortObjectSpec(spec, conn.createConnectionModel()));
        return new PortObject[]{dbObj};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        try {
            // try to create database connection
            DatabaseQueryConnectionSettings conn = m_load.getQueryConnection();
            DataTableSpec spec = m_load.getDataTableSpec();
            DatabasePortObjectSpec dbSpec = new DatabasePortObjectSpec(spec,
                    conn.createConnectionModel());
            return new PortObjectSpec[]{dbSpec};
        } catch (Throwable t) {
            throw new InvalidSettingsException(t);
        }
    }

}

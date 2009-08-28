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
 */
package org.knime.base.node.io.database;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class DBNodeModel extends NodeModel {
    
    NodeLogger LOGGER = NodeLogger.getLogger(DBNodeModel.class);
    
    /**
     * Creates a new database reader.
     * @param inPorts array of input port types
     * @param outPorts array of output port types
     */
    DBNodeModel(final PortType[] inPorts, final PortType[] outPorts) {
        super(inPorts, outPorts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

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
     * Creates a new query connection based on the connection settings, that 
     * is, either create a new table or wrap the SQL statement.
     * @param spec the database connection
     * @param newQuery the new query to execute
     * @param createTable true, if table should be created (e.g. during execute)
     * @return a database connection object
     * @throws InvalidSettingsException if the query to create the new table 
     *         inside the database could not be executed
     */
    final DatabaseQueryConnectionSettings createDBQueryConnection(
            final DatabasePortObjectSpec spec, final String newQuery) 
    		throws InvalidSettingsException {
    	DatabaseQueryConnectionSettings conn = 
    		new DatabaseQueryConnectionSettings(spec.getConnectionModel());
        return new DatabaseQueryConnectionSettings(conn, newQuery);
    }
        
}

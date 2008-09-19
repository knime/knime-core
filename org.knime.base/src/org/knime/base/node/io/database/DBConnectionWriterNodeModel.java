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
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBConnectionWriterNodeModel extends NodeModel {
    
    private SettingsModelString m_tableName = 
        DBConnectionWriterDialogPane.createTableNameModel();
    
    /**
     * Creates a new database connection reader.
     */
    DBConnectionWriterNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, new PortType[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) 
            throws CanceledExecutionException, Exception {
        DatabasePortObject dbObj = (DatabasePortObject) inData[0];
        exec.setProgress("Opening database connection...");
        String tableName = m_tableName.getStringValue();
        DBQueryConnection conn = new DBQueryConnection(
                dbObj.getConnectionModel());
        try {
            conn.execute("DROP TABLE " + tableName);
        } catch (Exception e) {
            // suppress exception thrown when table does not exist in database
        }
        conn.execute("CREATE TABLE " + tableName 
                + " AS (" + conn.getQuery() + ")");
        return new BufferedDataTable[0];
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
        try {
            DatabasePortObjectSpec spec = (DatabasePortObjectSpec) inSpecs[0];
            DBQueryConnection conn = new DBQueryConnection(
                    spec.getConnectionModel());
            conn.createConnection();
        } catch (InvalidSettingsException ise) {
            throw ise;
        } catch (Throwable t) {
            throw new InvalidSettingsException(t);
        }
        super.setWarningMessage("Existing table \"" 
                + m_tableName.getStringValue() + "\" will be dropped!");
        return new DataTableSpec[0];
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelString tableName = 
            m_tableName.createCloneWithValidatedValue(settings);
        String tableString = tableName.getStringValue();
        if (tableString == null || tableString.contains("<table_name>")) {
            throw new InvalidSettingsException("Database table place holder"
                    + " is not replaced!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_tableName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_tableName.saveSettingsTo(settings);
    }
    
}

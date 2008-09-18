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

import org.knime.base.node.io.database.DBConnectionDialogPanel.DBTableOptions;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
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
abstract class DBNodeModel extends GenericNodeModel {
    
    /** Config ID for temporary table. */
    static final String CFG_TABLE_ID = "tableID.xml";
    
    private final SettingsModelString m_tableOption =
        DBConnectionDialogPanel.createTableModel();

    private final SettingsModelIntegerBounded m_cachedRows =
        DBConnectionDialogPanel.createCachedRowsModel();
    
    private DBQueryConnection m_conn;

    private String m_tableId;
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(DBNodeModel.class);
    
    /**
     * Creates a new database reader.
     */
    DBNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, 
                new PortType[]{DatabasePortObject.TYPE});
        m_tableId = "table_" + System.identityHashCode(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_tableOption.saveSettingsTo(settings);
        m_cachedRows.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_tableOption.validateSettings(settings);
        m_cachedRows.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_tableOption.loadSettingsFrom(settings);
        m_cachedRows.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) 
            throws CanceledExecutionException, Exception {
        DatabasePortObject dbObj = (DatabasePortObject) inData[0];
        m_conn = new DBQueryConnection(dbObj.getConnectionModel());
        String newQuery = createQuery(m_conn.getQuery(), m_tableId); 
        LOGGER.debug("Executing SQL query during #execute: " + newQuery);
        if (DBTableOptions.CREATE_TABLE.getActionCommand().equals(
                m_tableOption.getStringValue())) {
            m_conn.execute("CREATE TABLE " + m_tableId + " AS " + newQuery);
            m_conn = new DBQueryConnection(m_conn, 
                    "SELECT * FROM " + m_tableId, Integer.MAX_VALUE);
        } else {
            m_conn = new DBQueryConnection(m_conn, newQuery, 
                    m_cachedRows.getIntValue());
        }
        DBReaderConnection load = new DBReaderConnection(m_conn);
        DatabasePortObject outObj = new DatabasePortObject(
                new DatabasePortObjectSpec(load.getDataTableSpec(),
                        m_conn.createConnectionModel()));
        return new PortObject[]{outObj};
    }
    
//    /**
//     *
//     */
//    protected abstract PortObject[] executeDB(final PortObject[] inData,
//            final ExecutionContext exec) 
//            throws CanceledExecutionException, Exception;
//    
//   /**
//    *
//    */
//   protected abstract PortObject[] configureDB(final PortObjectSpec[] inSpec) 
//           throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_conn != null) {
            if (DBTableOptions.CREATE_TABLE.getActionCommand().equals(
                    m_tableOption.getStringValue())) {
                try {
                    m_conn.execute("DROP TABLE " + m_tableId);
                } catch (Exception e) {
                    super.setWarningMessage("Can't drop table with id \"" 
                            + m_tableId + "\", reason: " + e.getMessage());
                }
            }
            m_conn = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettingsRO sett = NodeSettings.loadFromXML(new FileInputStream(
                new File(nodeInternDir, CFG_TABLE_ID)));
        m_tableId = sett.getString(CFG_TABLE_ID, m_tableId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettings sett = new NodeSettings(CFG_TABLE_ID);
        sett.addString(CFG_TABLE_ID, m_tableId);
        sett.saveToXML(new FileOutputStream(
                new File(nodeInternDir, CFG_TABLE_ID)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DatabasePortObjectSpec spec = (DatabasePortObjectSpec) inSpecs[0];
        m_conn.loadValidatedConnection(spec.getConnectionModel());
        String newQuery = createQuery(m_conn.getQuery(), m_tableId);
        LOGGER.debug("Executing SQL query during #configure: " + newQuery);
        DBQueryConnection conn = new DBQueryConnection(m_conn, newQuery,
                m_cachedRows.getIntValue());
        try {
            conn.createConnection();
            DBReaderConnection reader = new DBReaderConnection(conn);
            DataTableSpec outSpec = reader.getDataTableSpec();
            DatabasePortObjectSpec dbSpec = new DatabasePortObjectSpec(
                    outSpec, conn.createConnectionModel());
            return new PortObjectSpec[]{dbSpec};
        } catch (InvalidSettingsException ise) {
            throw ise;
        } catch (Throwable t) {
            throw new InvalidSettingsException(t);
        }
    }
    
    /**
     * Create and return a new query based on the given input 
     * <code>oldQuery</code>, the <code>tableId</code> has to be used to 
     * create an unique table. 
     * @param oldQuery the old SQL query from the input
     * @param tableID the table if
     * @return the new SQL query
     */
    abstract String createQuery(final String oldQuery, final String tableID);
        
}

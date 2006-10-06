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
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.HashSet;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class DBReaderNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DBReaderNodeModel.class);

    private DBReaderConnection m_load = null;

    private String m_driver = "sun.jdbc.odbc.JdbcOdbcDriver";

    private String m_query = "SELECT * FROM <table>";

    private String m_name = "jdbc:odbc:<database_name>";

    private String m_user = "<user>";

    private String m_pass = "";

    private final HashSet<String> m_driverLoaded = new HashSet<String>();
    
    static {        
        try {
            Class<?> driverClass = Class.forName(
                    "sun.jdbc.odbc.JdbcOdbcDriver");
            Driver theDriver = new WrappedDriver((Driver)driverClass
                    .newInstance());
            DriverManager.registerDriver(theDriver);
        } catch (Exception e) {
            LOGGER.warn("Could not load 'sun.jdbc.odbc.JdbcOdbcDriver'.");
            LOGGER.debug("", e);
        }
    }


    /**
     * Creates a new model with one data outport.
     */
    DBReaderNodeModel() {
        super(0, 1);
        reset();
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("driver", m_driver);
        settings.addString("statement", m_query);
        settings.addString("database", m_name);
        settings.addString("user", m_user);
        settings.addStringArray("loaded_driver", m_driverLoaded
                .toArray(new String[0]));
        // settings.addString("password", m_pass);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadSettings(settings, false);
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadSettings(settings, true);
    }

    private void loadSettings(final NodeSettingsRO settings, 
            final boolean write)
            throws InvalidSettingsException {
        String driver = settings.getString("driver");
        String statement = settings.getString("statement");
        String database = settings.getString("database");
        String user = settings.getString("user");
        // password
        String password = settings.getString("password", "");
        // loaded driver
        String[] loadedDriver = settings.getStringArray("loaded_driver");
        try {
            password = DBReaderConnection.decrypt(password);
        } catch (Exception e) {
            throw new InvalidSettingsException("Could not decrypt password", e);
        }
        if (write) {
            m_driver = driver;
            m_query = statement;
            m_name = database;
            m_user = user;
            m_pass = password;
            m_driverLoaded.clear();
            m_driverLoaded.addAll(Arrays.asList(loadedDriver));
            for (String fileName : m_driverLoaded) {
                try {
                    DBDriverLoader.loadDriver(new File(fileName));
                } catch (Exception e) {
                    LOGGER.info("Could not load driver from: " + loadedDriver);
                }
            }
        }
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        exec.setProgress(-1, "Opening database connection...");
        try {
            m_load = new DBReaderConnection(m_name, m_user, m_pass, m_query);
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
     * @see NodeModel#configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        try {
            DBReaderConnection conn = new DBReaderConnection(m_name, m_user,
                    m_pass, m_query);
            return new DataTableSpec[]{conn.getDataTableSpec()};
        } catch (Exception e) {
            throw new InvalidSettingsException("Could not establish connection"
                    + " to database: " + m_name);
        }
    }
}

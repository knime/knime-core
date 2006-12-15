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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.util.KnimeEncryption;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class DBReaderConnectionNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DBReaderConnectionNodeModel.class);

    private String m_driver = DBDriverLoader.JDBC_ODBC_DRIVER;

    private String m_query = null;

    private String m_name = null;
    private String m_user = null;
    private String m_pass = null;

    /**
     * The name of the view created on the database defined by the given query.
     */
    private String m_viewName;

    private final HashSet<String> m_driverLoaded = new HashSet<String>();

    static {
        try {
            Class<?> driverClass =
                    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            Driver theDriver =
                    new WrappedDriver((Driver)driverClass.newInstance());
            DriverManager.registerDriver(theDriver);
        } catch (Exception e) {
            LOGGER.warn("Could not load 'sun.jdbc.odbc.JdbcOdbcDriver'.");
            LOGGER.debug("", e);
        }
    }

    /**
     * Creates a new database reader.
     * 
     * @param dataIn data ins
     * @param dataOut data outs
     * @param modelIn model ins
     * @param modelOut models outs
     */
    DBReaderConnectionNodeModel(final int dataIn, final int dataOut,
            final int modelIn, final int modelOut) {
        super(dataIn, dataOut, modelIn, modelOut);
    }

    /**
     * @see NodeModel#saveModelContent(int, ModelContentWO)
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        saveConfig(predParams);

        // additionally save the view name
        predParams.addString("view", m_viewName);
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        saveConfig(settings);
    }

    private void saveConfig(final ConfigWO settings) {
        settings.addString("driver", m_driver);
        settings.addString("statement", m_query);
        settings.addString("database", m_name);
        settings.addString("user", m_user);
        settings.addStringArray("loaded_driver", m_driverLoaded
                .toArray(new String[0]));
        settings.addString("password", m_pass);
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
            final boolean write) throws InvalidSettingsException {
        String driver = settings.getString("driver");
        String statement = settings.getString("statement");
        String database = settings.getString("database");
        String user = settings.getString("user");
        // password
        String password = settings.getString("password", "");
        // loaded driver
        String[] loadedDriver = settings.getStringArray("loaded_driver");
        // write settings or skip it
        if (write) {
            m_driver = driver;
            m_query = statement;
            boolean changed = false;
            if (m_user != null && m_name != null && m_pass != null) { 
                if (!user.equals(m_user) || !database.equals(m_name)
                        || !password.equals(m_pass)) {
                    changed = true;
                }
            }
            m_name = database;
            m_user = user;
            m_pass = password;
            m_driverLoaded.clear();
            m_driverLoaded.addAll(Arrays.asList(loadedDriver));
            for (String fileName : m_driverLoaded) {
                try {
                    DBDriverLoader.loadDriver(new File(fileName));
                } catch (Exception e2) {
                    LOGGER.info("Could not load driver from: "
                            + loadedDriver, e2);
                }
            }
            if (changed) {
                connectionChanged();
            }
        }
    }
    
    /**
     * Called when the connection settings have changed, that are, database
     * name, user name, and password.
     */
    protected void connectionChanged() {
        
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {

        // create a view on the database according to the given select
        // statement
        // create a unique view name
        m_viewName = "VIEW_" + Long.toString(System.currentTimeMillis());
        String viewCreateSQL = "CREATE VIEW " + m_viewName 
            + " AS " + m_query;
        try {
            execute(viewCreateSQL);
        } catch (Exception e) {
            throw e;
        }
        return new BufferedDataTable[0];
    }
    
    private Exception execute(final String statement) {
        if (m_name == null || m_user == null || m_pass == null) {
            return new InvalidSettingsException("No settings available "
                    + "to create database connection.");
        }
        Connection conn = null;
        try {
            DBDriverLoader.registerDriver(getDriver());
            String password = KnimeEncryption.decrypt(m_pass);
            conn = DriverManager.getConnection(m_name, m_user, password);
            Statement stmt = conn.createStatement();
            stmt.execute(statement);
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException sqle) {
                    return sqle;
                }
            }
            return e;
        }
        return null;
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        if (m_viewName != null) {
            Exception e = execute("DROP VIEW " + m_viewName);
            if (e != null) {
                LOGGER.warn("Unable to delete view: " + m_viewName, e);
            }
            m_viewName = null;
        }
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettingsRO sett = NodeSettings.loadFromXML(new FileInputStream(
                new File(nodeInternDir, "internals.xml")));
        m_viewName = sett.getString("view", null);
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettings sett = new NodeSettings("internals");
        sett.addString("view", m_viewName);
        sett.saveToXML(new FileOutputStream(new File(
                nodeInternDir, "internals.xml")));
    }

    /**
     * @see NodeModel#configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DBDriverLoader.registerDriver(getDriver());
        return new DataTableSpec[0];
    }

    /**
     * @return user name to login to the database
     */
    protected final String getUser() {
        return m_user;
    }

    /**
     * @return password used to login to the database
     */
    protected final String getPassword() {
        return m_pass;
    }

    /**
     * @return database driver to create connection
     */
    protected final String getDriver() {
        return m_driver;
    }

    /**
     * @return database name to create connection to
     */
    protected final String getDatabaseName() {
        return m_name;
    }

    /**
     * @return SQl query/statement to execute
     */
    protected final String getQuery() {
        return m_query;
    }

}

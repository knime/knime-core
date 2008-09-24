/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 */
package org.knime.core.node.port.database;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.KnimeEncryption;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabaseConnectionSettings {
    
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(DatabaseConnectionSettings.class);
    
    /** Config for SQL statement. */
    public static final String CFG_STATEMENT = "statement";
    
    /** Keeps the history of all loaded driver and its order. */
    public static final StringHistory DRIVER_ORDER = StringHistory.getInstance(
            "database_drivers");
    
    /** Keeps the history of all driver URLs. */
    public static final StringHistory DRIVER_URLS = StringHistory.getInstance(
            "driver_urls");
    
    /** Keeps the history of all database URLs. */
    public static final StringHistory DATABASE_URLS = StringHistory.getInstance(
            "database_urls");

    private String m_driver;
    private String m_dbName;
    private String m_user = null;
    private String m_pass = null;

    /**
     */
    public DatabaseConnectionSettings() {
        // init default driver with the first from the driver list
        // or use Java JDBC-ODBC as default
        String[] history = DRIVER_ORDER.getHistory();
        if (history != null && history.length > 0) {
            m_driver = history[0];
        } else {
            m_driver = DatabaseDriverLoader.JDBC_ODBC_DRIVER;
        }
        // create database name from driver class
        m_dbName = DatabaseDriverLoader.getURLForDriver(m_driver);
    }
    
    /**
     * Creates a new <code>DBConnection</code> based on the given connection
     * object.
     * @param conn connection used to copy settings from
     */
    public DatabaseConnectionSettings(final DatabaseConnectionSettings conn) {
        this();
        m_driver = conn.m_driver;
        m_dbName = conn.m_dbName;
        m_user   = conn.m_user;
        m_pass   = conn.m_pass;
    }
    
    /**
     * Create a database connection based on this settings.
     * @return a new database connection object.
     * @throws SQLException {@link SQLException}
     * @throws InvalidSettingsException {@link InvalidSettingsException}
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws IOException {@link IOException}
     */
    public Connection createConnection() 
            throws InvalidSettingsException, SQLException,
            BadPaddingException, IllegalBlockSizeException,
            InvalidKeyException, IOException {
        if (m_dbName == null || m_user == null || m_pass == null
                || m_driver == null) {
            throw new InvalidSettingsException("No settings available "
                    + "to create database connection.");
        }
        Driver d = DatabaseDriverLoader.registerDriver(m_driver);
        if (!d.acceptsURL(m_dbName)) {
            throw new InvalidSettingsException("Driver \"" + d 
                    + "\" does not accept URL: " + m_dbName);
        }
        String password = KnimeEncryption.decrypt(m_pass);
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(m_dbName, m_user, password);
    }
    
    /**
     * Load settings.
     * @param settings connection settings
     */
    public void saveConnection(final ConfigWO settings) {
        settings.addString("driver", m_driver);
        settings.addString("database", m_dbName);
        settings.addString("user", m_user);
        settings.addString("password", m_pass);
        settings.addString("loaded_driver", 
                DatabaseDriverLoader.getDriverFileForDriverClass(m_driver));
        DRIVER_ORDER.add(m_driver);
        DRIVER_URLS.add(m_dbName);
    }

    /**
     * Validate settings.
     * @param settings to validate
     * @throws InvalidSettingsException if the settings are not valid
     */
    public void validateConnection(final ConfigRO settings)
            throws InvalidSettingsException {
        loadConnection(settings, false);
    }

    /**
     * Load validated settings.
     * @param settings to load
     * @return true, if settings have changed
     * @throws InvalidSettingsException if settings are invalid
     */
    public boolean loadValidatedConnection(final ConfigRO settings)
            throws InvalidSettingsException {
        return loadConnection(settings, true);
    }

    private boolean loadConnection(final ConfigRO settings, 
            final boolean write) throws InvalidSettingsException {
        if (settings == null) {
            throw new InvalidSettingsException(
                    "Connection settings not available!");
        }
        String driver = settings.getString("driver");
        String database = settings.getString("database");
        String user = settings.getString("user");
        // password
        String password = settings.getString("password", "");
        // loaded driver
        String[] loadedDriver = settings.getStringArray("loaded_driver", 
                new String[0]);
        // write settings or skip it
        if (write) {
            m_driver = driver;
            DRIVER_ORDER.add(m_driver);
            boolean changed = false;
            if (m_user != null && m_dbName != null && m_pass != null) { 
                if (!user.equals(m_user) || !database.equals(m_dbName)
                        || !password.equals(m_pass)) {
                    changed = true;
                }
            }
            m_dbName = database;
            DATABASE_URLS.add(m_dbName);
            DRIVER_URLS.add(m_dbName);
            m_user = user;
            m_pass = password;
            for (String fileName : loadedDriver) {
                try {
                    DatabaseDriverLoader.loadDriver(new File(fileName));
                } catch (Throwable t) {
                    LOGGER.info("Could not load driver from file \"" 
                            + fileName + "\".", t);
                }
            }
            return changed;
        }
        return false;
    }
    
    /**
     * Execute statement on current database connection.
     * @param statement to be executed
     * @throws SQLException {@link SQLException}
     * @throws InvalidSettingsException {@link InvalidSettingsException}
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws IOException {@link IOException}
     */
    public void execute(final String statement) throws InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException,
            InvalidSettingsException,
            SQLException, IOException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = createConnection();
            stmt = conn.createStatement();
            stmt.execute(statement);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }
    
    /**
     * Create connection model with all settings used to create a database
     * connection.
     * @return database connection model
     */
    public ModelContentRO createConnectionModel() {
        ModelContent cont = new ModelContent("database_connection_model");
        saveConnection(cont);
        return cont;
    } 
}

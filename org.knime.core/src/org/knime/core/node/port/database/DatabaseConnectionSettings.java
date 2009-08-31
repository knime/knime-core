/* 
 * ------------------------------------------------------------------
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
      
    /** Keeps the history of all database URLs. */
    public static final StringHistory DATABASE_URLS = StringHistory.getInstance(
            "database_urls");
    
    private static final ExecutorService CONNECTION_CREATOR_EXECUTOR = 
        Executors.newCachedThreadPool();
    
    /** DriverManager login timeout for database connection; not implemented/
     * used by all databases.
     */
    private static final int LOGIN_TIMEOUT = 5;

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
        
        final String password = KnimeEncryption.decrypt(m_pass);
        final String user = m_user;
        final String dbName = m_dbName;

        Callable<Connection> callable = new Callable<Connection>() {
            /** {@inheritDoc} */
            @Override
            public Connection call() throws Exception {
                LOGGER.debug("Opening database connection to \"" 
                        + dbName + "\"...");
                DriverManager.setLoginTimeout(LOGIN_TIMEOUT);
                return DriverManager.getConnection(dbName, user, password);
            }
        };
        Future<Connection> task = CONNECTION_CREATOR_EXECUTOR.submit(callable);
        try {
            LOGGER.debug("Setting database login timeout to " 
                    + LOGIN_TIMEOUT + "sec.");
            return task.get(LOGIN_TIMEOUT + 1, TimeUnit.SECONDS);
        } catch (ExecutionException ee) {
            throw new SQLException(ee.getCause());
        } catch (Exception ie) {
            throw new SQLException(ie);
        }
    }
    
    /**
     * Save settings.
     * @param settings connection settings
     */
    public void saveConnection(final ConfigWO settings) {
        settings.addString("driver", m_driver);
        settings.addString("database", m_dbName);
        settings.addString("user", m_user);
        settings.addString("password", m_pass);
        final File driverFile = 
            DatabaseDriverLoader.getDriverFileForDriverClass(m_driver);
        settings.addString("loaded_driver",
                (driverFile == null ? null : driverFile.getAbsolutePath()));
        DRIVER_ORDER.add(m_driver);
        DATABASE_URLS.add(m_dbName);
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
            m_user = user;
            m_pass = password;
            // loaded driver
            String loadedDriver = settings.getString("loaded_driver", null);
            if (loadedDriver != null) {
                try {
                    DatabaseDriverLoader.loadDriver(new File(loadedDriver));
                } catch (Throwable t) {
                    LOGGER.info("Could not load driver from file \"" 
                            + loadedDriver + "\".", t);
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
                stmt = null;
            }
            if (conn != null) {
                conn.close();
                conn = null;
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

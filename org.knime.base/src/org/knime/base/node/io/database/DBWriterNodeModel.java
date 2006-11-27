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
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.KnimeEncryption;

/**
 * Database writer model which creates a new table and adds the entire table to
 * it.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class DBWriterNodeModel extends NodeModel {

    /*
     * TODO not yet supported Double.MAX_VALUE, Double.NEGATIVE_INFINITY, and
     * DOUBLE.POSITIVE_INFINITY
     */

    /** Logger for the database writer. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DBWriterNodeModel.class);

    private String m_driver = DBDriverLoader.JDBC_ODBC_DRIVER;

    private String m_url = "jdbc:odbc:<database_name>";

    private String m_user = "<user>";

    private String m_pass = "";

    private String m_table = "<table_name>";

    private final Set<String> m_driverLoaded = new HashSet<String>();

    private final Map<String, String> m_types = 
        new LinkedHashMap<String, String>();

    /** Default SQL-type for Strings. */
    static final String SQL_TYPE_STRING = "varchar(255)";

    /** Default SQL-type for Integers. */
    static final String SQL_TYPE_INTEGER = "integer";

    /** Default SQL-type for Doubles. */
    static final String SQL_TYPE_DOUBLE = "numeric(30,10)";

    /** Config key for column to SQL-type mapping. */
    static final String CFG_SQL_TYPES = "sql_types";
    
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
    DBWriterNodeModel() {
        super(1, 0);
        reset();
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("driver", m_driver);
        settings.addString("database", m_url);
        settings.addString("user", m_user);
        settings.addString("password", m_pass);
        settings.addStringArray("loaded_driver", m_driverLoaded
                .toArray(new String[0]));
        settings.addString("table", m_table);
        // save sql type mapping
        NodeSettingsWO typeSett = settings.addNodeSettings(CFG_SQL_TYPES);
        for (Map.Entry<String, String> e : m_types.entrySet()) {
            typeSett.addString(e.getKey(), e.getValue());
        }
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
        String database = settings.getString("database");
        String user = settings.getString("user");
        String table = settings.getString("table");
        // password
        String password = settings.getString("password", "");
        // loaded driver
        String[] loadedDriver = settings.getStringArray("loaded_driver");
        // write settings or skip it
        if (write) {
            m_driver = driver;
            m_url = database;
            m_user = user;
            m_pass = password;
            m_table = table;
            m_driverLoaded.clear();
            m_driverLoaded.addAll(Arrays.asList(loadedDriver));
            for (String fileName : m_driverLoaded) {
                try {
                    DBDriverLoader.loadDriver(new File(fileName));
                } catch (Exception e) {
                    LOGGER.info("Could not load driver from: " + loadedDriver);
                }
            }
            // load SQL type for each column
            m_types.clear();
            try {
                NodeSettingsRO typeSett = settings
                        .getNodeSettings(CFG_SQL_TYPES);
                for (String key : typeSett.keySet()) {
                    m_types.put(key, typeSett.getString(key));
                }
            } catch (InvalidSettingsException ise) {
                // ignore, will be determined during configure
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
        exec.setProgress("Opening database connection...");
        Connection conn = null;
        try {
            DBDriverLoader.registerDriver(m_driver);
            // decryt password
            String password = KnimeEncryption.decrypt(m_pass);
            // create database connection
            conn = DriverManager.getConnection(m_url, m_user, password);
            // write entire data
            new DBWriterConnection(conn, m_table, inData[0], exec, m_types);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return new BufferedDataTable[0];
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
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
        // copy map to ensure only columns which are with the data
        Map<String, String> map = new LinkedHashMap<String, String>();
        // check that each column has a assigned type
        for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
            final String name = inSpecs[0].getColumnSpec(i).getName();
            String sqlType = m_types.get(name);
            if (sqlType == null) {
                final DataType type = inSpecs[0].getColumnSpec(i).getType();
                if (type.isCompatible(IntValue.class)) {
                    sqlType = DBWriterNodeModel.SQL_TYPE_INTEGER;
                } else if (type.isCompatible(DoubleValue.class)) {
                    sqlType = DBWriterNodeModel.SQL_TYPE_DOUBLE;
                } else {
                    sqlType = DBWriterNodeModel.SQL_TYPE_STRING;
                }
            }
            map.put(name, sqlType);
        }
        m_types.clear();
        m_types.putAll(map);
        
        try {
            WrappedDriver wDriver = DBDriverLoader.getWrappedDriver(m_driver);
            DriverManager.registerDriver(wDriver);
            if (!wDriver.acceptsURL(m_url)) {
                throw new InvalidSettingsException("Driver does not support"
                        + " url: " + m_url);
            }
        } catch (Exception e) {
            throw new InvalidSettingsException("Could not register database"
                    + " driver: " + m_driver);
        }
        
        // throw exception if no data provided
        if (inSpecs[0].getNumColumns() == 0) {
            throw new InvalidSettingsException("No columns in input data.");
        }
        
        // print warning if password is empty
        if (m_pass == null || m_pass.length() == 0) {
            super.setWarningMessage(
                    "Please check if you need to set a password.");
        }
        
        return new DataTableSpec[0];
    }
}

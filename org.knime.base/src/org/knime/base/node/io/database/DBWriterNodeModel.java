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
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseDriverLoader;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;

/**
 * Database writer model which creates a new table and adds the entire table to
 * it.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBWriterNodeModel extends NodeModel {

    /*
     * TODO not yet supported Double.MAX_VALUE, Double.NEGATIVE_INFINITY, and
     * DOUBLE.POSITIVE_INFINITY
     */

    /** Logger for the database writer. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DBWriterNodeModel.class);
    
    private final DatabaseConnectionSettings m_conn;

    private String m_table = null;
    
    private boolean m_append = false;

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
    
    /**
     * Creates a new model with one data input.
     */
    DBWriterNodeModel() {
        super(1, 0);
        m_conn = new DatabaseConnectionSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_conn.saveConnection(settings);
        settings.addString("table", m_table);
        settings.addBoolean("append_data", m_append);
        // save sql type mapping
        NodeSettingsWO typeSett = settings.addNodeSettings(CFG_SQL_TYPES);
        for (Map.Entry<String, String> e : m_types.entrySet()) {
            typeSett.addString(e.getKey(), e.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadSettings(settings, false);
        m_conn.validateConnection(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadSettings(settings, true);
        m_conn.loadValidatedConnection(settings);
    }

    private void loadSettings(
            final NodeSettingsRO settings, final boolean write) 
            throws InvalidSettingsException {
        boolean append = settings.getBoolean("append_data", false);
        String table = settings.getString("table");
        // write settings or skip it
        if (write) {
            if (table != null && table.contains(
                    DatabaseQueryConnectionSettings.TABLE_PLACEHOLDER)) {
                throw new InvalidSettingsException(
                    "Database table place holder not replaced.");
            }
            m_table = table;
            m_append = append;
            // loaded driver are needed to load settings before 1.2
            String[] loadedDriver = settings.getStringArray("loaded_driver",
                    new String[0]);
            for (String fileName : loadedDriver) {
                try {
                    DatabaseDriverLoader.loadDriver(new File(fileName));
                } catch (Throwable t) {
                    LOGGER.info("Could not load driver from file \"" 
                            + fileName + "\".", t);
                }
            }
            // load SQL type for each column
            m_types.clear();
            try {
                NodeSettingsRO typeSett = 
                    settings.getNodeSettings(CFG_SQL_TYPES);
                for (String key : typeSett.keySet()) {
                    m_types.put(key, typeSett.getString(key));
                }
            } catch (InvalidSettingsException ise) {
                // ignore, will be determined during configure
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        exec.setProgress("Opening database connection to write data...");
        // write entire data
        String error = DBWriterConnection.writeData(
                m_conn, m_table, inData[0], m_append, exec, m_types);
        if (error != null) {
            super.setWarningMessage(error);
        }
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
            final ExecutionMonitor exec) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_table == null || m_table.trim().isEmpty()) {
            throw new InvalidSettingsException(
                    "Configure node and enter a valid table name.");
        }
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
        
        // throw exception if no data provided
        if (inSpecs[0].getNumColumns() == 0) {
            throw new InvalidSettingsException("No columns in input data.");
        }
        
        if (!m_append && m_table != null) {
            super.setWarningMessage("Existing table \"" 
                    + m_table + "\" will be dropped!");
        }
        
        return new DataTableSpec[0];
    }
}

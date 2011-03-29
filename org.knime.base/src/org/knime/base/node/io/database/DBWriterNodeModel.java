/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseWriterConnection;

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

    /** Default SQL-type for Date. */
    static final String SQL_TYPE_DATEANDTIME = "datetime";

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
        m_conn.validateConnection(settings, getCredentialsProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadSettings(settings, true);
        m_conn.loadValidatedConnection(settings, getCredentialsProvider());
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
        String error = DatabaseWriterConnection.writeData(
                m_conn, m_table, inData[0], m_append, exec, m_types,
                getCredentialsProvider());
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
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        // empty
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
                } else if (type.isCompatible(DateAndTimeValue.class)) {
                    sqlType = DBWriterNodeModel.SQL_TYPE_DATEANDTIME;
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

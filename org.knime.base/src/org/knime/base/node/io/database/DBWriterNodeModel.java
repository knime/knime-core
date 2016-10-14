/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseConnectionPortObject;
import org.knime.core.node.port.database.DatabaseConnectionPortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.writer.DBWriter;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;

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

    private final DatabaseConnectionSettings m_conn = new DatabaseConnectionSettings();

    /** Config key for the table name. */
    static final String KEY_TABLE_NAME = "table";
    private String m_tableName;

    /** Config key for the batch size. */
    static final String KEY_BATCH_SIZE = "batch_size";
    private int m_batchSize = DatabaseConnectionSettings.BATCH_WRITE_SIZE;

    /** Config key for the append data. */
    static final String KEY_APPEND_DATA = "append_data";
    private boolean m_append = true;

    /** Config key for the insert null for missing columns. */
    static final String KEY_INSERT_NULL_FOR_MISSING_COLS = "insert_null_for_missing_cols";
    private boolean m_insertNullForMissingCols = false;

    private final Map<String, String> m_types =
        new LinkedHashMap<String, String>();

    /** Default SQL-type for Strings. */
    static final String SQL_TYPE_STRING = "varchar(255)";

    /** Default SQL-type for Booleans. */
    static final String SQL_TYPE_BOOLEAN = "boolean";

    /** Default SQL-type for Integers. */
    static final String SQL_TYPE_INTEGER = "integer";

    /** Default SQL-type for Doubles. */
    static final String SQL_TYPE_DOUBLE = "numeric(30,10)";

    /** Default SQL-type for Timestamps. */
    static final String SQL_TYPE_DATEANDTIME = "timestamp";

    /** Default SQL-type for Date. */
    static final String SQL_TYPE_BLOB = "blob";

    /** Config key for column to SQL-type mapping. */
    static final String CFG_SQL_TYPES = "sql_types";

    /* error message during streaming execution */
    private String m_errorMessage = null;

    /**
     * Creates a new model with one data input.
     */
    DBWriterNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE, DatabaseConnectionPortObject.TYPE_OPTIONAL}, new PortType[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_conn.saveConnection(settings);
        settings.addString(KEY_TABLE_NAME, m_tableName);
        settings.addBoolean(KEY_APPEND_DATA, m_append);
        settings.addBoolean(KEY_INSERT_NULL_FOR_MISSING_COLS, m_insertNullForMissingCols);
        // save SQL Types mapping
        NodeSettingsWO typeSett = settings.addNodeSettings(CFG_SQL_TYPES);
        for (Map.Entry<String, String> e : m_types.entrySet()) {
            typeSett.addString(e.getKey(), e.getValue());
        }
        // save batch size
        settings.addInt(KEY_BATCH_SIZE, m_batchSize);
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
        boolean append = settings.getBoolean(KEY_APPEND_DATA, true);
        final String table = settings.getString(KEY_TABLE_NAME);
        if (table == null || table.trim().isEmpty()) {
            throw new InvalidSettingsException(
                "Configure node and enter a valid table name.");
        }
        // read and validate batch size
        final int batchSize = settings.getInt(KEY_BATCH_SIZE, m_batchSize);
        if (batchSize <= 0) {
            throw new InvalidSettingsException("Batch size must be greater than 0, is " + batchSize);
        }
        // write settings or skip it
        if (write) {
            m_tableName = table;
            m_append = append;
            // load SQL Types for each column
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
            // load batch size
            m_batchSize = batchSize;
        }
        //introduced in KNIME 2.11 default behavior before was inserting null
        m_insertNullForMissingCols = settings.getBoolean(KEY_INSERT_NULL_FOR_MISSING_COLS, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        exec.setProgress("Opening database connection to write data...");

        DatabaseConnectionSettings connSettings;
        if ((inData.length > 1) && (inData[1] instanceof DatabaseConnectionPortObject)) {
            connSettings = ((DatabaseConnectionPortObject) inData[1]).getConnectionSettings(getCredentialsProvider());
        } else {
            connSettings = m_conn;
        }

        DBWriter writer = connSettings.getUtility().getWriter(connSettings);
        BufferedDataTable inputTable = (BufferedDataTable)inData[0];
        DataTableRowInput rowInput = new DataTableRowInput(inputTable);
        // write entire data
        final String error = writer.writeData(m_tableName, rowInput, inputTable.size(),
            m_append, exec, m_types, getCredentialsProvider(), m_batchSize, m_insertNullForMissingCols);
        // set error message generated during writing rows
        if (error != null) {
            super.setWarningMessage(error);
        }
        return new BufferedDataTable[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_STREAMABLE, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                exec.setProgress("Opening database connection to write data...");

                DatabaseConnectionSettings connSettings;
                PortObject portObj = ((PortObjectInput)inputs[1]).getPortObject();
                if (portObj != null && (portObj instanceof DatabaseConnectionPortObject)) {
                    connSettings =
                        ((DatabaseConnectionPortObject)portObj).getConnectionSettings(getCredentialsProvider());
                } else {
                    connSettings = m_conn;
                }
                DBWriter writer = connSettings.getUtility().getWriter(connSettings);
                // write entire data
                m_errorMessage =
                    writer.writeData(m_tableName, (RowInput) inputs[0], -1,
                        m_append, exec, m_types, getCredentialsProvider(), m_batchSize, m_insertNullForMissingCols);
            }

        };
    }

    /**
     * {@inheritDoc}
     *
     * NB: needs to be overwritten to enforce the
     * {@link DBWriterNodeModel#finishStreamableExecution(StreamableOperatorInternals, ExecutionContext, PortOutput[])}
     * to be called in order to set an error message.
     */
    @Override
    public MergeOperator createMergeOperator() {
        return new MergeOperator() {

            @Override
            public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
                 return operators[0];
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        if (m_errorMessage != null) {
            setWarningMessage(m_errorMessage);
        }
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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec tableSpec = (DataTableSpec)inSpecs[0];
        // check optional incoming connection
        if ((inSpecs.length > 1) && (inSpecs[1] instanceof DatabaseConnectionPortObjectSpec)) {
            DatabaseConnectionSettings connSettings =
                    ((DatabaseConnectionPortObjectSpec)inSpecs[1]).getConnectionSettings(getCredentialsProvider());

            if ((connSettings.getJDBCUrl() == null) || connSettings.getJDBCUrl().isEmpty()
                    || (connSettings.getDriver() == null) || connSettings.getDriver().isEmpty()) {
                throw new InvalidSettingsException("No valid database connection provided via second input port");
            }
            if (!connSettings.getUtility().supportsInsert()) {
                throw new InvalidSettingsException("Connected database does not support insert operations");
            }
        } else {
            if (!m_conn.getUtility().supportsInsert()) {
                throw new InvalidSettingsException("Selected database does not support insert operations");
            }
        }

        // check table name
        if ((m_tableName == null) || m_tableName.trim().isEmpty()) {
            throw new InvalidSettingsException(
                "Configure node and enter a valid table name.");
        }

        // throw exception if no data provided
        if (tableSpec.getNumColumns() == 0) {
            throw new InvalidSettingsException("No columns in input data.");
        }



        // copy map to ensure only columns which are with the data
        Map<String, String> map = new LinkedHashMap<String, String>();
        // check that each column has a assigned type
        for (int i = 0; i < tableSpec.getNumColumns(); i++) {
            final String name = tableSpec.getColumnSpec(i).getName();
            String sqlType = m_types.get(name);
            if (sqlType == null) {
                final DataType type = tableSpec.getColumnSpec(i).getType();
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


        if (!m_append) {
            super.setWarningMessage("Existing table \"" + m_tableName + "\" will be dropped!");
        }

        return new DataTableSpec[0];
    }
}

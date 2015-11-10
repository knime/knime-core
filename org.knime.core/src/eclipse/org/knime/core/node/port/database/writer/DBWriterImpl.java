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
 * ----------------------------------------------------------------------------
 */
package org.knime.core.node.port.database.writer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ConnectionPendingException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.TimeZone;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Creates a connection to write to database.
 *
 * <p>No public API.</p>
 *
 * @author Thomas Gabriel, University of Konstanz
 * @since 3.1
 */
public class DBWriterImpl implements DBWriter {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DBWriterImpl.class);
    private DatabaseConnectionSettings m_conn;

    /**
     * @param conn {@link DatabaseConnectionSettings}
     */
    public DBWriterImpl(final DatabaseConnectionSettings conn) {
        if (conn == null) {
            throw new NullPointerException("conn must not be null");
        }
        m_conn = conn;
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    @Deprecated
    @Override
    public String writeData(final String table, final RowInput input, final long rowCount, final boolean appendData,
        final ExecutionMonitor exec, final Map<String, String> sqlTypes, final CredentialsProvider cp,
        final int batchSize, final boolean insertNullForMissingCols) throws Exception {
        final Connection conn = m_conn.createConnection(cp);
        exec.setMessage("Waiting for free database connection...");
        final StringBuilder columnNamesForInsertStatement = new StringBuilder("(");
        synchronized (m_conn.syncConnection(conn)) {
            exec.setMessage("Start writing rows in database...");
            DataTableSpec spec = input.getDataTableSpec();
            // mapping from spec columns to database columns
            final int[] mapping;
            // append data to existing table
            if (appendData) {
                if (m_conn.getUtility().tableExists(conn, table)) {
                    String query =
                        m_conn.getUtility().getStatementManipulator().forMetadataOnly("SELECT * FROM " + table);
                    try (ResultSet rs = conn.createStatement().executeQuery(query)) {
                        ResultSetMetaData rsmd = rs.getMetaData();
                        final Map<String, Integer> columnNames =
                                new LinkedHashMap<String, Integer>();
                        for (int i = 0; i < spec.getNumColumns(); i++) {
                            String colName = replaceColumnName(spec.getColumnSpec(i).getName());
                            columnNames.put(colName.toLowerCase(), i);
                        }

                        // sanity check to lock if all input columns are in db
                        ArrayList<String> columnNotInSpec = new ArrayList<String>(
                                columnNames.keySet());
                        for (int i = 0; i < rsmd.getColumnCount(); i++) {
                            String dbColName = replaceColumnName(rsmd.getColumnName(i + 1));
                            if (columnNames.containsKey(dbColName.toLowerCase())) {
                                columnNotInSpec.remove(dbColName.toLowerCase());
                                columnNamesForInsertStatement.append(dbColName).append(',');
                            } else if (insertNullForMissingCols) {
                                //append the column name of a missing column only if the insert null for missing
                                //column option is enabled
                                columnNamesForInsertStatement.append(dbColName).append(',');
                            }
                        }
                        if (rsmd.getColumnCount() > 0) {
                            columnNamesForInsertStatement.deleteCharAt(columnNamesForInsertStatement.length() - 1);
                        }
                        columnNamesForInsertStatement.append(')');

                        if (columnNotInSpec.size() > 0) {
                            throw new RuntimeException("No. of columns in input"
                                    + " table > in database; not existing columns: "
                                    + columnNotInSpec.toString());
                        }
                        mapping = new int[rsmd.getColumnCount()];
                        for (int i = 0; i < mapping.length; i++) {
                            String name = replaceColumnName(rsmd.getColumnName(i + 1)).toLowerCase();
                            if (!columnNames.containsKey(name)) {
                                mapping[i] = -1;
                                continue;
                            }
                            mapping[i] = columnNames.get(name);
                            DataColumnSpec cspec = spec.getColumnSpec(mapping[i]);
                            int type = rsmd.getColumnType(i + 1);
                            switch (type) {
                                // check all boolean compatible types
                                case Types.BIT:
                                case Types.BOOLEAN:
                                    // types must be compatible to BooleanValue
                                    if (!cspec.getType().isCompatible(BooleanValue.class)) {
                                        throw new RuntimeException("Column \"" + name
                                            + "\" of type \"" + cspec.getType()
                                            + "\" from input does not match type "
                                            + "\"" + rsmd.getColumnTypeName(i + 1)
                                            + "\" in database at position " + i);
                                    }
                                    break;
                                    // check all int compatible types
                                case Types.TINYINT:
                                case Types.SMALLINT:
                                case Types.INTEGER:
                                    // types must be compatible to IntValue
                                    if (!cspec.getType().isCompatible(IntValue.class)) {
                                        throw new RuntimeException("Column \"" + name
                                            + "\" of type \"" + cspec.getType()
                                            + "\" from input does not match type "
                                            + "\"" + rsmd.getColumnTypeName(i + 1)
                                            + "\" in database at position " + i);
                                    }
                                    break;
                                case Types.BIGINT:
                                    // types must also be compatible to LongValue
                                    if (!cspec.getType().isCompatible(LongValue.class)) {
                                        throw new RuntimeException("Column \"" + name
                                            + "\" of type \"" + cspec.getType()
                                            + "\" from input does not match type "
                                            + "\"" + rsmd.getColumnTypeName(i + 1)
                                            + "\" in database at position " + i);
                                    }
                                    break;
                                    // check all double compatible types
                                case Types.FLOAT:
                                case Types.DOUBLE:
                                case Types.NUMERIC:
                                case Types.DECIMAL:
                                case Types.REAL:
                                    // types must also be compatible to DoubleValue
                                    if (!cspec.getType().isCompatible(DoubleValue.class)) {
                                        throw new RuntimeException("Column \"" + name
                                            + "\" of type \"" + cspec.getType()
                                            + "\" from input does not match type "
                                            + "\"" + rsmd.getColumnTypeName(i + 1)
                                            + "\" in database at position " + i);
                                    }
                                    break;
                                    // check for date-and-time compatible types
                                case Types.DATE:
                                case Types.TIME:
                                case Types.TIMESTAMP:
                                    // types must also be compatible to DataValue
                                    if (!cspec.getType().isCompatible(DateAndTimeValue.class)) {
                                        throw new RuntimeException("Column \"" + name
                                            + "\" of type \"" + cspec.getType()
                                            + "\" from input does not match type "
                                            + "\"" + rsmd.getColumnTypeName(i + 1)
                                            + "\" in database at position " + i);
                                    }
                                    break;
                                    // check for blob compatible types
                                case Types.BLOB:
                                case Types.BINARY:
                                case Types.LONGVARBINARY:
                                    // types must also be compatible to DataValue
                                    if (!cspec.getType().isCompatible(BinaryObjectDataValue.class)) {
                                        throw new RuntimeException("Column \"" + name
                                            + "\" of type \"" + cspec.getType()
                                            + "\" from input does not match type "
                                            + "\"" + rsmd.getColumnTypeName(i + 1)
                                            + "\" in database at position " + i);
                                    }
                                    break;
                                    // all other cases are defined as StringValue types
                            }
                        }
                    }
                } else {
                    LOGGER.info("Table \"" + table
                        + "\" does not exist in database, "
                        + "will create new table.");
                    // and create new table
                    final String query =
                            "CREATE TABLE " + table + " "
                                    + createTableStmt(spec, sqlTypes, columnNamesForInsertStatement);
                    LOGGER.debug("Executing SQL statement as execute: " + query);
                    Statement statement = conn.createStatement();
                    statement.execute(query);
                    mapping = new int[spec.getNumColumns()];
                    for (int k = 0; k < mapping.length; k++) {
                        mapping[k] = k;
                    }
                }
            } else {
                LOGGER.debug("Append not enabled. Table " + table + " will be dropped if exists.");
                mapping = new int[spec.getNumColumns()];
                for (int k = 0; k < mapping.length; k++) {
                    mapping[k] = k;
                }
                Statement statement = null;
                try {
                    statement = conn.createStatement();
                    // remove existing table (if any)
                    final String query = "DROP TABLE " + table;
                    LOGGER.debug("Executing SQL statement as execute: " + query);
                    statement.execute(query);
                } catch (Throwable t) {
                    if (statement == null) {
                        throw new SQLException("Could not create SQL statement,"
                            + " reason: " + t.getMessage(), t);
                    }
                    LOGGER.info("Exception droping table \"" + table + "\": " + t.getMessage()
                        + ". Will create new table.");
                } finally {
                    if (!conn.getAutoCommit()) {
                        conn.commit();
                    }
                }
                // and create new table
                final String query =
                    "CREATE TABLE " + table + " " + createTableStmt(spec, sqlTypes, columnNamesForInsertStatement);
                LOGGER.debug("Executing SQL statement as execute: " + query);
                statement.execute(query);
                statement.close();
            }

            // this is a (temporary) workaround for bug #5802: if there is a DataValue column in the input table
            // we need to use the SQL type for creating the insert statements.
            Map<Integer, Integer> columnTypes = null;
            for (DataColumnSpec cs : spec) {
                if (cs.getType().getPreferredValueClass() == DataValue.class) {
                    columnTypes = getColumnTypes(conn, table);
                    break;
                }
            }

            final String insertStamtement =
                    createInsertStatment(table, columnNamesForInsertStatement.toString(), mapping, insertNullForMissingCols);

            // problems writing more than 13 columns. the prepare statement
            // ensures that we can set the columns directly row-by-row, the
            // database will handle the commit
            long cnt = 1;
            long errorCnt = 0;
            long allErrors = 0;

            // count number of rows added to current batch
            int curBatchSize = 0;

            LOGGER.debug("Executing SQL statement as prepareStatement: " + insertStamtement);
            final PreparedStatement stmt = conn.prepareStatement(insertStamtement);
            // remember auto-commit flag
            final boolean autoCommit = conn.getAutoCommit();
            DatabaseConnectionSettings.setAutoCommit(conn, false);
            try {
                final TimeZone timezone = m_conn.getTimeZone();
                DataRow row; //get the first row
                DataRow nextRow = input.poll();
                //iterate over all incoming data rows
                while (nextRow != null) {
                    row = nextRow;
                    cnt++;
                    exec.checkCanceled();
                        if (rowCount > 0) {
                            exec.setProgress(1.0 * cnt / rowCount, "Row " + "#" + cnt);
                        } else {
                            exec.setProgress("Writing Row#" + cnt);
                        }

                    int dbIdx = 1;
                    for (int i = 0; i < mapping.length; i++) {
                        if (mapping[i] < 0) {
                            if (insertNullForMissingCols) {
                                //insert only null if the insert null for missing col option is enabled
                                stmt.setNull(dbIdx++, Types.NULL);
                            }
                        } else {
                            final DataColumnSpec cspec = spec.getColumnSpec(mapping[i]);
                            final DataCell cell = row.getCell(mapping[i]);
                            fillStatement(stmt, dbIdx++, cspec, cell, timezone, columnTypes);
                        }
                    }
                    // if batch mode
                    if (batchSize > 1) {
                        // a new row will be added
                        stmt.addBatch();
                    }

                    //get one more input row to check if 'row' is the last one
                    nextRow = input.poll();

                    curBatchSize++;
                    // if batch size equals number of row in batch or input table at end
                        if ((curBatchSize == batchSize) || nextRow == null) {
                            curBatchSize = 0;
                        try {
                            // write batch
                            if (batchSize > 1) {
                                stmt.executeBatch();
                            } else { // or write single row
                                stmt.execute();
                            }
                        } catch (Throwable t) {
                            // Postgres will refuse any more commands in this transaction after errors
                            // Therefore we commit the changes that were possible. We commit everything at the end
                            // anyway.
                            if (!conn.getAutoCommit()) {
                                conn.commit();
                            }

                            allErrors++;
                            if (errorCnt > -1) {
                                final String errorMsg;
                                if (batchSize > 1) {
                                    errorMsg = "Error while adding rows #" + (cnt - batchSize) + " - #" + cnt
                                        + ", reason: " + t.getMessage();
                                } else {
                                    errorMsg = "Error while adding row #" + cnt + " (" + row.getKey() + "), reason: "
                                        + t.getMessage();
                                }
                                exec.setMessage(errorMsg);
                                if (errorCnt++ < 10) {
                                    LOGGER.warn(errorMsg);
                                } else {
                                    errorCnt = -1;
                                    LOGGER.warn(errorMsg + " - more errors...", t);
                                }
                            }
                        } finally {
                            // clear batch if in batch mode
                            if (batchSize > 1) {
                                stmt.clearBatch();
                            }
                        }
                        }
                    }
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
                if (allErrors == 0) {
                        return null;
                    } else {
                        return "Errors \"" + allErrors + "\" writing " + (cnt - 1) + " rows.";
                    }
            } finally {
                DatabaseConnectionSettings.setAutoCommit(conn, autoCommit);
                stmt.close();
            }
        }
    }

    /**
     * @param table
     * @param columnNames
     * @param mapping
     * @param insertNullForMissingCols
     * @return the insert statement
     */
    protected String createInsertStatment(final String table, final String columnNames, final int[] mapping,
        final boolean insertNullForMissingCols) {
        // // creates the wild card string based on the number of columns
        // this string it used every time an new row is inserted into the db
        final StringBuilder wildcard = new StringBuilder("(");
        boolean first = true;
        for (int i = 0; i < mapping.length; i++) {
            if (mapping[i] >= 0 || insertNullForMissingCols) {
                    //insert only a ? if the column is available in the input table or the insert null for missing
                    //columns option is enabled
                if (first) {
                    first = false;
                } else {
                    wildcard.append(", ");
                }
                wildcard.append("?");
            }
        }
        wildcard.append(")");
        // create table meta data with empty column information
        final String query = "INSERT INTO " + table + " " + columnNames + " VALUES " + wildcard;
        return query;
    }

    /**
     * @param conn {@link ConnectionPendingException}
     * @param table table name
     * @return the column type mapping
     * @throws SQLException
     */
    protected Map<Integer, Integer> getColumnTypes(final Connection conn, final String table) throws SQLException {
        // TODO move this block to DatabaseUtility
        Map<Integer, Integer> columnTypes = new HashMap<>();
        try (ResultSet metaDataRs = conn.getMetaData().getColumns(conn.getCatalog(), null, table, null)) {
            while (metaDataRs.next()) {
                columnTypes.put(metaDataRs.getInt("ORDINAL_POSITION"), metaDataRs.getInt("DATA_TYPE"));
            }
        }

        if (columnTypes.isEmpty()) {
            // e.g. PostgreSQL converts all table names to lower case by default
            try (ResultSet metaDataRs =
                conn.getMetaData().getColumns(conn.getCatalog(), null, table.toLowerCase(), null)) {
                while (metaDataRs.next()) {
                    columnTypes.put(metaDataRs.getInt("ORDINAL_POSITION"), metaDataRs.getInt("DATA_TYPE"));
                }
            }
        }
        if (columnTypes.isEmpty()) {
            // e.g. Oracle converts all table names to upper case by default
            try (ResultSet metaDataRs =
                conn.getMetaData().getColumns(conn.getCatalog(), null, table.toUpperCase(), null)) {
                while (metaDataRs.next()) {
                    columnTypes.put(metaDataRs.getInt("ORDINAL_POSITION"), metaDataRs.getInt("DATA_TYPE"));
                }
            }
        }
        return columnTypes;
    }

    /** Create connection to update table in database.
     * @param data The data to write.
     * @param setColumns columns part of the SET clause
     * @param whereColumns columns part of the WHERE clause
     * @param updateStatus int array of length data#getRowCount; will be filled with
     *             update info from the database
     * @param table name of table to write
     * @param exec Used the cancel writing.
     * @param cp {@link CredentialsProvider} providing user/password
     * @param batchSize number of rows updated in one batch
     * @return error string or null, if non
     * @throws Exception if connection could not be established
     */
    @Override
    public final String updateTable(final String schema,
            final String table, final BufferedDataTable data,
            final String[] setColumns, final String[] whereColumns,
            final int[] updateStatus,
            final ExecutionMonitor exec,
            final CredentialsProvider cp,
            final int batchSize) throws Exception {
        final Connection conn = m_conn.createConnection(cp);
        exec.setMessage("Waiting for free database connection...");
        synchronized (m_conn.syncConnection(conn)) {
            exec.setMessage("Start updating rows in database...");
            final DataTableSpec spec = data.getDataTableSpec();
            final String updateStmt = createUpdateStatement(table, setColumns, whereColumns);
            // problems writing more than 13 columns. the prepare statement
            // ensures that we can set the columns directly row-by-row, the
            // database will handle the commit
            long rowCount = data.size();
            int cnt = 1;
            int errorCnt = 0;
            int allErrors = 0;

            // count number of rows added to current batch
            int curBatchSize = 0;

            // selected timezone
            final TimeZone timezone = m_conn.getTimeZone();

            LOGGER.debug("Executing SQL statement as prepareStatement: " + updateStmt);
            final PreparedStatement stmt = conn.prepareStatement(updateStmt);
            // remember auto-commit flag
            final boolean autoCommit = conn.getAutoCommit();
            DatabaseConnectionSettings.setAutoCommit(conn, false);
            try {
                for (RowIterator it = data.iterator(); it.hasNext(); cnt++) {
                    exec.checkCanceled();
                    exec.setProgress(1.0 * cnt / rowCount, "Row " + "#" + cnt);
                    final DataRow row = it.next();
                    // SET columns
                    for (int i = 0; i < setColumns.length; i++) {
                        final int dbIdx = i + 1;
                        final int columnIndex = spec.findColumnIndex(setColumns[i]);
                        final DataColumnSpec cspec = spec.getColumnSpec(columnIndex);
                        final DataCell cell = row.getCell(columnIndex);
                        fillStatement(stmt, dbIdx, cspec, cell, timezone, null);
                    }
                    // WHERE columns
                    for (int i = 0; i < whereColumns.length; i++) {
                        final int dbIdx = i + 1 + setColumns.length;
                        final int columnIndex = spec.findColumnIndex(whereColumns[i]);
                        final DataColumnSpec cspec = spec.getColumnSpec(columnIndex);
                        final DataCell cell = row.getCell(columnIndex);
                        fillStatement(stmt, dbIdx, cspec, cell, timezone, null);
                    }

                    // if batch mode
                    if (batchSize > 1) {
                        // a new row will be added
                        stmt.addBatch();
                    }
                    curBatchSize++;
                    // if batch size equals number of row in batch or input table at end
                    if ((curBatchSize == batchSize) || !it.hasNext()) {
                        curBatchSize = 0;
                        try {
                            // write batch
                            if (batchSize > 1) {
                                int[] status = stmt.executeBatch();
                                for (int i = 0; i < status.length; i++) {
                                    updateStatus[cnt - status.length + i] = status[i];
                                }
                            } else { // or write single row
                                int status = stmt.executeUpdate();
                                updateStatus[cnt - 1] = status;
                            }
                        } catch (Throwable t) {
                            // Postgres will refuse any more commands in this transaction after errors
                            // Therefore we commit the changes that were possible. We commit everything at the end
                            // anyway.
                            if (!conn.getAutoCommit()) {
                                conn.commit();
                            }

                            allErrors++;
                            if (errorCnt > -1) {
                                final String errorMsg;
                                if (batchSize > 1) {
                                    errorMsg = "Error while updating rows #" + (cnt - batchSize) + " - #" + cnt
                                        + ", reason: " + t.getMessage();
                                } else {
                                    errorMsg = "Error while updating row #" + cnt + " (" + row.getKey() + "), reason: "
                                        + t.getMessage();
                                }
                                exec.setMessage(errorMsg);
                                if (errorCnt++ < 10) {
                                    LOGGER.warn(errorMsg);
                                } else {
                                    errorCnt = -1;
                                    LOGGER.warn(errorMsg + " - more errors...", t);
                                }
                            }
                        } finally {
                            // clear batch if in batch mode
                            if (batchSize > 1) {
                                stmt.clearBatch();
                            }
                        }
                    }
                }
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
                if (allErrors == 0) {
                    return null;
                } else {
                    return "Errors \"" + allErrors + "\" updating " + rowCount + " rows.";
                }
            } finally {
                DatabaseConnectionSettings.setAutoCommit(conn, autoCommit);
                stmt.close();
            }
        }
    }

    /**
     * @param whereColumns
     * @param setColumns
     * @param table
     * @return update statment
     */
    protected String createUpdateStatement(final String table, final String[] setColumns, final String[] whereColumns) {
     // create query connection object
        final StringBuilder query = new StringBuilder("UPDATE " + table + " SET");
        for (int i = 0; i < setColumns.length; i++) {
            if (i > 0) {
                query.append(",");
            }
            final String newColumnName = replaceColumnName(setColumns[i]);
            query.append(" " + newColumnName + " = ?");
        }
        query.append(" WHERE");
        for (int i = 0; i < whereColumns.length; i++) {
            if (i > 0) {
                query.append(" AND");
            }
            final String newColumnName = replaceColumnName(whereColumns[i]);
            query.append(" " + newColumnName + " = ?");
        }
        return query.toString();
    }

    /** Create connection to update table in database.
     * @param schema table schema name. Could be <code>null</code>.
     * @param data The data to write.
     * @param whereColumns columns part of the WHERE clause
     * @param deleteStatus int array of length data#getRowCount; will be filled with
     *             the number of rows effected
     * @param table name of table to write
     * @param exec Used the cancel writing.
     * @param cp {@link CredentialsProvider} providing user/password
     * @param batchSize number of rows deleted in one batch
     * @return error string or null, if non
     * @throws Exception if connection could not be established
     */
    @Override
    public final String deleteRows(final String schema,
            final String table, final BufferedDataTable data,
            final String[] whereColumns,
            final int[] deleteStatus,
            final ExecutionMonitor exec,
            final CredentialsProvider cp,
            final int batchSize) throws Exception {
        final Connection conn = m_conn.createConnection(cp);
        exec.setMessage("Waiting for free database connection...");
        synchronized (m_conn.syncConnection(conn)) {
            exec.setMessage("Start deleting rows from database...");
            final DataTableSpec spec = data.getDataTableSpec();

            // create query connection object
            final StringBuilder query = new StringBuilder("DELETE FROM " + table + " WHERE");
            for (int i = 0; i < whereColumns.length; i++) {
                if (i > 0) {
                    query.append(" AND");
                }
                final String newColumnName = replaceColumnName(whereColumns[i]);
                query.append(" " + newColumnName + " = ?");
            }


            // problems writing more than 13 columns. the prepare statement
            // ensures that we can set the columns directly row-by-row, the
            // database will handle the commit
            long rowCount = data.size();
            int cnt = 1;
            int errorCnt = 0;
            int allErrors = 0;

            // count number of rows added to current batch
            int curBatchSize = 0;

            // selected timezone
            final TimeZone timezone = m_conn.getTimeZone();

            LOGGER.debug("Executing SQL statement as prepareStatement: " + query);
            final PreparedStatement stmt = conn.prepareStatement(query.toString());
            // remember auto-commit flag
            final boolean autoCommit = conn.getAutoCommit();
            DatabaseConnectionSettings.setAutoCommit(conn, false);
            try {
                for (RowIterator it = data.iterator(); it.hasNext(); cnt++) {
                    exec.checkCanceled();
                    exec.setProgress(1.0 * cnt / rowCount, "Row " + "#" + cnt);
                    final DataRow row = it.next();
                    // WHERE columns
                    for (int i = 0; i < whereColumns.length; i++) {
                        final int dbIdx = i + 1;
                        final int columnIndex = spec.findColumnIndex(whereColumns[i]);
                        final DataColumnSpec cspec = spec.getColumnSpec(columnIndex);
                        final DataCell cell = row.getCell(columnIndex);
                        fillStatement(stmt, dbIdx, cspec, cell, timezone, null);
                    }

                    // if batch mode
                    if (batchSize > 1) {
                        // a new row will be added
                        stmt.addBatch();
                    }
                    curBatchSize++;
                    // if batch size equals number of row in batch or input table at end
                    if ((curBatchSize == batchSize) || !it.hasNext()) {
                        curBatchSize = 0;
                        try {
                            // write batch
                            if (batchSize > 1) {
                                int[] status = stmt.executeBatch();
                                for (int i = 0; i < status.length; i++) {
                                    deleteStatus[cnt - status.length + i] = status[i];
                                }
                            } else { // or write single row
                                int status = stmt.executeUpdate();
                                deleteStatus[cnt - 1] = status;
                            }
                        } catch (Throwable t) {
                            // Postgres will refuse any more commands in this transaction after errors
                            // Therefore we commit the changes that were possible. We commit everything at the end
                            // anyway.
                            if (!conn.getAutoCommit()) {
                                conn.commit();
                            }
                            allErrors++;
                            if (errorCnt > -1) {
                                final String errorMsg;
                                if (batchSize > 1) {
                                    errorMsg = "Error while deleting rows #" + (cnt - batchSize) + " - #" + cnt
                                        + ", reason: " + t.getMessage();
                                } else {
                                    errorMsg = "Error while deleting row #" + cnt + " (" + row.getKey() + "), reason: "
                                        + t.getMessage();
                                }
                                exec.setMessage(errorMsg);
                                if (errorCnt++ < 10) {
                                    LOGGER.warn(errorMsg);
                                } else {
                                    errorCnt = -1;
                                    LOGGER.warn(errorMsg + " - more errors...", t);
                                }
                            }
                        } finally {
                            // clear batch if in batch mode
                            if (batchSize > 1) {
                                stmt.clearBatch();
                            }
                        }
                    }
                }
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
                if (allErrors == 0) {
                    return null;
                } else {
                    return "Errors \"" + allErrors + "\" deleting " + rowCount + " rows.";
                }
            } finally {
                DatabaseConnectionSettings.setAutoCommit(conn, autoCommit);
                stmt.close();
            }
        }
    }

    /**
     * Set given column value into SQL statement.
     * @param stmt statement used
     * @param dbIdx database index to update/write
     * @param cspec column spec to check type
     * @param cell the data cell to write into the statement
     * @param tz the {@link TimeZone} to use
     * @param columnTypes
     * @throws SQLException if the value can't be set
     */
    protected void fillStatement(final PreparedStatement stmt, final int dbIdx, final DataColumnSpec cspec,
        final DataCell cell, final TimeZone tz, final Map<Integer, Integer> columnTypes)
            throws SQLException {
        if (cspec.getType().isCompatible(BooleanValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.BOOLEAN);
            } else {
                boolean bool = ((BooleanValue) cell).getBooleanValue();
                stmt.setBoolean(dbIdx, bool);
            }
        } else if (cspec.getType().isCompatible(IntValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.INTEGER);
            } else {
                int integer = ((IntValue) cell).getIntValue();
                stmt.setInt(dbIdx, integer);
            }
        } else if (cspec.getType().isCompatible(LongValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.BIGINT);
            } else {
                long dbl = ((LongValue) cell).getLongValue();
                stmt.setLong(dbIdx, dbl);
            }
        } else if (cspec.getType().isCompatible(DoubleValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.DOUBLE);
            } else {
                double dbl = ((DoubleValue) cell).getDoubleValue();
                if (Double.isNaN(dbl)) {
                    stmt.setNull(dbIdx, Types.DOUBLE);
                } else {
                    stmt.setDouble(dbIdx, dbl);
                }
            }
        } else if (cspec.getType().isCompatible(DateAndTimeValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.DATE);
            } else {
                final DateAndTimeValue dateCell = (DateAndTimeValue) cell;
                final long corrDate = dateCell.getUTCTimeInMillis() - tz.getOffset(dateCell.getUTCTimeInMillis());
                if (!dateCell.hasTime() && !dateCell.hasMillis()) {
                    java.sql.Date date = new java.sql.Date(corrDate);
                    stmt.setDate(dbIdx, date);
                } else if (!dateCell.hasMillis()) {
                    java.sql.Time time = new java.sql.Time(corrDate);
                    stmt.setTime(dbIdx, time);
                } else {
                    java.sql.Timestamp timestamp = new java.sql.Timestamp(corrDate);
                    stmt.setTimestamp(dbIdx, timestamp);
                }
            }
        } else if (cspec.getType().isCompatible(BinaryObjectDataValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.BLOB);
            } else {
                try {
                    BinaryObjectDataValue value = (BinaryObjectDataValue) cell;
                    InputStream is = value.openInputStream();
                    if (is == null) {
                        stmt.setNull(dbIdx, Types.BLOB);
                    } else {
                        try {
                            // to be compatible with JDBC 3.0, the length of the stream is restricted to max integer,
                            // which are ~2GB; with JDBC 4.0 longs are supported and the respective method can be called
                            stmt.setBinaryStream(dbIdx, is, (int) value.length());
                        } catch (SQLException ex) {
                            // if no supported (i.e. SQLite) set byte array
                            byte[] bytes = IOUtils.toByteArray(is);
                            stmt.setBytes(dbIdx, bytes);
                        }
                    }
                } catch (IOException ioe) {
                    stmt.setNull(dbIdx, Types.BLOB);
                }
            }
        } else if (cspec.getType().isCompatible(CollectionDataValue.class)) {
            fillArray(stmt, dbIdx, cell, tz);
        } else if ((columnTypes == null) || cspec.getType().isCompatible(StringValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.VARCHAR);
            } else {
                stmt.setString(dbIdx, cell.toString());
            }
        } else {
            Integer sqlType = columnTypes.get(dbIdx);
            if (sqlType == null) {
                sqlType = Types.VARCHAR;
            }
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, sqlType);
            } else {
                stmt.setObject(dbIdx, cell.toString(), sqlType);
            }
        }
    }

    /**
     * @param stmt
     * @param dbIdx
     * @param cell
     * @param tz
     * @throws SQLException
     */
    protected void fillArray(final PreparedStatement stmt, final int dbIdx, final DataCell cell, final TimeZone tz)
        throws SQLException {
        if (cell.isMissing()) {
            stmt.setNull(dbIdx, Types.ARRAY);
        } else {
            final CollectionDataValue collection = (CollectionDataValue)cell;
            final Spliterator<DataCell> spliterator = collection.spliterator();
            final DataType baseType = cell.getType().getCollectionElementType();
            final Object[] vals = new Object[collection.size()];
            final String type;
            if (baseType.isCompatible(BooleanValue.class)) {
                type = "boolean";
                spliterator.forEachRemaining(new Consumer<DataCell>() {
                    int idx = 0;
                    @Override
                    public void accept(final DataCell t) {
                        vals[idx++] = t.isMissing() ? null : ((BooleanValue)t).getBooleanValue();
                    }
                });
            } else if (baseType.isCompatible(IntValue.class)) {
                type = "integer";
                spliterator.forEachRemaining(new Consumer<DataCell>() {
                    int idx = 0;
                    @Override
                    public void accept(final DataCell t) {
                        vals[idx++] = t.isMissing() ? null : ((IntValue)t).getIntValue();
                    }
                });
            } else if (baseType.isCompatible(LongValue.class)) {
                type = "bigint";
                spliterator.forEachRemaining(new Consumer<DataCell>() {
                    int idx = 0;
                    @Override
                    public void accept(final DataCell t) {
                        vals[idx++] = t.isMissing() ? null : ((LongValue)t).getLongValue();
                    }
                });
            } else if (baseType.isCompatible(DoubleValue.class)) {
                type = "double";
                spliterator.forEachRemaining(new Consumer<DataCell>() {
                    int idx = 0;
                    @Override
                    public void accept(final DataCell t) {
                        if (t.isMissing()) {
                            vals[idx++] = null;
                        } else {
                            final double value = ((DoubleValue)t).getDoubleValue();
                            vals[idx++] = Double.isNaN(value) ? null : value;
                        }
                    }
                });
            } else if (baseType.isCompatible(DateAndTimeValue.class)) {
                type = "timestamp";
            } else {
                type = "varchar";
                spliterator.forEachRemaining(new Consumer<DataCell>() {
                    int idx = 0;
                    @Override
                    public void accept(final DataCell t) {
                        vals[idx++] = t.isMissing() ? null : t.toString();
                    }
                });
            }
            @SuppressWarnings("resource") //will be closed by the framework
            final Connection conn = stmt.getConnection();
            final Array array = conn.createArrayOf(type, vals);
            stmt.setArray(dbIdx, array);
        }
    }

    /**
     * @param spec
     * @param sqlTypes
     * @param columnNamesForInsertStatement
     * @return the create table statement
     */
    protected String createTableStmt(final DataTableSpec spec,
            final Map<String, String> sqlTypes, final StringBuilder columnNamesForInsertStatement) {
        StringBuilder buf = new StringBuilder("(");
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (i > 0) {
                buf.append(", ");
                columnNamesForInsertStatement.append(", ");
            }
            DataColumnSpec cspec = spec.getColumnSpec(i);
            String colName = cspec.getName();
            String column = replaceColumnName(colName);
            buf.append(column + " " + sqlTypes.get(colName));

            columnNamesForInsertStatement.append(column);
        }
        buf.append(")");
        columnNamesForInsertStatement.append(')');

        return buf.toString();
    }

    /**
     * @param oldName old column name
     * @return new column name
     */
    protected String replaceColumnName(final String oldName) {
        final String colName;
        if (!m_conn.getAllowSpacesInColumnNames()) {
            //TK_TODO: this might replace not only spaces!!!
            colName = oldName.replaceAll("[^a-zA-Z0-9]", "_");
        } else {
            colName = oldName;
        }
        //always call the quote method to also quote key words etc.
        return m_conn.getUtility().getStatementManipulator().quoteIdentifier(colName);
    }
}

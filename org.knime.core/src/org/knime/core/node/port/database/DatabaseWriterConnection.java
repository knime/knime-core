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
 * ----------------------------------------------------------------------------
 */
package org.knime.core.node.port.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Creates a connection to write to database.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DatabaseWriterConnection {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DatabaseWriterConnection.class);

    private DatabaseWriterConnection() {
        // empty default constructor
    }

    /** Create connection to write into database.
     * @param dbConn a database connection object
     * @param data The data to write.
     * @param table name of table to write
     * @param appendData if checked the data is appended to an existing table
     * @param exec Used the cancel writing.
     * @param sqlTypes A mapping from column name to SQL-type.
     * @param cp {@link CredentialsProvider} providing user/password
     * @return error string or null, if non
     * @throws Exception if connection could not be established
     */
    public final static String writeData(
            final DatabaseConnectionSettings dbConn,
            final String table, final BufferedDataTable data,
            final boolean appendData, final ExecutionMonitor exec,
            final Map<String, String> sqlTypes,
            final CredentialsProvider cp) throws Exception {
        final Connection conn = dbConn.createConnection(cp);
        synchronized (dbConn.syncConnection(conn)) {
            DataTableSpec spec = data.getDataTableSpec();
            // mapping from spec columns to database columns
            final int[] mapping;
            // append data to existing table
            if (appendData) {
                Statement statement = null;
                ResultSet rs = null;
                try {
                    // try to count all rows to see if table exists
                    final String query = "SELECT * FROM " + table;
                    LOGGER.debug("Executing SQL statement \"" + query + "\"");
                    statement = conn.createStatement();
                    rs = statement.executeQuery(query);
                } catch (SQLException sqle) {
                    if (statement == null) {
                        throw new SQLException("Could not create SQL statement,"
                                + " reason: " + sqle.getMessage(), sqle);
                    }
                    LOGGER.info("Table \"" + table
                            + "\" does not exist in database, "
                            + "will create new table.");
                    // and create new table
                    final String query = "CREATE TABLE " + table + " "
                        + createStmt(spec, sqlTypes);
                    LOGGER.debug("Executing SQL statement \"" + query + "\"");
                    statement.execute(query);
                }
                // if table exists
                if (rs != null) {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    final Map<String, Integer> columnNames =
                        new LinkedHashMap<String, Integer>();
                    for (int i = 0; i < spec.getNumColumns(); i++) {
                        String colName = replaceColumnName(
                                spec.getColumnSpec(i).getName()).toLowerCase();
                        columnNames.put(colName, i);
                    }
                    // sanity check to lock if all input columns are in db
                    ArrayList<String> columnNotInSpec = new ArrayList<String>(
                            columnNames.keySet());
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        String colName =
                            rsmd.getColumnName(i + 1).toLowerCase();
                        if (columnNames.containsKey(colName)) {
                            columnNotInSpec.remove(colName);
                        }
                    }
                    if (columnNotInSpec.size() > 0) {
                        throw new RuntimeException("No. of columns in input"
                                + " table > in database; not existing columns: "
                                + columnNotInSpec.toString());
                    }
                    mapping = new int[rsmd.getColumnCount()];
                    for (int i = 0; i < mapping.length; i++) {
                        String name = rsmd.getColumnName(i + 1).toLowerCase();
                        if (!columnNames.containsKey(name)) {
                            mapping[i] = -1;
                            continue;
                        }
                        mapping[i] = columnNames.get(name);
                        DataColumnSpec cspec = spec.getColumnSpec(mapping[i]);
                        int type = rsmd.getColumnType(i + 1);
                        switch (type) {
                            // check all int compatible types
                            case Types.TINYINT:
                            case Types.SMALLINT:
                            case Types.INTEGER:
                            case Types.BIT:
                            case Types.BOOLEAN:
                                // types must be compatible to IntValue
                                if (!cspec.getType().isCompatible(IntValue.class)) {
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
                            case Types.BIGINT:
                                // types must also be compatible to DoubleValue
                                if (!cspec.getType().isCompatible(DoubleValue.class)) {
                                    throw new RuntimeException("Column \"" + name
                                            + "\" of type \"" + cspec.getType()
                                            + "\" from input does not match type "
                                            + "\"" + rsmd.getColumnTypeName(i + 1)
                                            + "\" in database at position " + i);
                                }
                                break;
                            // check for data compatible types
                            case Types.DATE:
                            case Types.TIME:
                            case Types.TIMESTAMP:
                                // those types must also be compatible to DataValue
                                if (!cspec.getType().isCompatible(
                                        DateAndTimeValue.class)) {
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
                    rs.close();
                    statement.close();
                } else {
                    mapping = new int[spec.getNumColumns()];
                    for (int k = 0; k < mapping.length; k++) {
                        mapping[k] = k;
                    }
                }
            } else {
                mapping = new int[spec.getNumColumns()];
                for (int k = 0; k < mapping.length; k++) {
                    mapping[k] = k;
                }
                Statement statement = null;
                try {
                    statement = conn.createStatement();
                    // remove existing table (if any)
                    final String query = "DROP TABLE " + table;
                    LOGGER.debug("Executing SQL statement \"" + query + "\"");
                    statement.execute(query);
                } catch (Throwable t) {
                    if (statement == null) {
                        throw new SQLException("Could not create SQL statement,"
                            + " reason: " + t.getMessage(), t);
                    }
                    LOGGER.info("Can't drop table \"" + table
                            + "\", will create new table.");
                }
                // and create new table
                final String query = "CREATE TABLE " + table + " "
                    + createStmt(spec, sqlTypes);
                LOGGER.debug("Executing SQL statement \"" + query + "\"");
                statement.execute(query);
                statement.close();
            }

            // creates the wild card string based on the number of columns
            // this string it used every time an new row is inserted into the db
            final StringBuilder wildcard = new StringBuilder("(");
            for (int i = 0; i < mapping.length; i++) {
                if (i > 0) {
                    wildcard.append(", ?");
                } else {
                    wildcard.append("?");
                }
            }
            wildcard.append(")");

            // problems writing more than 13 columns. the prepare statement
            // ensures that we can set the columns directly row-by-row, the
            // database will handle the commit
            int rowCount = data.getRowCount();
            int cnt = 1;
            int errorCnt = 0;
            int allErrors = 0;

            // create table meta data with empty column information
            final String query = "INSERT INTO "
                + table + " VALUES " + wildcard.toString();
            LOGGER.debug("Executing SQL statement \"" + query + "\"");
            final PreparedStatement stmt = conn.prepareStatement(query);
            try {
                conn.setAutoCommit(false);
                for (RowIterator it = data.iterator(); it.hasNext(); cnt++) {
                    exec.checkCanceled();
                    exec.setProgress(1.0 * cnt / rowCount, "Row " + "#" + cnt);
                    DataRow row = it.next();
                    for (int i = 0; i < mapping.length; i++) {
                        int dbIdx = i + 1;
                        if (mapping[i] < 0) {
                            stmt.setNull(dbIdx, Types.NULL);
                            continue;
                        }
                        DataColumnSpec cspec = spec.getColumnSpec(mapping[i]);
                        DataCell cell = row.getCell(mapping[i]);
                        if (cspec.getType().isCompatible(IntValue.class)) {
                            if (cell.isMissing()) {
                                stmt.setNull(dbIdx, Types.INTEGER);
                            } else {
                                int integer = ((IntValue) cell).getIntValue();
                                stmt.setInt(dbIdx, integer);
                            }
                        } else if (cspec.getType().isCompatible(
                                DoubleValue.class)) {
                            if (cell.isMissing()) {
                                stmt.setNull(dbIdx, Types.DOUBLE);
                            } else {
                                double dbl =
                                    ((DoubleValue) cell).getDoubleValue();
                                if (Double.isNaN(dbl)) {
                                    stmt.setNull(dbIdx, Types.DOUBLE);
                                } else {
                                    stmt.setDouble(dbIdx, dbl);
                                }
                            }
                        } else if (cspec.getType().isCompatible(
                                DateAndTimeValue.class)) {
                            if (cell.isMissing()) {
                                stmt.setNull(dbIdx, Types.DATE);
                            } else {
                                DateAndTimeValue dateCell =
                                    (DateAndTimeValue) cell;
                                if (!dateCell.hasTime()
                                        && !dateCell.hasMillis()) {
                            		java.sql.Date date = new java.sql.Date(
                            				dateCell.getUTCTimeInMillis());
                            		stmt.setDate(dbIdx, date);
                            	} else if (!dateCell.hasDate()) {
                            		java.sql.Time time = new java.sql.Time(
                            				dateCell.getUTCTimeInMillis());
                            		stmt.setTime(dbIdx, time);
                            	} else {
                            		java.sql.Timestamp timestamp =
                            		    new java.sql.Timestamp(
                            		            dateCell.getUTCTimeInMillis());
                            		stmt.setTimestamp(dbIdx, timestamp);
                            	}
                            }
                        } else {
                            if (cell.isMissing()) {
                                stmt.setNull(dbIdx, Types.VARCHAR);
                            } else {
                                stmt.setString(dbIdx, cell.toString());
                            }
                        }
                    }
                    try {
                        stmt.execute();
                    } catch (Throwable t) {
                        allErrors++;
                        if (errorCnt > -1) {
                            String errorMsg = "Error in row #" + cnt + ": "
                                + row.getKey() + ", " + t.getMessage();
                            exec.setMessage(errorMsg);
                            if (errorCnt++ < 10) {
                                LOGGER.warn(errorMsg);
                            } else {
                                errorCnt = -1;
                                LOGGER.warn(errorMsg + " - more errors...", t);
                            }
                        }
                    }
                }
                conn.commit();
                conn.setAutoCommit(true);
                if (allErrors == 0) {
                    return null;
                } else {
                    return "Errors \"" + allErrors + "\" writing "
                        + rowCount + " rows.";
                }
            } finally {
                stmt.close();
            }
        }
    }

    private static String createStmt(final DataTableSpec spec,
            final Map<String, String> sqlTypes) {
        StringBuilder buf = new StringBuilder("(");
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (i > 0) {
                buf.append(", ");
            }
            DataColumnSpec cspec = spec.getColumnSpec(i);
            String colName = cspec.getName();
            String column = replaceColumnName(colName);
            buf.append(column + " " + sqlTypes.get(colName));
        }
        buf.append(")");
        return buf.toString();
    }

    private static String replaceColumnName(final String oldName) {
        return oldName.replaceAll("[^a-zA-Z0-9]", "_");
    }

}

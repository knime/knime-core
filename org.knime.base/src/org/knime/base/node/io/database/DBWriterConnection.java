/*
 * ----------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package org.knime.base.node.io.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
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
import org.knime.core.node.port.database.DatabaseConnectionSettings;


/**
 * Creates a connection to write to database.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBWriterConnection {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DBWriterConnection.class);

    private DBWriterConnection() {

    }

    /**
     * Create connection to write into database.
     * @param dbConn a database connection object
     * @param data The data to write.
     * @param table name of table to write
     * @param appendData if checked the data is appended to an existing table
     * @param exec Used the cancel writing.
     * @param sqlTypes A mapping from column name to SQL-type.
     * @return error string or null, if non
     * @throws Exception if connection could not be established
     */
    static final String writeData(final DatabaseConnectionSettings dbConn,
            final String table, final BufferedDataTable data,
            final boolean appendData, final ExecutionMonitor exec,
            final Map<String, String> sqlTypes) throws Exception {
        final Connection conn = dbConn.createConnection();
        DataTableSpec spec = data.getDataTableSpec();
        // mapping from spec columns to database columns
        final int[] mapping;
        // append data to existing table
        if (appendData) {
            ResultSet rs = null;
            try {
                // try to count all rows to see if table exists
                rs = conn.createStatement().executeQuery(
                        "SELECT * FROM " + table);
            } catch (SQLException sqle) {
                LOGGER.info("Table \"" + table
                        + "\" does not exist in database, "
                        + "will create new table.");
                // and create new table
                conn.createStatement().execute("CREATE TABLE " + table + " "
                        + createStmt(spec, sqlTypes));
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
                // sanity check to lock if all input column are present in db
                ArrayList<String> columnNotInSpec = new ArrayList<String>(
                        columnNames.keySet());
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    String colName = rsmd.getColumnName(i + 1).toLowerCase();
                    if (columnNames.containsKey(colName)) {
                        columnNotInSpec.remove(colName);
                    }
                }
                if (columnNotInSpec.size() > 0) {
                    throw new RuntimeException("No. of columns in input table"
                            + " > in database; not existing columns: "
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
                            // those types must be compatible to IntValue
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
                            // those types must also be compatible to IntValue
                            if (!cspec.getType().isCompatible(IntValue.class)) {
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
            try {
                // remove existing table (if any)
                conn.createStatement().execute("DROP TABLE " + table);
            } catch (Throwable t) {
                LOGGER.info("Can't drop table \"" + table
                        + "\", will create new table.");
            }
            // and create new table
            conn.createStatement().execute("CREATE TABLE " + table + " "
                    + createStmt(spec, sqlTypes));
        }

        // creates the wild card string based on the number of columns
        // this string it used everytime an new row is inserted into the db
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
        // ensures that we can set the columns directly row-by-row, the database
        // will handle the commit
        int rowCount = data.getRowCount();
        int cnt = 1;
        int errorCnt = 0;
        int allErrors = 0;

        // create table meta data with empty column information
        final PreparedStatement stmt = conn.prepareStatement("INSERT INTO "
                + table + " VALUES " + wildcard.toString());
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
                            double dbl = ((DoubleValue) cell).getDoubleValue();
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
                        	DateAndTimeValue dateCell = (DateAndTimeValue) cell;
                        	if (!dateCell.hasTime() && !dateCell.hasMillis()) {
                        		java.sql.Date date = new java.sql.Date(
                        				dateCell.getUTCTimeInMillis());
                        		stmt.setDate(dbIdx, date);
                        	} else if (!dateCell.hasDate()) {
                        		java.sql.Time time = new java.sql.Time(
                        				dateCell.getUTCTimeInMillis());
                        		stmt.setTime(dbIdx, time);
                        	} else {
                        		java.sql.Timestamp timestamp = new java.sql.Timestamp(
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
            conn.close();
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

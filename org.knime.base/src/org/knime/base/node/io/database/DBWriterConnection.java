/*
 * ----------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package org.knime.base.node.io.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;


/**
 * Creates a connection to write to database.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBWriterConnection {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DBWriterConnection.class);
    
    /**
     * Trys to open database connection.
     * @param url To this URL.
     * @param user User name.
     * @param pw password.
     * @param table The table name to write into.
     * @throws SQLException If connection could not be established.
     */
    DBWriterConnection(final String url, final String user, final String pw,
            final String table)
        throws SQLException {
        assert table == table;
        Connection conn = DriverManager.getConnection(url, user, pw);
        conn.close();
        
    }

    /**
     * Create connection to write into database.
     * @param conn An already opened connection to a database.
     * @param url The database URL.
     * @param table The table name to write into.
     * @param data The data to write.
     * @param exec Used the cancel writting.
     * @throws SQLException If connection could not be established.
     * @throws CanceledExecutionException If canceled.
     */
    DBWriterConnection(final Connection conn, final String url,
            final String table, final BufferedDataTable data,
            final ExecutionMonitor exec) throws SQLException,
            CanceledExecutionException {
        
        /*
         * TODO Feature request by Brian: 
         * 'Although I do not at all think that this
         * is the right long-term solution for the problem we are seeing
         * (molecular structure strings are commonly too long to write to
         * 255-character VARCHAR columns in Oracle), this slight modification
         * should at least temporarily resolve the issue.  Line 86 and 87 of 
         * the current DBWriterConnection.java:'
         */
        DataTableSpec spec = data.getDataTableSpec();
        String cols = createColumnName(spec);
        
        // If we're using oracle, size any varchar(255) columns
        // to 4000 instead. Oracle will auto-interpret this as
        // varchar2(4000) upon table creation.
        if (url.indexOf("oracle") != -1) {
            cols = cols.replaceAll("255", "4000");
        }
        
        // bugfix for jdbd-odbc (e.g. MS Access) bridges which don't allow 
        // numeric SQL-type followed by parameter(s)
        if (url.startsWith("jdbc:odbc:")) {
            cols = cols.replace("numeric(30,10)", "numeric");
        }
        
        LOGGER.debug(cols);
               
        // String cols = createColumnName(data.getDataTableSpec());
        // LOGGER.debug(cols);
        try {
            conn.createStatement().execute("DROP TABLE " + table);
        } catch (Exception e) {
            LOGGER.debug("Table " + table + " not available.", e);
        }
        conn.createStatement().execute("CREATE TABLE " + table + " " + cols);
        
        StringBuilder wildcard = new StringBuilder("(");
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (i > 0) {
                wildcard.append(", ?");
            } else {
                wildcard.append("?");
            }
        }
        wildcard.append(")");
        
        // create table meta data with empty column information
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO " + table
                + " VALUES " + wildcard.toString());
        conn.setAutoCommit(false);
        
        // bugfix: problems writing more than 13 columns. the prepare statement 
        // ensures that we can set the columns directly row-by-row, the database
        // will handle the commit
        int rowCount = data.getRowCount();
        int cnt = 1;
        for (RowIterator it = data.iterator(); it.hasNext(); cnt++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * cnt / rowCount, "Row " + "#" + cnt);
            DataRow row = it.next();
            for (int i = 0; i < row.getNumCells(); i++) {
                int dbIdx = i + 1;
                DataColumnSpec cspec = spec.getColumnSpec(i);
                DataCell cell = row.getCell(i);
                if (cspec.getType().isCompatible(IntValue.class)) {
                    if (cell.isMissing()) {
                        stmt.setNull(dbIdx, Types.INTEGER);
                    } else {
                        int integer = ((IntValue) cell).getIntValue();
                        stmt.setInt(dbIdx, integer);
                    }
                } else if (cspec.getType().isCompatible(DoubleValue.class)) {
                    if (cell.isMissing()) {
                        stmt.setNull(dbIdx, Types.DECIMAL);
                    } else {
                        double dbl = ((DoubleValue) cell).getDoubleValue();
                        stmt.setDouble(dbIdx, dbl);
                    }
                } else {
                    if (cell.isMissing()) {
                        stmt.setNull(dbIdx, Types.VARCHAR);
                    } else {
                        stmt.setString(dbIdx, cell.toString());
                    }
                }
            }
            stmt.execute();
        }
        conn.commit();
        conn.setAutoCommit(true);
        stmt.close();
        conn.close();
    }
    
    private String createColumnName(final DataTableSpec spec) {
        StringBuilder buf = new StringBuilder("(");
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (i > 0) {
                buf.append(", ");
            }
            DataColumnSpec cspec = spec.getColumnSpec(i);
            String column = cspec.getName().replaceAll("[^a-zA-Z0-9]", "_");
            if (cspec.getType().isCompatible(IntValue.class)) {
                buf.append(column + " integer");
            } else if (cspec.getType().isCompatible(DoubleValue.class)) {
                // bugfix 791, some databases need these arguments to write 
                // doubles propertly and don't truncate them
                buf.append(column + " numeric(30,10)");
            } else {
                buf.append(column + " varchar(255)");
            }
        }
        return buf.toString() + ")";
    }

}

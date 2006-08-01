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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ----------------------------------------------------------------------------
 */
package org.knime.base.node.io.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
 * 
 * @author Thomas Gabriel, Konstanz University
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
     * @param url The URL.
     * @param user The user name.
     * @param pw The password.
     * @param table The table name to write into.
     * @param data The data to write.
     * @param exec Used the cancel writting.
     * @throws SQLException If connection could not be established.
     * @throws CanceledExecutionException If canceled.
     */
    DBWriterConnection(final String url, final String user, final String pw,
            final String table, final BufferedDataTable data,
            final ExecutionMonitor exec) throws SQLException,
            CanceledExecutionException {
        Connection conn = DriverManager.getConnection(url, user, pw);
        Statement stmt = conn.createStatement();
        DataTableSpec spec = data.getDataTableSpec();
        String cols = createColumnName(data.getDataTableSpec());
        LOGGER.debug(cols);
        try {
            stmt.execute("DROP TABLE " + table);
        } catch (Exception e) {
            LOGGER.debug("Table " + table + " not available.");
        }
        stmt.execute("CREATE TABLE " + table + " " + cols);
        int rowCount = data.getRowCount();
        int cnt = 1;
        for (RowIterator it = data.iterator(); it.hasNext(); cnt++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * cnt / rowCount, "Row " + "#" + cnt);
            String values = rowToValueString(it.next(), spec);
            //LOGGER.debug(values);
            stmt.addBatch("INSERT INTO " + table + " " + values);
        }
        exec.setMessage("Commiting data...");
        stmt.executeBatch();
        stmt.close();
        conn.close();
    }
    
    private String createColumnName(final DataTableSpec spec) {
        StringBuffer buf = new StringBuffer("(");
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (i > 0) {
                buf.append(", ");
            }
            DataColumnSpec cspec = spec.getColumnSpec(i);
            String column = cspec.getName().replaceAll("[^a-zA-Z0-9]", "_");
            if (cspec.getType().isCompatible(IntValue.class)) {
                buf.append(column + " integer");
            } else if (cspec.getType().isCompatible(DoubleValue.class)) {
                buf.append(column + " numeric");
            } else {
                buf.append(column + " varchar");
            }
        }
        return buf.toString() + ")";
    }
        

    private String rowToValueString(final DataRow row, 
            final DataTableSpec spec) {
        StringBuffer buf = new StringBuffer("(");
        for (int i = 0; i < row.getNumCells(); i++) {
            DataColumnSpec cspec = spec.getColumnSpec(i);
            DataCell cell = row.getCell(i);
            if (i > 0) {
                buf.append(", ");
            }
            if (cell.isMissing()) {
                buf.append("null");
            } else if (cspec.getType().isCompatible(IntValue.class)) {
                int integer = ((IntValue) cell).getIntValue();
                buf.append(integer);
            } else if (cspec.getType().isCompatible(DoubleValue.class)) {
                double dbl = ((DoubleValue) cell).getDoubleValue();
                buf.append(dbl);
            } else {
                buf.append("'" + cell.toString() + "'");
            }
        }
        buf.append(")");
        return "VALUES " + buf.toString();
    }
}

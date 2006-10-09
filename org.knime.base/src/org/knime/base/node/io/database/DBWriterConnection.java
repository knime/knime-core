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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

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
     * Create connection to write into database.
     * @param conn An already opened connection to a database.
     * @param table The table name to write into.
     * @param data The data to write.
     * @param exec Used the cancel writting.
     * @param sqlTypes A mapping from column name to SQL-type. 
     * @throws SQLException If connection could not be established.
     * @throws CanceledExecutionException If canceled.
     */
    DBWriterConnection(final Connection conn,
            final String table, final BufferedDataTable data,
            final ExecutionMonitor exec, final Map<String, String> sqlTypes) 
            throws SQLException, CanceledExecutionException {
        
        DataTableSpec spec = data.getDataTableSpec();
        StringBuilder buf = new StringBuilder("(");
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (i > 0) {
                buf.append(", ");
            }
            DataColumnSpec cspec = spec.getColumnSpec(i);
            String colName = cspec.getName();
            String column = colName.replaceAll("[^a-zA-Z0-9]", "_");
            buf.append(column + " " + sqlTypes.get(colName));
        }
        buf.append(")");
        LOGGER.debug(buf.toString());
        
        try {
            conn.createStatement().execute("DROP TABLE " + table);
        } catch (Exception e) {
            LOGGER.debug("Can't drop table, don't worry will create new one.");
        }
        // and create new table
        conn.createStatement().execute("CREATE TABLE " + table + " " 
                + buf.toString());

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
        int errorCnt = 0;
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
                        stmt.setNull(dbIdx, Types.NUMERIC);
                    } else {
                        double dbl = ((DoubleValue) cell).getDoubleValue();
                        if (Double.isNaN(dbl)) {
                            stmt.setNull(dbIdx, Types.NUMERIC);
                        } else {
                            stmt.setDouble(dbIdx, dbl);
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
            } catch (Exception e) {
                if (errorCnt > -1) {
                    if (errorCnt++ < 100) {
                        LOGGER.warn("Error in row #" + cnt + ": " 
                                + row.getKey() + ", " + e.getMessage(), e);
                    } else {
                        errorCnt = -1;
                        LOGGER.warn("Error in row #" + cnt + ": " 
                                + row.getKey() + ", " + e.getMessage() 
                                + " - more errors...", e);
                    }
                }
            }
        }
        conn.commit();
        conn.setAutoCommit(true);
        stmt.close();
        conn.close();
    }
    
}

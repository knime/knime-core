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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

/**
 * Creates a connection to read from database. 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DBReaderConnection implements DataTable {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DBReaderConnection.class);

    private final DataTableSpec m_spec;

    private final Connection m_conn;

    private final String m_query;

    /**
     * Create connection to database and read meta info.
     * @param url The URL.
     * @param user The user.
     * @param pw The password.
     * @param query The sql query.
     * @throws SQLException If connection could not established.
     */
    DBReaderConnection(final String url, final String user, final String pw,
            final String query) throws SQLException {
        DriverManager.setLoginTimeout(5);
        m_conn = DriverManager.getConnection(url, user, pw);
        Statement stmt = m_conn.createStatement();
        m_query = query;
        ResultSet result = stmt.executeQuery(m_query);
        m_spec = createTableSpec(result.getMetaData());
        stmt.close();
    }

    /**
     * Closes connection.
     */
    public void close() {
        try {
            m_conn.close();
        } catch (SQLException e) {
            LOGGER.warn(e);
        }
    }

    /**
     * @see org.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    public RowIterator iterator() {
        try {
            Statement stmt = m_conn.createStatement();
            ResultSet result = stmt.executeQuery(m_query);
            // stmt.close();
            return new DBRowIterator(m_spec, result);
        } catch (SQLException e) {
            LOGGER.error(e);
            return new DBRowIterator(m_spec, null);
        }
    }

    private DataTableSpec createTableSpec(final ResultSetMetaData meta)
            throws SQLException {
        int cols = meta.getColumnCount();
        DataColumnSpec[] cspecs = new DataColumnSpec[cols];
        for (int i = 0; i < cols; i++) {
            int dbIdx = i + 1;
            String name = meta.getColumnName(dbIdx);
            int type = meta.getColumnType(dbIdx);
            DataType newType;
            switch (type) {
                // bugfix: support all types which can be handled as integer
                case Types.INTEGER:
                case Types.BIT: // TODO (tg) later we might use BooleanType
                case Types.BINARY:
                case Types.BOOLEAN:
                case Types.VARBINARY:
                case Types.SMALLINT:
                case Types.TINYINT:
                case Types.BIGINT:
                    newType = IntCell.TYPE;
                    break;
                // bugfix: support all types which can be handled as double
                case Types.FLOAT:
                case Types.DOUBLE:
                case Types.NUMERIC:
                case Types.DECIMAL:
                case Types.REAL:
                    newType = DoubleCell.TYPE;
                    break;
                default:
                    newType = StringCell.TYPE;
            }
            cspecs[i] = new DataColumnSpecCreator(name, newType).createSpec();
        }
        return new DataTableSpec(cspecs);
    }
}

/**
 * RowIterator via a database ResultSet.
 */
final class DBRowIterator extends RowIterator {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DBReaderConnection.class);

    private boolean m_end;

    private final DataTableSpec m_spec;

    private final ResultSet m_result;

    /**
     * Creates new iterator.
     * 
     * @param spec With the given spec.
     * @param result Underlying ResultSet.
     */
    DBRowIterator(final DataTableSpec spec, final ResultSet result) {
        m_spec = spec;
        m_result = result;
        m_end = end();
    }

    /**
     * @see org.knime.core.data.RowIterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return !m_end;
    }

    /**
     * @see org.knime.core.data.RowIterator#next()
     */
    @Override
    public DataRow next() {
        DataCell[] cells = new DataCell[m_spec.getNumColumns()];
        for (int i = 0; i < cells.length; i++) {
            DataType type = m_spec.getColumnSpec(i).getType();
            if (type.isCompatible(IntValue.class)) {
                int integer = -1;
                try {
                    integer = m_result.getInt(i + 1);
                } catch (SQLException sqle) {
                    LOGGER.error("SQL Exception reading Int:", sqle);
                }
                if (wasNull()) {
                    cells[i] = DataType.getMissingCell();
                } else {
                    cells[i] = new IntCell(integer);
                }
            } else if (type.isCompatible(DoubleValue.class)) {
                double dbl = Double.NaN;
                try {
                    dbl = m_result.getDouble(i + 1);
                } catch (SQLException sqle) {
                    LOGGER.error("SQL Exception reading Double:", sqle);
                }
                if (wasNull()) {
                    cells[i] = DataType.getMissingCell();
                } else {
                    cells[i] = new DoubleCell(dbl);
                }
            } else {
                String s = "<invalid>";
                try {
                    s = m_result.getString(i + 1);
                } catch (SQLException sqle) {
                    LOGGER.error("SQL Exception reading String:", sqle);
                }
                if (s == null || wasNull()) {
                    cells[i] = DataType.getMissingCell();
                } else {
                    cells[i] = new StringCell(s);
                }
            }
        }
        int rowId = cells.hashCode();
        try {
            rowId = m_result.getRow();
        } catch (SQLException sqle) {
            LOGGER.error("SQL Exception:", sqle);        
        }
        m_end = end();
        return new DefaultRow(new StringCell("Row_" + rowId), cells);
    }
    
    private boolean end() {
        try {
            return !m_result.next();
        } catch (SQLException sqle) {
            LOGGER.error("SQL Exception:", sqle);
            return true;
        }
    }
    
    private boolean wasNull() {
        try {
            return m_result.wasNull();
        } catch (SQLException sqle) {
            LOGGER.error("SQL Exception:", sqle);
            return true;
        }
    }

}

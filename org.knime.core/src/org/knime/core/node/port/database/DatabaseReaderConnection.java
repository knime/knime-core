/*
 * ----------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package org.knime.core.node.port.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * Creates a connection to read from database.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DatabaseReaderConnection {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DatabaseReaderConnection.class);

    private DataTableSpec m_spec;
    
    private DatabaseQueryConnectionSettings m_conn;
    
    private PreparedStatement m_stmt;
    
    public DatabaseReaderConnection() {
        
    }
    
    /**
     * Create connection to database and read meta info.
     * @param conn a database connection object
     * @throws SQLException {@link SQLException}
     * @throws InvalidSettingsException {@link InvalidSettingsException}
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws IOException {@link IOException}
     */
    public DatabaseReaderConnection(final DatabaseQueryConnectionSettings conn) 
            throws SQLException, InvalidSettingsException, 
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
            IOException {
        setDBQueryConnection(conn);
    }
    
    public void setDBQueryConnection(final DatabaseQueryConnectionSettings conn)
            throws SQLException, InvalidSettingsException, 
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
            IOException {
        PreparedStatement stmt = null;
        m_conn = conn;
        try {
            String tableID = "table_" + hashCode();
            String pQuery = "SELECT * FROM (" + conn.getQuery() + ") " 
                + tableID + " WHERE 1 = 0";
            stmt = conn.createConnection().prepareStatement(pQuery);
            stmt.execute();
            m_spec = createTableSpec(stmt.getMetaData());
        } catch (SQLException sql) {
            if (stmt != null) {
                stmt.close();
            }
            m_stmt = null;
            throw sql;
        }
        m_stmt = stmt;
    }
    
    public DatabaseQueryConnectionSettings getQueryConnection() {
        return m_conn;
    }

    /**
     *
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }
    
    /**
     * 
     * @param exec
     * @return buffered data table read from database
     * @throws CanceledExecutionException
     * @throws SQLException
     */
    public BufferedDataTable createTable(final ExecutionContext exec)
            throws CanceledExecutionException, SQLException {
        m_stmt.execute(m_conn.getQuery());
        final ResultSet result = m_stmt.getResultSet();
        BufferedDataTable table = exec.createBufferedDataTable(new DataTable() {
            /**
             * {@inheritDoc}
             */
            @Override
            public DataTableSpec getDataTableSpec() {
                return m_spec;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public RowIterator iterator() {
                return new DBRowIterator(result);
            }

        }, exec);
        result.close();
        m_stmt.close();
        return table;
    }
    
    /**
     * @return buffered data table read from database
     * @throws SQLException
     */
    DataTable createTable(final int cachedNoRows) throws SQLException {
        String query;
        if (cachedNoRows < 0) {
            query = m_conn.getQuery();
        } else {
            String tableID = "table_" + hashCode();
            query = "SELECT * FROM (" + m_conn.getQuery() + ") " + tableID; 
            m_stmt.setMaxRows(cachedNoRows);
        }
        m_stmt.execute(query);
        final ResultSet result = m_stmt.getResultSet();
        DBRowIterator it = new DBRowIterator(result);
        DataContainer buf = new DataContainer(m_spec);
        while (it.hasNext()) {
            buf.addRowToTable(it.next());
        }
        buf.close();
        DataTable table = buf.getTable();
        result.close();
        m_stmt.close();
        return table;
    }

    private DataTableSpec createTableSpec(final ResultSetMetaData meta)
            throws SQLException {
        int cols = meta.getColumnCount();
        DataTableSpec spec = null;
        for (int i = 0; i < cols; i++) {
            int dbIdx = i + 1;
            String name = meta.getColumnName(dbIdx);
            int type = meta.getColumnType(dbIdx);
            DataType newType;
            switch (type) {
                case Types.INTEGER:
                case Types.BIT:
                case Types.BINARY:
                case Types.BOOLEAN:
                case Types.VARBINARY:
                case Types.SMALLINT:
                case Types.TINYINT:
                case Types.BIGINT:
                    newType = IntCell.TYPE;
                    break;
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
            if (spec == null) {
                spec = new DataTableSpec("database", 
                        new DataColumnSpecCreator(
                        name, newType).createSpec());
            } else {
                name = DataTableSpec.getUniqueColumnName(spec, name);
                spec = new DataTableSpec("database", spec, 
                       new DataTableSpec(new DataColumnSpecCreator(
                               name, newType).createSpec()));
            }
        }
        return spec;
    }

    /**
     * RowIterator via a database ResultSet.
     */
    private class DBRowIterator extends RowIterator {
        
        private final ResultSet m_result;

        /**
         * Creates new iterator.
         */
        DBRowIterator(final ResultSet result) {
            m_result = result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            boolean ret = false;
            try {
                ret = m_result.next();
            } catch (SQLException sql) {
                ret = false;
            }
            if (!ret) {
                try {
                    m_result.close();
                } catch (SQLException e) {
                    LOGGER.error("SQL Exception: ", e);
                } catch (Throwable t) {
                    LOGGER.fatal("Unknown error: ", t);
                }
            }
            return ret;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            DataCell[] cells = new DataCell[m_spec.getNumColumns()];
            for (int i = 0; i < cells.length; i++) {
                DataType type = m_spec.getColumnSpec(i).getType();
                if (type.isCompatible(IntValue.class)) {
                    try {
                        int integer = m_result.getInt(i + 1);
                        if (wasNull()) {
                            cells[i] = DataType.getMissingCell();
                        } else {
                            cells[i] = new IntCell(integer);
                        }
                    } catch (SQLException sqle) {
                        LOGGER.error("SQL Exception reading Int:", sqle);
                        cells[i] = DataType.getMissingCell();
                    }
                } else if (type.isCompatible(DoubleValue.class)) {
                    try {
                        double dbl = m_result.getDouble(i + 1);
                        if (wasNull()) {
                            cells[i] = DataType.getMissingCell();
                        } else {
                            cells[i] = new DoubleCell(dbl);
                        }
                    } catch (SQLException sqle) {
                        LOGGER.error("SQL Exception reading Double:", sqle);
                        cells[i] = DataType.getMissingCell();
                    }
                } else {
                    String s = null;
                    try {
                        int dbType =
                                m_result.getMetaData().getColumnType(i + 1);
                        if (dbType == Types.CLOB) {
                            Clob clob = m_result.getClob(i + 1);
                            if (wasNull() || clob == null) {
                                s = null;
                            } else {
                                BufferedReader buf =
                                        new BufferedReader(clob
                                                .getCharacterStream());
                                StringBuilder sb = null;
                                String line;
                                while ((line = buf.readLine()) != null) {
                                    if (sb == null) {
                                        sb = new StringBuilder();
                                    } else {
                                        sb.append("\n");
                                    }
                                    sb.append(line);
                                }
                                s = (sb == null ? null : sb.toString()); 
                            }
                        } else if (dbType == Types.BLOB) {
                            Blob blob = m_result.getBlob(i + 1);
                            if (wasNull() || blob == null) {
                                s = null;
                            } else {
                                InputStream is = blob.getBinaryStream();
                                BufferedReader buf =
                                        new BufferedReader(
                                                new InputStreamReader(is));
                                StringBuilder sb = null;
                                String line;
                                while ((line = buf.readLine()) != null) {
                                    if (sb == null) {
                                        sb = new StringBuilder();
                                    } else {
                                        sb.append("\n");
                                    }
                                    sb.append(line);
                                }
                                s = (sb == null ? null : sb.toString()); 
                            }
                        } else {
                            s = m_result.getString(i + 1);
                            if (wasNull()) {
                                s = null;
                            }
                        }
                    } catch (SQLException sqle) {
                        LOGGER.error("SQL Exception reading String:", sqle);
                    } catch (IOException ioe) {
                        LOGGER.error("I/O Exception reading String:", ioe);
                    }
                    if (s == null) {
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
            return new DefaultRow(RowKey.createRowKey(rowId), cells);
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
}

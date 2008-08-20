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
package org.knime.base.node.io.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * Creates a connection to read from database.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBReaderConnection implements DataTable {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DBReaderConnection.class);

    private final DataTableSpec m_spec;

    private final Connection m_conn;

    private final String m_query;

    private final int m_cacheNoRows;
    
    /**
     * Create connection to database and read meta info.
     * 
     * @param conn a database connection object
     * @param query SQL query executed to read data
     * @throws SQLException {@link SQLException}
     * @throws InvalidSettingsException {@link InvalidSettingsException}
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws IOException {@link IOException}
     */
    DBReaderConnection(final DBConnection conn, final String query) throws
            InvalidSettingsException, SQLException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException, IOException {
        this(conn, query, Integer.MAX_VALUE);
    }

    /**
     * Create connection to database and read meta info.
     * 
     * @param conn a database connection object
     * @param query SQL query executed to read data
     * @param cacheNoRows number of rows cached
     * @throws SQLException {@link SQLException}
     * @throws InvalidSettingsException {@link InvalidSettingsException}
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws IOException {@link IOException}
     */
    DBReaderConnection(final DBConnection conn, final String query,
            final int cacheNoRows) throws SQLException, 
            InvalidSettingsException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException, IOException {
        m_cacheNoRows = cacheNoRows;
        Statement stmt = null;
        ResultSet result = null;
        try {
            m_conn = conn.createConnection();
            stmt = m_conn.createStatement();
            m_query = query;
            result = stmt.executeQuery(m_query);
            m_spec = createTableSpec(result.getMetaData());
        } finally {
            if (result != null) {
                result.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Closes connection.
     */
    void close() {
        try {
            m_conn.close();
        } catch (Throwable t) {
            LOGGER.info("Could not close database connection, reason: "
                    + t.getMessage(), t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        try {
            Statement stmt = m_conn.createStatement();
            ResultSet result = stmt.executeQuery(m_query);
            // stmt.close();
            return new DBRowIterator(result);
        } catch (Throwable t) {
            LOGGER.error(t);
            return new DBRowIterator(null);
        }
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
                spec = new DataTableSpec(new DataColumnSpecCreator(
                        name, newType).createSpec());
            } else {
                name = DataTableSpec.getUniqueColumnName(spec, name);
                spec = new DataTableSpec(spec, new DataTableSpec(
                       new DataColumnSpecCreator(name, newType).createSpec()));
            }
        }
        return spec;
    }

    /**
     * RowIterator via a database ResultSet.
     */
    private class DBRowIterator extends RowIterator {

        private boolean m_end;

        private final ResultSet m_result;

        private int m_rowCnt = 0;

        /**
         * Creates new iterator.
         * 
         * @param result Underlying ResultSet.
         */
        DBRowIterator(final ResultSet result) {
            m_result = result;
            m_end = end();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return !m_end;
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
            m_end = end();
            m_rowCnt++;
            return new DefaultRow("Row_" + rowId, cells);
        }

        private boolean end() {
            boolean ret;
            if (m_rowCnt + 1 == m_cacheNoRows) {
                ret = true;
            } else {
                try {
                    ret = !m_result.next();
                } catch (SQLException sqle) {
                    LOGGER.error("SQL Exception:", sqle);
                    ret = true;
                }
                if (ret) {
                    try {
                        m_result.close();
                        m_conn.close();
                    } catch (Exception e) {
                        LOGGER.error("SQL Exception:", e);
                    }
                }
            }
            return ret;
                
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

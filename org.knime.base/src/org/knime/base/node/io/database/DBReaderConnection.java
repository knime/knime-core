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

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

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

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 
 * @author Thomas Gabriel, Konstanz University
 */
final class DBReaderConnection implements DataTable {

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
            return null;
        }
    }

    private DataTableSpec createTableSpec(final ResultSetMetaData meta)
            throws SQLException {
        int cols = meta.getColumnCount();
        DataColumnSpec[] cspecs = new DataColumnSpec[cols];
        for (int i = 0; i < cols; i++) {
            String name = meta.getColumnName(i + 1);
            int type = meta.getColumnType(i + 1);
            DataType newType;
            switch (type) {
            case Types.INTEGER:
                newType = IntCell.TYPE;
                break;
            case Types.FLOAT:
                newType = DoubleCell.TYPE;
                break;
            case Types.DOUBLE:
                newType = DoubleCell.TYPE;
                break;
            default:
                newType = StringCell.TYPE;
            }
            cspecs[i] = new DataColumnSpecCreator(name, newType).createSpec();
        }
        return new DataTableSpec(cspecs);
    }

    /**
     * Secret for the password de- and encyption.
     */
    private static SecretKey mSECRETKEY;
    static {
        try {
            mSECRETKEY = KeyGenerator.getInstance("DES").generateKey();
        } catch (NoSuchAlgorithmException e) {
            mSECRETKEY = null;
        }
    }

    /**
     * Enrypts password.
     * 
     * @param password Char array.
     * @return The password encrypt.
     * @throws Exception If something goes wrong.
     */
    static String encrypt(final char[] password) throws Exception {
        // Create Cipher
        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.ENCRYPT_MODE, mSECRETKEY);
        byte[] ciphertext = desCipher.doFinal(new String(password).getBytes());
        return new BASE64Encoder().encode(ciphertext);
    }

    /**
     * Decrypts password.
     * 
     * @param password The password to decrypt.
     * @return The decrypted password.
     * @throws Exception If something goes wrong.
     */
    static String decrypt(final String password) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, mSECRETKEY);
        // perform the decryption
        byte[] pw = new BASE64Decoder().decodeBuffer(password);
        byte[] decryptedText = cipher.doFinal(pw);
        return new String(decryptedText);
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
     * @throws SQLException If result next() failed.
     */
    DBRowIterator(final DataTableSpec spec, final ResultSet result)
            throws SQLException {
        m_spec = spec;
        m_result = result;
        m_end = !m_result.next();
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
        try {

            DataCell[] cells = new DataCell[m_spec.getNumColumns()];
            for (int i = 0; i < cells.length; i++) {
                DataType type = m_spec.getColumnSpec(i).getType();
                if (type.isCompatible(IntValue.class)) {
                    int integer = m_result.getInt(i + 1);
                    if (m_result.wasNull()) {
                        cells[i] = DataType.getMissingCell();
                    } else {
                        cells[i] = new IntCell(integer);
                    }
                } else if (type.isCompatible(DoubleValue.class)) {
                    double dbl = m_result.getDouble(i + 1);
                    if (m_result.wasNull()) {
                        cells[i] = DataType.getMissingCell();
                    } else {
                        cells[i] = new DoubleCell(dbl);
                    }
                } else {
                    String s = m_result.getString(i + 1);
                    if (s == null || m_result.wasNull()) {
                        cells[i] = DataType.getMissingCell();
                    } else {
                        cells[i] = new StringCell(s);
                    }
                }
            }
            StringCell id = new StringCell("Row_" + m_result.getRow());
            m_end = !m_result.next();
            return new DefaultRow(id, cells);
        } catch (SQLException e) {
            m_end = true;
            LOGGER.error("SQL Exception: ", e);
            return null;
        }
    }

}

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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;

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
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.util.FileUtil;

/**
 * Creates a connection to read from database.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DatabaseReaderConnection {

    /** Separator used to decided which SQL statements should be execute
     * line-by-line; the semicolon is not part of the executed query. */
    public static final String SQL_QUERY_SEPARATOR = ";\n";

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DatabaseReaderConnection.class);

    private DataTableSpec m_spec;

    private DatabaseQueryConnectionSettings m_conn;

    private Statement m_stmt;

    /**
     * Creates a empty handle for a new connection.
     * @param conn a database connection object
     */
    public DatabaseReaderConnection(
            final DatabaseQueryConnectionSettings conn) {
        setDBQueryConnection(conn);
    }

    /**
     * Sets a new connection object.
     * @param conn the connection
     */
    public void setDBQueryConnection(
            final DatabaseQueryConnectionSettings conn) {
        m_conn = conn;
        m_spec = null;
        if (m_stmt != null) {
            try {
                m_stmt.close();
            } catch (SQLException sqle) {
                LOGGER.debug("Error while closing SQL statement, reason "
                        + sqle.getMessage());
            }
        }
        m_stmt = null;
    }

    /**
     * @return connection settings object
     */
    public DatabaseQueryConnectionSettings getQueryConnection() {
        return m_conn;
    }

    /**
     * Returns the database meta data on the connection.
     * @param cp CredentialsProvider to receive user/password from
     * @return DatabaseMetaData on this connection
     * @throws SQLException if the connection to the database or the statement
     *         could not be created
     */
    public final DatabaseMetaData getDatabaseMetaData(
            final CredentialsProvider cp) throws SQLException {
        try {
            final Connection conn = m_conn.createConnection(cp);
            synchronized (m_conn.syncConnection(conn)) {
                return conn.getMetaData();
            }
        } catch (SQLException sql) {
            throw sql;
        } catch (Throwable t) {
            throw new SQLException(t);
        }
    }

    /**
     * Inits the statement and - if necessary - the database connection.
     * @throws SQLException if the connection to the database or the statement
     *         could not be created
     */
    private void initStatement(final CredentialsProvider cp)
            throws SQLException {
        if (m_stmt == null) {
            final Connection conn;
            try {
                conn = m_conn.createConnection(cp);
            } catch (SQLException sql) {
                if (m_stmt != null) {
                    try {
                        m_stmt.close();
                    } catch (SQLException e) {
                        LOGGER.debug(e);
                    }
                    m_stmt = null;
                }
                throw sql;
            } catch (Throwable t) {
                throw new SQLException(t);
            }
            synchronized (m_conn.syncConnection(conn)) {
                m_stmt = conn.createStatement();
            }
        }
    }

    /**
     * Returns a data table spec that reflects the meta data form the database
     * result set.
     * @param cp {@link CredentialsProvider} providing user/password
     * @return data table spec
     * @throws SQLException if the connection to the database could not be
     *         established
     */
    public DataTableSpec getDataTableSpec(final CredentialsProvider cp)
            throws SQLException {
        if (m_spec == null || m_stmt == null) {
            final Connection conn;
            try {
                conn = m_conn.createConnection(cp);
            } catch (SQLException sql) {
                if (m_stmt != null) {
                    try {
                        m_stmt.close();
                    } catch (SQLException e) {
                        LOGGER.debug(e);
                    }
                    m_stmt = null;
                }
                throw sql;
            } catch (Throwable t) {
                throw new SQLException(t);
            }
            synchronized (m_conn.syncConnection(conn)) {
                final String[] oQueries =  m_conn.getQuery().split(
                        SQL_QUERY_SEPARATOR);
                final int selectIndex = oQueries.length - 1;
                final int hashAlias = System.identityHashCode(this);
                // replace SELECT (last) query with wrapped statement
                /* Fixed Bug 2874. For sqlite the data must always be
                 * fetched as the column type string is returned
                 * for all columns when fetching only meta data. */
                if (!m_conn.getDriver().startsWith("org.sqlite")) {
                    oQueries[selectIndex] = "SELECT * FROM ("
                        + oQueries[selectIndex] + ") "
                        + "table_" + hashAlias + " WHERE 1 = 0";
                }
                ResultSet result = null;
                try {
                    // if only one SQL statement is being executed
                    if (oQueries.length == 1) {
                        // try to see if prepared statements are supported
                        LOGGER.debug("Executing SQL statement \""
                                + oQueries[selectIndex] + "\"");
                        m_stmt = conn.prepareStatement(oQueries[selectIndex]);
                        ((PreparedStatement) m_stmt).execute();
                        m_spec = createTableSpec(
                                ((PreparedStatement) m_stmt).getMetaData());
                    } else {
                        // otherwise use standard statement
                        m_stmt = conn.createStatement();
                        LOGGER.debug("Executing SQL statement(s) \""
                                + Arrays.toString(oQueries) + "\"");
                        for (int i = 0; i < oQueries.length - 1; i++) {
                            m_stmt.execute(oQueries[i]);
                        }
                        result = m_stmt.executeQuery(oQueries[selectIndex]);
                        m_spec = createTableSpec(result.getMetaData());
                    }
                } catch (Exception e) {
                    // otherwise use standard statement
                    LOGGER.warn("PreparedStatment not support by database: "
                            + e.getMessage(), e);
                    m_stmt = conn.createStatement();
                    // if more than one SQL statement is being executed
                    if (oQueries.length > 1) {
                        LOGGER.debug("Executing SQL statement(s) \""
                                + Arrays.toString(oQueries) + "\"");
                        for (int i = 0; i < oQueries.length - 1; i++) {
                            m_stmt.execute(oQueries[i]);
                        }
                    } else {
                        LOGGER.debug("Executing SQL statement \""
                                + oQueries[selectIndex] + "\"");
                    }
                    result = m_stmt.executeQuery(oQueries[selectIndex]);
                    m_spec = createTableSpec(result.getMetaData());
                } finally {
                    if (result != null) {
                        result.close();
                    }
                    // ensure we have a non-prepared statement to access data
                    if (m_stmt != null && m_stmt instanceof PreparedStatement) {
                        m_stmt.close();
                        m_stmt = conn.createStatement();
                    }
                }
            }
        }
        return m_spec;
    }

    /**
     * Read data from database.
     * @param exec used for progress info
     * @param cp {@link CredentialsProvider} providing user/password
     * @return buffered data table read from database
     * @throws CanceledExecutionException if canceled in between
     * @throws SQLException if the connection could not be opened
     */
    public BufferedDataTable createTable(final ExecutionContext exec,
            final CredentialsProvider cp)
            throws CanceledExecutionException, SQLException {
        initStatement(cp);
        Object sync = m_conn.syncConnection(m_stmt.getConnection());
        synchronized (sync) {
            try {
                if (DatabaseConnectionSettings.FETCH_SIZE != null) {
                    // fix 2741: postgresql databases ignore fetchsize when
                    // AUTOCOMMIT on; setting it to false
                    if (m_stmt.getClass().getCanonicalName().startsWith(
                            "org.postgresql")) {
                        m_stmt.getConnection().setAutoCommit(false);
                    }
                    m_stmt.setFetchSize(DatabaseConnectionSettings.FETCH_SIZE);
                } else {
                    // fix 2040: mySQL databases read everything into one, big
                    // ResultSet leading to an heap space error
                    // Integer.MIN_VALUE is an indicator in order to enable
                    // streaming results
                    if (m_stmt.getClass().getCanonicalName().startsWith(
                            "com.mysql")) {
                        m_stmt.setFetchSize(Integer.MIN_VALUE);
                        LOGGER.info("Database fetchsize for mySQL database set"
                                + " to \"" + Integer.MIN_VALUE + "\".");
                    }
                }
                final String[] oQueries = m_conn.getQuery().split(
                        SQL_QUERY_SEPARATOR);
                LOGGER.debug("Executing SQL statement(s) \""
                        + Arrays.toString(oQueries) + "\"");
                for (int i = 0; i < oQueries.length - 1; i++) {
                    m_stmt.execute(oQueries[i]);
                }
                final String selectQuery = oQueries[oQueries.length - 1];
                final ResultSet result = m_stmt.executeQuery(selectQuery);
                m_spec = createTableSpec(result.getMetaData());
                return exec.createBufferedDataTable(new DataTable() {
                    /** {@inheritDoc} */
                    @Override
                    public DataTableSpec getDataTableSpec() {
                        return m_spec;
                    }
                    /** {@inheritDoc} */
                    @Override
                    public RowIterator iterator() {
                        return new DBRowIterator(result);
                    }

                }, exec);
            } finally {
                if (m_stmt != null) {
                    m_stmt.close();
                    m_stmt = null;
                }
            }
        }
    }

    /** Called from the database port to read the first n-number of rows.
     * @param cachedNoRows number of rows cached for data preview
     * @param cp {@link CredentialsProvider} providing user/password
     * @return buffered data table read from database
     * @throws SQLException if the connection could not be opened
     */
    DataTable createTable(final int cachedNoRows, final CredentialsProvider cp)
            throws SQLException {
        initStatement(cp);
        synchronized (m_conn.syncConnection(m_stmt.getConnection())) {
            try {
                final String[] oQueries = m_conn.getQuery().split(
                        SQL_QUERY_SEPARATOR);
                if (cachedNoRows < 0) {
                    if (DatabaseConnectionSettings.FETCH_SIZE != null) {
                        // fix 2741: postgresql databases ignore fetchsize when
                        // AUTOCOMMIT on; setting it to false
                        if (m_stmt.getClass().getCanonicalName().startsWith(
                                "org.postgresql")) {
                            m_stmt.getConnection().setAutoCommit(false);
                        }
                        m_stmt.setFetchSize(
                                DatabaseConnectionSettings.FETCH_SIZE);
                    } else {
                        // fix 2040: mySQL databases read everything into one
                        // big ResultSet leading to an heap space error
                        // Integer.MIN_VALUE is an indicator in order to enable
                        // streaming results
                        if (m_stmt.getClass().getCanonicalName().startsWith(
                                "com.mysql")) {
                            m_stmt.setFetchSize(Integer.MIN_VALUE);
                            LOGGER.info("Database fetchsize for mySQL database "
                                    + "set to \"" + Integer.MIN_VALUE + "\".");
                        }
                    }
                } else {
                    final int hashAlias = System.identityHashCode(this);
                    final int selectIdx = oQueries.length - 1;
                    // replace last element in statement(s) with wrapped SQL
                    oQueries[selectIdx] = "SELECT * FROM "
                            + "(" + oQueries[selectIdx] + ") "
                            + "table_" + hashAlias;
                    try {
                        // bugfix 2925: may fail, e.g. on sqlite
                        m_stmt.setMaxRows(cachedNoRows);
                    } catch (SQLException sqle) {
                        LOGGER.warn("Can't set max rows on statement, reason: "
                                + sqle.getMessage());
                    }
                }
                LOGGER.debug("Executing SQL statement(s) \""
                        + Arrays.toString(oQueries) + "\"");
                // note: execute last, SELECT statement using Statement#execute
                for (int i = 0; i < oQueries.length; i++) {
                    m_stmt.execute(oQueries[i]);
                }
                final ResultSet result = m_stmt.getResultSet();
                m_spec = createTableSpec(result.getMetaData());
                DBRowIterator it = new DBRowIterator(result);
                DataContainer buf = new DataContainer(m_spec);
                while (it.hasNext()) {
                    buf.addRowToTable(it.next());
                }
                buf.close();
                return buf.getTable();
            } finally {
                if (m_stmt != null) {
                    m_stmt.close();
                    m_stmt = null;
                }
            }
        }
    }

    private DataTableSpec createTableSpec(final ResultSetMetaData meta)
            throws SQLException {
        int cols = meta.getColumnCount();
        if (cols == 0) {
            return new DataTableSpec("database");
        }
        DataTableSpec spec = null;
        for (int i = 0; i < cols; i++) {
            int dbIdx = i + 1;
            String name = meta.getColumnName(dbIdx);
            int type = meta.getColumnType(dbIdx);
            DataType newType;
            switch (type) {
                // all types that can be interpreted as integer
                case Types.TINYINT:
                case Types.SMALLINT:
                case Types.INTEGER:
                case Types.BIT:
                case Types.BOOLEAN:
                    newType = IntCell.TYPE;
                    break;
                // all types that can be interpreted as double
                case Types.FLOAT:
                case Types.DOUBLE:
                case Types.NUMERIC:
                case Types.DECIMAL:
                case Types.REAL:
                case Types.BIGINT:
                    newType = DoubleCell.TYPE;
                    break;
                case Types.TIME:
                case Types.DATE:
                case Types.TIMESTAMP:
                    newType = DateAndTimeCell.TYPE;
                    break;
                default:
                    newType = StringCell.TYPE;
            }
            if (spec == null) {
                spec = new DataTableSpec("database",
                        new DataColumnSpecCreator(name, newType).createSpec());
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

        private boolean m_hasExceptionReported = false;

        private int m_rowCounter = 0;

        /** FIXME: Some database (such as sqlite do NOT support) methods like
         * #getAsciiStream nor #getBinaryStream and will fail with an
         * SQLException. To prevent this exception for each ResultSet's value,
         * this flag for each column indicated that this exception has been
         * thrown and we directly can access the value via #getString.
         */
        private final boolean[] m_streamException;

        /**
         * Creates new iterator.
         * @param result result set to iterate
         */
        DBRowIterator(final ResultSet result) {
            m_result = result;
            m_streamException = new boolean[m_spec.getNumColumns()];
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
                    LOGGER.error("SQL Exception while closing result set: ", e);
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
                int dbType = Types.NULL;
                final DataCell cell;
                try {
                    dbType = m_result.getMetaData().getColumnType(i + 1);
                    if (type.isCompatible(IntValue.class)) {
                        switch (dbType) {
                            // all types that can be interpreted as integer
                            case Types.TINYINT:
                                cell = readByte(i);
                                break;
                            case Types.SMALLINT:
                                cell = readShort(i);
                                break;
                            case Types.INTEGER:
                                cell = readInt(i);
                                break;
                            case Types.BIT:
                            case Types.BOOLEAN:
                                cell = readBoolean(i);
                                break;
                            default: cell = readInt(i);
                        }
                    } else if (type.isCompatible(DoubleValue.class)) {
                        switch (dbType) {
                            // all types that can be interpreted as double
                            case Types.REAL:
                                cell = readFloat(i);
                                break;
                            case Types.FLOAT:
                            case Types.DOUBLE:
                                cell = readDouble(i);
                                break;
                            case Types.DECIMAL:
                            case Types.NUMERIC:
                                cell = readBigDecimal(i);
                                break;
                            case Types.BIGINT:
                                cell = readLong(i);
                                break;
                            default: cell = readDouble(i);
                        }
                    } else if (type.isCompatible(DateAndTimeValue.class)) {
                        switch (dbType) {
                            case Types.DATE:
                                cell = readDate(i); break;
                            case Types.TIME:
                                cell = readTime(i); break;
                            case Types.TIMESTAMP:
                                cell = readTimestamp(i); break;
                            default: cell = readString(i);
                        }
                    } else {
                        switch (dbType) {
                            case Types.CLOB:
                                cell = readClob(i); break;
                            case Types.BLOB:
                                cell = readBlob(i); break;
                            case Types.ARRAY:
                                cell = readArray(i); break;
                            case Types.CHAR:
                            case Types.VARCHAR:
                                cell = readString(i); break;
                            case Types.LONGVARCHAR:
                                cell = readAsciiStream(i); break;
                            case Types.BINARY:
                            case Types.VARBINARY:
                                cell = readBytes(i); break;
                            case Types.LONGVARBINARY:
                                cell = readBinaryStream(i); break;
                            case Types.REF:
                                cell = readRef(i); break;
                            case Types.NCHAR:
                            case Types.NVARCHAR:
                            case Types.LONGNVARCHAR:
                                cell = readNString(i); break;
                            case Types.NCLOB:
                                cell = readNClob(i); break;
                            case Types.DATALINK:
                                cell = readURL(i); break;
                            case Types.STRUCT:
                            case Types.JAVA_OBJECT:
                                cell = readObject(i); break;
                            default:
                                cell = readObject(i); break;

                        }
                    }
                    // finally set the new cell into the array of cells
                    cells[i] = cell;
                } catch (SQLException sqle) {
                    handlerException(
                            "SQL Exception reading Object of type \""
                            + dbType + "\": ", sqle);
                } catch (IOException ioe) {
                    handlerException(
                            "I/O Exception reading Object of type \""
                            + dbType + "\": ", ioe);
                }
            }
            int rowId = m_rowCounter;
            try {
                rowId = m_result.getRow();
                // Bug 2729: ResultSet#getRow return 0 if there is no row id
                if (rowId <= 0) {
                    // use row counter
                    rowId = m_rowCounter;
                }
            } catch (SQLException sqle) {
                 // ignored: use m_rowCounter
            }
            m_rowCounter++;
            return new DefaultRow(RowKey.createRowKey(rowId), cells);
        }

        private DataCell readClob(final int i)
                throws IOException, SQLException {
            Clob clob = m_result.getClob(i + 1);
            if (wasNull() || clob == null) {
                return DataType.getMissingCell();
            } else {
                Reader reader = clob.getCharacterStream();
                StringWriter writer = new StringWriter();
                FileUtil.copy(reader, writer);
                reader.close();
                writer.close();
                return new StringCell(writer.toString());
            }
        }

        private DataCell readNClob(final int i)
                throws IOException, SQLException {
            NClob nclob = m_result.getNClob(i + 1);
            if (wasNull() || nclob == null) {
                return DataType.getMissingCell();
            } else {
                Reader reader = nclob.getCharacterStream();
                StringWriter writer = new StringWriter();
                FileUtil.copy(reader, writer);
                reader.close();
                writer.close();
                return new StringCell(writer.toString());
            }
        }

        private DataCell readBlob(final int i)
                throws IOException, SQLException {
           Blob blob = m_result.getBlob(i + 1);
           if (wasNull() || blob == null) {
               return DataType.getMissingCell();
           } else {
               InputStreamReader reader =
                   // TODO: using default encoding
                   new InputStreamReader(blob.getBinaryStream());
               StringWriter writer = new StringWriter();
               FileUtil.copy(reader, writer);
               reader.close();
               writer.close();
               return new StringCell(writer.toString());
           }
        }

        private DataCell readAsciiStream(final int i)
                throws IOException, SQLException {
            if (m_streamException[i]) {
                return readString(i);
            }
            try {
                InputStream is = m_result.getAsciiStream(i + 1);
                if (wasNull() || is == null) {
                    return DataType.getMissingCell();
                } else {
                    InputStreamReader reader =
                    // TODO: using default encoding
                            new InputStreamReader(is);
                    StringWriter writer = new StringWriter();
                    FileUtil.copy(reader, writer);
                    reader.close();
                    writer.close();
                    return new StringCell(writer.toString());
                }
            } catch (SQLException sql) {
                m_streamException[i] = true;
                handlerException("Can't read from ASCII stream, "
                        + "trying to read string... ", sql);
                return readString(i);
            }
        }

        private DataCell readByte(final int i) throws SQLException {
            byte b = m_result.getByte(i + 1);
            if (wasNull()) {
                return DataType.getMissingCell();
            } else {
                return new IntCell(b);
            }
        }

        private DataCell readShort(final int i) throws SQLException {
            short s = m_result.getShort(i + 1);
            if (wasNull()) {
                return DataType.getMissingCell();
            } else {
                return new IntCell(s);
            }
        }

        private DataCell readInt(final int i) throws SQLException {
            int integer = m_result.getInt(i + 1);
            if (wasNull()) {
                return DataType.getMissingCell();
            } else {
                return new IntCell(integer);
            }
        }

        private DataCell readBoolean(final int i) throws SQLException {
            boolean b = m_result.getBoolean(i + 1);
            if (wasNull()) {
                return DataType.getMissingCell();
            } else {
                return new IntCell(b ? 1 : 0);
            }
        }

        private DataCell readDouble(final int i) throws SQLException {
            double d = m_result.getDouble(i + 1);
            if (wasNull()) {
                return DataType.getMissingCell();
            } else {
                return new DoubleCell(d);
            }
        }

        private DataCell readFloat(final int i) throws SQLException {
            float f = m_result.getFloat(i + 1);
            if (wasNull()) {
                return DataType.getMissingCell();
            } else {
                return new DoubleCell(f);
            }
        }

        private DataCell readLong(final int i) throws SQLException {
            long l = m_result.getLong(i + 1);
            if (wasNull()) {
                return DataType.getMissingCell();
            } else {
                return new DoubleCell(l);
            }
        }

        private DataCell readString(final int i) throws SQLException {
            String s = m_result.getString(i + 1);
            if (wasNull() || s == null) {
                return DataType.getMissingCell();
            } else {
                return new StringCell(s);
            }
        }

        private DataCell readBytes(final int i) throws SQLException {
            byte[] bytes = m_result.getBytes(i + 1);
            if (wasNull() || bytes == null) {
                return DataType.getMissingCell();
            } else {
                return new StringCell(new String(bytes));
            }
        }

        private DataCell readBigDecimal(final int i) throws SQLException {
            BigDecimal bc = m_result.getBigDecimal(i + 1);
            if (wasNull() || bc == null) {
                return DataType.getMissingCell();
            } else {
                return new DoubleCell(bc.doubleValue());
            }

        }

        private DataCell readBinaryStream(final int i)
                throws IOException, SQLException {
            if (m_streamException[i]) {
                return readString(i);
            }
            try {
                InputStream is = m_result.getBinaryStream(i + 1);
                if (wasNull() || is == null) {
                    return DataType.getMissingCell();
                } else {
                    InputStreamReader reader =
                    // TODO: using default encoding
                            new InputStreamReader(is);
                    StringWriter writer = new StringWriter();
                    FileUtil.copy(reader, writer);
                    reader.close();
                    writer.close();
                    return new StringCell(writer.toString());
                }
            } catch (SQLException sql) {
                m_streamException[i] = true;
                handlerException("Can't read from binary stream, "
                        + "trying to read string... ", sql);
                return readString(i);
            }
        }

        private DataCell readNString(final int i) throws SQLException {
            String str = m_result.getNString(i + 1);
            if (wasNull() || str == null) {
                return DataType.getMissingCell();
            } else {
                return new StringCell(str);
            }
        }

        private DataCell readDate(final int i) throws SQLException {
            Date date = m_result.getDate(i + 1);
            if (wasNull() || date == null) {
                return DataType.getMissingCell();
            } else {
                return new DateAndTimeCell(date.getTime(), true, false, false);
            }
        }

        private DataCell readTime(final int i) throws SQLException {
            Time time = m_result.getTime(i + 1);
            if (wasNull() || time == null) {
                return DataType.getMissingCell();
            } else {
                return new DateAndTimeCell(time.getTime(), false, true, true);
            }
        }

        private DataCell readTimestamp(final int i) throws SQLException {
            Timestamp timestamp = m_result.getTimestamp(i + 1);
            if (wasNull() || timestamp == null) {
                return DataType.getMissingCell();
            } else {
                return new DateAndTimeCell(
                        timestamp.getTime(), true, true, true);
            }
        }

        private DataCell readArray(final int i) throws SQLException {
            Array array = m_result.getArray(i + 1);
            if (wasNull() || array == null) {
                return DataType.getMissingCell();
            } else {
                return new StringCell(array.getArray().toString());
            }
        }

        private DataCell readRef(final int i) throws SQLException {
            Ref ref = m_result.getRef(i + 1);
            if (wasNull() || ref == null) {
                return DataType.getMissingCell();
            } else {
                return new StringCell(ref.getObject().toString());
            }
        }

        private DataCell readURL(final int i) throws SQLException {
            URL url = m_result.getURL(i + 1);
            if (url == null || wasNull()) {
                return DataType.getMissingCell();
            } else {
                return new StringCell(url.toString());
            }
        }

        private DataCell readObject(final int i) throws SQLException {
            Object o = m_result.getObject(i + 1);
            if (o == null || wasNull()) {
                return DataType.getMissingCell();
            } else {
                return new StringCell(o.toString());
            }
        }

        private boolean wasNull() {
            try {
                return m_result.wasNull();
            } catch (SQLException sqle) {
                handlerException("SQL Exception: ", sqle);
                return true;
            }
        }

        private void handlerException(final String msg, final Exception e) {
            if (m_hasExceptionReported) {
                LOGGER.debug(msg + e.getMessage(), e);
            } else {
                m_hasExceptionReported = true;
                LOGGER.error(msg + e.getMessage()
                        + " - all further errors are suppressed "
                        + "and reported on debug level only", e);
            }
        }
    }
}

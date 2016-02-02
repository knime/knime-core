/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.core.node.port.database.reader;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Creates a connection to read from database.
 *
 * @author Tobias Koetter, KNIME.com
 * @since 3.1
 */
public class DBReaderImpl implements DBReader {

    static final NodeLogger LOGGER =
            NodeLogger.getLogger(DBReaderImpl.class);

    DataTableSpec m_spec;

    DatabaseQueryConnectionSettings m_conn;

    /**
     * Creates a empty handle for a new connection.
     * @param conn a database connection object
     */
    public DBReaderImpl(final DatabaseQueryConnectionSettings conn) {
        if (conn == null) {
            throw new NullPointerException("conn must not be null");
        }
        m_conn = conn;
        m_spec = null;
    }

    /**
     * @return connection settings object
     */
    @Override
    public DatabaseQueryConnectionSettings getQueryConnection() {
        return m_conn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateQuery(final String query) {
        m_conn.setQuery(query);
    }

    /**
     * Returns the database meta data on the connection.
     * @param cp CredentialsProvider to receive user/password from
     * @return DatabaseMetaData on this connection
     * @throws SQLException if the connection to the database or the statement
     *         could not be created
     */
    @Override
    public final DatabaseMetaData getDatabaseMetaData(
            final CredentialsProvider cp) throws SQLException {
        try {
            final Connection conn = m_conn.createConnection(cp);
            synchronized (m_conn.syncConnection(conn)) {
                return conn.getMetaData();
            }
        } catch (SQLException sql) {
            throw sql;
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidSettingsException
                | IOException ex) {
            throw new SQLException(ex);
        }
    }

    /**
     * Inits the statement and - if necessary - the database connection.
     * @throws SQLException if the connection to the database or the statement could not be created
     */
    private Statement initStatement(final CredentialsProvider cp, final Connection conn) throws SQLException {
        synchronized (m_conn.syncConnection(conn)) {
            return conn.createStatement();
        }
    }

    private Connection initConnection(final CredentialsProvider cp) throws SQLException {
        try {
            return m_conn.createConnection(cp);
        } catch (Exception e) {
            throw new SQLException(e);
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
    @Override
    public DataTableSpec getDataTableSpec(final CredentialsProvider cp)
            throws SQLException {
        if (m_spec != null) {
            return m_spec;
        }
        // retrieve connection
        final Connection conn = initConnection(cp);
        synchronized (m_conn.syncConnection(conn)) {
            final String[] oQueries =  m_conn.getQuery().split(SQL_QUERY_SEPARATOR);
            final int selectIndex = oQueries.length - 1;
            if (oQueries[selectIndex].trim().endsWith(";")) {
                oQueries[selectIndex] = oQueries[selectIndex].trim();
                oQueries[selectIndex] = oQueries[selectIndex].substring(0, oQueries[selectIndex].length() - 1);
            }

            oQueries[selectIndex] =
                m_conn.getUtility().getStatementManipulator().forMetadataOnly(oQueries[selectIndex]);
            ResultSet result = null;
            final Statement stmt = initStatement(cp, conn);
            try {
                // execute all except the last query
                for (int i = 0; i < oQueries.length - 1; i++) {
                    LOGGER.debug("Executing SQL statement as execute: " + oQueries[i]);
                    stmt.execute(oQueries[i]);
                }
                LOGGER.debug("Executing SQL statement as executeQuery: " + oQueries[selectIndex]);
                result = stmt.executeQuery(oQueries[selectIndex]);
                LOGGER.debug("Reading meta data from database ResultSet...");
                m_spec = createTableSpec(result.getMetaData());
            } finally {
                if (result != null) {
                    result.close();
                }
                if (stmt != null) {
                    // Bug 4071: statement(s) not closed when fetching meta data
                    stmt.close();
                }
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
            }
        }
        return m_spec;
    }

    // internal execution context used to create blob/binary objects
    BinaryObjectCellFactory m_blobFactory = null;

    /**
     * Read data from database.
     * @param exec used for progress info
     * @param cp {@link CredentialsProvider} providing user/password
     * @param useDbRowId <code>true</code> if the row id returned by the database should be used to generate the
     * KNIME row id
     * @return buffered data table read from database
     * @throws CanceledExecutionException if canceled in between
     * @throws SQLException if the connection could not be opened
     * @since 2.12
     */
    @Override
    public BufferedDataTable createTable(final ExecutionContext exec, final CredentialsProvider cp,
        final boolean useDbRowId) throws CanceledExecutionException, SQLException {
        final Connection conn = initConnection(cp);
        synchronized (m_conn.syncConnection(conn)) {
            try (DBRowIterator ric = createRowIteratorConnection(conn, exec, cp, useDbRowId)) {
                return exec.createBufferedDataTable(new DataTable() {
                    /** {@inheritDoc} */
                    @Override
                    public DataTableSpec getDataTableSpec() {
                        return ric.getDataTableSpec();
                    }

                    /** {@inheritDoc} */
                    @Override
                    public RowIterator iterator() {
                        return ric.iterator();
                    }

                }, exec);
            }
        }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public DataTable getTable(final ExecutionMonitor exec, final CredentialsProvider cp, final boolean useDbRowId, final int cachedNoRows)
        throws CanceledExecutionException, SQLException {
        return createTable(useDbRowId, cachedNoRows, cp);
    }

    /**
     * Read data from database using a {@link RowIterator}.
     * @param conn
     * @param exec used for progress info
     * @param cp {@link CredentialsProvider} providing user/password
     * @param useDbRowId <code>true</code> if the row id returned by the database should be used to generate the
     * KNIME row id
     * @return an object that represents the open database connection. The individual entries are accessible by means of a {@link RowIterator}.
     * @throws SQLException if the connection could not be opened
     */
    private DBRowIterator createRowIteratorConnection(final Connection conn, final ExecutionContext exec,
        final CredentialsProvider cp, final boolean useDbRowId) throws SQLException {
        if (m_blobFactory == null) {
            m_blobFactory = new BinaryObjectCellFactory(exec);
        }

        exec.setMessage("Start reading rows from database...");
        // remember auto-commit flag
        final Statement stmt = initStatement(cp, conn);
        int fetchsize =
            (DatabaseConnectionSettings.FETCH_SIZE != null) ? DatabaseConnectionSettings.FETCH_SIZE : -1;
        m_conn.getUtility().getStatementManipulator().setFetchSize(stmt, fetchsize);
        final String[] oQueries = m_conn.getQuery().split(SQL_QUERY_SEPARATOR);
        // execute all except the last query
        for (int i = 0; i < oQueries.length - 1; i++) {
            LOGGER.debug("Executing SQL statement as execute: " + oQueries[i]);
            stmt.execute(oQueries[i]);
        }
        final String selectQuery = oQueries[oQueries.length - 1];
        LOGGER.debug("Executing SQL statement as executeQuery: " + selectQuery);
        final ResultSet result = stmt.executeQuery(selectQuery);
        LOGGER.debug("Reading meta data from database ResultSet...");
        m_spec = createTableSpec(result.getMetaData());
        LOGGER.debug("Parsing database ResultSet...");
        final RowIterator iterator = createDBRowIterator(m_spec, m_conn, m_blobFactory, useDbRowId, result);
        return new RowIteratorConnection(conn, stmt, m_spec, iterator);
    }

    /** Called from the database port to read the first n-number of rows.
     *
     * @param useDbRowId <code>true</code> if the KNIME row id should based on the db row id
     * @param cachedNoRows number of rows cached for data preview
     * @param cp {@link CredentialsProvider} providing user/password
     * @return buffered data table read from database
     * @throws SQLException if the connection could not be opened
     */
    DataTable createTable(final boolean useDbRowId, final int cachedNoRows, final CredentialsProvider cp)
            throws SQLException {
        if (m_blobFactory == null) {
            m_blobFactory = new BinaryObjectCellFactory();
        }
        // retrieve connection
        final Connection conn = initConnection(cp);
        synchronized (m_conn.syncConnection(conn)) {
            // remember auto-commit flag
            final boolean autoCommit = conn.getAutoCommit();
            final Statement stmt = initStatement(cp, conn);
            try {
                final String[] oQueries = m_conn.getQuery().split(SQL_QUERY_SEPARATOR);
                if (cachedNoRows < 0) {
                    int fetchsize = (DatabaseConnectionSettings.FETCH_SIZE != null)
                        ? DatabaseConnectionSettings.FETCH_SIZE : -1;
                    m_conn.getUtility().getStatementManipulator().setFetchSize(stmt, fetchsize);
                } else {
                    final int hashAlias = System.identityHashCode(this);
                    final int selectIdx = oQueries.length - 1;
                    // replace last element in statement(s) with wrapped SQL
                    oQueries[selectIdx] = "SELECT * FROM (" + oQueries[selectIdx] + ") table_" + hashAlias;
                    try {
                        // bugfix 2925: may fail, e.g. on sqlite
                        stmt.setMaxRows(cachedNoRows);
                    } catch (SQLException ex) {
                        Throwable cause = ExceptionUtils.getRootCause(ex);
                        if (cause == null) {
                            cause = ex;
                        }

                        LOGGER.warn("Can't set max rows on statement, reason: " + cause.getMessage(), ex);
                    }
                }
                // execute all except the last query
                for (int i = 0; i < oQueries.length - 1; i++) {
                    LOGGER.debug("Executing SQL statement as execute: " + oQueries[i]);
                    stmt.execute(oQueries[i]);
                }
                final String lastQuery = oQueries[oQueries.length - 1];
                LOGGER.debug("Executing SQL statement as executeQuery: " + lastQuery);
                final ResultSet result = stmt.executeQuery(lastQuery);
                LOGGER.debug("Reading meta data from database ResultSet...");
                m_spec = createTableSpec(result.getMetaData());
                LOGGER.debug("Parsing database ResultSet...");
//                final DBRowIterator dbIt = createRowIterator(useDbRowId, result);
                final RowIterator it = createDBRowIterator(m_spec, m_conn, m_blobFactory, useDbRowId, result);
                DataContainer buf = new DataContainer(m_spec);
                while (it.hasNext()) {
                    buf.addRowToTable(it.next());
                }
                buf.close();
                return buf.getTable();
            } finally {
                if (stmt != null) {
                    if (!conn.getAutoCommit()) {
                        conn.commit();
                    }
                    DatabaseConnectionSettings.setAutoCommit(conn, autoCommit);
                    stmt.close();
                }
            }
        }
    }

    @SuppressWarnings("javadoc")
    protected RowIterator createDBRowIterator(final DataTableSpec spec, final DatabaseQueryConnectionSettings conn,
        final BinaryObjectCellFactory blobFactory, final boolean useDbRowId, final ResultSet result) throws SQLException {
        return new DBRowIteratorImpl(spec, conn, blobFactory, result, useDbRowId);
    }

    protected DataTableSpec createTableSpec(final ResultSetMetaData meta)
            throws SQLException {
        int cols = meta.getColumnCount();
        if (cols == 0) {
            return new DataTableSpec("database");
        }
        StatementManipulator manipulator = m_conn.getUtility().getStatementManipulator();
        DataTableSpec spec = null;
        for (int i = 0; i < cols; i++) {
            int dbIdx = i + 1;
            String name =  manipulator.unquoteColumn(meta.getColumnLabel(dbIdx));
            int type = meta.getColumnType(dbIdx);
            DataType newType = getKNIMEType(type, meta, dbIdx);
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
     * @param type
     * @param dbIdx
     * @param meta
     * @return the KNIME {@link DataType}
     * @throws SQLException
     */
    protected DataType getKNIMEType(final int type, final ResultSetMetaData meta, final int dbIdx) throws SQLException {
        DataType newType;
        switch (type) {
            // all types that can be interpreted as integer
            case Types.BIT:
            case Types.BOOLEAN:
                newType = BooleanCell.TYPE;
                break;
            // all types that can be interpreted as integer
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                newType = IntCell.TYPE;
                break;
             // all types that can be interpreted as long
            case Types.BIGINT:
                newType = LongCell.TYPE;
                break;
            // all types that can be interpreted as double
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
            case Types.REAL:
                newType = DoubleCell.TYPE;
                break;
            // all types that can be interpreted as data-and-time
            case Types.TIME:
            case Types.DATE:
            case Types.TIMESTAMP:
                newType = DateAndTimeCell.TYPE;
                break;
            // all types that can be interpreted as binary object
            case Types.BLOB:
            case Types.LONGVARBINARY:
            case Types.BINARY:
                newType = BinaryObjectDataCell.TYPE;
                break;
            case Types.ARRAY:
                //by default we convert the array elements to string and return a list cell with string cells
                newType = ListCell.getCollectionType(StringCell.TYPE);
                break;
            // fallback string
            default:
                newType = StringCell.TYPE;
        }
        return newType;
    }

}

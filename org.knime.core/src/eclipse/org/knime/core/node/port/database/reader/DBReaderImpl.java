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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseHelper;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.util.UniqueNameGenerator;

/**
 * Creates a connection to read from database.
 *
 * @author Tobias Koetter, KNIME.com
 * @since 3.1
 */
public class DBReaderImpl extends DatabaseHelper implements DBReader {

    static final NodeLogger LOGGER =
            NodeLogger.getLogger(DBReaderImpl.class);

    DataTableSpec m_spec;

    private BufferedDataContainer m_errorContainer;

    /**
     * Creates a empty handle for a new connection.
     * @param conn a database connection object
     */
    public DBReaderImpl(final DatabaseQueryConnectionSettings conn) {
        super(conn);
        m_spec = null;
    }

    /**
     * @return connection settings object
     */
    @Override
    public DatabaseQueryConnectionSettings getQueryConnection() {
        return (DatabaseQueryConnectionSettings) getDatabaseConnectionSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateQuery(final String query) {
        getQueryConnection().setQuery(query);
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
            final DatabaseQueryConnectionSettings dbConn = getQueryConnection();
            final Connection conn = dbConn.createConnection(cp);
            synchronized (dbConn.syncConnection(conn)) {
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
        synchronized (getQueryConnection().syncConnection(conn)) {
            return conn.createStatement();
        }
    }

    private Connection initConnection(final CredentialsProvider cp) throws SQLException {
        try {
            return getQueryConnection().createConnection(cp);
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
        final DatabaseQueryConnectionSettings dbConn = getQueryConnection();
        synchronized (dbConn.syncConnection(conn)) {
            final String[] oQueries =  dbConn.getQuery().split(SQL_QUERY_SEPARATOR);
            final int selectIndex = oQueries.length - 1;
            if (oQueries[selectIndex].trim().endsWith(";")) {
                oQueries[selectIndex] = oQueries[selectIndex].trim();
                oQueries[selectIndex] = oQueries[selectIndex].substring(0, oQueries[selectIndex].length() - 1);
            }

            oQueries[selectIndex] =
                dbConn.getUtility().getStatementManipulator().forMetadataOnly(oQueries[selectIndex]);
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
        synchronized (getQueryConnection().syncConnection(conn)) {
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
        final DatabaseQueryConnectionSettings dbConn = getQueryConnection();
        dbConn.getUtility().getStatementManipulator().setFetchSize(stmt, fetchsize);
        final String[] oQueries = dbConn.getQuery().split(SQL_QUERY_SEPARATOR);
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
        final RowIterator iterator = createDBRowIterator(m_spec, dbConn, m_blobFactory, useDbRowId, result);
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
        final DatabaseQueryConnectionSettings dbConn = getQueryConnection();
        synchronized (dbConn.syncConnection(conn)) {
            // remember auto-commit flag
            final boolean autoCommit = conn.getAutoCommit();
            final Statement stmt = initStatement(cp, conn);
            try {
                final String[] oQueries = dbConn.getQuery().split(SQL_QUERY_SEPARATOR);
                if (cachedNoRows < 0) {
                    int fetchsize = (DatabaseConnectionSettings.FETCH_SIZE != null)
                        ? DatabaseConnectionSettings.FETCH_SIZE : -1;
                    dbConn.getUtility().getStatementManipulator().setFetchSize(stmt, fetchsize);
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
                final RowIterator it = createDBRowIterator(m_spec, dbConn, m_blobFactory, useDbRowId, result);
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
        return createDBRowIterator(spec, conn, blobFactory, useDbRowId, result, 0);
    }

    /**
     * @since 3.2
     */
    @SuppressWarnings("javadoc")
    protected RowIterator createDBRowIterator(final DataTableSpec spec, final DatabaseConnectionSettings conn,
        final BinaryObjectCellFactory blobFactory, final boolean useDbRowId, final ResultSet result,
        final long startCounter) throws SQLException {
        return new DBRowIteratorImpl(spec, conn, blobFactory, result, useDbRowId, startCounter);
    }

    /**
     * @since 3.2
     */
    @SuppressWarnings("resource")
    @Override
    public BufferedDataTableRowOutput loopTable(final ExecutionContext exec, final CredentialsProvider cp,
        final RowInput data, final long rowCount, final boolean failIfException, final boolean appendInputColumns,
        final boolean includeEmptyResults, final boolean retainAllColumns, final String... columns) throws Exception {

        if (m_blobFactory == null) {
            m_blobFactory = new BinaryObjectCellFactory();
        }

        final DatabaseQueryConnectionSettings dbConn = getQueryConnection();
        final Connection conn = getQueryConnection().createConnection(cp);
        exec.setMessage("Waiting for free database connection...");
        synchronized (dbConn.syncConnection(conn)) {

            /* Get the selected timezone */
            final TimeZone timezone = dbConn.getTimeZone();

            /* Get the input table spec */
            final DataTableSpec inSpec = data.getDataTableSpec();

            /* Create PreparedStatement */
            final String query = dbConn.getQuery();
            LOGGER.debug("Executing SQL preparedStatement as execute: "
                    + query);

            /* Initialize the error table */
            final UniqueNameGenerator errorGenerator = new UniqueNameGenerator(inSpec);
            final DataColumnSpec errorColSpec = errorGenerator.newColumn(DEF_ERROR_COL_NAME, StringCell.TYPE);
            final DataTableSpec errorSpec = new DataTableSpec(inSpec, new DataTableSpec(errorColSpec));
            m_errorContainer = exec.createDataContainer(errorSpec);

            DataTableSpec dbSpec = new DataTableSpec();
            BufferedDataTableRowOutput output = null;

            exec.setMessage("Start reading rows from database...");
            try (final PreparedStatement stmt = conn.prepareStatement(query);) {

                long inDataCounter = 1;
                long rowIdCounter = 0;
                DataRow row;
                while((row = data.poll()) != null) {
                    exec.checkCanceled();
                    if(rowCount > 0) {
                        exec.setProgress(1.0 * inDataCounter / rowCount,
                            "Row " + "#" + inDataCounter + " of " + rowCount);
                    }else {
                        exec.setProgress("Writing Row " + "#" + inDataCounter);
                    }

                    final DataCell[] inCells = new DataCell[columns.length];
                    for(int i = 0; i < columns.length; i++) {
                        final int dbIdx = i + 1;
                        final int colIdx = inSpec.findColumnIndex(columns[i]);
                        final DataColumnSpec colSpec = inSpec.getColumnSpec(colIdx);
                        inCells[i] = row.getCell(colIdx);
                        fillStatement(stmt, dbIdx, colSpec, inCells[i], timezone, null);
                    }
                    try (final ResultSet result = stmt.executeQuery();) {

                        /* In the first iteration, create the out DataTableSpec and BufferedDataTableRowOutput */
                        if(output == null) {
                            dbSpec = createTableSpec(result.getMetaData());
                            if(appendInputColumns) {
                                // Create out DataTableSpec for input table
                                final DataTableSpec newInSpec;
                                if(retainAllColumns) {
                                    newInSpec = inSpec;
                                } else {
                                    final DataColumnSpec[] inColSpecs = new DataColumnSpec[columns.length];
                                    for(int i = 0; i < inColSpecs.length; i++) {
                                        inColSpecs[i] = inSpec.getColumnSpec(columns[i]);
                                    }

                                    newInSpec = new DataTableSpec(inColSpecs);
                                }

                                // Create DataTableSpec for database columns, rename if necessary
                                final UniqueNameGenerator generator = new UniqueNameGenerator(newInSpec);
                                final DataColumnSpec[] dbColSpecs = new DataColumnSpec[dbSpec.getNumColumns()];
                                for(int i = 0; i < dbColSpecs.length; i++) {
                                    final DataColumnSpec colSpec = dbSpec.getColumnSpec(i);
                                    dbColSpecs[i] = generator.newColumn(colSpec.getName(), colSpec.getType());
                                }

                                dbSpec = new DataTableSpec(dbColSpecs);
                                m_spec = new DataTableSpec(newInSpec, dbSpec);

                            } else {
                                m_spec = dbSpec;
                            }

                            output = new BufferedDataTableRowOutput(
                                exec.createDataContainer(m_spec));
                        }

                        /* Iterate over the result of the database query and put it into the output table*/
                        final RowIterator dbRowIterator = createDBRowIterator(
                            dbSpec, dbConn, m_blobFactory, false, result, rowIdCounter);
                        boolean hasDbRow = false;
                        while (dbRowIterator.hasNext()) {
                            hasDbRow = true;
                            final DataRow dbRow = dbRowIterator.next();

                            if(appendInputColumns) {
                                final DataRow inRow;
                                if(retainAllColumns) {
                                    inRow = new DefaultRow(dbRow.getKey(), row);
                                } else {
                                    inRow = new DefaultRow(dbRow.getKey(), inCells);
                                }
                                final JoinedRow joinedRow = new JoinedRow(inRow, dbRow);
                                output.push(joinedRow);
                            } else {
                                output.push(dbRow);
                            }

                            rowIdCounter++;
                        }

                        /* Append columns using MissingCell if no result is returned */
                        if(!hasDbRow && appendInputColumns && includeEmptyResults) {
                            final DataCell[] cells = new DataCell[dbSpec.getNumColumns()];
                            Arrays.fill(cells, DataType.getMissingCell());
                            final RowKey rowKey = RowKey.createRowKey(rowIdCounter);
                            final DataRow emptyDbRows = new DefaultRow(rowKey, cells);
                            final DataRow inRow;
                            if(retainAllColumns) {
                                inRow = new DefaultRow(rowKey, row);
                            } else {
                                inRow = new DefaultRow(rowKey, inCells);
                            }

                            final JoinedRow joinedRow = new JoinedRow(inRow, emptyDbRows);
                            output.push(joinedRow);
                            rowIdCounter++;
                        }

                        inDataCounter++;

                    } catch(SQLException ex) {
                        LOGGER.debug("SQLException: " + ex.getMessage());
                        if(!failIfException) {
                            if(output == null) {
                                throw new SQLException(ex);
                            }
                            final AppendedColumnRow appendedRow = new AppendedColumnRow(row, new StringCell(ex.getMessage()));
                            m_errorContainer.addRowToTable(appendedRow);
                        } else {
                            throw new SQLException(ex);
                        }
                    }
                }

            } finally {
                data.close();
                if(output == null) {
                    output = new BufferedDataTableRowOutput(
                        exec.createDataContainer(inSpec));
                }
                output.close();
                if(m_errorContainer != null) {
                    m_errorContainer.close();
                }
            }
            return output;
        }
    }

    /**
     * @return {@link BufferedDataTable} containing error message.
     * The method {@link #loopTable(ExecutionContext, CredentialsProvider, RowInput, long, boolean, boolean, boolean, boolean, String...) loopTable}
     * must be called before using this method, otherwise it will throw {@link IllegalStateException}.
     * @since 3.2
     */
    @Override
    public BufferedDataTable getErrorDataTable(){
        if(m_errorContainer != null) {
            return m_errorContainer.getTable();
        }
        throw new IllegalStateException("The method \"loopTable\" must be called before using this method.");
    }
}

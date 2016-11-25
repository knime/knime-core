/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   26.10.2015 (koetter): created
 */
package org.knime.core.node.port.database.reader;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 *
 * @author Tobias Koetter, KNIME.com
 * @since 3.1
 */
public interface DBReader {

    /** Default error column name used in the error table
     * @since 3.2*/
    String DEF_ERROR_COL_NAME = "Error";

    /** Separator used to decided which SQL statements should be execute
     * line-by-line; the semicolon is not part of the executed query.
     * We need the \n in addition to the semicolon to ensure that commands that contain a ;
     * are handled correctly!*/
    String SQL_QUERY_SEPARATOR = ";\n";

    /**
     * @return connection settings object
     */
    DatabaseQueryConnectionSettings getQueryConnection();

    /**
     * @param query the new query to execute
     */
    void updateQuery(String query);

    /**
     * Returns the database meta data on the connection.
     * @param cp CredentialsProvider to receive user/password from
     * @return DatabaseMetaData on this connection
     * @throws SQLException if the connection to the database or the statement
     *         could not be created
     */
    DatabaseMetaData getDatabaseMetaData(CredentialsProvider cp) throws SQLException;

    /**
     * Returns a data table spec that reflects the meta data form the database
     * result set.
     * @param cp {@link CredentialsProvider} providing user/password
     * @return data table spec
     * @throws SQLException if the connection to the database could not be
     *         established
     */
    DataTableSpec getDataTableSpec(CredentialsProvider cp) throws SQLException;

    /**
     * Read data from database.
     * @param exec used for progress info
     * @param cp {@link CredentialsProvider} providing user/password
     * @return buffered data table read from database
     * @throws CanceledExecutionException if canceled in between
     * @throws SQLException if the connection could not be opened
     */
    default BufferedDataTable createTable(final ExecutionContext exec, final CredentialsProvider cp)
        throws CanceledExecutionException, SQLException {
        return createTable(exec, cp, true);
    }

    /**
     * Read data from database.
     * @param exec used for progress info
     * @param cp {@link CredentialsProvider} providing user/password
     * @param useDbRowId <code>true</code> if the row id returned by the database should be used to generate the
     * KNIME row id
     * @return buffered data table read from database
     * @throws CanceledExecutionException if canceled in between
     * @throws SQLException if the connection could not be opened
     */
    default BufferedDataTable createTable(final ExecutionContext exec, final CredentialsProvider cp,
        final boolean useDbRowId)
        throws CanceledExecutionException, SQLException {
        return createTable(exec, cp, useDbRowId, -1);
    }

    /**
     * Read data from database.
     * @param exec used for progress info
     * @param cp {@link CredentialsProvider} providing user/password
     * @param useDbRowId <code>true</code> if the row id returned by the database should be used to generate the
     * KNIME row id
     * @param cachedNoRows number of rows cached for data preview or -1 to retrieve all rows
     * @return buffered data table read from database
     * @throws CanceledExecutionException if canceled in between
     * @throws SQLException if the connection could not be opened
     */
    default BufferedDataTable createTable(final ExecutionContext exec, final CredentialsProvider cp,
        final boolean useDbRowId, final int cachedNoRows)
        throws CanceledExecutionException, SQLException {
        final DataTable table = getTable(exec, cp, useDbRowId, cachedNoRows);
        return exec.createBufferedDataTable(table, exec);
    }

    /**
     * Read data from database.
     * @param exec used for progress info
     * @param cp {@link CredentialsProvider} providing user/password
     * @param useDbRowId <code>true</code> if the row id returned by the database should be used to generate the
     * KNIME row id
     * @param cachedNoRows number of rows cached for data preview
     * @return buffered data table read from database
     * @throws CanceledExecutionException if canceled in between
     * @throws SQLException if the connection could not be opened
     */
    DataTable getTable(ExecutionMonitor exec, CredentialsProvider cp, boolean useDbRowId, final int cachedNoRows)
        throws CanceledExecutionException, SQLException;

    /**
     * Loop table in database.
     * @param exec {@link ExecutionContext}
     * @param cp {@link CredentialsProvider}
     * @param data the data rows with the parameters to loop over
     * @param rowCount the number of input rows or -1 if unknown
     * @param failIfException flag that indicates if the method should thrown an exception on error
     * @param appendInputColumns <code>true</code> if parameter input columns should be appended 
     * @param includeEmptyResults <code>true</code> if a row should be added for parameters that do not return a result
     * @param retainAllColumns <code>true</code> all input columns should be retained
     * @param columns the parameter column names
     * @return {@link BufferedDataTableRowOutput} containing the loop result
     * @throws Exception
     * @since 3.2
     */
    BufferedDataTableRowOutput loopTable(final ExecutionContext exec, final CredentialsProvider cp, final RowInput data,
        final long rowCount, final boolean failIfException, final boolean appendInputColumns,
        final boolean includeEmptyResults, final boolean retainAllColumns,
        final String... columns) throws Exception;

    /**
     * @return {@link BufferedDataTable} containing error message. The column name with the error message
     * should be {@value #DEF_ERROR_COL_NAME}.
     * The method {@link #loopTable(ExecutionContext, CredentialsProvider, RowInput, long, boolean, boolean, boolean, boolean, String...) loopTable}
     * must be called before using this method, otherwise it will throw {@link IllegalStateException}.
     * @since 3.2
     */
    BufferedDataTable getErrorDataTable();

//    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//    STREAMING IS DISABLED UNTIL WE HAVE A PROPPER CONNECTION HANDLING SINCE MYSQL FOR EXAMPLE DOES NOT ALLOW
//    CONCURRENT READS WHICH HAPPEN IF WE USE THE DBRowIterator!!!
//    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//    /**
//     * Read data from database using a {@link RowIterator}.
//     * @param exec used for progress info
//     * @param cp {@link CredentialsProvider} providing user/password
//     * @param useDbRowId <code>true</code> if the row id returned by the database should be used to generate the
//     * KNIME row id
//     * @return an object that represents the open database connection. The individual entries are accessible by means of a {@link RowIterator}.
//     * @throws SQLException if the connection could not be opened
//     * @since 3.1
//     */
//    DBRowIterator createRowIteratorConnection(final ExecutionContext exec,
//        final CredentialsProvider cp, final boolean useDbRowId) throws SQLException;
}
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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.05.2014 (thor): created
 */
package org.knime.core.node.port.database;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.binning.BinningStatamentGenerator;
import org.knime.core.node.port.database.binning.CaseBinningStatementGenerator;
import org.knime.core.node.port.database.binning.DefaultBinningStatementGenerator;
import org.knime.core.node.port.database.pivoting.CasePivotStatementGenerator;
import org.knime.core.node.port.database.pivoting.DefaultPivotStatementGenerator;
import org.knime.core.node.port.database.pivoting.PivotColumnNameGenerator;
import org.knime.core.node.port.database.pivoting.PivotStatementGenerator;
import org.knime.core.util.Pair;

/**
 * This class lets you manipulate SQL statement by adding database-specific parts or changing statement parameters.
 * Subclasses may override some methods and change the manipulation. All implementations must be thread-safe.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class StatementManipulator {

    private final PivotStatementGenerator m_pivot;

    private final BinningStatamentGenerator m_binning;

    /**
     * Constructor of class {@link StatementManipulator}.
     */
    public StatementManipulator() {
        this(DefaultPivotStatementGenerator.getINSTANCE(), DefaultBinningStatementGenerator.getINSTANCE());
    }

    /**
     * @param supportsCase <code>true</code> if "CASE" statements can be used in SQL statements
     * @since 3.1
     */
    public StatementManipulator(final boolean supportsCase) {
        this(supportsCase ? CasePivotStatementGenerator.getINSTANCE() : DefaultPivotStatementGenerator.getINSTANCE(),
            supportsCase ? CaseBinningStatementGenerator.getINSTANCE() : DefaultBinningStatementGenerator.getINSTANCE());
    }

    /**
     * @param pivot {@link PivotStatementGenerator} implementation
     * @param binning {@link BinningStatamentGenerator} implementation
     * @since 3.1
     */
    protected StatementManipulator(final PivotStatementGenerator pivot, final BinningStatamentGenerator binning) {
        if (pivot == null) {
            throw new IllegalArgumentException("PivotStatementGenerator must not be null");
        }
        if (binning == null) {
            throw new IllegalArgumentException("BinningStatementGenerator must not be null");
        }
        m_pivot = pivot;
        m_binning = binning;
    }

    /**
     * Pattern for matching any character that needs escaping.
     *
     * @deprecated use {@link #SAVE_COLUMN_NAME_PATTERN} instead (and negate the condition) because this pattern is not
     *             complete
     */
    @Deprecated
    protected static final Pattern ESCAPE_CHARACTER_PATTERN = Pattern.compile("(\\s|[()-])+");

    /**
     * Pattern for matching any column name that needs no escaping.
     * @since 2.10
     */
    protected static final Pattern SAVE_COLUMN_NAME_PATTERN = Pattern.compile("^[\\w\\d]+$");

    private final Random m_rand = new Random();

    /**
     * Modifies the incoming SQL query so that the number of rows is limited. The default implementation uses the LIMIT
     * clause.
     *
     * @param sql any valid SQL query
     * @param count the maximum number of rows
     * @return an SQL query
     */
    public String limitRows(final String sql, final long count) {
        return "SELECT * FROM (" + sql + ") " + getTempTableName() + " LIMIT " + count;
    }

    /**
     * @param sql A valid SQL query
     * @param count The number of rows to take randomly
     * @return a SQL query
     * @since 3.1
     */
    public String randomRows(final String sql, final long count) {
        return limitRows(sql, count);
    }

    /**
     * Modifies the query so that it does not return any rows. This is usually used for just getting the result's
     * metadata from the database.
     *
     * @param sql any valid SQL query
     * @return an SQL query
     */
    public String forMetadataOnly(final String sql) {
        return "SELECT * FROM (" + sql + ") " + getTempTableName() + " WHERE (1 = 0)";
    }

    /**
     * @param tableName the name of the table to create
     * @param query the select statement
     * @return the sql statement that creates a table with the given name from the given sql query
     * @since 2.11
     */
    public String[] createTableAsSelect(final String tableName, final String query) {
        return new String[] {"CREATE TABLE " + tableName + " AS (" + query + ")"};
    }

    /**
     * @param tableName the name of the table to drop
     * @param cascade <code>true</code> if the cascade option should be used
     * @return the statement to drop the given table
     * @since 2.11
     */
    public String dropTable(final String tableName, final boolean cascade) {
        final StringBuilder buf = new StringBuilder("DROP TABLE ");
        buf.append(tableName);
        if (cascade) {
            buf.append(" CASCADE");
        }
        return buf.toString();
    }


    /**
     * Quotes an identifier e.g. column or table name if it contains characters that need
     * quoting e.g. white spaces.
     *
     * @param identifier the identifier to quote
     * @return the identifier, possibly quoted
     * @since 2.11
     */
    public String quoteIdentifier(final String identifier) {
        return quoteColumn(identifier);
    }

    /**
     * Quotes a column name if it contains characters that need
     * quoting e.g. white spaces.
     *
     * @param colName the column's name
     * @return the column's name, possibly quoted
     * @deprecated use {@link #quoteIdentifier(String)} instead
     */
    @Deprecated
    public String quoteColumn(final String colName) {
        if (colName == null) {
            return null;
        }
        Matcher m = SAVE_COLUMN_NAME_PATTERN.matcher(colName);
        if (!m.matches()) {
            return "\"" + colName + "\"";
        } else {
            // no need to quote
            return colName;
        }
    }

    /**
     * Replaces all characters that are not supported by the database in
     * identifiers with supported characters.
     *
     * @param colName the name to convert into a valid db column name
     * @return the given name with all characters replaced that are not
     * supported by the database
     * @since 2.10
     */
    public String getValidColumnName(final String colName) {
        return colName;
    }

    /**
     * Unquotes a column name from database metadata. Most databases don't need unquoting, but some drivers return
     * column names with quotes.
     *
     * @param colName the column's name
     * @return the column's name, unquoted
     */
    public String unquoteColumn(final String colName) {
        return colName;
    }

    /**
     * Sets the fetch size on the statement. Note that some database specific implementation need to modify connection
     * properties such as auto-commit, therefore you should save and restore the connection state around this commend.
     *
     * @param statement a statement
     * @param fetchSize the fetch size; a negative value indicates that the fetch size should not be set explicitly.
     * @throws SQLException if a database access error occurs, this method is called on a closed <code>Statement</code>
     */
    public void setFetchSize(final Statement statement, final int fetchSize) throws SQLException {
        if (fetchSize >= 0) {
            statement.setFetchSize(fetchSize);
        }
    }

    /**
     * Returns a random name for a temporary table.
     *
     * @return a random table name
     */
    protected final String getTempTableName() {
        return "tempTable_" + Math.abs(m_rand.nextLong());
    }

    /**
     * Returns a SQL statement for pivoting.
     *
     * @param tableName Input query
     * @param groupByColumnsList Name of columns used for GROUP BY
     * @param pivotElements Columns and corresponding elements used for pivot
     * @param aggValues Aggregation columns and corresponding functions
     * @param pivotColGenerator Column name generator
     * @return result SQL Statement for pivoting
     * @since 3.1
     */
    public String getPivotStatement(final String tableName, final List<String> groupByColumnsList,
        final Map<DataColumnSpec, Set<Object>> pivotElements,
        final List<Pair<String, DBAggregationFunction>> aggValues, final PivotColumnNameGenerator pivotColGenerator) {
        return m_pivot.getPivotStatement(this, tableName, groupByColumnsList, pivotElements, aggValues, pivotColGenerator);
   }



    /**
     * Returns a SQL statement for sampling.
     *
     * @param sql Input SQL query
     * @param valueToLimit Number of rows to take
     * @param random <code>true</code>, if rows are selected randomly
     * @return a SQL statement for sampling
     * @since 3.1
     */
    public String getSamplingStatement(final String sql, final long valueToLimit, final boolean random) {
        if (random) {
            return randomRows(sql, valueToLimit);
        } else {
            return limitRows(sql, valueToLimit);
        }
    }


    /**
     * Returns a SQL statement for binning
     *
     * @param query The input query
     * @param includeCols Names of columns that are binned
     * @param excludeCols Names of columns that are not binned
     * @param limitsMap Map containing limits of bins as values
     * @param includeMap Map containing boolean which indicates if edge is open (true) or closed (false)
     * @param namingMap Map containing names of bins as values
     * @param appendMap Map containing names of columns that should be appended as values (can be null).
     * @return a SQL statement for binning
     * @since 3.1
     */
    public String getBinnerStatement(final String query, final String[] includeCols, final String[] excludeCols, final Map<String, List<Double[]>> limitsMap,
        final Map<String, List<Boolean[]>> includeMap, final Map<String, List<String>> namingMap, final Map<String, String> appendMap) {
        return m_binning.getBinnerStatement(this, query, includeCols, excludeCols, limitsMap, includeMap , namingMap, appendMap);
    }
}

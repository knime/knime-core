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
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class lets you manipulate SQL statement by adding database-specific parts or changing statement parameters.
 * Subclasses may override some methods and change the manipulation. All implementations must be thread-safe.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class StatementManipulator {
    /**
     * Pattern for matching any character that needs escaping.
     */
    protected static final Pattern ESCAPE_CHARACTER_PATTERN = Pattern.compile("(\\s|[()-])+");


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

//    /**
//     * Modifies the incoming SQL query so that the number of rows is limited. The default implementation uses the LIMIT
//     * clause.
//     *
//     * @param sql any valid SQL query
//     * @param offset the offset
//     * @param count the maximum number of rows
//     * @return an SQL query
//     */
//    public String limitRows(final String sql, final long count, final long offset) {
//        return "SELECT * FROM (" + sql + ") " + getTempTableName() + " LIMIT " + count + " OFFSET " + offset;
//    }


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
     * Quotes a column name if it contains characters that need
     * quoting e.g. white spaces.
     *
     * @param colName the column's name
     * @return the column's name, possibly quoted
     */
    public String quoteColumn(final String colName) {
        Matcher m = ESCAPE_CHARACTER_PATTERN.matcher(colName);
        if (m.find()) {
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
}

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
 *   Jun 2, 2016 (budiyanto): created
 */
package org.knime.core.node.port.database.tablecreator;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Default {@link DBTableCreator} implementation.
 * @author Budi Yanto, KNIME.com
 * @since 3.3
 */
public class DBTableCreatorImpl implements DBTableCreator {

    private String m_warning = null;
    private String m_schema;
    private String m_tableName;
    private boolean m_isTempTable;
    private StatementManipulator m_sm;

    /**
     * Constructor of DefaultDBTableCreatorStatementGenerator
     * @param sm {@link StatementManipulator} to use
     * @param schema schema of the table to create
     * @param tableName name of the table to create
     * @param isTempTable <code>true</code> if the table is a temporary table, otherwise <code>false</code>
     */
    public DBTableCreatorImpl (final StatementManipulator sm, final String schema, final String tableName,
            final boolean isTempTable) {
        m_sm = sm;
        m_schema = schema;
        m_tableName = tableName;
        m_isTempTable = isTempTable;
    }

    /**
     * @return the {@link StatementManipulator}
     */
    protected StatementManipulator getStatementManipulator() {
        return m_sm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final boolean ifNotExists, final DBColumn[] columns, final DBKey[] keys,
        final String additionalSQLStatement)
        throws Exception {
        getCreateTableStatement(getSchema(), getTableName(), isTempTable(), ifNotExists,
            columns, keys, additionalSQLStatement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createTable(final DatabaseConnectionSettings conn, final CredentialsProvider cp, final boolean ifNotExists, final DBColumn[] columns,
            final DBKey[] keys, final String additionalSQLStatement) throws Exception {
        if(conn == null) {
            throw new NullPointerException("conn must not be null");
        }
        if (ifNotExists && tableExists(conn, cp, getSchema(), getTableName())) {
            return;
        }

        final String statement = getCreateTableStatement(getSchema(), getTableName(), isTempTable(), ifNotExists,
            columns, keys, additionalSQLStatement);
        conn.execute(statement, cp);
    }

    @Override
    public String getWarning() {
        return m_warning;
    }

    /**
     * @param warning the warning to be set
     */
    protected void setWarning(final String warning) {
        m_warning = warning;
    }

    /**
     * @param conn
     * @param cp {@link CredentialsProvider}
     * @param schema the db schema or <code>null</code> for no schema
     * @param tableName the name of the table
     * @return <code>true</code> if the table already exists in the database
     * @throws Exception if an error occurs during checking
     */
    protected boolean tableExists(final DatabaseConnectionSettings conn, final CredentialsProvider cp,
        final String schema, final String tableName) throws Exception {
        return conn.getUtility().tableExists(conn.createConnection(cp),
            createSchemaTableName(schema, tableName));
    }

    /**
     * Generates query to create a new table in database
     * @param schema the db schema or <code>null</code> for no schema
     * @param tableName name of the table to create
     * @param isTempTable true if the table is temporary table, otherwise false
     * @param ifNotExists option to create the table only if it does not exist
     * @param columns columns of the table
     * @param keys keys of the table
     * @param additionalSQLStatement additional SQL statement appended to the create table statement
     * @return query to create a new table
     * @throws Exception if an option is not supported
     */
    protected String getCreateTableStatement(final String schema, final String tableName, final boolean isTempTable,
            final boolean ifNotExists, final DBColumn[] columns, final DBKey[] keys,
            final String additionalSQLStatement) throws Exception {
        final StringBuilder builder = new StringBuilder();
        builder.append(getCreateTableFragment(schema, tableName, isTempTable, ifNotExists))
        .append(" (")
        .append(getColumnDefinitionFragment(columns));
        if(keys.length == 0) {
            builder.append(")");
        } else {
            builder.append(", ")
            .append(getTableConstraintsFragment(keys))
            .append(")");
        }
        if(!StringUtils.isBlank(additionalSQLStatement)) {
            builder.append(" ")
            .append(additionalSQLStatement);
        }
        builder.append(getTerminalCharacter());
        return builder.toString();

    }

    /**
     * @return the standard termination character
     */
    protected String getTerminalCharacter() {
        return ";";
    }

    /**
     * @param schema schema of the table
     * @param tableName name of the table
     * @param isTempTable <code>true</code> if the table is temporary table, otherwise <code>false</code>
     * @param ifNotExists <code>true</code> if the "ifNotExists" flag should be used, otherwise <code>false</code>
     * @return create table fragment
     * @throws Exception if an option is not supported
     */
    protected String getCreateTableFragment(final String schema, final String tableName, final boolean isTempTable,
            final boolean ifNotExists) throws Exception {
        final String schemaTable = createSchemaTableName(schema, tableName);
        final StringBuilder builder = new StringBuilder();
        builder.append(getCreateFragment())
        .append(" ")
        .append(getTempFragment(isTempTable))
        .append(" ")
        .append("TABLE")
        .append(" ")
        .append(getIfNotExistsFragment(ifNotExists))
        .append(" ")
        .append(schemaTable);

        return builder.toString();
    }

    /**
     * @return create fragment used in the create table fragment
     */
    protected String getCreateFragment() {
        return "CREATE";
    }

    /**
     * @param schema the db schema name or <code>null</code> for none
     * @param tableName the table name
     * @return the combination of table name and schema name and quotes them if necessary
     */
    protected String createSchemaTableName(final String schema, final String tableName) {
        return (StringUtils.isBlank(schema)) ? tableName : schema + "." + tableName;
    }

    /**
     * @param isTempTable <code>true</code> if the table is temporary table, otherwise <code>false</code>
     * @return temporary fragment used in create table fragment
     * @throws Exception if temporary tables aren't supported
     */
    protected String getTempFragment(final boolean isTempTable) throws Exception {
        if(isTempTable) {
            return "TEMPORARY";
        }else {
            return "";
        }
    }

    /**
     * @param ifNotExists option to create the table only if it does not exist
     * @return ifNotExists fragment used in create table fragment
     */
    protected String getIfNotExistsFragment(final boolean ifNotExists) {
        return "";
    }


    /**
     * Creates column definitions statement to create a table in database
     * @param columns columns of the table
     * @return column definitions statement
     * @throws Exception if an option is not supported
     */
    protected String getColumnDefinitionFragment(final DBColumn[] columns) throws Exception {
        if(columns.length == 0){
            throw new IllegalArgumentException("At least one column must be defined");
        }
        final StringBuilder builder = new StringBuilder();
        for(DBColumn col : columns){
            builder.append(getStatementManipulator().quoteIdentifier(col.getName()))
            .append(" ")
            .append(col.getType())
            .append(" ")
            .append(getNotNullFragment(col.isNotNull()))
            .append(", ");
        }

        // removes the tailing ", "
        return builder.substring(0, builder.lastIndexOf(","));
    }

    /**
     * @param isNotNull <code>true</code> if the column should be defined as NOT NULL
     * @return "not null" fragment used in column definitions fragment
     * @throws Exception if not null or null is not supported
     */
    protected String getNotNullFragment(final boolean isNotNull) throws Exception {
        if(isNotNull) {
            return "NOT NULL";
        } else {
            return "";
        }
    }

    /**
     * Creates key constraints statement to create a table in database
     * @param keys keys of the table
     * @return table constraints fragment
     * @throws Exception if table constraints aren't supported
     */
    protected String getTableConstraintsFragment(final DBKey[] keys) throws Exception {
        final StringBuilder builder = new StringBuilder();
        for(DBKey key : keys){
            builder.append("CONSTRAINT")
            .append(" ")
            .append(getStatementManipulator().quoteIdentifier(key.getName()))
            .append(" ")
            .append(getKeyConstraintFragment(key.isPrimaryKey()))
            .append(" (");
            for(final DBColumn col : key.getColumns()) {
                builder.append(getStatementManipulator().quoteIdentifier(col.getName()))
                .append(", ");
            }

            // replace last "," character with ")"
            builder.replace(builder.lastIndexOf(","), builder.length(), ")")
            .append(", ");
        }

        // removes the tailing ", "
        return builder.substring(0, builder.lastIndexOf(","));
    }

    /**
     * @param isPrimaryKey
     * @return primary key fragment
     * @throws Exception if primary and/or unique keys arent't supported
     */
    protected String getKeyConstraintFragment(final boolean isPrimaryKey) throws Exception {
        if(isPrimaryKey) {
            return "PRIMARY KEY";
        } else {
            return "UNIQUE";
        }
    }

    /**
     * @return the schema name used in the query. This might differ from what was entered in the dialog.
     */
    @Override
    public String getSchema() {
        // return the used schema
        return m_schema;
    }

    /**
     * @return the table name used in the query. This might differ from what was entered in the dialog.
     */
    @Override
    public String getTableName() {
        // return the used table name
        return m_tableName;
    }

    /**
     * @return whether the table that is created is temporary
     */
    protected boolean isTempTable() {
        return m_isTempTable;
    }
}

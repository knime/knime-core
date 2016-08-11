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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 *
 * @author Budi Yanto, KNIME.com
 * @since 3.2
 */
public class DBTableCreatorImpl implements DBTableCreator {

    private DatabaseConnectionSettings m_conn;

    /**
     * Constructor of DefaultDBTableCreatorStatementGenerator
     * @param conn a database connection settings object
     */
    public DBTableCreatorImpl (final DatabaseConnectionSettings conn) {
        if(conn == null) {
            throw new NullPointerException("conn must not be null");
        }
        m_conn = conn;
    }

    /**
     * @return the conn
     */
    protected DatabaseConnectionSettings getConnection() {
        return m_conn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createTable(final CredentialsProvider cp, final String schema, final String tableName,
        final boolean isTempTable, final boolean ifNotExists, final DBColumn[] columns, final DBKey[] keys)
                throws Exception{
        if (ifNotExists && tableExists(cp, schema, tableName)) {
            return;
        }
        final String statement = getCreateTableStatement(schema, tableName, isTempTable, ifNotExists, columns, keys);
        @SuppressWarnings("resource") //we are not allowed to close the connection
        final Connection sqlConn = getConnection().createConnection(cp);
        synchronized (m_conn.syncConnection(sqlConn)) {
            m_conn.execute(statement, cp);
        }
    }

    /**
     * @param cp {@link CredentialsProvider}
     * @param schema the db schema or <code>null</code> for no schema
     * @param tableName the name of the table
     * @return <code>true</code> if the table already exists in the database
     * @throws SQLException
     * @throws InvalidSettingsException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws IOException
     */
    protected boolean tableExists(final CredentialsProvider cp, final String schema, final String tableName)
            throws SQLException,
        InvalidSettingsException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, IOException {
        return getConnection().getUtility().tableExists(getConnection().createConnection(cp),
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
     * @return query to create a new table
     * @since 3.2
     */
    protected String getCreateTableStatement(final String schema, final String tableName, final boolean isTempTable,
            final boolean ifNotExists, final DBColumn[] columns, final DBKey[] keys){

        final StringBuilder builder = new StringBuilder();
        builder.append(getCreateTableFragment(schema, tableName, isTempTable, ifNotExists))
        .append(" (")
        .append(getColumnDefinitionFragment(columns));
        if(keys.length == 0) {
            builder.append(");");
        } else {
            builder.append(", ")
            .append(getTableConstraintsFragment(keys))
            .append(");");
        }

        return builder.toString();

    }

    /**
     * @param schema schema of the table
     * @param tableName name of the table
     * @param isTempTable <code>true</code> if the table is temporary table, otherwise <code>false</code>
     * @param ifNotExists <code>true</code> if the "ifNotExists" flag should be used, otherwise <code>false</code>
     * @return create table fragment
     */
    protected String getCreateTableFragment(final String schema, final String tableName, final boolean isTempTable,
            final boolean ifNotExists) {
        final String schemaTable = createSchemaTableName(schema, tableName);
        final StringBuilder builder = new StringBuilder();
        builder.append("CREATE")
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
     * @param schema the db schema name or <code>null</code> for none
     * @param tableName the table name
     * @return the combination of table name and schema name
     */
    protected String createSchemaTableName(final String schema, final String tableName) {
        return (StringUtils.isBlank(schema)) ? tableName : schema + "." + tableName;
    }

    /**
     * @param isTempTable
     * @return temporary fragment used in create table fragment
     */
    protected String getTempFragment(final boolean isTempTable) {
        if(isTempTable) {
            return "TEMPORARY";
        }else {
            return "";
        }
    }

    /**
     * @param ifNotExists
     * @return ifNotExists fragment used in create table fragment
     */
    protected String getIfNotExistsFragment(final boolean ifNotExists) {
        return "";
    }


    /**
     * Creates column definitions statement to create a table in database
     * @param columns columns of the table
     * @return column definitions statement
     */
    protected String getColumnDefinitionFragment(final DBColumn[] columns){
        if(columns.length == 0){
            throw new IllegalArgumentException("At least one column must be defined");
        }
        final StatementManipulator sm = m_conn.getUtility().getStatementManipulator();
        final StringBuilder builder = new StringBuilder();
        for(DBColumn col : columns){
            builder.append(sm.quoteIdentifier(col.getName()))
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
     * @param isNotNull
     * @return "not null" fragment used in column definitions fragment
     */
    protected String getNotNullFragment(final boolean isNotNull) {
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
     */
    protected String getTableConstraintsFragment(final DBKey[] keys){
        final StatementManipulator sm = m_conn.getUtility().getStatementManipulator();
        final StringBuilder builder = new StringBuilder();
        for(DBKey key : keys){
            builder.append("CONSTRAINT")
            .append(" ")
            .append(sm.quoteIdentifier(key.getName()))
            .append(" ")
            .append(getPrimaryKeyFragment(key.isPrimaryKey()))
            .append(" (");
            for(final DBColumn col : key.getColumns()) {
                builder.append(sm.quoteIdentifier(col.getName()))
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
     */
    protected String getPrimaryKeyFragment(final boolean isPrimaryKey) {
        if(isPrimaryKey) {
            return "PRIMARY KEY";
        } else {
            return "UNIQUE";
        }
    }

}

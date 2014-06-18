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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.port.database;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DatabaseQueryConnectionSettings
        extends DatabaseConnectionSettings {

    /** Table place holder <code>#table#</code>. */
    public static final String TABLE_PLACEHOLDER = "#table#";

    private String m_query;

    private boolean m_validateQuery;

    /**
     * Creates a new empty settings object. Settings must be loaded with
     * {@link #loadValidatedConnection(ConfigRO, CredentialsProvider)}.
     * @since 2.10
     */
    public DatabaseQueryConnectionSettings() {

    }

    /**
     * Create a new connection with an empty query object.
     * @param settings settings to load from
     * @param cp credentials provider used to get user name and password
     * @throws InvalidSettingsException if settings could not be loaded
     */
    public DatabaseQueryConnectionSettings(final ConfigRO settings,
            final CredentialsProvider cp)
            throws InvalidSettingsException {
        super();
        m_query = settings.getString(DatabaseConnectionSettings.CFG_STATEMENT);
        super.loadValidatedConnection(settings, cp);
    }

    /**
     * Creates a new connection based in the given connection and the query
     * string.
     * @param conn connection to copy
     * @param query the SQL query
     */
    public DatabaseQueryConnectionSettings(
            final DatabaseConnectionSettings conn,
            final String query) {
        super(conn);
        setQuery(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateConnection(final ConfigRO settings,
            final CredentialsProvider cp)
            throws InvalidSettingsException {
        String query = settings.getString(CFG_STATEMENT);
        if (query != null && query.contains(TABLE_PLACEHOLDER)) {
            throw new InvalidSettingsException("Database table place holder ("
                    + TABLE_PLACEHOLDER + ") not replaced in query:\n"
                    + query);
        }
        super.validateConnection(settings, cp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveConnection(final ConfigWO settings) {
        settings.addString(CFG_STATEMENT, m_query);
        settings.addBoolean("execute_without_configure", !m_validateQuery);
        super.saveConnection(settings);
    }

    /**
     * @return SQL statement
     */
    public String getQuery() {
        return m_query;
    }

    /**
     * Sets the query in this object.
     *
     * @param sql a SQL query
     * @since 2.10
     */
    public void setQuery(final String sql) {
        // Some database don't like ; in between an SQL query. This happens when we wrap the original query, e.g.
        // SELECT * FROM (SELECT * FROM bla;) temp
        if (sql.endsWith(";")) {
            m_query = sql.substring(0, sql.length() - 1);
        } else {
            m_query = sql;
        }
    }


    /**
     * Sets whether the query should be validated during configure.
     *
     * @param validateQuery <code>true</code> if the query should be validated, <code>false</code> otherwise
     * @since 2.10
     */
    public void setValidateQuery(final boolean validateQuery) {
        m_validateQuery = validateQuery;
    }


    /**
     * Returns whether the query should be validated during configure.
     *
     * @return <code>true</code> if the query should be validated, <code>false</code> otherwise
     * @since 2.10
     */
    public boolean getValidateQuery() {
        return m_validateQuery;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ModelContentRO createConnectionModel() {
        ModelContent cont = new ModelContent("database_query_connection_model");
        saveConnection(cont);
        return cont;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean loadValidatedConnection(final ConfigRO settings, final CredentialsProvider cp)
        throws InvalidSettingsException {
        boolean settingsChanged = false;

        String newQuery = settings.getString(DatabaseConnectionSettings.CFG_STATEMENT);
        settingsChanged |= (newQuery != m_query) && (newQuery != null) && !newQuery.equals(m_query);
        m_query = newQuery;

        boolean newValidateQuery = !settings.getBoolean("execute_without_configure");
        settingsChanged |= (newValidateQuery != m_validateQuery);
        m_validateQuery = newValidateQuery;

        return super.loadValidatedConnection(settings, cp) || settingsChanged;
    }
}

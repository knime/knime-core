/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   08.05.2014 (thor): created
 */
package org.knime.base.node.io.database.connection.util;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Extension of {@link DatabaseConnectionSettings} the allows the user to specify the individual parts of the database
 * URL (i.e. hostname, port, and database name).
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.10
 */
public class DefaultDatabaseConnectionSettings extends DatabaseConnectionSettings {
    private String m_host;

    private int m_port = 10000;

    private String m_databaseName;

    /**
     * Returns the database's host name.
     *
     * @return a hostname (or IP address)
     */
    public String getHost() {
        return m_host;
    }

    /**
     * Sets the database's host name.
     *
     * @param host a hostname (or IP address)
     */
    public void setHost(final String host) {
        m_host = host;
    }

    /**
     * Returns the database's port.
     *
     * @return the port
     */
    public int getPort() {
        return m_port;
    }

    /**
     * Sets the database's port.
     *
     * @param port the port, must be between 1 and 65535
     */
    public void setPort(final int port) {
        m_port = port;
    }

    /**
     * Returns the database's name.
     *
     * @return a database name
     */
    public String getDatabaseName() {
        return m_databaseName;
    }

    /**
     * Sets the database's name.
     *
     * @param databaseName a database name
     */
    public void setDatabaseName(final String databaseName) {
        m_databaseName = databaseName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveConnection(final ConfigWO settings) {
        super.saveConnection(settings);

        Config conf = settings.addConfig("default-connection");
        conf.addString("hostname", m_host);
        conf.addInt("port", m_port);
        conf.addString("databaseName", m_databaseName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateConnection(final ConfigRO settings, final CredentialsProvider cp)
        throws InvalidSettingsException {
        super.validateConnection(settings, cp);

        Config conf = settings.getConfig("default-connection");
        conf.getString("hostname");
        conf.getInt("port");
        conf.getString("databaseName");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean loadValidatedConnection(final ConfigRO settings, final CredentialsProvider cp)
        throws InvalidSettingsException {
        boolean b = super.loadValidatedConnection(settings, cp);

        Config conf = settings.getConfig("default-connection");
        m_host = conf.getString("hostname");
        m_port = conf.getInt("port");
        m_databaseName = conf.getString("databaseName");

        return b;
    }
}

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
 *   20.11.2014 (thor): created
 */
package org.knime.testing.core;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseDriverLoader;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Abstract base class for janitors that create test databases.
 *
 * <b>This class is not part of the API and my change without notice!</b>
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractDatabaseJanitor extends TestrunJanitor {
    private static final SecureRandom RAND = new SecureRandom();

    private final String m_driverName;

    private final String m_initialDatabase;

    private final String m_host;

    private final int m_port;

    private final String m_username;

    private final String m_password;

    private String m_dbName;

    private boolean m_databaseCreated;

    /**
     * Creates a new database janitor.
     *
     * @param driverName the JDBC driver's class name
     * @param initialDatabase the name of the database to which the initial connection is made
     * @param host the database host
     * @param port the database port
     * @param username the user's name
     * @param password the password
     */
    protected AbstractDatabaseJanitor(final String driverName, final String initialDatabase, final String host,
        final int port, final String username, final String password) {
        m_driverName = driverName;
        m_initialDatabase = initialDatabase;
        m_host = host;
        m_port = port;
        m_username = username;
        m_password = password;
        m_dbName = "knime_testing_" + Long.toHexString(RAND.nextLong()) + Long.toHexString(RAND.nextLong());
    }

    /**
     * Returns a JDBC URL for the given database.
     *
     * @param dbName the database's name
     * @return a JDBC URL
     */
    protected abstract String getJDBCUrl(String dbName);

    private String getVariablePrefix() {
        return getJDBCUrl("db").split(":")[1] + ".";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FlowVariable> getFlowVariables() {
        String variablePrefix = getVariablePrefix();

        List<FlowVariable> flowVariables = new ArrayList<>();
        flowVariables.add(new FlowVariable(variablePrefix + "db-name", m_dbName));
        flowVariables.add(new FlowVariable(variablePrefix + "driver-name", m_driverName));
        flowVariables.add(new FlowVariable(variablePrefix + "host", m_host));
        flowVariables.add(new FlowVariable(variablePrefix + "port", m_port));
        flowVariables.add(new FlowVariable(variablePrefix + "username", m_username));
        flowVariables.add(new FlowVariable(variablePrefix + "password", m_password));
        flowVariables.add(new FlowVariable(variablePrefix + "jdbc-url", getJDBCUrl(m_dbName)));
        return flowVariables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void before() throws Exception {
        DatabaseDriverLoader.registerDriver(m_driverName);

        try (Connection conn = DriverManager.getConnection(getJDBCUrl(m_initialDatabase), m_username, m_password)) {
            Statement stmt = conn.createStatement();
            String sql = "CREATE DATABASE " + m_dbName;
            stmt.execute(sql);
            m_databaseCreated = true;
            NodeLogger.getLogger(getClass()).info("Created temporary testing database " + m_dbName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void after() throws Exception {
        if (m_databaseCreated) {
            m_databaseCreated = false;

            // close the connection first in order to avoid open transactions that prevent dropping the database
            Class<DatabaseConnectionSettings> clazz = DatabaseConnectionSettings.class;
            Field mapField = clazz.getDeclaredField("CONNECTION_MAP");
            mapField.setAccessible(true);
            Map<?, Connection> connectionMap = (Map<?, Connection>)mapField.get(null);
            for (Connection conn : connectionMap.values()) {
                if (!conn.isClosed() && conn.getMetaData().getURL().equals(getJDBCUrl(m_dbName))) {
                    conn.close();
                }
            }

            try (Connection conn = DriverManager.getConnection(getJDBCUrl(m_initialDatabase), m_username, m_password)) {
                Statement stmt = conn.createStatement();
                String sql = "DROP DATABASE " + m_dbName;
                stmt.execute(sql);
                NodeLogger.getLogger(getClass()).info("Deleted temporary testing database " + m_dbName);
            }
        }

        m_dbName = "knime_testing_" + Long.toHexString(RAND.nextLong()) + Long.toHexString(RAND.nextLong());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Sets up a test database at " + m_host + " and exports several flow variables prefixed with '"
            + getVariablePrefix() + "'.";
    }
}

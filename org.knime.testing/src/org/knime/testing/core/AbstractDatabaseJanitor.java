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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.connection.CachedConnectionFactory;
import org.knime.core.node.port.database.connection.DBDriverFactory;
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

    private final DateFormat m_dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private final String m_driverName;

    private final String m_initialDatabase;

    private final String m_host;

    private final int m_port;

    private final String m_username;

    private final String m_password;

    private String m_dbName;

    /**
     * Flag that indicates if the database was actually created and that it should be deleted afterwards.
     */
    protected boolean m_databaseCreated;

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
        m_dbName = "knime_testing_" + m_dateFormat.format(new Date()) + "_" + Integer.toHexString(RAND.nextInt()) +
                "_" + KNIMEConstants.VERSION.replace('.', '_').replace("_v\\d+$", "");
    }

    /**
     * Returns a JDBC URL for the given database.
     *
     * @param dbName the database's name
     * @return a JDBC URL
     */
    protected abstract String getJDBCUrl(String dbName);

    /**
     * Returns the prefix for all variables returned by {@link #getFlowVariables()}.
     *
     * @return the prefix (ending with ".")
     */
    protected final String getVariablePrefix() {
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
//        DatabaseDriverLoader.registerDriver(m_driverName);
        createDatabase(m_initialDatabase, m_username, m_password, m_dbName);
        m_databaseCreated = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void after() throws Exception {
        if (m_databaseCreated) {
            m_databaseCreated = false;

            // close the connection first in order to avoid open transactions that prevent dropping the database
            Class<CachedConnectionFactory> clazz = CachedConnectionFactory.class;
            Field mapField = clazz.getDeclaredField("CONNECTION_MAP");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<?, Connection> connectionMap = (Map<?, Connection>)mapField.get(null);
            Iterator<Connection> it = connectionMap.values().iterator();
            while (it.hasNext()) {
                Connection conn = it.next();
                if (conn.getMetaData().getURL().contains(m_dbName)) {
                    if (!conn.isClosed()) {
                        NodeLogger.getLogger(getClass()).debug("Closing connection " + conn.getMetaData().getURL()
                            + " for user " + conn.getMetaData().getUserName());
                        conn.setAutoCommit(false);
                        conn.rollback();
                        conn.close();
                    }
                    it.remove();
                }
            }

            dropDatabase(m_initialDatabase, m_username, m_password, m_dbName);
        }

        m_dbName = "knime_testing_" + m_dateFormat.format(new Date()) + "_" + Long.toHexString(RAND.nextLong()) +
                "_" + KNIMEConstants.VERSION.replace('.', '_');

    }

    /**
     * Creates a new database. Subclasses may override this method.
     *
     * @param initialDatabase the initial database to connect to
     * @param username the username
     * @param password the password
     * @param dbName name of the new database
     * @throws Exception
     */
    protected void createDatabase(final String initialDatabase, final String username, final String password,
        final String dbName) throws Exception {
        Driver driver = getDriver(initialDatabase, username, password);
        final Properties props = getProperties(username, password);
        try (Connection conn = driver.connect(getJDBCUrl(initialDatabase), props)) {
            String sql = "CREATE DATABASE " + dbName;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                NodeLogger.getLogger(getClass()).info("Created temporary testing database " + dbName);
            }
        }
    }

    /**
     * @param username
     * @param password
     * @return
     */
    protected Properties getProperties(final String username, final String password) {
        final Properties props = new Properties();
        props.put("user", username);
        props.put("password", password);
        return props;
    }

    /**
     * @param initialDatabase
     * @param username
     * @param password
     * @return
     * @throws Exception
     */
    protected Driver getDriver(final String initialDatabase, final String username, final String password)
        throws Exception {
        @SuppressWarnings("deprecation")
        final DatabaseConnectionSettings settings =
                new DatabaseConnectionSettings(m_driverName, getJDBCUrl(initialDatabase), username, password, null);
        final DBDriverFactory driverFactory = settings.getUtility().getConnectionFactory().getDriverFactory();
        Driver driver = driverFactory.getDriver(settings);
        return driver;
    }

    /**
     * Drops a database. Subclasses may override this method.
     *
     * @param initialDatabase the initial database to connect to
     * @param username the username
     * @param password the password
     * @param dbName name of the database to drop
     * @throws SQLException if a database error occurs
     */
    protected void dropDatabase(final String initialDatabase, final String username, final String password,
        final String dbName) throws SQLException {
        try (Connection conn = DriverManager.getConnection(getJDBCUrl(initialDatabase), username, password)) {
            String sql = "DROP DATABASE " + dbName;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                NodeLogger.getLogger(getClass()).info("Deleted temporary testing database " + dbName);
            }
        }
    }

    /**
     * Returns the name of the temporary database. The name is reset during {@link #after()}.
     *
     * @return a database name
     */
    protected final String getDatabaseName() {
        return m_dbName;
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

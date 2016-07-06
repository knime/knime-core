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
 *   19.05.2016 (koetter): created
 */
package org.knime.core.node.port.database.connection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.RegisteredDriversConnectionFactory;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.util.ThreadUtils;

/**
 *
 * @author Tobias Koetter, KNIME.com
 * @since 3.2
 */
public class CachedConnectionFactory implements DBConnectionFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RegisteredDriversConnectionFactory.class);
    /** Map the keeps database connection based on the user and URL. */
    private static final Map<ConnectionKey, Connection> CONNECTION_MAP = Collections.synchronizedMap(new HashMap<ConnectionKey, Connection>());
    /** Holding the database connection keys used to sync the open connection
     * process. */
    private static final Map<ConnectionKey, ConnectionKey> CONNECTION_KEYS = new HashMap<ConnectionKey, ConnectionKey>();
    private static final ExecutorService CONNECTION_CREATOR_EXECUTOR = ThreadUtils.executorServiceWithContext(Executors.newCachedThreadPool());
    private DBDriverFactory m_driverFactory;

    private static final class ConnectionKey {
            private final String m_un;
            private final String m_pw;
            private final String m_dn;
            private boolean m_kerberos;
            private ConnectionKey(final String userName, final String password,
                    final String databaseName, final boolean kerberos) {
                m_un = userName;
                m_pw = password;
                m_dn = databaseName;
                m_kerberos = kerberos;
            }
            /** {@inheritDoc} */
            @Override
            public boolean equals(final Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || !(obj instanceof ConnectionKey)) {
                    return false;
                }
                ConnectionKey ck = (ConnectionKey) obj;
                if (!ConvenienceMethods.areEqual(this.m_un, ck.m_un)
                      || !ConvenienceMethods.areEqual(this.m_pw, ck.m_pw)
                      || !ConvenienceMethods.areEqual(this.m_dn, ck.m_dn)
                      || this.m_kerberos != ck.m_kerberos) {
                    return false;
                }
                return true;
            }

            /** {@inheritDoc} */
            @Override
            public int hashCode() {
                return m_un.hashCode() ^ m_dn.hashCode();
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "ConnectionKey [m_un=" + m_un + ", m_dn=" + m_dn + "]";
            }

        }

    /**
     * @param driverFactory the {@link DBDriverFactory} to get the {@link Driver}
     */
    public CachedConnectionFactory(final DBDriverFactory driverFactory) {
        m_driverFactory = driverFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DBDriverFactory getDriverFactory() {
        return m_driverFactory;
    }

    /**
     * {@inheritDoc}
     * @throws IOException
     */
    @Override
    public Connection getConnection(final CredentialsProvider cp, final DatabaseConnectionSettings settings) throws InvalidSettingsException, SQLException, IOException {
        final String jdbcUrl = settings.getJDBCUrl();
        final String user = settings.getUserName(cp);
        final String pass = settings.getPassword(cp);
        final boolean kerberos = settings.useKerberos();

        // database connection key with user, password and database URL
        ConnectionKey databaseConnKey = new ConnectionKey(user, pass, jdbcUrl, kerberos);

        // retrieve original key and/or modify connection key map
        synchronized (CONNECTION_KEYS) {
            if (CONNECTION_KEYS.containsKey(databaseConnKey)) {
                databaseConnKey = CONNECTION_KEYS.get(databaseConnKey);
            } else {
                CONNECTION_KEYS.put(databaseConnKey, databaseConnKey);
            }
        }

        // sync database connection key: unique with database url and user name
        synchronized (databaseConnKey) {
            Connection conn = CONNECTION_MAP.get(databaseConnKey);
            // if connection already exists
            if (conn != null) {
                LOGGER.debug("Connection found in cache for key: " + databaseConnKey);
                try {
                    if (conn.isClosed() || !settings.getUtility().isValid(conn)) {
                        LOGGER.debug("Connection closed or invalid for key: " + databaseConnKey);
                        CONNECTION_MAP.remove(databaseConnKey);
                    } else {
                        conn.clearWarnings();
                        return conn;
                    }
                } catch (Exception e) { // remove invalid connection
                    LOGGER.debug("Invalid connection with key: " + databaseConnKey + " Exception: " + e.getMessage());
                    CONNECTION_MAP.remove(databaseConnKey);
                }
            }
            final Driver d;
            try {
                d = getDriverFactory().getDriver(settings);
            } catch (Exception ex1) {
                throw new InvalidSettingsException(ex1);
            }
            if (!d.acceptsURL(jdbcUrl)) {
                throw new InvalidSettingsException("Driver \"" + d + "\" does not accept URL: " + jdbcUrl);
            }
            // if a connection is not available
            Callable<Connection> callable = new Callable<Connection>() {
                /** {@inheritDoc} */
                @Override
                public Connection call() throws Exception {
                    LOGGER.debug("Opening database connection to \"" + jdbcUrl + "\"...");
                    return createConnection(jdbcUrl, user, pass, kerberos, d);
                }
            };
            //TODO:this has to be more robust e.g. the thread should terminate when KNIME terminates and should be
            //cancelable if the user presses cancel. If no credentials are present for Phoenix the thread keeps KNIME
            //alive for ages
            Future<Connection> task = CONNECTION_CREATOR_EXECUTOR.submit(callable);
            try {
                conn = task.get(DatabaseConnectionSettings.getDatabaseTimeout() + 1, TimeUnit.SECONDS);
                CONNECTION_MAP.put(databaseConnKey, conn);
                return conn;
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof SQLException) {
                    throw (SQLException) ee.getCause();
                } else {
                    throw new SQLException(ee.getCause());
                }
            } catch (InterruptedException ex) {
                throw new SQLException("Thread was interrupted while waiting for database to respond");
            } catch (TimeoutException ex) {
                throw new IOException("Connection to database '" + jdbcUrl + "' timed out");
            }
        }
    }

    /**
     * @param user the user name. Might be <code>null</code>.
     * @param pass the password. Might be <code>null</code>.
     * @return {@link Properties} to use
     */
    protected Properties createConnectionProperties(final String user, final String pass) {
        final Properties props = new Properties();
        if (user != null) {
            props.put("user", user);
        }
        if (pass != null) {
            props.put("password", pass);
        }
        return props;
    }

    /**
     * @param jdbcUrl jdbc url
     * @param user user name
     * @param pass optional password
     * @param useKerberos <code>true</code> if Kerberos should be used
     * @param d the {@link Driver}
     * @return the {@link Connection}
     * @throws SQLException
     */
    protected Connection createConnection(final String jdbcUrl, final String user, final String pass,
        final boolean useKerberos, final Driver d)
        throws SQLException {
    	if (useKerberos){
    		throw new SQLException("Kerberos not supported");
    	}
        final Properties props = createConnectionProperties(user, pass);
        final Connection connection = d.connect(jdbcUrl, props);
        if (connection == null) {
            LOGGER.warnWithFormat("Driver \"%s\" return 'null' connection for DB URL \"%s\"",
                d.getClass().getName(), jdbcUrl);
        }
        return connection;
    }
}
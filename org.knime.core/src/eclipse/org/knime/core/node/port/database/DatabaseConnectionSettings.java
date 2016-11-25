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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.StringHistory;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.core.util.KnimeEncryption;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabaseConnectionSettings {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(DatabaseConnectionSettings.class);

    /** Config for SQL statement. */
    public static final String CFG_STATEMENT = "statement";

    /** Keeps the history of all loaded driver and its order. */
    public static final StringHistory DRIVER_ORDER = StringHistory.getInstance(
            "database_drivers");

    /** Keeps the history of all database URLs. */
    public static final StringHistory DATABASE_URLS = StringHistory.getInstance(
            "database_urls");
//
//    private static final ExecutorService CONNECTION_CREATOR_EXECUTOR =
//            ThreadUtils.executorServiceWithContext(Executors.newCachedThreadPool());

    private static int databaseTimeout = Math.max(15, getSystemPropertyDatabaseTimeout());

    static {
        if (databaseTimeout >= 0) {
            setDatabaseTimeout(databaseTimeout);
        }
    }

    /**
     * Returns the database timeout set via the (deprecated) system property.
     *
     * @return timeout in seconds or -1 if not timeout is set
     * @since 2.10
     */
    public static int getSystemPropertyDatabaseTimeout() {
        int timeout = -1;
        @SuppressWarnings("deprecation")
        String sysPropTimeout = System.getProperty(KNIMEConstants.PROPERTY_DATABASE_LOGIN_TIMEOUT);

        if (sysPropTimeout != null) {
            LOGGER.warn("Please use the preferences for setting the database timeout instead of the system property");
            try {
                int t = Integer.parseInt(sysPropTimeout);
                if (t <= 0) {
                    LOGGER.warn("Database timeout set via system propery not valid (<= 0) '" + sysPropTimeout + "'.");
                } else {
                    timeout = t;
                }
            } catch (NumberFormatException nfe) {
                LOGGER.warn("Database timeout set via system propery not valid '" + sysPropTimeout + "'.");
            }
        }
        return timeout;
    }

    /**
     * Returns the current database timeout in seconds. The timeout is usually controlled via a preference by
     * may also be taken from the (deprecated) system property {@link KNIMEConstants#PROPERTY_DATABASE_LOGIN_TIMEOUT}
     * in case no value has been set in the preferences yet.
     *
     * @return the timeout in seconds
     * @since 2.10
     */
    public static int getDatabaseTimeout() {
        return databaseTimeout;
    }

    /**
     * Sets the global database timeout. The timeout is mainly used for login but some drivers also use it for
     * all other database operations.
     *
     * @param seconds timeout in seconds
     *
     * @since 2.10
     */
    public static void setDatabaseTimeout(final int seconds) {
        databaseTimeout = seconds;
        LOGGER.debug("Settings database timeout to " + databaseTimeout + " seconds");
        DriverManager.setLoginTimeout(databaseTimeout);
    }

    /** Used to switch on/off the database connection access (applies only for the same database connection).
     * Default is true, that is all database accesses are synchronized based on single connection; false means off,
     * that is, the access is not synchronized and may lead to database errors.
     */
    private static final boolean SQL_CONCURRENCY = initSQLConcurrency();
    private static boolean initSQLConcurrency() {
        String sconcurrency = System.getProperty(KNIMEConstants.PROPERTY_DATABASE_CONCURRENCY);
        boolean concurrency = true; // default
        if (sconcurrency != null) {
            concurrency = Boolean.parseBoolean(sconcurrency);
        }
        LOGGER.debug("Database concurrency (sync via database connection) is " + concurrency + ".");
        return concurrency;
    }

    /** {@link DriverManager} fetch size to chunk specified number of rows while reading from database. */
    public static final Integer FETCH_SIZE = initFetchSize();
    private static Integer initFetchSize() {
        String fsize = System.getProperty(
                KNIMEConstants.PROPERTY_DATABASE_FETCHSIZE);
        if (fsize != null) {
            try {
                int fetchsize = Integer.parseInt(fsize);
                LOGGER.debug("Database fetch size: " + fetchsize + " rows.");
                return fetchsize;
            } catch (NumberFormatException nfe) {
                LOGGER.warn("Database fetch size not valid '" + fsize
                        + "', no fetch size will be set.");
            }
        }
        return null;
    }

    /** Properties defines the number of rows written in on chunk into the database.
     * @since 2.6 */
    public static final int BATCH_WRITE_SIZE = initBatchWriteSize();
    private static int initBatchWriteSize() {
        String bsize = System.getProperty(KNIMEConstants.PROPERTY_DATABASE_BATCH_WRITE_SIZE);
        if (bsize != null) {
            try {
                final int batchsize = Integer.parseInt(bsize);
                if (batchsize > 0) {
                    LOGGER.debug("Database batch write size: " + batchsize + " rows.");
                    return batchsize;
                } else {
                    LOGGER.warn("Database property knime.database.batch_write_size=" + batchsize
                            + " can't be smaller than 1, using 1 as default.");
                }
            } catch (NumberFormatException nfe) {
                LOGGER.warn("Database batch write size not valid '" + bsize
                        + "', using 1 as default.");
            }
        }
        // default batch write size
        return 1;
    }

    private String m_driver;
    private String m_credName = null;

    private String m_jdbcUrl;
    private String m_user = null;
    private String m_pass = null;

    private boolean m_kerberos = false;

    private String m_timezone = "current"; // use current as of KNIME 2.8, none before 2.8

    private boolean m_validateConnection;

    private boolean m_retrieveMetadataInConfigure;

    private boolean m_allowSpacesInColumnNames;
    /**Access this field only via the {@link #getDatabaseIdentifier()} method since it might be <code>null</code>
     * in which case the identifier gets extracted from the JDBC url.*/
    private String m_dbIdentifier;

    // this is to fix bug #4066 and not exposed to the user
    private boolean m_rowIdsStartWithZero;

    /**
     * Create a default settings connection object.
     */
    public DatabaseConnectionSettings() {
        // init default driver with the first from the driver list
        // or use Java JDBC-ODBC as default
        String[] history = DRIVER_ORDER.getHistory();
        if (history != null && history.length > 0) {
            m_driver = history[0];
        } else {
            m_driver = DatabaseDriverLoader.JDBC_ODBC_DRIVER;
        }
        // create database name from driver class
        m_jdbcUrl = DatabaseDriverLoader.getURLForDriver(m_driver);
        m_allowSpacesInColumnNames = true;
        m_dbIdentifier = null;
    }

    /** Create a default database connection object.
     * @param driver the database driver
     * @param jdbcUrl database URL
     * @param user user name
     * @param pass password for user name
     * @param credName credential id from {@link CredentialsProvider} or null
     */
    @Deprecated
    public DatabaseConnectionSettings(final String driver, final String jdbcUrl,
            final String user, final String pass, final String credName) {
        this(driver, jdbcUrl, user, pass, credName, "none");
    }

    /** Create a default database connection object.
     * @param driver the database driver
     * @param jdbcUrl database URL
     * @param user user name
     * @param pass password for user name
     * @param credName credential id from {@link CredentialsProvider} or null
     * @param timezone the TimeZone to correct data/time/timestamp fields
     * @since 2.8
     */
    @Deprecated
    public DatabaseConnectionSettings(final String driver, final String jdbcUrl,
            final String user, final String pass, final String credName, final String timezone) {
//        this();
        this(null, driver, jdbcUrl, user, pass, credName, timezone);
    }

    /** Create a default database connection object.
     * @param dbIdentifier the unique database identifier used to retrieve the {@link DatabaseUtility} class
     * ( see {@link DatabaseUtility#getDatabaseIdentifier()}) or <code>null</code> if the second part of the JDBC url
     * should be used {@link #getDatabaseIdentifierFromJDBCUrl(String)}
     * @param driver the database driver
     * @param jdbcUrl database URL
     * @param user user name
     * @param pass password for user name
     * @param credName credential id from {@link CredentialsProvider} or null
     * @param timezone the TimeZone to correct data/time/timestamp fields
     * @since 2.11
     */
    public DatabaseConnectionSettings(final String dbIdentifier, final String driver, final String jdbcUrl,
            final String user, final String pass, final String credName, final String timezone) {
//        this();
        m_dbIdentifier = dbIdentifier;
        m_driver   = driver;
        m_jdbcUrl  = jdbcUrl;
        m_user     = user;
        m_pass     = pass;
        m_credName = credName;
        m_timezone = timezone;
    }

    /**
     * Creates and inits a new database configuration.
     * @param config to load
     * @param cp <code>CredentialProvider</code> used to get user name/password, may be <code>null</code>
     * @throws InvalidSettingsException if settings are invalid
     * @since 2.7
     */
    public DatabaseConnectionSettings(final ConfigRO config, final CredentialsProvider cp)
            throws InvalidSettingsException {
//        this();
        loadValidatedConnection(config, cp);
    }

    /**
     * Creates a new <code>DBConnection</code> based on the given connection
     * object.
     * @param conn connection used to copy settings from
     * @throws NullPointerException if the connection is null
     */
    public DatabaseConnectionSettings(final DatabaseConnectionSettings conn) {
        m_driver   = conn.m_driver;
        m_jdbcUrl  = conn.m_jdbcUrl;
        m_user     = conn.m_user;
        m_pass     = conn.m_pass;
        m_credName = conn.m_credName;
        m_timezone = conn.m_timezone;
        m_allowSpacesInColumnNames = conn.m_allowSpacesInColumnNames;
        m_rowIdsStartWithZero = conn.m_rowIdsStartWithZero;
        m_retrieveMetadataInConfigure = conn.m_retrieveMetadataInConfigure;
        m_dbIdentifier = conn.getDatabaseIdentifier();
        m_kerberos = conn.useKerberos();
    }

//    /** Map the keeps database connection based on the user and URL. */
//    private static final Map<ConnectionKey, Connection> CONNECTION_MAP =
//        Collections.synchronizedMap(new HashMap<ConnectionKey, Connection>());
//    /** Holding the database connection keys used to sync the open connection
//     * process. */
//    private static final Map<ConnectionKey, ConnectionKey>
//        CONNECTION_KEYS = new HashMap<ConnectionKey, ConnectionKey>();
//
//    private static final class ConnectionKey {
//        private final String m_un;
//        private final String m_pw;
//        private final String m_dn;
//        private ConnectionKey(final String userName, final String password,
//                final String databaseName) {
//            m_un = userName;
//            m_pw = password;
//            m_dn = databaseName;
//        }
//        /** {@inheritDoc} */
//        @Override
//        public boolean equals(final Object obj) {
//            if (obj == this) {
//                return true;
//            }
//            if (obj == null || !(obj instanceof ConnectionKey)) {
//                return false;
//            }
//            ConnectionKey ck = (ConnectionKey) obj;
//            if (!ConvenienceMethods.areEqual(this.m_un, ck.m_un)
//                  || !ConvenienceMethods.areEqual(this.m_pw, ck.m_pw)
//                  || !ConvenienceMethods.areEqual(this.m_dn, ck.m_dn)) {
//                return false;
//            }
//            return true;
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public int hashCode() {
//            return m_un.hashCode() ^ m_dn.hashCode();
//        }
//    }

    /** Create a database connection based on this settings. Note, don't close
     * the connection since it cached for subsequent calls or later reuse to
     * same database URL (under the same user name).
     * @return a new database connection object
     * @throws SQLException {@link SQLException}
     * @throws InvalidSettingsException {@link InvalidSettingsException}
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws IOException {@link IOException}
     * @deprecated use {@link #createConnection(CredentialsProvider)}
     */
    @Deprecated
    public Connection createConnection()
            throws InvalidSettingsException, SQLException,
            BadPaddingException, IllegalBlockSizeException,
            InvalidKeyException, IOException {
        return createConnection(null);
    }

    /** Create a database connection based on this settings. Note, don't close
     * the connection since it cached for subsequent calls or later reuse to
     * same database URL (under the same user name).
     * @return a new database connection object
     * @param cp {@link CredentialsProvider} provides user/password pairs
     * @throws SQLException {@link SQLException}
     * @throws InvalidSettingsException {@link InvalidSettingsException}
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws IOException {@link IOException}
     */
    public Connection createConnection(final CredentialsProvider cp)
            throws InvalidSettingsException, SQLException,
            BadPaddingException, IllegalBlockSizeException,
            InvalidKeyException, IOException {
        CheckUtils.checkSettingNotNull(m_driver, "No settings available to create database connection.");
        CheckUtils.checkSettingNotNull(m_jdbcUrl, "No JDBC URL set.");
        return getUtility().getConnectionFactory().getConnection(cp, this);
    }

    /**
     * Used to sync access to all databases depending if <code>SQL_CONCURRENCY</code> is true.
     * @param conn connection used to sync access to all databases
     * @return sync object which is either the given connection or an new object (no sync necessary)
     * @since 3.1
     */
    public final Object syncConnection(final Connection conn) {
        if (SQL_CONCURRENCY && conn != null) {
            return conn;
        } else {
            return new Object();
        }
    }

    /**
     * Save settings.
     * @param settings connection settings
     */
    public void saveConnection(final ConfigWO settings) {
        settings.addString("driver", m_driver);
        settings.addString("database", m_jdbcUrl);
        if (m_credName == null) {
            settings.addString("user", m_user);
            try {
                if (m_pass == null) {
                    settings.addString("password", null);
                } else {
                    settings.addString("password", KnimeEncryption.encrypt(
                            m_pass.toCharArray()));
                }
            } catch (Throwable t) {
                LOGGER.error("Could not encrypt password, reason: "
                        + t.getMessage(), t);
            }
        } else {
            settings.addString("credential_name", m_credName);
        }
        DRIVER_ORDER.add(m_driver);
        DATABASE_URLS.add(m_jdbcUrl);
        settings.addString("timezone", m_timezone);
        settings.addBoolean("validateConnection", m_validateConnection);
        settings.addBoolean("retrieveMetadataInConfigure", m_retrieveMetadataInConfigure);
        settings.addBoolean("allowSpacesInColumnNames", m_allowSpacesInColumnNames);
        settings.addBoolean("rowIdsStartWithZero", m_rowIdsStartWithZero);
        settings.addString("databaseIdentifier", m_dbIdentifier);
        settings.addBoolean("kerberos", m_kerberos);
    }

    /**
     * Validate settings.
     * @param settings to validate
     * @param cp <code>CredentialProvider</code> used to get user name/password
     * @throws InvalidSettingsException if the settings are not valid
     */
    public void validateConnection(final ConfigRO settings,
            final CredentialsProvider cp)
            throws InvalidSettingsException {
        // fix AP-5784 (Credentials validation in all database connect nodes happens too early during load phase)
        // passing null to the validation is better as the credentials provider may not be fully initialized
        // null provider = no password checking.
        loadConnection(settings, false, null);
    }

    /**
     * Load validated settings.
     * @param settings to load
     * @param cp <code>CredentialProvider</code> used to get user name/password, may be <code>null</code>
     * @return true, if settings have changed
     * @throws InvalidSettingsException if settings are invalid
     */
    public boolean loadValidatedConnection(final ConfigRO settings,
            final CredentialsProvider cp)
            throws InvalidSettingsException {
        return loadConnection(settings, true, cp);
    }

    private boolean loadConnection(final ConfigRO settings,
            final boolean write, final CredentialsProvider cp)
            throws InvalidSettingsException {
        if (settings == null) {
            throw new InvalidSettingsException("Connection settings not available!");
        }
        String driver = settings.getString("driver");
        String jdbcUrl = settings.getString("database");

        String user = "";
        String password = null;
        String credName = null;
        String timezone = settings.getString("timezone", "none");
        boolean validateConnection = settings.getBoolean("validateConnection", false);
        boolean retrieveMetadataInConfigure = settings.getBoolean("retrieveMetadataInConfigure", true);
        boolean allowSpacesInColumnNames = settings.getBoolean("allowSpacesInColumnNames", false);
        boolean rowIdsStartWithZero = settings.getBoolean("rowIdsStartWithZero", false);
        boolean kerberos = settings.getBoolean("kerberos", false);
        boolean useCredential = settings.containsKey("credential_name");
        if (useCredential) {
            credName = settings.getString("credential_name");
            if (cp != null) {
                try {
                    ICredentials cred = cp.get(credName);
                    user = cred.getLogin();
                    password = cred.getPassword();
                    if (password == null) {
                        LOGGER.warn("Credentials/Password has not been set, using empty password.");
                    }
                } catch (IllegalArgumentException e) {
                    if (!write) {
                        throw new InvalidSettingsException(e.getMessage());
                    }
                }
            }
        } else {
            // user and password
            user = settings.getString("user");
            try {
                String pw = settings.getString("password", "");
                if (pw != null) {
                    password = KnimeEncryption.decrypt(pw);
                }
            } catch (Exception e) {
                LOGGER.error("Password could not be decrypted, reason: " + e.getMessage());
            }
        }
        final String dbIdentifier = settings.getString("databaseIdentifier", null);
        // write settings or skip it
        if (write) {
            m_driver = driver;
            DRIVER_ORDER.add(m_driver);
            boolean changed = false;
            if (useCredential) {
                changed = (m_credName != null) && (credName != null) && credName.equals(m_credName);
                m_credName = credName;
            } else {
                if ((m_user != null) && (m_jdbcUrl != null) && (m_pass != null)) {
                    if (!m_user.equals(user) || !m_jdbcUrl.equals(jdbcUrl) || !m_pass.equals(password)) {
                        changed = true;
                    }
                }
                m_credName = null;
            }
            m_user = user;
            m_pass = (password == null ? "" : password);
            m_jdbcUrl = jdbcUrl;
            m_timezone = timezone;
            m_validateConnection = validateConnection;
            m_retrieveMetadataInConfigure = retrieveMetadataInConfigure;
            m_allowSpacesInColumnNames = allowSpacesInColumnNames;
            m_rowIdsStartWithZero = rowIdsStartWithZero;
            m_dbIdentifier = dbIdentifier;
            m_kerberos = kerberos;
            DATABASE_URLS.add(m_jdbcUrl);
            return changed;
        }
        return false;
    }

    /**
     * Execute statement on current database connection.
     * @param statement to be executed
     * @param cp {@link CredentialsProvider} providing user/password
     * @throws SQLException {@link SQLException}
     * @throws InvalidSettingsException {@link InvalidSettingsException}
     * @throws IllegalBlockSizeException {@link IllegalBlockSizeException}
     * @throws BadPaddingException {@link BadPaddingException}
     * @throws InvalidKeyException {@link InvalidKeyException}
     * @throws IOException {@link IOException}
     */
    @SuppressWarnings("resource")
    public void execute(final String statement, final CredentialsProvider cp)
                throws InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException,
            InvalidSettingsException,
            SQLException, IOException {
        Connection conn = createConnection(cp);
        synchronized (syncConnection(conn)) {
            try (final Statement stmt = conn.createStatement()) {
                LOGGER.debug("Executing SQL statement \"" + statement + "\"");
                stmt.execute(statement);
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
            } catch (SQLException ex) {
                try {
                    if ((conn != null) && !conn.getAutoCommit()) {
                        conn.rollback();
                    }
                } catch (SQLException ex2) {
                    // ignore this one and throw the original exception
                    LOGGER.debug("Caught another SQL exception while rolling back transaction: " + ex2.getMessage(), ex2);
                }
                throw ex;
            }
        }
    }

    private static final Set<Class<? extends Connection>> AUTOCOMMIT_EXCEPTIONS =
            Collections.synchronizedSet(new HashSet<Class<? extends Connection>>());
    /**
     * Calls {@link java.sql.Connection#setAutoCommit(boolean)} on the connection given the commit flag and catches
     * all <code>Exception</code>s, which is reported only once.
     * @param conn the Connection to call auto commit on.
     * @param commit the commit flag.
     * @since 3.1
     */
    public static void setAutoCommit(final Connection conn, final boolean commit) {
        try {
            conn.setAutoCommit(commit);
        } catch (Exception e) {
            if (AUTOCOMMIT_EXCEPTIONS.add(conn.getClass())) {
                LOGGER.debug(conn.getClass() + "#setAutoCommit(" + commit + ") error, reason: ", e);
            }
        }
    }

    /**
     * Create connection model with all settings used to create a database
     * connection.
     * @return database connection model
     */
    public ModelContentRO createConnectionModel() {
        ModelContent cont = new ModelContent("database_connection_model");
        saveConnection(cont);
        return cont;
    }

    /**
     * @return database driver used to open the connection
     */
    public final String getDriver() {
        return m_driver;
    }

    /**
     * @return database name used to access the database URL
     * @deprecated use {@link #getJDBCUrl()} instead
     */
    @Deprecated
    public final String getDBName() {
        return m_jdbcUrl;
    }

    /**
     * Returns the unique database identifier. Never returns <code>null</code>. if the database identifier
     * is not specified the method return the identifier based on the url.
     * @return the dbIdentifier the unique database identifier usually the second part of the JDBC url
     * @see #getDatabaseIdentifierFromJDBCUrl(String)
     * @see #setDatabaseIdentifier(String)
     * @since 2.11
     */
    public String getDatabaseIdentifier() {
        if (m_dbIdentifier == null) {
            return getDatabaseIdentifierFromJDBCUrl(m_jdbcUrl);
        }
        return m_dbIdentifier;
    }

    /**
     * @return the value of the database identifier. Might be <code>null</code> id the identifier should be
     * determined based on the connection url.
     * @see #getDatabaseIdentifierFromJDBCUrl(String)
     * @since 2.11
     */
    public String getDatabaseIdentifierValue() {
        return m_dbIdentifier;
    }

    /**
     * @param databaseIdentifier the unique database identifier if <code>null</code> the second part of the JDBC url
     * is used (see {@link #getDatabaseIdentifierFromJDBCUrl(String)}.
     * @since 2.11
     */
    public void setDatabaseIdentifier(final String databaseIdentifier) {
        m_dbIdentifier = databaseIdentifier;
    }

    /**
     * Returns the JDBC URL for the database.
     *
     * @return a JDBC URL
     * @since 2.10
     */
    public final String getJDBCUrl() {
        return m_jdbcUrl;
    }

    /**
     * @param cp {@link CredentialsProvider}
     * @return user name used to login to the database
     */
    public String getUserName(final CredentialsProvider cp) {
        if (cp == null || m_credName == null) {
            return m_user;
        } else {
            ICredentials cred = cp.get(m_credName);
            return cred.getLogin();
        }
    }

    /**
     * @param cp {@link CredentialsProvider}
     * @return password (decrypted) used to login to the database
     */
    public String getPassword(final CredentialsProvider cp) {
        if (cp == null || m_credName == null) {
            return m_pass;
        } else {
            ICredentials cred = cp.get(m_credName);
            return cred.getPassword();
        }
    }

    /** @return the TimeZone.
     * @since 2.8
     */
    public final TimeZone getTimeZone() {
        if (m_timezone.equals("none")) {
            return DateAndTimeCell.UTC_TIMEZONE;
        } else if (m_timezone.equals("current")) {
            return TimeZone.getDefault();
        } else {
            return TimeZone.getTimeZone(m_timezone);
        }
    }

    /** @return the TimeZone correction, offset in milli seconds.
     * @param date in the current date to compute the offset for
     * @since 2.8
     */
    public final long getTimeZoneOffset(final long date) {
        return getTimeZone().getOffset(date);
    }

    /**
     * @param useKerberos <code>true</code> if Kerberos should be used
     * @since 3.2
     */
    public void setKerberos(final boolean useKerberos) {
        m_kerberos = useKerberos;
    }

    /**
     * @return <code>true</code> if Kerberos should be used
     * @since 3.2
     */
    public boolean useKerberos() {
        return m_kerberos;
    }

    /**
     * @return user name used to login to the database
     * @deprecated use {@link #getUserName(CredentialsProvider)}
     */
    @Deprecated
    public String getUserName() {
        return getUserName(null);
    }

    /**
     * @return password (decrypted) used to login to the database
     * @deprecated use {@link #getPassword(CredentialsProvider)}
     */
    @Deprecated
    public String getPassword() {
        return getPassword(null);
    }


    /**
     * Set a new database driver.
     * @param driver used to open the connection
     */
    public final void setDriver(final String driver) {
        m_driver = driver;
    }

    /**
     * Set a new database name.
     * @param databaseName used to access the database URL
     * @deprecated use {@link #setJDBCUrl(String)} instead
     */
    @Deprecated
    public final void setDBName(final String databaseName) {
        m_jdbcUrl = databaseName;
    }

    /**
     * Sets the JDBC URL for the database.
     *
     * @param url a JDBC URL
     * @since 2.10
     */
    public final void setJDBCUrl(final String url) {
        m_jdbcUrl = url;
    }


    /**
     * Set a new user name.
     * @param userName used to login to the database
     */
    public final void setUserName(final String userName) {
        m_user = userName;
    }

    /**
     * Set a new password.
     * @param password (decrypted) used to login to the database
     */
    public final void setPassword(final String password) {
        m_pass = password;
    }

    /**
     * Returns the name of the credential entry that should be used. If <code>null</code> is returned username and
     * password should be used instead.
     *
     * @return a credential identifier or <code>null</code>
     * @since 2.10
     */
    public String getCredentialName() {
        return m_credName;
    }

    /**
     * Returns the name of the credential entry that should be used. If it is set to <code>null</code> username and
     * password should be used instead.
     *
     * @param name a credential identifier or <code>null</code>
     * @since 2.10
     */
    public final void setCredentialName(final String name) {
        m_credName = name;
    }


    /**
     * Returns the manually set timezone that should be assumed for dates returned by the database. If the timezone is
     * set to <tt>current</tt> the client's local timezone should be used. If the timezone is set to <tt>none</tt> no
     * correction is applied
     *
     * @return a timezone identifier, <tt>current</tt>, or <tt>none</tt>
     * @since 2.10
     */
    public final String getTimezone() {
        return m_timezone;
    }

    /**
     * Sets the manually set timezone that should be assumed for dates returned by the database. If the timezone is
     * set to <tt>current</tt> the client's local timezone should be used. If the timezone is set to <tt>none</tt> no
     * correction is applied
     *
     * @param tz a timezone identifier, <tt>current</tt>, or <tt>none</tt>
     * @since 2.10
     */
    public void setTimezone(final String tz) {
        m_timezone = tz;
    }


    /**
     * Returns whether the connection should be validated by dialogs.
     *
     * @return <code>true</code> if the connection should be validated, <code>false</code> otherwise
     * @since 2.10
     */
    public final boolean getValidateConnection() {
        return m_validateConnection;
    }

    /**
     * Sets whether the connection should be validated by dialogs.
     *
     * @param b <code>true</code> if the connection should be validated, <code>false</code> otherwise
     * @since 2.10
     */
    public final void setValidateConnection(final boolean b) {
        m_validateConnection = b;
    }


    /**
     * Returns whether the metadata for the current query should be retrieved during configure.
     *
     * @return <code>true</code> if metadata should be retrieved, <code>false</code> otherwise
     * @since 2.10
     */
    public final boolean getRetrieveMetadataInConfigure() {
        return m_retrieveMetadataInConfigure;
    }

    /**
     * Sets whether the metadata for the current query should be retrieved during configure.
     *
     * @param b <code>true</code> if metadata should be retrieved, <code>false</code> otherwise
     * @since 2.10
     */
    public final void setRetrieveMetadataInConfigure(final boolean b) {
        m_retrieveMetadataInConfigure = b;
    }


    /**
     * Returns whether spaces in columns names are allowed and passed on to the database. If spaces are not allowed
     * they will be replaced.
     *
     * @return <code>true</code> if spaces are allowed, <code>false</code> otherwise
     * @since 2.10
     */
    public boolean getAllowSpacesInColumnNames() {
        return m_allowSpacesInColumnNames;
    }


    /**
     * Sets whether spaces in columns names are allowed and passed on to the database. If spaces are not allowed
     * they will be replaced.
     *
     * @param allow <code>true</code> if spaces are allowed, <code>false</code> otherwise
     * @since 2.10
     */
    public void setAllowSpacesInColumnNames(final boolean allow) {
        m_allowSpacesInColumnNames = allow;
    }


    /**
     * Returns whether row IDs returned by a database reader should start with zero (correct behavior) or with
     * (backward compatibility with pre 2.10). The default is <code>false</code>.
     *
     * @return <code>true</code> if row ids should start with 0, <code>false</code> if they should start with 1
     * @since 2.10
     */
    public boolean getRowIdsStartWithZero() {
        return m_rowIdsStartWithZero;
    }


    /**
     * Sets whether row IDs returned by a database reader should start with zero (correct behavior) or with
     * (backward compatibility with pre 2.10). This should only be enabled for nodes that did not exist before 2.10.
     *
     * @param b <code>true</code> if row ids should start with 0, <code>false</code> if they should start with 1
     * @since 2.10
     */
    public void setRowIdsStartWithZero(final boolean b) {
        m_rowIdsStartWithZero = b;
    }


    /**
     * Returns a utility implementation for the current database.
     *
     * @return a statement manipulator
     * @since 2.10
     */
    public DatabaseUtility getUtility() {
        return  DatabaseUtility.getUtility(getDatabaseIdentifier());
    }

    /**
     * @param jdbcUrl the JDBC url must not be <code>null</code>
     * @return the database identifier based on the given JDBC url
     * @since 2.11
     */
    public static String getDatabaseIdentifierFromJDBCUrl(final String jdbcUrl) {
        final String[] parts = jdbcUrl.split(":");
        if ((parts.length < 2) || !"jdbc".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid JDBC URL in settings: " + jdbcUrl);
        }
        return parts[1];
    }
}

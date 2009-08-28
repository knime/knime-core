/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.node.port.database;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.StringHistory;

/**
 * Utility class to load additional drivers from jar and zip to the
 * <code>DriverManager</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DatabaseDriverLoader {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(DatabaseDriverLoader.class);
    
    /** Map from driver file to driver class. */
    private static final Map<String, File> DRIVERFILE_TO_DRIVERCLASS
        = new LinkedHashMap<String, File>();
    
    /**
     * Name of the standard JDBC-ODBC database driver, 
     * <i>sun.jdbc.odbc.JdbcOdbcDriver</i> object. Loaded per default.
     */
    static final String JDBC_ODBC_DRIVER = "sun.jdbc.odbc.JdbcOdbcDriver";
    
    private static final Map<String, String> DRIVER_TO_URL
        = new LinkedHashMap<String, String>();
    
    static {
        DRIVER_TO_URL.put(JDBC_ODBC_DRIVER, "jdbc:odbc:");
        DRIVER_TO_URL.put("com.ibm.db2.jcc.DB2Driver", "jdbc:db2:");
        DRIVER_TO_URL.put("org.firebirdsql.jdbc.FBDriver", "jdbc:firebirdsql:");
        DRIVER_TO_URL.put("com.mysql.jdbc.Driver", "jdbc:mysql:");
        DRIVER_TO_URL.put(
                "oracle.jdbc.driver.OracleDriver", "jdbc:mysql:thin:");
        DRIVER_TO_URL.put("org.postgresql.Driver", "jdbc:postgresql:");
        DRIVER_TO_URL.put("com.microsoft.sqlserver.jdbc.SQLServerDriver", 
                "jdbc:sqlserver:");
        DRIVER_TO_URL.put("com.microsoft.jdbc.sqlserver.SQLServerDriver", 
                "jdbc:microsoft:sqlserver:");
        DRIVER_TO_URL.put("org.apache.derby.jdbc.ClientDriver", "jdbc:derby:");
        DRIVER_TO_URL.put("jdbc.FrontBase.FBJDriver", "jdbc:FrontBase:");
        DRIVER_TO_URL.put("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql:");
        DRIVER_TO_URL.put("com.ingres.jdbc.IngresDriver", "jdbc:ingres:");
        DRIVER_TO_URL.put("com.openbase.jdbc.ObDriver", "jdbc:openbase:");
        DRIVER_TO_URL.put("net.sourceforge.jtds.jdbc.Driver", 
                "jdbc:jtds:sybase:");
        DRIVER_TO_URL.put("com.sybase.jdbc3.jdbc.SybDriver", 
                "jdbc:sybase:Tds:");
        DRIVER_TO_URL.put("org.sqlite.JDBC", "jdbc:sqlite:");
    }

    /**
     * Allowed file extensions, jar and zip only.
     */
    public static final String[] EXTENSIONS = {".jar", ".zip"};

    private static final ClassLoader CLASS_LOADER = ClassLoader
            .getSystemClassLoader();
    
    private static final Map<String, WrappedDriver> DRIVER_MAP
        = new HashMap<String, WrappedDriver>();
    
    /**
     * Keeps history of loaded driver libraries.
     */
    private static final StringHistory DRIVER_LIBRARY_HISTORY =
        StringHistory.getInstance("database_library_files");
    
    /**
     * Register Java's jdbc-odbc bridge.
     */
    static {
        try {
            Class<?> driverClass = Class.forName(JDBC_ODBC_DRIVER);
            WrappedDriver d = new WrappedDriver((Driver)driverClass
                    .newInstance());
            // DriverManager.registerDriver(d);
            DRIVER_MAP.put(d.toString(), d);
        } catch (Throwable t) {
            LOGGER.warn("Could not load driver class \"" 
                    + JDBC_ODBC_DRIVER + "\".", t);
        }
    }
    
    /**
     * Init driver history on start-up.
     */
    static {
    	// load all drivers from history file
        for (String hist : DRIVER_LIBRARY_HISTORY.getHistory()) {
            try {
                File histFile = new File(hist);
                loadDriver(histFile);
            } catch (Throwable t) {
                LOGGER.info("Could not load driver library file \"" 
                        + hist + "\" from history.", t);
            }
        }
        // load additional driver from Java property
        String[] dbDrivers = System.getProperty(
        		KNIMEConstants.DATABASE_DRIVER_FILES).split(";");
        for (String driver : dbDrivers) {
            try {
                File driverFile = new File(driver);
                loadDriver(driverFile);
            } catch (Throwable t) {
                LOGGER.info("Could not load driver library file \"" 
                        + driver + "\" from history.", t);
            }
        }
    }

    /**
     * Hide (empty) constructor.
     *
     */
    private DatabaseDriverLoader() {

    }
    
    /**
     * Registers given <code>Driver</code> at the <code>DriverManager</code>.
     * @param driver to register
     * @return SQL Driver 
     * @throws InvalidSettingsException if the database drivers could not
     *             registered
     */
    static Driver registerDriver(final String driver) 
            throws InvalidSettingsException {
        try {
            Driver wrappedDriver = 
                DatabaseDriverLoader.getWrappedDriver(driver);
            DriverManager.registerDriver(wrappedDriver);
            return wrappedDriver;
        } catch (Throwable t) {
            throw new InvalidSettingsException("Could not register database"
                  + " driver \"" + driver + "\", reason: " + t.getMessage(), t);
        }
    }

    /**
     * Loads <code>Driver</code> from the given file.
     * 
     * @param file Load driver from.
     * @throws IOException {@link IOException}
     */
    public static final void loadDriver(final File file) throws IOException {
        if (file.isDirectory()) {
            // not yet supported
            return;
        }
        if (DRIVERFILE_TO_DRIVERCLASS.containsValue(file)) {
        	return;
        }
        final String fileName = file.getAbsolutePath();
        if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
            readZip(file, new JarFile(file));
            DRIVER_LIBRARY_HISTORY.add(fileName);
        }
    }

    private static void readZip(final File file, final ZipFile zipFile) 
            throws MalformedURLException {
        final ClassLoader cl = new URLClassLoader(
                new URL[]{file.toURI().toURL()}, CLASS_LOADER);
        for (Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            zipEntries.hasMoreElements();) {
            ZipEntry e = zipEntries.nextElement();
            if (e == null) {
                continue;
            }
            String name = e.getName();
            if (name.endsWith(".class")) {
                try {
                    Class<?> c = loadClass(cl, name);
                    if (Driver.class.isAssignableFrom(c)) {
                        WrappedDriver d = new WrappedDriver(
                                (Driver)c.newInstance());
                        String driverName = d.toString();
                        DRIVER_MAP.put(driverName, d);
                        DRIVERFILE_TO_DRIVERCLASS.put(driverName, file);
                    }
                } catch (Throwable t) {
                    // ignored
                }
            }
        }
    }
    
    /**
     * @return A set if loaded driver names.
     */
    public static Set<String> getLoadedDriver() {
        return DRIVER_MAP.keySet();
    }

    /**
     * Get driver for name.
     * @param driverName The driver name.
     * @return The <code>WrappedDriver</code> for the given driver name.
     */
    public static WrappedDriver getWrappedDriver(final String driverName) {
        return DRIVER_MAP.get(driverName);
    }

    private static Class<?> loadClass(final ClassLoader cl, final String name) 
            throws ClassNotFoundException {
        String newName = name.substring(0, name.indexOf(".class"));
        String className = newName.replace('/', '.');
        return cl.loadClass(className);
    }
    
    /**
     * Returns a URL protocol for a given <code>Driver</code> extended by 
     * an default host, port, database name String. If no protocol URL has been
     * defined the default String staring with protocol is return.
     * @param driver the driver to match URL protocol
     * @return an String containing protocol, port, host, and database name
     *      place holder 
     */
    public static final String getURLForDriver(final String driver) {
        String url = DRIVER_TO_URL.get(driver);
        if (url == null) {
            return "<protocol>://<host>:<port>/<database_name>";
        }
        return url + "//<host>:<port>/<database_name>";
    }
    
    /**
     * Returns the absolute path for the driver class name from which it has
     * been loaded.
     * @param driverClass driver class name
     * @return driver file location
     */
    public static final File getDriverFileForDriverClass(
            final String driverClass) {
        return DRIVERFILE_TO_DRIVERCLASS.get(driverClass);
    }
    
    /**
     * Wraps a Driver object.
     * 
     * @author Thomas Gabriel, University of Konstanz
     */
    private static final class WrappedDriver implements Driver {
        private final Driver m_d;

        /**
         * Create wrapper.
         * 
         * @param d For this <code>Driver</code>.
         */
        WrappedDriver(final Driver d) {
            m_d = d;
        }

        /**
         * {@inheritDoc}
         */
        public Connection connect(final String url, final Properties info)
                throws SQLException {
            return m_d.connect(url, info);
        }

        /**
         * {@inheritDoc}
         */
        public boolean acceptsURL(final String url) throws SQLException {
            return m_d.acceptsURL(url);
        }

        /**
         * {@inheritDoc}
         */
        public DriverPropertyInfo[] getPropertyInfo(final String url,
                final Properties info) throws SQLException {
            return m_d.getPropertyInfo(url, info);
        }

        /**
         * {@inheritDoc}
         */
        public int getMajorVersion() {
            return m_d.getMajorVersion();
        }

        /**
         * {@inheritDoc}
         */
        public int getMinorVersion() {
            return m_d.getMinorVersion();
        }

        /**
         * {@inheritDoc}
         */
        public boolean jdbcCompliant() {
            return m_d.jdbcCompliant();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_d.getClass().getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return m_d.hashCode();
        }
    }
    
}

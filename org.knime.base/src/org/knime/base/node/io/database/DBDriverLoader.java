/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * History
 *   06.07.2006 (gabriel): created
 */
package org.knime.base.node.io.database;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * Utility class to load additional drivers from jar and zip to the
 * <code>DriverManager</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBDriverLoader {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(DBDriverLoader.class);

    /**
     * Allowed file extensions, jar and zip only.
     */
    static final String[] EXTENSIONS = {".jar", ".zip"};

    private static final ClassLoader CLASS_LOADER = ClassLoader
            .getSystemClassLoader();
    
    private static final Map<String, WrappedDriver> DRIVER_MAP
        = new HashMap<String, WrappedDriver>();
    
    /**
     * Name of the standard JDBC-ODBC database driver, 
     * <i>sun.jdbc.odbc.JdbcOdbcDriver</i> object. Loaded per default.
     */
    static final String JDBC_ODBC_DRIVER = "sun.jdbc.odbc.JdbcOdbcDriver";
    
    static {
        try {
            Class<?> driverClass = Class.forName(JDBC_ODBC_DRIVER);
            WrappedDriver d = new WrappedDriver((Driver)driverClass
                    .newInstance());
            // DriverManager.registerDriver(d);
            DRIVER_MAP.put(d.toString(), d);
        } catch (Exception e) {
            LOGGER.warn("Could not load driver class: " + JDBC_ODBC_DRIVER);
            LOGGER.debug("", e);
        }
    }

    private DBDriverLoader() {

    }
    
    /**
     * Registers given <code>Driver</code> at the <code>DriverManager</code>.
     * @param driver to register
     * @throws InvalidSettingsException if the database drivers could not
     *             registered
     */
    static void registerDriver(final String driver) 
        throws InvalidSettingsException {
        try {
            DriverManager.registerDriver(DBDriverLoader
                    .getWrappedDriver(driver));
        } catch (Exception e) {
            throw new InvalidSettingsException("Could not register database"
                    + " driver: " + driver);
        }
    }

    /**
     * Loads <code>Driver</code> from the given file.
     * 
     * @param file Load driver from.
     * @throws Exception If an exception occurs.
     */
    static final void loadDriver(final File file) throws Exception {
        String fileName = file.getAbsolutePath();
        if (file.isDirectory()) {
            // not yet supported
        } else if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
            readZip(file, new JarFile(file));
        }
    }

    private static void readZip(final File file, final ZipFile zipFile) 
            throws Exception {
        final ClassLoader cl = new URLClassLoader(
                new URL[]{file.toURL()}, CLASS_LOADER);
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
                        DRIVER_MAP.put(d.toString(), d);
//                        DriverManager.registerDriver(d);
                    }
                } catch (Exception ex) {
                    // ignore
                } catch (Error er) {
                    // ignore
                }
            }
        }
    }
    
    /**
     * @return A set if loaded driver names.
     */
    static Set<String> getLoadedDriver() {
        return DRIVER_MAP.keySet();
    }

    /**
     * Get driver for name.
     * @param driverName The driver name.
     * @return The <code>WrappedDriver</code> for the given driver name.
     */
    static WrappedDriver getWrappedDriver(final String driverName) {
        return DRIVER_MAP.get(driverName);
    }

    private static Class<?> loadClass(final ClassLoader cl, final String name)
            throws Exception, Error {
        String newName = name.substring(0, name.indexOf(".class"));
        String className = newName.replace('/', '.');
        return cl.loadClass(className);
    }
}

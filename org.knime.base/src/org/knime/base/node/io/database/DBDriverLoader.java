/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utility class to load additional drivers from jar and zip to the
 * <code>DriverManager</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBDriverLoader {

    /**
     * Allowsed file extensions, jar and zip only.
     */
    static final String[] EXTENSIONS = {".jar", ".zip"};

    private static final ClassLoader CLASS_LOADER = ClassLoader
            .getSystemClassLoader();

    private DBDriverLoader() {

    }

    /**
     * Loads <code>Driver</code> from the given file.
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
        for (Enumeration zipEntries = zipFile.entries(); zipEntries
                .hasMoreElements();) {
            ZipEntry e = (ZipEntry)zipEntries.nextElement();
            if (e == null) {
                continue;
            }
            String name = e.getName();
            if (name.endsWith(".class")) {
                try {
                    readFile(file, name);
                } catch (Exception ex) {
                    // ignore
                } catch (Error er) {
                    // ignore
                }
            }
        }
    }
    
    private static void readFile(final File file, final String name)
            throws Exception, Error {
        String newName = name.substring(0, name.indexOf(".class"));
        String className = newName.replace('/', '.');
        ClassLoader cl = new URLClassLoader(new URL[]{file.toURL()},
                CLASS_LOADER);
        Class c = cl.loadClass(className);
        if (Driver.class.isAssignableFrom(c)) {
            try {
                Driver theDriver = new WrappedDriver((Driver)c.newInstance());
                // if (theDriver.jdbcCompliant()) {
                DriverManager.registerDriver(theDriver);
                assert DriverManager.getDriver(className) == theDriver;
            } catch (Exception allE) {
                // ignore
            }
        }
    }
}

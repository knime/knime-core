/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
package org.knime.base.node.io.database;

import java.awt.Component;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import javax.swing.JFileChooser;

/**
 * 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBDriverLoaderTest {

    private DBDriverLoaderTest() throws Exception {
        JFileChooser chooser = new JFileChooser();
        int ret = chooser.showOpenDialog((Component)null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            DBDriverLoader.loadDriver(chooser.getSelectedFile());
        }
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            System.out.println(d + " jdbc: " + d.jdbcCompliant());
        }
    }

    /**
     * 
     * @param args The args.
     * @throws Exception If happening.
     */
    public static void main(final String[] args) throws Exception {
        new DBDriverLoaderTest();
    }
}

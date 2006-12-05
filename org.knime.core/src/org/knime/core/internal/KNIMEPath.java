/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Oct 13, 2006 (wiswedel): created
 */
package org.knime.core.internal;

import java.io.File;

import org.knime.core.node.KNIMEConstants;

/**
 * Container for a field, which holds the home directory of KNIME. This class
 * serves as an abstraction layer to bridge from eclipse to non-GUI KNIME.
 * During startup of eclipse, the home directory is set in this class, which is
 * polled from the KNIMEConstants class to do the final initialization.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class KNIMEPath {

    private static File homeDir;

    /**
     * Constructor which package scope.
     * 
     * @param file The file to use as home dir (should be a directory).
     */
    private KNIMEPath() {
    }

    /**
     * Set the knime home dir.
     * 
     * @param file The file to use as home dir (should be a directory).
     */
    static void setKNIMEHomeDir(final File file) {
        homeDir = file.getAbsoluteFile();
    }

    /**
     * Getter for the home dir of KNIME.
     * 
     * @return The directory to use.
     */
    public static File getKNIMEHomeDirPath() {
        String parent;
        String child;

        if (homeDir == null) {
            // see if the home dir got set through a command line argument
            if (System.getProperty(KNIMEConstants.KNIME_HOME_PROPERTYNAME) 
                    != null) {
                homeDir =
                        new File(System.getProperty(
                                KNIMEConstants.KNIME_HOME_PROPERTYNAME));
                parent = homeDir.getParentFile().getAbsolutePath();
                child = homeDir.getName();
            } else {
                // if no home dir is specified use "~user/knime"
                parent = System.getProperty("user.home");
                child = "knime";
                homeDir = new File(parent, child);
            }

        } else {
            parent = homeDir.getParentFile().getAbsolutePath();
            child = homeDir.getName();
        }

        while (homeDir.exists() && !homeDir.isDirectory()) {
            child = child.concat("_");
            homeDir = new File(parent, child);
        }
        if (!homeDir.exists()) {

            if (!homeDir.mkdirs()) {

                // if creation of home dir failed, try the ~user/knime
                homeDir = new File(System.getProperty("user.home"), "knime");
                if (!homeDir.exists()) {
                    homeDir.mkdir();
                }
            }
        }
        return homeDir;
    }
}

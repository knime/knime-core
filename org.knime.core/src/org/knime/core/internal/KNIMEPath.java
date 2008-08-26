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
    private static File workspaceDir;

    /**
     * Disallow instantiation.
     */
    private KNIMEPath() {
    }

    /**
     * Set the workspace directory.
     * @param file The directory of the workspace.
     */
    static void setWorkspaceDir(final File file) {
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalArgumentException("KNIME workspace path " 
                        + "is not a directory: " + file.getAbsolutePath());
            }
            if (!file.canWrite()) {
                throw new IllegalArgumentException("Unable to write to " 
                        + "workspace path: " + file.getAbsolutePath());
            }
        } else {
            if (!file.mkdirs()) {
                throw new IllegalArgumentException("Unable to create workspace "
                        + "directory " + file.getAbsolutePath());
            }
        }
        workspaceDir = file.getAbsoluteFile();
    }
    
    /**
     * Getter for the workspace directory.
     * 
     * @return The workspace directory.
     */
    public static File getWorkspaceDirPath() {
        if (workspaceDir == null) {
            initDefaultDir();
        }
        return workspaceDir;
    }
    
    /**
     * Set the knime home dir.
     * @param file The file to use as home dir (should be a directory).
     */
    static void setKNIMEHomeDir(final File file) {
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalArgumentException("KNIME home path is not a " 
                        + "directory: " + file.getAbsolutePath());
            }
            if (!file.canWrite()) {
                throw new IllegalArgumentException("Unable to write to " 
                        + "KNIME home: " + file.getAbsolutePath());
            }
        } else {
            if (!file.mkdirs()) {
                throw new IllegalArgumentException("Unable to create KNIME "
                        + "home directory " + file.getAbsolutePath());
            }
        }
        homeDir = file.getAbsoluteFile();
    }

    /**
     * Getter for the home dir of KNIME.
     * 
     * @return The directory to use.
     */
    public static File getKNIMEHomeDirPath() {
        if (homeDir == null) {
            initDefaultDir();
        }
        return homeDir;
    }
    
    private static void initDefaultDir() {
        // see if the home dir got set through a command line argument
        String knimePropertyHome =
            System.getProperty(KNIMEConstants.KNIME_HOME_PROPERTYNAME);
        File tempHomeDir = null;
        if (knimePropertyHome != null) {
            tempHomeDir = createUniqueDir(new File(knimePropertyHome));
        }
        if (tempHomeDir == null) {
            File userHome = new File(System.getProperty("user.home"));
            tempHomeDir = createUniqueDir(new File(userHome, "knime"));
            if (tempHomeDir == null) {
                tempHomeDir = userHome;
            }
        }
        homeDir = tempHomeDir;
    }
    
    private static File createUniqueDir(final File file) {
        final String parent = file.getParentFile().getAbsolutePath();
        String child = file.getName();
        File temp = new File(parent, child);
        if (temp.exists() && !temp.isDirectory()) {
            // Do not use logger here as it is not yet available!
            System.err.println("Unable to set KNIME home directory to \""
                    + file.getAbsolutePath() + "\"");
            while (temp.exists() && !temp.isDirectory()) {
                child = child.concat("_");
                temp = new File(parent, child);
            }
        }
        if (!temp.exists() && !temp.mkdirs()) {
            return null;
        }
        return temp;
    }
}

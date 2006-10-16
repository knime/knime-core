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

/**
 * Container for a field, which holds the home directory of KNIME. This
 * class serves as an abstraction layer to brigde from eclipse to non-GUI KNIME.
 * During startup of eclipse, the home dir is set in this class, which 
 * is polled from the KNIMEConstants class to do the final initialisation.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class KNIMEPath {

    private static File homeDir;

    /** Constructor which package scope. 
    * @param file The file to use as home dir (should be a directory).
    */
    private KNIMEPath() {
    }
    
    /** Set the knime home dir. 
     * @param file The file to use as home dir (should be a directory).
     */
    static void setKNIMEHomeDir(final File file) {
        homeDir = file.getAbsoluteFile();
    }
    
    /** Getter for the home dir of KNIME.
     * @return The directory to use.
     */
    public static File getKNIMEHomeDirPath() {
        String parent = System.getProperty("user.dir");
        String child = "knime";
        if (homeDir == null) {
            homeDir = new File(parent, child);
        } else {
            parent = homeDir.getParentFile().getAbsolutePath();
            child = homeDir.getName();
        }
        while (homeDir.exists() && !homeDir.isDirectory()) {
            child = child.concat("_");
            homeDir = new File(parent, child);
        }
        if (!homeDir.exists()) {
            homeDir.mkdir();
        }
        return homeDir;
    }
}

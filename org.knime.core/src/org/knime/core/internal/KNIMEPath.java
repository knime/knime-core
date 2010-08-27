/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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

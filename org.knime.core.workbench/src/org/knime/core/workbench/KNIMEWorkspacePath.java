/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
package org.knime.core.workbench;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;

import org.knime.core.node.util.CheckUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for a field that holds the home directory of KNIME. This class serves as an abstraction layer to bridge
 * from eclipse to non-GUI KNIME. During startup of eclipse, the home directory is set in this class, which is polled
 * from the KNIMEConstants class to do the final initialization.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @since 5.8
 */
// this class was moved/copied from org.knime.core.internal.KNIMEPath in version 5.8 (AP-24828)
public final class KNIMEWorkspacePath {

    private static final Logger LOGGER = LoggerFactory.getLogger(KNIMEWorkspacePath.class);

    /**
     * The name of the system property whose value is - if set - used as knime
     * home directory. If no (or an invalid) value is set, ~user/knime will be
     * used instead. To set the knime home dir from the command line, use
     * -Dknime.home=&lt;absolutePathToNewHomeDir&gt;.
     */
    public static final String PROPERTY_KNIME_HOME = "knime.home";

    private static File knimeHomeDir;
    private static File workspaceDir;

    /**
     * Disallow instantiation.
     */
    private KNIMEWorkspacePath() {
    }


    static {
        try {
            Class.forName("org.eclipse.core.runtime.Platform");
            initializePaths();
        } catch (ClassNotFoundException e) {
            // this only happens in a non-Eclipse OSGi environment
        }
    }


    private static void initializePaths() {
        try {
            URL workspaceURL = org.eclipse.core.runtime.Platform.getInstanceLocation().getURL();
            if (workspaceURL.getProtocol().equalsIgnoreCase("file")) {
                // we can create our home only in local workspaces
                File wsDir = Paths.get(workspaceURL.toURI()).toFile();
                if (!wsDir.exists() && !wsDir.mkdirs()) {
                    throw new IllegalArgumentException("Unable to create workspace directory "
                        + wsDir.getAbsolutePath());
                }
                if (!wsDir.isDirectory()) {
                    throw new IllegalArgumentException("KNIME workspace " + wsDir.getAbsolutePath()
                        + " is not a directory");
                }
                if (!wsDir.canWrite()) {
                    throw new IllegalArgumentException("Unable to write to workspace directory at "
                        + wsDir.getAbsolutePath());
                }
                workspaceDir = wsDir;

                File homeDir = new File(wsDir, ".metadata" + File.separator + "knime");
                if (!homeDir.exists() && !homeDir.mkdirs()) {
                    throw new IllegalArgumentException("Unable to create KNIME home directory "
                        + homeDir.getAbsolutePath());
                }
                if (!homeDir.isDirectory()) {
                    throw new IllegalArgumentException("KNIME home path " + homeDir.getAbsolutePath()
                        + " is not a directory");
                }
                if (!wsDir.canWrite()) {
                    throw new IllegalArgumentException("Unable to write to KNIME home directory at "
                        + homeDir.getAbsolutePath());
                }
                knimeHomeDir = homeDir;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize workspace paths", e);
        }
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
     * Sets a custom workspace directory path. Only for testing purposes and will fail if not called from a
     * workflow-test!
     *
     * @param wsDir the workspace directory or <code>null</code> if it should be reset to the default one
     */
    public static void setWorkspaceDirPath(final File wsDir) {
        final String allowedTestClass = "org.knime.testing.core.ng.WorkflowTest";
        // precaution to ensure that this is really only be called from a test case
        CheckUtils.checkState(
            Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(e -> e.getClassName().equals(allowedTestClass)),
            "A custom workspace directory is only allowed to be set by the %s", allowedTestClass);

        if (wsDir == null) {
            initializePaths();
        } else {
            workspaceDir = wsDir;
        }
    }

    /**
     * Getter for the home dir of KNIME.
     *
     * @return The directory to use.
     */
    public static File getKNIMEHomeDirPath() {
        if (knimeHomeDir == null) {
            initDefaultDir();
        }
        return knimeHomeDir;
    }

    private static void initDefaultDir() {
        // see if the home dir got set through a command line argument
        String knimePropertyHome = System.getProperty(PROPERTY_KNIME_HOME);
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
        knimeHomeDir = tempHomeDir;
    }

    private static File createUniqueDir(final File file) {
        final String parent = file.getParentFile().getAbsolutePath();
        String child = file.getName();
        File temp = new File(parent, child);
        if (temp.exists() && !temp.isDirectory()) {
            LOGGER.debug("Unable to set KNIME home directory to \"{}\"", file.getAbsolutePath());
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

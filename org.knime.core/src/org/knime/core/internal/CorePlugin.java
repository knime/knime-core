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
 * History
 *   Oct 13, 2006 (wiswedel): created
 */
package org.knime.core.internal;

import java.io.File;
import java.net.URL;

import org.knime.core.node.NodeLogger;

/**
 * Plugin class that is initialized when the plugin project is started. It will
 * set the workspace path as KNIME home dir in the KNIMEConstants utility class.
 * @author wiswedel, University of Konstanz
 */
public class CorePlugin extends org.eclipse.core.runtime.Plugin {

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final org.osgi.framework.BundleContext context) 
        throws Exception {
        super.start(context);
        try {
            URL workspaceURL = 
               org.eclipse.core.runtime.Platform.getInstanceLocation().getURL();
            if (workspaceURL.getProtocol().equalsIgnoreCase("file")) {
                // we can create our home only in local workspaces
                File workspaceDir = new File(workspaceURL.getPath());
                File metaDataFile = new File(workspaceDir, ".metadata");
                if (!metaDataFile.exists()) {
                    metaDataFile.mkdir();
                }
                File knimeHomeDir = new File(metaDataFile, "knime");
                if (!knimeHomeDir.exists()) {
                    knimeHomeDir.mkdir();
                }
                KNIMEPath.setKNIMEHomeDir(knimeHomeDir.getAbsoluteFile());
                KNIMEPath.setWorkspaceDir(workspaceDir.getAbsoluteFile());
            }

        } catch (Exception e) {
            // the logger will use the "user.dir" as knime home, unfortunately.
            NodeLogger.getLogger(getClass()).warn(
                    "Can't init knime home dir to workspace path.", e);
        }
    }
}

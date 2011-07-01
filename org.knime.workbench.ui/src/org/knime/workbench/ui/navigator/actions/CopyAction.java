/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: May 18, 2011
 * Author: ohl
 */
package org.knime.workbench.ui.navigator.actions;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TreeViewer;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.VMFileLocker;

/**
 * Copies a resource to a target resource.
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class CopyAction extends MoveWorkflowAction {

    public static final String ID = "org.knime.workbench.CopyAction";

    private final String m_newName;

    /**
     * @param source
     * @param target
     * @param newName name of the copy (of the source) in the target, if null
     * it is the name of the source.
     */
    public CopyAction(final IPath source, final IPath target, final String newName) {
        super(source, target);
        m_newName = newName;
        setId(ID);
        setActionDefinitionId("Copy...");
    }

    /**
     * @param viewer
     */
    public CopyAction(final TreeViewer viewer) {
        super(viewer);
        m_newName = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSourceNameInTarget() {
        if (m_newName == null || m_newName.isEmpty()) {
            return super.getSourceNameInTarget();
        } else {
            return m_newName;
        }
    }

    /**
     * Copies the content of the source into the target directory.
     * {@inheritDoc}
     */
    @Override
    protected void moveFiles(final File source, final File target) {
        // copy action copies.
        assert source.isDirectory();
        assert target.isDirectory();

        try {
            copyRecWithoutLockFile(source, target);
        } catch (IOException e) {
            NodeLogger.getLogger(CopyAction.class)
                    .error("Error while copying workflow/groups: "
                            + e.getMessage(), e);
        }
    }

    private void copyRecWithoutLockFile(final File src, final File target)
            throws IOException {
        File[] list = src.listFiles();
        if (list == null) {
            throw new IOException("Unable to read source "
                    + src.getAbsolutePath());
        }
        for (File f : list) {
            if (f.isDirectory()) {
                File targetDir = new File(target, f.getName());
                targetDir.mkdir();
                copyRecWithoutLockFile(f, targetDir);
            } else {
                // skip the lock file
                if (!f.getName().equalsIgnoreCase(VMFileLocker.LOCK_FILE)) {
                    FileUtils.copyFileToDirectory(f, target);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void deleteSourceDir(final IResource source,
            final IProgressMonitor monitor) throws CoreException {
        // don't delete in copy action
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean containsOpenWorkflows(final IResource src) {
        // open workflows are okay during copy
        return false;
    }
}

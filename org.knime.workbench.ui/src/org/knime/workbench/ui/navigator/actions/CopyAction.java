/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
Cop *
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

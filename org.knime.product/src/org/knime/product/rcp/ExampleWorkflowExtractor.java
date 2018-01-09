/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   25.06.2014 (thor): created
 */
package org.knime.product.rcp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.core.internal.KNIMEPath;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.workbench.explorer.view.ExplorerView;

/**
 * Runnable that extract the example workflows into a fresh workspace and refreshes the explorer view afterwards.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class ExampleWorkflowExtractor implements Runnable {
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Location loc = Platform.getInstallLocation();
        if (loc == null) {
            NodeLogger.getLogger(getClass()).error("Cannot detect KNIME installation directory");
            return;
        } else if (!loc.getURL().getProtocol().equals("file")) {
            NodeLogger.getLogger(getClass()).error("KNIME installation directory is not local");
            return;
        }

        String path = loc.getURL().getPath();
        if (Platform.OS_WIN32.equals(Platform.getOS()) && path.matches("^/[a-zA-Z]:/.*")) {
            // Windows path with drive letter => remove first slash
            path = path.substring(1);
        }
        Path initialWorkspace = Paths.get(path, "knime-workspace.zip");
        if (!Files.exists(initialWorkspace)) {
            NodeLogger.getLogger(getClass()).warn(
                initialWorkspace.toAbsolutePath()
                    + " not found in installation directory, not creating inital workspace");
            return;
        }


        File workspace = KNIMEPath.getWorkspaceDirPath();
        try (ZipInputStream is = new ZipInputStream(Files.newInputStream(initialWorkspace))) {
            FileUtil.unzip(is, workspace, 0);
        } catch (IOException ex) {
            NodeLogger.getLogger(getClass()).error("Could not extract example workflows: " + ex.getMessage(), ex);
        }

        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IViewReference ref : page.getViewReferences()) {
                    if (ExplorerView.ID.equals(ref.getId())) {
                        final ExplorerView explorer = (ExplorerView)ref.getView(true);
                        final TreeViewer viewer = explorer.getViewer();
                        if (viewer.getControl() != null && viewer.getControl().getDisplay() != null) {
                            viewer.getControl().getDisplay().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    viewer.refresh();
                                    viewer.expandAll();
                                }
                            });
                        }
                    }
                }
            }
        }
    }
}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *
 * History
 *   Dec 10, 2015 (hornm): created
 */
package org.knime.core.clientproxy.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.clientproxy.util.ClientProxyUtil;
import org.knime.core.clientproxy.util.ObjectCache;
import org.knime.core.gateway.services.ServerServiceConfig;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.WorkflowManagerInput;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.ContentObject;

import com.knime.enterprise.client.filesystem.KnimeRemoteWorkflowInMem;
import com.knime.explorer.server.ServerExplorerFileInfo;
import com.knime.explorer.server.ServerExplorerFileStore;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class OpenRemoteWorkflowAction implements IObjectActionDelegate {
    private final List<ServerExplorerFileStore> m_filestores = new ArrayList<ServerExplorerFileStore>();
    private static final NodeLogger LOGGER = NodeLogger.getLogger(OpenRemoteWorkflowAction.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final IAction action) {
        m_filestores.forEach(fs -> {
            IFileStore nativeFilestore = fs.getNativeFilestore();
            assert nativeFilestore instanceof KnimeRemoteWorkflowInMem;
            KnimeRemoteWorkflowInMem krwim = (KnimeRemoteWorkflowInMem) nativeFilestore;
            UUID jobID = krwim.getExecID().getJobID();
            URI uri = krwim.toURI();
            IWorkflowManager wfm = ClientProxyUtil.getWorkflowManager(jobID.toString(), Optional.empty(), new ObjectCache(), new ServerServiceConfig(uri.getHost(), uri.getPort()));
            openEditor(wfm, fs);
        });
    }

    private boolean openEditor(final IWorkflowManager wfm, final ServerExplorerFileStore fileStore) {
        IEditorDescriptor editorDescriptor;
        try {
            editorDescriptor = IDE.getEditorDescriptor(WorkflowPersistor.WORKFLOW_FILE);
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .openEditor(new WorkflowManagerInput(wfm, fileStore.toURI()), editorDescriptor.getId());
//            NodeTimer.GLOBAL_TIMER.incWorkflowOpening();
            return true;
        } catch (PartInitException ex) {
            LOGGER.warn("Cannot open editor for " + fileStore + ": " + ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IAction action, final ISelection selection) {
        m_filestores.clear();

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection sSel = (IStructuredSelection)selection;
            for (@SuppressWarnings("unchecked")
            Iterator<Object> it = sSel.iterator(); it.hasNext();) {
                Object selectedObject = it.next();
                if (selectedObject instanceof ContentObject) {
                    if (((ContentObject)selectedObject).getObject() instanceof ServerExplorerFileStore) {
                        ServerExplorerFileStore fs = (ServerExplorerFileStore)((ContentObject)selectedObject).getObject();
                        try {
                            processFilestore(fs);
                        } catch (CoreException ex) {
                            NodeLogger.getLogger(OpenRemoteWorkflowAction.class).error(
                                "Cannot determine workflows in selection: " + ex.getMessage(), ex);
                        }
                    }
                }
            }

        }
        action.setEnabled(!m_filestores.isEmpty());
    }

    private void processFilestore(final ServerExplorerFileStore fs) throws CoreException {
        ServerExplorerFileInfo info = fs.fetchInfo();

        if (info.isWorkflowJob()) {
            m_filestores.add(fs);
        } else if (info.isWorkflowGroup()) {
            for (AbstractExplorerFileStore child : fs.childStores(EFS.NONE, null)) {
                if (child instanceof LocalExplorerFileStore) {
                    processFilestore((ServerExplorerFileStore)child);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setActivePart(final IAction action, final IWorkbenchPart targetPart) {

    }
}

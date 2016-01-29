/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.ui.navigator.actions;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;

/**
 *
 * @author Fabian Dill, KNIME.com AG
 */
public class EditMetaInfoAction extends Action {

    /** Action ID. */
    public static final String ID = KNIMEUIPlugin.PLUGIN_ID + "edit-meta-info";

    private static ImageDescriptor icon;

    private IFileStore m_parent;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        if (icon == null) {
            icon = AbstractUIPlugin.imageDescriptorFromPlugin(
                    KNIMEUIPlugin.PLUGIN_ID, "icons/meta_info_edit.png");
        }
        return icon;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Edit Meta Information...";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Opens the editor for the meta information";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        m_parent = null;
        // first get selection
        Object element = getSelection();
        if (element instanceof IWorkspaceRoot) {
            // check for IWorkspaceRoot first (since it is also an IContainer
            return false;
        }
        // if we are here only IProjects and IFolders left as IContainer's
        if (element instanceof IContainer) {
            m_parent =
                    EFS.getLocalFileSystem().getStore(
                            ((IContainer)element).getLocation());
        } else if (element instanceof IFileStore) {
            m_parent = ((IFileStore)element);
        }
        if (m_parent == null) {
            return false;
        }
        return true;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void run() {
        boolean isWorkflow = false;
        if (m_parent.getChild(WorkflowPersistor.WORKFLOW_FILE).fetchInfo()
                .exists()) {
            isWorkflow = true;
        }
        // if no meta file is available
        IFileStore metaFileTest = m_parent.getChild(WorkflowPersistor.METAINFO_FILE);
        IFileInfo fetchInfo = metaFileTest.fetchInfo();
        if (!fetchInfo.exists() || (fetchInfo.getLength() == 0)) {
            // create one
            File parentFile;
            try {
                parentFile = m_parent.toLocalFile(EFS.NONE, null);
            } catch (CoreException e) {
                throw new RuntimeException("Meta Info files can only be created"
                        + " for local workflows or groups. "
                        + m_parent.getName() + " doesn't provide a local file.");
            }
            if (parentFile == null) {
                throw new RuntimeException("Meta Info files can only be created"
                        + " for local workflows or groups. "
                        + m_parent.getName() + " doesn't provide a local file.");
            }
            MetaInfoFile.createMetaInfoFile(parentFile, isWorkflow);
        }
        IFileStore metaFile =
            m_parent.getChild(WorkflowPersistor.METAINFO_FILE);
        try {
            IDE.openEditorOnFileStore(PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage(), metaFile);
        } catch (PartInitException e) {
            String m = e.getMessage() == null ? "<no details>" : e.getMessage();
            throw new RuntimeException(
                    "Unable to initialize editor for Meta Info file of "
                            + m_parent.getName() + ": " + m, e);
        }
    }

    private Object getSelection() {
        ISelectionService service  = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getSelectionService();
        IStructuredSelection selection
            = (IStructuredSelection)service.getSelection();
        if (selection == null) {
            return null;
        }
        return selection.getFirstElement();
    }

}

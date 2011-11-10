/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 */
package org.knime.workbench.ui.navigator.actions;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
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
import org.knime.core.node.NodeLogger;
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
            icon = KNIMEUIPlugin.imageDescriptorFromPlugin(
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
            if (m_parent == null) {
                NodeLogger.getLogger(EditMetaInfoAction.class).debug(
                        "Only local meta info files can be opened");
            }
        }
        if (m_parent == null) {
            return false;
        }
        // check whether it contains a meta.info file
        if (m_parent.getChild(
                WorkflowPersistor.METAINFO_FILE).fetchInfo().exists()
                || m_parent.getChild(
                        WorkflowPersistor.WORKFLOW_FILE).fetchInfo().exists()) {
            return true;
        }
        return false;
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
        if (!metaFileTest.fetchInfo().exists()) {
            // create one
            File parentFile;
            try {
                parentFile = m_parent.toLocalFile(EFS.NONE, null);
            } catch (CoreException e) {
                throw new RuntimeException("Meta Info files can only be created"
                        + " for local workflows or groups. "
                        + m_parent.getName() + "doesn't provide a local file.");
            }
            if (parentFile == null) {
                throw new RuntimeException("Meta Info files can only be created"
                        + " for local workflows or groups. "
                        + m_parent.getName() + "doesn't provide a local file.");
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

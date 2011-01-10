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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.OpenFileAction;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;
import org.knime.workbench.ui.navigator.KnimeResourceLabelProvider;

/**
 *
 * @author Fabian Dill, KNIME.com GmbH
 */
public class EditMetaInfoAction extends Action {

    /** Action ID. */
    public static final String ID = KNIMEUIPlugin.PLUGIN_ID + "edit-meta-info";

    private static ImageDescriptor icon;

    private IContainer m_parent;

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
        // first get selection
        Object element = getSelection();
        if (element instanceof IWorkspaceRoot) {
            // check for IWorkspaceRoot first (since it is also an IContainer
            return false;
        }
        // if we are here only IProjects and IFolders left as IContainer's
        if (element instanceof IContainer) {
            m_parent = (IContainer)element;
            // check whether it contains a meta.info file
            if (m_parent.exists(KnimeResourceLabelProvider.METAINFO_FILE)
                    || m_parent.exists(
                            KnimeResourceLabelProvider.WORKFLOW_FILE)) {
                return true;
            }
            return false;
        }
        // as long as a IContainer is selected (not root)
        return false;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void run() {
        boolean isWorkflow = false;
        if (m_parent.exists(new Path(WorkflowPersistor.WORKFLOW_FILE))) {
            isWorkflow = true;
        }
        // if no meta file is available
        File metaFileTest = new File(m_parent.getLocation().toFile(),
                MetaInfoFile.METAINFO_FILE);
        if (!metaFileTest.exists()) {
            // create one
            MetaInfoFile.createMetaInfoFile(
                    new File(m_parent.getLocationURI()), isWorkflow);
        }
        IFile metaFile = m_parent.getFile(new Path(MetaInfoFile.METAINFO_FILE));
        // open file action -> run with meta file..
        OpenFileAction openFile = new OpenFileAction(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage());
        openFile.selectionChanged(new StructuredSelection(metaFile));
        openFile.run();
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

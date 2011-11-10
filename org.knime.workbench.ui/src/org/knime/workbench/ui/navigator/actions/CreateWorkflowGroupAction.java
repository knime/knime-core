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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.wizards.workflowgroup.NewWorkflowGroupProjectWizard;

/**
 * Action to create a workflow group.
 * 
 * @author Fabian Dill, KNIME.com AG
 */
public class CreateWorkflowGroupAction extends Action {

    private static ImageDescriptor icon;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        if (icon == null) {
            icon = KNIMEUIPlugin.imageDescriptorFromPlugin(
                    KNIMEUIPlugin.PLUGIN_ID, "icons/wf_group_new.png");
        }
        return icon;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "New Workflow Group...";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Creates a folder to group KNIME workflows in";
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void run() {
        NewWorkflowGroupProjectWizard wizard 
            = new NewWorkflowGroupProjectWizard();
        IWorkbench workbench = PlatformUI.getWorkbench();
        ISelection selection = workbench.getActiveWorkbenchWindow()
            .getSelectionService().getSelection();
        if (selection instanceof IStructuredSelection) {
            wizard.init(workbench,  
                (IStructuredSelection)selection);
        }
        WizardDialog dialog = new WizardDialog(
                Display.getDefault().getActiveShell(), 
                wizard);
        dialog.open();
    }

}

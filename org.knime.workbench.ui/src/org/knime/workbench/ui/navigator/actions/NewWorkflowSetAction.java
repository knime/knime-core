/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.navigator.KnimeResourceNavigator;
import org.knime.workbench.ui.wizards.workflowset.NewWorkflowSetProjectWizard;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class NewWorkflowSetAction extends Action {
    
    private static ImageDescriptor m_icon;
    
    @Override
    public ImageDescriptor getImageDescriptor() {
        if (m_icon == null) {
            m_icon = KNIMEUIPlugin.imageDescriptorFromPlugin(
                    KNIMEUIPlugin.PLUGIN_ID, "icons/wf_group_new.png");
        }
        return m_icon;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Opens the wizard for creating a new workflow set";
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Create new workflow group...";
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void run() {
        NewWorkflowSetProjectWizard wizard = new NewWorkflowSetProjectWizard();
        IWorkbench workbench = PlatformUI.getWorkbench(); 
        
        // get the selection 
        // since it is the sleection of the navigator it must be a 
        // structured selection (because of the tree viewer)
        IStructuredSelection selection = (IStructuredSelection)workbench
            .getActiveWorkbenchWindow().getActivePage().getSelection(
                    KnimeResourceNavigator.ID);

        wizard.init(workbench, selection);
        
        // get the shell
        Shell shell = workbench.getActiveWorkbenchWindow().getShell();
        
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.create();
        dialog.open();
    }

}

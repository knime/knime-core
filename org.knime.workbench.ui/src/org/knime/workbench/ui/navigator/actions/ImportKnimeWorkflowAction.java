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
 *   20.10.2006 (sieb): created
 */
package org.knime.workbench.ui.navigator.actions;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.wizards.imports.WorkflowImportWizard;

/**
 * Action to invoke the KNIME import wizard.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME.com GmbH, Zurich, Switzerland
 */
public class ImportKnimeWorkflowAction extends Action {
    
    private static final ImageDescriptor ICON 
        = KNIMEUIPlugin.imageDescriptorFromPlugin(
                KNIMEUIPlugin.PLUGIN_ID, "icons/knime_import.png");
    
    /**
     * The id for this action.
     */
    public static final String ID = "KNIMEImport";

    /**
     * The workbench window; or <code>null</code> if this action has been
     * <code>dispose</code>d.
     */
    private final IWorkbenchWindow m_workbenchWindow;

    /**
     * Create a new instance of this class.
     * 
     * @param window the window
     */
    public ImportKnimeWorkflowAction(final IWorkbenchWindow window) {
        super("Import KNIME workflow...");
        if (window == null) {
            throw new IllegalArgumentException();
        }
        this.m_workbenchWindow = window;
        setToolTipText("Imports a KNIME workflow from an archive"
                + " or a directory structure");
        setId(ID);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ICON;
    }

    /**
     * Invoke the Import wizards selection Wizard.
     */
    @Override
    public void run() {
        WorkflowImportWizard wizard = new WorkflowImportWizard();
        WizardDialog dialog = new WizardDialog(
                Display.getDefault().getActiveShell(), wizard);
        IStructuredSelection selectionToPass;
        // get the current workbench selection
        ISelection workbenchSelection = m_workbenchWindow.getSelectionService()
            .getSelection();
        if (workbenchSelection instanceof IStructuredSelection) {
            selectionToPass = (IStructuredSelection)workbenchSelection;
            if (selectionToPass.size() == 1) {
                Object o = selectionToPass.getFirstElement();
                if (o instanceof IContainer) {
                    wizard.setInitialDestination((IContainer)o);
                }
            }
        }
        dialog.open();
    }

}

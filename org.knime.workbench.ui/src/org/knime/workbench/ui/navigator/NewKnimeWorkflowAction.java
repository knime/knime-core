/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * -------------------------------------------------------------------
 * 
 * History
 *   20.10.2006 (sieb): created
 */
package org.knime.workbench.ui.navigator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.knime.workbench.repository.ImageRepository;
import org.knime.workbench.ui.wizards.project.NewProjectWizard;

/**
 * Action to invoke the knime new workflow wizard.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class NewKnimeWorkflowAction extends Action {

    private static final int SIZING_WIZARD_WIDTH = 470;

    private static final int SIZING_WIZARD_HEIGHT = 550;

    /**
     * The id for this action.
     */
    public static final String ID = "NewKNIMEWorkflow";

    /**
     * The workbench window; or <code>null</code> if this action has been
     * <code>dispose</code>d.
     */
    private IWorkbenchWindow m_workbenchWindow;

    /**
     * Create a new instance of this class.
     * 
     * @param window the window
     */
    public NewKnimeWorkflowAction(final IWorkbenchWindow window) {
        super("New KNIME workflow...");
        if (window == null) {
            throw new IllegalArgumentException();
        }
        this.m_workbenchWindow = window;
        setToolTipText("Creates a new KNIME workflow project.");
        setId(ID); //$NON-NLS-1$
        // window.getWorkbench().getHelpSystem().setHelp(this,
        // IWorkbenchHelpContextIds.IMPORT_ACTION);
        // self-register selection listener (new for 3.0)

    }

    /**
     * Create a new instance of this class.
     * 
     * @param workbench the workbench
     * @deprecated use the constructor
     *             <code>ImportResourcesAction(IWorkbenchWindow)</code>
     */
    public NewKnimeWorkflowAction(final IWorkbench workbench) {
        this(workbench.getActiveWorkbenchWindow());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/view.PNG");
    }

    /**
     * Invoke the Import wizards selection Wizard.
     */
    public void run() {
        if (m_workbenchWindow == null) {
            // action has been disposed
            return;
        }

        NewProjectWizard wizard = new NewProjectWizard();

        IStructuredSelection selectionToPass;
        // get the current workbench selection
        ISelection workbenchSelection =
                m_workbenchWindow.getSelectionService().getSelection();
        if (workbenchSelection instanceof IStructuredSelection) {
            selectionToPass = (IStructuredSelection)workbenchSelection;
        } else {
            selectionToPass = StructuredSelection.EMPTY;
        }

        wizard.init(m_workbenchWindow.getWorkbench(), selectionToPass);

        // wizard.setForcePreviousAndNextButtons(true);

        Shell parent = m_workbenchWindow.getShell();
        WizardDialog dialog = new WizardDialog(parent, wizard);
        dialog.create();
        dialog.getShell().setSize(
                Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
                SIZING_WIZARD_HEIGHT);
        // PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(),
        // IWorkbenchHelpContextIds.IMPORT_WIZARD);
        dialog.open();
    }

    /*
     * (non-Javadoc) Method declared on ActionFactory.IWorkbenchAction.
     * 
     * @since 3.0
     */
    public void dispose() {
        if (m_workbenchWindow == null) {
            // action has already been disposed
            return;
        }

        m_workbenchWindow = null;
    }
}

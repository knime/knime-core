/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.wizards.project.NewProjectWizard;

/**
 * Action to invoke the knime new workflow wizard.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class NewKnimeWorkflowAction extends Action {

    private static final int SIZING_WIZARD_WIDTH = 470;

    private static final int SIZING_WIZARD_HEIGHT = 550;
    
    private static final ImageDescriptor ICON = 
        KNIMEUIPlugin.imageDescriptorFromPlugin(
            KNIMEUIPlugin.PLUGIN_ID, 
            "icons/new_knime16.png");;

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
        return ICON;
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

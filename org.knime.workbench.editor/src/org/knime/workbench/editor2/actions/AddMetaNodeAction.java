/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 *
 * History
 *   11.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.meta.AddMetaNodeWizard;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class AddMetaNodeAction implements IEditorActionDelegate {

    /** unique ID for this action. * */
    public static final String ID = "knime.action.meta";

    private WorkflowEditor m_editor;

    private AddMetaNodeWizard m_wizard;

    /**
     *
     * {@inheritDoc}
     */
    public void setActiveEditor(final IAction action,
            final IEditorPart targetEditor) {
        m_editor = (WorkflowEditor)targetEditor;
    }


    /**
     *
     * {@inheritDoc}
     */
    public void run(final IAction action) {
        Display display = Display.getCurrent();
        m_wizard = new AddMetaNodeWizard(m_editor);
        WizardDialog dialog = new WizardDialog(display.getActiveShell(),
                m_wizard);
        dialog.create();
        dialog.open();
    }


    /**
     *
     * {@inheritDoc}
     */
    public void selectionChanged(final IAction action,
            final ISelection selection) {
    }



}

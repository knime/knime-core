/*
 * ------------------------------------------------------------------ *
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
 *   11.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.meta.AddMetaNodeWizard;
import org.knime.workbench.editor2.meta.MetaNodeWizardDialog;

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
        MetaNodeWizardDialog dialog = new MetaNodeWizardDialog(
                display.getActiveShell(), m_wizard);
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

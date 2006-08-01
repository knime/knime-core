/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2004
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
 *   10.11.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.actions.delegates;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.actions.AbstractNodeAction;

/**
 * Abstract base class for Editor Actions.
 * 
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractEditorAction implements IEditorActionDelegate,
        WorkflowListener {

    private WorkflowEditor m_editor;

    private AbstractNodeAction m_decoratedAction;

    /**
     * @see org.eclipse.ui.IEditorActionDelegate
     *      #setActiveEditor(org.eclipse.jface.action.IAction,
     *      org.eclipse.ui.IEditorPart)
     */
    public final void setActiveEditor(final IAction action,
            final IEditorPart targetEditor) {

        if (targetEditor instanceof WorkflowEditor) {

            m_editor = (WorkflowEditor)targetEditor;
            m_editor.getWorkflowManager().addListener(this);
            m_decoratedAction = createAction(m_editor);

        } else {
            if (m_decoratedAction != null) {
                m_decoratedAction.dispose();
            }
            m_decoratedAction = null;
            if (m_editor != null) {
                m_editor.getWorkflowManager().removeListener(this);
            }
            m_editor = null;
        }
    }

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public final void run(final IAction action) {
        if (m_decoratedAction != null) {
            m_decoratedAction.run();
        }
    }

    /**
     * @see org.eclipse.ui.IActionDelegate
     *      #selectionChanged(org.eclipse.jface.action.IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    public final void selectionChanged(final IAction action,
            final ISelection selection) {
        if (m_decoratedAction != null) {
            m_decoratedAction.dispose();
            m_decoratedAction = null;
        }

        if (m_editor != null) {
            m_decoratedAction = createAction(m_editor);
            action.setEnabled(m_decoratedAction.isEnabled());
        }

    }

    /**
     * @see de.unikn.knime.core.node.workflow.WorkflowListener
     *      #workflowChanged(de.unikn.knime.core.node.workflow.WorkflowEvent)
     */
    public void workflowChanged(final WorkflowEvent event) {
        m_editor.getSite().getSelectionProvider().setSelection(
                m_editor.getSite().getSelectionProvider().getSelection());
    }

    /**
     * Clients must implement this method.
     * 
     * @param editor the knime editor
     * @return Decorated action
     */
    protected abstract AbstractNodeAction createAction(
            final WorkflowEditor editor);

}

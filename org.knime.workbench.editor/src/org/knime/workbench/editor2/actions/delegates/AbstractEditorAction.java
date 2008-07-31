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
 *   10.11.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions.delegates;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;

import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.actions.AbstractNodeAction;

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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public final void run(final IAction action) {
        if (m_decoratedAction != null) {
            m_decoratedAction.run();
        }
    }

    /**
     * {@inheritDoc}
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
    
    private final SelectionRunnable m_selectionRunnable =
        new SelectionRunnable();

    private class SelectionRunnable implements Runnable {
        /** Flags to memorize if this runnable has already been queued. I 
         * (Bernd) ran into serious problems when using meta nodes that 
         * execute/reset nodes quickly (and frequently). There where 
         * many (> 500000) runnables in the async-queue. */
        private boolean m_isQueued;
        public void run() {
            m_isQueued = false;
            ISelectionProvider p = m_editor.getSite().getSelectionProvider();
            p.setSelection(p.getSelection());
        }
        
        private void asyncExec() {
            if (!m_isQueued) {
                m_isQueued = true;
                Display.getDefault().asyncExec(this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void workflowChanged(final WorkflowEvent event) {
        m_selectionRunnable.asyncExec();
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

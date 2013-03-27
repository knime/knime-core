/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 */
package org.knime.workbench.editor2.actions.delegates;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.actions.AbstractNodeAction;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.SyncExecQueueDispatcher;

/**
 * Abstract base class for Editor Actions.
 *
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractEditorAction implements IEditorActionDelegate,
        NodeStateChangeListener {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            AbstractEditorAction.class);

    private WorkflowEditor m_editor;

    private AbstractNodeAction m_decoratedAction;

    private final List<NodeContainerEditPart> m_currentSelection
        = new ArrayList<NodeContainerEditPart>();

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setActiveEditor(final IAction action,
            final IEditorPart targetEditor) {

        if (targetEditor instanceof WorkflowEditor) {

            m_editor = (WorkflowEditor)targetEditor;
            m_editor.getWorkflowManager().addNodeStateChangeListener(this);
            m_decoratedAction = createAction(m_editor);

        } else {
            if (m_decoratedAction != null) {
                m_decoratedAction.dispose();
            }
            m_decoratedAction = null;
            if (m_editor != null) {
                m_editor.getWorkflowManager().removeNodeStateChangeListener(
                        this);
            }
            m_editor = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void run(final IAction action) {
        if (m_decoratedAction != null) {
            m_decoratedAction.run();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void selectionChanged(final IAction action,
            final ISelection selection) {
        if (m_decoratedAction != null) {
            m_decoratedAction.dispose();
            m_decoratedAction = null;
            // and unregister from old selection
            for (NodeContainerEditPart cont : m_currentSelection) {
                cont.getNodeContainer().removeNodeStateChangeListener(
                        this);
            }
            m_currentSelection.clear();
        }

        if (m_editor != null) {
            m_decoratedAction = createAction(m_editor);
            StructuredSelection sel = ((StructuredSelection)selection);
            if (sel != null) {
                // register to new selection
                for (Iterator itr = sel.iterator(); itr.hasNext();) {
                    Object o = itr.next();
                    if (o instanceof NodeContainerEditPart) {
                        NodeContainerEditPart ncEP
                            = (NodeContainerEditPart)o;
                        m_currentSelection.add(ncEP);
                        ncEP.getNodeContainer().addNodeStateChangeListener(
                                this);
                    }
                }
            }
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
        /**
         *
         * {@inheritDoc}
         */
        @Override
        public void run() {
            m_isQueued = false;
            ISelectionProvider p = m_editor.getSite().getSelectionProvider();
            p.setSelection(p.getSelection());
        }

        private void asyncExec() {
            if (!m_isQueued) {
                m_isQueued = true;
                SyncExecQueueDispatcher.asyncExec(this);
            }
        }
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final NodeStateEvent state) {
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

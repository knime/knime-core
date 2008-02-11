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
 *   17.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowManagerInput implements IEditorInput {

    private final WorkflowManager m_manager;
    private final WorkflowEditor m_parent;

    public WorkflowManagerInput(final WorkflowManager manager,
            final WorkflowEditor parent) {
        m_manager = manager;
        m_parent = parent;
    }

    public WorkflowManager getWorkflowManager() {
        return m_manager;
    }

    public WorkflowEditor getParentEditor() {
        return m_parent;
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists() {
        return m_manager != null;
    }

    /**
     * {@inheritDoc}
     */
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
         return m_manager.getName();
    }

    /**
     * {@inheritDoc}
     */
    public IPersistableElement getPersistable() {
        // TODO: what if?
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getToolTipText() {
       return m_manager.getName() + "(not persisted yet)";
    }

    /**
     * {@inheritDoc}
     */
    public Object getAdapter(final Class adapter) {
        return null;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof WorkflowManagerInput)) {
            return false;
        }
        WorkflowManagerInput in = (WorkflowManagerInput)obj;
        return m_manager.equals(in.getWorkflowManager());
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_manager.hashCode();
    }

}

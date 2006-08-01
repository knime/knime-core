/* 
 * -------------------------------------------------------------------
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
 *   14.12.2005 (Christoph Sieb): created
 */
package org.knime.workbench.editor2;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.knime.core.node.meta.MetaNodeModel;
import org.knime.core.node.workflow.NodeContainer;


/**
 * This node wraps a meta-workflow in form of setting. It is used to set up a
 * new <code>WorkflowEditor</code> instance that alows modification of the
 * meta-workflow.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class MetaWorkflowEditorInput implements IEditorInput {
    private NodeContainer m_nodeContainer;

    /**
     * Creates a <code>IEditorInput</code> wrapper for the settings describing
     * a meta workflow.
     * 
     * @param container the container node representing a meta-workflow
     */
    public MetaWorkflowEditorInput(final NodeContainer container) {
        if (!MetaNodeModel.class.isAssignableFrom(container.getModelClass())) {
            throw new IllegalArgumentException("This node container does not"
                    + " hold a MetaNodeModel");
        }
        m_nodeContainer = container;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#exists()
     */
    public boolean exists() {
        return m_nodeContainer != null;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getName()
     */
    public String getName() {
        return "Meta-workflow editor";
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    public IPersistableElement getPersistable() {
        return null;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getToolTipText()
     */
    public String getToolTipText() {
        return "Meta-workflow editor";
    }

    /**
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(final Class adapter) {
        return null;
    }

    /**
     * @return the <code>NodeContainer</code> representing the meta-node
     *         input.
     */
    public NodeContainer getNodeContainer() {
        return m_nodeContainer;
    }
}

/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
     * {@inheritDoc}
     */
    public boolean exists() {
        return m_nodeContainer != null;
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
        return "Meta-workflow editor";
    }

    /**
     * {@inheritDoc}
     */
    public IPersistableElement getPersistable() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getToolTipText() {
        return "Meta-workflow editor";
    }

    /**
     * {@inheritDoc}
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

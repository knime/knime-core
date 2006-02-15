/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   19.12.2005 (cebron): created
 */
package de.unikn.knime.core.node.meta;

import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.Node;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.node.workflow.WorkflowManager;

/**
 * The MetaNode is an extension to a normal Node that wraps a MetaWorkflow,
 * which can be accessed from outside.
 *
 * @author cebron, University of Konstanz
 */
public class MetaNode extends Node implements NodeStateListener {

    /**
     * Constructor for the MetaNode.
     * 
     * @param nodeFactory The node factory for the creation of model, view,
     *        dialog.
     * @see Node
     */
    public MetaNode(final NodeFactory nodeFactory) {
        super(nodeFactory);
        ((MetaNodeModel)getNodeModel()).addStateListener(this);
    }

    /**
     * Returns the <code>WorkflowManager</code> if the underlying model is a
     * <code>MetaNodeModel</code>. IMPORTANT: This method should not be
     * located here!! It is just to enable a faster proof of concept of the meta
     * node technique. TODO: Re-design this access to the workflow manager
     * 
     * @return the workflow manager if this node is a meta-node
     */
    public WorkflowManager getMetaNodeWorkflowManager() {

        if (getNodeModel() instanceof MetaNodeModel) {
            MetaNodeModel metaNodeModel = (MetaNodeModel)getNodeModel();
            return metaNodeModel.getMetaWorkflowManager();
        }

        // else no workflow manager is available
        return null;
    }

    /**
     * static method to create a new Node based on <code>NodeSettings</code>.
     * 
     * @param settings The object to read the node settings from.
     * @return newly created <code>Node</code>, initialized using the config
     *         object
     * @throws InvalidSettingsException If a property is not available
     */
    public static Node createNode(final NodeSettings settings)
            throws InvalidSettingsException {

        // create new node
        Node newNode = new MetaNode(Node.createNodeFactory(settings));
        newNode.loadConfigFrom(settings); // load remaining settings
        return newNode; // return fully initialized Node
    }

    /**
     * Implements a <code>NodeStateListener</code> registering at the
     * underlying meta model to get notification once the underlying meta
     * workflow changes.
     * 
     * @see de.unikn.knime.core.node.NodeStateListener#stateChanged(
     * de.unikn.knime.core.node.NodeStatus, int)
     */
    public void stateChanged(final NodeStatus state, final int id) {

        if (state.getStatusId() == NodeStatus.STATUS_EXTRA_INFO_CHANGED) {
            // Just forward an extra info change
            notifyStateListeners(state);
        }
    }
}

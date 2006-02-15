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
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowManager;

/**
 * The MetaNodeContainer is an extension to a normal NodeContainer that wraps a
 * MetaNode.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaNodeContainer extends NodeContainer {

    /**
     * Constructor for the MetaNodeContainer.
     * 
     * @param n node to wrap
     * @param id identifier of the node
     * @see NodeContainer
     */
    public MetaNodeContainer(final Node n, final int id) {
        super(n, id);
    }

    /**
     * Returns the workflow manager if the underlying model is a
     * <code>MetaNodeModel</code>. IMPORTANT: This method should not be
     * located here!! It is just to enable a faster proof of concept of the meta
     * node technique. TODO: Re-design this access to the workflow manager
     * 
     * @return the workflow manager if this node is a meta-node
     */
    public WorkflowManager getMetaNodeWorkflowManager() {
        return ((MetaNode)getNode()).getMetaNodeWorkflowManager();
    }

    /**
     * Creates a new NodeContainer and reads it's status and information from
     * the NodeSettings object. Note that the list of predecessors and
     * successors will NOT be initalized correctly. The Workflow manager is
     * supposed to take care of re-initializing the connections.
     * 
     * @param sett Retrieve the data from.
     * @return new NodeContainer
     * @throws InvalidSettingsException If the required keys are not available
     *             in the NodeSettings.
     * 
     */
    public static NodeContainer createNewNodeContainer(final NodeSettings sett)
            throws InvalidSettingsException {
        // create new Node based on configuration
        Node newNode = MetaNode.createNode(sett);
        // read id
        int newID = sett.getInt(NodeContainer.KEY_ID);
  
        // create new NodeContainer and return it
        NodeContainer newNC = new MetaNodeContainer(newNode, newID);
        newNC.setExtraInfo(NodeContainer.createExtraInfo(sett));
        return newNC;
    }

    /**
     * Overrides the parent method to deal with the meta node event of extra
     * info changes.
     * 
     * @param state Indicates the change of this node.
     * @param id id from <code>Node</code>: will be overwritten
     */
    public synchronized void stateChanged(final NodeStatus state,
                                          final int id) {
                
        if (state.getStatusId() == NodeStatus.STATUS_EXTRA_INFO_CHANGED) {
            // just forward this event
            notifyStateListeners(state);
        }

        // invoke the super implementation
        super.stateChanged(state, id);
    }
}

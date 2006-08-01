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
 *   30.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.editparts.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.knime.core.node.workflow.WorkflowManager;

import de.unikn.knime.workbench.editor2.commands.DeleteNodeContainerCommand;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;
import de.unikn.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * This is the COMPONENT_POLICY for <code>NodeContainerEditPart</code>s. This
 * simply delivers the delete-command for nodes in a workflow.
 * 
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeContainerComponentEditPolicy extends ComponentEditPolicy {
    /**
     * @see org.eclipse.gef.editpolicies.ComponentEditPolicy
     *      #getDeleteCommand(org.eclipse.gef.requests.GroupRequest)
     */
    @Override
    protected Command getDeleteCommand(final GroupRequest request) {

        // we need the parent WFM ...
        WorkflowManager manager = ((WorkflowRootEditPart)getHost().getParent())
                .getWorkflowManager();

        NodeContainerEditPart nodePart = (NodeContainerEditPart)getHost();
        return new DeleteNodeContainerCommand(nodePart, manager);
    }
}

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
 *   09.06.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.editparts.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;

import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.commands.DeleteConnectionCommand;
import de.unikn.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import de.unikn.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * Policy that enables a connection to create their own delete commands.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewConnectionComponentEditPolicy extends ComponentEditPolicy {
    /**
     * @see org.eclipse.gef.editpolicies.ComponentEditPolicy
     *      #createDeleteCommand(org.eclipse.gef.requests.GroupRequest)
     */
    @Override
    protected Command createDeleteCommand(final GroupRequest deleteRequest) {

        ConnectionContainerEditPart c = (ConnectionContainerEditPart) getHost();
        // we need the workflow manager
        // This is a bitr tricky here, as the parent of the connection's edit
        // part is the ScalableFreefromEditPart. We need to get the first (and
        // only) child to get a reference to "our" root (WorkflowRootEditPart)
        WorkflowManager manager = ((WorkflowRootEditPart) getHost().getRoot()
                .getChildren().get(0)).getWorkflowManager();

        return new DeleteConnectionCommand(c, manager);
    }
}

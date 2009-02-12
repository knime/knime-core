/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   30.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.knime.core.node.workflow.WorkflowManager;

import org.knime.workbench.editor2.commands.DeleteNodeContainerCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * This is the COMPONENT_POLICY for <code>NodeContainerEditPart</code>s. This
 * simply delivers the delete-command for nodes in a workflow.
 * 
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeContainerComponentEditPolicy extends ComponentEditPolicy {
    /**
     * {@inheritDoc}
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

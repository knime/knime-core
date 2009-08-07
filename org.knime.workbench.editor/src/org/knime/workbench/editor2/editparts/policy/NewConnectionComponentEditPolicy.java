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
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.commands.DeleteConnectionCommand;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * Policy that enables a connection to create their own delete commands.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewConnectionComponentEditPolicy extends ComponentEditPolicy {
    /**
     * {@inheritDoc}
     */
    @Override
    protected Command createDeleteCommand(final GroupRequest deleteRequest) {

        ConnectionContainerEditPart c = (ConnectionContainerEditPart) getHost();
        // we need the workflow manager
        // This is a bit tricky here, as the parent of the connection's edit
        // part is the ScalableFreefromEditPart. We need to get the first (and
        // only) child to get a reference to "our" root (WorkflowRootEditPart)
        WorkflowManager manager = ((WorkflowRootEditPart) getHost().getRoot()
                .getChildren().get(0)).getWorkflowManager();

        return new DeleteConnectionCommand(c, manager);
    }
}

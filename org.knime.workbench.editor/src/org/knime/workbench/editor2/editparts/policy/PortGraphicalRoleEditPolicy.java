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

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.commands.CreateConnectionCommand;
import org.knime.workbench.editor2.commands.ReconnectConnectionCommand;
import org.knime.workbench.editor2.editparts.AbstractPortEditPart;
import org.knime.workbench.editor2.editparts.ConnectableEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.SubWorkFlowOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * This is the edit policy that enables port-edit parts to create connections
 * between each other. This policy can handle connections between such as
 * in->out , out<-in
 *
 * TODO and out->node and in<-node (looks up the first free port as target)
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class PortGraphicalRoleEditPolicy extends GraphicalNodeEditPolicy {
  
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PortGraphicalRoleEditPolicy.class);
    /**
     * This tries to initialize the command to create a connection as far as
     * possible. However, it is completed by
     * <code>getConnectionCompleteCommand</code>
     *
     * {@inheritDoc}
     */
    @Override
    protected Command getConnectionCreateCommand(
            final CreateConnectionRequest req) {
        CreateConnectionCommand cmd = new CreateConnectionCommand();

        if (!(getHost() instanceof AbstractPortEditPart)) {
            return null;
        }
        ConnectableEditPart nodePart = (ConnectableEditPart)getHost()
                .getParent();

        if (getHost() instanceof NodeOutPortEditPart
                || getHost() instanceof WorkflowInPortEditPart
                || getHost() instanceof SubWorkFlowOutPortEditPart) {
            // request started on out port?
            cmd.setSourceNode(nodePart);
            cmd.setSourcePortID(((AbstractPortEditPart)getHost()).getIndex());
            cmd.setStartedOnOutPort(true);
        } else if (getHost() instanceof NodeInPortEditPart
                || getHost() instanceof WorkflowOutPortEditPart) {
            // // request started on in port ?
            // cmd.setTargetNode(nodePart);
            // cmd.setTargetPortID(((NodeInPortEditPart) getHost()).getId());
            // cmd.setStartedOnOutPort(false);
            return null;
            // LOGGER.debug("Started connection on in-port...");
        }

        // we need the manager to execute the command

        // TODO: if NodeContainerEditPart -> getParent
        if (nodePart instanceof NodeContainerEditPart) {
        cmd.setManager(
                ((WorkflowRootEditPart)((NodeContainerEditPart)nodePart)
                        .getParent()).getWorkflowManager());
        } else if (nodePart instanceof WorkflowInPortBarEditPart) {
            WorkflowInPortBarEditPart barEditPart 
                = (WorkflowInPortBarEditPart)nodePart;
            WorkflowManager manager = ((WorkflowPortBar)barEditPart.getModel())
                .getWorkflowManager();
            cmd.setManager(manager);
        }

        // we must remember this partially initialized command in the request.
        req.setStartCommand(cmd);

        return cmd;
    }

    /**
     * This tries to complete the command to create a connection.
     *
     * {@inheritDoc}
     */
    @Override
    protected Command getConnectionCompleteCommand(
            final CreateConnectionRequest request) {
        
        // get the previously started command
        CreateConnectionCommand cmd = (CreateConnectionCommand)request
                .getStartCommand();


        if (cmd == null) {
            return null;
        }

        EditPart target = request.getTargetEditPart();

        if ((target instanceof NodeOutPortEditPart)
                || target instanceof WorkflowInPortEditPart) {
            return null;

//             LOGGER.debug("Ending connection on out-port...");
        } else if (target instanceof NodeInPortEditPart
                || target instanceof WorkflowOutPortEditPart) {
            cmd.setTargetPortID(((AbstractPortEditPart)target).getIndex());
            cmd.setTargetNode((ConnectableEditPart)target.getParent());
        } else if (target instanceof NodeContainerEditPart) {

            if (cmd.wasStartedOnOutPort()) {
                cmd.setTargetPortID(-1);
                cmd.setTargetNode((NodeContainerEditPart)target);
            } else {
                cmd.setSourcePortID(-1);
                cmd.setSourceNode((NodeContainerEditPart)target);
            }
            // LOGGER.debug("Ending connection on NODE...");

        } else {
            return null;

        }

        // LOGGER.debug("Command is executable: " + cmd.canExecute());

        return cmd;

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Command getReconnectSourceCommand(final ReconnectRequest req) {

        LOGGER.debug("getReconnectSourceCommand. Host: "
                + getHost() + ". TargetEditPart: " + getTargetEditPart(req));
//        // get the connection to change
//        ConnectionContainerEditPart connection
//            = (ConnectionContainerEditPart)getHost();
//
//        // get the workflow manager
//        if (!(getHost() instanceof AbstractPortEditPart)) {
//            return null;
//        }
//        NodeContainerEditPart nodePart = (NodeContainerEditPart)getHost()
//                .getParent();
//        WorkflowManager manager = ((WorkflowRootEditPart)nodePart.getParent())
//                .getWorkflowManager();
//
//        ReconnectConnectionCommand reconnectCommand 
        // = new ReconnectConnectionCommand(
//                connection, manager, null, null);
//
//        return reconnectCommand;

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Command getReconnectTargetCommand(final ReconnectRequest req) {
        // target port or node changes
        // only connect to inports
        if (!(getHost() instanceof NodeInPortEditPart
                || getHost() instanceof WorkflowOutPortEditPart)) {
            return null;
        }
        // get new target in port
        AbstractPortEditPart target = (AbstractPortEditPart)req.getTarget();
        ReconnectConnectionCommand reconnectCmd 
            = new ReconnectConnectionCommand(
                    (ConnectionContainerEditPart)req.getConnectionEditPart(),
                    (AbstractPortEditPart)req.getConnectionEditPart()
                    .getSource(), 
                    target);
        return reconnectCmd;
    }
}

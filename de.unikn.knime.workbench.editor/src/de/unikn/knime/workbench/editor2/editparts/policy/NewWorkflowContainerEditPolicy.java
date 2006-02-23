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
 *   29.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.editparts.policy;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.commands.CreateNodeCommand;
import de.unikn.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * Container policy, handles the creation of new nodes that are inserted into
 * the workflow. The request contains the
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewWorkflowContainerEditPolicy extends ContainerEditPolicy {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NewWorkflowContainerEditPolicy.class);

    /**
     * @see ContainerEditPolicy#
     *      getCreateCommand(org.eclipse.gef.requests.CreateRequest)
     */
    protected Command getCreateCommand(final CreateRequest request) {

        Object obj = request.getNewObject();

        // Today, we only support nodes to be created here
        if (!(obj instanceof NodeFactory)) {
            LOGGER.error("Illegal drop object: " + obj);
            return null;
        }
        // point where the command occured
        // The node/description should be initially located here
        Point location = request.getLocation();

        // adapt the location according to the zoom factor
        // this seems to be a workaround for a bug in the framework
        ZoomManager zoomManager = (ZoomManager)getTargetEditPart(request)
                .getRoot().getViewer()
                .getProperty(ZoomManager.class.toString());
        
        double zoomLevel = zoomManager.getZoom();
        
        System.out.println(zoomLevel);
        
        location.x = (int)Math.round(location.x * (1.0 / zoomLevel));
        location.y = (int)Math.round(location.y * (1.0 / zoomLevel));
        
        WorkflowRootEditPart workflowPart = (WorkflowRootEditPart)this
                .getHost();
        WorkflowManager manager = workflowPart.getWorkflowManager();

        // Case 1:
        // create a new node
        if (obj instanceof NodeFactory) {
            NodeFactory factory = (NodeFactory)obj;

            CreateNodeCommand cmd = new CreateNodeCommand(manager, factory,
                    location);
            return cmd;
        }

        // // Case 2:
        // // create a new description
        // if (obj instanceof FlowDescription) {
        // // FlowDescription desc = (FlowDescription) newObject;
        //
        // DescriptionAddCommand descAddCommand = new DescriptionAddCommand(
        // project, location);
        // return descAddCommand;
        // }

        return null;
    }

    /**
     * @see org.eclipse.gef.EditPolicy
     *      #getTargetEditPart(org.eclipse.gef.Request)
     */
    public EditPart getTargetEditPart(final Request request) {
        if (REQ_CREATE.equals(request.getType())) {
            return getHost();
        }
        if (REQ_ADD.equals(request.getType())) {
            return getHost();
        }
        if (REQ_MOVE.equals(request.getType())) {
            return getHost();
        }
        return super.getTargetEditPart(request);
    }

}

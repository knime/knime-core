/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts.policy;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;

import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.CreateNodeCommand;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

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
     * {@inheritDoc}
     */
    @Override
    protected Command getCreateCommand(final CreateRequest request) {

        Object obj = request.getNewObject();

        // Today, we only support nodes to be created here
        if (!(obj instanceof GenericNodeFactory)) {
            LOGGER.error("Illegal drop object: " + obj);
            return null;
        }
        // point where the command occured
        // The node/description should be initially located here
        Point location = request.getLocation();

        // adapt the location according to the viewport location and the zoom
        // factor
        // this seems to be a workaround for a bug in the framework
        ZoomManager zoomManager = (ZoomManager)getTargetEditPart(request)
                .getRoot().getViewer()
                .getProperty(ZoomManager.class.toString());

        // adjust the location according to the viewport position
        // seems to be a workaround for a bug in the framework
        // (should imediately deliver the correct view position and not
        // the position of the viewport)
        WorkflowEditor.adaptZoom(zoomManager, location, true);

        WorkflowRootEditPart workflowPart = (WorkflowRootEditPart)this
                .getHost();
        WorkflowManager manager = workflowPart.getWorkflowManager();

        // Case 1:
        // create a new node
        if (obj instanceof GenericNodeFactory) {
            GenericNodeFactory<? extends GenericNodeModel> factory 
                = (GenericNodeFactory<? extends GenericNodeModel>)obj;

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
     * {@inheritDoc}
     */
    @Override
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

/* @(#)$$RCSfile$$ 
 * $$Revision$$ $$Date$$ $$Author$$
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
 *   ${date} (${user}): created
 */
package de.unikn.knime.workbench.editor2.editparts.policy;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.BendpointEditPolicy;
import org.eclipse.gef.requests.BendpointRequest;

import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.workbench.editor2.commands.NewBendpointCreateCommand;
import de.unikn.knime.workbench.editor2.commands.NewBendpointDeleteCommand;
import de.unikn.knime.workbench.editor2.commands.NewBendpointMoveCommand;

/**
 * Bendpoint policy, needed for creation of the add/delete/move commands of the
 * connections bendpoints.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ConnectionBendpointEditPolicy extends BendpointEditPolicy {

    /**
     * @see org.eclipse.gef.editpolicies.BendpointEditPolicy
     *      #getCreateBendpointCommand
     *      (org.eclipse.gef.requests.BendpointRequest)
     */
    protected Command getCreateBendpointCommand(final BendpointRequest req) {
        int index = req.getIndex();
        Point loc = req.getLocation();
        ConnectionContainer model = (ConnectionContainer)getHost().getModel();

        ZoomManager zoomManager = (ZoomManager)getHost().getRoot().getViewer()
                .getProperty(ZoomManager.class.toString());

        return new NewBendpointCreateCommand(model, index, loc, zoomManager);
    }

    /**
     * @see org.eclipse.gef.editpolicies.BendpointEditPolicy
     *      #getDeleteBendpointCommand
     *      (org.eclipse.gef.requests.BendpointRequest)
     */
    protected Command getDeleteBendpointCommand(final BendpointRequest req) {
        // get the index of the bendpoint to delete
        int index = req.getIndex();
        ConnectionContainer model = (ConnectionContainer)getHost().getModel();

        return new NewBendpointDeleteCommand(model, index);
    }

    /**
     * @see org.eclipse.gef.editpolicies.BendpointEditPolicy
     *      #getMoveBendpointCommand(org.eclipse.gef.requests.BendpointRequest)
     */
    protected Command getMoveBendpointCommand(final BendpointRequest request) {
        // index of the bendpoint to move
        int index = request.getIndex();
        Point loc = request.getLocation();
        ConnectionContainer model = (ConnectionContainer)getHost().getModel();

        ZoomManager zoomManager = (ZoomManager)getHost().getRoot().getViewer()
                .getProperty(ZoomManager.class.toString());

        return new NewBendpointMoveCommand(model, index, loc, zoomManager);
    }

}

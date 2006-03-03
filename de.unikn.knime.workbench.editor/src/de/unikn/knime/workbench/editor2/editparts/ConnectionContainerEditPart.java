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
 *   09.06.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.editparts;

import java.util.ArrayList;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.workbench.editor2.commands.ChangeBendPointLocationCommand;
import de.unikn.knime.workbench.editor2.editparts.policy.ConnectionBendpointEditPolicy;
import de.unikn.knime.workbench.editor2.editparts.policy.NewConnectionComponentEditPolicy;
import de.unikn.knime.workbench.editor2.extrainfo.ModellingConnectionExtraInfo;

/**
 * EditPart controlling a <code>ConnectionContainer</code> object in the
 * workflow.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ConnectionContainerEditPart extends AbstractConnectionEditPart
        implements WorkflowListener {

    private final boolean m_isModelPortConnection;

    /**
     * The constructor.
     * 
     * @param isModelConn a flag telling if this is a connection between model
     *            ports or not.
     */
    public ConnectionContainerEditPart(final boolean isModelConn) {
        m_isModelPortConnection = isModelConn;
    }

    /**
     * Creates a GEF command to shift the connections bendpoints.
     * 
     * @param request the underlying request holding information about the shift
     * @return the command to change the bendpoint locations
     */
    public Command getBendpointAdaptionCommand(final Request request) {

        ChangeBoundsRequest boundsRequest = (ChangeBoundsRequest)request;

        ZoomManager zoomManager = (ZoomManager)(getRoot().getViewer()
                .getProperty(ZoomManager.class.toString()));

        Point moveDelta = boundsRequest.getMoveDelta();
        return new ChangeBendPointLocationCommand(
                (ConnectionContainer)getModel(), moveDelta, zoomManager);
    }

    /**
     * @see org.eclipse.gef.EditPart#activate()
     */
    public void activate() {
        super.activate();
        ((ConnectionContainer)getModel()).addWorkflowListener(this);
    }

    /**
     * @see org.eclipse.gef.EditPart#deactivate()
     */
    public void deactivate() {
        super.deactivate();
        ((ConnectionContainer)getModel()).removeWorkflowListener(this);
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
     */
    protected void createEditPolicies() {

        installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
                new ConnectionEndpointEditPolicy());

        // enable bendpoints (must be stored in extra info)
        installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE,
                new ConnectionBendpointEditPolicy());

        installEditPolicy(EditPolicy.CONNECTION_ROLE,
                new NewConnectionComponentEditPolicy());

    }

    /**
     * @see org.eclipse.gef.editparts.AbstractConnectionEditPart#createFigure()
     */
    protected IFigure createFigure() {

        PolylineConnection conn = (PolylineConnection)super.createFigure();

        // Bendpoints
        BendpointConnectionRouter router = new BendpointConnectionRouter();
        conn.setConnectionRouter(router);
        conn.setRoutingConstraint(new ArrayList());

        // Decorations
        PolygonDecoration pD = new PolygonDecoration();
        if (m_isModelPortConnection) {
            // pD.setScale(9, 5);
            conn.setForegroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_BLUE));
            conn.setLineWidth(1);
        }

        conn.setTargetDecoration(pD);

        return conn;
    }

    /**
     * @see de.unikn.knime.core.node.workflow.WorkflowListener
     *      #workflowChanged(de.unikn.knime.core.node.workflow.WorkflowEvent)
     */
    public void workflowChanged(final WorkflowEvent event) {
        refreshVisuals();
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractEditPart#refreshVisuals()
     */
    protected void refreshVisuals() {
        super.refreshVisuals();
        ModellingConnectionExtraInfo ei = null;
        ei = (ModellingConnectionExtraInfo)((ConnectionContainer)getModel())
                .getExtraInfo();
        if (ei == null) {
            return;
        }

        Connection fig = (Connection)getFigure();
        // recreate list of bendpoints
        int[][] p = ei.getAllBendpoints();
        ArrayList<AbsoluteBendpoint> constraint = new ArrayList<AbsoluteBendpoint>();
        for (int i = 0; i < p.length; i++) {
            AbsoluteBendpoint bp = new AbsoluteBendpoint(p[i][0], p[i][1]);
            constraint.add(bp);
        }

        fig.setRoutingConstraint(constraint);

    }
}

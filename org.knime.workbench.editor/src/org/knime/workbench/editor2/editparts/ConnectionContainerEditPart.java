/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.workbench.editor2.commands.ChangeBendPointLocationCommand;
import org.knime.workbench.editor2.editparts.policy.ConnectionBendpointEditPolicy;
import org.knime.workbench.editor2.editparts.policy.NewConnectionComponentEditPolicy;
import org.knime.workbench.editor2.extrainfo.ModellingConnectionExtraInfo;

/**
 * EditPart controlling a <code>ConnectionContainer</code> object in the
 * workflow.
 *
 * @author Florian Georg, University of Konstanz
 */
public class ConnectionContainerEditPart extends AbstractConnectionEditPart
        implements WorkflowListener {//, ZoomListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ConnectionContainerEditPart.class);

    /**
     * The constructor.
     *
     * @param isModelConn
     *            a flag telling if this is a connection between model ports or
     *            not.
     */
    public ConnectionContainerEditPart() {
    }

    /**
     * Creates a GEF command to shift the connections bendpoints.
     *
     * @param request
     *            the underlying request holding information about the shift
     * @return the command to change the bendpoint locations
     */
    public Command getBendpointAdaptionCommand(final Request request) {

        ChangeBoundsRequest boundsRequest = (ChangeBoundsRequest) request;

        ZoomManager zoomManager =
                (ZoomManager) (getRoot().getViewer()
                        .getProperty(ZoomManager.class.toString()));

        Point moveDelta = boundsRequest.getMoveDelta();
        return new ChangeBendPointLocationCommand(
                (ConnectionContainer) getModel(), moveDelta, zoomManager);
    }


    @Override
    public void setSource(final EditPart editPart) {
        LOGGER.debug("set source: " + editPart);
        super.setSource(editPart);
    }

    @Override
    public void setTarget(final EditPart editPart) {
        LOGGER.debug("set target: " + editPart);
        super.setTarget(editPart);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activate() {
        super.activate();
        LOGGER.debug("activate: " + getModel());
        refresh();
//        ((ConnectionContainer) getModel()).addWorkflowListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deactivate() {
        super.deactivate();
//        ((ConnectionContainer) getModel()).removeWorkflowListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {

        PolylineConnection conn = (PolylineConnection) super.createFigure();

        // Bendpoints
        SnapOffBendPointConnectionRouter router =
                new SnapOffBendPointConnectionRouter();
        conn.setConnectionRouter(router);
        conn.setRoutingConstraint(new ArrayList());

        // Decorations
        // PolygonDecoration pD = new PolygonDecoration();
        // TODO: functionality disabled
//        if (m_type.equals(ModelContent.TYPE)) {
//            // pD.setScale(9, 5);
//            conn.setForegroundColor(Display.getCurrent().getSystemColor(
//                    SWT.COLOR_BLUE));
//            conn.setLineWidth(1);
//        }

//        // register as zoom listener to adapt the line width
//        ZoomManager zoomManager =
//                (ZoomManager) getRoot().getViewer().getProperty(
//                        ZoomManager.class.toString());
//
//        zoomManager.addZoomListener(this);
//
//        conn.setLineWidth(calculateLineWidthFromZoomLevel(zoomManager
//                        .getZoom()));

        // conn.setTargetDecoration(pD);

        return conn;
    }

    /**
     * {@inheritDoc}
     */
    public void workflowChanged(final WorkflowEvent event) {
        refreshVisuals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();
        LOGGER.debug("refreshing visuals for: " + getModel());
        ModellingConnectionExtraInfo ei = null;
        ei =
                (ModellingConnectionExtraInfo) ((ConnectionContainer)getModel())
                        .getUIInfo();
        LOGGER.debug("modelling info: " + ei);
        if (ei == null) {
            return;
        }

        Connection fig = (Connection) getFigure();
        // recreate list of bendpoints
        int[][] p = ei.getAllBendpoints();
        ArrayList<AbsoluteBendpoint> constraint =
                new ArrayList<AbsoluteBendpoint>();
        for (int i = 0; i < p.length; i++) {
            AbsoluteBendpoint bp = new AbsoluteBendpoint(p[i][0], p[i][1]);
            constraint.add(bp);
        }

        fig.setRoutingConstraint(constraint);
    }

//    private int calculateLineWidthFromZoomLevel(final double zoom) {
//        double newZoomValue = zoom;
//        // if the zoom level is larger than 100% the width
//        // is adapted accordingly
//        if (zoom < 1.0) {
//            newZoomValue = 1.0;
//        }
//
//        double connectinWidth = Math.round(newZoomValue);
//
//        return (int) connectinWidth;
//    }
//
//    /**
//     * Adapts the line width according to the zoom level.
//     *
//     * @param zoom
//     *            the zoom level from the zoom manager
//     */
//    public void zoomChanged(final double zoom) {
//
//        ((PolylineConnection) getFigure())
//                .setLineWidth(calculateLineWidthFromZoomLevel(zoom));
//    }
}

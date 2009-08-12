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
import org.eclipse.gef.editparts.ZoomListener;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.ConnectionUIInformationEvent;
import org.knime.core.node.workflow.ConnectionUIInformationListener;
import org.knime.core.node.workflow.UIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.commands.ChangeBendPointLocationCommand;
import org.knime.workbench.editor2.editparts.policy.ConnectionBendpointEditPolicy;
import org.knime.workbench.editor2.editparts.snap.SnapOffBendPointConnectionRouter;

/**
 * EditPart controlling a <code>ConnectionContainer</code> object in the
 * workflow.
 * Model: {@link ConnectionContainer}
 * View: {@link PolylineConnection} created in {@link #createFigure()}
 * Controller: {@link ConnectionContainerEditPart}
 * 
 *
 * @author Florian Georg, University of Konstanz
 */
public class ConnectionContainerEditPart extends AbstractConnectionEditPart
        implements ZoomListener, ConnectionUIInformationListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ConnectionContainerEditPart.class);
    
    /** {@inheritDoc} */
    @Override
    public ConnectionContainer getModel() {
        return (ConnectionContainer)super.getModel();
    }
    

    /** Returns the parent WFM. This method may also return null if the target
     * edit part has no parent assigned.
     * @return The hosting WFM
     */
    public WorkflowManager getWorkflowManager() {
        EditPart targetEditPart = getTarget();
        if (targetEditPart instanceof NodeInPortEditPart) {
            return ((NodeInPortEditPart)targetEditPart).getManager();
        }
        if (targetEditPart instanceof WorkflowOutPortEditPart) {
            return ((WorkflowOutPortEditPart)targetEditPart).getManager();
        }
        return null;
    }

    
    /** {@inheritDoc} */
    @Override
    public void activate() {
        getModel().addUIInformationListener(this);
        super.activate();
    }
    
    /** {@inheritDoc} */
    @Override
    public void deactivate() {
        getModel().removeUIInformationListener(this);
        super.deactivate();
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
        return new ChangeBendPointLocationCommand(this, moveDelta, zoomManager);
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
        // register as zoom listener to adapt the line width
        ZoomManager zoomManager =
                (ZoomManager) getRoot().getViewer().getProperty(
                        ZoomManager.class.toString());
        zoomManager.addZoomListener(this);
        conn.setLineWidth(calculateLineWidthFromZoomLevel(zoomManager
                        .getZoom()));
        return conn;
    }
    
    /** {@inheritDoc} */
    @Override
    public void connectionUIInformationChanged(
            final ConnectionUIInformationEvent evt) {
        refreshVisuals();
    }

    /**
     * Forwards the ui information to the model and refreshes the connection.
     * 
     * @param uiInfo the information about the connection (used only if 
     * bendpoints are used)
     */
    public void setUIInformation(final UIInformation uiInfo) {
        getModel().setUIInfo(uiInfo);
    }
    
    
    /**
     * 
     * @return the ui information of this connection (may be null if no 
     *  bendpoints are involved)
     */
    public UIInformation getUIInformation() {
        return (getModel()).getUIInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();
        LOGGER.debug("refreshing visuals for: " + getModel());
        ConnectionUIInformation ei = null;
        ei =
                (ConnectionUIInformation) (getModel())
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

    private int calculateLineWidthFromZoomLevel(final double zoom) {
        double newZoomValue = zoom;
        // if the zoom level is larger than 100% the width
        // is adapted accordingly
        if (zoom < 1.0) {
            newZoomValue = 1.0;
        }
        double connectinWidth = Math.round(newZoomValue);
        return (int) connectinWidth;
    }

    /**
     * Adapts the line width according to the zoom level.
     *
     * @param zoom the zoom level from the zoom manager
     */
    public void zoomChanged(final double zoom) {
        double newZoomValue = zoom;
        // if the zoom level is larger than 100% the width
        // is adapted accordingly
        if (zoom < 1.0) {
            newZoomValue = 1.0;
        }
        double lineWidth = Math.round(newZoomValue);
        ((PolylineConnection) getFigure())
                .setLineWidth((int)lineWidth);
    }
}

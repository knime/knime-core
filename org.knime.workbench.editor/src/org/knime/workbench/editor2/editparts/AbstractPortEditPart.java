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
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editparts.ZoomListener;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.gef.tools.ConnectionDragCreationTool;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.anchor.InPortConnectionAnchor;
import org.knime.workbench.editor2.editparts.anchor.OutPortConnectionAnchor;
import org.knime.workbench.editor2.editparts.policy.PortGraphicalRoleEditPolicy;
import org.knime.workbench.editor2.figures.AbstractPortFigure;
import org.knime.workbench.editor2.figures.NewToolTipFigure;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * Abstract base class for the edit parts that control the ports. This editpart
 * returns a <code>DragTracker</code> for starting connections between in- and
 * out ports. Note that all(!) ports  are registered as listener for workflow
 * events on the underlying {@link WorkflowManager}. This is necessary
 * because we need de be able to react on connection changes.
 *
 * @author Florian Georg, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractPortEditPart extends AbstractGraphicalEditPart
        implements NodeEditPart, WorkflowListener, ZoomListener {

    private final int m_index;
    private final PortType m_type;
    private final boolean m_isInPort;
    
    /**
     * Instead of using the Collections.EMPTY_LIST we have our own typed empty
     * list if no connections are available.
     * @see WorkflowOutPortEditPart#getModelSourceConnections()
     * @see WorkflowOutPortEditPart#getModelTargetConnections()
     * 
     */ 
    protected static final List<ConnectionContainer> EMPTY_LIST 
        = new LinkedList<ConnectionContainer>();

    /**
     * Subclasses must call this with the appropriate port type, port index and
     * a flag whether it is an in or out port.
     *
     * @param portIndex The index of this port
     * @param type the port type
     * @param inPort true if it is an inport, false otherwise
     */
    public AbstractPortEditPart(final PortType type, final int portIndex,
            final boolean inPort) {
        m_index = portIndex;
        m_type = type;
        m_isInPort = inPort;
    }

    
    /**
     * 
     * @return true if it is an in port, false if it is an out port
     */
    public boolean isInPort() {
        return m_isInPort;
    }

    /**
     *
     * @return type of this port (usually data, model or database)
     */
    public PortType getType() {
        return m_type;
    }

    /**
     * @return the port index.
     */
    public int getIndex() {
        return m_index;
    }
    
    /**
     * Convenience, returns the id of the hosting container.
     * 
     * @return node id of hosting container
     */
    public NodeID getID() {
        return getNodeContainer().getID();
    }
    
    /**
     * Convenience, returns the hosting container.
     *
     * @return the container
     */
    protected NodeContainer getNodeContainer() {
        if (getParent().getModel() instanceof WorkflowPortBar) {
            return ((WorkflowPortBar)getParent().getModel())
                .getWorkflowManager();
        }
        return (NodeContainer) getParent().getModel();
    }

    /**
     * Convenience, returns the WFM.
     *
     * @return the workflow manager
     */
    protected WorkflowManager getManager() {
        // should be no problem to return null
        // but avoid NullPointerException by calling methods on null object
        if (getParent() != null 
                && getParent().getParent() != null) {
        return ((WorkflowRootEditPart) getParent().getParent())
                .getWorkflowManager();
        }
        return null;
    }

    /**
     * We must register *every* node as a listener on the workflow, as we have
     * not real objects for it.
     *
     * @see org.eclipse.gef.EditPart#activate()
     */
    @Override
    public void activate() {
        super.activate();
        if (getManager() != null) {
            getManager().addListener(this);
        }
//      // register as zoom listener to adapt the line width
        ZoomManager zoomManager =
                (ZoomManager) getRoot().getViewer().getProperty(
                        ZoomManager.class.toString());

        zoomManager.addZoomListener(this);
    }

    /**
     * Remove the port as a listener from the workflow.
     *
     * @see org.eclipse.gef.EditPart#deactivate()
     */
    @Override
    public void deactivate() {
        super.deactivate();
        if (getManager() != null) {
            getManager().removeListener(this);
        }
    }

    /**
     * We install the the <code>GRAPHICAL_NODE_ROLE</code> which enables the
     * edit part to create connections to other edit parts.
     *
     * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
     */
    @Override
    protected void createEditPolicies() {
        // This policy provides create/reconnect commands for connections that
        // are associated at this port
        this.installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
                new PortGraphicalRoleEditPolicy());

    }

    /**
     * Refreshes the visuals of the port visuals.
     *
     * @see org.eclipse.gef.editparts.AbstractEditPart#refreshVisuals()
     */
    @Override
    protected void refreshVisuals() {
        // get the figure and update the constraint for it - locator is provided
        // by the figure itself
        AbstractWorkflowEditPart parent 
            = (AbstractWorkflowEditPart)getParent();
        AbstractPortFigure f = (AbstractPortFigure) getFigure();
        parent.setLayoutConstraint(this, f, f.getLocator());
    }

    /**
     * We're just interessted in events that have something to do with our port.
     * In this case we need to update the connections and visuals.
     *
     * @param event the workflow event
     */
    public void workflowChanged(final WorkflowEvent event) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {

                ConnectionContainer c = null;

                if (event.getType().equals(WorkflowEvent.Type.CONNECTION_ADDED)) {
                    c = (ConnectionContainer)event.getNewValue();
                } else if (event.getType().equals(
                        WorkflowEvent.Type.CONNECTION_REMOVED)) {
                    c = (ConnectionContainer)event.getOldValue();
                }

                // if we have a connection to refresh...
                if (c != null && getNodeContainer() != null) {
                    // only refresh if we are actually involved in the
                    // connection change
                    if (c.getSource() == getNodeContainer().getID()
                            || c.getDest() == getNodeContainer().getID()) {
                        refreshChildren();
                        refreshSourceConnections();
                        refreshTargetConnections();
                    }
                }
            }
        });
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
        double connectionWidth = Math.round(newZoomValue);
        ((AbstractPortFigure) getFigure())
                .setLineWidth((int)connectionWidth);
    }

    /**
     * This activates the ConnectionDragCreationTool, as soon as the user clicks
     * on this edit part. (event REQ_SELECTION)
     *
     * {@inheritDoc}
     */
    @Override
    public DragTracker getDragTracker(final Request request) {

        // Selection event: Start the connection creation
        if (request.getType().equals(REQ_SELECTION)) {

            // we need to select the parent edit part !
            // Be sure to preserve already selected nodes, so check for keyboard
            // modifiers
            SelectionRequest req = (SelectionRequest) request;
            if (req.isControlKeyPressed() || req.isShiftKeyPressed()
                    || req.isAltKeyPressed()) {
                // append parent to current selection
                getViewer().appendSelection(getParent());
            } else {
                // single select
                getViewer().select(getParent());
            }
            return new ConnectionDragCreationTool();
        }

        return super.getDragTracker(request);
    }

    /**
     * {@inheritDoc}
     */
    public ConnectionAnchor getSourceConnectionAnchor(
            final ConnectionEditPart connection) {
        return new OutPortConnectionAnchor(getFigure());
    }

    /**
     * {@inheritDoc}
     */
    public ConnectionAnchor getSourceConnectionAnchor(final Request request) {
        return new OutPortConnectionAnchor(getFigure());
    }

    /**
     * {@inheritDoc}
     */
    public ConnectionAnchor getTargetConnectionAnchor(
            final ConnectionEditPart connection) {
        return new InPortConnectionAnchor(getFigure());
    }

    /**
     * {@inheritDoc}
     */
    public ConnectionAnchor getTargetConnectionAnchor(final Request request) {
        return new InPortConnectionAnchor(getFigure());
    }


    /**
     *
     * @param portName port name
     * @param port the underlying port
     * @return tooltip text for the port (with number of columns and rows)
     */
    protected String getTooltipText(final String portName,
            final NodeOutPort port) {
        String name = portName;
        if (portName == null) {
            name = port.getPortName();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        String portSummary = port.getPortSummary();
        if (portSummary != null && portSummary.length() > 0) {
            sb.append(" (");
            sb.append(portSummary);
            sb.append(")");
        }
        return sb.toString();
    }

    /**
     * Tries to build the tooltip from the port name and if this is a data
     * outport and the node is configured/executed, it appends also the number
     * of columns and rows.
     */
    public void rebuildTooltip() {
        NodeOutPort port = getNodeContainer().getOutPort(getIndex());
        String tooltip = getTooltipText(port.getPortName(), port);
        ((NewToolTipFigure)getFigure().getToolTip()).setText(tooltip);
    }

}

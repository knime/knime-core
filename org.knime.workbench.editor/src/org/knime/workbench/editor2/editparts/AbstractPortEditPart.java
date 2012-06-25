/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
 * out ports. Note that all(!) ports are registered as listener for workflow
 * events on the underlying {@link WorkflowManager}. This is necessary because
 * we need de be able to react on connection changes.
 *
 * @author Florian Georg, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractPortEditPart extends AbstractGraphicalEditPart
        implements NodeEditPart, WorkflowListener, ZoomListener {

    private int m_index;

    private final PortType m_type;

    private final boolean m_isInPort;

    /**
     * Instead of using the Collections.EMPTY_LIST we have our own typed empty
     * list if no connections are available.
     *
     * @see WorkflowOutPortEditPart#getModelSourceConnections()
     * @see WorkflowOutPortEditPart#getModelTargetConnections()
     *
     */
    protected static final List<ConnectionContainer> EMPTY_LIST =
            new LinkedList<ConnectionContainer>();

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

    public void updateNumberOfPorts() {
        if (isInPort()) {
            ((AbstractPortFigure)getFigure()).setNumberOfPorts(getNodeContainer().getNrInPorts());
        } else {
            ((AbstractPortFigure)getFigure()).setNumberOfPorts(getNodeContainer().getNrOutPorts());
        }
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
     * @param index the new position of the port.
     */
    public void setIndex(final int index) {
        m_index = index;
        AbstractPortFigure fig = (AbstractPortFigure)getFigure();
        fig.setPortIdx(index);
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
     * @return true, if the underlying port is connected.
     */
    public boolean isConnected() {
        WorkflowManager manager = getManager();
        if (manager == null) {
            return false;
        }
        if (m_isInPort) {
            return (manager.getIncomingConnectionFor(
                    getID(), getIndex()) != null);
        } else {
            return (manager.getOutgoingConnectionsFor(getID(),
                    getIndex()).size() > 0);
        }
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
        return (NodeContainer)getParent().getModel();
    }

    /**
     * Convenience, returns the WFM.
     *
     * @return the workflow manager
     */
    protected WorkflowManager getManager() {
        // should be no problem to return null
        // but avoid NullPointerException by calling methods on null object
        if (getParent() != null && getParent().getParent() != null) {
            return ((WorkflowRootEditPart)getParent().getParent())
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
        // // register as zoom listener to adapt the line width
        ZoomManager zoomManager =
                (ZoomManager)getRoot().getViewer().getProperty(
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
        if (getManager() != null) {
            getManager().removeListener(this);
        }
        // // register as zoom listener to adapt the line width
        ZoomManager zoomManager =
                (ZoomManager)getRoot().getViewer().getProperty(
                        ZoomManager.class.toString());
        zoomManager.removeZoomListener(this);
        super.deactivate();
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
        AbstractWorkflowEditPart parent = (AbstractWorkflowEditPart)getParent();
        AbstractPortFigure f = (AbstractPortFigure)getFigure();
        parent.setLayoutConstraint(this, f, f.getLocator());
    }

    /**
     * We're just interested in events that have something to do with our port.
     * In this case we need to update the connections and visuals.
     *
     * @param event the workflow event
     */
    @Override
    public void workflowChanged(final WorkflowEvent event) {
        if (event.getType().equals(WorkflowEvent.Type.CONNECTION_ADDED)
                || event.getType()
                        .equals(WorkflowEvent.Type.CONNECTION_REMOVED)) {
            // only enqueue runnable if we are interested in this event
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (!isActive()) {
                        return;
                    }
                    ConnectionContainer c = null;

                    if (event.getType().equals(
                            WorkflowEvent.Type.CONNECTION_ADDED)) {
                        c = (ConnectionContainer)event.getNewValue();
                    } else if (event.getType().equals(
                            WorkflowEvent.Type.CONNECTION_REMOVED)) {
                        c = (ConnectionContainer)event.getOldValue();
                    }

                    // if we have a connection to refresh...
                    if (c != null && getNodeContainer() != null) {
                        // only refresh if we are actually involved in the
                        // connection change
                        if (c.getSource().equals(getNodeContainer().getID())
                                || c.getDest().equals(
                                        getNodeContainer().getID())) {
                            AbstractPortFigure fig =
                                    (AbstractPortFigure)getFigure();
                            fig.setIsConnected(isConnected());
                            fig.repaint();
                            refreshChildren();
                            refreshSourceConnections();
                            refreshTargetConnections();
                        }
                    }
                }
            });
        }
    }

    /**
     * Adapts the line width according to the zoom level.
     *
     * @param zoom the zoom level from the zoom manager
     */
    @Override
    public void zoomChanged(final double zoom) {
        double newZoomValue = zoom;
        // if the zoom level is larger than 100% the width
        // is adapted accordingly
        if (zoom < 1.0) {
            newZoomValue = 1.0;
        }
        double connectionWidth = Math.round(newZoomValue);
        ((AbstractPortFigure)getFigure()).setLineWidth((int)connectionWidth);
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
            SelectionRequest req = (SelectionRequest)request;
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
    @Override
    public ConnectionAnchor getSourceConnectionAnchor(
            final ConnectionEditPart connection) {
        return new OutPortConnectionAnchor(getFigure());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionAnchor getSourceConnectionAnchor(final Request request) {
        return new OutPortConnectionAnchor(getFigure());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionAnchor getTargetConnectionAnchor(
            final ConnectionEditPart connection) {
        return new InPortConnectionAnchor(getFigure());
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        if (getIndex() < getNodeContainer().getNrOutPorts()) {
            NodeOutPort port = getNodeContainer().getOutPort(getIndex());
            String tooltip = getTooltipText(port.getPortName(), port);
            ((NewToolTipFigure)getFigure().getToolTip()).setText(tooltip);
        }
    }

}

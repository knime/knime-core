/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeOutPort;
import org.knime.core.node.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowInPort;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowContextMenuProvider;
import org.knime.workbench.editor2.figures.NewToolTipFigure;
import org.knime.workbench.editor2.figures.WorkflowInPortFigure;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowInPortEditPart extends AbstractPortEditPart {
//
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowInPortEditPart.class);

    private static final String PORT_NAME = "Workflow In Port";


    /**
     *
     * @param type port type
     * @param portID port id
     */
    public WorkflowInPortEditPart(final PortType type, final int portID) {
        super(type, portID, true);
    }



    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void refreshVisuals() {
        IFigure fig = getFigure();
        fig.setBounds(new Rectangle(new Point(0, 50), new Dimension(30, 30)));
        super.refreshVisuals();
    }



    /**
     * Convenience, returns the hosting container.
     *
     * {@inheritDoc}
     */
    @Override
    protected final NodeContainer getNodeContainer() {
        if (getParent() == null) {
            return null;
        }
        return ((WorkflowRootEditPart)getParent()).getWorkflowManager();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setParent(final EditPart parent) {
        super.setParent(parent);
    }

    /**
     * Convenience, returns the WFM.
     *
     * {@inheritDoc}
     */
    @Override
    protected final WorkflowManager getManager() {
        return (WorkflowManager)getNodeContainer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        NodeOutPort port = getManager().getInPort(getId()).getUnderlyingPort();
        String tooltip = getTooltipText(PORT_NAME + ": "  + getId(), port);
//        LOGGER.warn("tooltip: " + tooltip + " for port: " + port
//                + " with obj: " + port.getPortObject());

        // TODO not port.getName -> workflow inport
        WorkflowInPortFigure f = new WorkflowInPortFigure(getType(),
                getManager().getNrInPorts(), getId(), tooltip);
        f.addMouseListener(new MouseListener() {

            public void mouseDoubleClicked(final MouseEvent me) {
                // TODO: open port view
                LOGGER.debug("workflow in port double clicked!");
            }

            public void mousePressed(final MouseEvent me) {
                // TODO: is rightClick
                LOGGER.debug("workflow in port mouse pressed...");
                ((WorkflowInPortFigure)getFigure()).setSelected(true);

            }

            public void mouseReleased(final MouseEvent me) {
                // do nothing
                LOGGER.debug("workflow in port mouse released...");
                ((WorkflowInPortFigure)getFigure()).setSelected(false);
            }

        });
        return f;
    }

    /**
     * The context menu ({@link WorkflowContextMenuProvider#buildContextMenu(
     * org.eclipse.jface.action.IMenuManager)}) reads and resets the selection 
     * state.
     * 
     * @return true if the underlying workflow in port figure was clicked, false
     *  otherwise
     * @see WorkflowContextMenuProvider#buildContextMenu(
     *  org.eclipse.jface.action.IMenuManager)
     * @see WorkflowInPortFigure#setSelected(boolean)
     *  
     */
    public boolean isSelected() {
        return ((WorkflowInPortFigure)getFigure()).isSelected();
    }

    /**
     * The context menu ({@link WorkflowContextMenuProvider#buildContextMenu(
     * org.eclipse.jface.action.IMenuManager)}) reads and resets the selection 
     * state.
     * 
     * @param isSelected sets and resets the selectino state of the figure
     * 
     * @see WorkflowContextMenuProvider#buildContextMenu(
     *  org.eclipse.jface.action.IMenuManager)
     *  @see WorkflowInPortFigure#setSelected(boolean)
     */
    public void setSelected(final boolean isSelected) {
        ((WorkflowInPortFigure)getFigure()).setSelected(isSelected);
    }

    /**
     * This returns the (single !) connection that has this in-port as a target.
     *
     * @return singleton list containing the connection, or an empty list. Never
     *         <code>null</code>
     *
     * @see org.eclipse.gef.GraphicalEditPart#getTargetConnections()
     */
    @Override
    public List<ConnectionContainer> getModelSourceConnections() {

        Set<ConnectionContainer> containers =
                getManager().getOutgoingConnectionsFor(
                        getNodeContainer().getID(),
                        getId());

        //ConnectionContainer container = getManager().getIncomingConnectionFor(
        //        getNodeContainer().getID(), getId());
        LOGGER.debug("manager: " + getManager());
        LOGGER.debug("node container: " + getNodeContainer());
        List<ConnectionContainer>conns = new ArrayList<ConnectionContainer>();

        for (ConnectionContainer c : containers) {
            LOGGER.debug("connection container: " + c);
        }

//        LOGGER.debug("incoming connection: " + container);
        if (containers != null) {
            conns.addAll(containers);
        }
        return conns;
    }


    /**
     * Tries to build the tooltip from the port name and if this is a data
     * outport and the node is configured/executed, it appends also the number
     * of columns and rows.
     */
    @Override
    public void rebuildTooltip() {
        NodeOutPort port = ((WorkflowInPort)getNodeContainer().getInPort(
                getId())).getUnderlyingPort();
        String tooltip = getTooltipText(PORT_NAME + ": " + getId(), port);
        ((NewToolTipFigure)getFigure().getToolTip()).setText(tooltip);
    }


    /**
     *
     * @return empty list, as out-ports are never target for connections
     *
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart
     *      #getModelSourceConnections()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected List getModelTargetConnections() {
        return Collections.EMPTY_LIST;
    }

}

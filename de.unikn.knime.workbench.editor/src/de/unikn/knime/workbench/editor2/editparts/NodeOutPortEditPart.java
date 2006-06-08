/* 
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
 *   31.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.editparts;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;

import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.workbench.editor2.figures.NodeOutPortFigure;

/**
 * Edit part for <code>NodeOutPort</code>s.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeOutPortEditPart extends AbstractPortEditPart {

    /**
     * @param portID The ID of this out port
     */
    public NodeOutPortEditPart(final int portID) {
        super(portID);
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
     */
    @Override
    protected IFigure createFigure() {

        // Create the figure, we need the number of ports from the parent
        // container
        NodeContainer container = getNodeContainer();
        boolean isModelPort = container.isPredictorOutPort(getId());
        NodeOutPortFigure portFigure = new NodeOutPortFigure(getId(), container
                .getNrPredictorOutPorts(), container.getNrDataOutPorts(),
                container.getOutputDescription(getId()),
                isModelPort);

        return portFigure;
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
    public List getModelSourceConnections() {
        List<ConnectionContainer> containers;
        containers = getManager().getOutgoingConnectionsAt(getNodeContainer(),
                getId());

        if (containers != null) {
             return containers;
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * 
     * @return empty list, as out-ports are never target for connections
     * 
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart
     *      #getModelSourceConnections()
     */
    @Override
    protected List getModelTargetConnections() {
        return Collections.EMPTY_LIST;
    }
}

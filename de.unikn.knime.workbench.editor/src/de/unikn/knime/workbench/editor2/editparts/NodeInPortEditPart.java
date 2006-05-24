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
 *   31.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.editparts;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;

import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.workbench.editor2.figures.NodeInPortFigure;

/**
 * Edit Part for a <code>NodeInPort</code>.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeInPortEditPart extends AbstractPortEditPart {

    /**
     * @param portID The id for this incoming port
     */
    public NodeInPortEditPart(final int portID) {
        super(portID);
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
     */
    protected IFigure createFigure() {

        // Create the figure, we need the number of ports from the parent
        // container
        NodeContainer container = getNodeContainer();
        boolean isModelPort = container.isPredictorInPort(getId());
        NodeInPortFigure portFigure = new NodeInPortFigure(getId(), container
                .getNrPredictorInPorts(), container.getNrDataInPorts(),
                container.getInputDescription(getId()), isModelPort);

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
    public List getModelTargetConnections() {

        ConnectionContainer container = getManager().getIncomingConnectionAt(
                getNodeContainer(), getId());

        if (container != null) {
            return Collections.singletonList(container);
        }

        return Collections.EMPTY_LIST;

    }

    /**
     * @return empty list, as in-ports are never source for connections
     * 
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart
     *      #getModelSourceConnections()
     */
    protected List getModelSourceConnections() {
        return Collections.EMPTY_LIST;
    }

}

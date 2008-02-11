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
 *   31.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.knime.core.node.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;

import org.knime.workbench.editor2.figures.NodeInPortFigure;

/**
 * Edit Part for a <code>NodeInPort</code>.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeInPortEditPart extends AbstractPortEditPart {
    /**
     * @param type the type of the port
     * @param portID The id for this incoming port
     */
    public NodeInPortEditPart(final PortType type, final int portID) {
        super(type, portID, true);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        // Create the figure, we need the number of ports from the parent
        // container
        NodeContainer container = getNodeContainer();
        NodeInPortFigure portFigure = new NodeInPortFigure(getType(),
                getId(), container.getNrInPorts(),
                container.getInPort(getId()).getPortName());

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
    public List getModelTargetConnections() {
        ConnectionContainer container = getManager().getIncomingConnectionFor(
                getNodeContainer().getID(), getId());

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
    @Override
    protected List getModelSourceConnections() {
        return Collections.EMPTY_LIST;
    }
}

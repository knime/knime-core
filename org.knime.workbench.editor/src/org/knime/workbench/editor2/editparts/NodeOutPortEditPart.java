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
 *   31.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.IFigure;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.workbench.editor2.figures.NodeOutPortFigure;

/**
 * Edit part for a {@link NodeOutPort}.
 * Model: {@link NodeOutPort}
 * View: {@link NodeOutPortFigure}
 * Controller: {@link NodeOutPortEditPart}
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeOutPortEditPart extends AbstractPortEditPart {
    
    /**
     * @param type the port type
     * @param portIndex the port index
     */
    public NodeOutPortEditPart(final PortType type, final int portIndex) {
        super(type, portIndex, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        // Create the figure, we need the number of ports from the parent
        // container
        NodeContainer container = getNodeContainer();
        NodeOutPort port = container.getOutPort(getIndex());
        String tooltip = getTooltipText(port.getPortName(), port);
        NodeOutPortFigure portFigure =
                new NodeOutPortFigure(getType(), getIndex(), container
                        .getNrOutPorts(), tooltip);
        return portFigure;
    }

    /**
     * This returns the (single !) connection that has this in-port as a target.
     *
     * @return singleton list containing the connection, or an empty list. Never
     *         <code>null</code>
     *
     * {@inheritDoc}
     */
    @Override
    public List<ConnectionContainer> getModelSourceConnections() {
        Set<ConnectionContainer> containers =
                getManager().getOutgoingConnectionsFor(
                        getNodeContainer().getID(),
                        getIndex());
        List<ConnectionContainer>conns = new ArrayList<ConnectionContainer>();
        if (containers != null) {
            conns.addAll(containers);
        }
        return conns;
    }

    /**
     * 
     * @return empty list, as out-ports are never target for connections
     * 
     * {@inheritDoc}
     */
    @Override
    protected List<ConnectionContainer> getModelTargetConnections() {
        return EMPTY_LIST;
    }
}

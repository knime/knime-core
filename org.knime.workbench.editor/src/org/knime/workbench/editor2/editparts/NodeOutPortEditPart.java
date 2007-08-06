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
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.figures.NewToolTipFigure;
import org.knime.workbench.editor2.figures.NodeOutPortFigure;

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
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {

        // Create the figure, we need the number of ports from the parent
        // container
        NodeContainer container = getNodeContainer();
        boolean isModelPort = container.isPredictorOutPort(getId());
        NodeOutPortFigure portFigure =
                new NodeOutPortFigure(getId(), container
                        .getNrModelContentOutPorts(), container
                        .getNrDataOutPorts(),
                        container.getOutportName(getId()), isModelPort);

        // BW: double click on port has been disabled
        // portFigure.addMouseListener(this);

        return portFigure;
    }

    /**
     * Tries to build the tooltip from the port name and if this is a data
     * outport and the node is configured/executed, it appends also the number
     * of columns and rows
     */
    public void rebuildTooltip() {
        String name = getNodeContainer().getOutportName(getId());
        int cols = getNodeContainer().getNumOutportCols(getId());
        int rows = getNodeContainer().getNumOutportRows(getId());
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (cols >= 0) {
            sb.append(" (Cols: " + cols);
            if (rows >= 0) {
                sb.append(", Rows: " + rows + ")");
            } else {
                sb.append(")");
            }
        }
        ((NewToolTipFigure)getFigure().getToolTip()).setText(sb.toString());
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
        containers =
                getManager().getOutgoingConnectionsAt(getNodeContainer(),
                        getId());

        if (containers != null) {
            return containers;
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModelPort() {
        return getNodeContainer().isPredictorOutPort(getId());
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

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
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;

/**
 * Figure for displaying a <code>NodeOutPort</code> inside a node.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeOutPortFigure extends AbstractPortFigure {
    private final int m_id;

    /**
     * 
     * @param id The id of the port, needed to determine the position inside the
     *            surrounding node visual
     * @param type the port type
     * @param numPorts total number of ports
     * @param tooltip the tooltip for the node
     */
    public NodeOutPortFigure(final PortType type,
            final int id,
            final int numPorts, final String tooltip) {

        super(type, numPorts);
        m_id = id;
//        setOpaque(false);
        setToolTip(new NewToolTipFigure(tooltip));
//        setFill(true);
//        setOutline(true);
    }

    /**
     * Create a point list for the triangular figure (a polygon).
     * 
     * @param r The bounds
     * @return the pointlist (size=3)
     * {@inheritDoc}
     */
    @Override
    protected PointList createShapePoints(final Rectangle r) {
        if (getType().equals(BufferedDataTable.TYPE)) {
            PointList points = new PointList(3);
            points.addPoint(r.getLeft().getCopy().translate(NODE_PORT_SIZE,
                    -(NODE_PORT_SIZE / 2)));
            points.addPoint(r.getLeft().getCopy().translate(
                    NODE_PORT_SIZE * 2, 0));
            points.addPoint(r.getLeft().getCopy()
                    .translate(NODE_PORT_SIZE, (NODE_PORT_SIZE / 2)));
            return points;
        } 
        PointList points = new PointList(4);
        points.addPoint(r.getLeft().getCopy().translate(NODE_PORT_SIZE,
                -(NODE_PORT_SIZE / 2 - 0)));
        points.addPoint(r.getLeft().getCopy().translate(NODE_PORT_SIZE * 2,
                -(NODE_PORT_SIZE / 2 - 0)));
        points.addPoint(r.getLeft().getCopy().translate(NODE_PORT_SIZE * 2,
                (NODE_PORT_SIZE / 2 + 0)));
        points.addPoint(r.getLeft().getCopy().translate(NODE_PORT_SIZE,
                (NODE_PORT_SIZE / 2 + 0)));
        return points;
    }

    /**
     * Returns the preffered size of a port. A port is streched in length,
     * depending on the number of ports. Always try to fill up as much height as
     * possible.
     * 
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(final int wHint, final int hHint) {
        Dimension d = new Dimension();

        d.height = (getParent().getBounds().height) / getNrPorts();
        d.width = getParent().getBounds().width / 4;
        return d;
    }

    /**
     * @return The <code>RelativeLocator</code> that places this figure on the
     *         right side (y offset corresponds to the number of the port).
     */
    @Override
    public Locator getLocator() {
        return new NodePortLocator(
                (NodeContainerFigure)getParent().getParent(),
            false, getNrPorts(), m_id, getType());
    }
}

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
 * Figure for displaying a <code>NodeInPort</code> inside a node.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeInPortFigure extends AbstractPortFigure {
    private final int m_id;

    /**
     * 
     * @param type the type of the port
     * @param id The id of the port, needed to determine the position inside the
     *            surrounding node visual
     * @param numPorts the total ports
     * @param tooltip the tooltip text
     */
    public NodeInPortFigure(final PortType type, final int id,
            final int numPorts, final String tooltip) {
        super(type, numPorts);
        m_id = id;
        setToolTip(new NewToolTipFigure(tooltip));
        setOpaque(false);
        setFill(true);
    }

    /**
     * Create a point list for the port figure (a polygon).
     * 
     * There are two shapes. A triangular one for the data ports and a square
     * shaped one for the model ports.
     * 
     * @param r The bounds
     * @return the pointlist (size=3)
     */
    @Override
    protected PointList createShapePoints(final Rectangle r) {
        if (getType().equals(BufferedDataTable.TYPE)) {
            PointList points = new PointList(3);
            points.addPoint(r.getRight().getCopy().translate(
                    -NODE_PORT_SIZE * 2 - 3,
                    -(NODE_PORT_SIZE / 2)));
            points.addPoint(r.getRight().getCopy().translate(
                    -NODE_PORT_SIZE - 3, 0));
            points.addPoint(r.getRight().getCopy().translate(
                    -NODE_PORT_SIZE * 2 - 3,
                    (NODE_PORT_SIZE / 2)));
            return points;
        }
        PointList points = new PointList(4);
        points.addPoint(r.getRight().getCopy().translate(
                -NODE_PORT_SIZE * 2 - 3,
                -((NODE_PORT_SIZE - 1) / 2)));
        points.addPoint(r.getRight().getCopy().translate(
                -NODE_PORT_SIZE - 3,
                -((NODE_PORT_SIZE - 1) / 2)));
        points.addPoint(r.getRight().getCopy().translate(
                -NODE_PORT_SIZE - 3,
                ((NODE_PORT_SIZE - 1) / 2)));
        points.addPoint(r.getRight().getCopy().translate(
                -NODE_PORT_SIZE * 2 - 3,
                ((NODE_PORT_SIZE - 1) / 2 - 1)));
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
//        System.out.println("parent: " + getParent());
//        System.out.println("parent's bounds: " + getParent().getBounds());
        d.height = (getParent().getBounds().height) / getNrPorts();
        d.width = NODE_PORT_SIZE;
        return d;
    }

    /**
     * @return The <code>RelativeLocator</code> that places this figure on the
     *         left side (y offset corresponds to the number of the port).
     * {@inheritDoc}
     */
    @Override
    public Locator getLocator() {
        return new NodePortLocator((NodeContainerFigure)getParent().getParent(),
                true, getNrPorts(),
                m_id, getType());
    }
}

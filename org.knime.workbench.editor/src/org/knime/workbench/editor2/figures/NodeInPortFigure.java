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
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Figure for displaying a <code>NodeInPort</code> inside a node.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeInPortFigure extends AbstractNodePortFigure {
    private int m_id;

    private boolean m_isModelPort;

    /**
     * 
     * @param id The id of the port, needed to determine the position inside the
     *            surrounding node visual
     * @param numModelPorts the number of model ports for this figure
     * @param numDataPorts the number of data ports for this figure
     * @param tooltip The tooltip text for this port
     * @param isModelPort indicates a model inPort, displayed with a diff. shape
     */
    public NodeInPortFigure(final int id, final int numModelPorts,
            final int numDataPorts, final String tooltip,
            final boolean isModelPort) {
        super(numModelPorts, numDataPorts);
        m_id = id;
        m_isModelPort = isModelPort;
        setToolTip(new NewToolTipFigure(tooltip));
        setOpaque(false);
        setFill(true);
    }

    /**
     * 
     * @see de.unikn.knime.workbench.editor2.figures.AbstractNodePortFigure
     *  #isModelPort()
     */
    @Override
    protected boolean isModelPort() {
        return m_isModelPort;
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
        if (!m_isModelPort) {
            PointList points = new PointList(3);
            points.addPoint(r.getRight().getCopy().translate(-WIDTH * 2 - 3,
                    -(HEIGHT / 2)));
            points.addPoint(r.getRight().getCopy().translate(-WIDTH - 3, 0));
            points.addPoint(r.getRight().getCopy().translate(-WIDTH * 2 - 3,
                    (HEIGHT / 2)));
            return points;
        } else {
            PointList points = new PointList(4);
            points.addPoint(r.getRight().getCopy().translate(-WIDTH * 2 - 3,
                    -((HEIGHT - 1) / 2)));
            points.addPoint(r.getRight().getCopy().translate(-WIDTH - 3,
                    -((HEIGHT - 1) / 2)));
            points.addPoint(r.getRight().getCopy().translate(-WIDTH - 3,
                    ((HEIGHT - 1) / 2)));
            points.addPoint(r.getRight().getCopy().translate(-WIDTH * 2 - 3,
                    ((HEIGHT - 1) / 2 - 1)));
            return points;
        }
    }

    /**
     * Returns the preffered size of a port. A port is streched in length,
     * depending on the number of ports. Always try to fill up as much height as
     * possible.
     * 
     * @see org.eclipse.draw2d.IFigure#getPreferredSize(int, int)
     */
    @Override
    public Dimension getPreferredSize(final int wHint, final int hHint) {
        Dimension d = new Dimension();

        d.height = (getParent().getBounds().height) / getNumPorts();
        d.width = WIDTH;
        return d;
    }

    /**
     * @return The <code>RelativeLocator</code> that places this figure on the
     *         left side (y offset corresponds to the number of the port).
     */
    @Override
    public Locator getLocator() {
        return new PortLocator((NodeContainerFigure)getParent().getParent(),
                PortLocator.TYPE_INPORT, getNumModelPorts(), getNumDataPorts(),
                m_id, m_isModelPort);
    }
}

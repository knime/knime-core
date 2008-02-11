/* -------------------------------------------------------------------
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
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.PortType;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowInPortFigure extends AbstractWorkflowPortFigure {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowInPortFigure.class);

    private boolean m_isSelected = false;

    /**
     *
     * @param type port type
     * @param nrOfPorts total number of ports
     * @param portIndex port index
     * @param tooltip initial tooltip
     */
    public WorkflowInPortFigure(final PortType type,
            final int nrOfPorts, final int portIndex, final String tooltip) {
        super(type, nrOfPorts, portIndex);
        setToolTip(new NewToolTipFigure(tooltip));
    }

    public void setSelected(final boolean isSelected) {
        m_isSelected = isSelected;
    }

    public boolean isSelected() {
        return m_isSelected;
    }

    @Override
    public void setBounds(final Rectangle rect) {
        LOGGER.debug("set bounds to: " + rect);
        super.setBounds(rect);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        Rectangle parent = getParent().getBounds().getCopy();
        int yPos = (parent.height / (getNrPorts() + 1)) * (getPortIndex() + 1);
        setBounds(new Rectangle(new Point(0, yPos),
                new Dimension(SIZE, SIZE)));
        fireFigureMoved();
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void drawSquare(final Graphics g) {
        Rectangle parent = getParent().getBounds().getCopy();
        int yPos = (parent.height / (getNrPorts() + 1)) * (getPortIndex() + 1);
        getBounds().x = 0;
        getBounds().y = yPos;
        getBounds().width = SIZE;
        getBounds().height = SIZE;
        Rectangle r = new Rectangle(new Point(0, yPos),
                new Dimension(SIZE, SIZE));
//        LOGGER.info("drawing square " + r + " bounds: " + bounds);
        g.fillRectangle(r);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void drawTriangle(final Graphics g) {
        Rectangle parent = getParent().getBounds().getCopy();
        int yPos = (parent.height / (getNrPorts() + 1)) * (getPortIndex() + 1);
        getBounds().x = 0;
        getBounds().y = yPos;
        getBounds().width = SIZE;
        getBounds().height = SIZE;
        Rectangle r = getBounds().getCopy();
        PointList list = new PointList(3);
        list.addPoint(r.x, r.y);
        list.addPoint(r.width, r.y + (r.height / 2));
        list.addPoint(r.x, r.y + r.height);
//        LOGGER.info("drawing triangle..." + r);
        g.drawPolygon(list);
    }

    @Override
    public Locator getLocator() {
        return new WorkflowPortLocator(getType(), getPortIndex(),
                true, getNrPorts());
    }



}

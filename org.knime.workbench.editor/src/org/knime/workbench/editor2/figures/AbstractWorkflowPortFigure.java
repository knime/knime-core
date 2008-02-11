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
 *   24.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.Shape;
import org.knime.core.node.ModelContent;
import org.knime.core.node.PortType;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractWorkflowPortFigure extends Shape {

    /** Constant for the width and height of the port figure. */
    protected static final int SIZE = 20;

    private final int m_nrOfPorts;
    private final int m_portIndex;
    private final PortType m_portType;

    /**
     *
     * @param type port type
     * @param nrOfPorts total number of ports
     * @param portIndex port index
     */
    public AbstractWorkflowPortFigure(final PortType type,
            final int nrOfPorts, final int portIndex) {
        m_portType = type;
        m_nrOfPorts = nrOfPorts;
        m_portIndex = portIndex;
        setBackgroundColor(ColorConstants.darkBlue);
        setFill(true);
        setFillXOR(false);
        setOutline(true);
        setForegroundColor(ColorConstants.black);
    }

    protected PortType getType() {
        return m_portType;
    }

    /**
     *
     * @return total number of ports
     */
    protected int getNrPorts() {
        return m_nrOfPorts;
    }

    /**
     *
     * @return index of this port
     */
    protected int getPortIndex() {
        return m_portIndex;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isOpaque() {
        return true;
    }


    @Override
    public void paint(final Graphics graphics) {
        fireFigureMoved();
        super.paint(graphics);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void outlineShape(final Graphics graphics) {
        if (!m_portType.equals(ModelContent.TYPE)) {
            drawTriangle(graphics);
        }
    }


    /**
     *
     * {@inheritDoc}
     */
     @Override
     protected void fillShape(final Graphics g) {
         if (m_portType.equals(ModelContent.TYPE)) {
             drawSquare(g);
         }
     }

     /**
      * Draws a data inport (triangle).
      *
      * @param g graphics context
      */
     protected abstract void drawTriangle(final Graphics g);

     /**
      * Draws a model port (filled square).
      *
      * @param g graphics context
      */
     protected abstract void drawSquare(final Graphics g);


     public abstract Locator getLocator();
}

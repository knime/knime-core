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
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DatabaseContent;
import org.knime.core.node.ModelContent;
import org.knime.core.node.PortType;

/**
 * Abstract figure for common displaying behaviour of node ports.
 *
 * @author Florian Georg, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractNodePortFigure extends Shape {
    /** width of a port figure. * */
    public static final int WIDTH = 9;

    /** height of a port figure. * */
    public static final int HEIGHT = 9;

    private final int m_numPorts;

    private final PortType m_type;

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            AbstractNodePortFigure.class);


    /**
     * Abstract consturctor, must be called.
     *
     * @param type port type
     * @param numPorts number of ports
     */
    public AbstractNodePortFigure(final PortType type,
            final int numPorts) {
        m_type = type;
        m_numPorts = numPorts;
    }

    /**
     *
     * @return type of the port
     */
    public PortType getType() {
        return m_type;
    }

    /**
     * We need to set the colors explicitly dependend on the {@link PortType}.
     * Currently supported are {@link BufferedDataTable#TYPE} : black, 
     * {@link ModelContent#TYPE} : blue, {@link DBConnection#TYPE} : 
     * dark yellow.
     * 
     * @return the background color, dependend on the {@link PortType}
     * {@inheritDoc}
     */
    @Override
    public Color getBackgroundColor() {
        Color color = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
        if (getType().equals(ModelContent.TYPE)) {
            // model
            color = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
        } else if (getType().equals(BufferedDataTable.TYPE)) {
            // data
            color = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
        } else if (getType().equals(DatabaseContent.TYPE)) {
            // database
            color = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW);
        }
        return color;
    }
    
    /**
     * The color is determined with {@link #getBackgroundColor()} and set.
     * 
     * {@inheritDoc}
     */
    @Override
    public void paintFigure(final Graphics graphics) {
        graphics.setBackgroundColor(getBackgroundColor());        
        super.paintFigure(graphics);
    }

    /**
     * Fills the shape, the points of the actual shape are set in 
     * {@link NodeInPortFigure#createShapePoints(Rectangle)} and 
     * {@link NodeOutPortFigure#createShapePoints(Rectangle)}. Only data ports
     * (ports of type {@link BufferedDataTable#TYPE})are outlined, all other 
     * port types are filled.
     *
     * {@inheritDoc}
     * @see NodeInPortFigure#createShapePoints(Rectangle)
     * @see NodeOutPortFigure#createShapePoints(Rectangle)
     */
    @Override
    protected void fillShape(final Graphics graphics) {
        Rectangle r = getBounds().getCopy().shrink(3, 3);
        PointList points = createShapePoints(r);
        // data ports are not filled, model ports are filled
        if (!getType().equals(BufferedDataTable.TYPE)) {
            graphics.fillPolygon(points);
        }
    }


    /**
     * Outlines the shape, the points of the actual shape are set in 
     * {@link NodeInPortFigure#createShapePoints(Rectangle)} and 
     * {@link NodeOutPortFigure#createShapePoints(Rectangle)}. Only data ports
     * (ports of type {@link BufferedDataTable#TYPE})are outlined, all other 
     * port types are filled.
     *
     * {@inheritDoc}
     * @see NodeInPortFigure#createShapePoints(Rectangle)
     * @see NodeOutPortFigure#createShapePoints(Rectangle)
     */
    @Override
    protected void outlineShape(final Graphics graphics) {
        Rectangle r = getBounds().getCopy().shrink(3, 3);
        PointList points = createShapePoints(r);
        if (getType().equals(BufferedDataTable.TYPE)) {            
            graphics.drawPolygon(points);
        }
    }

    /**
     * Create a point list for the triangular figure (a polygon).
     *
     * @param r The bounds
     * @return the pointlist (size=3)
     */
    protected abstract PointList createShapePoints(final Rectangle r);


    /**
     * Children must return a <code>Locator</code> that calculate the position
     * inside the hosting figure.
     *
     * @return The locator
     */
    public abstract Locator getLocator();

    /**
     * @return Returns the allover number of ports.
     */
    public int getNumPorts() {
        return m_numPorts;
    }

}

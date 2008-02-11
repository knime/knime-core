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

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ModelContent;
import org.knime.core.node.PortType;

/**
 * Abstract figure for common displaying behaviour of node ports.
 *
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractNodePortFigure extends Shape {
    /** width of a port figure. * */
    public static final int WIDTH = 9;

    /** height of a port figure. * */
    public static final int HEIGHT = 9;

    private boolean m_hasData;

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
     * @return The background color, depending on the current state
     * @see org.eclipse.draw2d.IFigure#getBackgroundColor()
     */
    @Override
    public Color getBackgroundColor() {
        return (m_hasData ? ColorConstants.green : ColorConstants.white);
    }

    /**
     * TODO FIXME isConnected state is never set, so we can't get nice red/green
     * colors.
     *
     * @return The foreground color, depending on the current state
     * @see org.eclipse.draw2d.IFigure#getForegroundColor()
     */
    @Override
    public Color getForegroundColor() {
        // return (m_isConnected ? ColorConstants.green : ColorConstants.red);
        return ColorConstants.black;
    }

    /**
     * @return Returns the hasData.
     */
    public boolean getHasData() {
        return m_hasData;
    }

    /**
     * @param hasData The hasData to set.
     */
    public void setHasData(final boolean hasData) {
        m_hasData = hasData;

        setFill(m_hasData);
    }

    /**
     * We need to set the colors explicitly.
     *
     * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
     */
    @Override
    protected void fillShape(final Graphics graphics) {
        if (getType().equals(ModelContent.TYPE)) {
            // model
            graphics.setBackgroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_BLUE));
        } else if (getType().equals(BufferedDataTable.TYPE)) {
            // data
            graphics.setBackgroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_BLACK));
        } else {
            // unknown type
            graphics.setBackgroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_GRAY));
        }

        // TODO: database port

        // graphics.setBackgroundColor(getBackgroundColor());
        // TODO: different for workflow port and node port!!!
        Rectangle r;
            r = getBounds().getCopy().shrink(3, 3);
        PointList points = createShapePoints(r);
        // data ports are not filled, model ports are filled
        if (getType().equals(ModelContent.TYPE)) {
            graphics.fillPolygon(points);
        } else {
            graphics.drawPolygon(points);
        }

        // graphics.fillRectangle(getBounds());
    }

    /**
     * NOT USED AT THE MOMENT.
     *
     * @see org.eclipse.draw2d.Shape#outlineShape(org.eclipse.draw2d.Graphics)
     */
    @Override
    protected void outlineShape(final Graphics graphics) {
        if (getType().equals(ModelContent.TYPE)) {
            graphics.setForegroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_BLUE));
        } else if (getType().equals(BufferedDataTable.TYPE)){
            graphics.setForegroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_BLACK));
        } else {
            graphics.setForegroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_GRAY));
        }

        // TODO: data base port

        // graphics.setForegroundColor(getForegroundColor());
        // Rectangle r = getBounds().getCopy().shrink(2, 2);
        // TODO debug border
        // graphics.drawRectangle(r);

        // get polygon from implementation...
        // PointList points = createShapePoints(r);

        // graphics.drawPolygon(points);
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

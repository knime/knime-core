/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
package de.unikn.knime.workbench.editor2.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

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

    private boolean m_isConnected;

    private boolean m_hasData;

    private int m_numModelPorts;

    private int m_numDataPorts;

    /**
     * Abstract consturctor, must be called.
     * 
     * @param numModelPorts Number of model ports
     * @param numDataPorts Number of data ports
     */
    public AbstractNodePortFigure(final int numModelPorts,
            final int numDataPorts) {

        m_numModelPorts = numModelPorts;
        m_numDataPorts = numDataPorts;
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
     * @return Returns the isConnected.
     */
    public boolean isConnected() {
        return m_isConnected;
    }

    /**
     * @param isConnected The isConnected to set.
     */
    public void setConnected(final boolean isConnected) {
        m_isConnected = isConnected;
    }

    /**
     * We need to set the colors explicitly.
     * 
     * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
     */
    @Override
    protected void fillShape(final Graphics graphics) {
        if (isModelPort()) {
            graphics.setBackgroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_BLUE));
        } else {
            graphics.setBackgroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_BLACK));
        }
        // graphics.setBackgroundColor(getBackgroundColor());
        Rectangle r = getBounds().getCopy().shrink(3, 3);
        PointList points = createShapePoints(r);

        // data ports are not filled, model ports are filled
        if (isModelPort()) {
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
        if (isModelPort()) {
            graphics.setForegroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_BLUE));
        } else {
            graphics.setForegroundColor(Display.getCurrent().getSystemColor(
                    SWT.COLOR_BLACK));
        }

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
     * Whether this is a model or data port.
     * 
     * @return model port (true - false)
     */
    protected abstract boolean isModelPort();

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
        return m_numDataPorts + m_numModelPorts;
    }

    /**
     * @return the number of data ports.
     */
    int getNumDataPorts() {
        return m_numDataPorts;
    }

    /**
     * @return the number of model ports.
     */
    int getNumModelPorts() {
        return m_numModelPorts;
    }
}

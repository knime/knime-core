/*
 * ------------------------------------------------------------------
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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.scattermatrix;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.viz.plotter.Axis;
import org.knime.base.node.viz.plotter.scatter.DotInfo;
import org.knime.base.node.viz.plotter.scatter.DotInfoArray;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;


/**
 * Holds a matrix of 
 * {@link org.knime.base.node.viz.plotter.scattermatrix.ScatterMatrixElement}s.
 * This class only paints the surrounding rectangles of the matrix elements, 
 * the painting of the actual dots is done by the 
 * {@link org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane}.
 * In addition to the scatter matrix elements the coordinates of the scatter
 * matrix elements are plotted at the borders of the drawing pane.
 * The {@link #setBackground(Color)} is overriden, since it sets the color of
 * the scatter matrix elements and not of the whole component.
 * 
 * The 
 * {@link org.knime.base.node.viz.plotter.scattermatrix.ScatterMatrixElement}s
 *  know there associated dots. By setting them with the 
 *  {@link #setScatterMatrixElements(ScatterMatrixElement[][])}, the 
 *  {@link org.knime.base.node.viz.plotter.scatter.DotInfo}s are put into a 
 *  {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray}, which can then
 *  be painted with the inherited functionality of the 
 *  {@link org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane}.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterMatrixDrawingPane extends ScatterPlotterDrawingPane {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            ScatterMatrixDrawingPane.class);
//    
    private ScatterMatrixElement[][] m_matrixElements;
    
    private int m_hMargin = 50;
    
    private int m_vMargin = 40;
    
    private Color m_backgroundColor = this.getBackground();
    
    
    /**
     * Sets the background color to white.
     *
     */
    public ScatterMatrixDrawingPane() {
        super.setBackground(Color.white);
    }
    
    /**
     * 
     * @param hMargin sets the horizontal margin.
     */
    public void setHorizontalMargin(final int hMargin) {
        m_hMargin = hMargin;
    }
    
    /**
     * 
     * @param vMargin the vertical margin
     */
    public void setVerticalMargin(final int vMargin) {
        m_vMargin = vMargin;
    }

    
    /**
     * 
     * @param elements the rectangles with the coordinates.
     */
    public void setScatterMatrixElements(
            final ScatterMatrixElement[][] elements) {
        m_matrixElements = elements;
        if (elements != null) {
        List<DotInfo>dotList = new ArrayList<DotInfo>();
        for (int i = 0; i < m_matrixElements.length; i++) {
                for (int j = 0; j < m_matrixElements[i].length; j++) {
                    // matrix element might be null (if no rows available) since
                    // the array is initialized with column length
                    if (m_matrixElements[i][j] == null) {
                        continue;
                    }
                    dotList.addAll(m_matrixElements[i][j].getDots());
                }
            }
        DotInfo[] dots = new DotInfo[dotList.size()];
        dotList.toArray(dots);
        setDotInfoArray(new DotInfoArray(dots));
//        repaint();
        }
    }
    
    /**
     * 
     * @return the scatter matrix elements.
     */
    public ScatterMatrixElement[][] getMatrixElements() {
        return m_matrixElements;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setBackground(final Color bg) {
        m_backgroundColor = bg;
        repaint();
    }
    
    
    /**
     * Paints the rectangles of the scatter matrix elements and the vertical and
     * horizontal coordinates at the border of the drawing pane. The painting 
     * of the dots is inherited from the 
     *{@link org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane}.
     * 
     * {@inheritDoc}
     */
    @Override
    public void paintContent(final Graphics g) {
        if (getDotInfoArray() == null) {
            return;
        }
        if (m_matrixElements == null) {
            return;
        }
        for (int i = 0; i < m_matrixElements.length; i++) {
            for (int j = 0; j < m_matrixElements[i].length; j++) {
                ScatterMatrixElement element = m_matrixElements[i][j];
                // matrix element might be null (if no rows available) since 
                // the array is initialized with column length
                if (element == null) {
                    continue;
                }
                g.drawRect(element.getCorner().x, element.getCorner().y, 
                        element.getWidth(), element.getHeight());
                // paint the background color of the matrix elements
                // not the background fo the whole panel but only of the matrix 
                // elements should be changeable
                Color backupC = g.getColor();
                g.setColor(m_backgroundColor);
                g.fillRect(element.getCorner().x + 1, element.getCorner().y + 1,
                        element.getWidth() - 1, element.getHeight() - 1);
                g.setColor(backupC);
                // for the y axis draw vertical axes
                if (j  == 0 && i % 2 == 1) {
                    // paint at top
                    int y = m_hMargin - 6;
                    paintHorizontalCoordinate(g, element, y, true);
                } 
                if (j % 2 == 1 && i == m_matrixElements.length - 1) {
                    // paint right
                    int x = getWidth() - m_vMargin + 3;
                    paintVerticalCoordinate(g, element, x, false);
                }
                if (j % 2 == 0 && i == 0) {
                    // paint left
                    int x = m_vMargin - 6;
                    paintVerticalCoordinate(g, element, x, true);
                }
                if (j == m_matrixElements.length - 1 && i % 2 == 0) {
                    // paint at bottom
                    int y = getHeight() - m_hMargin + 3;
                    paintHorizontalCoordinate(g, element, y, false);
                }
            }
            super.paintContent(g);
        } 
    }
    
    /**
     * Paints the vertical coordinates at the border of the drawing pane.
     * 
     * @param g graphics
     * @param element element to provide position and coordinate
     * @param x the x position
     * @param left true if the coordinate is painted on the left side.
     */
    protected void paintVerticalCoordinate(final Graphics g, 
            final ScatterMatrixElement element, final int x, 
            final boolean left) {
        Axis paintAxis = new Axis(Axis.VERTICAL, element.getHeight(), !left);
        paintAxis.setCoordinate(element.getXCoordinate());
        int axisY = element.getCorner().y; 
        int transX = x - Axis.SIZE;
        g.translate(transX, axisY);
        paintAxis.paintComponent(g);
        g.translate(-transX, -axisY);
    }

    /**
     * Paints the horizontal coordinates of the matrix elements at the border 
     * of the drawing pane.
     * 
     * @param g graphics
     * @param element the element to provide position and coordinate
     * @param y y position
     * @param top true if the coordinate is painted at the top.
     */
    protected void paintHorizontalCoordinate(final Graphics g, 
            final ScatterMatrixElement element, final int y, 
            final boolean top) {
        Axis paintAxis = new Axis(Axis.HORIZONTAL, element.getWidth(), top);
        paintAxis.setCoordinate(element.getXCoordinate());
        int axisX = element.getCorner().x; 
        g.translate(axisX, y);
        paintAxis.paintComponent(g);
        g.translate(-axisX, -y);
    }
    
    
}

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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.viz.plotter.LabelPaintUtil;
import org.knime.base.node.viz.plotter.scatter.DotInfo;
import org.knime.base.node.viz.plotter.scatter.DotInfoArray;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;
import org.knime.base.util.coordinate.CoordinateMapping;
import org.knime.base.util.coordinate.NominalCoordinate;
import org.knime.base.util.coordinate.NominalCoordinateMapping;

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
    
    
    private static final int TICK_SIZE = 4;
    
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
     * @see org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane
     * #paintContent(java.awt.Graphics)
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
     * @param element element to provide posiotn and coordinate
     * @param x the x position
     * @param left true if the coordinate is painted on the left side.
     */
    protected void paintVerticalCoordinate(final Graphics g, 
            final ScatterMatrixElement element, final int x, 
            final boolean left) {
        // draw the coordinate
        g.drawLine(x, element.getCorner().y, x, element.getCorner().y 
                + element.getHeight());
        // draw top and bottom borders
        g.drawLine(x - TICK_SIZE / 2, element.getCorner().y, 
                x + TICK_SIZE / 2, element.getCorner().y);
        g.drawLine(x - TICK_SIZE / 2, 
                element.getCorner().y + element.getHeight(), 
                x + TICK_SIZE / 2, 
                element.getCorner().y + element.getHeight());
        CoordinateMapping[] mappings;
        boolean rotate = false;
        if (element.getYCoordinate().isNominal()) {
            mappings = ((NominalCoordinate)element.getYCoordinate())
                .getReducedTickPositions(element.getHeight());
            rotate = LabelPaintUtil.rotateLabels(mappings, 
                    m_vMargin - TICK_SIZE, 
                    g.getFontMetrics());
        } else {
            mappings = element.getYCoordinate()
                .getTickPositions(element.getHeight(), true);
        }
        for (CoordinateMapping mapping : mappings) {
            int value = (int)mapping.getMappingValue();
            int y = element.getCorner().y + (element.getHeight() - value);
            g.drawLine(x - TICK_SIZE / 2, y, x + TICK_SIZE / 2, y);
            String label = mapping.getDomainValueAsString();
            int labelY = y; 
            int rectHeight = (int)element.getYCoordinate()
            .getUnusedDistBetweenTicks(element.getHeight());
            if (left) {
                Rectangle available = new Rectangle(
                        TICK_SIZE / 2, labelY - rectHeight, 
                        x - TICK_SIZE, rectHeight);
                LabelPaintUtil.drawLabel(label, (Graphics2D)g, available,
                        LabelPaintUtil.Position.LEFT, rotate);
            } else {
                Rectangle available = new Rectangle(
                        x + g.getFontMetrics().getHeight(),
                        labelY - rectHeight, 
                        m_vMargin 
                        - g.getFontMetrics().getHeight(), 
                        rectHeight);
                LabelPaintUtil.drawLabel(label, (Graphics2D)g, available,
                    LabelPaintUtil.Position.RIGHT, rotate);
            }
        }
    }

    /**
     * Paints the horizontal coordinates of the matrix elements at the border 
     * of the drawing pane.
     * 
     * @param g graphics
     * @param element the element to provide position and coordinate
     * @param y y position
     * @param top treu if the coordinate is painted at the top.
     */
    protected void paintHorizontalCoordinate(final Graphics g, 
            final ScatterMatrixElement element, final int y, 
            final boolean top) {
        g.drawLine(element.getCorner().x, y, 
                element.getCorner().x + element.getWidth(), y);
        // draw left and right borders
        g.drawLine(element.getCorner().x, y - TICK_SIZE / 2,
                element.getCorner().x, y + TICK_SIZE / 2);
        g.drawLine(element.getCorner().x + element.getWidth(), 
                y - TICK_SIZE / 2,
                element.getCorner().x + element.getWidth(), 
                y + TICK_SIZE / 2);
        CoordinateMapping[] mappings;
        int rectWidth = 0; 
        boolean rotate = false;
        if (element.getXCoordinate().isNominal()) {
            mappings = ((NominalCoordinate)element.getXCoordinate())
                .getReducedTickPositions(element.getWidth());
            rectWidth = element.getWidth() / mappings.length;
            rotate = LabelPaintUtil.rotateLabels(mappings, rectWidth, 
                    g.getFontMetrics());
        } else {
            mappings = element.getXCoordinate()
            .getTickPositions(element.getWidth(), true);
        }
        int i = 0;
        int hOffset = g.getFontMetrics().getHeight();
        for (CoordinateMapping mapping : mappings) {
            int value = (int)mapping.getMappingValue();
            g.drawLine(element.getCorner().x + value,
                    y + (TICK_SIZE / 2), element.getCorner().x + value,
                    y - (TICK_SIZE / 2));
            String label = mapping.getDomainValueAsString();
            int labelX = element.getCorner().x + value 
                - g.getFontMetrics().stringWidth(label) / 4;
            int labelY;
            if (top) {
                if (!(mapping instanceof NominalCoordinateMapping)) {
                    // set the labels up and down so that they dont overlap
                    if (i % 2 == 1) {
                        labelY = y - hOffset;
                    } else {
                        labelY  = y - TICK_SIZE;
                    }
                    g.drawString(label, labelX, labelY);
                } else {
                    int x = element.getCorner().x + value;
                    
                    Rectangle available = new Rectangle(
                            x, TICK_SIZE / 2, rectWidth, y - TICK_SIZE);
                    LabelPaintUtil.drawLabel(label, (Graphics2D)g, available, 
                            LabelPaintUtil.Position.TOP, rotate);
                }
            } else {
                if (!(mapping instanceof NominalCoordinateMapping)) {
                    if (i % 2 == 1) {
                        labelY = y + hOffset + g.getFontMetrics().getHeight();
                    } else {
                        labelY  = y + g.getFontMetrics().getHeight();
                    }
                    g.drawString(label, labelX, labelY);                    
                } else {
                    // rotate the labels
                    int bottomHeight = getHeight() - y - TICK_SIZE / 2; 
                    Rectangle available = new Rectangle(
                            element.getCorner().x + value, y + TICK_SIZE
                            + g.getFontMetrics().getHeight() / 2,
                            rectWidth, 
                            bottomHeight);
                    LabelPaintUtil.drawLabel(label, (Graphics2D)g, available,
                            LabelPaintUtil.Position.BOTTOM, rotate);
                }
            }
            i++;
        }
    }
    
    
}

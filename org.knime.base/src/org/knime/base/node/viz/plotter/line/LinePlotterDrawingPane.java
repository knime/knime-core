/*
 * ------------------------------------------------------------------
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
 *   21.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.line;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

import org.knime.base.node.viz.plotter.scatter.DotInfo;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;
import org.knime.core.data.property.ShapeFactory;

/**
 * Connects the dots in the passed 
 * {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray} with lines. 
 * The number of lines has to be set, in order to determine, where a line starts
 * since the dots are stored sequentially in the 
 * {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray}.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LinePlotterDrawingPane extends ScatterPlotterDrawingPane {
    
    /** To know when a line starts. */
    private int m_nrOfLines;
    
    private boolean m_showDots = true;
    
    private int m_thickness = 1;


    /**
     * Sets the number of line (used for modulo calculation since all dots 
     * are in one 
     * {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray}).
     * 
     * @param nrOfLines the number of lines
     */
    public void setNumberOfLines(final int nrOfLines) {
        m_nrOfLines = nrOfLines;
    }
    
    /**
     * 
     * @param showDots true if dots should be painted, false otherwise
     */
    public void setShowDots(final boolean showDots) {
        m_showDots = showDots;
    }
    
    /**
     * 
     * @param thickness the thickness of the lines
     */
    public void setLineThickness(final int thickness) {
        m_thickness = thickness;
    }
    
    /**
     * Connects the points of one column by a line, which is done by modulo 
     * calculation, the color information is stored in the 
     * {@link org.knime.base.node.viz.plotter.scatter.DotInfo}s.
     * 
     * @see org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane
     * #paintContent(java.awt.Graphics)
     */
    @Override
    public void paintContent(final Graphics g) {
        if (getDotInfoArray() == null
                || getDotInfoArray().getDots().length == 0) {
            return;
        }
        ShapeFactory.Shape shape = ShapeFactory.getShape(
                ShapeFactory.RECTANGLE);
        int dotSize = getDotSize();
        DotInfo[] dotInfo = getDotInfoArray().getDots();
        GeneralPath path = new GeneralPath();
        for (int i = 0; i < dotInfo.length; i++) {
            DotInfo dot1 = dotInfo[i];
            if (m_showDots && dot1.paintDot()) {
                boolean isSelected = getSelectedDots().contains(
                        dotInfo[i].getRowID());
                boolean isHilite = dotInfo[i].isHiLit();
                Color c = dotInfo[i].getColor().getColor();
                int x = dotInfo[i].getXCoord();
                int y = dotInfo[i].getYCoord();
                shape.paint(g, x, y, dotSize, c, isHilite, isSelected, false);
            }
            if (i % m_nrOfLines == 0 && path != null) {
                // start of one line
                path.reset();
                // move to start point
                path.moveTo(dot1.getXCoord(), dot1.getYCoord());
            }
            if (dot1.paintDot()) {
                // if we had a missing value and !interpolate
                if (path == null) {
                    path = new GeneralPath();
                    path.moveTo(dot1.getXCoord(), dot1.getYCoord());
                } else {
                    // add line segment to path
                    path.lineTo(dot1.getXCoord(), dot1.getYCoord());
                }
            } else  if (path != null) {
                // if not paint dot (y = -1) => missing value && !interpolate
                // we have to close the path and continue a new one
                g.setColor(dot1.getColor().getColor());
                ((Graphics2D)g).setStroke(new BasicStroke(m_thickness));
                ((Graphics2D)g).draw(path);
                path = null;
            }
            if (i %  m_nrOfLines == (m_nrOfLines - 1) && path != null) {
                // end of one line -> paint it
                g.setColor(dot1.getColor().getColor());
                ((Graphics2D)g).setStroke(new BasicStroke(m_thickness));
                ((Graphics2D)g).draw(path);
            }
        }
    }
   

}

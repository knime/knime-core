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
 *   21.09.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.line;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.knime.core.data.property.ShapeFactory;
import org.knime.exp.node.view.plotter.scatter.DotInfo;
import org.knime.exp.node.view.plotter.scatter.ScatterPlotterDrawingPane;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LinePlotterDrawingPane extends ScatterPlotterDrawingPane {
    
    
    private int m_nrOfLines;
    
    private boolean m_showDots = true;
    
    private int m_thickness = 1;


    /**
     * Sets the number of line (used for modulo calculation since all dots 
     * are in one array.
     * @param nrOfLines the number of lines.
     */
    public void setNumberOfLines(final int nrOfLines) {
        m_nrOfLines = nrOfLines;
    }
    
    /**
     * 
     * @param showDots true if dots should be painted, false otherwise.
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
     * @see org.knime.exp.node.view.plotter.scatter.ScatterPlotterDrawingPane#
     * paintContent(java.awt.Graphics)
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
        for (int i = 0; i < dotInfo.length - 1; i++) {
            DotInfo dot1 = dotInfo[i];
            DotInfo dot2 = dotInfo[i + 1];
            if (!dot1.paintDot()) {                
                continue;
            }
            if (m_showDots) {
                boolean isSelected = getSelectedDots().contains(
                        dotInfo[i].getRowID());
                boolean isHilite = dotInfo[i].isHiLit();
                Color c = dotInfo[i].getColor().getColor();
                int x = dotInfo[i].getXCoord();
                int y = dotInfo[i].getYCoord();
                shape.paint(g, x, y, dotSize, c, isHilite, isSelected, false);
            }
            if (i > 0 && (i + 1) % m_nrOfLines == 0) {
                // we are at the end of the line
                // so dont paint the line
                continue;
            }
            if (dot1.paintDot() && dot2.paintDot()) {
                // paint line
                paintLine(g, dot1, dot2);
            }
        }
    }
    
    /**
     * Draws the line between two dots.
     * @param g graphics
     * @param dot1 left dot
     * @param dot2 right dot
     */
    protected void paintLine(final Graphics g, final DotInfo dot1, 
            final DotInfo dot2) {
        g.setColor(dot1.getColor().getColor());
        ((Graphics2D)g).setStroke(new BasicStroke(m_thickness));
        g.drawLine(dot1.getXCoord(), dot1.getYCoord(),
                dot2.getXCoord(), dot2.getYCoord());        
    }
   

}

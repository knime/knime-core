/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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

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
 *   11.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.scatter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ToolTipManager;

import org.knime.base.node.viz.plotter.basic.BasicDrawingPane;
import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ShapeFactory;

/**
 * Shows the mapped data points and provides a tooltip for each data point with
 * the domain value and the row ID.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterPlotterDrawingPane extends BasicDrawingPane {
    
    private static final int MAX_TOOLTIP_LENGTH = 10;

    private DotInfoArray m_dots;
    
    // Hash set of selected dots
    private Set<RowKey> m_selDots;
    
//    private Shape m_shape = new Rectangle();
    
    private int m_dotSize;
    
    private boolean m_fade;
    
    /**
     * 
     *
     */
    public ScatterPlotterDrawingPane() {
        super();
        m_dots = new DotInfoArray(0);
        m_selDots = new HashSet<RowKey>();
        ToolTipManager.sharedInstance().registerComponent(this);
    }
    
    /**
     * 
     * @param dotSize the dot size 
     */
    public void setDotSize(final int dotSize) {
        m_dotSize = dotSize;
    }
    
    /**
     * 
     * @return the dot size
     */
    public int getDotSize() {
        return m_dotSize;
    }
    
    /**
     * 
     * @param fade true if unhilited dots should be faded.
     */
    public void setFadeUnhilited(final boolean fade) {
        m_fade = fade;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void paintContent(final Graphics g) {
        super.paintContent(g);
        // paint the data points - if any
        if ((m_dots != null) && (m_dots.length() != 0)) {
            DotInfo[] dotInfo = m_dots.getDots();
            List<DotInfo> hilited 
                = new ArrayList<DotInfo>();
            List<DotInfo> hilitedSelected 
                = new ArrayList<DotInfo>();
            for (int i = 0; i < dotInfo.length; i++) {
                if (!dotInfo[i].paintDot()) {
                    // dont paint dots with negative coord. (These are dots with
                    // missing values or NaN/Infinity values.)
                    continue;
                }
                // paint selection border if selected
                // and the hilited dots
                boolean isSelected = m_selDots.contains(dotInfo[i].getRowID());
                boolean isHilite = dotInfo[i].isHiLit();
                // store the hilited dots to paint them at the end(in the front)
                if (isHilite && !isSelected) {
                    hilited.add(dotInfo[i]);
                } else if (isHilite && isSelected) {
                    hilitedSelected.add(dotInfo[i]);
                } else {
                    paintDot(g, dotInfo[i]);
                }
            }
            for (DotInfo dot : hilited) {
                paintDot(g, dot);
            }
            for (DotInfo dot : hilitedSelected) {
                paintDot(g, dot);
            }
        }
    }
    
    /**
     * Paints the dot with the right shape, color, size and at its correct 
     * position.
     * @param g the graphics object
     * @param dot the dot to paint.
     */
    protected void paintDot(final Graphics g, final DotInfo dot) {
        boolean isSelected = m_selDots.contains(dot.getRowID());
        boolean isHilite = dot.isHiLit();
        ShapeFactory.Shape shape = dot.getShape();
        Color c = dot.getColor().getColor();
        int x = dot.getXCoord();
        int y = dot.getYCoord();
        int size = (int)(m_dotSize * dot.getSize());
        shape.paint(g, x, y, size, c, isHilite, isSelected, m_fade);
    }
    
    /**
     * 
     * @return row keys of selecte dots.
     */
    public Set<RowKey> getSelectedDots() {
        return m_selDots;
    }
    
    /**
     * for extending classes the possibility to set the selected dots.
     * @param selected the rowkey ids of the selected elements.
     */
    protected void setSelectedDots(final Set<RowKey> selected) {
        m_selDots = selected;
    }
    
    /**
     * 
     * @param x1 left corner x
     * @param y1 left corner y
     * @param x2 right corner x
     * @param y2 right corner y
     */
    public void selectElementsIn(final int x1, final int y1, final int x2, 
            final int y2) {
        List<DotInfo> selected = m_dots.getDotsContainedIn(
                x1, y1, x2, y2, 4);
        for (int i = 0; i < selected.size(); i++) {
            m_selDots.add(selected.get(i).getRowID());
        }
    }
    
    /**
     * 
     * clears current selection.
     */
    public void clearSelection() {
        m_selDots.clear();
    }
    
    /**
     * 
     * @param p the clicked point
     */
    public void selectClickedElement(final Point p) {
        selectElementsIn(p.x - 1 , p.y - 1, p.x + 1, p.y + 1);
    }
    
    
    /**
     * Sets the dots to be painted.
     * @param dotInfo the dots to be painted.
     */
    public void setDotInfoArray(final DotInfoArray dotInfo) {
        if (dotInfo != null) {
            m_dots = dotInfo;
        } else {
            m_dots = new DotInfoArray(0);
        }
    }
    
    /**
     * 
     * @return the dots.
     */
    public DotInfoArray getDotInfoArray() {
        return m_dots;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent e) {
        Point p = e.getPoint();
        StringBuffer tooltip = new StringBuffer();
        boolean first = true;
        int points = 0;
        for (DotInfo dot : m_dots.getDots()) {
            int x = dot.getXCoord();
            int y = dot.getYCoord();
            DataCell xDomain = dot.getXDomainValue();
            DataCell yDomain = dot.getYDomainValue();
            double dotSize = (m_dotSize * dot.getSize());
            // assure to have at least one pixel to test
            if (dotSize < 2) {
                dotSize = 2;
            }
            if (x < p.x + (dotSize / 2) && x > p.x - (dotSize / 2)
                    && y < p.y + (dotSize / 2) && y > p.y - (dotSize / 2)) {
                if (first) {
                    if (xDomain != null) {
                        tooltip.append("x: " + xDomain.toString());
                    }
                    if (yDomain != null) {
                        tooltip.append(" y: " + yDomain.toString() + " | ");
                    }
                    tooltip.append(dot.getRowID().toString());
                    first = false;
                } else if (points > MAX_TOOLTIP_LENGTH) {
                    tooltip.append(", ...");
                    break;
                } else {
                    tooltip.append(", " + dot.getRowID().toString());
                }
                points++;
            }
        }
        if (tooltip.toString().length() > 0) {
            return tooltip.toString();
        } else {
            return null;
        }
    }

}

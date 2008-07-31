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
 *   23.02.2007 (Rosaria Silipo): created
 */
package org.knime.timeseries.node.display.barchart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.timeseries.node.display.FinancialDotInfo;
import org.knime.timeseries.node.display.FinancialDotInfoArray;
import org.knime.timeseries.node.display.FinancialShapeFactory;
import org.knime.timeseries.node.display.timeplot.TimePlotterDrawingPane;

/**
 * Shows the mapped data points and provides a tooltip for each data point with
 * the domain value and the row ID.
 * 
 * @author Rosaria Silipo
 */
public class BarChartDrawingPane extends TimePlotterDrawingPane {
    
    private static final int MAX_TOOLTIP_LENGTH = 30;

    private FinancialDotInfoArray m_dots;
    
    // Hash set of selected dots
    private Set<RowKey> m_selDots;
        
    private int m_dotSize = 4;
    
    private boolean m_fade;
    
    /**
     * line 69-72 I can not resize the drawing pane to a bigger size.
     * -- Rosaria
     *
     */
    public BarChartDrawingPane() {
        super();
        
        int m_width = 800;
        int m_height = 400;
        super.setPreferredSize(new Dimension(m_width, m_height));

        m_selDots = new HashSet<RowKey>();
        setToolTipText("");
     }
    
    /**
     * 
     * @param dotSize the dot size 
     */
    @Override
    public void setDotSize(final int dotSize) {
        m_dotSize = dotSize;
    }
    
    /**
     * 
     * @return the dot size
     */
    @Override
    public int getDotSize() {
        return m_dotSize;
    }
    
    /**
     * 
     * @param fade true if unhilited dots should be faded.
     */
    @Override
    public void setFadeUnhilited(final boolean fade) {
        m_fade = fade;
    }
    
    /**
     * @see org.knime.base.node.viz.plotter.AbstractDrawingPane
     * #paintContent(java.awt.Graphics)
     */
    @Override
    public void paintContent(final Graphics g) {
        super.paintContent(g);
        // paint the data points - if any
        if ((m_dots != null) && (m_dots.length() != 0)) {
            FinancialDotInfo[] dotInfo = m_dots.getDots();
            List<FinancialDotInfo> hilited 
                = new ArrayList<FinancialDotInfo>();
            List<FinancialDotInfo> hilitedSelected 
                = new ArrayList<FinancialDotInfo>();
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
            for (FinancialDotInfo dot : hilited) {
                paintDot(g, dot);
            }
            for (FinancialDotInfo dot : hilitedSelected) {
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
    protected void paintDot(final Graphics g, final FinancialDotInfo dot) {
        boolean isSelected = m_selDots.contains(dot.getRowID());
        boolean isHilite = dot.isHiLit();
        FinancialShapeFactory.Shape shape = dot.getShape();
        Color c = dot.getColor().getColor();
        int x = dot.getXCoord();
        
        int openPrice = dot.getOpenPrice();
        int closePrice = dot.getClosePrice();
        int highPrice = dot.getHighPrice();
        int lowPrice = dot.getLowPrice();
 
        shape.paint(g, x, 
                openPrice, closePrice, highPrice, lowPrice, 
                c, isHilite, isSelected, m_fade);
    }
    
    /**
     * 
     * @return row keys of selected dots.
     */
    @Override
    public Set<RowKey> getSelectedDots() {
        return m_selDots;
    }
    
    /**
     * for extending classes the possibility to set the selected dots.
     * @param selected the rowkey ids of the selected elements.
     */
    @Override
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
    @Override
    public void selectElementsIn(final int x1, final int y1, final int x2, 
            final int y2) {
        List<FinancialDotInfo> selected = m_dots.getDotsContainedIn(
                x1, y1, x2, y2, 4);
        for (int i = 0; i < selected.size(); i++) {
            m_selDots.add(selected.get(i).getRowID());
        }
    }
    
    /**
     * 
     * clears current selection.
     */
    @Override
    public void clearSelection() {
        m_selDots.clear();
    }
    
    /**
     * 
     * @param p the clicked point
     */
    @Override
    public void selectClickedElement(final Point p) {
        selectElementsIn(p.x - 1 , p.y - 1, p.x + 1, p.y + 1);
    }
    
    
    /**
     * Sets the dots to be painted.
     * @param dotInfo the dots to be painted.
     */
    public void setFinancialDotInfoArray(final FinancialDotInfoArray dotInfo) {
        m_dots = dotInfo;
    }
    
    /**
     * 
     * @return the dots.
     */
     public FinancialDotInfoArray getFinancialDotInfoArray() {
        return m_dots;
    }
    
    /**
     * 
     * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
     */
    @Override
    public String getToolTipText(final MouseEvent e) {
        Point p = e.getPoint();
        StringBuffer tooltip = new StringBuffer();
        boolean first = true;
        int points = 0;
        for (FinancialDotInfo dot : m_dots.getDots()) {
            int x = dot.getXCoord();
            
//            int openPrice = dot.getOpenPrice();
//            int closePrice = dot.getClosePrice();
            int lowPrice = dot.getLowPrice();
            int highPrice = dot.getHighPrice();
            int financialDotSize = lowPrice - highPrice;
            
            DataCell xDomain = dot.getXDomainValue();
            
            DataCell openPriceDomain = dot.getOpenPriceDomainValue();
            DataCell closePriceDomain = dot.getClosePriceDomainValue();
            DataCell highPriceDomain = dot.getHighPriceDomainValue();
            DataCell lowPriceDomain = dot.getLowPriceDomainValue();
           
            if (x < p.x + (m_dotSize / 2) && x > p.x - (m_dotSize / 2)
                    && highPrice < (p.y + financialDotSize / 2)
                    && lowPrice > (p.y - financialDotSize / 2)) {
                if (first) {
                    if (xDomain != null) {
                        tooltip.append("x: " + xDomain.toString());
                    }
                    if (openPriceDomain != null) {
                        tooltip.append(" open: " + openPriceDomain.toString());
                    }
                    if (closePriceDomain != null) {
                        tooltip.append(" close: " + closePriceDomain.toString());
                    }
                    if (highPriceDomain != null) {
                        tooltip.append(" high: " + highPriceDomain.toString());
                    }
                    if (lowPriceDomain != null) {
                        tooltip.append(" low: " + lowPriceDomain.toString());
                    }
                    tooltip.append(" | "  + dot.getRowID().toString());
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
        return tooltip.toString();
    }

}

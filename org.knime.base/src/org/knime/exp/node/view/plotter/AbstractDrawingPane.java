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
 *   24.08.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/**
 * Handles the selection of elements.
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractDrawingPane extends JPanel {
    
    private Point m_dragStart;
    
    private Point m_dragEnd; 
    
    private boolean m_isMouseDown;
    
    private boolean m_antialiasing = true;

    /**
     * 
     * @param doAntialiasing true if antialiasing should be turned on, 
     * false otherwise.
     */
    public void setAntialiasing(final boolean doAntialiasing) {
        m_antialiasing = doAntialiasing;
    }
    
    /**
     * 
     * @param start the start point of the dragging rectangle.
     */
    public void setDragStart(final Point start) {
        m_dragStart = start;
    }
    
    /**
     * 
     * @return the start poin of the dragging
     */
    public Point getDragStart() {
        return m_dragStart;
    }
    
    /**
     * 
     * @param end the end point of the dragging rectangle. 
     */
    public void setDragEnd(final Point end) {
        m_dragEnd = end;
    }
    
    /**
     * 
     * @param isMouseDown true if the mouse is down, false otherwise.
     */
    public void setMouseDown(final boolean isMouseDown) {
        m_isMouseDown = isMouseDown;
    }
    
    /**
     * 
     * @return true if we are in drag mode, false otherwise.
     */
    public boolean isMouseDown() {
        return m_isMouseDown;
    }
    

    
    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected synchronized void paintComponent(final Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        if (m_antialiasing) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);            
        }
        paintContent(g2);
        // paint selection rectangle if mouse is down
        if (m_isMouseDown) {
            paintSelectionRectangle(g2);
        }
    }
    
    /**
     * Paints the dragged selection rectangle.
     * @param g graphics object.
     */
    protected void paintSelectionRectangle(final Graphics g) {
        g.setXORMode(Color.WHITE);
        Rectangle selRect = getSelectionRectangle();
        if (selRect != null) {
            g.drawRect(selRect.x, selRect.y, selRect.width, selRect.height);
        }
        g.setPaintMode();
    }
    
    /**
     * 
     * @return the current dragged rectangle.
     */
    public Rectangle getSelectionRectangle() {
        if (m_dragStart == null || m_dragEnd == null) {
            return null;
        }
        int x1 = Math.min(m_dragStart.x, m_dragEnd.x);
        int x2 = Math.max(m_dragStart.x, m_dragEnd.x);
        int y1 = Math.min(m_dragStart.y, m_dragEnd.y);
        int y2 = Math.max(m_dragStart.y, m_dragEnd.y);
        int width = x2 - x1;
        int height = y2 - y1;
        return new Rectangle(x1, y1,
                width, height);
    }
    
    /**
     * Paint the actual content of the drawing pane.
     * @param g the graphics object
     */
    public abstract void paintContent(final Graphics g);
    

}

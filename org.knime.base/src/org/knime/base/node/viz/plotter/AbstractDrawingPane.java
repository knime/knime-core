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
 *   24.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.JPanel;

/**
 * Supports the selection of elements (also paints the selection rectangle) and 
 * anti-aliasing, override {@link #paintContent(Graphics)}. For deriving classes
 * it is enough to override the {@link #paintContent(Graphics)} and to somehow
 * store the mapped values locally.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractDrawingPane extends JPanel {
    
    /** The start point of the dragging gesture. */
    private Point m_dragStart;
    /** The end point of the dragging gesture. */
    private Point m_dragEnd; 
    /** Flag whether the mouse is currently pressed. */
    private boolean m_isMouseDown;
    /** Flag for anti-aliasing. */
    private boolean m_antialiasing = false;

    /**
     * Turns anti-aliasing on(true) or off(false). 
     * Use carefully: anti-aliasing slows down performance.
     * 
     * @param doAntialiasing true if antialiasing should be turned on, 
     * false otherwise.
     */
    public void setAntialiasing(final boolean doAntialiasing) {
        m_antialiasing = doAntialiasing;
    }
    
    /**
     * For normal behavior, this method should is by the 
     * {@link org.knime.base.node.viz.plotter.AbstractPlotter} only.
     * 
     * @param start the start point of the dragging rectangle.
     */
    public void setDragStart(final Point start) {
        m_dragStart = start;
    }
    
    /**
     * For normal behavior, this method should is by the 
     * {@link org.knime.base.node.viz.plotter.AbstractPlotter} only.
     *  
     * @return the start point of the dragging
     */
    public Point getDragStart() {
        return m_dragStart;
    }
    
    /**
     * For normal behavior, this method should is by the 
     * {@link org.knime.base.node.viz.plotter.AbstractPlotter} only.
     * 
     * @param end the end point of the dragging rectangle. 
     */
    public void setDragEnd(final Point end) {
        m_dragEnd = end;
    }
    
    /**
     * For normal behavior, this method should is by the 
     * {@link org.knime.base.node.viz.plotter.AbstractPlotter} only.
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
     * Calls the {@link #paintContent(Graphics)} method and then draws the 
     * selection rectangle. Also the flag for anti-aliasing is evaluated here.
     * 
     * {@inheritDoc}
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
        Color backupColor = g2.getColor();
        Stroke backupStroke = g2.getStroke();
        paintContent(g2);
        // restore the original color
        g2.setColor(backupColor);
        // and the original stroke
        g2.setStroke(backupStroke);
        // paint selection rectangle if mouse is down
        if (m_isMouseDown) {
            paintSelectionRectangle(g2);
        }
    }
    
    /**
     * Paints the dragged selection rectangle.
     * 
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
     * For normal behavior, this method should is by the 
     * {@link org.knime.base.node.viz.plotter.AbstractPlotter} only.
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
     * Paints the actual content of the drawing pane.
     * 
     * @param g the graphics object
     */
    public abstract void paintContent(final Graphics g);
    

}

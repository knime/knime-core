/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

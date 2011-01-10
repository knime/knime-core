/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   17.03.2006 (sieb): created
 */
package org.knime.base.node.viz.plotter2D;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.JPanel;

import org.knime.core.node.NodeLogger;


/**
 * This class provides an abstract drawing pane to draw 2D plots. The plots
 * should have domain ranges (numeric or nominal). The abstract plot provides
 * default listeners for selection handling.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class AbstractDrawingPane extends JPanel {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AbstractDrawingPane.class);

    // Selection variables:
    private int m_mouseDownX;

    private int m_mouseDownY;

    private int m_dragX;

    private int m_dragY;

    private int m_cursorX;

    private int m_cursorY;
    
   

    /**
     * Keeps the information whether the left or right mouse button triggers the
     * selection rectangle. Left mouse is for selection hiliting, the right
     * button is for zoom area selection.
     */
    private boolean m_rightMouseDown;

    private String m_errorText;
    
    /**
     * Creates a new empty drawing pane.
     */
    public AbstractDrawingPane() {
        super();
        m_errorText = null;
        m_mouseDownX = -1;
        m_mouseDownY = -1;
//        setToolTipText("");
    }

    /**
     * If coordinates are set, a cursor is painted at the specified location. If
     * a coordinate value less than zero is set, it will be changed to zero.
     * 
     * @param x the X coord. of the cursor to paint
     * @param y the Y coord. of the cursor to paint
     */
    public void setCursorCoord(final int x, final int y) {
        if (x < 0) {
            m_cursorX = 0;
        } else {
            m_cursorX = x;
        }
        if (y < 0) {
            m_cursorY = 0;
        } else {
            m_cursorY = y;
        }
    }

    /**
     * clears the cursor coordinates - no cursor will be painted after that.
     */
    public void clearCursorCoord() {
        m_cursorX = -1;
        m_cursorY = -1;
    }

    /**
     * @return true if cursor coordinates are set to valid values
     */
    public boolean cursorCoordSet() {
        return (m_cursorX >= 0) && (m_cursorY >= 0);
    }

    /**
     * sets new mouse down coordinates for the selection rectangle.
     * 
     * @param x The new mouse down X coordinate
     * @param y The new mouse down Y coordinate
     */
    public void setMouseDown(final int x, final int y) {
        m_mouseDownX = x;
        m_mouseDownY = y;
    }

    /**
     * @return true if the mouse down coordinates were set.
     */
    public boolean mouseDownSet() {
        return (m_mouseDownX != -1) && (m_mouseDownY != -1);
    }

    /**
     * sets new mouse dragging coordinates for the selection rectangle.
     * 
     * @param x The new mouse dragging X coordinate
     * @param y The new mouse dragging Y coordinate
     */
    public void setDragCoord(final int x, final int y) {
        m_dragX = x;
        m_dragY = y;
    }

    /**
     * @return true if mouse dragging coordinates were set (to a value other
     *         than -1), false otherwise
     */
    public boolean dragCoordSet() {
        return ((m_dragX != -1) && (m_dragY != -1));
    }

    /**
     * Clears the selection rectangle (i.e. is resets the mouse down and dragged
     * coordinates
     */
    public void clearDragTangle() {
        m_mouseDownX = -1;
        m_mouseDownY = -1;
        m_dragX = -1;
        m_dragY = -1;
    }
    

    /**
     * This method has to be implemented by all sub classes. It draws the actual
     * content of this plot.
     * 
     * @param g the graphics context to draw in
     */
    protected abstract void paintPlotDrawingPane(Graphics g);

    /**
     * Implemented by the subclass to select all elements in the selection
     * rectangle.
     * 
     * @param mouseDownX the X position where the mouse was pressed
     * @param mouseDownY the Y position where the mouse was pressed
     * @param mouseUpX the X position where the mouse was released
     * @param mouseUpY the Y position where the mouse was released
     */
    protected abstract void selectElementsInDragTangle(int mouseDownX,
            int mouseDownY, int mouseUpX, int mouseUpY);

    /**
     * Changes the selection state of all elements at the provided coordinates.
     * 
     * @param x The x coord.
     * @param y The y coord.
     */
    protected abstract void toggleSelectionAt(final int x, final int y);

    /**
     * @return returns the number of selected elements
     */
    public abstract int getNumberSelectedElements();

    /**
     * The <code>paintComponent</code> method of the abstract drawing pane
     * invokes the abstract <code>paintPlotDrawingPane</code> method that has
     * to be implemented by all subclasses.
     * 
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected final void paintComponent(final Graphics g) {
        // invoke the parents paintComponent method
        super.paintComponent(g);
        // invoke the abstract paint method implemented by the subclasses
        // this method draws the individual content
        paintPlotDrawingPane(g);

        // now paint the cross hair (if wanted) and other default functionality

        // first check the error text
        if (m_errorText != null) {

            LOGGER.warn("Error text set in plot: " + m_errorText);
            g.drawString(m_errorText, 20, 40);
        }
        
        paintCursor(g);
    }
    
    private synchronized void paintCursor(final Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.0f));
        // Now check if we have a new selection rectangle to paint
        if (dragCoordSet()) {
            int x, y;
            int w, h;
            if (m_mouseDownX < m_dragX) {
                x = m_mouseDownX;
                w = m_dragX - m_mouseDownX;
                // it's actually the width+1
            } else {
                // which is fine for drawRect!!
                x = m_dragX;
                w = m_mouseDownX - m_dragX;
            }
            if (m_mouseDownY < m_dragY) {
                y = m_mouseDownY;
                h = m_dragY - m_mouseDownY;
                // it's actually the heigth+1
            } else {
                // which is fine for drawRect!!
                y = m_dragY;
                h = m_mouseDownY - m_dragY;
            }

            // set the color depending on the pressed mouse button
            // and therefore the intended operation
            if (m_rightMouseDown) {
                g2.setColor(Color.lightGray);
                g2.setXORMode(Color.blue);
            } else {
                g2.setColor(Color.lightGray);
                g2.setXORMode(Color.darkGray);
            }
            g2.drawRect(x, y, w, h);
            g2.setPaintMode();
        }
        // Now paint a nice cursor - that is a crosshair for now
        if (cursorCoordSet()) {
            g2.setColor(Color.darkGray);
            g2.setXORMode(Color.black);
            // draw the new ones
            g2.drawLine(0, m_cursorY, getWidth(), m_cursorY);
            g2.drawLine(m_cursorX, 0, m_cursorX, getHeight());
            g2.setPaintMode();
        }
        g2.setStroke(oldStroke);
    }

    /**
     * Clears a possible selection of this drawing pane which has been made by
     * the selection drag tangle.
     */
    protected abstract void clearSelection();

    /**
     * the passed string - if not null - will be displayed instead of any data.
     * This can be used to indicate an error in the data. If set null the
     * regular display reappears.
     * 
     * @param errMsg the error message to show. If null the dots will be
     *            displayed.
     */
    public void setErrorText(final String errMsg) {
        m_errorText = errMsg;
    }

    /**
     * Sets wether the right or left mouse button is pressed.
     * 
     * @param rightMouseDown true if the right button is supposed to be pressed
     */
    void setRightMouseDown(final boolean rightMouseDown) {
        m_rightMouseDown = rightMouseDown;
    }

    /**
     * @return the x position of the last mouse down event
     */
    int getMouseDownX() {
        return m_mouseDownX;
    }

    /**
     * @return the y position of the last mouse down event
     */
    int getMouseDownY() {
        return m_mouseDownY;
    }

    /**
     * @return true if the last mouse down event was triggered by the right
     *         mouse button. (Responsible for popup menue and zoom)
     */
    boolean isRightMouseDown() {
        return m_rightMouseDown;
    }

    /**
     * @return the x position of the current drag (mouse pointer)
     */
    int getDragX() {
        return m_dragX;
    }

    /**
     * @return the y position of the current drag (mouse pointer)
     */
    int getDragY() {
        return m_dragY;
    }
} // end of class AbstractDrawingPane


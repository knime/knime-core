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
 *   13.09.2006 (Fabian Dill): created
 */
package org.knime.core.data.property;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ShapeSelectionListRenderer extends DefaultListCellRenderer { 
    

    /**
     * 
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(
     * javax.swing.JList, java.lang.Object, int, boolean, boolean)
     */
    @Override
    public Component getListCellRendererComponent(final JList list, 
            final Object value, final int index, final boolean isSelected, 
            final boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
        JPanel cell = new ShapeListCellComponent(ShapeFactory.getShape(
                value.toString()));
        cell.setPreferredSize(c.getPreferredSize());
        cell.setBackground(c.getBackground());
        cell.setForeground(c.getForeground());
        return cell;
    }
    
    
    /**
     * Draws the passed shape.
     * 
     * @author Fabian Dill, University of Konstanz
     */
    private class ShapeListCellComponent extends JPanel {
        
        private ShapeFactory.Shape m_shape;
        
        private static final int BORDER = 5;
        
        /**
         * 
         * @param s the shape to be drawn.
         */
        public ShapeListCellComponent(final ShapeFactory.Shape s) {
            m_shape = s;
        }
        
        /**
         * 
         * @see java.awt.Component#paint(java.awt.Graphics)
         */
        @Override
        public void paint(final Graphics g) {
            super.paint(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            m_shape.setPosition(new Point(
                    getPreferredSize().height / 2, 
                    getPreferredSize().height / 2));
            m_shape.setDimension(new Dimension(
                    getPreferredSize().height - BORDER, 
                    getPreferredSize().height - BORDER));
            m_shape.paintShape(g, false, false);
            g.drawString(m_shape.toString(), getPreferredSize().height + 2, 
                    (getPreferredSize().height / 2) + BORDER);
        }
    }

}

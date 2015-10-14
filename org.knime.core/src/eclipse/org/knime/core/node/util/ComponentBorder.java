package org.knime.core.node.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

/**
 *
 * A border which includes a arbitrary component.
 *
 * MySwing: Advanced Swing Utilites Copyright (C) 2005 Santhosh Kumar T
 * <p>
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Slightly modfied by Marcel Hanser
 *
 * @author Marcel Hanser
 */

final class ComponentBorder implements Border, MouseListener, MouseMotionListener, SwingConstants {
    private int m_offset = 5;

    private Component m_comp;

    private JComponent m_container;

    private Rectangle m_rect;

    private Border m_border;

    private boolean m_mouseEntered = false;

    /**
     * Creates a new border titled with the given component.
     *
     * @param comp the component to be shown in the border.
     * @param container the container for the border
     * @param border the border
     */
    public ComponentBorder(final JComponent comp, final JComponent container, final Border border) {
        this.m_comp = comp;
        this.m_container = container;
        this.m_border = border;
        container.addMouseListener(this);
        container.addMouseMotionListener(this);
        container.repaint();
        container.revalidate();
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }

    @Override
    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width,
        final int height) {
        m_container.revalidate();
        m_container.validate();

        Insets borderInsets = m_border.getBorderInsets(c);
        Insets insets = getBorderInsets(c);
        int temp = (insets.top - borderInsets.top) / 2;
        Dimension size = m_comp.getPreferredSize();
        int huha = size.height;
        m_border.paintBorder(c, g, x, y + temp, width, (height - temp) - huha);
        m_rect = new Rectangle(m_offset, 0, size.width, size.height);
        SwingUtilities.paintComponent(g, m_comp, (Container)c, m_rect);
    }

    @Override
    public Insets getBorderInsets(final Component c) {
        Dimension size = m_comp.getPreferredSize();
        Insets insets = m_border.getBorderInsets(c);
        insets.top = Math.max(insets.top, size.height);
        return insets;
    }

    private void dispatchEvent(final MouseEvent me) {
        if (m_rect != null && m_rect.contains(me.getX(), me.getY())) {
            dispatchEvent(me, me.getID());
        }
    }

    private void dispatchEvent(final MouseEvent me, final int id) {
        Point pt = me.getPoint();
        pt.translate(-m_offset, 0);

        m_comp.setSize(m_rect.width, m_rect.height);
        m_comp.dispatchEvent(new MouseEvent(m_comp, id, me.getWhen(), me.getModifiers(), pt.x, pt.y,
            me.getClickCount(), me.isPopupTrigger(), me.getButton()));
        if (!m_comp.isValid()) {
            m_container.repaint();
        }
    }

    @Override
    public void mouseClicked(final MouseEvent me) {
        dispatchEvent(me);
    }

    @Override
    public void mouseEntered(final MouseEvent me) {
    }

    @Override
    public void mouseExited(final MouseEvent me) {
        if (m_mouseEntered) {
            m_mouseEntered = false;
            dispatchEvent(me, MouseEvent.MOUSE_EXITED);
        }
    }

    @Override
    public void mousePressed(final MouseEvent me) {
        dispatchEvent(me);
    }

    @Override
    public void mouseReleased(final MouseEvent me) {
        dispatchEvent(me);
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
    }

    @Override
    public void mouseMoved(final MouseEvent me) {
        if (m_rect == null) {
            return;
        }

        if (!m_mouseEntered && m_rect.contains(me.getX(), me.getY())) {
            m_mouseEntered = true;
            dispatchEvent(me, MouseEvent.MOUSE_ENTERED);
        } else if (m_mouseEntered) {
            if (!m_rect.contains(me.getX(), me.getY())) {
                m_mouseEntered = false;
                dispatchEvent(me, MouseEvent.MOUSE_EXITED);
            } else {
                dispatchEvent(me, MouseEvent.MOUSE_MOVED);
            }
        }
    }
}
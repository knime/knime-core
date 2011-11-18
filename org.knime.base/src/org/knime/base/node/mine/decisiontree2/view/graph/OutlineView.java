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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   28.07.2010 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.view.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;

import org.knime.core.data.property.ColorAttr;

/**
 * This component displays a typically smaller representation of a
 * {@link HierarchicalGraphView}. It provides a way to change the visible
 * area of a {@link HierarchicalGraphView}.
 *
 * @author Heiko Hofer
 * @param <K> The type of a {@link NodeWidget} the in graph
 */
public final class OutlineView<K> extends JComponent
        implements ComponentListener, GraphListener {
    private static final long serialVersionUID = 9122100990183873158L;
    private final HierarchicalGraphView<K> m_graph;
    private Point m_pressedAt;
    private Rectangle m_visRectWhenPressed;
    private Rectangle m_translucentRect;
    private boolean m_translucentRectVisible;

    /**
     * Creates a new instance.
     *
     * @param graph the hierarchical graph view
     */
    OutlineView(final HierarchicalGraphView<K> graph) {
        m_graph = graph;
        setDoubleBuffered(true);
        setPreferredSize(new Dimension(128, 128));

        MyMouseListener mouseListener = new MyMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNotify() {
        super.addNotify();
        m_graph.getView().addComponentListener(this);
        m_graph.addGraphListener(this);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        m_graph.getView().removeComponentListener(this);
        m_graph.removeGraphListener(this);
        super.removeNotify();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(40, 30);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.paint(g);

        Dimension graphSize = m_graph.getView().getPreferredSize();
        Rectangle b = new Rectangle(
                getInsets().left, getInsets().top,
                getSize().width - getInsets().left - getInsets().right - 1,
                getSize().height - getInsets().top - getInsets().bottom - 1);
        Color prevColor = g2.getColor();
        g2.setColor(ColorAttr.BACKGROUND);
        g2.fill(b);
        g2.setColor(prevColor);

        double sx = graphSize.width > 0
            ? (double) (b.width - 1) / graphSize.width : 0.0;
        double sy = graphSize.width > 0
            ? (double) (b.height - 1) / graphSize.height : 0.0;
        double scale = Math.min(sx, sy);
        scale = Math.min(0.33, scale);

        AffineTransform previousTransform = g2.getTransform();
        g2.scale(scale, scale);
        m_graph.removeGraphListener(this);
        m_graph.paint(this, g2, 0, 0, graphSize.width, graphSize.height);
        m_graph.addGraphListener(this);
        g2.setTransform(previousTransform);

        Rectangle graphVisRect = m_graph.getView().getVisibleRect();
        Rectangle graphRectangle =
            new Rectangle(0, 0, graphSize.width, graphSize.height);
        m_translucentRectVisible = !graphVisRect.contains(graphRectangle);
        if (m_translucentRectVisible) {
            graphVisRect = graphVisRect.intersection(graphRectangle);
            m_translucentRect = new Rectangle(
                (int) (graphVisRect.x * scale),
                (int) (graphVisRect.y * scale),
                (int) (graphVisRect.width * scale) + 1,
                (int) (graphVisRect.height * scale) + 1
            );
            g2.setColor(new Color(254, 245, 228, 128));
            g2.fill(m_translucentRect);
            g2.setColor(new Color(250, 209, 132));
            g2.drawRect(m_translucentRect.x, m_translucentRect.y,
                    m_translucentRect.width, m_translucentRect.height);
        }
    }

    private void moveVisibleRect(final Point p) {
        JComponent component = m_graph.getView();

        Dimension graphSize = component.getPreferredSize();
        Rectangle b = new Rectangle(
                getInsets().left, getInsets().top,
                getSize().width - getInsets().left - getInsets().right - 1,
                getSize().height - getInsets().top - getInsets().bottom - 1);

        double sx = graphSize.width > 0
            ? (double) (b.width - 1) / graphSize.width : 0.0;
        double sy = graphSize.width > 0
            ? (double) (b.height - 1) / graphSize.height : 0.0;
        double scale = Math.min(sx, sy);
        scale = Math.min(0.33, scale);

        int deltax = (int) ((p.x - m_pressedAt.x) / scale);
        int deltay = (int) ((p.y - m_pressedAt.y) / scale);

        Rectangle visibleRect = component.getVisibleRect();
        visibleRect.x = m_visRectWhenPressed.x + deltax;
        visibleRect.y = m_visRectWhenPressed.y + deltay;
        component.scrollRectToVisible(visibleRect);
    }


    private class MyMouseListener extends MouseAdapter {

        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            if (m_translucentRectVisible) {
                if (!m_translucentRect.contains(e.getPoint())) {
                    JComponent component = m_graph.getView();
                    m_visRectWhenPressed = component.getVisibleRect();
                    m_pressedAt = new Point(
                            m_translucentRect.x + m_translucentRect.width / 2,
                            m_translucentRect.y + m_translucentRect.height / 2);
                    moveVisibleRect(e.getPoint());
                }
                m_pressedAt = e.getPoint();
                m_visRectWhenPressed = m_graph.getView().getVisibleRect();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseDragged(final MouseEvent e) {
            if (m_translucentRectVisible) {
               moveVisibleRect(e.getPoint());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void componentHidden(final ComponentEvent e) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void componentMoved(final ComponentEvent e) {
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void componentResized(final ComponentEvent e) {
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void componentShown(final ComponentEvent e) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void graphRepaint() {
       repaint();
    }

}

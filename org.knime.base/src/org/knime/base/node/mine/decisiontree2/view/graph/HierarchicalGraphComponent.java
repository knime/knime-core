/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   22.07.2010 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.view.graph;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.knime.core.data.property.ColorAttr;

/**
 * This is a {@link JComponent} which displays a {@link HierarchicalGraphView}.
 *
 * @author Heiko Hofer
 * @param <K> The type of the graphs user objects
 */
final class HierarchicalGraphComponent<K> extends JPanel implements Scrollable {
    private static final long serialVersionUID = -8435691901973256733L;
    private HierarchicalGraphView<K> m_graph;

    /**
     * @param graph the graph view
     */
    public HierarchicalGraphComponent(final HierarchicalGraphView<K> graph) {
        m_graph = graph;
        setLayout(null);
        setBackground(ColorAttr.BACKGROUND);
        addMouseListener(new MyMouseListener());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        Toolkit tk = Toolkit.getDefaultToolkit();
        @SuppressWarnings("rawtypes")
        Map desktopHints =
            (Map)(tk.getDesktopProperty("awt.font.desktophints"));
        if (desktopHints != null) {
            g2.addRenderingHints(desktopHints);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        super.paint(g2);
        m_graph.paint(this, g2, 0, 0, this.getWidth(), this.getHeight());
    }

    /**
     * Just forward mouse events to the graph.
     *
     * @author Heiko Hofer
     */
    public class MyMouseListener extends MouseAdapter {
        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            m_graph.mousePressed(e);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            m_graph.mouseClicked(e);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseReleased(final MouseEvent e) {
            m_graph.mouseReleased(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(300, 400);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getScrollableUnitIncrement(final Rectangle visibleRect,
            final int orientation, final int direction) {
        return getScrollableBlockIncrement(visibleRect,
                orientation, direction) / 4;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getScrollableBlockIncrement(final Rectangle visibleRect,
            final int orientation, final int direction) {
        boolean vertical = SwingConstants.VERTICAL == orientation;
        if (vertical) {
            return visibleRect.height / 3;
        } else {
            return visibleRect.width / 3;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getPreferredSize().width < getParent().getWidth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getPreferredSize().height < getParent().getHeight();
    }
}

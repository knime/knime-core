/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseListener;

/**
 * The visual representation of a node in an {@link  HierarchicalGraphView}.
 *
 * @author Heiko Hofer
 * @param <K> The type of the user object.
 */
public abstract class NodeWidget<K> implements MouseListener {
    private Dimension m_size;
    private HierarchicalGraphView<K> m_graph;
    private K m_object;
    private float m_scale;

    /**
     * Creates a new instance.
     *
     * @param graph the graph where this {@link NodeWidget} is element of
     * @param object the user object
     */
    public NodeWidget(final HierarchicalGraphView<K> graph, final K object) {
        m_graph = graph;
        m_object = object;
        m_size = new Dimension(0, 0);
        m_scale = 1.0f;
    }

    /**
     * Get the preferred size.
     * @return the preferred size
     */
    public abstract Dimension getPreferredSize();

    /**
     * Paint this {@link NodeWidget} using the given {@link Graphics2D}.
     * @param c the component to paint on
     * @param g the graphics
     * @param x the x coordinate in the component
     * @param y the y coordinate in the component
     * @param width the width of the widget
     * @param height the height of the widget
     */
    protected abstract void paint(Component c, Graphics2D g, int x, int y,
            int width, int height);

    /**
     * Paint this {@link NodeWidget} using the given {@link Graphics2D}.
     * @param c the component to paint on
     * @param g the graphics
     * @param r position and size on the component
     */
    public void paint(final Component c, final Graphics2D g,
            final Rectangle r) {
        paint(c, g, r.x, r.y, r.width, r.height);
    }

    /**
     * Get the label that should be painted on the connector above the
     * {@link NodeWidget}.
     * @return the label painted above
     */
    public abstract String getConnectorLabelAbove();

    /**
     * Get the label that should be painted on the connector below the
     * {@link NodeWidget}.
     * @return the label painted below
     */
    public abstract String getConnectorLabelBelow();

    /**
     * Get the current size.
     * @return the size
     */
    final Dimension getSize() {
        return m_size;
    }

    /**
     * Set the size of this {@link NodeWidget}.
     * @param size the size to set
     */
    final void setSize(final Dimension size) {
        m_size = size;
    }

    /**
     * Set the scale factor.
     *
     * @return the scale factor
     */
    public float getScaleFactor() {
        return m_scale;
    }

    /**
     * Get the scale factor.
     *
     * @param scale the scale factor to set
     */
    public void setScaleFactor(final float scale) {
        m_scale = scale;
    }

    /**
     * Get the graph where this {@link NodeWidget} is element of.
     * @return the graph
     */
    protected final HierarchicalGraphView<K> getGraphView() {
        return m_graph;
    }

    /**
     * Get the user object of this {@link NodeWidget}.
     * @return the user object
     */
    protected final  K getUserObject() {
        return m_object;
    }
}

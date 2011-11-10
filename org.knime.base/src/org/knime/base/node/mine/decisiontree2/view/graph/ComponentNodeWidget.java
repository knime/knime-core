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
 *   22.07.2010 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.view.graph;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A {@link NodeWidget} consisting of single {@link JComponent}.
 *
 * @author Heiko Hofer
 * @param <K> The type
 */
public abstract class ComponentNodeWidget<K> extends NodeWidget<K> {
    private JComponent m_component;
    /**
     * Creates a new instance.
     *
     * @param graph the graph where this {@link NodeWidget} is element of
     * @param object the user object
     */
    public ComponentNodeWidget(final HierarchicalGraphView<K> graph,
            final K object) {
        super(graph, object);
    }

    /**
     * Creates the component.
     * @return a component which this {@link NodeWidget} will display
     */
    protected abstract JComponent createComponent();

    /**
     * Returns the component which this {@link NodeWidget} will display.
     * @return a component which this {@link NodeWidget} will display
     */
    private JComponent getComponent() {
        if (m_component == null) {
            JPanel p;
            m_component = createComponent();
            if (m_component instanceof JPanel) {
                p = (JPanel)m_component;
            } else {
                p = new JPanel(new BorderLayout());
                p.add(m_component, BorderLayout.CENTER);
            }
            Dimension d = getPreferredSize();
            p.setBounds(0, 0, d.width, d.height);
            // Space for selection and hiliting borders
            p.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            adjustComponentProperties(p);
        }
        return m_component;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setScaleFactor(final float scale) {
        if (scale != getScaleFactor()) {
            // recreate component
            m_component = null;
            super.setScaleFactor(scale);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Dimension getPreferredSize() {
        return getComponent().getPreferredSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paint(final Component c, final Graphics2D g,
            final int x, final int y, final int width, final int height) {
        JComponent comp = getComponent();
        comp.setFont(g.getFont());
        comp.setBounds(0, 0, width, height);
        comp.doLayout();
        adjustComponentProperties(comp);

        AffineTransform previousTransform = g.getTransform();
        AffineTransform trans = g.getTransform();
        trans.concatenate(AffineTransform.getTranslateInstance(
                x, y));
        g.setTransform(trans);
        comp.paint(g);
        g.setTransform(previousTransform);
    }

    private void adjustComponentProperties(final JComponent p) {
        if (p instanceof JPanel) {
            p.doLayout();
            /* see bug:
             *http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6668436
             */
            ((JPanel)p).setDoubleBuffered(false);
        }
        for (Component cc : p.getComponents()) {
            if (cc instanceof JComponent) {
                adjustComponentProperties((JComponent)cc);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(final MouseEvent e) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(final MouseEvent e) {
        sendToTarget(getComponent(), 0, 0, e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(final MouseEvent e) {
       // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(final MouseEvent e) {
       // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(final MouseEvent e) {
       // do nothing
    }

    /**
     * Find target of the event. This method is called recursively.
     * @param component the possible target
     * @return true when target is found an recursive search should be stopped.
     */
    private boolean sendToTarget(final Container component,
            final int x, final int y, final MouseEvent e) {
        if (!component.isVisible()) {
            return false;
        }
        int xx = x + component.getX();
        int yy = y + component.getY();
        for (Component c : component.getComponents()) {
            if (sendToTarget((Container)c, xx, yy, e)) {
                return true;
            }
        }
        Point p = new Point(e.getPoint());
        p.translate(-x, -y);
        boolean isIn = component.getBounds().contains(p);
        if (isIn) {
            // translate mouse event
            MouseEvent ee = new MouseEvent(
                    e.getComponent(),
                    e.getID(),
                    e.getWhen(),
                    e.getModifiers(),
                    e.getX() - x,
                    e.getY() - y,
                    e.getXOnScreen(),
                    e.getYOnScreen(),
                    e.getClickCount(),
                    e.isPopupTrigger(),
                    e.getButton());
            for (MouseListener l : component.getMouseListeners()) {
                l.mousePressed(ee);
            }
            if (component instanceof JComponent) {
                JComponent jcomp = (JComponent)component;
                if (jcomp.getBorder() instanceof MouseListener) {
                    MouseListener l = (MouseListener)jcomp.getBorder();
                    l.mousePressed(ee);
                }
            }
            return true;
        } else {
            return false;
        }
    }


}

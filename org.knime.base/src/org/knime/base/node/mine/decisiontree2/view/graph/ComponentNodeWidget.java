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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.knime.core.data.property.ColorAttr;

/**
 * A {@link NodeWidget} consisting of single {@link JComponent}.
 *
 * @author Heiko Hofer
 * @param <K> The type
 */
public abstract class ComponentNodeWidget<K> extends NodeWidget<K> {
//    private Image m_image;

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
    protected abstract JComponent getComponent();

    /**
     * {@inheritDoc}
     */
    @Override
    protected final Dimension getPreferredSize() {
        Dimension d = getComponent().getPreferredSize();
        return new Dimension(d.width + 6, d.height + 6);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void paint(final Graphics2D g) {
//        if (null == m_image) {
            JPanel p;
            Component comp = getComponent();
            if (comp instanceof JPanel) {
                p = (JPanel)comp;
            } else {
                p = new JPanel(new BorderLayout());
                p.add(comp, BorderLayout.CENTER);
            }
            Dimension d = getSize();
            p.setBounds(0, 0, d.width, d.height);
            // Space for selection and hiliting borders
            p.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));


            //p.setBackground(Color.TRANSLUCENT);
            p.doLayout();
            /* see bug:
             * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6668436
             */
            p.setDoubleBuffered(false);

            p.setOpaque(false);
            for (Component c : p.getComponents()) {
                ((JComponent)c).setOpaque(false);
                if (c instanceof JPanel) {
                    c.setBackground(ColorAttr.BACKGROUND);
                    c.doLayout();
                    /* see bug:
                     *http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6668436
                     */
                    ((JPanel)c).setDoubleBuffered(false);
                }
            }
            p.paint(g);
//
//
//
//            GraphicsEnvironment ge = GraphicsEnvironment
//                  .getLocalGraphicsEnvironment();
//            GraphicsDevice gs = ge.getDefaultScreenDevice();
//            GraphicsConfiguration gc = gs.getDefaultConfiguration();
//            // Create an image that does not support transparency
//            BufferedImage image = gc.createCompatibleImage(p.getWidth(),
//                    p.getHeight(), Transparency.TRANSLUCENT);
//            // Alternatively:
//            //BufferedImage image = new BufferedImage(d.width, d.height,
//                    BufferedImage.TYPE_INT_ARGB);
//
//            Graphics g2 = image.createGraphics();
//
//            p.paint(g2);
//            g2.dispose();
//            m_image = image;
//        }
//        g.drawImage(m_image, null, null);
    }


}

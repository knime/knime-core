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
 *   25.11.2010 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;


/**
 * A JPanel which is a container for a single component. The Panel has a border
 * which a allows to collapse it by mouse click which means to set its
 * component invisible and update the border to the collapsed state.
 *
 * @author Heiko Hofer
 */
public class CollapsiblePanel extends JPanel {
    private static final long serialVersionUID = 5514462816375715971L;
    private CollapsibleBorder m_border;
    private JPanel m_content;

    /**
     * @param title The title which will be displayed in the top left corner
     * of the border.
     * @param content The content of this {@link JPanel}
     * @param scale a scale factor
     */
    public CollapsiblePanel(final String title, final JPanel content,
            final float scale) {
        super(new GridBagLayout());
        m_border = new CollapsibleBorder(this, title, false, scale);
        m_content = content;

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        content.setVisible(true);
        add(content, c);
        setBorder(m_border);
    }

    /**
     * Collapse are expand the CollapsiblePanel.
     * @param collapsed true when panel should be collapsed.
     */
    public void setCollapsed(final boolean collapsed) {
        m_content.setVisible(!collapsed);
        m_border.setCollapsed(collapsed);
        validate();
    }

    /**
     * @return true when this panel is collapsed which means that its content
     * is not visible.
     */
    public boolean isCollapsed() {
        return m_border.isCollapsed();
    }

    private static class CollapsibleBorder implements Border, MouseListener {
        private static ImageIcon collapsedIcon;
        private static ImageIcon expandedIcon;

        private CollapsiblePanel m_container;
        private boolean m_collapsed;
        // The bounds ot the JLabel in the top left corner
        private Rectangle m_rect;
        // The x-offset for the JLabel in the top left corner
        private int m_offset;
        // The label in the top left corner
        private JLabel m_label;
        // The border color
        private Color m_color;
        // scaled version of collapsedIcon
        private ImageIcon m_collapsedIcon;
        // scaled version of expandedIcon
        private ImageIcon m_expandedIcon;


        /**
         * @param container The Panel that this border surrounds
         * @param text The text displayed in the top left corner
         * @param collapsed Whether the initial state is collapsed or not
         * @param scale a scale factor
         */
        public CollapsibleBorder(final CollapsiblePanel container,
                final String text, final boolean collapsed, final float scale) {
            m_collapsed = collapsed;
            m_offset = 3;
            m_container = container;
            Package pack = CollapsiblePanel.class.getPackage();
            String iconBase = pack.getName().replace(".", "/") + "/";
            if (collapsedIcon == null) {
                URL collapsedUrl =
                    this.getClass().getClassLoader().getResource(iconBase
                            + "collapsed.png");
                if (collapsedUrl == null) {
                    collapsedIcon = new ImageIcon();
                } else {
                    collapsedIcon = new ImageIcon(collapsedUrl);
                }
            }
            m_collapsedIcon = scaled(collapsedIcon, scale);
            if (expandedIcon == null) {
                URL expandedUrl =
                    this.getClass().getClassLoader().getResource(iconBase
                            + "expanded.png");
                if (expandedUrl == null) {
                    expandedIcon = new ImageIcon();
                } else {
                    expandedIcon = new ImageIcon(expandedUrl);
                }
            }
            m_expandedIcon = scaled(expandedIcon, scale);

            m_label = new JLabel(text, m_expandedIcon,
                    SwingConstants.LEFT);
            m_label.setFont(m_label.getFont().deriveFont(
                    m_label.getFont().getSize() * scale));
            m_color = Color.lightGray;
        }

        /* Get a scaled version of the give icon */
        private ImageIcon scaled(final ImageIcon icon, final float scale) {
            Image img = icon.getImage();
            Image newimg = img.getScaledInstance(
                    Math.round(icon.getIconWidth() * scale),
                    Math.round(icon.getIconHeight() * scale),
                    java.awt.Image.SCALE_SMOOTH);
            return new ImageIcon(newimg);
        }

        /**
         * @param collapsed true when border should be collapsed
         */
        public void setCollapsed(final boolean collapsed) {
            m_collapsed = collapsed;
            if (collapsed) {
                m_label.setIcon(m_collapsedIcon);
            } else {
                m_label.setIcon(m_expandedIcon);
            }
        }

        /**
         * @return true when border is collapsed
         */
        public boolean isCollapsed() {
            return m_collapsed;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintBorder(final Component c, final Graphics g,
                final int x, final int y,
                final int width, final int height) {
            Insets insets = getBorderInsets(c);
            Color oldColor = g.getColor();
            g.translate(x, y);

            g.setColor(m_color);
            Dimension size = m_label.getPreferredSize();
            m_rect = new Rectangle(m_offset, 0, size.width, size.height);
            SwingUtilities.paintComponent(g, m_label, (Container)c, m_rect);
            if (!m_collapsed) {
                // left border
                g.drawLine(0, insets.top / 2, 0, height - insets.bottom);
                // bottom border
                g.drawLine(0, height - insets.bottom, width - insets.right,
                        height - insets.bottom);
                // right border
                g.drawLine(width - insets.right, height - insets.bottom,
                        width - insets.right, insets.top / 2);
            }
            // top to label
            g.drawLine(insets.left, insets.top / 2, insets.left + m_offset,
                    insets.top / 2);
            // top from label to left
            g.drawLine(insets.left + m_offset + size.width, insets.top / 2,
                    width - insets.right, insets.top / 2);

            g.translate(-x, -y);
            g.setColor(oldColor);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Insets getBorderInsets(final Component c) {
            Dimension size = m_label.getPreferredSize();
            if (m_collapsed) {
                return new Insets(size.height, 0, 0, 0);
            } else {
                return new Insets(size.height, 1, 1, 1);
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isBorderOpaque() {
            return true;
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
            if (e.isPopupTrigger()) {
                return;
            }
            Insets insets = getBorderInsets(m_container);
            Rectangle bounds = m_container.getBounds();
            if (e.getY() <= insets.top + bounds.y
                    && e.getX() <= insets.left + bounds.x
                                    + m_rect.x + m_rect.width) {
                m_container.setCollapsed(!m_collapsed);
            }
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
    }

}

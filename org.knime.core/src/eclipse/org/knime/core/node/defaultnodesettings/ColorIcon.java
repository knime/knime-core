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
 */

package org.knime.core.node.defaultnodesettings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;


/**
 * Icon that shows a rectangle with the given color and size.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class ColorIcon implements Icon {

    private Color m_color;

    private int m_size = 15;

    private final boolean m_framed;

    /**
     * Creates new squared color icon.
     *
     * @param color the color to show
     */
    public ColorIcon(final Color color) {
        this(color, 15);
    }

    /**
     * Creates new squared color icon.
     *
     * @param color the color to show
     * @param size the size of the icon
     */
    public ColorIcon(final Color color, final int size) {
        this(color, size, true);
    }

    /**
     * Creates new squared color icon.
     *
     * @param color the initial color
     * @param size the size of the icon
     * @param framed <code>true</code> if the icon should be framed
     */
    public ColorIcon(final Color color, final int size, final boolean framed) {
        m_color = color;
        m_size = size;
        m_framed = framed;
    }

    /**
     * Set's a new color.
     *
     * @param color the new Color
     */
    public void setColor(final Color color) {
        m_color = color;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return m_color;
    }

    /**
     * @param size the size of the icon
     */
    public void setSize(final int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Icon size must be positive");
        }
        m_size = size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIconHeight() {
        return m_size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIconWidth() {
        return m_size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintIcon(final Component c, final Graphics g, final int x,
            final int y) {
        if (m_color == null) {
            //draw a cross if no color is selected
            g.setColor(Color.BLACK);
            g.drawLine(x, y, x + m_size, y + m_size);
            g.drawLine(x, y + m_size, x + m_size, y);
            //draw a box
            g.drawRect(x, y, m_size, m_size);
        } else {
            g.setColor(m_color);
            g.fillRect(x, y, m_size, m_size);
            if (m_framed) {
                g.setColor(m_color.darker());
                g.drawRect(x, y, m_size, m_size);
            }
        }
    }
}

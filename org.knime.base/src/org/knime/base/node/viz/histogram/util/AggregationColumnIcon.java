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
 *   16.03.2007 (koetter): created
 */
package org.knime.base.node.viz.histogram.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import org.knime.core.data.DataColumnSpec;


/**
 * Constructs a new icon with its specific Color and size.
 */
public class AggregationColumnIcon implements Icon {
    private Color m_color;

    private final DataColumnSpec m_columnSpec;

    private static final int SIZE = 15;

    /**
     * Creates new squared color icon.
     * 
     * @param columnSpec The column specification.
     * @param color The initial color.
     */
    public AggregationColumnIcon(final DataColumnSpec columnSpec,
            final Color color) {
        m_columnSpec = columnSpec;
        m_color = color;
    }

    /**
     * Set's a new color.
     * 
     * @param color The new Color.
     */
    public void setColor(final Color color) {
        m_color = color;
    }

    /**
     * @return The color.
     */
    public Color getColor() {
        return m_color;
    }

    /**
     * @return The display text which is the name of the column
     */
    public String getText() {
        return m_columnSpec.getName();
    }

    /**
     * @return The column specification
     */
    public DataColumnSpec getColumnSpec() {
        return m_columnSpec;
    }
    
    /**
     * {@inheritDoc}
     */
    public int getIconHeight() {
        return SIZE;
    }

    /**
     * {@inheritDoc}
     */
    public int getIconWidth() {
        return SIZE;
    }

    /**
     * {@inheritDoc}
     */
    public void paintIcon(final Component c, final Graphics g, final int x,
            final int y) {
        g.setColor(m_color);
        g.fillRect(x, y, SIZE, SIZE);
    }
}

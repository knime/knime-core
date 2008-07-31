/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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

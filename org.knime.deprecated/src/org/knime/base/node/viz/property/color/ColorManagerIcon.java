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
 *   09.02.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;


/**
 * Constructs a new icon with its specific Color and size.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class ColorManagerIcon implements Icon {
    
    private Color m_color;

    private final DataCell m_cell;

    private static final int SIZE = 15;

    private final String m_prefix;

    /**
     * Creates new squared color icon.
     * 
     * @param cell The label.
     * @param color The initial color.
     */
    ColorManagerIcon(final DataCell cell, final Color color) {
        this(cell, "", color);
    }

    /**
     * Creates new squared color icon.
     * 
     * @param cell The label.
     * @param prefix The label's prefix.
     * @param color The inital color.
     */
    ColorManagerIcon(final DataCell cell, final String prefix, 
            final Color color) {
        m_color = color;
        m_cell = cell;
        m_prefix = prefix;
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
     * @return The label.
     */
    public DataCell getCell() {
        if (m_cell == null) {
            return DataType.getMissingCell();
        }
        return m_cell;
    }

    /**
     * @return The display text which is the prefix plus DataCell.
     */
    public String getText() {
        if (m_cell == null) {
            return m_prefix + "?";
        }
        return m_prefix + m_cell.toString();
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

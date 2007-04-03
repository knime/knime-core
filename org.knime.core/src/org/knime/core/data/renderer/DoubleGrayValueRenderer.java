/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Dec 5, 2005 (wiswedel): created
 */
package org.knime.core.data.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;


/**
 * Renderer for double cells that paints the whole range in gray color according
 * to the cell's value. It uses the domain information from the column spec to
 * determine min and max value and to find the appropriate gray value. If no
 * domain information is available, 0.0 and 1.0 are assumed to define the range.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DoubleGrayValueRenderer extends DefaultDataValueRenderer {
    
    private final Icon m_icon = new GrayIcon();
    
    /** Creates new instance given a column spec. This object will get the
     * information about min/max from the spec and do the normalization 
     * accordingly.
     * @param spec The spec from which to get min/max. May be null in which 
     *         case 0.0 and 1.0 are assumed.
     */
    public DoubleGrayValueRenderer(final DataColumnSpec spec) {
        super(spec);
        setIcon(new GrayIcon());
        setIconTextGap(0);
    }
    
    /** Overridden to ignore any invocation.
     * @see javax.swing.JLabel#setText(java.lang.String)
     */
    @Override
    public void setText(final String text) {
    }
    
    /* Internal setter. */
    private void setTextInternal(final String text) {
        super.setText(text);
    }
    
    /** Sets the color in the icon.
     * @param c The color to be used.
     */
    public void setIconColor(final Color c) {
        Icon icon = getIcon();
        if (icon == null) {
            super.setIcon(m_icon);
            icon = m_icon;
        }
        ((GrayIcon)icon).setColor(c);
    }
    
    /**
    /** Sets the gray value according to the value and the column
     * domain's min/max. If the object is not instance of DoubleValue, the
     * cell is painted red.
     * @param value The value to be rendered.
     * @see javax.swing.table.DefaultTableCellRenderer#setValue(Object)
     */
    @Override
    protected void setValue(final Object value) {
        Color c;
        if (value instanceof DoubleValue) {
            DoubleValue cell = (DoubleValue)value;
            double val = cell.getDoubleValue();
            DataColumnSpec spec = getColSpec();
            boolean takeValuesFrom = spec != null;
            takeValuesFrom &= spec.getDomain().hasLowerBound();
            takeValuesFrom &= spec.getDomain().hasUpperBound();
            takeValuesFrom &= DoubleCell.TYPE.isASuperTypeOf(
                    spec.getType());
            double min;
            double max;
            if (takeValuesFrom) {
                DataColumnDomain d = spec.getDomain();
                min = ((DoubleValue)d.getLowerBound()).getDoubleValue();
                max = ((DoubleValue)d.getUpperBound()).getDoubleValue();
            } else {
                min = Double.POSITIVE_INFINITY;
                max = Double.NEGATIVE_INFINITY;
            }
            if (min >= max) {
                min = 0.0;
                max = 1.0;
            }
            c = setDoubleValue(val, min, max);
            setIconColor(c);
            setTextInternal(null);
        } else {
            setIcon(null);
            setTextInternal(DataType.getMissingCell().toString());
        }
    }
    
    /** Method that may be overwritten to return a more specific color.
     * @param val The current value
     * @param min The minimum according the column spec.
     * @param max The maximum according the column spec.
     * @return The color for the current value, never <code>null</code>.
     */
    protected Color setDoubleValue(final double val, 
            final double min, final double max) {
        float gray = (float)((val - min) / (max - min));
        gray = Math.max(0.0f, gray);
        gray = Math.min(1.0f, gray);
        gray = 1.0f - gray;
        return new Color(gray, gray, gray);
        
    }
    
    /** Returns "Gray Scale".
     * @see org.knime.core.data.renderer.DataValueRenderer#getDescription()
     */
    @Override
    public String getDescription() {
        return "Gray Scale";
    }

    /** Private icon that is shown instead of any string. 
     * The code is mainly copied from javax.swing.plaf.basic.BasicIconFactory 
     * and altered accordingly.
     */
    private class GrayIcon implements Icon {

        private Color m_color = Color.WHITE;

        /**
         * @see javax.swing.Icon#getIconHeight()
         */
        public int getIconHeight() {
            return getHeight();
        }
        
        /**
         * @see javax.swing.Icon#getIconWidth()
         */
        public int getIconWidth() {
            return getWidth();
        }
        
        /** Sets the color for the next paint operation.
         * @param c The color to be used.
         */
        public void setColor(final Color c) {
            m_color = c;
        }
        
        /**
         * @see javax.swing.Icon#paintIcon(
         *      java.awt.Component, java.awt.Graphics, int, int)
         */
        public void paintIcon(
            final Component c, final Graphics g, final int x, final int y) {
            g.setColor(m_color);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
        }
    } // end class GrayIcon
}

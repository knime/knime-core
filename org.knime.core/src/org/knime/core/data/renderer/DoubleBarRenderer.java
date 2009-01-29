/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   01.02.2006 (cebron): created
 */
package org.knime.core.data.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Icon;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;


/**
 * Renderer for double cells that paints the whole range in a bar according
 * to the cell's value. It uses the domain information from the column spec to
 * determine min and max value and to find the appropriate gray value. If no
 * domain information is available, 0.0 and 1.0 are assumed to define the range.
 * @author Bernd Wiswedel, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class DoubleBarRenderer extends DefaultDataValueRenderer {

    /** Description that's returned in {@link #getDescription()}. */
    public static final String DESCRIPTION = "Bars";
    
    private BarIcon m_icon;

    /** Creates new instance given a column spec. This object will get the
     * information about min/max from the spec and do the normalization 
     * accordingly.
     * @param spec The spec from which to get min/max. May be null in which 
     *         case 0.0 and 1.0 are assumed.
     */
    public DoubleBarRenderer(final DataColumnSpec spec) {
        super(spec);
        m_icon = new BarIcon();
        setIcon(m_icon);
        setIconTextGap(0);
    }

    /** Overridden to ignore any invocation.
     * {@inheritDoc}
     */
    @Override
    public void setText(final String text) {
    }

    /** Sets the value for the icon.
     * @param d the value to be used.
     */
    public void setIconValue(final double d) {
        Icon icon = getIcon();
        if (icon == null) {
            super.setIcon(m_icon);
            icon = m_icon;
        }
        ((BarIcon)icon).setValue(d);
    }

    /**
     /** Sets the value according to the column domain's min/max. If the 
     * object is not instance of DoubleValue, the cell is painted red.
     * @param value The value to be rendered.
     * @see javax.swing.table.DefaultTableCellRenderer#setValue(Object)
     */
    @Override
    protected void setValue(final Object value) {
        double d = 0;
        if (value instanceof DoubleValue) {
            DoubleValue cell = (DoubleValue)value;
            double val = cell.getDoubleValue();
            DataColumnSpec spec = getColSpec();
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            if (spec != null) {
                DataColumnDomain domain = spec.getDomain();
                DataCell lower = domain.getLowerBound();
                DataCell upper = domain.getUpperBound();
                if (lower instanceof DoubleValue) {
                    min = ((DoubleValue)lower).getDoubleValue();
                }
                if (upper instanceof DoubleValue) {
                    max = ((DoubleValue)upper).getDoubleValue();
                }
            }
            if (min >= max) {
                min = 0.0;
                max = 1.0;
            }
            d = (float)((val - min) / (max - min));
            setToolTipText(Double.toString(val));
            setIconValue(d);
            setTextInternal(null);
        } else {
            setToolTipText("Missing Value");
            setIcon(null);
            setTextInternal(DataType.getMissingCell().toString());
        }
    }

    /* Internal setter. */
    private void setTextInternal(final String text) {
        super.setText(text);
    }

    /** Returns "Bars".
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    /** Returns <code>true</code> if the spec contains domain information 
     * and <code>false</code> otherwise. 
     * {@inheritDoc} */
    @Override
    public boolean accepts(final DataColumnSpec spec) {
        DataColumnDomain domain = spec.getDomain();
        return domain.hasLowerBound() && domain.hasUpperBound();
    }

    /** Private icon that is shown instead of any string. 
     * The code is mainly copied from javax.swing.plaf.basic.BasicIconFactory 
     * and altered accordingly.
     */
    private class BarIcon implements Icon {

        private double m_value = 0;

        /**
         * {@inheritDoc}
         */
        public int getIconHeight() {
            return getHeight();
        }

        /**
         * {@inheritDoc}
         */
        public int getIconWidth() {
            return getWidth();
        }

        /**
         * Sets the current vale.
         * @param d double value.
         */
        public void setValue(final double d) {
            m_value = d;
        }

        /**
         * {@inheritDoc}
         */
        public void paintIcon(final Component c, final Graphics g, final int x,
                final int y) {
            int iconWidth = getIconWidth();
            int width = (int)(m_value * iconWidth);
            GradientPaint redtogreen = new GradientPaint(x, y, Color.red,
                    iconWidth, y, Color.green);
            ((Graphics2D)g).setPaint(redtogreen);
            g.draw3DRect(x, y + (getIconHeight() / 4), width,
                    getIconHeight() / 2, true);
            ((Graphics2D)g).fill(new Rectangle2D.Double(x, y
                    + (getIconHeight() / 4), width, getIconHeight() / 2));
        }
    } // end class BarIcon
}

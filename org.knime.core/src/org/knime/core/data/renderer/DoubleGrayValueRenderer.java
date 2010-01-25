/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   Dec 5, 2005 (wiswedel): created
 */
package org.knime.core.data.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;


/**
 * Renderer for double cells that paints the whole range in gray color according
 * to the cell's value. It uses the domain information from the column spec to
 * determine min and max value and to find the appropriate gray value. If no
 * domain information is available, 0.0 and 1.0 are assumed to define the range.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DoubleGrayValueRenderer extends DefaultDataValueRenderer {
    
    /** Description that's returned in {@link #getDescription()}. */
    public static final String DESCRIPTION = "Gray Scale";
    
    private final Icon m_icon = new GrayIcon();
    private boolean m_isPaintCrossForMissing = false;
    
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
     * {@inheritDoc}
     */
    @Override
    public void setText(final String text) {
    }
    
    /** Internal setter for the text, delegates to super.setText().
     * @param text The text to write. */
    protected void setTextInternal(final String text) {
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
            setToolTipText(Double.toString(val));
            c = setDoubleValue(val, min, max);
            setIconColor(c);
            setTextInternal(null);
        } else if (isPaintCrossForMissing()) {
            setToolTipText("Missing Value");
            setIconColor(null);
            setTextInternal(null);
        } else {
            setToolTipText("Missing Value");
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
    
    /**
     * If a cross is painted for missing values.
     * @return the isPaintCrossForMissing property.
     * @see #setPaintCrossForMissing(boolean)
     */
    protected boolean isPaintCrossForMissing() {
        return m_isPaintCrossForMissing;
    }

    /**
     * If to paint a cross for missing values (if false a '?' is written).
     * @param isPaintCross If to paint a cross for missing values.
     */
    protected void setPaintCrossForMissing(final boolean isPaintCross) {
        m_isPaintCrossForMissing = isPaintCross;
    }

    /** Returns "Gray Scale".
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
    
    /** @return the width of the icon, defaults to <code>getWidth()</code>. */
    protected int getIconWidth() {
        return getWidth();
    }
    
    /** @return the height of the icon, defaults to <code>getHeight()</code>. */
    protected int getIconHeight() {
        return getHeight();
    }

    /** Private icon that is shown instead of any string. 
     * The code is mainly copied from javax.swing.plaf.basic.BasicIconFactory 
     * and altered accordingly.
     */
    private class GrayIcon implements Icon {

        private Color m_color = Color.WHITE;

        /**
         * {@inheritDoc}
         */
        public int getIconHeight() {
            return DoubleGrayValueRenderer.this.getIconHeight();
        }
        
        /**
         * {@inheritDoc}
         */
        public int getIconWidth() {
            return DoubleGrayValueRenderer.this.getIconWidth();
        }
        
        /** Sets the color for the next paint operation.
         * @param c The color to be used.
         */
        public void setColor(final Color c) {
            m_color = c;
        }
        
        /**
         * {@inheritDoc}
         */
        public void paintIcon(
            final Component c, final Graphics g, final int x, final int y) {
            if (m_color == null) {
                g.setColor(Color.BLACK);
                g.drawLine(x, y, x + getIconWidth(), y + getIconHeight());
                g.drawLine(x, y + getIconHeight(), x + getIconWidth(), y);
            } else {
                g.setColor(m_color);
                g.fillRect(x, y, getIconWidth(), getIconHeight());
            }
        }
    } // end class GrayIcon
}

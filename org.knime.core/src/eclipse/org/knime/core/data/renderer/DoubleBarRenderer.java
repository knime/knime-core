/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

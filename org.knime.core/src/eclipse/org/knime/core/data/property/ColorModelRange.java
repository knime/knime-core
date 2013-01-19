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
 *   02.06.2006 (gabriel): created
 */
package org.knime.core.data.property;

import java.awt.Color;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.property.ColorHandler.ColorModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;


/**
 * Computes colors based on a range of minimum and maximum values assigned to
 * certain colors which are interpolated between a min and maximum color.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ColorModelRange implements ColorModel {

    /** Lower bound. */
    private final double m_minValue;

    /** Upper bound. */
    private final double m_maxValue;

    /** Upper minus lower bound. */
    private final double m_range;

    /** Minimum color for lower bound. */
    private final Color m_minColor;

    /** Maximum color for upper bound. */
    private final Color m_maxColor;

    /**
     * Creates new ColorHandler based on a mapping.
     * 
     * @param lower Lower bound.
     * @param min Color of lower bound.
     * @param upper Upper bound.
     * @param max Color of upper bound.
     * @throws IllegalArgumentException If lower is greater or equal upper
     *             bound, or min or max is <code>null</code>.
     */
    public ColorModelRange(final double lower, final Color min,
            final double upper, final Color max) {
        if (lower > upper) {
            throw new IllegalArgumentException("Lower and upper bound are not"
                    + " ordered: lower=" + lower + ",upper=" + upper);
        }
        m_minValue = lower;
        m_maxValue = upper;
        m_range = upper - lower;
        if (min == null || max == null) {
            throw new IllegalArgumentException();
        }
        m_minColor = min;
        m_maxColor = max;
    }

    /**
     * Returns a ColorAttr for the given DataCell value, or
     * <code>ColorAttr.DEFAULT</code> if not set. The colors red, green, and
     * blue are merged in the same ratio from the original spread of the lower
     * and upper bounds.
     * 
     * @param dc A DataCell value to get color for.
     * @return A ColorAttr for a DataCell value or the DEFAULT ColorAttr.
     */
    public ColorAttr getColorAttr(final DataCell dc) {
        if (dc == null || dc.isMissing()
                || !dc.getType().isCompatible(DoubleValue.class)) {
            return ColorAttr.DEFAULT;
        }
        double ratio;
        // if lower and upper bound are equal
        if (m_range == 0.0) {
            // take the "middle" of both colors
            ratio = 0.5;
        } else {
            double value = ((DoubleValue)dc).getDoubleValue();
            ratio = ((value - m_minValue) / m_range);
        }
        int r = (int)((m_maxColor.getRed() - m_minColor.getRed()) * ratio)
                + m_minColor.getRed();
        int g = (int)((m_maxColor.getGreen() - m_minColor.getGreen()) * ratio)
                + m_minColor.getGreen();
        int b = (int)((m_maxColor.getBlue() - m_minColor.getBlue()) * ratio)
                + m_minColor.getBlue();
        // check color ranges first
        if (r < 0) {
            r = 0;
        }
        if (r > 255) {
            r = 255;
        }
        if (g < 0) {
            g = 0;
        }
        if (g > 255) {
            g = 255;
        }
        if (b < 0) {
            b = 0;
        }
        if (b > 255) {
            b = 255;
        }
        int alpha = (m_minColor.getAlpha() + m_maxColor.getAlpha()) / 2;
        return ColorAttr.getInstance(new Color(r, g, b, alpha));
    }

    /** @return minimum double value. */
    public double getMinValue() {
        return m_minValue;
    }

    /** @return minimum Color value. */
    public Color getMinColor() {
        return m_minColor;
    }

    /** @return maximum double value. */
    public double getMaxValue() {
        return m_maxValue;
    }

    /** @return maximum Color value. */
    public Color getMaxColor() {
        return m_maxColor;
    }

    private static final String CFG_LOWER_VALUE = "lower_value";

    private static final String CFG_UPPER_VALUE = "upper_value";

    private static final String CFG_LOWER_COLOR = "lower_color";

    private static final String CFG_UPPER_COLOR = "upper_color";

    /**
     * Save lower and upper, and min and max colors to the given Config.
     * 
     * @param config to save settings to.
     * @see org.knime.core.data.property.ColorHandler.ColorModel
     *      #save(ConfigWO)
     */
    public void save(final ConfigWO config) {
        config.addDouble(CFG_LOWER_VALUE, m_minValue);
        config.addDouble(CFG_UPPER_VALUE, m_maxValue);
        config.addIntArray(CFG_LOWER_COLOR, m_minColor.getRed(), m_minColor
                .getGreen(), m_minColor.getBlue(), m_minColor.getAlpha());
        config.addIntArray(CFG_UPPER_COLOR, m_maxColor.getRed(), m_maxColor
                .getGreen(), m_maxColor.getBlue(), m_maxColor.getAlpha());
    }

    /**
     * Load color settings from Config including lower and upper bound, and min
     * and max colors.
     * 
     * @param config Read settings from.
     * @return A new <code>ColorModelRange</code> object.
     * @throws InvalidSettingsException If the settings could not be read.
     */
    public static ColorModelRange load(final ConfigRO config)
            throws InvalidSettingsException {
        double lower = config.getDouble(CFG_LOWER_VALUE);
        double upper = config.getDouble(CFG_UPPER_VALUE);
        Color minColor;
        try {
            // load color components before 2.0
            int[] min = config.getIntArray(CFG_LOWER_COLOR);
            minColor = new Color(min[0], min[1], min[2], min[3]);
        } catch (InvalidSettingsException ise) {
            int min = config.getInt(CFG_LOWER_COLOR);
            minColor = new Color(min, true);
        }
        Color maxColor;
        try {
            // load color components before 2.0
            int[] max = config.getIntArray(CFG_UPPER_COLOR);
            maxColor = new Color(max[0], max[1], max[2], max[3]);
        } catch (InvalidSettingsException ise) {
            int max = config.getInt(CFG_UPPER_COLOR);
            maxColor = new Color(max, true);
        }
        return new ColorModelRange(lower, minColor, upper, maxColor);
    }

    /**
     * @return A string summary for this color model containing type and
     *         min/max values and colors.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String min = m_minValue + "," + m_minColor;
        String max = m_maxValue + "," + m_maxColor;
        return "DoubleRange ColorModel (min=<" + min + ">,max=<" + max + ">)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof ColorModelRange)) {
            return false;
        }
        ColorModelRange cmodel = (ColorModelRange) obj;
        // Double.compare() also handles NaN appropriately 
        return Double.compare(m_maxValue, cmodel.m_maxValue) == 0
            && Double.compare(m_minValue, cmodel.m_minValue) == 0
            && Double.compare(m_range, cmodel.m_range) == 0
            && m_maxColor.equals(cmodel.m_maxColor)
            && m_minColor.equals(cmodel.m_minColor); 
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_minColor.hashCode() ^ m_maxColor.hashCode();
    }
}

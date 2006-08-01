/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   02.06.2006 (gabriel): created
 */
package de.unikn.knime.core.data.property;

import java.awt.Color;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.property.ColorHandler.ColorModel;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.config.ConfigRO;
import de.unikn.knime.core.node.config.ConfigWO;

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
     * blue are merged in the same ratio from the orginal spread of the lower
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
        return ColorAttr.getInstance(new Color(r, g, b));
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
     * @see de.unikn.knime.core.data.property.ColorHandler.ColorModel
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
        int[] min = config.getIntArray(CFG_LOWER_COLOR);
        Color minColor = new Color(min[0], min[1], min[2], min[3]);
        int[] max = config.getIntArray(CFG_UPPER_COLOR);
        Color maxColor = new Color(max[0], max[1], min[2], min[3]);
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

}

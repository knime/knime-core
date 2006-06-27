/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   02.06.2006 (gabriel): created
 */
package de.unikn.knime.core.data.property;

import java.awt.Color;
import java.util.Arrays;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.property.ColorHandler.ColorModel;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.config.Config;

/**
 * Computes colors based on a range of minimum and maximum values
 * assigned to certain colors which are interpolated between a min and
 * maximum color.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ColorModelRange implements ColorModel {

    /** Lower bound. */
    private final double m_lower;
    
    /** Upper bound. */
    private final double m_upper;

    /** Upper minus lower bound. */
    private final double m_range;

    /** Minimum color for lower bound. */
    private final Color m_min;

    /** Maximum color for upper bound. */
    private final Color m_max;

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
        m_lower = lower;
        m_upper = upper;
        m_range = upper - lower;
        if (min == null || max == null) {
            throw new IllegalArgumentException();
        }
        m_min = min;
        m_max = max;
    }

    /**
     * Returns a ColorAttr for the given DataCell value, or 
     * <code>ColorAttr.DEFAULT</code> if not set. The colors red, green, and
     * blue are merged in the same ratio from the orginal spread of the 
     * lower and upper bounds.
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
            ratio = ((value - m_lower) / m_range);
        }
        int r = (int)((m_max.getRed() - m_min.getRed()) * ratio)
                + m_min.getRed();
        int g = (int)((m_max.getGreen() - m_min.getGreen()) * ratio)
                + m_min.getGreen();
        int b = (int)((m_max.getBlue() - m_min.getBlue()) * ratio)
                + m_min.getBlue();
        // check color ranges first
        if (r < 0) { r = 0; }
        if (r > 255) { r = 255; }
        if (g < 0) { g = 0; }
        if (g > 255) { g = 255; }
        if (b < 0) { b = 0; }
        if (b > 255) { b = 255; }
        return ColorAttr.getInstance(new Color(r, g, b));
    }
    
    private static final String CFG_LOWER_VALUE = "lower_value";
    private static final String CFG_UPPER_VALUE = "upper_value";
    private static final String CFG_LOWER_COLOR = "lower_color";
    private static final String CFG_UPPER_COLOR = "upper_color";

    /**
     * Save lower and upper, and min and max colors to the given Config.
     * @param config to save settings to.
     * @see de.unikn.knime.core.data.property.ColorHandler.ColorModel
     *      #save(de.unikn.knime.core.node.config.Config)
     */
    public void save(final Config config) {
        assert config.keySet().isEmpty() : "Subconfig must be empty: " 
            +  Arrays.toString(config.keySet().toArray());
        config.addDouble(CFG_LOWER_VALUE, m_lower);
        config.addDouble(CFG_UPPER_VALUE, m_upper);
        config.addIntArray(CFG_LOWER_COLOR, m_min.getRed(), m_min.getGreen(), 
                m_min.getBlue(), m_min.getAlpha());
        config.addIntArray(CFG_UPPER_COLOR, m_max.getRed(), m_max.getGreen(), 
                m_max.getBlue(), m_max.getAlpha());
    }
    
    /**
     * Load color settings from Config including lower and upper bound, and
     * min and max colors.
     * @param config Read settings from.
     * @return A new <code>ColorModelRange</code> object. 
     * @throws InvalidSettingsException If the settings could not be read.
     */
    public static ColorModelRange load(final Config config) 
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
     * Returns a string representation containing the type of the model and
     * an instance unique ID. 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Range ColorModel"; 
    }

}

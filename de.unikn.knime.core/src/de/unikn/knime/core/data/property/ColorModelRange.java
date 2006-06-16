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

    /**
     * Save lower and upper, and min and max colors to the given Config.
     * @param config to save settings to.
     * @see de.unikn.knime.core.data.property.ColorHandler.ColorModel
     *      #save(de.unikn.knime.core.node.config.Config)
     */
    public void save(final Config config) {
        config.addDouble("lower", m_lower);
        config.addDouble("upper", m_upper);
        config.addIntArray("min_color", m_min.getRed(), m_min.getGreen(), 
                m_min.getBlue(), m_min.getAlpha());
        config.addIntArray("max_color", m_max.getRed(), m_max.getGreen(), 
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
        double lower = config.getDouble("lower");
        double upper = config.getDouble("upper");
        int[] min = config.getIntArray("min_color");
        Color minColor = new Color(min[0], min[1], min[2], min[3]);
        int[] max = config.getIntArray("max_color");
        Color maxColor = new Color(max[0], max[1], min[2], min[3]);
        return new ColorModelRange(lower, minColor, upper, maxColor);
    }

}

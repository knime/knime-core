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
 *   07.02.2006 (gabriel): created
 */
package de.unikn.knime.core.node.property;

import java.awt.Color;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DoubleValue;

/**
 * ColorHandler computes colors based on a range of minimum and maximum values
 * assigned to certain colors which are interpolated.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorHandlerDoubleRange extends ColorHandler {

    private final double m_lower;

    private final double m_range;

    private final Color m_min;

    private final Color m_max;

    /**
     * Creates new ColorHandler based on a mapping.
     * 
     * @param lower Lower bound.
     * @param min Color of lower bound.
     * @param upper Upper bound.
     * @param max Color of upper bound.
     * @throws NullPointerException If min or max is <code>null</code>.
     * @throws IllegalArgumentException If lower is greater or equal upper
     *             bound.
     */
    public ColorHandlerDoubleRange(final double lower, final Color min,
            final double upper, final Color max) {
        if (lower > upper) {
            throw new IllegalArgumentException("Lower and upper bound are not"
                    + " ordered: lower=" + lower + ",upper=" + upper);
        }
        m_lower = lower;
        m_range = upper - lower;
        if (min == null || max == null) {
            throw new NullPointerException();
        }
        m_min = min;
        m_max = max;
    }

    /**
     * Returns a ColorAttr for the given DataCell value, or ColorAttr.DEFAULT if
     * not set. The colors red, green, and blue are merged in the same ratio
     * from the orginal spread of the lower and upper bounds.
     * 
     * @param dc A DataCell value to get color for.
     * @return A ColorAttr for a DataCell value or the DEFAULT ColorAttr.
     */
    @Override
    public ColorAttr getColorAttr(final DataCell dc) {
        if (dc == null || !dc.getType().isCompatible(DoubleValue.class)) {
            return ColorAttr.DEFAULT;
        }
        double ratio;
        // if lower and upper bound are equal, take the "middle" of both colors
        if (m_range == 0.0) {
            ratio = 0.5;
        } else {
            double value = ((DoubleValue)dc).getDoubleValue();
            ratio = ((value - m_lower) / m_range);
        }
        int r = (int)((m_min.getRed() - m_max.getRed()) * ratio)
                + m_max.getRed();
        int g = (int)((m_min.getGreen() - m_max.getGreen()) * ratio)
                + m_max.getGreen();
        int b = (int)((m_min.getBlue() - m_max.getBlue()) * ratio)
                + m_max.getBlue();
        return ColorAttr.getInstance(new Color(r, g, b));
    }

}

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
 *    23.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation;

import org.knime.core.node.util.ButtonGroupEnumInterface;


/**
 * Enumeration of different value scales.
 * @author Tobias Koetter, University of Konstanz
 */
public enum ValueScale implements ButtonGroupEnumInterface {
    /**The original scaling.*/
    ORIGINAL("original", "Original", "Original scale", null),
//    /**Logarithm scaling.*/
//    LOG("Log", "Log", "Logarithm scale", null),
    /**Percentage scaling.*/
    PERCENT("Percent", "Percent", "Percentage scale", "%");

    private final String m_actionCommand;

    private final String m_text;

    private final String m_toolTip;

    private final String m_extension;

    private ValueScale(final String id, final String label,
            final String toolTip, final String extension) {
        m_actionCommand = id;
        m_text = label;
        m_toolTip = toolTip;
        m_extension = extension;
    }

    /**
     * {@inheritDoc}
     */
    public String getActionCommand() {
        return m_actionCommand;
    }

    /**
     * {@inheritDoc}
     */
    public String getText() {
        return m_text;
    }

    /**
     * {@inheritDoc}
     */
    public String getToolTip() {
        return m_toolTip;
    }


    /**
     * @return the optional value scale extension
     */
    public String getExtension() {
        return m_extension;
    }

    /**
     * @return the default scale
     */
    public static ValueScale getDefaultMethod() {
        return ValueScale.ORIGINAL;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this.equals(ValueScale.getDefaultMethod());
    }

    /**
     * @param value the value to scale
     * @param totalValue the total value used to calculate percentage
     * @return the scaled value
     */
    public double scale(final double value, final double totalValue) {
        switch (this) {
        case PERCENT:
            if (totalValue == 0) {
                return 0;
            }
            return 100.0 / totalValue * Math.abs(value);
        default:
            return value;
        }
    }

    /**
     * Returns the scale for the given action command. If the command is
     * <code>null</code> or has length zero the method returns the default
     * scale.
     *
     * @param action the action command to check
     * @return the aggregation method with the given name
     */
    public static ValueScale getScale4Command(final String action) {
        if (action == null || action.length() < 1) {
            return getDefaultMethod();
        }
        for (final ValueScale value : ValueScale.values()) {
            if (value.getActionCommand().equals(action)) {
                return value;
            }
        }
        return getDefaultMethod();
    }

}

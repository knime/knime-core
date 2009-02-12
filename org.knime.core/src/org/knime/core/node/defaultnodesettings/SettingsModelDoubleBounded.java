/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   25.09.2006 (ohl): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;

/**
 * A settingsmodel for double default components accepting double between a min
 * and max value.
 * 
 * @author ohl, University of Konstanz
 */
public class SettingsModelDoubleBounded extends SettingsModelDouble {

    private final double m_minValue;

    private final double m_maxValue;

    /**
     * Creates a new double settings model with min and max value for the
     * accepted double value.
     * 
     * @param configName id the value is stored with in config objects.
     * @param defaultValue the initial value.
     * @param minValue lower bounds of the acceptable values.
     * @param maxValue upper bounds of the acceptable values.
     */
    public SettingsModelDoubleBounded(final String configName,
            final double defaultValue, final double minValue,
            final double maxValue) {
        super(configName, defaultValue);

        if (minValue > maxValue) {
            throw new IllegalArgumentException("Specified min value must be"
                    + " smaller than the max value.");
        }
        m_minValue = minValue;
        m_maxValue = maxValue;

        // the actual value is the specified default value
        try {
            checkBounds(defaultValue);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("InitialValue:"
                    + iae.getMessage());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SettingsModelDoubleBounded createClone() {
        return new SettingsModelDoubleBounded(getConfigName(),
                getDoubleValue(), m_minValue, m_maxValue);
    }

    /**
     * @return the lower bound of the acceptable values.
     */
    public double getLowerBound() {
        return m_minValue;
    }

    /**
     * @return the upper bound of the acceptable values.
     */
    public double getUpperBound() {
        return m_maxValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateValue(final double value)
            throws InvalidSettingsException {
        super.validateValue(value);
        try {
            checkBounds(value);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDoubleValue(final double newValue) {
        checkBounds(newValue);
        super.setDoubleValue(newValue);
    }

    private void checkBounds(final double val) {
        if ((val < m_minValue) || (m_maxValue < val)) {
            throw new IllegalArgumentException("value (=" + val
                    + ") must be within the range [" + m_minValue + "..."
                    + m_maxValue + "].");
        }
    }
}

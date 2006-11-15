/* ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   25.09.2006 (ohl): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

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
     * Constructor initializing the value from the specified settings object. If
     * the settings object doesn't contain a valid value for this model, it will
     * throw an InvalidSettingsException.
     * 
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param settings the object to read the initial value from
     * @param minValue lower bounds of the acceptable values.
     * @param maxValue upper bounds of the acceptable values.
     * @throws InvalidSettingsException if the settings object doesn't contain a
     *             (valid) value for this object
     */
    public SettingsModelDoubleBounded(final String configName,
            final NodeSettingsRO settings, final double minValue,
            final double maxValue) throws InvalidSettingsException {
        this(configName, minValue, minValue, maxValue);
        loadSettingsForModel(settings);
    }

    /**
     * Creates a new settings model with identical values for everything except
     * the stored value. The value stored in the model will be retrieved from
     * the specified settings object. If the settings object doesn't contain a
     * (valid) value it will throw an InvalidSettingsException.
     * 
     * @param settings the object to read the new model's value(s) from
     * @return a new settings model with the same constraints and configName but
     *         a value read from the specified settings object.
     * @throws InvalidSettingsException if the settings object passed doesn't
     *             contain a valid value for the newly created settings model.
     */
    public SettingsModelDoubleBounded createCloneWithNewValue(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return new SettingsModelDoubleBounded(getConfigName(), settings,
                m_minValue, m_maxValue);
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
     * @see SettingsModelDouble#setDoubleValue(double)
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

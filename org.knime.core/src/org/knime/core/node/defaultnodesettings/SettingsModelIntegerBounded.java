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
 * A settingsmodel for integer default components accepting double between a min
 * and max value.
 * 
 * @author ohl, University of Konstanz
 */
public class SettingsModelIntegerBounded extends SettingsModelInteger {

    private final int m_minValue;

    private final int m_maxValue;

    /**
     * Creates a new integer settings model with min and max value for the
     * accepted int value.
     * 
     * @param configName id the value is stored with in config objects.
     * @param defaultValue the initial value.
     * @param minValue lower bounds of the acceptable values.
     * @param maxValue upper bounds of the acceptable values.
     */
    public SettingsModelIntegerBounded(final String configName,
            final int defaultValue, final int minValue, final int maxValue) {
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
            throw new IllegalArgumentException("InitialValue: "
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
    public SettingsModelIntegerBounded(final String configName,
            final NodeSettingsRO settings, final int minValue,
            final int maxValue) throws InvalidSettingsException {
        this(configName, minValue, minValue, maxValue);
        loadSettingsForModel(settings);
    }

    /**
     * @return the lower bound of the acceptable values.
     */
    public int getLowerBound() {
        return m_minValue;
    }

    /**
     * @return the upper bound of the acceptable values.
     */
    public int getUpperBound() {
        return m_maxValue;
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModelInteger
     *      #setIntValue(int)
     */
    @Override
    public void setIntValue(final int newValue) {
        checkBounds(newValue);
        super.setIntValue(newValue);
    }

    private void checkBounds(final double val) {
        if ((val < m_minValue) || (m_maxValue < val)) {
            throw new IllegalArgumentException("value (=" + val
                    + ") must be within the range [" + m_minValue + "..."
                    + m_maxValue + "].");
        }
    }
}

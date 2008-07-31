/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   12.09.2007 (Fabian Dill): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class SettingsModelLongBounded extends SettingsModelLong {
    
    private final long m_minValue;
    
    private final long m_maxValue;

    /**
     * @param configName the key for the settings
     * @param defaultValue default value
     * @param minValue lower bound
     * @param maxValue upper bound
     * 
     */
    public SettingsModelLongBounded(final String configName, 
            final long defaultValue, final long minValue, 
            final long maxValue) {
        super(configName, defaultValue);
        m_minValue = minValue;
        m_maxValue = maxValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected SettingsModelLongBounded createClone() {
        return new SettingsModelLongBounded(getConfigName(), 
                getLongValue(), m_minValue, m_maxValue);
    }

    
    /**
     * 
     * @return lower bound
     */
    public long getLowerBound() {
        return m_minValue;
    }
    
    /**
     * 
     * @return upper bound
     */
    public long getUpperBound() {
        return m_maxValue;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateValue(final long value)
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
    public void setLongValue(final long newValue) {
        checkBounds(newValue);
        super.setLongValue(newValue);
    }

    private void checkBounds(final long val) {
        if ((val < m_minValue) || (m_maxValue < val)) {
            throw new IllegalArgumentException("value (=" + val
                    + ") must be within the range [" + m_minValue + "..."
                    + m_maxValue + "].");
        }
    }
    
}

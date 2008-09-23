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
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class SettingsModelLong extends SettingsModelNumber {
    
    private long m_value;

    private final String m_configKey;

    /**
     * Creates a new object holding a long value.
     * 
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelLong(final String configName, final long defaultValue) {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }        
        m_configKey = configName;
        m_value = defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getNumberValueStr() {
        return Long.toString(m_value);
    }
    
    /**
     * If the new value is different from the old value the listeners are 
     * notified.
     * 
     * @param value the new value
     */
    public void setLongValue(final long value) {
        boolean notify = (value != m_value);

        m_value = value;

        if (notify) {
            notifyChangeListeners();
        }
    }
    
    /**
     * 
     * @return the stored long value
     */
    public long getLongValue() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setNumberValueStr(final String newValueStr) {
        m_value = Long.parseLong(newValueStr);
    }

    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected SettingsModelLong createClone() {
        SettingsModelLong model = new SettingsModelLong(m_configKey, m_value);
        return model;
    }

    

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMN_long";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            // use the current value, if no value is stored in the settings
            setLongValue(settings.getLong(m_configKey, m_value));
        } catch (IllegalArgumentException e) {
            // if the value is not accepted, keep the old value.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            // use the current value, if no value is stored in the settings
            setLongValue(settings.getLong(m_configKey, m_value));
        } catch (IllegalArgumentException e) {
            // if the value is not accepted, keep the old value.
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addLong(m_configKey, m_value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configKey + "')";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        validateValue(settings.getLong(m_configKey));
    }
    
    /**
     * Called during {@link #validateSettingsForModel}, can be overridden by
     * derived classes.
     * 
     * @param value the value to validate
     * @throws InvalidSettingsException if the value is not valid and should be
     *             rejected
     */
    protected void validateValue(final long value)
            throws InvalidSettingsException {
        if (value != value) {
            throw new InvalidSettingsException("Value is not equal to itself");
        }
    }    

}

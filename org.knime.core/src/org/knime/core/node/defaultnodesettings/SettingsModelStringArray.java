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
 *   18.10.2006 (ohl): created
 */
package org.knime.core.node.defaultnodesettings;

import java.util.Arrays;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class SettingsModelStringArray extends SettingsModel {

    private String[] m_value;

    private final String m_configName;

    /**
     * Creates a new object holding a string value.
     * 
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelStringArray(final String configName,
            final String[] defaultValue) {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_value = defaultValue;
        m_configName = configName;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override    
    protected SettingsModelStringArray createClone() {
        return new SettingsModelStringArray(m_configName, m_value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_stringarray";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            // use the current value, if no value is stored in the settings
            setStringArrayValue(settings.getStringArray(m_configName, m_value));
        } catch (IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
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
     * set the value stored to (a copy of) the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setStringArrayValue(final String[] newValue) {
        boolean same;
        if (newValue == null) {
            same = (m_value == null);
        } else {
            if ((m_value == null) || (m_value.length != newValue.length)) {
                same = false;
            } else {
                List<String> current = Arrays.asList(m_value);
                same = true;
                for (String s : newValue) {
                    if (!current.contains(s)) {
                        same = false;
                        break;
                    }
                }
            }
        }
        
        if (newValue == null) {
            m_value = null;
        } else {
            m_value = new String[newValue.length];
            System.arraycopy(newValue, 0, m_value, 0, newValue.length);
        }
        
        if (!same) {
            notifyChangeListeners();
        }
    }

    /**
     * @return the (a copy of the) current value stored.
     */
    public String[] getStringArrayValue() {
        if (m_value == null) {
            return null;
        }
        String[] result = new String[m_value.length];
        System.arraycopy(m_value, 0, result, 0, m_value.length);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getStringArray(m_configName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            setStringArrayValue(settings.getStringArray(m_configName));
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addStringArray(m_configName, getStringArrayValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

}

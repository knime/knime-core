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
 *   18.10.2006 (ohl): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

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
     * Constructor initializing the value from the specified settings object. If
     * the settings object doesn't contain a valid value for this model, it will
     * throw an InvalidSettingsException.
     * 
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param settings the object to read the initial value from
     * @throws InvalidSettingsException if the settings object doesn't contain a
     *             (valid) value for this object
     */
    public SettingsModelStringArray(final String configName,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        this(configName, (String[])null);
        loadSettingsForModel(settings);
    }
    
    /**
     * @see SettingsModel#getModelTypeID()
     */
    @Override
    String getModelTypeID() {
        return "SMID_stringarray";
    }

    /**
     * @see SettingsModel#getConfigName()
     */
    @Override
    String getConfigName() {
        return m_configName;
    }

    /**
     * @see SettingsModel
     *      #loadSettingsForDialog(org.knime.core.node.NodeSettingsRO,
     *      org.knime.core.data.DataTableSpec[])
     */
    @Override
    void loadSettingsForDialog(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            // use the current value, if no value is stored in the settings
            setStringArrayValue(settings.getStringArray(m_configName, m_value));
        } catch (IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
        } finally {
            // always notify the listeners. That is, because there could be an
            // invalid value displayed in the listener.
            notifyChangeListeners();
        }

    }

    /**
     * @see SettingsModel
     *      #saveSettingsForDialog(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    void saveSettingsForDialog(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        saveSettingsTo(settings);
    }

    /**
     * set the value stored to (a copy of) the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setStringArrayValue(final String[] newValue) {
        if (newValue == null) {
            m_value = null;
        } else {
            m_value = new String[m_value.length];
            System.arraycopy(newValue, 0, m_value, 0, newValue.length);
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
     * @see SettingsModel
     *      #validateSettingsForModel(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    public void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getStringArray(m_configName);
    }

    /**
     * @see SettingsModel
     *      #loadSettingsForModel(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    public void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            setStringArrayValue(settings.getStringArray(m_configName));
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * @see SettingsModel
     *      #saveSettingsForModel(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addStringArray(m_configName, getStringArrayValue());
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

}

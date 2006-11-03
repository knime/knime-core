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
public class SettingsModelString extends SettingsModel {

    private String m_value;

    private final String m_configName;

    /**
     * Creates a new object holding a string value.
     * 
     * @param configName the identifier the value is stored with in the
     *            {@link NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelString(final String configName,
            final String defaultValue) {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_value = defaultValue;
        m_configName = configName;
    }

    /**
     * @see SettingsModel#getModelTypeID()
     */
    @Override
    String getModelTypeID() {
        return "SMID_string";
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
            setStringValue(settings.getString(m_configName, m_value));
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
     * set the value stored to the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setStringValue(final String newValue) {
        m_value = newValue;
    }

    /**
     * @return the current value stored.
     */
    public String getStringValue() {
        return m_value;
    }

    /**
     * @see SettingsModel
     *      #validateSettingsForModel(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    public void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(m_configName);
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
            setStringValue(settings.getString(m_configName));
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
        settings.addString(m_configName, getStringValue());
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

}

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
 *   22.09.2006 (ohl): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * A settingsmodel for boolean default components.
 * 
 * @author ohl, University of Konstanz
 */
public class SettingsModelBoolean extends SettingsModel {

    private boolean m_value;

    private final String m_configName;

    /**
     * Creates a new object holding a boolean value.
     * 
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelBoolean(final String configName,
            final boolean defaultValue) {
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
    public SettingsModelBoolean(final String configName,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        this(configName, true);
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
    public SettingsModelBoolean createCloneWithNewValue(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return new SettingsModelBoolean(m_configName, settings);
    }

    /**
     * @see SettingsModel#getModelTypeID()
     */
    @Override
    String getModelTypeID() {
        return "SMID_boolean";
    }

    /**
     * @see SettingsModel#getConfigName()
     */
    @Override
    String getConfigName() {
        return m_configName;
    }

    /**
     * set the value stored to the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setBooleanValue(final boolean newValue) {
        m_value = newValue;
    }

    /**
     * @return the current value stored.
     */
    public boolean getBooleanValue() {
        return m_value;
    }

    /**
     * @see SettingsModel
     *      #loadSettingsForDialog(org.knime.core.node.NodeSettingsRO,
     *      org.knime.core.data.DataTableSpec[])
     */
    @Override
    void loadSettingsForDialog(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // the load method for the dialog is not checking the id - we can load
        // anything!

        // use the current value, if no value is stored in the settings
        m_value = settings.getBoolean(m_configName, m_value);
        // let the associated component know that the value changed.
        notifyChangeListeners();
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
     * @see SettingsModel
     *      #validateSettingsForModel(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    public void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getBoolean(m_configName);
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel
     *      #loadSettingsForModel(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    public void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // no default value, throw an exception instead
        m_value = settings.getBoolean(m_configName);
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel
     *      #saveSettingsForModel(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addBoolean(m_configName, m_value);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }
}

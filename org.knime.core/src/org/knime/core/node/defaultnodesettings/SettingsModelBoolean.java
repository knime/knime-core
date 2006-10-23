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
     *            {@link NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelBoolean(final String configName,
            final boolean defaultValue) {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        setBooleanValue(defaultValue);
        m_configName = configName;
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
     *      #dlgLoadSettingsFrom(org.knime.core.node.NodeSettingsRO,
     *      org.knime.core.data.DataTableSpec[])
     */
    @Override
    void dlgLoadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // use the current value, if no value is stored in the settings
        m_value = settings.getBoolean(m_configName, m_value);
        // let the associated component know that the value changed.
        notifyChangeListeners();
    }

    /**
     * @see SettingsModel #dlgSaveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    void dlgSaveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        saveSettingsTo(settings);
    }

    /**
     * @see SettingsModel#loadSettingsFrom(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // no default value, throw an exception instead
        m_value = settings.getBoolean(m_configName);
    }

    /**
     * @see SettingsModel#saveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(m_configName, m_value);
    }

}

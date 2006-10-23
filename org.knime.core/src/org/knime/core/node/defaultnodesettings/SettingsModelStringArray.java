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
     *            {@link NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelStringArray(final String configName,
            final String[] defaultValue) {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        setStringArrayValue(defaultValue);
        
        m_configName = configName;
    }

    /**
     * @see SettingsModel
     *      #dlgLoadSettingsFrom(org.knime.core.node.NodeSettingsRO,
     *      org.knime.core.data.DataTableSpec[])
     */
    @Override
    void dlgLoadSettingsFrom(final NodeSettingsRO settings, 
            final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            // use the current value, if no value is stored in the settings
            setStringArrayValue(settings.getStringArray(m_configName, m_value));
        } catch (IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
            return;
        }
        // let the associated component know that the value changed.
        notifyChangeListeners();

    }

    /**
     * @see SettingsModel#dlgSaveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    void dlgSaveSettingsTo(final NodeSettingsWO settings)
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
     * @see SettingsModel#loadSettingsFrom(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            setStringArrayValue(settings.getStringArray(m_configName));
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae);
        }
    }

    /**
     * @see SettingsModel#saveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addStringArray(m_configName, getStringArrayValue());    
    }

}

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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * A settingsmodel for double default components.
 * 
 * @author ohl, University of Konstanz
 */
public class SettingsModelDouble extends SettingsModelNumber {

    private double m_value;

    private final String m_configName;

    /**
     * Creates a new object holding a double value.
     * 
     * @param configName the identifier the value is stored with in the
     *            {@link NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelDouble(final String configName,
            final double defaultValue) {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        setDoubleValue(defaultValue);
        m_configName = configName;
    }

    /**
     * set the value stored to the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setDoubleValue(final double newValue) {
        m_value = newValue;
    }

    /**
     * @return the current value stored.
     */
    public double getDoubleValue() {
        return m_value;
    }

    /**
     * Allows to set a new value by passing a string that will be parsed and, if
     * valid, set as new value.
     * 
     * @param newValueStr the new value to be set, as string representation
     */
    @Override
    void setNumberValueStr(final String newValueStr) {
        try {
            double tmp = Double.valueOf(newValueStr);
            setDoubleValue(tmp);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid floating point number"
                    + " format.");
        }
    }
    
    /**
     * @see SettingsModelNumber#getNumberValueStr()
     */
    @Override
    String getNumberValueStr() {
        return Double.toString(getDoubleValue());
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
            setDoubleValue(settings.getDouble(m_configName, m_value));
            // let the associated component know that the value changed.
            notifyChangeListeners();
        } catch (IllegalArgumentException e) {
            // if the value is not accepted, keep the old value.
        }
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
        try {
            // no default value, throw an exception instead
            setDoubleValue(settings.getDouble(m_configName));

        } catch (IllegalArgumentException iae) {
            // value not accepted
            throw new InvalidSettingsException(iae);
        }
    }

    /**
     * @see SettingsModel#saveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addDouble(m_configName, m_value);
    }

}

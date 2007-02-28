/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   25.09.2006 (ohl): created
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
public class SettingsModelInteger extends SettingsModelNumber {

    private int m_value;

    private final String m_configName;

    /**
     * Creates a new object holding an integer value.
     * 
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelInteger(final String configName, 
            final int defaultValue) {
        if ((configName == null) || (configName == "")) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_value = defaultValue;
        m_configName = configName;
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.SettingsModel#createClone()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelInteger createClone() {
        return new SettingsModelInteger(m_configName, m_value);
    }

    /**
     * @see SettingsModel#getModelTypeID()
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_integer";
    }

    /**
     * @see SettingsModel#getConfigName()
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * set the value stored to the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setIntValue(final int newValue) {
        boolean notify = (m_value != newValue);

        m_value = newValue;

        if (notify) {
            notifyChangeListeners();
        }
    }

    /**
     * @return the current value stored.
     */
    public int getIntValue() {
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
            int tmp = Integer.valueOf(newValueStr);
            setIntValue(tmp);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer number"
                    + " format ('" + newValueStr + "')");
        }
    }

    /**
     * @return a string representation of the current value
     */
    @Override
    String getNumberValueStr() {
        return Integer.toString(getIntValue());
    }

    /**
     * @see SettingsModel
     *      #loadSettingsForDialog(org.knime.core.node.NodeSettingsRO,
     *      org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        try {
            // use the current value, if no value is stored in the settings
            setIntValue(settings.getInt(m_configName, m_value));
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
    protected void saveSettingsForDialog(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * @see SettingsModel
     *      #validateSettingsForModel(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        // no default value, throw an exception instead
        validateValue(settings.getInt(m_configName));
    }

    /**
     * Called during {@link #validateSettingsForModel}, can be overriden by
     * derived classes.
     * 
     * @param value the value to validate
     * @throws InvalidSettingsException if the value is not valid and should be
     *             rejected
     */
    protected void validateValue(final int value)
            throws InvalidSettingsException {
        if (value != value) {
            throw new InvalidSettingsException("Value is not equal to itself");
        }
    }

    /**
     * @see SettingsModel
     *      #loadSettingsForModel(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            setIntValue(settings.getInt(m_configName));
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * @see SettingsModel
     *      #saveSettingsForModel(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addInt(m_configName, m_value);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

}

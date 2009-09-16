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
 *   25.09.2006 (ohl): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A settingsmodel for double default components.
 *
 * @author ohl, University of Konstanz
 */
public class SettingsModelDouble extends SettingsModelNumber
implements SettingsModelFlowVariableCompatible {

    private double m_value;

    private final String m_configName;

    /**
     * Creates a new object holding a double value.
     *
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelDouble(final String configName,
            final double defaultValue) {
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
    protected SettingsModelDouble createClone() {
        return new SettingsModelDouble(m_configName, m_value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_double";
    }

    /**
     * {@inheritDoc}
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
    public void setDoubleValue(final double newValue) {
        boolean notify = (newValue != m_value);

        m_value = newValue;

        if (notify) {
            notifyChangeListeners();
        }
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
     * {@inheritDoc}
     */
    @Override
    String getNumberValueStr() {
        return Double.toString(getDoubleValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            // use the current value, if no value is stored in the settings
            setDoubleValue(settings.getDouble(m_configName, m_value));
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
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        validateValue(settings.getDouble(m_configName));
    }

    /**
     * Called during {@link #validateSettingsForModel}, can be overwritten by
     * derived classes.
     *
     * @param value the value to validate
     * @throws InvalidSettingsException if the value is not valid and should be
     *             rejected
     */
    protected void validateValue(final double value)
            throws InvalidSettingsException {
        // derived class needs to check value
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            setDoubleValue(settings.getDouble(m_configName));

        } catch (IllegalArgumentException iae) {
            // value not accepted
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addDouble(m_configName, m_value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKey() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowVariable.Type getFlowVariableType() {
        return FlowVariable.Type.DOUBLE;
    }
}

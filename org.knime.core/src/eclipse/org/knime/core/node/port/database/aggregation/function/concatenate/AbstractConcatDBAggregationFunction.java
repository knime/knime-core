/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on 22.08.2014 by koetter
 */
package org.knime.core.node.port.database.aggregation.function.concatenate;

import java.awt.Component;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;

/**
 * Abstract class that can be used by concatenation functions. It provides a simple dialog that allows to set
 * the concatenation separator.
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public abstract class AbstractConcatDBAggregationFunction implements DBAggregationFunction {

    private ConcatDBAggregationFuntionSettingsPanel m_settingsPanel;
    private final ConcatDBAggregationFuntionSettings m_settings;

    /**
     * @param settings {@link ConcatDBAggregationFuntionSettings}
     */
    public AbstractConcatDBAggregationFunction(final ConcatDBAggregationFuntionSettings settings) {
        m_settings = settings;
    }

    /**
     * @return the settings
     */
    protected ConcatDBAggregationFuntionSettings getSettings() {
        return m_settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getSettingsPanel() {
        if (m_settingsPanel == null) {
            m_settingsPanel = new ConcatDBAggregationFuntionSettingsPanel(m_settings);
        }
        return m_settingsPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
        try {
            m_settings.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws InvalidSettingsException {
        //nothing to validate
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final DataTableSpec spec) throws InvalidSettingsException {
        //nothing to check or do
    }
}
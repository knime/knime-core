/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com AG, Zurich, Switzerland
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
 * Created: 02.07.2012
 * Author: Peter Ohl
 */
package org.knime.core.node.defaultnodesettings;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

/**
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 * @since 2.6
 */
public class DialogComponentColumnFilter2 extends DialogComponent {

    private final DataColumnSpecFilterPanel m_colFilterPanel;

    /**
     * @param model
     * @param showKeepAllBox
     */
    @SuppressWarnings("unchecked")
    public DialogComponentColumnFilter2(final SettingsModelColumnFilter2 model, final boolean showKeepAllBox) {
        this(model, showKeepAllBox, DataValue.class);
    }

    /**
     * @param model
     * @param showKeepAllBox
     * @param allowedTypes
     */
    public DialogComponentColumnFilter2(final SettingsModelColumnFilter2 model, final boolean showKeepAllBox,
            final Class<? extends DataValue>... allowedTypes) {
        this(model, showKeepAllBox, new DataTypeColumnFilter(allowedTypes));
    }

    /**
     * @param model
     * @param showKeepAllBox
     * @param filter
     */
    public DialogComponentColumnFilter2(final SettingsModelColumnFilter2 model, final boolean showKeepAllBox,
            final DataTypeColumnFilter filter) {
        super(model);
        m_colFilterPanel = new DataColumnSpecFilterPanel(showKeepAllBox, filter);
        getComponentPanel().add(m_colFilterPanel);
        m_colFilterPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateModel();
            }
        });
        getModel().prependChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        SettingsModelColumnFilter2 model = (SettingsModelColumnFilter2)getModel();
        DataColumnSpecFilterConfiguration modelConfiguration = model.getFilterConfiguration();
        DataColumnSpecFilterConfiguration panelConfig =
                new DataColumnSpecFilterConfiguration(modelConfiguration.getConfigRootName());
        m_colFilterPanel.saveConfiguration(panelConfig);
        if (!modelConfiguration.equals(panelConfig)) {
            // only update if out of sync
            m_colFilterPanel.loadConfiguration(modelConfiguration,
                    (DataTableSpec)getLastTableSpec(model.getInputPortIndex()));
        }

        m_colFilterPanel.setEnabled(model.isEnabled());
    }

    private void updateModel() {
        DataColumnSpecFilterConfiguration panelConf = new DataColumnSpecFilterConfiguration(getModel().getConfigName());
        m_colFilterPanel.saveConfiguration(panelConf);
        ((SettingsModelColumnFilter2)getModel()).setFilterConfiguration(panelConf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        // just in case we didn't get notified about the last change...
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // currently we are opening the dialog even with empty lists...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_colFilterPanel.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_colFilterPanel.setToolTipText(text);
    }

}

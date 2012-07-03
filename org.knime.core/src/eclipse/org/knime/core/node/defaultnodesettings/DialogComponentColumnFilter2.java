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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 * @since 2.6
 */
public class DialogComponentColumnFilter2 extends DialogComponent {

    private final DataColumnSpecFilterPanel m_colFilterPanel;

    private final int m_inPortIdx;

    /**
     * @param model
     * @param inPortIdx
     */
    public DialogComponentColumnFilter2(final SettingsModelColumnFilter2 model, final int inPortIdx) {
        this(model, inPortIdx, false);
    }

    /**
     * @param model
     * @param inPortIdx
     * @param showSelectionListsOnly if true, the panel shows no additional options like search box,
     * force-include-option, etc.
     */
    public DialogComponentColumnFilter2(final SettingsModelColumnFilter2 model, final int inPortIdx,
            final boolean showSelectionListsOnly) {
        super(model);
        m_inPortIdx = inPortIdx;
        // the model needs the port index in the loadSettingsFrom method
        model.setInputPortIndex(inPortIdx);
        m_colFilterPanel = new DataColumnSpecFilterPanel(showSelectionListsOnly, model.getColumnFilter());
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
        DataColumnSpecFilterConfiguration modelConfig = model.getFilterConfiguration();
        DataColumnSpecFilterConfiguration panelConfig =
                new DataColumnSpecFilterConfiguration(modelConfig.getConfigRootName(), modelConfig.getFilter());
        m_colFilterPanel.saveConfiguration(panelConfig);
        if (!modelConfig.equals(panelConfig)) {
            // only update if out of sync
            m_colFilterPanel.loadConfiguration(modelConfig, (DataTableSpec)getLastTableSpec(m_inPortIdx));
        }

        m_colFilterPanel.setEnabled(model.isEnabled());
    }

    private void updateModel() {
        SettingsModelColumnFilter2 model = (SettingsModelColumnFilter2)getModel();
        DataColumnSpecFilterConfiguration panelConf =
                new DataColumnSpecFilterConfiguration(model.getConfigName(), model.getFilterConfiguration().getFilter());
        m_colFilterPanel.saveConfiguration(panelConf);
        model.setFilterConfiguration(panelConf);
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

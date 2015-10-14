/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.BorderLayout;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * A column twin list with include &amp; exclude list and optionally column name and type matcher.
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 * @since 2.6
 */
public class DialogComponentColumnFilter2 extends DialogComponent {

    private final DataColumnSpecFilterPanel m_colFilterPanel;
    private boolean m_componentUpdateOngoing;

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
        getComponentPanel().setLayout(new BorderLayout());
        getComponentPanel().add(m_colFilterPanel);
        m_colFilterPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (!m_componentUpdateOngoing) {
                    updateModel();
                }
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
        DataColumnSpecFilterConfiguration panelConfig = modelConfig.clone();
        m_colFilterPanel.saveConfiguration(panelConfig);
        // bug 4681, we always load since the specs could have changed
        m_componentUpdateOngoing = true;
        try {
            // causes events to be fired and recursive calls, prevent updates
            m_colFilterPanel.loadConfiguration(modelConfig, (DataTableSpec)getLastTableSpec(m_inPortIdx));
        } finally {
            m_componentUpdateOngoing = false;
        }

        m_colFilterPanel.setEnabled(model.isEnabled());
    }

    private void updateModel() {
        SettingsModelColumnFilter2 model = (SettingsModelColumnFilter2)getModel();
        DataColumnSpecFilterConfiguration panelConf = model.getFilterConfiguration().clone();
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
     * Sets the title of the include panel.
     *
     * @param title the new title
     */
    public void setIncludeTitle(final String title) {
        m_colFilterPanel.setIncludeTitle(title);
    }

    /**
     * Sets the title of the exclude panel.
     *
     * @param title the new title
     */
    public void setExcludeTitle(final String title) {
        m_colFilterPanel.setExcludeTitle(title);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_colFilterPanel.setToolTipText(text);
    }

}

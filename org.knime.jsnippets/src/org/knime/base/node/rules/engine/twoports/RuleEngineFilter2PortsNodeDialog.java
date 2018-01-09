/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   2 June 2015 (Gabor): created
 */
package org.knime.base.node.rules.engine.twoports;

import java.awt.GridBagConstraints;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.base.node.rules.engine.RuleFactory;
import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The dialog for the Rule-based Filter/Splitter (Dictionary) nodes.
 *
 * @author Gabor Bakos
 */
class RuleEngineFilter2PortsNodeDialog extends RuleEngine2PortsSimpleNodeDialog {
    private JRadioButton m_top, m_bottom;
    private DataTableSpec m_dataTableSpec;

    /**
     * Constructs the appropriate dialog.
     *
     * @param ruleType Either filter or splitter.
     */
    public RuleEngineFilter2PortsNodeDialog(final RuleNodeSettings ruleType) {
        super(new RuleEngine2PortsSimpleSettings(), ruleType);
    }

    /**
     * {@inheritDoc}
     * Adds radio buttons for selecting the output content.
     */
    @Override
    protected void addAppendOrReplace(final JPanel panel, final GridBagConstraints gbc) {
        m_top = new JRadioButton(m_ruleType.topText());
        m_bottom = new JRadioButton(m_ruleType.bottomText());
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_top);
        buttonGroup.add(m_bottom);
        gbc.gridx = 0;

        gbc.gridwidth = 1;
        gbc.weightx = 0;
        if (m_ruleType == RuleNodeSettings.RuleSplitter) {
            panel.add(new JLabel("TRUE matches go to:"), gbc);
        }
        gbc.gridx++;
        panel.add(m_top, gbc);
        gbc.gridx++;
        gbc.gridx++;
        panel.add(m_bottom, gbc);
        gbc.gridy++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RuleFactory ruleFactory() {
        return RuleFactory.getInstance(m_ruleType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSpecAvailable() {
        return getDataSpec() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec getDataSpec() {
        return m_dataTableSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_dataTableSpec = (DataTableSpec)specs[RuleEngine2PortsNodeModel.DATA_PORT];
        boolean includeOnMatch = settings.getBoolean(RuleEngineFilter2PortsNodeModel.CFGKEY_INCLUDE_ON_MATCH, RuleEngineFilter2PortsNodeModel.DEFAULT_INCLUDE_ON_MATCH);
        m_top.setSelected(includeOnMatch);
        m_bottom.setSelected(!includeOnMatch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        settings.addBoolean(RuleEngineFilter2PortsNodeModel.CFGKEY_INCLUDE_ON_MATCH, m_top.isSelected());
    }
}

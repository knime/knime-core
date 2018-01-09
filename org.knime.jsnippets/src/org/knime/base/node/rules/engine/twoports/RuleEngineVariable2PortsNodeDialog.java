/*
 * ------------------------------------------------------------------------
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
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules.engine.twoports;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.base.node.rules.engine.RuleFactory;
import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.base.node.rules.engine.Util;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Rule Engine Variable (Dictionary) node dialog.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Gabor Bakos
 */
class RuleEngineVariable2PortsNodeDialog extends RuleEngine2PortsSimpleNodeDialog {
    private JTextField m_newVariableName;

    /**
     * Constructs the default {@link RuleEngineVariable2PortsNodeDialog}.
     */
    RuleEngineVariable2PortsNodeDialog() {
        super(new RuleEngine2PortsSimpleSettings(), RuleNodeSettings.VariableRule);
    }

    /**
     * {@inheritDoc}
     * Adds the control for the new flow variable name.
     */
    @Override
    protected void addAppendOrReplace(final JPanel panel, final GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 0;
        panel.add(new JLabel("Result variable:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        m_newVariableName = Util.createTextFieldWithWatermark(RuleEngineVariable2PortsNodeModel.DEFAULT_VARIABLE_NAME, 33, "Name of the new flow variable");
        panel.add(m_newVariableName, gbc);
        gbc.gridy++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RuleFactory ruleFactory() {
        RuleFactory ret = RuleFactory.getInstance(RuleNodeSettings.VariableRule).cloned();
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_newVariableName.setText(settings.getString(RuleEngineVariable2PortsNodeModel.VARIABLE_NAME, RuleEngineVariable2PortsNodeModel.DEFAULT_VARIABLE_NAME));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        settings.addString(RuleEngineVariable2PortsNodeModel.VARIABLE_NAME, m_newVariableName.getText());
    }
}

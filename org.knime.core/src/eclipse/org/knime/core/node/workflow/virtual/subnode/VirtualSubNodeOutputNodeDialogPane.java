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
 *   Apr 11, 2014 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual.subnode;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer.FlowVariableCell;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.util.filter.variable.FlowVariableFilterPanel;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * Dialog to sub node output. Shows currently only a twin list to allow the user to select variables to
 * export.
 * <p>No API.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class VirtualSubNodeOutputNodeDialogPane extends NodeDialogPane {
    private final int m_portCount;

    private final FlowVariableFilterPanel m_variableFilterPanel;
    private final JCheckBox m_variablePrefixChecker;
    private final JTextField m_variablePrefixTextField;

    // for backwards compatibility (preservation of values set in previous versions of KAP)
    private String[] m_historicConfigurationPortNames;
    private String[] m_historicConfigurationPortDescriptions;

    /**
     * @param numberOfPorts The number of out ports of this virtual out node
     */
    VirtualSubNodeOutputNodeDialogPane(final int numberOfPorts) {
        m_portCount = numberOfPorts;

        m_variableFilterPanel = new FlowVariableFilterPanel(new InputFilter<FlowVariableCell>() {
            @Override
            public boolean include(final FlowVariableCell name) {
                FlowVariable flowVariable = name.getFlowVariable();
                return !flowVariable.isGlobalConstant();
            }
        });
        m_variablePrefixTextField = new JTextField(8);
        m_variablePrefixChecker = new JCheckBox("Add prefix to all variables: ");
        m_variablePrefixChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_variablePrefixTextField.setEnabled(m_variablePrefixChecker.isSelected());
            }
        });
        m_variablePrefixChecker.doClick(); // sync
        addTab("Configuration", initLayout());
    }

    private JPanel initLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        JPanel result = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;

        gbc.gridwidth = 2;
        gbc.weighty = 0;
        result.add(new JLabel("Choose variables from workflow to be visible outside the Component"), gbc);

        gbc.gridy += 1;
        gbc.weighty = 1.0;
        result.add(m_variableFilterPanel, gbc);

//        gbc.gridwidth = 1;
//        gbc.gridy += 1;
//        gbc.gridx = 0;
//        gbc.weighty = 0;
//        result.add(m_variablePrefixChecker, gbc);
//
//        gbc.gridx += 1;
//        result.add(m_variablePrefixTextField, gbc);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final String prefix;
        if (m_variablePrefixChecker.isSelected()) {
            String text = StringUtils.trimToEmpty(m_variablePrefixTextField.getText());
            if (text.isEmpty()) {
                throw new InvalidSettingsException("Prefix string must not be empty");
            }
            prefix = text;
        } else {
            prefix = null;
        }
        final VirtualSubNodeOutputConfiguration configuration = new VirtualSubNodeOutputConfiguration(m_portCount);
        configuration.setFlowVariablePrefix(prefix);
        final FlowVariableFilterConfiguration f = AbstractVirtualSubNodeConfiguration.createFilterConfiguration();
        m_variableFilterPanel.saveConfiguration(f);
        configuration.setFilterConfiguration(f);

        final String[] portNames;
        if ((m_historicConfigurationPortNames == null) || (m_historicConfigurationPortNames.length != m_portCount)) {
            // Were we to stop populating all together, we would potentially break SubNodeContainer which
            //      relies on these values when there is no
            portNames = AbstractVirtualSubNodeConfiguration.correctedPortNames(new String[0], m_portCount);
        } else {
            portNames = m_historicConfigurationPortNames;
        }
        final String[] portDescriptions;
        if ((m_historicConfigurationPortDescriptions == null) || (m_historicConfigurationPortDescriptions.length != m_portCount)) {
            // Were we to stop populating all together, we would potentially break SubNodeContainer which
            //      relies on these values when there is no
            portDescriptions = AbstractVirtualSubNodeConfiguration.correctedPortDescriptions(new String[0], m_portCount);
        } else {
            portDescriptions = m_historicConfigurationPortDescriptions;
        }
        configuration.setPortNames(portNames);
        configuration.setPortDescriptions(portDescriptions);
        configuration.saveConfiguration(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        final VirtualSubNodeOutputConfiguration configuration = new VirtualSubNodeOutputConfiguration(m_portCount);
        final Map<String, FlowVariable> availableVariables = Node.invokeGetAvailableFlowVariables(this, Type.values());
        configuration.loadConfigurationInDialog(settings, availableVariables);
        final String prefix = configuration.getFlowVariablePrefix();
        if ((prefix != null) != m_variablePrefixChecker.isSelected()) {
            m_variablePrefixChecker.doClick();
        }
        m_variablePrefixTextField.setText(prefix == null ? "outer." : prefix);
        m_variableFilterPanel.loadConfiguration(configuration.getFilterConfiguration(), availableVariables);
        m_historicConfigurationPortNames = configuration.getPortNames();
        m_historicConfigurationPortDescriptions = configuration.getPortDescriptions();
    }
}

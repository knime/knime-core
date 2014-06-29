/*
 * ------------------------------------------------------------------------
 *
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

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.util.filter.variable.FlowVariableFilterPanel;

/**
 * Dialog to sub node output. Shows currently only a twin list to allow the user to select variables to
 * export.
 * <p>No API.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class VirtualSubNodeOutputNodeDialogPane extends NodeDialogPane {

    private final FlowVariableFilterPanel m_variableFilterPanel;
    private final JCheckBox m_variablePrefixChecker;
    private final JTextField m_variablePrefixTextField;

    private final JPanel m_portPanel = new JPanel(new GridBagLayout());
    private PortDescriptionPanel[] m_portDescriptionPanels = new PortDescriptionPanel[0];

    /** Default const.
     * @param numberOfPorts The number of in ports of this virtual in node */
    VirtualSubNodeOutputNodeDialogPane(final int numberOfPorts) {
        m_variableFilterPanel = new FlowVariableFilterPanel();
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
        addTab("Descriptions", createDescriptionsPanel());
        fillPortDescriptionPanel(numberOfPorts);
    }

    private JPanel initLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        JPanel result = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;

        gbc.gridwidth = 2;
        gbc.weighty = 0;
        result.add(new JLabel("Choose variables from workflow to be visible inside the subnode"), gbc);

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

    private JPanel createDescriptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(m_portPanel, gbc);
        return panel;
    }

    private void fillPortDescriptionPanel(final int numberOfPorts) {
        m_portDescriptionPanels = new PortDescriptionPanel[numberOfPorts];
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        for (int i = 0; i < m_portDescriptionPanels.length; i++) {
            m_portDescriptionPanels[i] = new PortDescriptionPanel(i);
            m_portPanel.add(m_portDescriptionPanels[i], gbc);
            gbc.gridy++;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        String prefix;
        if (m_variablePrefixChecker.isSelected()) {
            String text = StringUtils.trimToEmpty(m_variablePrefixTextField.getText());
            if (text.isEmpty()) {
                throw new InvalidSettingsException("Prefix string must not be empty");
            }
            prefix = text;
        } else {
            prefix = null;
        }
        VirtualSubNodeInputConfiguration configuration =
            new VirtualSubNodeInputConfiguration(m_portDescriptionPanels.length);
        configuration.setFlowVariablePrefix(prefix);
        FlowVariableFilterConfiguration f = VirtualSubNodeInputConfiguration.createFilterConfiguration();
        m_variableFilterPanel.saveConfiguration(f);
        configuration.setFilterConfiguration(f);
        String[] portNames = new String[m_portDescriptionPanels.length];
        String[] portDescriptions = new String[m_portDescriptionPanels.length];
        for (int i = 0; i < m_portDescriptionPanels.length; i++) {
            portNames[i] = m_portDescriptionPanels[i].getPortName();
            portDescriptions[i] = m_portDescriptionPanels[i].getPortDescription();
        }
        configuration.setPortNames(portNames);
        configuration.setPortDescriptions(portDescriptions);
        configuration.saveConfiguration(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        VirtualSubNodeInputConfiguration configuration =
            new VirtualSubNodeInputConfiguration(m_portDescriptionPanels.length);
        configuration.loadConfigurationInDialog(settings, getAvailableFlowVariables());
        String prefix = configuration.getFlowVariablePrefix();
        if ((prefix != null) != m_variablePrefixChecker.isSelected()) {
            m_variablePrefixChecker.doClick();
        }
        m_variablePrefixTextField.setText(prefix == null ? "outer." : prefix);
        m_variableFilterPanel.loadConfiguration(configuration.getFilterConfiguration(), getAvailableFlowVariables());
        String[] portNames = configuration.getPortNames();
        String[] portDescriptions = configuration.getPortDescriptions();
        for (int i = 0; i < m_portDescriptionPanels.length; i++) {
            String name = i < portNames.length ? portNames[i] : "";
            String description = i < portDescriptions.length ? portDescriptions[i] : "";
            m_portDescriptionPanels[i].setPortName(name);
            m_portDescriptionPanels[i].setPortDescription(description);
        }
    }

    private static class PortDescriptionPanel extends JPanel {
        private static final long serialVersionUID = -725452335646797350L;
        private final JTextField m_name = new JTextField();
        private final JTextArea m_description = new JTextArea();
        PortDescriptionPanel(final int number) {
            setBorder(new TitledBorder(new EtchedBorder(), "Port " + (number + 1)));
            m_description.setBorder(new EtchedBorder());
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy = 0;
            add(new JLabel("Name:"), gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            add(m_name, gbc);
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 0;
            gbc.weighty = 1;
            add(new JLabel("Description:"), gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            add(m_description, gbc);
        }
        String getPortName() {
            return m_name.getText();
        }
        void setPortName(final String portName) {
            m_name.setText(portName);
        }
        String getPortDescription() {
            return m_description.getText();
        }
        void setPortDescription(final String portDescription) {
            m_description.setText(portDescription);
        }
    }

}

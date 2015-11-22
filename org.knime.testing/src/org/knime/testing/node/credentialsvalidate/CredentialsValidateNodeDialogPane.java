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
 *   Mar 13, 2015 (wiswedel): created
 */
package org.knime.testing.node.credentialsvalidate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;



/**
 *
 * @author wiswedel
 */
final class CredentialsValidateNodeDialogPane extends NodeDialogPane {

    private final JComboBox<String> m_credentialsIDField;
    private final JTextField m_usernameField;
    private final JTextField m_passwordField;
    private final JCheckBox m_passwordExpectedToBeSetChecker;

    CredentialsValidateNodeDialogPane() {
        final int cols = 20;
        m_credentialsIDField = new JComboBox<>(new DefaultComboBoxModel<>());
        m_usernameField = new JTextField(cols);
        m_passwordField = new JTextField(cols);
        m_passwordExpectedToBeSetChecker = new JCheckBox("Password expected to be set");
        m_passwordExpectedToBeSetChecker.addItemListener(
            e -> m_passwordField.setEnabled(m_passwordExpectedToBeSetChecker.isSelected()));
        m_passwordExpectedToBeSetChecker.doClick();

        addTab("Main", createPanel());
    }

    private JPanel createPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        p.add(new JLabel("Credentials ID"), gbc);
        gbc.gridx += 1;
        p.add(m_credentialsIDField, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        p.add(new JLabel("Expected Username"), gbc);
        gbc.gridx += 1;
        p.add(m_usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        p.add(m_passwordExpectedToBeSetChecker, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy += 1;
        p.add(new JLabel("Expected Password"), gbc);
        gbc.gridx += 1;
        p.add(m_passwordField, gbc);

        return p;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        CredentialsValidateNodeConfiguration c = new CredentialsValidateNodeConfiguration();

        c.setCredentialsID((String)m_credentialsIDField.getSelectedItem());
        c.setUsername(m_usernameField.getText());
        c.setPassword(m_passwordExpectedToBeSetChecker.isSelected() ? m_passwordField.getText() : null);
        c.setPasswordExpectedToBeSet(m_passwordExpectedToBeSetChecker.isSelected());

        c.saveSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
                throws NotConfigurableException {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>)m_credentialsIDField.getModel();
        model.removeAllElements();
        getCredentialsNames().stream().forEach(c -> model.addElement(c));

        CredentialsValidateNodeConfiguration configuration = new CredentialsValidateNodeConfiguration();
        configuration.loadSettingsInDialog(settings);

        int index = model.getIndexOf(configuration.getCredId());
        if (index >= 0) {
            model.setSelectedItem(configuration.getCredId());
        }
        m_usernameField.setText(configuration.getUsername());
        m_passwordField.setText(configuration.getPassword());
        if (m_passwordExpectedToBeSetChecker.isSelected() != configuration.isPasswordExpectedToBeSet()) {
            m_passwordExpectedToBeSetChecker.doClick(); // triggers event
        }
    }

}

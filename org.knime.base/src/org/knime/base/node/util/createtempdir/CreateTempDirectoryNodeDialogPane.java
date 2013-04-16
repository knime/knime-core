/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.util.createtempdir;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.knime.base.node.util.createtempdir.CreateTempDirectoryConfiguration.VarNameFileNamePair;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.KeyValuePanel;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
class CreateTempDirectoryNodeDialogPane extends NodeDialogPane {

    private final JTextField m_baseNameField;
    private final JTextField m_variableName;
    private final JCheckBox m_deleteOnResetChecker;
    private final KeyValuePanel m_flowPairPanel;

    /**
     *
     */
    public CreateTempDirectoryNodeDialogPane() {
        int fieldLength = 20;
        m_baseNameField = new JTextField(fieldLength);
        m_variableName = new JTextField(fieldLength);
        m_deleteOnResetChecker = new JCheckBox("Delete directory on reset");
        m_flowPairPanel = new KeyValuePanel();
        m_flowPairPanel.setKeyColumnLabel("Variable");
        m_flowPairPanel.setValueColumnLabel("File");
        addTab("Configuration", initLayout());
    }

    private JPanel initLayout() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        p.add(new JLabel("Directory base name"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1;
        p.add(m_baseNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0;
        p.add(new JLabel("Export path as (variable name)"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1;
        p.add(m_variableName, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        p.add(m_deleteOnResetChecker, gbc);

        gbc.gridy += 1;
        p.add(new JSeparator(), gbc);

        gbc.gridy += 1;
        
        p.add(new JLabel("Additional path variables"), gbc);

        gbc.gridy += 1;
        gbc.weighty = 1;
        m_flowPairPanel.getTable().setPreferredScrollableViewportSize(null);
        p.add(m_flowPairPanel, gbc);
        return p;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        CreateTempDirectoryConfiguration c =
            new CreateTempDirectoryConfiguration();
        c.loadInDialog(settings);
        m_baseNameField.setText(c.getBaseName());
        m_variableName.setText(c.getVariableName());
        m_deleteOnResetChecker.setSelected(c.isDeleteOnReset());
        VarNameFileNamePair[] pairs = c.getPairs();
        String[] varNames = new String[pairs.length];
        String[] fileNames = new String[pairs.length];
        for (int i = 0; i < pairs.length; i++) {
            varNames[i] = pairs[i].getVariableName();
            fileNames[i] = pairs[i].getFileName();
        }
        m_flowPairPanel.setTableData(varNames, fileNames);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        CreateTempDirectoryConfiguration c =
            new CreateTempDirectoryConfiguration();
        c.setBaseName(m_baseNameField.getText());
        c.setVariableName(m_variableName.getText());
        c.setDeleteOnReset(m_deleteOnResetChecker.isSelected());
        String[] varNames = m_flowPairPanel.getKeys();
        String[] fileNames = m_flowPairPanel.getValues();
        VarNameFileNamePair[] pairs = new VarNameFileNamePair[varNames.length];
        for (int i = 0; i < varNames.length; i++) {
            pairs[i] = new VarNameFileNamePair(varNames[i], fileNames[i]);
        }
        c.setPairs(pairs);
        c.save(settings);
    }

}

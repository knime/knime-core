/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * History
 *   Apr 16, 2011 (wiswedel): created
 */
package org.knime.base.node.io.linereader;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Dialog to Line Reader node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class LineReaderNodeDialogPane extends NodeDialogPane {

    private final FilesHistoryPanel m_filePanel;
    private final FlowVariableModelButton m_filePanelVarButton;
    private final JTextField m_columnHeaderField;
    private final JTextField m_rowHeadPrefixField;
    private final JCheckBox m_limitRowCountChecker;
    private final JCheckBox m_skipEmptyLinesChecker;
    private final JSpinner m_limitRowCountSpinner;

    /** Create new dialog, init layout. */
    LineReaderNodeDialogPane() {
        m_filePanel = new FilesHistoryPanel("line_read");
        FlowVariableModel varModel = createFlowVariableModel(
                LineReaderConfig.CFG_URL, FlowVariable.Type.STRING);
        m_filePanelVarButton = new FlowVariableModelButton(varModel);
        varModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                FlowVariableModel wvm = (FlowVariableModel)(e.getSource());
                m_filePanel.setEnabled(!wvm.isVariableReplacementEnabled());
            }
        });
        int col = 10;
        m_columnHeaderField = new JTextField("Column", col);
        m_rowHeadPrefixField = new JTextField("Row", col);
        m_skipEmptyLinesChecker = new JCheckBox("Skip empty lines");
        m_limitRowCountSpinner = new JSpinner(
                new SpinnerNumberModel(1000, 0, Integer.MAX_VALUE, 100));
        m_limitRowCountChecker = new JCheckBox("Limit number of rows");
        m_limitRowCountChecker.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean selected = m_limitRowCountChecker.isSelected();
                m_limitRowCountSpinner.setEnabled(selected);
            }
        });
        m_limitRowCountSpinner.setEnabled(false);
        JPanel panel = initLayout();
        addTab("Line Reader", panel);
    }

    private JPanel initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridwidth = 2;
        JPanel filePanel = new JPanel(new FlowLayout());
        filePanel.add(m_filePanel);
        filePanel.add(m_filePanelVarButton);
        panel.add(filePanel, gbc);

        gbc.gridy += 1;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Row Header Prefix "), gbc);
        gbc.gridx += 1;
        panel.add(m_rowHeadPrefixField, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Column Header "), gbc);
        gbc.gridx += 1;
        panel.add(m_columnHeaderField, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(m_skipEmptyLinesChecker, gbc);

        gbc.gridy += 1;
        gbc.gridwidth = 1;
        panel.add(m_limitRowCountChecker, gbc);
        gbc.gridx += 1;
        panel.add(m_limitRowCountSpinner, gbc);
        return panel;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        LineReaderConfig config = new LineReaderConfig();
        config.loadConfigurationInDialog(settings);
        String url = config.getUrlString();
        m_filePanel.updateHistory();
        m_filePanel.setSelectedFile(url);
        m_columnHeaderField.setText(config.getColumnHeader());
        m_rowHeadPrefixField.setText(config.getRowPrefix());
        m_skipEmptyLinesChecker.setSelected(config.isSkipEmptyLines());
        int limitRows = config.getLimitRowCount();
        if (limitRows < 0) { // no row limit
            if (m_limitRowCountChecker.isSelected()) {
                m_limitRowCountChecker.doClick(); // unselect with event
            }
            m_limitRowCountSpinner.setValue(1000);
        } else {
            if (!m_limitRowCountChecker.isSelected()) {
                m_limitRowCountChecker.doClick(); // select with event
            }
            m_limitRowCountSpinner.setValue(limitRows);
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        LineReaderConfig config = new LineReaderConfig();
        String fileS = m_filePanel.getSelectedFile();
        if (fileS == null) {
            throw new InvalidSettingsException("No input selected");
        }
        config.setUrlString(fileS);
        config.setColumnHeader(m_columnHeaderField.getText());
        config.setRowPrefix(m_rowHeadPrefixField.getText());
        config.setSkipEmptyLines(m_skipEmptyLinesChecker.isSelected());
        if (m_limitRowCountChecker.isSelected()) {
            config.setLimitRowCount((Integer)m_limitRowCountSpinner.getValue());
        } else {
            config.setLimitRowCount(-1);
        }
        config.saveConfiguration(settings);
    }

}

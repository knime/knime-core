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
 *
 * History
 *   Apr 16, 2011 (wiswedel): created
 */
package org.knime.base.node.io.linereader;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.util.StringHistoryPanel;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Dialog to Line Reader node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class LineReaderNodeDialogPane extends NodeDialogPane {

    private final FilesHistoryPanel m_filePanel;
    private final JTextField m_columnHeaderField;
    private final JTextField m_rowHeadPrefixField;
    private final JCheckBox m_limitRowCountChecker;
    private final JCheckBox m_skipEmptyLinesChecker;
    private final JSpinner m_limitRowCountSpinner;
    private final StringHistoryPanel m_regexField;
    private final JCheckBox m_regexChecker;

    /** Create new dialog, init layout. */
    LineReaderNodeDialogPane() {
        m_filePanel = new FilesHistoryPanel(createFlowVariableModel(LineReaderConfig.CFG_URL, FlowVariable.Type.STRING),
            "line_read", LocationValidation.FileInput, "");
        m_filePanel.setDialogType(JFileChooser.OPEN_DIALOG);

        int col = 10;
        m_columnHeaderField = new JTextField("Column", col);
        m_rowHeadPrefixField = new JTextField("Row", col);
        m_skipEmptyLinesChecker = new JCheckBox("Skip empty lines");
        m_regexField = new StringHistoryPanel("org.knime.base.node.io.linereader.RegexHistory");
        //set the size of the ComboBox to 42
        m_regexField.setPrototypeDisplayValue("123456789012345678901234567890123456789012");
        m_regexChecker = new JCheckBox("Match input against regex");
        m_regexChecker.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean selected = m_regexChecker.isSelected();
                m_regexField.setEnabled(selected);
            }
        });
        m_regexField.setEnabled(false);
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

        addTab("Settings", initLayout());
    }

    private JPanel initLayout() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Input location:"));
        filePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, m_filePanel.getPreferredSize().height));
        filePanel.add(m_filePanel);
        filePanel.add(Box.createHorizontalGlue());


        JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
            .createEtchedBorder(), "Reader options:"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy += 1;
        gbc.gridwidth = 1;
        optionsPanel.add(new JLabel("Row Header Prefix "), gbc);
        gbc.gridx += 1;
        optionsPanel.add(m_rowHeadPrefixField, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        optionsPanel.add(new JLabel("Column Header "), gbc);
        gbc.gridx += 1;
        optionsPanel.add(m_columnHeaderField, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        optionsPanel.add(m_skipEmptyLinesChecker, gbc);

        gbc.gridy += 1;
        gbc.gridwidth = 1;
        optionsPanel.add(m_limitRowCountChecker, gbc);
        gbc.gridx += 1;
        optionsPanel.add(m_limitRowCountSpinner, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        optionsPanel.add(m_regexChecker, gbc);
        gbc.gridx += 1;
        optionsPanel.add(m_regexField, gbc);

        //empty panel to eat up extra space
        gbc.gridx++;
        gbc.gridy++;
        gbc.weightx = 1;
        gbc.weighty = 1;
        optionsPanel.add(new JPanel(), gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(filePanel);
        panel.add(optionsPanel);

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
        if (!"".equals(config.getRegex())) {
            if (!m_regexChecker.isSelected()) {
                m_regexChecker.doClick();
            }
            m_regexField.updateHistory();
            m_regexField.setSelectedString(config.getRegex());
        } else {
            if (m_regexChecker.isSelected()) {
                m_regexChecker.doClick();
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        LineReaderConfig config = new LineReaderConfig();
        String fileS = m_filePanel.getSelectedFile().trim();
        config.setUrlString(fileS);
        config.setColumnHeader(m_columnHeaderField.getText());
        config.setRowPrefix(m_rowHeadPrefixField.getText());
        config.setSkipEmptyLines(m_skipEmptyLinesChecker.isSelected());
        if (m_limitRowCountChecker.isSelected()) {
            config.setLimitRowCount((Integer)m_limitRowCountSpinner.getValue());
        } else {
            config.setLimitRowCount(-1);
        }
        if(m_regexChecker.isSelected()){
            if(m_regexField.getSelectedString().equals("")){
                throw new InvalidSettingsException("Empty Regex!");
            }
            config.setRegex(m_regexField.getSelectedString());
            m_regexField.commitSelectedToHistory();
        }else{
            config.setRegex("");
        }
        config.saveConfiguration(settings);
        m_filePanel.addToHistory();
    }

}

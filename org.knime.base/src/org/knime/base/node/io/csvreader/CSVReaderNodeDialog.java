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
 *   Jan 3, 2010 (wiswedel): created
 */
package org.knime.base.node.io.csvreader;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.io.filereader.CharsetNamePanel;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileReaderSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Dialog to CSV Reader node.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
// this class is also used in the wide data extension. Once it's moved to the base bundle the scope if this class
// should be limited again.
public final class CSVReaderNodeDialog extends NodeDialogPane {

    private final FilesHistoryPanel m_filePanel;
    private final JTextField m_colDelimiterField;
    private final JTextField m_rowDelimiterField;
    private final JTextField m_quoteStringField;
    private final JTextField m_commentStartField;
    private final JCheckBox m_hasRowHeaderChecker;
    private final JCheckBox m_hasColHeaderChecker;
    private final JCheckBox m_supportShortLinesChecker;
    private final JCheckBox m_limitRowsChecker;
    private final JSpinner m_limitRowsSpinner;
    private final JCheckBox m_skipFirstLinesChecker;
    private final JSpinner m_skipFirstLinesSpinner;
    private final CharsetNamePanel m_encodingPanel;


    /** Create new dialog, init layout.*/
    public CSVReaderNodeDialog() {
        m_filePanel =
            new FilesHistoryPanel(createFlowVariableModel(CSVReaderConfig.CFG_URL, FlowVariable.Type.STRING),
                "csv_read", LocationValidation.FileInput, ".csv", ".txt");
        m_filePanel.setDialogType(JFileChooser.OPEN_DIALOG);

        int col = 3;
        m_colDelimiterField = new JTextField("###", col);
        m_rowDelimiterField = new JTextField("###", col);
        m_quoteStringField = new JTextField("###", col);
        m_commentStartField = new JTextField("###", col);
        m_hasRowHeaderChecker = new JCheckBox("Has Row Header");
        m_hasColHeaderChecker = new JCheckBox("Has Column Header");
        m_supportShortLinesChecker = new JCheckBox("Support Short Lines");
        m_skipFirstLinesChecker = new JCheckBox("Skip first lines ");
        m_skipFirstLinesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        m_skipFirstLinesChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_skipFirstLinesSpinner.setEnabled(m_skipFirstLinesChecker.isSelected());
            }
        });
        m_skipFirstLinesChecker.doClick();
        m_limitRowsChecker = new JCheckBox("Limit rows ");
        m_limitRowsSpinner = new JSpinner(new SpinnerNumberModel(50, 0, Integer.MAX_VALUE, 50));
        m_limitRowsChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_limitRowsSpinner.setEnabled(m_limitRowsChecker.isSelected());
            }
        });
        m_limitRowsChecker.doClick();

        addTab("Settings", initLayout());

        m_encodingPanel = new CharsetNamePanel(new FileReaderSettings());
        addTab("Encoding", m_encodingPanel);
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
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        optionsPanel.add(getInFlowLayout(m_colDelimiterField, new JLabel("Column Delimiter ")), gbc);
        gbc.gridx += 1;
        optionsPanel.add(getInFlowLayout(m_rowDelimiterField, new JLabel("Row Delimiter ")), gbc);
        gbc.gridx +=1;
        gbc.weightx = 1;
        optionsPanel.add(new JPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0;
        optionsPanel.add(getInFlowLayout(m_quoteStringField, new JLabel("Quote Char ")), gbc);
        gbc.gridx += 1;
        optionsPanel.add(getInFlowLayout(m_commentStartField, new JLabel("Comment Char ")), gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        optionsPanel.add(getInFlowLayout(m_hasColHeaderChecker), gbc);
        gbc.gridx += 1;
        optionsPanel.add(getInFlowLayout(m_hasRowHeaderChecker), gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        optionsPanel.add(getInFlowLayout(m_supportShortLinesChecker), gbc);

        gbc.gridy += 1;
        optionsPanel.add(getInFlowLayout(m_skipFirstLinesChecker), gbc);
        gbc.gridx += 1;
        optionsPanel.add(getInFlowLayout(m_skipFirstLinesSpinner), gbc);
        gbc.gridy += 1;
        gbc.gridx = 0;
        optionsPanel.add(getInFlowLayout(m_limitRowsChecker), gbc);
        gbc.gridx += 1;
        optionsPanel.add(getInFlowLayout(m_limitRowsSpinner), gbc);

        //empty panel to eat up extra space
        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weighty = 1;
        optionsPanel.add(new JPanel(), gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(filePanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(optionsPanel);

        return panel;
    }

    private static JPanel getInFlowLayout(final JComponent... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : comps) {
            p.add(c);
        }
        return p;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        CSVReaderConfig config = new CSVReaderConfig();
        config.loadSettingsInDialog(settings);
        m_filePanel.updateHistory();
        m_filePanel.setSelectedFile(config.getLocation());
        m_colDelimiterField.setText(escape(config.getColDelimiter()));
        m_rowDelimiterField.setText(escape(config.getRowDelimiter()));
        m_quoteStringField.setText(config.getQuoteString());
        m_commentStartField.setText(config.getCommentStart());
        m_hasColHeaderChecker.setSelected(config.hasColHeader());
        m_hasRowHeaderChecker.setSelected(config.hasRowHeader());
        m_supportShortLinesChecker.setSelected(config.isSupportShortLines());
        int skipFirstLinesCount = config.getSkipFirstLinesCount();
        if (skipFirstLinesCount > 0) {
            m_skipFirstLinesChecker.setSelected(true);
            m_skipFirstLinesSpinner.setValue(skipFirstLinesCount);
        } else {
            m_skipFirstLinesChecker.setSelected(false);
            m_skipFirstLinesSpinner.setValue(1);
        }
        long limitRowsCount = config.getLimitRowsCount();
        if (limitRowsCount >= 0) { // 0 is allowed -- will only read header
            m_limitRowsChecker.setSelected(true);
            m_limitRowsSpinner.setValue(limitRowsCount);
        } else {
            m_limitRowsChecker.setSelected(false);
            m_limitRowsSpinner.setValue(50);
        }
        m_encodingPanel.loadSettings(getEncodingSettings(config));
    }

    private FileReaderSettings getEncodingSettings(final CSVReaderConfig settings) {
        FileReaderSettings s = new FileReaderSettings();
        s.setCharsetName(settings.getCharSetName());
        return s;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        CSVReaderConfig config = new CSVReaderConfig();
        String fileS = m_filePanel.getSelectedFile().trim();
        config.setLocation(fileS);
        config.setColDelimiter(unescape(m_colDelimiterField.getText()));
        config.setRowDelimiter(unescape(m_rowDelimiterField.getText()));
        config.setQuoteString(m_quoteStringField.getText());
        config.setCommentStart(m_commentStartField.getText());
        config.setHasRowHeader(m_hasRowHeaderChecker.isSelected());
        config.setHasColHeader(m_hasColHeaderChecker.isSelected());
        config.setSupportShortLines(m_supportShortLinesChecker.isSelected());
        int skiptFirstLines = (Integer)(m_skipFirstLinesChecker.isSelected() ? m_skipFirstLinesSpinner.getValue() : -1);
        config.setSkipFirstLinesCount(skiptFirstLines);
        int limitRows = (Integer)(m_limitRowsChecker.isSelected() ? m_limitRowsSpinner.getValue() : -1);
        config.setLimitRowsCount(limitRows);
        FileReaderNodeSettings s = new FileReaderNodeSettings();
        m_encodingPanel.overrideSettings(s);
        config.setCharSetName(s.getCharsetName());

        config.saveSettingsTo(settings);
        m_filePanel.addToHistory();
    }

    private static String unescape(final String s) {
        if ("\\t".equals(s)) {
            return "\t";
        } else if ("\\n".equals(s)) {
            return "\n";
        } else if ("\\r".equals(s)) {
            return "\r";
        } else if ("\\r\\n".equals(s)) {
            return "\r\n";
        } else {
            return s;
        }
    }

    private static String escape(final String s) {
        if ("\t".equals(s)) {
            return "\\t";
        } else if ("\n".equals(s)) {
            return "\\n";
        } else if ("\r".equals(s)) {
            return "\\r";
        } else if ("\r\n".equals(s)) {
            return "\\r\\n";
        } else {
            return s;
        }
    }

}

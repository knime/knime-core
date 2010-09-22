/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   Jan 3, 2010 (wiswedel): created
 */
package org.knime.base.node.io.csvreader;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
 * Dialog to CSV Reader node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class CSVReaderNodeDialog extends NodeDialogPane {

    private final FilesHistoryPanel m_filePanel;
    private final JTextField m_colDelimiterField;
    private final JTextField m_rowDelimiterField;
    private final JTextField m_quoteStringField;
    private final JTextField m_commentStartField;
    private final JCheckBox m_hasRowHeaderChecker;
    private final JCheckBox m_hasColHeaderChecker;

    /** Create new dialog, init layout. */
    CSVReaderNodeDialog() {
        JPanel panel = new JPanel(new BorderLayout());
        m_filePanel = new FilesHistoryPanel("csv_read");
        FlowVariableModel varModel = createFlowVariableModel(
                CSVReaderConfig.CFG_URL, FlowVariable.Type.STRING);
        FlowVariableModelButton button = new FlowVariableModelButton(varModel);
        varModel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                FlowVariableModel wvm = (FlowVariableModel)(e.getSource());
                m_filePanel.setEnabled(!wvm.isVariableReplacementEnabled());
            }
        });
        int col = 3;
        m_colDelimiterField = new JTextField("###", col);
        m_rowDelimiterField = new JTextField("###", col);
        m_quoteStringField = new JTextField("###", col);
        m_commentStartField = new JTextField("###", col);
        m_hasRowHeaderChecker = new JCheckBox("Has Row Header");
        m_hasColHeaderChecker = new JCheckBox("Has Column Header");
        panel.add(getInFlowLayout(m_filePanel, button), BorderLayout.NORTH);
        JPanel centerPanel = new JPanel(new GridLayout(0, 2));
        centerPanel.add(getInFlowLayout(m_colDelimiterField,
                new JLabel("Column Delimiter ")));
        centerPanel.add(getInFlowLayout(m_rowDelimiterField,
                new JLabel("Row Delimiter ")));
        centerPanel.add(getInFlowLayout(m_quoteStringField,
                new JLabel("Quote Char ")));
        centerPanel.add(getInFlowLayout(m_commentStartField,
                new JLabel("Comment Char ")));
        centerPanel.add(getInFlowLayout(m_hasColHeaderChecker));
        centerPanel.add(getInFlowLayout(m_hasRowHeaderChecker));
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(new JLabel("   "), BorderLayout.WEST);
        addTab("CSV Reader", panel);
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
        URL url = config.getUrl();
        String urlS;
        if (url != null) {
            if ("file".equals(url.getProtocol())) {
                try {
                    urlS = new File(url.toURI()).getAbsolutePath();
                } catch (URISyntaxException e) {
                    urlS = url.toString();
                }
            } else {
                urlS = url.toString();
            }
        } else {
            urlS = "";
        }
        m_filePanel.updateHistory();
        m_filePanel.setSelectedFile(urlS);
        m_colDelimiterField.setText(escape(config.getColDelimiter()));
        m_rowDelimiterField.setText(escape(config.getRowDelimiter()));
        m_quoteStringField.setText(config.getQuoteString());
        m_commentStartField.setText(config.getCommentStart());
        m_hasColHeaderChecker.setSelected(config.hasColHeader());
        m_hasRowHeaderChecker.setSelected(config.hasRowHeader());
    }


    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        CSVReaderConfig config = new CSVReaderConfig();
        String fileS = m_filePanel.getSelectedFile();
        if (fileS == null) {
            throw new InvalidSettingsException("No input selected");
        }
        URL url;
        try {
            if (!fileS.matches("^[a-z]*:/")) {
                File f = new File(fileS);
                if (!f.exists()) {
                    throw new InvalidSettingsException(
                            "No such file: " + fileS);
                }
                url = f.toURI().toURL();
            } else {
                url = new URL(fileS);
            }
        } catch (MalformedURLException e) {
            throw new InvalidSettingsException(
                    "Unable to set URL: " + e.getMessage(), e);
        }
        config.setUrl(url);
        config.setColDelimiter(unescape(m_colDelimiterField.getText()));
        config.setRowDelimiter(unescape(m_rowDelimiterField.getText()));
        config.setQuoteString(m_quoteStringField.getText());
        config.setCommentStart(m_commentStartField.getText());
        config.setHasRowHeader(m_hasRowHeaderChecker.isSelected());
        config.setHasColHeader(m_hasColHeaderChecker.isSelected());
        config.saveSettingsTo(settings);
    }

    private static String unescape(final String s) {
        if ("\\t".equals(s)) {
            return "\t";
        } else if ("\\n".equals(s)) {
            return "\n";
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
        } else if ("\r\n".equals(s)) {
            return "\\r\\n";
        } else {
            return s;
        }
    }

}

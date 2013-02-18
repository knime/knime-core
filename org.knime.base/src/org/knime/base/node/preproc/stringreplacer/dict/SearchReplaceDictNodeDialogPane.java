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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   Jun 16, 2007 (wiswedel): created
 */
package org.knime.base.node.preproc.stringreplacer.dict;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * Dialog for Search & Replace (Dictionary) node. Contains a column
 * selection panel for target column selection, a file chooser panel to
 * select the dictionary location, and a checkbox + textfield for the new
 * column (if any).
 * @author Bernd Wiswedel, University of Konstanz
 */
final class SearchReplaceDictNodeDialogPane extends NodeDialogPane {

    private final ColumnSelectionComboxBox m_targetColBox;
    private final FilesHistoryPanel m_fileChooserPanel;
    private final JCheckBox m_appendChecker;
    private final JTextField m_delimiterField;
    private final JTextField m_appendTextField;

    /** Inits GUI. */
    @SuppressWarnings("unchecked")
    public SearchReplaceDictNodeDialogPane() {
        m_targetColBox =
            new ColumnSelectionComboxBox((Border)null, StringValue.class);
        m_fileChooserPanel = new FilesHistoryPanel("string_replace_dict");
        m_appendChecker = new JCheckBox("Append new column");
        m_appendChecker.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_appendTextField.setEnabled(m_appendChecker.isSelected());
            }
        });
        m_delimiterField = new JTextField(2);
        m_delimiterField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                m_delimiterField.selectAll();
            }
        });
        m_delimiterField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(final KeyEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String text = m_delimiterField.getText();
                        String newText;
                        if (text.length() > 1) {
                            if (text.charAt(0) == '\\') {
                                switch (text.charAt(1)) {
                                case '\\':
                                case 't':
                                    newText = text.substring(0, 2);
                                    break;
                                default:
                                    newText = "\\\\";
                                }
                            } else {
                                newText = Character.toString(text.charAt(0));
                            }
                        } else {
                            newText = text;
                        }
                        if (!newText.equals(text)) {
                            m_delimiterField.setText(newText);
                        }
                        if ((newText.length() == 1 && !newText.startsWith("\\"))
                                || newText.length() > 1) {
                            m_delimiterField.selectAll();
                        }
                    }
                });
            }
        });
        m_appendTextField = new JTextField();
        layout();
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        String targetCol = settings.getString(
                SearchReplaceDictNodeModel.CFG_TARGET_COLUMN, null);
        String dictLocation = settings.getString(
                SearchReplaceDictNodeModel.CFG_DICT_LOCATION, null);
        char delimChar = settings.getChar(
                SearchReplaceDictNodeModel.CFG_DELIMITER_IN_DICT, ',');
        String appendCol = settings.getString(
                SearchReplaceDictNodeModel.CFG_APPEND_COLUMN, null);
        m_targetColBox.update(specs[0], targetCol);
        if (dictLocation != null) {
            m_fileChooserPanel.addToHistory(new File(dictLocation));
        }
        m_fileChooserPanel.updateHistory();
        m_fileChooserPanel.setSelectedFile(dictLocation);
        String delimString;
        switch (delimChar) {
        case '\t':
            delimString = "\\t";
            break;
        case '\\':
            delimString = "\\\\";
            break;
        default:
            delimString = Character.toString(delimChar);
        }
        m_delimiterField.setText(delimString);
        if ((appendCol != null) != m_appendChecker.isSelected()) {
            m_appendChecker.doClick();
        }
        if (appendCol != null) {
            m_appendTextField.setText(appendCol);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
        throws InvalidSettingsException {
        String targetCol = m_targetColBox.getSelectedColumn();
        String dictLoc = m_fileChooserPanel.getSelectedFile();
        String delimString = m_delimiterField.getText();
        if (delimString.length() < 1) {
            throw new InvalidSettingsException("No delimiter has been set.");
        }
        if (delimString.equals(Character.toString('\\'))) {
            throw new InvalidSettingsException("Invalid character expression "
                    + "'\\', enter '\\\\' to get a single backslash");
        }
        char delim;
        if (delimString.charAt(0) == '\\') {
            switch (delimString.charAt(1)) {
            case '\\':
                delim = '\\';
                break;
            case 't':
                delim = '\t';
                break;
            default:
                throw new InvalidSettingsException(
                        "\"" + delimString + "\" is not a valid delimiter, "
                        + "use any single character or \"\\t\" for a tab "
                        + "character or \"\\\\\" for a single backslash.");
            }
        } else {
            delim = delimString.charAt(0);
        }
        String appendCol = m_appendChecker.isSelected()
            ? m_appendTextField.getText() : null;
        settings.addString(
                SearchReplaceDictNodeModel.CFG_TARGET_COLUMN, targetCol);
        settings.addString(
                SearchReplaceDictNodeModel.CFG_DICT_LOCATION, dictLoc);
        settings.addString(
                SearchReplaceDictNodeModel.CFG_APPEND_COLUMN, appendCol);
        settings.addChar(
                SearchReplaceDictNodeModel.CFG_DELIMITER_IN_DICT, delim);
    }

    /** Layout the GUI elements with GridBagLayout. */
    private void layout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.gridy = 0;
        g.anchor = GridBagConstraints.EAST;

        g.gridx = 0;
        panel.add(new JLabel("Target Column "), g);

        g.gridy++;
        panel.add(new JLabel("Dictionary Location "), g);

        g.gridy++;
        panel.add(new JLabel("Delimiter in Dictionary "), g);

        g.gridy++;
        panel.add(m_appendChecker, g);

        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;
        g.gridx++;
        panel.add(m_targetColBox, g);

        g.gridy++;
        panel.add(m_fileChooserPanel, g);

        g.fill = GridBagConstraints.NONE;
        g.weightx = 0.0;
        g.gridy++;
        panel.add(m_delimiterField, g);

        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy++;
        panel.add(m_appendTextField, g);

        addTab("Default", panel);

    }

}

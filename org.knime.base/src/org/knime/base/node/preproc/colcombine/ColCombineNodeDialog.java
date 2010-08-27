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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 */
package org.knime.base.node.preproc.colcombine;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.text.DefaultFormatter;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * <code>NodeDialog</code> for the "ColCombine" Node.
 * It shows column filter panel to select a set of columns, a text field
 * in which to enter the delimiter string, a text field for the new (to 
 * be appended) column and 
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ColCombineNodeDialog extends NodeDialogPane {
    
    private final ColumnFilterPanel m_colFilterPanel;
    private final JTextField m_appendColNameField;
    private final JTextField m_delimTextField;
    private final JFormattedTextField m_quoteCharField;
    private final JCheckBox m_quoteAlwaysChecker;
    private final JTextField m_replaceDelimStringField;
    private final JRadioButton m_quoteCharRadioButton;
    private final JRadioButton m_replaceDelimRadioButton;

    /** Inits GUI. */
    public ColCombineNodeDialog() {
        m_colFilterPanel = new ColumnFilterPanel();
        m_appendColNameField = new JTextField(12);
        int defaultTextFieldWidth = 2;
        m_delimTextField = new JTextField(defaultTextFieldWidth);
        m_quoteCharField = new JFormattedTextField(new DefaultFormatter());
        m_quoteCharField.setColumns(defaultTextFieldWidth);
        m_quoteCharField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(final KeyEvent e) {
                String t = m_quoteCharField.getText();
                if (t.length() > 1) {
                    m_quoteCharField.setText(Character.toString(t.charAt(0)));
                }
                m_quoteCharField.selectAll();
            }
        });
        m_quoteAlwaysChecker = new JCheckBox("Quote always");
        m_quoteAlwaysChecker.setToolTipText("If checked, the output is " 
                + "always quoted (surrounded by quote char), if unchecked, "
                + "the quote character is only added when necessary");
        m_replaceDelimStringField = new JTextField(defaultTextFieldWidth);
        ButtonGroup butGroup = new ButtonGroup();
        m_quoteCharRadioButton = new JRadioButton("Quote Character ");
        m_replaceDelimRadioButton = 
            new JRadioButton("Replace Delimiter by ");
        butGroup.add(m_quoteCharRadioButton);
        butGroup.add(m_replaceDelimRadioButton);
        ActionListener radioButtonListener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                boolean isQuote = m_quoteCharRadioButton.isSelected();
                m_quoteCharField.setEnabled(isQuote);
                m_quoteAlwaysChecker.setEnabled(isQuote);
                m_replaceDelimStringField.setEnabled(!isQuote);
            }
        };
        m_quoteCharRadioButton.addActionListener(radioButtonListener);
        m_replaceDelimRadioButton.addActionListener(radioButtonListener);
        m_quoteCharRadioButton.doClick();
        initLayout();
    }
    
    private void initLayout() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gdb = new GridBagConstraints();
        gdb.insets = new Insets(5, 5, 5, 5);
        gdb.anchor = GridBagConstraints.NORTHWEST;
        
        gdb.gridy = 0;
        gdb.gridx = 0;
        gdb.gridwidth = 1;
        gdb.weightx = 0.0;
        gdb.weighty = 0.0;
        gdb.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Delimiter"), gdb);
        gdb.gridx++;
        p.add(m_delimTextField, gdb);
        
        gdb.gridy++;
        gdb.gridx = 0;
        p.add(m_quoteCharRadioButton, gdb);
        gdb.gridx++;
        p.add(m_quoteCharField, gdb);
        gdb.gridx++;
        p.add(m_quoteAlwaysChecker, gdb);
        
        gdb.gridy++;
        gdb.gridx = 0;
        p.add(m_replaceDelimRadioButton, gdb);
        gdb.gridx++;
        p.add(m_replaceDelimStringField, gdb);
        
        gdb.gridy++;
        gdb.gridx = 0;
        p.add(new JLabel("Name of appended column"), gdb);
        gdb.gridx++;
        gdb.gridwidth = 2;
        p.add(m_appendColNameField, gdb);

        gdb.gridy++;
        gdb.gridx = 0; 
        gdb.weightx = 1.0;
        gdb.weighty = 1.0;
        gdb.gridwidth = 3;
        p.add(m_colFilterPanel, gdb);
        
        addTab("Settings", p);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, 
            final DataTableSpec[] specs) throws NotConfigurableException {
        DataTableSpec in = specs[0];
        if (in.getNumColumns() == 0) {
            throw new NotConfigurableException("No input data available");
        }
        List<String> defIncludes = new ArrayList<String>();
        for (DataColumnSpec s : in) {
            if (s.getType().isCompatible(StringValue.class)) {
                defIncludes.add(s.getName());
            }
        }
        String[] includes = settings.getStringArray(
                ColCombineNodeModel.CFG_COLUMNS, 
                defIncludes.toArray(new String[defIncludes.size()]));
        String delimiter = settings.getString(
                ColCombineNodeModel.CFG_DELIMITER_STRING, ",");
        boolean isQuoting = settings.getBoolean(
                ColCombineNodeModel.CFG_IS_QUOTING, true);
        char quote = settings.getChar(ColCombineNodeModel.CFG_QUOTE_CHAR, '"');
        String replaceDelim = "";
        if (!isQuoting) {
            replaceDelim  = settings.getString(
                    ColCombineNodeModel.CFG_REPLACE_DELIMITER_STRING, 
                    replaceDelim);
        }
        String newColBase = "combined string";
        String newColName = newColBase;
        for (int i = 1; in.containsName(newColName); i++) {
            newColName = newColBase + " #" + i;
        }
        newColName = settings.getString(
                ColCombineNodeModel.CFG_NEW_COLUMN_NAME, newColName);
        m_colFilterPanel.update(in, false, includes);
        m_delimTextField.setText(delimiter);
        m_appendColNameField.setText(newColName);
        if (isQuoting) {
            m_quoteCharRadioButton.doClick();
            m_quoteCharField.setValue(Character.valueOf(quote));
            boolean isQuotingAlways = settings.getBoolean(
                    ColCombineNodeModel.CFG_IS_QUOTING_ALWAYS, true);
            m_quoteAlwaysChecker.setSelected(isQuotingAlways);
        } else {
            m_replaceDelimRadioButton.doClick();
            m_replaceDelimStringField.setText(replaceDelim);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(
            final NodeSettingsWO settings) throws InvalidSettingsException {
        Set<String> included = m_colFilterPanel.getIncludedColumnSet();
        String[] columns = included.toArray(new String[included.size()]);
        settings.addStringArray(ColCombineNodeModel.CFG_COLUMNS, columns);
        String delimiter = m_delimTextField.getText();
        settings.addString(ColCombineNodeModel.CFG_DELIMITER_STRING, delimiter);
        String newColName = m_appendColNameField.getText();
        settings.addString(ColCombineNodeModel.CFG_NEW_COLUMN_NAME, newColName);
        boolean isQuoting = m_quoteCharRadioButton.isSelected();
        settings.addBoolean(ColCombineNodeModel.CFG_IS_QUOTING, isQuoting);
        if (isQuoting) {
            String quoteFieldText = (String)m_quoteCharField.getValue();
            if (quoteFieldText == null || quoteFieldText.trim().length() == 0) {
                throw new InvalidSettingsException("Quote field is empty");
            }
            if (quoteFieldText.length() > 1) {
                throw new InvalidSettingsException(
                        "Quote field must only contain single character");
            }
            Character quote = quoteFieldText.charAt(0);
            settings.addChar(ColCombineNodeModel.CFG_QUOTE_CHAR, quote);
            settings.addBoolean(ColCombineNodeModel.CFG_IS_QUOTING_ALWAYS, 
                    m_quoteAlwaysChecker.isSelected());
        } else {
            String replace = m_replaceDelimStringField.getText();
            settings.addString(
                    ColCombineNodeModel.CFG_REPLACE_DELIMITER_STRING, replace);
        }
    }
}

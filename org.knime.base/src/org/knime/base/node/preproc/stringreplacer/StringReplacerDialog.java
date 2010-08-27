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
 * ---------------------------------------------------------------------
 * 
 * History
 *   18.06.2007 (thor): created
 */
package org.knime.base.node.preproc.stringreplacer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This is the dialog for the string replacer node where the user can
 * enter the pattern, the replacement string and several other options.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class StringReplacerDialog extends NodeDialogPane {
    private final StringReplacerSettings m_settings =
            new StringReplacerSettings();

    private final JCheckBox m_caseSensitiv = new JCheckBox();

    private final ColumnSelectionComboxBox m_colName =
            new ColumnSelectionComboxBox((Border)null, StringValue.class);

    private final JCheckBox m_createNewCol = new JCheckBox();

    private final JTextField m_newColName = new JTextField("New Column");

    private final JTextField m_pattern = new JTextField();
    
    private final JRadioButton m_wholeWordReplacement = new JRadioButton();
    
    private final JRadioButton m_replaceOccurrences = new JRadioButton();
    
    private final JTextField m_replacement = new JTextField();
   
    /**
     * Creates a new dialog.
     */
    public StringReplacerDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        
        
        p.add(new JLabel("Target column   "), c);
        c.gridx++;
        c.gridwidth = 2;
        p.add(m_colName, c);
        
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        p.add(new JLabel("Wildcard pattern   "), c);
        c.gridx++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        p.add(m_pattern, c);
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        p.add(new JLabel("Replacement text   "), c);
        c.gridx++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        p.add(m_replacement, c);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        
        ButtonGroup b = new ButtonGroup();
        b.add(m_wholeWordReplacement);
        b.add(m_replaceOccurrences);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        int oldSep = c.insets.bottom;
        c.insets.bottom = 1;
        p.add(new JLabel("Replace whole string   "), c);
        c.gridx++;
        p.add(m_wholeWordReplacement, c);
        
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets.bottom = oldSep;
        c.insets.top = 1;
        p.add(new JLabel("Replace all occurrences of pattern   "), c);
        c.gridx++;
        p.add(m_replaceOccurrences, c);
        c.insets.top = oldSep;

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Case sensitive search   "), c);
        c.gridx++;
        p.add(m_caseSensitiv, c);

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Append new column   "), c);
        c.gridx++;
        p.add(m_createNewCol, c);
        
        m_createNewCol.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_newColName.setEnabled(m_createNewCol.isSelected());
            }
        });
        
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        p.add(m_newColName, c);
        m_newColName.setEnabled(false);

        addTab("Standard settings", p);
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);
        
        m_caseSensitiv.setSelected(m_settings.caseSensitive());
        m_colName.update(specs[0], m_settings.columnName());
        m_createNewCol.setSelected(m_settings.createNewColumn());
        if (m_settings.newColumnName() != null) {
            m_newColName.setText(m_settings.newColumnName());
        } else {
            m_newColName.setText("Enter column name");
        }
        m_pattern.setText(m_settings.pattern());
        if (m_settings.replaceAllOccurrences()) {
            m_replaceOccurrences.doClick();
        } else {
            m_wholeWordReplacement.doClick();
        }
        m_replacement.setText(m_settings.replacement());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.caseSensitive(m_caseSensitiv.isSelected());
        m_settings.columnName(m_colName.getSelectedColumn());
        m_settings.createNewColumn(m_createNewCol.isSelected());
        m_settings.newColumnName(m_newColName.getText());
        m_settings.pattern(m_pattern.getText());
        m_settings.replaceAllOccurrences(m_replaceOccurrences.isSelected());
        m_settings.replacement(m_replacement.getText());
        
        m_settings.saveSettings(settings);
    }

}

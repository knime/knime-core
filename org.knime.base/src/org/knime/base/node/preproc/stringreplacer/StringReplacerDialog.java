/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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

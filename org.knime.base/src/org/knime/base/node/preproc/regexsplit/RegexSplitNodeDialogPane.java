/* ------------------------------------------------------------------
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
 *   Sep 1, 2008 (wiswedel): created
 */
package org.knime.base.node.preproc.regexsplit;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.StringHistoryPanel;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
final class RegexSplitNodeDialogPane extends NodeDialogPane {
    
    private final ColumnSelectionComboxBox m_columnSelectionPanel;
    private final StringHistoryPanel m_patternPanel;
    private final JCheckBox m_caseInsensitiveChecker;
    private final JCheckBox m_muliLineChecker;

    /** Inits components. */
    @SuppressWarnings("unchecked")
    public RegexSplitNodeDialogPane() {
        m_columnSelectionPanel = new ColumnSelectionComboxBox(
                (Border)null, StringValue.class);
        m_patternPanel = new StringHistoryPanel("regexsplitNodeDialog");
        m_patternPanel.setPrototypeDisplayValue(
                "#######################################");
        JComboBox box = m_patternPanel.getComboBox();
        Font font = box.getFont();
        box.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 
                (font == null ? 12 : font.getSize())));
        m_caseInsensitiveChecker = new JCheckBox(
                "Ignore Case (Case Insensitive)");
        m_muliLineChecker = new JCheckBox("Multiline");
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        
        p.add(new JLabel("Target Column: "), gbc);
        gbc.gridx += 1;
        p.add(m_columnSelectionPanel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy += 1;
        p.add(new JLabel("Pattern: "), gbc);
        gbc.gridx += 1;
        p.add(m_patternPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        p.add(new JLabel(" "), gbc);
        
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridy += 1;
        p.add(m_caseInsensitiveChecker, gbc);
        
        gbc.gridy += 1;
        p.add(m_muliLineChecker, gbc);
        
        addTab("Settings", p);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        RegexSplitSettings set = new RegexSplitSettings();
        set.loadSettingsInDialog(settings, specs[0]);
        m_patternPanel.updateHistory();
        m_patternPanel.setSelectedString(set.getPattern());
        m_caseInsensitiveChecker.setSelected(set.isCaseInsensitive());
        m_muliLineChecker.setSelected(set.isMultiLine());
        m_columnSelectionPanel.update(specs[0], set.getColumn());
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_patternPanel.commitSelectedToHistory();
        RegexSplitSettings set = new RegexSplitSettings();
        set.setPattern(m_patternPanel.getSelectedString());
        set.setCaseInsensitive(m_caseInsensitiveChecker.isSelected());
        set.setMultiLine(m_muliLineChecker.isSelected());
        set.setColumn(m_columnSelectionPanel.getSelectedColumn());
        set.saveSettingsTo(settings);
    }

}

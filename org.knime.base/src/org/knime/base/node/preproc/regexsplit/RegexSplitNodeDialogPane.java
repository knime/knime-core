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

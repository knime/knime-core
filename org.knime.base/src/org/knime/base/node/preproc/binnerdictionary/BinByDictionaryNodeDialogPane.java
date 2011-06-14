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
 */
package org.knime.base.node.preproc.binnerdictionary;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.DataValueColumnFilter;

/** Dialog to node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class BinByDictionaryNodeDialogPane extends NodeDialogPane {

    private final ColumnSelectionComboxBox m_valueColumnPort0Combo;
    private final JCheckBox m_lowerColumnPort1Checker;
    private final ColumnSelectionComboxBox m_lowerColumnPort1Combo;
    private final JCheckBox m_lowerBoundInclusiveChecker;
    private final JCheckBox m_upperColumnPort1Checker;
    private final ColumnSelectionComboxBox m_upperColumnPort1Combo;
    private final JCheckBox m_upperBoundInclusiveChecker;
    private final ColumnSelectionComboxBox m_labelColumnPort1Combo;
    private final JRadioButton m_failIfNoRuleMatchesButton;
    private final JRadioButton m_insertMissingIfNoRuleMatchesButton;

    /**  */
    @SuppressWarnings("unchecked")
    public BinByDictionaryNodeDialogPane() {
        m_valueColumnPort0Combo =
            new ColumnSelectionComboxBox((Border)null, DataValue.class);
        m_lowerColumnPort1Combo =
            new ColumnSelectionComboxBox((Border)null, DataValue.class);
        m_lowerColumnPort1Checker =
            new JCheckBox("Lower Bound Column (2nd port): ");
        m_lowerBoundInclusiveChecker = new JCheckBox("Inclusive");
        m_upperColumnPort1Combo =
            new ColumnSelectionComboxBox((Border)null, DataValue.class);
        m_upperColumnPort1Checker =
            new JCheckBox("Upper Bound Column (2nd port): ");
        m_upperBoundInclusiveChecker = new JCheckBox("Inclusive");
        m_labelColumnPort1Combo = new ColumnSelectionComboxBox((Border)null,
                new DataValueColumnFilter(DataValue.class));
        ButtonGroup buttonGroup = new ButtonGroup();
        m_failIfNoRuleMatchesButton = new JRadioButton("Fail");
        m_insertMissingIfNoRuleMatchesButton =
            new JRadioButton("Insert Missing");
        buttonGroup.add(m_failIfNoRuleMatchesButton);
        buttonGroup.add(m_insertMissingIfNoRuleMatchesButton);

        m_lowerColumnPort1Checker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean isSelected = m_lowerColumnPort1Checker.isSelected();
                m_lowerColumnPort1Combo.setEnabled(isSelected);
                m_lowerBoundInclusiveChecker.setEnabled(isSelected);
                if (!isSelected && !m_upperColumnPort1Checker.isSelected()) {
                    m_upperColumnPort1Checker.doClick();
                }
            }
        });
        m_upperColumnPort1Checker.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean isSelected = m_upperColumnPort1Checker.isSelected();
                m_upperColumnPort1Combo.setEnabled(isSelected);
                m_upperBoundInclusiveChecker.setEnabled(isSelected);
                if (!isSelected && !m_lowerColumnPort1Checker.isSelected()) {
                    m_lowerColumnPort1Checker.doClick();
                }
            }
        });
        m_lowerColumnPort1Checker.doClick();
        m_upperColumnPort1Checker.doClick();
        m_failIfNoRuleMatchesButton.doClick();
        initLayout();
    }

    /**
      *  */
    private void initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridwidth = 1;

        panel.add(new JLabel("Value Column to bin (1st port): "), gbc);
        gbc.gridx += 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(m_valueColumnPort0Combo, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(m_lowerColumnPort1Checker, gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx += 1;
        panel.add(m_lowerColumnPort1Combo, gbc);
        gbc.gridx += 1;
        panel.add(m_lowerBoundInclusiveChecker, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(m_upperColumnPort1Checker, gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx += 1;
        panel.add(m_upperColumnPort1Combo, gbc);
        gbc.gridx += 1;
        panel.add(m_upperBoundInclusiveChecker, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Label Column (2nd port): "), gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx += 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(m_labelColumnPort1Combo, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("If no rule matches: "), gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx += 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets.bottom = 0;
        panel.add(m_failIfNoRuleMatchesButton, gbc);
        gbc.insets.bottom = 5;
        gbc.gridy += 1;
        gbc.insets.top = 0;
        panel.add(m_insertMissingIfNoRuleMatchesButton, gbc);
        gbc.insets.top = 5;

        addTab("Configuration", panel);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        BinByDictionaryConfiguration c = new BinByDictionaryConfiguration();
        c.loadSettingsDialog(settings, specs);
        m_valueColumnPort0Combo.update(specs[0], c.getValueColumnPort0());
        String lowerBound = c.getLowerBoundColumnPort1();
        if ((lowerBound != null) != m_lowerColumnPort1Checker.isSelected()) {
            m_lowerColumnPort1Checker.doClick();
        }
        try {
            m_lowerColumnPort1Combo.update(specs[1], lowerBound);
        } catch (NotConfigurableException nce) {
            // ignore
        }
        m_lowerBoundInclusiveChecker.setSelected(c.isLowerBoundInclusive());
        String upperBound = c.getUpperBoundColumnPort1();
        if ((upperBound != null) != m_upperColumnPort1Checker.isSelected()) {
            m_upperColumnPort1Checker.doClick();
        }
        try {
            m_upperColumnPort1Combo.update(specs[1], upperBound);
        } catch (NotConfigurableException nce) {
            // ignore
        }
        m_upperBoundInclusiveChecker.setSelected(c.isUpperBoundInclusive());
        String labelCol = c.getLabelColumnPort1();
        m_labelColumnPort1Combo.update(specs[1], labelCol);
        if (c.isFailIfNoRuleMatches()) {
            m_failIfNoRuleMatchesButton.doClick();
        } else {
            m_insertMissingIfNoRuleMatchesButton.doClick();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        BinByDictionaryConfiguration c = new BinByDictionaryConfiguration();
        String valueCol = m_valueColumnPort0Combo.getSelectedColumn();
        String lowerCol = m_lowerColumnPort1Checker.isSelected()
            ? m_lowerColumnPort1Combo.getSelectedColumn() : null;
        String upperCol = m_upperColumnPort1Checker.isSelected()
            ? m_upperColumnPort1Combo.getSelectedColumn() : null;
        String labelCol = m_labelColumnPort1Combo.getSelectedColumn();
        c.setValueColumnPort0(valueCol);
        c.setLowerBoundColumnPort1(lowerCol);
        c.setLowerBoundInclusive(m_lowerBoundInclusiveChecker.isSelected());
        c.setUpperBoundColumnPort1(upperCol);
        c.setUpperBoundInclusive(m_upperBoundInclusiveChecker.isSelected());
        c.setLabelColumnPort1(labelCol);
        c.setFailIfNoRuleMatches(m_failIfNoRuleMatchesButton.isSelected());
        c.saveSettingsTo(settings);
    }

}

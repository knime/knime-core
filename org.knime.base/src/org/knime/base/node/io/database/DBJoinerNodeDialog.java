/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   27.07.2007 (thor): created
 */
package org.knime.base.node.io.database;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.io.database.DBJoinerSettings.DuplicateHandling;
import org.knime.base.node.io.database.DBJoinerSettings.JoinMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.ColumnPairsSelectionPanel;

/**
 * This is the dialog for the database joiner node.
 *
 * @author Heiko Hofer
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
final class DBJoinerNodeDialog extends NodeDialogPane {

    private final JComboBox<JoinMode> m_joinMode = new JComboBox<JoinMode>(JoinMode.values());

    private final JRadioButton m_dontExecute = new JRadioButton("Don't execute");

    private final JRadioButton m_filterDuplicates = new JRadioButton("Filter duplicates");

    private final JRadioButton m_appendSuffixAutomatic = new JRadioButton("Append suffix (automatic)");

    private final JRadioButton m_appendSuffix = new JRadioButton("Append custom suffix:");

    private final JTextField m_suffix = new JTextField();

    private ColumnPairsSelectionPanel m_columnPairs;

    private ColumnFilterPanel m_leftFilterPanel;

    private ColumnFilterPanel m_rightFilterPanel;

    private final JCheckBox m_removeLeftJoinCols =
            new JCheckBox("Remove joining columns from top input ('left' table)");

    private final JCheckBox m_removeRightJoinCols =
            new JCheckBox("Remove joining columns from bottom input ('right' table)");

    private JRadioButton m_matchAllButton = new JRadioButton("Match all of the following");

    private JRadioButton m_matchAnyButton = new JRadioButton("Match any of the following");

    /**
     * Creates a new dialog for the database joiner node.
     */
    DBJoinerNodeDialog() {
        addTab("Joiner Settings", createJoinerSettingsTab());
        addTab("Column Selection", createColumnSelectionTab());
    }

    private JPanel createJoinerSettingsTab() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weighty = 0;
        p.add(createJoinModeUIControls(), c);
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        p.add(createJoinPredicateUIControls(), c);
        c.weightx = 0;
        c.weighty = 0;
        c.gridy++;
        c.weightx = 100;
        c.weighty = 100;
        c.fill = GridBagConstraints.BOTH;
        p.add(new JPanel(), c);
        return p;
    }

    private JPanel createJoinPredicateUIControls() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        JPanel matchButtonPanel = new JPanel(new FlowLayout());
        matchButtonPanel.add(m_matchAllButton);
        matchButtonPanel.add(m_matchAnyButton);
        p.add(matchButtonPanel, c);
        ButtonGroup group = new ButtonGroup();
        group.add(m_matchAllButton);
        group.add(m_matchAnyButton);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weightx = 1;
        c.weighty = 1;
        m_columnPairs = new ColumnPairsSelectionPanel(false) {
            private static final long serialVersionUID = 1738914698074362596L;
            @SuppressWarnings("rawtypes")
            @Override
            protected void initComboBox(final DataTableSpec spec, final JComboBox comboBox, final String selected) {
                super.initComboBox(spec, comboBox, selected);
                if (selected == null && comboBox.getModel().getSize() > 0) {
                    comboBox.setSelectedIndex(0);
                }
            }
        };
        JScrollPane scrollPane = new JScrollPane(m_columnPairs);
        m_columnPairs.setBackground(Color.white);
        Component header = m_columnPairs.getHeaderView();
        header.setPreferredSize(new Dimension(300, 20));
        scrollPane.setColumnHeaderView(header);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        scrollPane.setMinimumSize(new Dimension(300, 100));
        p.add(scrollPane, c);
        p.setBorder(BorderFactory.createTitledBorder("Joining Columns"));
        return p;
    }

    private JPanel createJoinModeUIControls() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Join mode  ", SwingConstants.RIGHT), c);
        c.gridx++;
        p.add(m_joinMode, c);
        p.setBorder(BorderFactory.createTitledBorder("Join Mode"));
        return p;
    }

    private JComponent createColumnSelectionTab() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.gridwidth = 1;
        m_leftFilterPanel = new ColumnFilterPanel(true);
        m_leftFilterPanel.setBorder(BorderFactory.createTitledBorder("Top Input ('left' table)"));
        p.add(m_leftFilterPanel, c);
        c.gridy++;
        m_rightFilterPanel = new ColumnFilterPanel(true);
        m_rightFilterPanel.setBorder(BorderFactory.createTitledBorder("Bottom Input ('right' table)"));
        p.add(m_rightFilterPanel, c);
        c.gridy++;
        p.add(createDuplicateColumnHandlingUIConstrols(), c);
        return new JScrollPane(p);
    }

    private JPanel createDuplicateColumnHandlingUIConstrols() {
        JPanel left = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        left.add(m_filterDuplicates, c);
        c.gridy++;
        left.add(m_dontExecute, c);
        c.gridy++;
        left.add(m_appendSuffixAutomatic, c);
        c.gridy++;
        left.add(m_appendSuffix, c);
        c.gridx++;
        m_suffix.setPreferredSize(new Dimension(100, m_suffix.getPreferredSize().height));
        left.add(m_suffix, c);
        m_appendSuffix.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_suffix.setEnabled(m_appendSuffix.isSelected());
            }
        });
        ButtonGroup duplicateColGroup = new ButtonGroup();
        duplicateColGroup.add(m_filterDuplicates);
        duplicateColGroup.add(m_dontExecute);
        duplicateColGroup.add(m_appendSuffixAutomatic);
        duplicateColGroup.add(m_appendSuffix);
        left.setBorder(BorderFactory.createTitledBorder("Duplicate Column Handling"));
        JPanel right = new JPanel(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        right.add(m_removeLeftJoinCols, c);
        c.gridy++;
        right.add(m_removeRightJoinCols, c);
        right.setBorder(BorderFactory.createTitledBorder("Joining Columns Handling"));
        JPanel p = new JPanel(new GridLayout(1, 2));
        p.add(left);
        p.add(right);
        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        if (specs == null || specs.length < 2 || specs[0] == null || specs[1] == null) {
            throw new NotConfigurableException("No input specification available.");
        }
        DataTableSpec tableSpec1 = ((DatabasePortObjectSpec)specs[0]).getDataTableSpec();
        DataTableSpec tableSpec2 = ((DatabasePortObjectSpec)specs[1]).getDataTableSpec();
        DBJoinerSettings joinerSettings = new DBJoinerSettings();
        joinerSettings.loadSettingsInDialog(settings);
        m_joinMode.setSelectedItem(joinerSettings.getJoinMode());
        m_matchAllButton.setSelected(joinerSettings.getAndComposition());
        m_matchAnyButton.setSelected(!joinerSettings.getAndComposition());
        m_columnPairs.updateData(new DataTableSpec[]{tableSpec1, tableSpec2}, joinerSettings.getLeftJoinOnColumns(),
            joinerSettings.getRightJoinOnColumns());
        m_leftFilterPanel.update(tableSpec1, false, joinerSettings.getLeftColumns());
        m_rightFilterPanel.update(tableSpec2, false, joinerSettings.getRightColumns());
        m_leftFilterPanel.setKeepAllSelected(joinerSettings.getAllLeftColumns());
        m_rightFilterPanel.setKeepAllSelected(joinerSettings.getAllRightColumns());
        m_removeLeftJoinCols.setSelected(joinerSettings.getFilterLeftJoinOnColumns());
        m_removeRightJoinCols.setSelected(joinerSettings.getFilterRightJoinOnColumns());
        m_suffix.setText(joinerSettings.getCustomSuffix());
        m_filterDuplicates.setSelected(joinerSettings.getDuplicateHandling().equals(DuplicateHandling.Filter));
        m_dontExecute.setSelected(joinerSettings.getDuplicateHandling().equals(DuplicateHandling.DontExecute));
        m_appendSuffixAutomatic.setSelected(joinerSettings.getDuplicateHandling().equals(
            DuplicateHandling.AppendSuffixAutomatic));
        m_appendSuffix.setSelected(joinerSettings.getDuplicateHandling().equals(DuplicateHandling.AppendSuffix));
        m_suffix.setEnabled(joinerSettings.getDuplicateHandling().equals(DuplicateHandling.AppendSuffix));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        DBJoinerSettings joinerSettings = new DBJoinerSettings();
        joinerSettings.setJoinMode(((JoinMode)m_joinMode.getSelectedItem()));
        joinerSettings.setAndComposition(m_matchAllButton.isSelected());
        Object[] lr = m_columnPairs.getLeftSelectedItems();
        String[] ls = new String[lr.length];
        for (int i = 0; i < lr.length; i++) {
            if (lr[i] == null) {
                throw new InvalidSettingsException("There are invalid "
                    + "joining columns (highlighted with a red border).");
            }
            ls[i] = ((DataColumnSpec)lr[i]).getName();
        }
        joinerSettings.setLeftJoinOnColumns(ls);
        Object[] rr = m_columnPairs.getRightSelectedItems();
        String[] rs = new String[rr.length];
        for (int i = 0; i < rr.length; i++) {
            if (rr[i] == null) {
                throw new InvalidSettingsException("There are invalid "
                    + "joining columns (highlighted with a red border).");
            }
            rs[i] = ((DataColumnSpec)rr[i]).getName();
        }
        joinerSettings.setRightJoinOnColumns(rs);
        joinerSettings.setLeftColumns(m_leftFilterPanel.getIncludedColumnSet().toArray(new String[0]));
        joinerSettings.setRightColumns(m_rightFilterPanel.getIncludedColumnSet().toArray(new String[0]));
        joinerSettings.setAllLeftColumns(m_leftFilterPanel.isKeepAllSelected());
        joinerSettings.setAllRightColumns(m_rightFilterPanel.isKeepAllSelected());
        joinerSettings.setFilterLeftJoinOnColumns(m_removeLeftJoinCols.isSelected());
        joinerSettings.setFilterRightJoinOnColumns(m_removeRightJoinCols.isSelected());
        joinerSettings.setCustomSuffix(m_suffix.getText());
        if (m_filterDuplicates.isSelected()) {
            joinerSettings.setDuplicateHandling(DuplicateHandling.Filter);
        } else if (m_dontExecute.isSelected()) {
            joinerSettings.setDuplicateHandling(DuplicateHandling.DontExecute);
        } else if (m_appendSuffixAutomatic.isSelected()) {
            joinerSettings.setDuplicateHandling(DuplicateHandling.AppendSuffixAutomatic);
        } else {
            joinerSettings.setDuplicateHandling(DuplicateHandling.AppendSuffix);
        }
        joinerSettings.saveSettings(settings);
    }
}

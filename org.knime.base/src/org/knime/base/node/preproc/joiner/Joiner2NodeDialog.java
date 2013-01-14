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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   27.07.2007 (thor): created
 */
package org.knime.base.node.preproc.joiner;

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

import org.knime.base.node.preproc.joiner.Joiner2Settings.DuplicateHandling;
import org.knime.base.node.preproc.joiner.Joiner2Settings.JoinMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.ColumnPairsSelectionPanel;

/**
 * This is the dialog for the joiner node.
 *
 * @author Heiko Hofer
 */
public class Joiner2NodeDialog extends NodeDialogPane {
    private final JComboBox m_joinMode =
            new JComboBox(new Object[]{JoinMode.InnerJoin,
                    JoinMode.LeftOuterJoin, JoinMode.RightOuterJoin,
                    JoinMode.FullOuterJoin});

    private final JRadioButton m_dontExecute =
            new JRadioButton("Don't execute");

    private final JRadioButton m_filterDuplicates =
            new JRadioButton("Filter duplicates");

    private final JRadioButton m_appendSuffixAutomatic =
        new JRadioButton("Append suffix (automatic)");

    private final JRadioButton m_appendSuffix =
            new JRadioButton("Append custom suffix:");
    private final JTextField m_suffix = new JTextField();

    private final Joiner2Settings m_settings = new Joiner2Settings();

    private ColumnPairsSelectionPanel m_columnPairs;

    private ColumnFilterPanel m_leftFilterPanel;
    private ColumnFilterPanel m_rightFilterPanel;
    private final JCheckBox m_removeLeftJoinCols =
        new JCheckBox("Filter left joining columns");
    private final JCheckBox m_removeRightJoinCols =
        new JCheckBox("Filter right joining columns");
    private JRadioButton m_matchAllButton = new JRadioButton(
            "Match all of the following");
    private JRadioButton m_matchAnyButton = new JRadioButton(
            "Match any of the following");

    private final JTextField m_maxOpenFiles = new JTextField();
    private final JTextField m_rowKeySeparator = new JTextField();

    private final JCheckBox m_enableHiLite =
        new JCheckBox("Enable hiliting");
    /**
     * Creates a new dialog for the joiner node.
     */
    public Joiner2NodeDialog() {
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

        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        p.add(createPerformanceUIControls(), c);
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        p.add(createRowKeyUIControls(), c);
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

        m_columnPairs = new ColumnPairsSelectionPanel();
        m_columnPairs.setRowKeyIdentifier(Joiner2Settings.ROW_KEY_IDENTIFIER);
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

    private JPanel createPerformanceUIControls() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;

        p.add(new JLabel("Maximum number of open files:"), c);
        c.gridx++;
        m_maxOpenFiles.setPreferredSize(new Dimension(200,
                m_maxOpenFiles.getPreferredSize().height));
        p.add(m_maxOpenFiles, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        p.add(m_enableHiLite, c);

        p.setBorder(BorderFactory.createTitledBorder("Performance Tuning"));
        return p;
    }

    private JPanel createRowKeyUIControls() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;

        p.add(new JLabel("Row ID separator in joined table:"), c);
        c.gridx++;
        m_rowKeySeparator.setPreferredSize(new Dimension(50,
                m_rowKeySeparator.getPreferredSize().height));
        p.add(m_rowKeySeparator, c);

        p.setBorder(BorderFactory.createTitledBorder("Row IDs"));
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
        m_leftFilterPanel.setBorder(
                BorderFactory.createTitledBorder("Left Table"));
        p.add(m_leftFilterPanel, c);
        c.gridy++;
        m_rightFilterPanel = new ColumnFilterPanel(true);
        m_rightFilterPanel.setBorder(
                BorderFactory.createTitledBorder("Right Table"));
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
        m_suffix.setPreferredSize(new Dimension(100,
                m_suffix.getPreferredSize().height));
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

        left.setBorder(BorderFactory.createTitledBorder(
                "Duplicate Column Handling"));

        JPanel right = new JPanel(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        right.add(m_removeLeftJoinCols, c);
        c.gridy++;
        right.add(m_removeRightJoinCols, c);

        right.setBorder(BorderFactory.createTitledBorder(
        "Joining Columns Handling"));

        JPanel p = new JPanel(new GridLayout(1, 2));
        p.add(left);
        p.add(right);
        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_joinMode.setSelectedItem(m_settings.getJoinMode());

        m_matchAllButton.setSelected(m_settings.getCompositionMode().equals(
                    Joiner2Settings.CompositionMode.MatchAll));

        m_matchAnyButton.setSelected(m_settings.getCompositionMode().equals(
                    Joiner2Settings.CompositionMode.MatchAny));


        m_columnPairs.updateData(specs, m_settings.getLeftJoinColumns(),
                m_settings.getRightJoinColumns());

        m_filterDuplicates.setSelected(m_settings.getDuplicateHandling().equals(
                DuplicateHandling.Filter));
        m_dontExecute.setSelected(m_settings.getDuplicateHandling().equals(
                DuplicateHandling.DontExecute));
        m_appendSuffixAutomatic.setSelected(m_settings.getDuplicateHandling().equals(
                DuplicateHandling.AppendSuffixAutomatic));
        m_appendSuffix.setSelected(m_settings.getDuplicateHandling().equals(
                DuplicateHandling.AppendSuffix));

        m_suffix.setText(m_settings.getDuplicateColumnSuffix());
        m_suffix.setEnabled(m_settings.getDuplicateHandling().equals(
                DuplicateHandling.AppendSuffix));

        m_leftFilterPanel.update(specs[0], false,
                m_settings.getLeftIncludeCols());
        m_rightFilterPanel.update(specs[1], false,
                m_settings.getRightIncludeCols());

        m_leftFilterPanel.setKeepAllSelected(m_settings.getLeftIncludeAll());
        m_rightFilterPanel.setKeepAllSelected(m_settings.getRightIncludeAll());

        m_removeLeftJoinCols.setSelected(m_settings.getRemoveLeftJoinCols());
        m_removeRightJoinCols.setSelected(m_settings.getRemoveRightJoinCols());

        m_maxOpenFiles.setText(Integer.toString(m_settings.getMaxOpenFiles()));
        m_rowKeySeparator.setText(m_settings.getRowKeySeparator());
        m_enableHiLite.setSelected(m_settings.getEnableHiLite());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.setJoinMode((JoinMode)m_joinMode.getSelectedItem());

        if (m_matchAllButton.isSelected()) {
            m_settings.setCompositionMode(
                    Joiner2Settings.CompositionMode.MatchAll);
        }
        if (m_matchAnyButton.isSelected()) {
            m_settings.setCompositionMode(
                    Joiner2Settings.CompositionMode.MatchAny);
        }

        Object[] lr = m_columnPairs.getLeftSelectedItems();
        String[] ls = new String[lr.length];
        for (int i = 0; i < lr.length; i++) {
            if (lr[i] == null) {
                throw new InvalidSettingsException("There are invalid "
                    + "joining columns (highlighted with a red border).");
            }
            if (lr[i] instanceof String) {
                ls[i] = Joiner2Settings.ROW_KEY_IDENTIFIER;
            } else {
                ls[i] = ((DataColumnSpec)lr[i]).getName();
            }
        }

        m_settings.setLeftJoinColumns(ls);
        Object[] rr = m_columnPairs.getRightSelectedItems();
        String[] rs = new String[rr.length];
        for (int i = 0; i < rr.length; i++) {
            if (rr[i] == null) {
                throw new InvalidSettingsException("There are invalid "
                    + "joining columns (highlighted with a red border).");
            }
            if (rr[i] instanceof String) {
                rs[i] = Joiner2Settings.ROW_KEY_IDENTIFIER;
            } else {
                rs[i] = ((DataColumnSpec)rr[i]).getName();
            }
        }
        m_settings.setRightJoinColumns(rs);

        if (m_filterDuplicates.isSelected()) {
            m_settings.setDuplicateHandling(DuplicateHandling.Filter);
        } else if (m_dontExecute.isSelected()) {
            m_settings.setDuplicateHandling(DuplicateHandling.DontExecute);
        } else if (m_appendSuffixAutomatic.isSelected()) {
            m_settings.setDuplicateHandling(
                DuplicateHandling.AppendSuffixAutomatic);
        } else {
            String suffix = m_suffix.getText().trim().isEmpty() ? ""
                    : m_suffix.getText();
            m_settings.setDuplicateHandling(DuplicateHandling.AppendSuffix);
            m_settings.setDuplicateColumnSuffix(suffix);
        }
        m_settings.setVersion(Joiner2Settings.VERSION_2_1);

        m_settings.setLeftIncludeCols(
                m_leftFilterPanel.getIncludedColumnSet().toArray(
                        new String[0]));
        m_settings.setRightIncludeCols(
                m_rightFilterPanel.getIncludedColumnSet().toArray(
                        new String[0]));

        m_settings.setLeftIncludeAll(m_leftFilterPanel.isKeepAllSelected());
        m_settings.setRightIncludeAll(m_rightFilterPanel.isKeepAllSelected());

        m_settings.setRemoveLeftJoinCols(m_removeLeftJoinCols.isSelected());
        m_settings.setRemoveRightJoinCols(m_removeRightJoinCols.isSelected());

        m_settings.setMaxOpenFiles(Integer.parseInt(m_maxOpenFiles.getText()));
        m_settings.setRowKeySeparator(m_rowKeySeparator.getText());
        m_settings.setEnableHiLite(m_enableHiLite.isSelected());

        m_settings.saveSettings(settings);
    }
}

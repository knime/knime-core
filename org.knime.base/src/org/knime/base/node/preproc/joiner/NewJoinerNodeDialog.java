/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   27.07.2007 (thor): created
 */
package org.knime.base.node.preproc.joiner;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.joiner.NewJoinerSettings.DuplicateHandling;
import org.knime.base.node.preproc.joiner.NewJoinerSettings.JoinMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * This is the dialog for the joiner node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class NewJoinerNodeDialog extends NodeDialogPane {
    private final DefaultComboBoxModel m_comboModel =
            new DefaultComboBoxModel();

    private final JComboBox m_secondTableColumn = new JComboBox(m_comboModel);

    private final JComboBox m_joinMode =
            new JComboBox(new Object[]{JoinMode.InnerJoin,
                    JoinMode.LeftOuterJoin, JoinMode.RightOuterJoin,
                    JoinMode.FullOuterJoin});

    private final JRadioButton m_dontExecute =
            new JRadioButton("Don't execute");

    private final JRadioButton m_filterDuplicates =
            new JRadioButton("Filter duplicates");

    private final JRadioButton m_appendSuffix =
            new JRadioButton("Append suffix");

    private final JTextField m_suffix = new JTextField();

    private final NewJoinerSettings m_settings = new NewJoinerSettings();

    /**
     * Creates a new dialog for the joiner node.
     */
    public NewJoinerNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Join column from second table   ", JLabel.RIGHT), c);
        c.gridx++;
        p.add(m_secondTableColumn, c);
        m_secondTableColumn.setRenderer(new ColumnSpecListRenderer());

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Duplicate column handling   ", JLabel.RIGHT), c);
        c.gridx++;
        p.add(m_filterDuplicates, c);

        c.gridy++;
        p.add(m_dontExecute, c);

        c.gridy++;
        p.add(m_appendSuffix, c);
        c.gridy++;
        p.add(m_suffix, c);

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Join mode  ", JLabel.RIGHT), c);
        c.gridx++;
        p.add(m_joinMode, c);

        ButtonGroup bg = new ButtonGroup();
        bg.add(m_dontExecute);
        bg.add(m_filterDuplicates);
        bg.add(m_appendSuffix);

        m_appendSuffix.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_suffix.setEnabled(m_appendSuffix.isSelected());
            }
        });

        addTab("Standard Settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_joinMode.setSelectedItem(m_settings.joinMode());

        m_comboModel.removeAllElements();
        m_comboModel.addElement(NewJoinerSettings.ROW_KEY_COL_NAME);
        m_secondTableColumn.setSelectedIndex(0);
        for (DataColumnSpec colSpec : specs[1]) {
            m_comboModel.addElement(colSpec);
            if (colSpec.getName().equals(m_settings.secondTableColumn())) {
                m_secondTableColumn.setSelectedItem(colSpec);
            }
        }

        m_filterDuplicates.setSelected(m_settings.duplicateHandling().equals(
                DuplicateHandling.Filter));
        m_dontExecute.setSelected(m_settings.duplicateHandling().equals(
                DuplicateHandling.DontExecute));
        m_appendSuffix.setSelected(m_settings.duplicateHandling().equals(
                DuplicateHandling.AppendSuffix));

        m_suffix.setText(m_settings.suffix());
        m_suffix.setEnabled(m_settings.duplicateHandling().equals(
                DuplicateHandling.AppendSuffix));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.joinMode((JoinMode)m_joinMode.getSelectedItem());

        if (m_secondTableColumn.getSelectedItem() instanceof String) {
            m_settings.secondTableColumn(NewJoinerSettings.ROW_KEY_IDENTIFIER);
        } else {
            m_settings.secondTableColumn(((DataColumnSpec)m_secondTableColumn
                    .getSelectedItem()).getName());
        }
        if (m_filterDuplicates.isSelected()) {
            m_settings.duplicateHandling(DuplicateHandling.Filter);
        } else if (m_dontExecute.isSelected()) {
            m_settings.duplicateHandling(DuplicateHandling.DontExecute);
        } else {
            m_settings.duplicateHandling(DuplicateHandling.AppendSuffix);
            m_settings.suffix(m_suffix.getText());
        }

        m_settings.saveSettings(settings);
    }
}

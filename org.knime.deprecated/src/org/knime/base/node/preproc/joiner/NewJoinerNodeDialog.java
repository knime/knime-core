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
import javax.swing.SwingConstants;
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

    private final JTextField m_keySuffix = new JTextField();

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
        p.add(new JLabel("Join column from second table   ", SwingConstants.RIGHT), c);
        c.gridx++;
        p.add(m_secondTableColumn, c);
        m_secondTableColumn.setRenderer(new ColumnSpecListRenderer());

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Duplicate column handling   ", SwingConstants.RIGHT), c);
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
        p.add(new JLabel("Join mode  ", SwingConstants.RIGHT), c);
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

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Multiple-match row ID suffix   ", SwingConstants.RIGHT), c);
        c.gridx++;
        p.add(m_keySuffix, c);


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

        m_keySuffix.setText(m_settings.keySuffix());
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

        m_settings.keySuffix(m_keySuffix.getText());

        m_settings.saveSettings(settings);
    }
}

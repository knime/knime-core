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
package org.knime.base.node.preproc.columnmerge;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.base.node.preproc.columnmerge.ColumnMergerConfiguration.OutputPlacement;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.ViewUtils;

/** Dialog to node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnMergerNodeDialogPane extends NodeDialogPane {

    private final ColumnSelectionComboxBox m_primaryColBox;
    private final ColumnSelectionComboxBox m_secondaryColBox;
    private final JRadioButton m_replacePrimButton;
    private final JRadioButton m_replaceSecButton;
    private final JRadioButton m_replaceBothButton;
    private final JRadioButton m_appendNewButton;
    private final JTextField m_newColumnField;

    /** Creates controls, does layout. */
    @SuppressWarnings("unchecked")
    public ColumnMergerNodeDialogPane() {
        m_primaryColBox = new ColumnSelectionComboxBox(
                (Border)null, DataValue.class);
        m_secondaryColBox = new ColumnSelectionComboxBox(
                (Border)null, DataValue.class);
        ButtonGroup b = new ButtonGroup();
        m_replacePrimButton = new JRadioButton("Replace primary column");
        b.add(m_replacePrimButton);
        m_replaceSecButton = new JRadioButton("Replace secondary column");
        b.add(m_replaceSecButton);
        m_replaceBothButton = new JRadioButton("Replace both columns");
        b.add(m_replaceBothButton);
        m_appendNewButton = new JRadioButton("Append new column: ");
        b.add(m_appendNewButton);
        m_newColumnField = new JTextField(8);
        m_appendNewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_newColumnField.requestFocus();
                m_newColumnField.selectAll();
            }
        });
        m_appendNewButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_newColumnField.setEnabled(m_appendNewButton.isSelected());
            }
        });
        m_newColumnField.setEnabled(m_appendNewButton.isSelected());
        m_replaceBothButton.doClick();

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        gbc.gridwidth = GridBagConstraints.RELATIVE;
        p.add(new JLabel("Primary Column: "), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        p.add(m_primaryColBox, gbc);

        gbc.gridwidth = GridBagConstraints.RELATIVE;
        p.add(new JLabel("Secondary Column: "), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        p.add(m_secondaryColBox, gbc);

        p.add(new JLabel(" "), gbc);
        p.add(new JLabel("Output Placement"), gbc);
        gbc.insets = new Insets(3, 10, 0, 10);
        p.add(getInFlowLayout(m_replacePrimButton), gbc);
        p.add(getInFlowLayout(m_replaceSecButton), gbc);
        p.add(getInFlowLayout(m_replaceBothButton), gbc);
        p.add(getInFlowLayout(m_appendNewButton, m_newColumnField), gbc);
        addTab("Control", p);
    }

    private static JPanel getInFlowLayout(final JComponent... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : cs) {
            p.add(c);
        }
        return p;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        ColumnMergerConfiguration c = new ColumnMergerConfiguration();
        if (m_replacePrimButton.isSelected()) {
            c.setOutputPlacement(OutputPlacement.ReplacePrimary);
        } else if (m_replaceBothButton.isSelected()) {
            c.setOutputPlacement(OutputPlacement.ReplaceBoth);
        } else if (m_replaceSecButton.isSelected()) {
            c.setOutputPlacement(OutputPlacement.ReplaceSecondary);
        } else {
            c.setOutputPlacement(OutputPlacement.AppendAsNewColumn);
            c.setOutputName(m_newColumnField.getText());
        }
        c.setPrimaryColumn(m_primaryColBox.getSelectedColumn());
        c.setSecondaryColumn(m_secondaryColBox.getSelectedColumn());
        c.saveConfiguration(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        final ColumnMergerConfiguration c = new ColumnMergerConfiguration();
        c.loadConfigurationInDialog(settings, specs[0]);
        final AtomicReference<NotConfigurableException> ex =
            new AtomicReference<NotConfigurableException>();
        Runnable run = new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                switch (c.getOutputPlacement()) {
                case ReplacePrimary:
                    m_replacePrimButton.doClick();
                    break;
                case ReplaceBoth:
                    m_replaceBothButton.doClick();
                    break;
                case ReplaceSecondary:
                    m_replaceSecButton.doClick();
                    break;
                case AppendAsNewColumn:
                    m_appendNewButton.doClick();
                    m_newColumnField.setText(c.getOutputName());
                    break;
                default:
                    // unknown
                }
                try {
                    m_primaryColBox.update(specs[0], c.getPrimaryColumn());
                    m_secondaryColBox.update(specs[0], c.getSecondaryColumn());
                } catch (NotConfigurableException nfe) {
                    ex.set(nfe);
                }
            }
        };
        ViewUtils.invokeAndWaitInEDT(run);
        if (ex.get() != null) {
            throw ex.get();
        }
    }

}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.addemptyrows;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * <code>NodeDialog</code> for the "AddEmptyRows" Node. Adds a certain number of
 * empty rows with missing values or a given constant.
 *
 * @author Bernd Wiswedel
 */
public class AddEmptyRowsNodeDialog extends NodeDialogPane {

    private final JSpinner m_atLeastCountSpinner;

    private final JSpinner m_additionalCountSpinner;

    private final JRadioButton m_atLeastRadio;

    private final JRadioButton m_additionalRadio;

    private final JTextField m_newRowKeyPrefixField;

    private final JCheckBox m_useMissingDoubleChecker;

    private final JFormattedTextField m_fillDataDoubleField;

    private final JCheckBox m_useMissingIntChecker;

    private final JFormattedTextField m_fillDataIntField;

    private final JCheckBox m_useMissingStringChecker;

    private final JTextField m_fillDataStringField;

    /**
     * New pane for configuring the AddEmptyRows node.
     */
    protected AddEmptyRowsNodeDialog() {
        m_atLeastCountSpinner =
                new JSpinner(
                        new SpinnerNumberModel(15, 0, Integer.MAX_VALUE, 5));
        m_additionalCountSpinner =
                new JSpinner(
                        new SpinnerNumberModel(15, 0, Integer.MAX_VALUE, 5));
        ButtonGroup group = new ButtonGroup();
        m_atLeastRadio = new JRadioButton("At least ");
        group.add(m_atLeastRadio);
        addEnableListener(m_atLeastRadio, m_atLeastCountSpinner, false);

        m_additionalRadio = new JRadioButton("Additional ");
        group.add(m_additionalRadio);
        addEnableListener(m_additionalRadio, m_additionalCountSpinner, false);
        // get initial enable status right
        m_additionalRadio.doClick();
        m_atLeastRadio.doClick();

        m_newRowKeyPrefixField = new JTextField(8);

        m_useMissingDoubleChecker = new JCheckBox("Missing");
        m_fillDataDoubleField = new JFormattedTextField(0.0);
        m_fillDataDoubleField.setColumns(10);
        addEnableListener(m_useMissingDoubleChecker, m_fillDataDoubleField,
                true);

        m_useMissingIntChecker = new JCheckBox("Missing");
        m_fillDataIntField = new JFormattedTextField(0);
        m_fillDataIntField.setColumns(10);
        addEnableListener(m_useMissingIntChecker, m_fillDataIntField, true);

        m_useMissingStringChecker = new JCheckBox("Missing");
        m_fillDataStringField = new JTextField("empty");
        m_fillDataStringField.setColumns(10);
        addEnableListener(m_useMissingStringChecker, m_fillDataStringField,
                true);
        addTab("Settings", createPanel());
    }

    private JPanel createPanel() {
        JPanel result = new JPanel(new GridLayout(0, 1));

        JPanel rowsPanel = new JPanel(new GridLayout(0, 2));
        rowsPanel.add(getInFlowLayout(m_atLeastRadio));
        rowsPanel.add(getInFlowLayout(m_atLeastCountSpinner));
        rowsPanel.add(getInFlowLayout(m_additionalRadio));
        rowsPanel.add(getInFlowLayout(m_additionalCountSpinner));
        rowsPanel.add(getInFlowLayout(new JLabel("RowID Prefix: ")));
        rowsPanel.add(getInFlowLayout(m_newRowKeyPrefixField));

        rowsPanel.setBorder(BorderFactory
                .createTitledBorder("Numbers of rows in output"));
        result.add(rowsPanel);

        JPanel fillDataPanel = new JPanel(new GridLayout(0, 2));
        fillDataPanel.add(getInFlowLayout(new JLabel("Double: ")));
        fillDataPanel.add(getInFlowLayout(m_fillDataDoubleField,
                m_useMissingDoubleChecker));
        fillDataPanel.add(getInFlowLayout(new JLabel("Int: ")));
        fillDataPanel.add(getInFlowLayout(m_fillDataIntField,
                m_useMissingIntChecker));
        fillDataPanel.add(getInFlowLayout(new JLabel("String: ")));
        fillDataPanel.add(getInFlowLayout(m_fillDataStringField,
                m_useMissingStringChecker));
        fillDataPanel.setBorder(BorderFactory.createTitledBorder("Fill Data"));
        result.add(fillDataPanel);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        AddEmptyRowsConfig config = new AddEmptyRowsConfig();
        if (m_atLeastRadio.isSelected()) {
            config.setAtLeastMode(true);
            config.setRowCount((Integer)m_atLeastCountSpinner.getValue());
        } else {
            config.setAtLeastMode(false);
            config.setRowCount((Integer)m_additionalCountSpinner.getValue());
        }
        config.setNewRowKeyPrefix(m_newRowKeyPrefixField.getText());
        config.setFillValueDouble(m_useMissingDoubleChecker.isSelected(),
                (Double)m_fillDataDoubleField.getValue());
        config.setFillValueInt(m_useMissingIntChecker.isSelected(),
                (Integer)m_fillDataIntField.getValue());
        config.setFillValueString(m_useMissingStringChecker.isSelected(),
                m_fillDataStringField.getText());
        config.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        AddEmptyRowsConfig config = new AddEmptyRowsConfig();
        try {
            config.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            config = new AddEmptyRowsConfig();
        }
        if (config.isAtLeastMode()) {
            m_atLeastRadio.doClick();
            m_atLeastCountSpinner.setValue(config.getRowCount());
        } else {
            m_additionalRadio.doClick();
            m_additionalCountSpinner.setValue(config.getRowCount());
        }
        m_newRowKeyPrefixField.setText(config.getNewRowKeyPrefix());
        boolean useMissingDouble = config.isUseMissingDouble();
        if (useMissingDouble != m_useMissingDoubleChecker.isSelected()) {
            m_useMissingDoubleChecker.doClick();
        }
        m_fillDataDoubleField.setValue(config.getFillValueDouble());

        boolean useMissingInt = config.isUseMissingInt();
        if (useMissingInt != m_useMissingIntChecker.isSelected()) {
            m_useMissingIntChecker.doClick();
        }
        m_fillDataIntField.setValue(config.getFillValueInt());

        boolean useMissingString = config.isUseMissingString();
        if (useMissingString != m_useMissingStringChecker.isSelected()) {
            m_useMissingStringChecker.doClick();
        }
        m_fillDataStringField.setText(config.getFillValueString());
    }

    private static final JPanel getInFlowLayout(final JComponent... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : comps) {
            p.add(c);
        }
        return p;
    }

    private static final void addEnableListener(final AbstractButton button,
            final JComponent comp, final boolean reverse) {
        button.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent evt) {
                boolean shouldEnable;
                if (reverse) {
                    shouldEnable = !button.isSelected();
                } else {
                    shouldEnable = button.isSelected();
                }
                comp.setEnabled(shouldEnable);
                if (shouldEnable) {
                    comp.requestFocus();
                }
            }
        });
    }

}

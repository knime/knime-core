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
 *   02.09.2008 (thor): created
 */
package org.knime.base.node.meta.looper.condition;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.base.node.meta.looper.condition.LoopEndConditionSettings.Operator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * This class is the dialog for the condition loop tail node in which the user
 * can enter the condition.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopEndConditionNodeDialog extends NodeDialogPane {
    private static final Object[] NUMERIC_OPERATORS =
            {Operator.EQ, Operator.LE, Operator.LT, Operator.GE, Operator.GT,
                    Operator.NE};

    private static final Object[] STRING_OPERATORS = {Operator.EQ, Operator.NE};

    private final LoopEndConditionSettings m_settings =
            new LoopEndConditionSettings();

    private final DefaultComboBoxModel m_variablesModel =
            new DefaultComboBoxModel();

    private final JComboBox m_variables = new JComboBox(m_variablesModel);

    private final JComboBox m_operator = new JComboBox(NUMERIC_OPERATORS);

    private final JTextField m_value = new JTextField(5);

    private final JLabel m_selectedVariable = new JLabel();

    private final JCheckBox m_addLastRows =
            new JCheckBox("Collect rows from last iteration");

    private final JCheckBox m_addLastRowsOnly =
            new JCheckBox("Collect rows from last iteration only");

    private final JCheckBox m_addIterationColumn =
        new JCheckBox("Add iteration column");


    /**
     * Creates a new dialog.
     */
    public LoopEndConditionNodeDialog() {
        final JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 3, 2, 3);
        p.add(new JLabel("Available flow variables   "), c);

        c.gridx = 1;
        c.gridwidth = 2;
        m_variables.setRenderer(new FlowVariableListCellRenderer());
        m_variables.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (m_variables.getSelectedIndex() >= 0) {
                    FlowVariable v =
                            (FlowVariable)m_variablesModel.getSelectedItem();
                    m_operator.removeAllItems();
                    if (v.getType().equals(Type.STRING)) {
                        for (Object o : STRING_OPERATORS) {
                            m_operator.addItem(o);
                        }
                    } else {
                        for (Object o : NUMERIC_OPERATORS) {
                            m_operator.addItem(o);
                        }
                    }
                    m_selectedVariable.setText(v.getName());
                    p.revalidate();
                }
            }
        });
        p.add(m_variables, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Finish loop if selected variable is "), c);

        c.gridx = 1;
        p.add(m_operator, c);

        c.gridx = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(m_value, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.insets = new Insets(0, 0, 0, 0);

        p.add(m_addLastRows, c);

        c.gridy++;
        p.add(m_addLastRowsOnly, c);
        m_addLastRowsOnly.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_addLastRowsOnly.isSelected()) {
                    m_addLastRows.setSelected(true);
                    m_addLastRows.setEnabled(false);
                } else {
                    m_addLastRows.setEnabled(true);
                }
            }
        });

        c.gridy++;
        p.add(m_addIterationColumn, c);

        addTab("Default settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_variablesModel.removeAllElements();
        for (FlowVariable v : getAvailableFlowVariables().values()) {
            m_variablesModel.addElement(v);
            if (v.getName().equals(m_settings.variableName())) {
                m_variables.setSelectedItem(v);
            }
        }

        m_operator.setSelectedItem(m_settings.operator());
        m_value.setText(m_settings.value());
        m_addLastRows.setSelected(m_settings.addLastRows()
                || m_settings.addLastRowsOnly());
        m_addLastRowsOnly.setSelected(m_settings.addLastRowsOnly());
        m_addLastRows.setEnabled(!m_settings.addLastRowsOnly());
        m_addIterationColumn.setSelected(m_settings.addIterationColumn());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Object sel = m_variables.getSelectedItem();
        if (sel == null) {
            m_settings.variable(null);
        } else {
            m_settings.variable((FlowVariable)sel);
        }

        m_settings.operator((Operator)m_operator.getSelectedItem());
        m_settings.value(m_value.getText());
        m_settings.addLastRows(m_addLastRows.isSelected());
        m_settings.addLastRowsOnly(m_addLastRowsOnly.isSelected());
        m_settings.addIterationColumn(m_addIterationColumn.isSelected());

        m_settings.saveSettings(settings);
    }
}

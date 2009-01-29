/* ------------------------------------------------------------------
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
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ScopeVariableListCellRenderer;
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.node.workflow.ScopeVariable.Type;

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
        m_variables.setRenderer(new ScopeVariableListCellRenderer());
        m_variables.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (m_variables.getSelectedIndex() >= 0) {
                    ScopeVariable v =
                            (ScopeVariable)m_variablesModel.getSelectedItem();
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
        for (ScopeVariable v : getAvailableScopeVariables().values()) {
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
            m_settings.variable((ScopeVariable)sel);
        }

        m_settings.operator((Operator)m_operator.getSelectedItem());
        m_settings.value(m_value.getText());
        m_settings.addLastRows(m_addLastRows.isSelected());
        m_settings.addLastRowsOnly(m_addLastRowsOnly.isSelected());

        m_settings.saveSettings(settings);
    }
}

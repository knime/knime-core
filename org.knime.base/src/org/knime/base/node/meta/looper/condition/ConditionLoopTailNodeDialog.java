/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.base.node.meta.looper.condition.ConditionLoopTailSettings.Operator;
import org.knime.base.util.scopevariable.ScopeVariableListCellRenderer;
import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.node.workflow.ScopeVariable.Type;

/**
 * This class is the dialog for the condition loop tail node in which the user
 * can enter the condition.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ConditionLoopTailNodeDialog extends GenericNodeDialogPane {
    private static final Object[] NUMERIC_OPERATORS =
            {Operator.EQ, Operator.LE, Operator.LT, Operator.GE,
                    Operator.GT, Operator.NE};

    private static final Object[] STRING_OPERATORS =
            {Operator.EQ, Operator.NE};

    private final ConditionLoopTailSettings m_settings =
            new ConditionLoopTailSettings();

    private final DefaultListModel m_variablesModel = new DefaultListModel();

    private final JList m_variables = new JList(m_variablesModel);

    private final JComboBox m_operator = new JComboBox(NUMERIC_OPERATORS);

    private final JTextField m_value = new JTextField(5);

    private final JLabel m_selectedVariable = new JLabel();

    private final JCheckBox m_addLastRows =
            new JCheckBox("Collect rows from last iteration");

    /**
     * Creates a new dialog.
     */
    public ConditionLoopTailNodeDialog() {
        final JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        m_variables.setCellRenderer(new ScopeVariableListCellRenderer());
        m_variables.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_variables.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()
                        && m_variables.getSelectedIndex() >= 0) {
                    ScopeVariable v =
                            (ScopeVariable)m_variablesModel.get(m_variables
                                    .getSelectedIndex());
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

        JScrollPane sp = new JScrollPane(m_variables);
        sp.setBorder(BorderFactory.createTitledBorder("Available variables"));
        p.add(sp, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 3, 2, 3);
        c.gridwidth = 1;
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

        addTab("Default settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_variablesModel.clear();
        for (ScopeVariable v : getAvailableScopeVariables().values()) {
            m_variablesModel.addElement(v);
            if (v.getName().equals(m_settings.variableName())) {
                m_variables.setSelectedValue(v, true);
            }
        }

        m_operator.setSelectedItem(m_settings.operator());
        m_value.setText(m_settings.value());
        m_addLastRows.setSelected(m_settings.addLastRows());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Object sel = m_variables.getSelectedValue();
        if (sel == null) {
            m_settings.variable(null);
        } else {
            m_settings.variable((ScopeVariable)sel);
        }

        m_settings.operator((Operator)m_operator.getSelectedItem());
        m_settings.value(m_value.getText());
        m_settings.addLastRows(m_addLastRows.isSelected());

        m_settings.saveSettings(settings);
    }
}

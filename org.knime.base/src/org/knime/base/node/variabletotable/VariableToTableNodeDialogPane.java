/*
 * ------------------------------------------------------------------ *
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
 *   May 1, 2008 (wiswedel): created
 */
package org.knime.base.node.variabletotable;

import java.awt.Component;
import java.util.Arrays;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.util.Pair;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
class VariableToTableNodeDialogPane extends NodeDialogPane {

    private final JList m_list;

    /** Inits components. */
    public VariableToTableNodeDialogPane() {
        m_list = new JList(new DefaultListModel());
        m_list.setCellRenderer(new Renderer());
        m_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        addTab("Variable Selection", new JScrollPane(m_list));
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        Map<String, ScopeVariable> scopeVars = getAvailableScopeVariables();
        VariableToTableSettings sets = new VariableToTableSettings();
        sets.loadSettingsFrom(settings, scopeVars);
        DefaultListModel model = (DefaultListModel)m_list.getModel();
        model.removeAllElements();
        int[] selIndices = new int[sets.getVariablesOfInterest().size()];
        int current = 0;
        int pointer = 0;
        for (ScopeVariable v : scopeVars.values()) {
            model.addElement(v);
            if (sets.getVariablesOfInterest().contains(new Pair<
                    String, ScopeVariable.Type>(v.getName(), v.getType()))) {
                selIndices[pointer++] = current;
            }
            current += 1;
        }
        selIndices = Arrays.copyOf(selIndices, pointer);
        m_list.setSelectedIndices(selIndices);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Object[] sels = m_list.getSelectedValues();
        ScopeVariable[] svSels = new ScopeVariable[sels.length];
        System.arraycopy(sels, 0, svSels, 0, sels.length);
        VariableToTableSettings sets = new VariableToTableSettings();
        sets.setVariablesOfInterest(svSels);
        sets.saveSettingsTo(settings);
    }

    private static final class Renderer extends DefaultListCellRenderer {
        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            Component c =
                    super.getListCellRendererComponent(list, value, index,
                            isSelected, cellHasFocus);
            if (value instanceof ScopeVariable) {
                ScopeVariable v = (ScopeVariable)value;
                Icon icon;
                setText(v.getName());
                switch (v.getType()) {
                case DOUBLE:
                    icon = DoubleValue.UTILITY.getIcon();
                    break;
                case INTEGER:
                    icon = IntValue.UTILITY.getIcon();
                    break;
                case STRING:
                    icon = StringValue.UTILITY.getIcon();
                    break;
                default:
                    icon = DataValue.UTILITY.getIcon();
                }
                setIcon(icon);
            }
            return c;
        }
    }

}

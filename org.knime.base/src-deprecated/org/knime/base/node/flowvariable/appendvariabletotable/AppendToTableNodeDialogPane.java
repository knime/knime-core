/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   May 1, 2008 (wiswedel): created
 */
package org.knime.base.node.flowvariable.appendvariabletotable;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.Pair;

/**
 *
 * @author wiswedel, University of Konstanz
 */
@Deprecated
class AppendToTableNodeDialogPane extends NodeDialogPane {

    private final JCheckBox m_all;

    private final JList m_list;

    /** Inits components. */
    public AppendToTableNodeDialogPane() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        m_all = new JCheckBox("Include all flow variables");
        panel.add(m_all, gbc);
        m_list = new JList(new DefaultListModel());
        m_list.setCellRenderer(new FlowVariableListCellRenderer());
        m_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy++;
        panel.add(m_list, gbc);
        m_all.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_list.setEnabled(!m_all.isSelected());
            }
        });
        addTab("Variable Selection", new JScrollPane(panel));
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        Map<String, FlowVariable> scopeVars = getAvailableFlowVariables();
        AppendVariableToTableSettings sets = new AppendVariableToTableSettings();
        sets.loadSettingsFrom(settings, scopeVars);
        DefaultListModel model = (DefaultListModel)m_list.getModel();
        model.removeAllElements();
        int[] selIndices = new int[sets.getVariablesOfInterest().size()];
        int current = 0;
        int pointer = 0;
        for (FlowVariable v : scopeVars.values()) {
            model.addElement(v);
            if (sets.getVariablesOfInterest().contains(new Pair<String, FlowVariable.Type>(v.getName(), v.getType()))) {
                selIndices[pointer++] = current;
            }
            current += 1;
        }
        selIndices = Arrays.copyOf(selIndices, pointer);
        m_list.setSelectedIndices(selIndices);
        m_all.setSelected(sets.getIncludeAll());
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        Object[] sels = m_list.getSelectedValues();
        FlowVariable[] svSels = new FlowVariable[sels.length];
        System.arraycopy(sels, 0, svSels, 0, sels.length);
        AppendVariableToTableSettings sets = new AppendVariableToTableSettings();
        sets.setIncludeAll(m_all.isSelected());
        sets.setVariablesOfInterest(svSels);
        sets.saveSettingsTo(settings);
    }

}

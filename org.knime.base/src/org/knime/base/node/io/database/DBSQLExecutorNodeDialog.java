/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 *
 * History
 *   02.02.2005 (cebron): created
 */
package org.knime.base.node.io.database;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import org.knime.base.util.flowvariable.FlowVariableResolver;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Dialog for choosing the columns that will be sorted. It is also possible to set the order of columns
 *
 * @author Nicolas Cebron, University of Konstanz
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class DBSQLExecutorNodeDialog extends NodeDialogPane {

    private JList<FlowVariable> m_variables = new JList<FlowVariable>(new DefaultListModel<FlowVariable>());

    private JTextArea m_text = new JTextArea(6, 40);

    /**
     *
     */
    DBSQLExecutorNodeDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.7;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("SQL Statement:"), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(new JScrollPane(m_text), gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        panel.add(new JScrollPane(m_variables), gbc);
        m_variables.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_variables.setCellRenderer(new FlowVariableListCellRenderer());
        m_variables.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public final void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    FlowVariable var = m_variables.getSelectedValue();
                    if (var != null) {
                        m_text.replaceSelection(FlowVariableResolver.getPlaceHolderForVariable(var));
                        m_variables.clearSelection();
                        m_text.requestFocus();
                    }
                }
            }
        });
        addTab("Config", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_text.setText(settings.getString(DBSQLExecutorNodeModel.CFG_STATEMENT, ""));
        DefaultListModel<FlowVariable> listModel = (DefaultListModel<FlowVariable>)m_variables.getModel();
        listModel.removeAllElements();
        for (FlowVariable var : getAvailableFlowVariables().values()) {
            listModel.addElement(var);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addString(DBSQLExecutorNodeModel.CFG_STATEMENT, m_text.getText());
    }
}

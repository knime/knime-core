/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   26.09.2014 (koetter): created
 */
package org.knime.base.node.io.database.drop;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Drop table dialog.
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 */
public class DBDropTableNodeDialog extends NodeDialogPane {

    private final SettingsModelString m_tableNameModel = DBDropTableNodeModel.createTableNameModel();
    private final DialogComponentString m_tableNameComp = new DialogComponentString(m_tableNameModel, null, true, 35);
    private SettingsModelBoolean m_cascadeModel = DBDropTableNodeModel.createCascadeModel();
    private SettingsModelBoolean m_failIfNotExistsModel = DBDropTableNodeModel.createFailIfNotExistsModel();
    private DialogComponentBoolean m_failIfNotExistsComp = new DialogComponentBoolean(m_failIfNotExistsModel,
            "Fail if table does not exist");
    private DialogComponentBoolean m_cascadeComp = new DialogComponentBoolean(m_cascadeModel,
        "<html>cascade <b>(WARNING: This might drop dependent objects!)</b></html>");

    /**
     * Constructor.
     */
    public DBDropTableNodeDialog() {
        /* add flow variable button for the table name */
        final FlowVariableModel fvm =
                createFlowVariableModel(DBDropTableNodeModel.CFG_TABLE_NAME, FlowVariable.Type.STRING);
        fvm.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent evt) {
                final FlowVariableModel vm = (FlowVariableModel)(evt.getSource());
                m_tableNameModel.setEnabled(!vm.isVariableReplacementEnabled());
                if (vm.isVariableReplacementEnabled()) {
                    m_tableNameModel.setStringValue(vm.getInputVariableName());
                }
            }
        });
        final FlowVariableModelButton variableModelButton = new FlowVariableModelButton(fvm);
        final JPanel root = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        root.add(new JLabel("Table name:"), c);
        c.gridx++;
        root.add(m_tableNameComp.getComponentPanel(), c);
        c.gridx++;
        root.add(variableModelButton, c);
        //add a dummy label that stretches itself
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        root.add(new JLabel(), c);

        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        root.add(m_cascadeComp.getComponentPanel(), c);
        //add a dummy label that stretches itself
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        root.add(new JLabel(), c);



        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        root.add(m_failIfNotExistsComp.getComponentPanel(), c);
        //add a dummy label that stretches itself
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        root.add(new JLabel(), c);

        addTab(" Settings ", root);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        m_tableNameComp.loadSettingsFrom(settings, specs);
        m_cascadeComp.loadSettingsFrom(settings, specs);
        m_failIfNotExistsComp.loadSettingsFrom(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_tableNameComp.saveSettingsTo(settings);
        m_cascadeComp.saveSettingsTo(settings);
        m_failIfNotExistsComp.saveSettingsTo(settings);
    }
}
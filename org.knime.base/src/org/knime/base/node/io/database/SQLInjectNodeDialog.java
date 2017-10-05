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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.io.database;

import java.util.Collection;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFlowVariableNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * <code>NodeDialog</code> for the "SQL Inject" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Alexander Fillbrunn, University of Konstanz, Germany
 * @since 2.10
 */
public class SQLInjectNodeDialog extends DefaultNodeSettingsPane {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SQLInjectNodeDialog.class);

    private SettingsModelString m_flowVarSettingsModel;
    private DialogComponentFlowVariableNameSelection m_flowVarSelection;

    /**
     * New pane for configuring the SQLInject node.
     */
    protected SQLInjectNodeDialog() {
        Collection<FlowVariable> flowVars = getAvailableFlowVariables().values();
        m_flowVarSettingsModel = SQLInjectNodeModel.createFlowVariableNameSettingsModel();

        m_flowVarSelection = new DialogComponentFlowVariableNameSelection(
                                      m_flowVarSettingsModel, "Flow Variable with SQL Query",
                                      flowVars, false, FlowVariable.Type.STRING);

        addDialogComponent(m_flowVarSelection);
    }

    /**
     * List of available string flow variables must be updated since it could
     * have changed.
     *
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        Map<String, FlowVariable> flowVars = getAvailableFlowVariables();

        // check for selected value
        String flowVar = "";
        try {
            flowVar = ((SettingsModelString)m_flowVarSettingsModel
                            .createCloneWithValidatedValue(settings))
                            .getStringValue();
        } catch (InvalidSettingsException e) {
            LOGGER.debug("Settings model could not be cloned with given settings!");
        } finally {
            m_flowVarSelection.replaceListItems(flowVars.values(), flowVar);
        }
    }
}


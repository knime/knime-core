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
package org.knime.base.node.flowvariable.variabletotablerow2;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.util.filter.variable.FlowVariableFilterPanel;

/** Dialog for the "Variable To TableRow" node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 *
 * @since 2.9
 */
class VariableToTable2NodeDialogPane extends NodeDialogPane {

    private final FlowVariableFilterPanel m_filter;
    private final DialogComponentString m_rowID;

    /** Inits components. */
    public VariableToTable2NodeDialogPane() {
        m_filter = new FlowVariableFilterPanel();
        m_rowID = new DialogComponentString(createSettingsModelRowID(), "Name of RowID: ");
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(m_filter, BorderLayout.CENTER);
        final JPanel panelRowID = new JPanel(new BorderLayout());
        panelRowID.setBorder(BorderFactory.createTitledBorder(" Generated RowID "));
        panelRowID.add(m_rowID.getComponentPanel(), BorderLayout.CENTER);
        panel.add(panelRowID, BorderLayout.SOUTH);
        addTab("Variable Selection", new JScrollPane(panel));
    }

    /** @return settings model to set the RowID name
     */
    static SettingsModelString createSettingsModelRowID() {
        return new SettingsModelString("row-id", "values");
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        m_rowID.loadSettingsFrom(settings, specs);
        FlowVariableFilterConfiguration config =
            new FlowVariableFilterConfiguration(VariableToTable2NodeModel.CFG_KEY_FILTER);
        config.loadConfigurationInDialog(settings, getAvailableFlowVariables());
        m_filter.loadConfiguration(config, getAvailableFlowVariables());
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_rowID.saveSettingsTo(settings);
        FlowVariableFilterConfiguration config =
            new FlowVariableFilterConfiguration(VariableToTable2NodeModel.CFG_KEY_FILTER);
        m_filter.saveConfiguration(config);
        config.saveConfiguration(settings);
    }

}

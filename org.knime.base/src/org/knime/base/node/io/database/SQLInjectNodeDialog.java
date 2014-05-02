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
 * @author Alexander Fillbrunn
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
                                      flowVars, true, FlowVariable.Type.STRING);

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


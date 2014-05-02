package org.knime.base.node.io.database;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "SQLExtract" Node.
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
public class SQLExtractNodeDialog extends DefaultNodeSettingsPane {
    /**
     * New pane for configuring the SQLInject node.
     */
    protected SQLExtractNodeDialog() {
        SettingsModelString flowVarSettingsModel = SQLExtractNodeModel.createFlowVariableNameSettingsModel();

        DialogComponentString flowVarSelection =
                new DialogComponentString(flowVarSettingsModel, "Flow Variable with SQL Query");

        addDialogComponent(flowVarSelection);
    }
}


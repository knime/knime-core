package org.knime.base.node.flowvariable.tablecoltovariable;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.util.ColumnFilterPanel.ValueClassFilter;

/**
 * <code>NodeDialog</code> for the "TableColumnToVariable" Node. Converts the values from a table column to flow
 * variables with the row ids as their variable name.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Gabor Bakos
 */
public class TableColumnToVariableNodeDialog extends DefaultNodeSettingsPane {
    /**
     * New pane for configuring the TableColumnToVariable node.
     */
    protected TableColumnToVariableNodeDialog() {
        @SuppressWarnings("unchecked")
        final ValueClassFilter columnFilter = new ValueClassFilter(DoubleValue.class, StringValue.class);
        addDialogComponent(new DialogComponentColumnNameSelection(
            TableColumnToVariableNodeModel.createColumnSettings(), "Column name", 0, true, columnFilter));
        final DialogComponentBoolean ignoreMissing =
            new DialogComponentBoolean(TableColumnToVariableNodeModel.createIgnoreMissing(), "Skip missing values");
        ignoreMissing
            .setToolTipText("When unchecked, the execution fails when a missing value is in the input column.");
        addDialogComponent(ignoreMissing);
    }
}

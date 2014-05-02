package org.knime.base.node.io.database;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;

/**
 * This is the model implementation of SQLExtract.
 *
 * @author Alexander Fillbrunn
 * @since 2.10
 */
public class SQLExtractNodeModel extends NodeModel {

    private static final String CFG_FLOW_VAR = "flowVarName";

    /**
     * Constructor for the node model.
     */
    protected SQLExtractNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE},
              new PortType[]{FlowVariablePortObject.TYPE, BufferedDataTable.TYPE});
    }

    /**
     * Creates a settings model for the flow variable which contains the SQL.
     * @return the settings model
     */
    static SettingsModelString createFlowVariableNameSettingsModel() {
        return new SettingsModelString(CFG_FLOW_VAR, "sql");
    }

    private SettingsModelString m_flowVariableName = createFlowVariableNameSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {

        DatabasePortObject dbIn = (DatabasePortObject)inData[0];
        String query = dbIn.getConnectionSettings(getCredentialsProvider()).getQuery();

        final String flowVarName = m_flowVariableName.getStringValue();
        pushFlowVariableString(flowVarName, query);

        // Create a table with the SQL query in a StringCell
        DataContainer container = exec.createDataContainer(createSpec(flowVarName));
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new StringCell(query)));
        container.close();
        BufferedDataTable outTable = (BufferedDataTable)container.getTable();

        return new PortObject[]{FlowVariablePortObject.INSTANCE, outTable};
    }

    private DataTableSpec createSpec(final String name) {
        final DataColumnSpec colSpec = new DataColumnSpecCreator(name, StringCell.TYPE).createSpec();
        return new DataTableSpec(colSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        final String flowVarName = m_flowVariableName.getStringValue();
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE, createSpec(flowVarName)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_flowVariableName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_flowVariableName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_flowVariableName.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}


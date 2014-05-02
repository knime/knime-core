package org.knime.base.node.io.database;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.port.database.DatabaseConnectionPortObject;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseReaderConnection;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;

/**
 * This is the model implementation of "SQL Inject" node.
 *
 * @author Alexander Fillbrunn
 * @since 2.10
 */
public class SQLInjectNodeModel extends NodeModel {

    private static final String CFG_FLOW_VAR = "flowVarName";

    /**
     * Constructor for the node model.
     */
    protected SQLInjectNodeModel() {
        super(new PortType[]{DatabaseConnectionPortObject.TYPE, FlowVariablePortObject.TYPE},
              new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * Creates a settings model for the flow variable which contains the SQL.
     * @return the settings model
     */
    static SettingsModelString createFlowVariableNameSettingsModel() {
        return new SettingsModelString(CFG_FLOW_VAR, null);
    }

    private SettingsModelString m_flowVariableName = createFlowVariableNameSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        DatabaseConnectionPortObject dbIn = (DatabaseConnectionPortObject)inData[0];
        String sql = peekFlowVariableString(m_flowVariableName.getStringValue());

        DatabaseConnectionSettings inSettings = dbIn.getConnectionSettings(getCredentialsProvider());
        // Attach the SQL query
        DatabaseQueryConnectionSettings outSettings = new DatabaseQueryConnectionSettings(inSettings, sql);

        // Probe the database to see how the result table looks like
        DatabaseReaderConnection load = new DatabaseReaderConnection(outSettings);
        DataTableSpec tableSpec = load.getDataTableSpec(getCredentialsProvider());
        DatabasePortObjectSpec outSpec = new DatabasePortObjectSpec(tableSpec, outSettings.createConnectionModel());

        return new PortObject[]{new DatabasePortObject(outSpec)};
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
        // We do not know the SQL query yet, so we cannot retrieve the DataTableSpec and therefore cannot
        // produce a DatabasePortObjectSpec
        return new PortObjectSpec[]{null};
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


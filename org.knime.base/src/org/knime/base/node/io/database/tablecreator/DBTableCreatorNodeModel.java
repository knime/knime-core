package org.knime.base.node.io.database.tablecreator;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.knime.base.node.io.database.DBNodeModel;
import org.knime.base.node.io.database.tablecreator.util.DBTableCreatorConfiguration;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseConnectionPortObject;
import org.knime.core.node.port.database.DatabaseConnectionPortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.tablecreator.DBColumn;
import org.knime.core.node.port.database.tablecreator.DBKey;
import org.knime.core.node.port.database.tablecreator.DBTableCreator;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;

/**
 * This is the model implementation of DBTableCreator.
 *
 * @author Budi Yanto, KNIME.com
 */
public class DBTableCreatorNodeModel extends DBNodeModel {

    private static final String FLOW_VARIABLE_SCHEMA = "schema";

    private static final String FLOW_VARIABLE_TABLE_NAME = "tableName";

    private final DBTableCreatorConfiguration m_config = new DBTableCreatorConfiguration();

    /**
     * Constructor for the node model.
     */
    protected DBTableCreatorNodeModel() {
        super(new PortType[]{DatabaseConnectionPortObject.TYPE, BufferedDataTable.TYPE_OPTIONAL},
            new PortType[]{FlowVariablePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {

        final List<DBColumn> columns = m_config.getColumns();
        final List<DBKey> keys = m_config.getKeys();

        final DatabaseConnectionSettings conn = ((DatabaseConnectionPortObject)inData[0]).getConnectionSettings(getCredentialsProvider());
        final DBTableCreator tableCreator = conn.getUtility().getTableCreator(conn);
        tableCreator.createTable(getCredentialsProvider(), m_config.getSchema(), getTableName(), m_config.isTempTable(), m_config.ifNotExists(),
            columns.toArray(new DBColumn[columns.size()]), keys.toArray(new DBKey[keys.size()]));

        pushFlowVariables();
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[0] == null && !(inSpecs[0] instanceof DatabaseConnectionPortObjectSpec)) {
            throw new InvalidSettingsException("No valid database connection available.");
        }

        m_config.setTableSpec((DataTableSpec) inSpecs[1]);
        final boolean isColumnsEmpty = m_config.getColumns().isEmpty();

        if(m_config.getTableSpec() != null && (m_config.useDynamicSettings() || isColumnsEmpty)) {
            m_config.loadColumnSettingsFromTableSpec(m_config.getTableSpec());
            m_config.updateKeysWithDynamicSettings();
        }

        if (isColumnsEmpty) {
            throw new InvalidSettingsException("At least one column must be defined.");
        }
        pushFlowVariables();
        return new FlowVariablePortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettings(settings);
    }

    private void pushFlowVariables() {
        pushFlowVariableString(FLOW_VARIABLE_SCHEMA, m_config.getSchema());
        pushFlowVariableString(FLOW_VARIABLE_TABLE_NAME, getTableName());
    }

    private String getTableName() {
        return (StringUtils.isBlank(m_config.getTableName()))
                ? DBTableCreatorConfiguration.DEF_TABLE_NAME : m_config.getTableName();
    }
}

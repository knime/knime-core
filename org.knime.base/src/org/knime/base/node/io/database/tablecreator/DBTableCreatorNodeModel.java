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
            new PortType[]{DatabaseConnectionPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[0] == null && !(inSpecs[0] instanceof DatabaseConnectionPortObjectSpec)) {
            throw new InvalidSettingsException("No valid database connection available.");
        }

        final DatabaseConnectionPortObjectSpec dbSpec = (DatabaseConnectionPortObjectSpec) inSpecs[0];

        m_config.setTableSpec((DataTableSpec) inSpecs[1]);
        final boolean isColumnsEmpty = m_config.getColumns().isEmpty();

        if(m_config.getTableSpec() != null && (m_config.useDynamicSettings() || isColumnsEmpty)) {
            m_config.loadColumnSettingsFromTableSpec(m_config.getTableSpec());
            m_config.updateKeysWithDynamicSettings();
        }

        if (m_config.getTableSpec() == null && m_config.useDynamicSettings()) {
            throw new InvalidSettingsException("Dynamic settings enabled but no input table available.");
        }

        if (isColumnsEmpty) {
            throw new InvalidSettingsException("At least one column must be defined.");
        }

        final DatabaseConnectionSettings conn = dbSpec.getConnectionSettings(getCredentialsProvider());
        final DBTableCreator tableCreator = conn.getUtility().getTableCreator(m_config.getSchema(), getTableName(),
            m_config.isTempTable());
        final List<DBColumn> columns = m_config.getColumns();
        final List<DBKey> keys = m_config.getKeys();
        try {
            tableCreator.validateSettings(m_config.ifNotExists(),
                columns.toArray(new DBColumn[columns.size()]), keys.toArray(new DBKey[keys.size()]),
                m_config.getAdditionalOptions());
        } catch (Exception e) {
            throw new InvalidSettingsException(e.getMessage());
        }
        pushFlowVariables(tableCreator.getSchema(), tableCreator.getTableName());
        return new PortObjectSpec[]{dbSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {

        exec.setMessage("Creating table");
        final DatabaseConnectionPortObject dbConn = (DatabaseConnectionPortObject)inData[0];
        final DatabaseConnectionSettings conn =
                dbConn.getConnectionSettings(getCredentialsProvider());
        final DBTableCreator tableCreator =
                conn.getUtility().getTableCreator(m_config.getSchema(), getTableName(), m_config.isTempTable());
        final List<DBColumn> columns = m_config.getColumns();
        final List<DBKey> keys = m_config.getKeys();
        tableCreator.createTable(conn, getCredentialsProvider(), m_config.ifNotExists(),
            columns.toArray(new DBColumn[columns.size()]), keys.toArray(new DBKey[keys.size()]),
            m_config.getAdditionalOptions());
        pushFlowVariables(tableCreator.getSchema(), tableCreator.getTableName());
        final String warning = tableCreator.getWarning();
        if (!StringUtils.isBlank(warning)) {
            setWarningMessage(warning);
        }
        return new PortObject[]{dbConn};
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }

    /**
     * Push the schema and table name flow variables
     * @param schema the schema to push
     * @param tableName the table name to push
     */
    private void pushFlowVariables(final String schema, final String tableName) {
        pushFlowVariableString(FLOW_VARIABLE_SCHEMA, schema);
        pushFlowVariableString(FLOW_VARIABLE_TABLE_NAME, tableName);
    }

    private String getTableName() {
        return (StringUtils.isBlank(m_config.getTableName()))
                ? DBTableCreatorConfiguration.DEF_TABLE_NAME : m_config.getTableName();
    }
}

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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseReaderConnection;
import org.knime.core.node.port.database.DatabaseUtility;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;

/**
 * @author Thomas Gabriel, University of Konstanz
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
final class DBGroupByNodeModel extends DBNodeModel {

    /**
     * Config key for the columns that will be grouped.
     */
    static final String CFG_GROUP_BY_COLUMNS = "groupByColumns";

    /**
     * Config key for the name policy of aggregated columns.
     */
    static final String CFG_COLUMN_NAME_POLICY = "columnNamePolicy";

    /**
     * Config key for columns that will be aggregated.
     */
    static final String CFG_AGGREGATED_COLUMNS = "aggregatedColumns";

    /**
     * Config key for the aggregation methods applied to the columns.
     */
    static final String CFG_AGGREGATION_METHODS = "aggregationMethods";

    private final SettingsModelFilterString m_groupByCols = new SettingsModelFilterString(CFG_GROUP_BY_COLUMNS);

    private final SettingsModelString m_columnNamePolicy = new SettingsModelString(CFG_COLUMN_NAME_POLICY,
        ColumnNamePolicy.getDefault().getLabel());

    private String[] m_aggregatedColumns = new String[0];

    private String[] m_aggregationMethods = new String[0];

    /**
     * Creates a new database group by.
     */
    DBGroupByNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_groupByCols.saveSettingsTo(settings);
        m_columnNamePolicy.saveSettingsTo(settings);
        settings.addStringArray(CFG_AGGREGATED_COLUMNS, m_aggregatedColumns);
        settings.addStringArray(CFG_AGGREGATION_METHODS, m_aggregationMethods);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_groupByCols.validateSettings(settings);
        m_columnNamePolicy.validateSettings(settings);
        settings.getStringArray(CFG_AGGREGATED_COLUMNS);
        settings.getStringArray(CFG_AGGREGATION_METHODS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_groupByCols.loadSettingsFrom(settings);
        m_columnNamePolicy.loadSettingsFrom(settings);
        String[] aggregationColumns = settings.getStringArray(CFG_AGGREGATED_COLUMNS);
        String[] aggregationMethods = settings.getStringArray(CFG_AGGREGATION_METHODS);
        m_aggregatedColumns = aggregationColumns;
        m_aggregationMethods = aggregationMethods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, Exception {
        exec.setMessage("Retrieving metadata from database");
        DatabasePortObject dbObject = (DatabasePortObject)inData[0];
        DatabasePortObject outObject =
            new DatabasePortObject(createDbOutSpec(dbObject.getSpec(), false));
        return new PortObject[]{outObject};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createDbOutSpec((DatabasePortObjectSpec)inSpecs[0], true)};
    }

    /**
     * @param inSpec Spec of the input database object
     * @param checkRetrieveMetadata true if the retrieveMetadataInConfigure settings should be respected,
     *            <code>false</code> if the metadata should be retrieved in any case (for execute)
     * @return Spec of the output database object
     * @throws InvalidSettingsException If the current settings are invalid
     */
    private DatabasePortObjectSpec createDbOutSpec(final DatabasePortObjectSpec inSpec,
        final boolean checkRetrieveMetadata) throws InvalidSettingsException {
        if (m_groupByCols.getIncludeList().isEmpty() && m_aggregatedColumns.length == 0) {
            throw new InvalidSettingsException("Please select at least one group or aggregation column");
        }
        DatabaseQueryConnectionSettings connection = inSpec.getConnectionSettings(getCredentialsProvider());
        String newQuery = createQuery(connection.getQuery(), connection.getUtility().getStatementManipulator());
        connection = createDBQueryConnection(inSpec, newQuery);

        if (checkRetrieveMetadata && !connection.getRetrieveMetadataInConfigure()) {
            return null;
        }

        DataTableSpec tableSpec = createOutSpec(inSpec.getDataTableSpec(), connection, newQuery, checkRetrieveMetadata);
        return new DatabasePortObjectSpec(tableSpec, connection.createConnectionModel());
    }

    /**
     * @param query Query for the input table
     * @param manipulator Statement manipulator for the current database
     * @return SQL query that applies a group by to the input query
     */
    private String createQuery(final String query, final StatementManipulator manipulator) {
        final StringBuilder buf = new StringBuilder();
        final String[] queries = query.split(DatabaseReaderConnection.SQL_QUERY_SEPARATOR);
        for (int i = 0; i < queries.length - 1; i++) {
            buf.append(queries[i]);
            buf.append(DatabaseReaderConnection.SQL_QUERY_SEPARATOR);
        }
        final String selectQuery = queries[queries.length - 1];
        // Build identifier for input table
        String tableName = "table_" + System.identityHashCode(this);
        final StringBuilder columnBuf = new StringBuilder();
        List<String> groupByCols = m_groupByCols.getIncludeList();
        // Add group by columns
        for (int i = 0; i < groupByCols.size(); i++) {
            columnBuf.append(manipulator.quoteColumn(groupByCols.get(i)));
            if (i + 1 < groupByCols.size() || m_aggregatedColumns.length > 0) {
                columnBuf.append(", ");
            }
        }
        // Add aggregated columns
        for (int i = 0; i < m_aggregatedColumns.length; i++) {
            columnBuf.append(m_aggregationMethods[i] + "(");
            columnBuf.append(tableName + "." + manipulator.quoteColumn(m_aggregatedColumns[i]));
            columnBuf.append(")");
            columnBuf.append(" AS ");
            columnBuf.append(manipulator
                .quoteColumn(generateColumnName(m_aggregatedColumns[i], m_aggregationMethods[i])));
            if (i + 1 < m_aggregatedColumns.length) {
                columnBuf.append(", ");
            }
        }
        buf.append("SELECT " + columnBuf.toString() + " FROM (" + selectQuery + ") " + tableName);
        // build GROUP BY clause
        if (!groupByCols.isEmpty()) {
            buf.append(" GROUP BY ");
        }
        for (int i = 0; i < groupByCols.size(); i++) {
            buf.append(manipulator.quoteColumn(groupByCols.get(i)));
            if (i + 1 < groupByCols.size()) {
                buf.append(", ");
            }
        }
        return buf.toString();
    }

    /**
     * @param inSpec Spec of the input table
     * @param checkRetrieveMetadata
     * @return Spec of the output table
     * @throws InvalidSettingsException if settings do not match the input specification
     */
    private DataTableSpec createOutSpec(final DataTableSpec inSpec, final DatabaseConnectionSettings settings,
        final String query, final boolean ignoreExceptions) throws InvalidSettingsException {
        // Try get spec from database
        try {
            DatabaseQueryConnectionSettings querySettings = new DatabaseQueryConnectionSettings(settings, query);
            DatabaseReaderConnection conn = new DatabaseReaderConnection(querySettings);
            return conn.getDataTableSpec(getCredentialsProvider());
        } catch (SQLException e) {
            NodeLogger.getLogger(getClass()).info("Could not determine table spec from database, trying to guess now",
                e);
            if (!ignoreExceptions) {
                throw new InvalidSettingsException("Error in automatically build sql statement: " + e.getMessage());
            }
            // Otherwise guess spec
        }
        List<DataColumnSpec> colSpecs = new ArrayList<>();
        // Add all group by columns
        for (String col : m_groupByCols.getIncludeList()) {
            colSpecs.add(inSpec.getColumnSpec(col));
        }
        // Add aggregated columns
        for (int i = 0; i < m_aggregatedColumns.length; i++) {
            String col = m_aggregatedColumns[i];
            String method = m_aggregationMethods[i];

            if (inSpec.getColumnSpec(col) == null) {
                throw new InvalidSettingsException("Column '" + col + "' in aggregation " + method + " does not exist");
            }
            final DatabaseUtility databaseUtility = settings.getUtility();
            final DBAggregationFunction function = databaseUtility.getAggregationFunction(method);
            // Get type of column after aggregation
            DataType type = function.getType(inSpec.getColumnSpec(col).getType());
            colSpecs.add(new DataColumnSpecCreator(generateColumnName(col, method), type).createSpec());
        }
        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
    }

    /**
     * @param columnName Original column name
     * @param method Name of the aggregation method
     * @return New column name based on the naming policy
     */
    private String generateColumnName(final String columnName, final String method) {
        switch (ColumnNamePolicy.getPolicy4Label(m_columnNamePolicy.getStringValue())) {
            case KEEP_ORIGINAL_NAME:
                return columnName;
            case AGGREGATION_METHOD_COLUMN_NAME:
                return method + "(" + columnName + ")";
            case COLUMN_NAME_AGGREGATION_METHOD:
                return columnName + "(" + method + ")";
            default:
                return columnName;
        }
    }

}

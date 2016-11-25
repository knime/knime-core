/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.database.groupby;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.knime.base.node.io.database.DBNodeModel;
import org.knime.base.node.io.database.groupby.dialog.column.DBColumnAggregationFunctionRow;
import org.knime.base.node.io.database.groupby.dialog.pattern.DBPatternAggregationFunctionRow;
import org.knime.base.node.io.database.groupby.dialog.type.DBDataTypeAggregationFunctionRow;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseUtility;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.aggregation.AggregationFunction;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.InvalidAggregationFunction;
import org.knime.core.node.port.database.reader.DBReader;

/**
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 */
final class DBGroupByNodeModel2 extends DBNodeModel {

    /**
     * Config key for the add count star option.
     */
    static final String CFG_ADD_COUNT_STAR = "addCountStar";

    /**
     * Config key for the columns that will be grouped.
     */
    static final String CFG_GROUP_BY_COLUMNS = "groupByColumns";

    /**
     * Config key for the name policy of aggregated columns.
     */
    static final String CFG_COLUMN_NAME_POLICY = "columnNamePolicy";

    /**
     * Config key for the manual aggregation methods applied to the columns.
     */
    static final String CFG_AGGREGATION_FUNCTIONS = "manualAggregationFunctions";

    /**
     * Config key for the pattern based aggregation methods applied to the columns.
     */
    static final String CFG_PATTERN_AGGREGATION_FUNCTIONS = "patternAggregationFunctions";

    /**
     * Config key for the type based aggregation methods applied to the columns.
     */
    static final String CFG_TYPE_AGGREGATION_FUNCTIONS = "typeAggregationFunctions";

    private final SettingsModelBoolean m_addCountStar = new SettingsModelBoolean(CFG_ADD_COUNT_STAR, false);

    private final SettingsModelString m_countStarColName = createCountStarColNameModel();

    /**
     * @return the count star result column name
     */
    static SettingsModelString createCountStarColNameModel() {
        final SettingsModelString model = new SettingsModelString("countStarColName", "COUNT(*)");
        model.setEnabled(false);
        return model;
    }

    private final SettingsModelFilterString m_groupByCols = new SettingsModelFilterString(CFG_GROUP_BY_COLUMNS);

    private final SettingsModelString m_columnNamePolicy = new SettingsModelString(CFG_COLUMN_NAME_POLICY,
        ColumnNamePolicy.getDefault().getLabel());

    private final List<DBColumnAggregationFunctionRow> m_aggregationFunction2Use = new LinkedList<>();

    private NodeSettings m_settings = null;

    /**
     * Creates a new database group by.
     */
    DBGroupByNodeModel2() {
        super(new PortType[]{DatabasePortObject.TYPE}, new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        if (m_settings != null) {
            m_settings.copyTo(settings);
        } else {
            //initialize the node settings
            m_addCountStar.saveSettingsTo(settings);
            m_countStarColName.saveSettingsTo(settings);
            m_groupByCols.saveSettingsTo(settings);
            m_columnNamePolicy.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_groupByCols.validateSettings(settings);
        final boolean addCountStar =
                ((SettingsModelBoolean)m_addCountStar.createCloneWithValidatedValue(settings)).getBooleanValue();
        final String colName =
                ((SettingsModelString)m_countStarColName.createCloneWithValidatedValue(settings)).getStringValue();
        if (addCountStar && (colName == null || colName.isEmpty())) {
            throw new IllegalArgumentException("Please specify the count(*) column name");
        }
        if (ColumnNamePolicy.getPolicy4Label(((SettingsModelString)m_columnNamePolicy.createCloneWithValidatedValue(
            settings)).getStringValue()) == null) {
            throw new InvalidSettingsException("Invalid column name policy");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_addCountStar.loadSettingsFrom(settings);
        m_countStarColName.loadSettingsFrom(settings);
        m_groupByCols.loadSettingsFrom(settings);
        m_columnNamePolicy.loadSettingsFrom(settings);
        m_settings = new NodeSettings("copy");
        settings.copyTo(m_settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, Exception {
        exec.setMessage("Retrieving metadata from database");
        final DatabasePortObject dbObject = (DatabasePortObject)inData[0];
        final DatabasePortObject outObject = new DatabasePortObject(createDbOutSpec(dbObject.getSpec(), false));
        return new PortObject[]{outObject};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec)inSpecs[0];
        final DataTableSpec tableSpec = dbSpec.getDataTableSpec();
        final DatabaseQueryConnectionSettings connection = dbSpec.getConnectionSettings(null);
        final String dbIdentifier = connection.getDatabaseIdentifier();
        final List<DBColumnAggregationFunctionRow> columnFunctions = DBColumnAggregationFunctionRow.loadFunctions(
            m_settings, DBGroupByNodeModel2.CFG_AGGREGATION_FUNCTIONS, dbIdentifier, tableSpec);
        final ArrayList<DBColumnAggregationFunctionRow> invalidColAggrs = new ArrayList<>(1);
        final Set<String> usedColNames = new HashSet<>(tableSpec.getNumColumns());
        usedColNames.addAll(m_groupByCols.getIncludeList());
        m_aggregationFunction2Use.clear();
        for (DBColumnAggregationFunctionRow row : columnFunctions) {
            final DataColumnSpec columnSpec = row.getColumnSpec();
            final DataColumnSpec inputSpec = tableSpec.getColumnSpec(columnSpec.getName());
            final AggregationFunction function = row.getFunction();
            if (inputSpec == null || !inputSpec.getType().equals(columnSpec.getType())) {
                invalidColAggrs.add(row);
                continue;
            }
            if (function instanceof InvalidAggregationFunction) {
                throw new InvalidSettingsException(((InvalidAggregationFunction)function).getErrorMessage());
            }
            if (function.hasOptionalSettings()) {
                try {
                    function.configure(tableSpec);
                } catch (InvalidSettingsException e) {
                    throw new InvalidSettingsException("Exception in aggregation function " + function.getLabel()
                        + " of column " + row.getColumnSpec().getName() + ": " + e.getMessage());
                }
            }
            usedColNames.add(row.getColumnSpec().getName());
            m_aggregationFunction2Use.add(row);
        }
        final List<DBPatternAggregationFunctionRow> patternFunctions = DBPatternAggregationFunctionRow.loadFunctions(
            m_settings, CFG_PATTERN_AGGREGATION_FUNCTIONS, dbIdentifier, tableSpec);
        if (tableSpec.getNumColumns() > usedColNames.size() && !patternFunctions.isEmpty()) {
            for (final DataColumnSpec spec : tableSpec) {
                if (!usedColNames.contains(spec.getName())) {
                    for (final DBPatternAggregationFunctionRow patternFunction : patternFunctions) {
                        final Pattern pattern = patternFunction.getRegexPattern();
                        final DBAggregationFunction function = patternFunction.getFunction();
                        if (pattern != null && pattern.matcher(spec.getName()).matches()
                                && function.isCompatible(spec.getType())) {
                            final DBColumnAggregationFunctionRow row =
                                    new DBColumnAggregationFunctionRow(spec, patternFunction.getFunction());
                            m_aggregationFunction2Use.add(row);
                            usedColNames.add(spec.getName());
                        }
                    }
                }
            }
        }
        final List<DBDataTypeAggregationFunctionRow> typeFunctions = DBDataTypeAggregationFunctionRow.loadFunctions(
            m_settings, CFG_TYPE_AGGREGATION_FUNCTIONS, dbIdentifier, tableSpec);
      //check if some columns are left
        if (tableSpec.getNumColumns() > usedColNames.size() && !typeFunctions.isEmpty()) {
            for (final DataColumnSpec spec : tableSpec) {
                if (!usedColNames.contains(spec.getName())) {
                    final DataType dataType = spec.getType();
                    for (final DBDataTypeAggregationFunctionRow typeAggregator : typeFunctions) {
                        if (typeAggregator.isCompatibleType(dataType)) {
                            final DBColumnAggregationFunctionRow row =
                                    new DBColumnAggregationFunctionRow(spec, typeAggregator.getFunction());
                            m_aggregationFunction2Use.add(row);
                            usedColNames.add(spec.getName());
                        }
                    }
                }
            }
        }
        if (m_groupByCols.getIncludeList().isEmpty() && m_aggregationFunction2Use.isEmpty()
                && !m_addCountStar.getBooleanValue()) {
            throw new InvalidSettingsException("Please select at least one group or aggregation function or the "
                + "COUNT(*) option.");
        }
        if (!invalidColAggrs.isEmpty()) {
            setWarningMessage(invalidColAggrs.size() + " aggregation functions ignored due to incompatible columns.");
        }
        return new PortObjectSpec[]{createDbOutSpec(dbSpec, true)};
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
        DatabaseQueryConnectionSettings connection = inSpec.getConnectionSettings(getCredentialsProvider());
        final StatementManipulator statementManipulator = connection.getUtility().getStatementManipulator();
        final String newQuery = createQuery(connection, connection.getQuery(), statementManipulator);
        connection = createDBQueryConnection(inSpec, newQuery);

        if (checkRetrieveMetadata && !connection.getRetrieveMetadataInConfigure()) {
            return null;
        }

        final DataTableSpec tableSpec = createOutSpec(inSpec.getDataTableSpec(), connection, newQuery,
            statementManipulator, checkRetrieveMetadata);
        return new DatabasePortObjectSpec(tableSpec, connection.createConnectionModel());
    }

    /**
     * @param connection
     * @param query Query for the input table
     * @param manipulator Statement manipulator for the current database
     * @return SQL query that applies a group by to the input query
     */
    private String createQuery(final DatabaseQueryConnectionSettings connection, final String query, final StatementManipulator manipulator) {
        final StringBuilder buf = new StringBuilder();
        final String[] queries = query.split(DBReader.SQL_QUERY_SEPARATOR);
        for (int i = 0; i < queries.length - 1; i++) {
            buf.append(queries[i]);
            buf.append(DBReader.SQL_QUERY_SEPARATOR);
        }
        final String selectQuery = queries[queries.length - 1];
        // Build identifier for input table
        String tableName = "table_" + System.identityHashCode(this);
        final StringBuilder columnBuf = new StringBuilder();
        final List<String> groupByCols = m_groupByCols.getIncludeList();
        // Add group by columns
        for (int i = 0; i < groupByCols.size(); i++) {
            columnBuf.append(manipulator.quoteIdentifier(groupByCols.get(i)));
            if (i + 1 < groupByCols.size() || m_aggregationFunction2Use.size() > 0
                    || m_addCountStar.getBooleanValue()) {
                columnBuf.append(", ");
            }
        }
        final ColumnNamePolicy columnNamePolicy = ColumnNamePolicy.getPolicy4Label(m_columnNamePolicy.getStringValue());
        if (m_addCountStar.getBooleanValue()) {
            columnBuf.append("COUNT(*) AS " + manipulator.quoteIdentifier(m_countStarColName.getStringValue()));
            if (!m_aggregationFunction2Use.isEmpty()) {
                columnBuf.append(", ");
            }
        }
        // Add aggregated columns
        for (int i = 0; i < m_aggregationFunction2Use.size(); i++) {
            final DBColumnAggregationFunctionRow row = m_aggregationFunction2Use.get(i);
            columnBuf.append(row.getSQLFragment(manipulator, tableName));
            columnBuf.append(" AS ");
            columnBuf.append(manipulator.quoteIdentifier(generateColumnName(columnNamePolicy, row)));
            if (i + 1 < m_aggregationFunction2Use.size()) {
                columnBuf.append(", ");
            }
        }
        //we add this hack since google big query requires the AS here but Oracle for example does not supports it
        final boolean appendAs = connection.getDriver().toLowerCase().contains("googlebigquery");
        buf.append("SELECT " + columnBuf.toString() + " FROM (" + selectQuery + ") ");
        if (appendAs) {
            buf.append("AS ");
        }
        buf.append(manipulator.quoteIdentifier(tableName));
        // build GROUP BY clause
        if (!groupByCols.isEmpty()) {
            buf.append(" GROUP BY ");
        }
        for (int i = 0; i < groupByCols.size(); i++) {
            buf.append(manipulator.quoteIdentifier(groupByCols.get(i)));
            if (i + 1 < groupByCols.size()) {
                buf.append(", ");
            }
        }
        return buf.toString();
    }

    /**
     * @param inSpec Spec of the input table
     * @param manipulator
     * @param checkRetrieveMetadata
     * @return Spec of the output table
     * @throws InvalidSettingsException if settings do not match the input specification
     */
    private DataTableSpec createOutSpec(final DataTableSpec inSpec, final DatabaseConnectionSettings settings,
        final String query, final StatementManipulator manipulator, final boolean ignoreExceptions)
                throws InvalidSettingsException {
        // Try get spec from database
        try {
            DatabaseQueryConnectionSettings querySettings = new DatabaseQueryConnectionSettings(settings, query);
            DBReader conn = querySettings.getUtility().getReader(querySettings);
            return conn.getDataTableSpec(getCredentialsProvider());
        } catch (SQLException e) {
            NodeLogger.getLogger(getClass()).info("Could not determine table spec from database, trying to guess now",
                e);
            if (!ignoreExceptions) {
                throw new InvalidSettingsException("Error in automatically build sql statement: " + e.getMessage());
            }
            // Otherwise guess spec
        }
        final List<DataColumnSpec> colSpecs = new ArrayList<>();
        // Add all group by columns
        for (String col : m_groupByCols.getIncludeList()) {
            final DataColumnSpec columnSpec = inSpec.getColumnSpec(col);
            if (columnSpec == null) {
                throw new InvalidSettingsException("Group column '" + col + "' not found in input table");
            }
            colSpecs.add(columnSpec);
        }
        if (m_addCountStar.getBooleanValue()) {
            colSpecs.add(new DataColumnSpecCreator(manipulator.getValidColumnName(m_countStarColName.getStringValue()),
                LongCell.TYPE).createSpec());
        }
        final ColumnNamePolicy columnNamePolicy = ColumnNamePolicy.getPolicy4Label(m_columnNamePolicy.getStringValue());
        // Add aggregated columns
        for (int i = 0; i < m_aggregationFunction2Use.size(); i++) {
            final DBColumnAggregationFunctionRow row = m_aggregationFunction2Use.get(i);
            final String col = row.getColumnSpec().getName();
            final String methodId = row.getFunction().getId();

            if (inSpec.getColumnSpec(col) == null) {
                throw new InvalidSettingsException("Column '" + col + "' for aggregation function "
                        + row.getFunction().getLabel() + " does not exist");
            }
            final DatabaseUtility databaseUtility = settings.getUtility();
            final DBAggregationFunction function = databaseUtility.getAggregationFunction(methodId);

            // Get type of column after aggregation
            final DataType type = function.getType(inSpec.getColumnSpec(col).getType());
            colSpecs.add(new DataColumnSpecCreator(
                manipulator.getValidColumnName(generateColumnName(columnNamePolicy, row)), type).createSpec());
        }
        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
    }

    /**
     *
     * @param policy the {@link ColumnNamePolicy}
     * @param row {@link DBColumnAggregationFunctionRow}
     * @return New column name based on the naming policy
     */
    public static String generateColumnName(final ColumnNamePolicy policy, final DBColumnAggregationFunctionRow row) {
        final String columnName = row.getColumnSpec().getName();
        final String method = row.getFunction().getColumnName();
        switch (policy) {
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

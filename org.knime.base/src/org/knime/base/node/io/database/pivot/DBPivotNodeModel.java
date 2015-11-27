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
package org.knime.base.node.io.database.pivot;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.base.node.io.database.DBNodeModel;
import org.knime.base.node.io.database.groupby.dialog.column.DBColumnAggregationFunctionRow;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.aggregation.AggregationFunction;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.InvalidAggregationFunction;
import org.knime.core.node.port.database.pivoting.PivotColumnNameGenerator;
import org.knime.core.node.port.database.reader.DBReader;
import org.knime.core.util.Pair;

/**
 * The node model of the database pivot node
 *
 * @author Lara Gorini
 */
final class DBPivotNodeModel extends DBNodeModel {

    /**
     * Config key for the manual aggregation methods applied to the columns.
     */
    static final String CFG_AGGREGATION_FUNCTIONS = "manualAggregationFunctions";

    private final SettingsModelFilterString m_groupByCols = createGroupByModel();

    private final SettingsModelFilterString m_pivotCols = createPivotModel();

    private final SettingsModelString m_columnNamePolicy = createPolicyModel();

    private final List<DBColumnAggregationFunctionRow> m_aggregationFunction2Use = new LinkedList<>();

    private NodeSettings m_settings = null;

    /**
     * Creates a new database group by.
     */
    DBPivotNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, new PortType[]{DatabasePortObject.TYPE});
    }

    static SettingsModelFilterString createGroupByModel() {
        return new SettingsModelFilterString("groupByColumns");
    }

    static SettingsModelFilterString createPivotModel() {
        return new SettingsModelFilterString("pivotColumns");
    }

    static SettingsModelString createPolicyModel() {
        return new SettingsModelString("columnNamePolicy", ColumnNamePolicy.getDefault().getLabel());
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
            m_groupByCols.saveSettingsTo(settings);
            m_pivotCols.saveSettingsTo(settings);
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
        m_pivotCols.validateSettings(settings);
        if (ColumnNamePolicy
            .getPolicy4Label(((SettingsModelString)m_columnNamePolicy.createCloneWithValidatedValue(settings))
                .getStringValue()) == null) {
            throw new InvalidSettingsException("Invalid column name policy");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_groupByCols.loadSettingsFrom(settings);
        m_pivotCols.loadSettingsFrom(settings);
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
        final DatabasePortObject outObject = new DatabasePortObject(createDbOutSpec(dbObject.getSpec(), exec));
        return new PortObject[]{outObject};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec)inSpecs[0];
        final DataTableSpec tableSpec = dbSpec.getDataTableSpec();
        final DatabaseQueryConnectionSettings connection = dbSpec.getConnectionSettings(getCredentialsProvider());
        final String dbIdentifier = connection.getDatabaseIdentifier();
        final List<DBColumnAggregationFunctionRow> columnFunctions = DBColumnAggregationFunctionRow
            .loadFunctions(m_settings, DBPivotNodeModel.CFG_AGGREGATION_FUNCTIONS, dbIdentifier, tableSpec);
        final ArrayList<DBColumnAggregationFunctionRow> invalidColAggrs = new ArrayList<>(1);
        final Set<String> usedColNames = new HashSet<>(tableSpec.getNumColumns());
        usedColNames.addAll(m_groupByCols.getIncludeList());
        usedColNames.addAll(m_pivotCols.getIncludeList());
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
                    throw new InvalidSettingsException("Wrong aggregation function configuration '" + function.getLabel()
                        + "' of column '" + row.getColumnSpec().getName() + "': " + e.getMessage(), e);
                }
            }
            usedColNames.add(row.getColumnSpec().getName());
            m_aggregationFunction2Use.add(row);
        }

        if (m_aggregationFunction2Use.isEmpty()) {
            throw new InvalidSettingsException("No aggregation columns selected.");
        }

        if (m_groupByCols.getIncludeList().isEmpty()) {
            setWarningMessage("No grouping column included. Aggregate complete table");
        }

        if (m_pivotCols.getIncludeList().isEmpty()) {
            throw new InvalidSettingsException("No pivot columns selected.");
        }

        if (!invalidColAggrs.isEmpty()) {
            setWarningMessage(invalidColAggrs.size() + " aggregation functions ignored due to incompatible columns.");
        }

        final DatabasePortObjectSpec resultSpec;
        if (connection.getRetrieveMetadataInConfigure()) {
            try {
                resultSpec = createDbOutSpec(dbSpec, new ExecutionMonitor());
            } catch (CanceledExecutionException e) {
                throw new InvalidSettingsException(e.getMessage());
            }
        } else {
            resultSpec = null;
        }
        return new PortObjectSpec[]{resultSpec};
    }

    /**
     * @param inSpec Spec of the input database object
     * @param exec The {@link ExecutionMonitor}
     * @return Spec of the output database object
     * @throws InvalidSettingsException if the current settings are invalid
     * @throws CanceledExecutionException if execution is canceled
     */
    private DatabasePortObjectSpec createDbOutSpec(final DatabasePortObjectSpec inSpec, final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException {
        DatabaseQueryConnectionSettings connectionSettings = inSpec.getConnectionSettings(getCredentialsProvider());
        final StatementManipulator statementManipulator = connectionSettings.getUtility().getStatementManipulator();
        try {
            Connection connection = connectionSettings.createConnection(getCredentialsProvider());
            String newQuery =
                createQuery(connection, connectionSettings.getQuery(), statementManipulator, inSpec.getDataTableSpec(), exec);
            connectionSettings = createDBQueryConnection(inSpec, newQuery);
            DatabaseQueryConnectionSettings querySettings =
                new DatabaseQueryConnectionSettings(connectionSettings, newQuery);
            DBReader reader = querySettings.getUtility().getReader(querySettings);
            DataTableSpec tableSpec;
            exec.setMessage("Retrieving result specification.");
            tableSpec = reader.getDataTableSpec(getCredentialsProvider());
            return new DatabasePortObjectSpec(tableSpec, connectionSettings.createConnectionModel());
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | SQLException
                | IOException e1) {
            throw new InvalidSettingsException("Failure during query generation. Error: " + e1.getMessage(), e1);
        }
    }

    private String createQuery(final Connection connection, final String query, final StatementManipulator manipulator,
        final DataTableSpec dataTableSpec, final ExecutionMonitor exec) throws SQLException, CanceledExecutionException {

        exec.setMessage("Getting pivot values.");
        ExecutionMonitor subExec = exec.createSubProgress(0.7);
        final Map<DataColumnSpec, Set<Object>> pivotElements = new LinkedHashMap<DataColumnSpec, Set<Object>>();
        final List<String> pivotColumns = m_pivotCols.getIncludeList();
        int counter = 1;
        for (String pivotColumn : pivotColumns) {
            subExec.setProgress(counter / (double) pivotColumns.size(), "Fetching unique values for column " + pivotColumn + ". There are " + (pivotColumns.size() - counter) + " columns left.");
            DataColumnSpec columnSpec = dataTableSpec.getColumnSpec(pivotColumn);
            final String valueQuery =
                "SELECT DISTINCT " + manipulator.quoteIdentifier(pivotColumn) + " FROM (" + query + ") T";
            try (ResultSet valueSet = connection.createStatement().executeQuery(valueQuery);) {
                exec.checkCanceled();
                final Set<Object> vals = new HashSet<Object>();
                while (valueSet.next()) {
                    final Object dbVal = valueSet.getObject(1);
                    if (!valueSet.wasNull()) {
                        vals.add(dbVal);
                    }
                }
                pivotElements.put(columnSpec, vals);
                counter++;
            }
        }


        exec.setProgress(0.8, "Getting aggregation methods and columns.");
        List<String> groupByColumns = m_groupByCols.getIncludeList();
        final List<Pair<String, DBAggregationFunction>> aggValues = new LinkedList<>();
        for (int i = 0; i < m_aggregationFunction2Use.size(); i++) {
            exec.checkCanceled();
            final DBColumnAggregationFunctionRow aggregationFunction = m_aggregationFunction2Use.get(i);
            String colName = aggregationFunction.getColumnSpec().getName();
            DBAggregationFunction function = aggregationFunction.getFunction();
            aggValues.add(new Pair<String, DBAggregationFunction>(colName, function));
        }

        final ColumnNamePolicy pivotColPoliciy = ColumnNamePolicy.getPolicy4Label(m_columnNamePolicy.getStringValue());
        PivotColumnNameGenerator pivotColName = new PivotColumnNameGenerator() {

            @Override
            public String createColumnName(final String columnName, final DBAggregationFunction function,
                final List<Object> pivotValues) {
                String vals = "";
                Iterator<Object> iterator = pivotValues.iterator();
                while (iterator.hasNext()) {
                    vals = vals + iterator.next() + "_";
                }
                vals = vals.substring(0, vals.length() - 1);
                String method = function.getColumnName();
                switch (pivotColPoliciy) {
                    case KEEP_ORIGINAL_NAME:
                        return vals + "+" + columnName;
                    case AGGREGATION_METHOD_COLUMN_NAME:
                        return vals + "+" + method + "(" + columnName + ")";
                    case COLUMN_NAME_AGGREGATION_METHOD:
                        return vals + "+" + columnName + " (" + method + ")";
                    default:
                        throw new IllegalStateException("Unhandled column naming policy: " + pivotColPoliciy);
                }
            }
        };

        exec.setProgress(0.9, "Creating query.");
        exec.checkCanceled();
        return manipulator.getPivotStatement(query, groupByColumns, pivotElements, aggValues, pivotColName);
    }
}

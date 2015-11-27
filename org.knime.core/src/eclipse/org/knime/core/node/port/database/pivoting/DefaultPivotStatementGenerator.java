/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 22, 2015 (Lara): created
 */
package org.knime.core.node.port.database.pivoting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.util.Pair;

/**
 * This class creates a SQL statement for pivoting using basic SQL. A SQL statement for pivoting is built as follows:
 *
 * SELECT outerSelectStatement innerStatement GROUP BY outerGroupByStatement
 *
 * The innerStatement combines the aggregation method with aggregation values and pivot values, respectively.
 *
 * @author Lara Gorini
 * @since 3.1
 */
public class DefaultPivotStatementGenerator implements PivotStatementGenerator {

    private static final DefaultPivotStatementGenerator INSTANCE = new DefaultPivotStatementGenerator();

    /**
     * // * Constructor of DefaultPivotStatementGenerator //
     */
    protected DefaultPivotStatementGenerator() {

    }

    /**
     * @return the iNSTANCE
     */
    public static DefaultPivotStatementGenerator getINSTANCE() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPivotStatement(final StatementManipulator sm, final String tableName,
        final List<String> groupByColumnsList, final Map<DataColumnSpec, Set<Object>> pivotElements,
        final List<Pair<String, DBAggregationFunction>> aggValues, final PivotColumnNameGenerator pivotColGenerator) {

        final List<PivotData> pivotDataQueries = getPivotDataQueries(sm, pivotElements);
        final StringBuilder outerSelectStatement = createOuterSelectStatement(sm, tableName, groupByColumnsList,
            pivotDataQueries, aggValues, pivotColGenerator);
        final StringBuilder innerStatement =
            createInnerStatement(sm, tableName, groupByColumnsList, pivotDataQueries, aggValues, pivotColGenerator);
        final StringBuilder outerGroupByStatement = createOuterGroupByStatement(sm, tableName, groupByColumnsList,
            pivotDataQueries, aggValues, pivotColGenerator);
        return getResultStatement(outerSelectStatement, innerStatement, outerGroupByStatement).toString();
    }

    /**
     * @param sm The {@link StatementManipulator} to use
     * @param tableName Input query
     * @param groupByColumnsList Name of columns used for GROUP BY
     * @param pivotDataQueries A list of {@link PivotData}s containing all combinations of values in pivot columns
     * @param aggValues Aggregation columns and corresponding functions
     * @param pivotColGenerator Column name generator
     * @return Outer SELECT statement
     */
    protected StringBuilder createOuterSelectStatement(final StatementManipulator sm, final String tableName,
        final List<String> groupByColumnsList, final List<PivotData> pivotDataQueries,
        final List<Pair<String, DBAggregationFunction>> aggValues, final PivotColumnNameGenerator pivotColGenerator) {
        StringBuilder outerSelectStatement = new StringBuilder();

        for (int i = 1; i < groupByColumnsList.size() + 1; i++) {
            final String groupColName = "T." + sm.quoteIdentifier(groupByColumnsList.get(i - 1));
            outerSelectStatement.append(groupColName).append(", ");
        }
        int id_counter = 1;
        for (PivotData pivotData : pivotDataQueries) {
            String identifier = sm.quoteIdentifier("P_" + id_counter);
            for (int j = 1; j < aggValues.size() + 1; j++) {
                Pair<String, DBAggregationFunction> pair = aggValues.get(j - 1);
                String pivotColName =
                    pivotColGenerator.createColumnName(pair.getFirst(), pair.getSecond(), pivotData.getValues());
                String columnIdentifier = identifier + ".c" + j;
                outerSelectStatement.append(columnIdentifier).append(" ").append(sm.quoteIdentifier(pivotColName))
                    .append(", ");
            }
            id_counter++;
        }
        if (outerSelectStatement.length() > 2) {
            outerSelectStatement.delete(outerSelectStatement.length() - 2, outerSelectStatement.length());
        }
        outerSelectStatement.append(" FROM (" + tableName + ") T");
        return outerSelectStatement;
    }

    /**
     * @param sm The {@link StatementManipulator} to use
     * @param tableName Input query
     * @param groupByColumnsList Name of columns used for GROUP BY
     * @param pivotDataQueries A list of {@link PivotData}s containing all combinations of values in pivot columns
     * @param aggValues Aggregation columns and corresponding functions
     * @param pivotColGenerator Column name generator
     * @return Inner SQL statement
     */
    protected StringBuilder createInnerStatement(final StatementManipulator sm, final String tableName,
        final List<String> groupByColumnsList, final List<PivotData> pivotDataQueries,
        final List<Pair<String, DBAggregationFunction>> aggValues, final PivotColumnNameGenerator pivotColGenerator) {

        final StringBuilder aggregationValueBuilder = new StringBuilder();
        int agg_counter = 1;
        for (Pair<String, DBAggregationFunction> entry : aggValues) {
            aggregationValueBuilder
                .append(entry.getSecond().getSQLFragment(sm, "A", entry.getFirst()) + " c" + agg_counter + ", ");
            agg_counter++;
        }
        final String aggregationValues = aggregationValueBuilder.substring(0, aggregationValueBuilder.length() - 2);

        StringBuilder columnsGroupBy = new StringBuilder();
        StringBuilder columnsGroupByOperator = new StringBuilder();
        ArrayList<String> columnsGroupByOuterSelection = new ArrayList<String>(groupByColumnsList.size());
        for (int i = 1; i < groupByColumnsList.size() + 1; i++) {
            final String groupColName = sm.quoteIdentifier(groupByColumnsList.get(i - 1));
            columnsGroupBy.append(groupColName + " g" + i).append(", ");
            columnsGroupByOuterSelection.add("T." + groupColName);
            columnsGroupByOperator.append(groupColName).append(", ");
        }
        if (columnsGroupByOperator.length() > 2) {
            columnsGroupByOperator.delete(columnsGroupByOperator.length() - 2, columnsGroupByOperator.length());
        }

        StringBuilder innerStatement = new StringBuilder();
        int id_counter = 1;
        for (PivotData pivotData : pivotDataQueries) {
            innerStatement.append(" LEFT JOIN ");
            String identifier = sm.quoteIdentifier("P_" + id_counter);
            innerStatement.append("(SELECT ").append(columnsGroupBy).append(aggregationValues).append(" FROM (")
                .append(tableName).append(") A WHERE ").append(pivotData.getQuery());
            if (columnsGroupByOperator.length() > 0) {
                innerStatement.append(" GROUP BY ").append(columnsGroupByOperator);
            }
            innerStatement.append(") ").append(identifier);
            id_counter++;
            StringBuilder onStatement = new StringBuilder();
            for (int i = 1; i < columnsGroupByOuterSelection.size() + 1; i++) {
                if (i != 1) {
                    onStatement.append(" AND ");
                }
                onStatement.append("(").append(columnsGroupByOuterSelection.get(i - 1)).append("=").append(identifier)
                    .append(".g").append(i).append(")");
            }
            if (onStatement.length() > 0) {
                innerStatement.append(" ON ").append(onStatement);
            }
        }
        return innerStatement;
    }

    /**
     * @param sm The {@link StatementManipulator} to use
     * @param tableName Input query
     * @param groupByColumnsList Name of columns used for GROUP BY
     * @param pivotDataQueries A list of {@link PivotData}s containing all combinations of values in pivot columns
     * @param aggValues Aggregation columns and corresponding functions
     * @param pivotColGenerator Column name generator
     * @return Outer GROUP BY statement
     */
    protected StringBuilder createOuterGroupByStatement(final StatementManipulator sm, final String tableName,
        final List<String> groupByColumnsList, final List<PivotData> pivotDataQueries,
        final List<Pair<String, DBAggregationFunction>> aggValues, final PivotColumnNameGenerator pivotColGenerator) {

        StringBuilder outerGroupByStatement = new StringBuilder();
        for (int i = 1; i < groupByColumnsList.size() + 1; i++) {
            final String groupColName = "T." + sm.quoteIdentifier(groupByColumnsList.get(i - 1));
            outerGroupByStatement.append(groupColName).append(", ");
        }

        for (int i = 1; i < pivotDataQueries.size() + 1; i++) {
            String identifier = sm.quoteIdentifier("P_" + i);
            for (int j = 1; j < aggValues.size() + 1; j++) {
                outerGroupByStatement.append(identifier).append(".c").append(j).append(", ");
            }
        }
        if (outerGroupByStatement.length() > 2) {
            outerGroupByStatement.delete(outerGroupByStatement.length() - 2, outerGroupByStatement.length());
        }
        return outerGroupByStatement;
    }

    private StringBuilder getResultStatement(final StringBuilder outerSelectStatement,
        final StringBuilder innerStatement, final StringBuilder outerGroupByStatement) {
        StringBuilder result = new StringBuilder();
        result.append(" SELECT " + outerSelectStatement);
        result.append(innerStatement);
        if (outerGroupByStatement.length() > 0) {
            result.append(" GROUP BY " + outerGroupByStatement);
        }
        return result;
    }

    /**
     *
     * @param sm The {@link StatementManipulator} to use
     * @param pivotElements Columns and corresponding elements used as pivot
     * @return A list of {@link PivotData}s containing all combinations of values in pivot columns
     */
    protected List<PivotData> getPivotDataQueries(final StatementManipulator sm,
        final Map<DataColumnSpec, Set<Object>> pivotElements) {
        List<DataColumnSpec> columnSpecs = new LinkedList<DataColumnSpec>();
        Set<DataColumnSpec> columnSpecSet = pivotElements.keySet();
        Iterator<DataColumnSpec> iterator = columnSpecSet.iterator();
        while (iterator.hasNext()) {
            columnSpecs.add(iterator.next());
        }
        final List<PivotData> pivotDataQueries = new LinkedList<>();
        getPivotData(sm, pivotElements, columnSpecs, null, null, pivotDataQueries);
        return pivotDataQueries;
    }

    private void getPivotData(final StatementManipulator sm, final Map<DataColumnSpec, Set<Object>> pivotElements,
        final List<DataColumnSpec> columnSpecs, final List<Object> parentVals, final List<DataColumnSpec> parentCols,
        final List<PivotData> pivotDataQueries) {
        final DataColumnSpec rootColumn = columnSpecs.get(0);
        final LinkedList<DataColumnSpec> restColNames = new LinkedList<DataColumnSpec>(columnSpecs);
        restColNames.remove(rootColumn);
        final Set<Object> vals = pivotElements.get(rootColumn);
        for (Object val : vals) {
            final List<Object> myVals;
            final List<DataColumnSpec> myCols;
            if (parentVals != null) {
                myVals = new LinkedList<Object>(parentVals);
                myCols = new LinkedList<>(parentCols);
            } else {
                myVals = new LinkedList<Object>();
                myCols = new LinkedList<>();
            }
            myVals.add(val);
            myCols.add(rootColumn);
            if (!restColNames.isEmpty()) {
                getPivotData(sm, pivotElements, restColNames, myVals, myCols, pivotDataQueries);
            } else {
                pivotDataQueries.add(new PivotData(sm, myCols, myVals));
            }
        }
    }
}

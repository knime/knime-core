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

import java.util.List;

import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.util.Pair;

/**
 * This class creates a SQL statement for pivoting using CASE WHEN keyword. A SQL statement for pivoting is built as
 * follows:
 *
 * SELECT outerSelectStatement
 * innerStatement
 * GROUP BY outerGroupByStatement
 *
 * The innerStatement combines the aggregation method with aggregation values and pivot values, respectively.
 *
 * @author Lara Gorini
 * @since 3.1
 */
public class CasePivotStatementGenerator extends DefaultPivotStatementGenerator {

    private static final CasePivotStatementGenerator INSTANCE = new CasePivotStatementGenerator();

    private CasePivotStatementGenerator() {
        super();
    }

    /**
     * @return the iNSTANCE
     */
    public static CasePivotStatementGenerator getINSTANCE() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StringBuilder createOuterSelectStatement(final StatementManipulator sm, final String tableName,
        final List<String> groupByColumnsList, final List<PivotData> pivotDataQueries,
        final List<Pair<String, DBAggregationFunction>> aggValues, final PivotColumnNameGenerator pivotColGenerator) {

        StringBuilder outerSelectStatement = new StringBuilder();
        for (int i = 0; i < groupByColumnsList.size(); i++) {
            final String groupColName = sm.quoteIdentifier(groupByColumnsList.get(i));
            outerSelectStatement.append("T." + groupColName + ", ");
        }
        return outerSelectStatement;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StringBuilder createInnerStatement(final StatementManipulator sm, final String tableName,
        final List<String> groupByColumnsList, final List<PivotData> pivotDataQueries,
        final List<Pair<String, DBAggregationFunction>> aggValues, final PivotColumnNameGenerator pivotColGenerator) {

        StringBuilder innerStatementBuilder = new StringBuilder();
        for (PivotData pivotData : pivotDataQueries) {
            StringBuilder caseStatement = new StringBuilder();
            for (Pair<String, DBAggregationFunction> entry : aggValues) {
                String columnName = entry.getFirst();
                DBAggregationFunction aggMethod = entry.getSecond();
                caseStatement.append(aggMethod.getSQLFragment4SubQuery(sm, "T",
                    "CASE WHEN " + pivotData.getQuery() + " THEN " + sm.quoteIdentifier(columnName) + " END"));
                caseStatement.append(" ");
                caseStatement.append(sm
                    .quoteIdentifier(pivotColGenerator.createColumnName(columnName, aggMethod, pivotData.getValues())));
                caseStatement.append(", ");
            }
            innerStatementBuilder.append(caseStatement);
        }
        StringBuilder innerStatement = new StringBuilder();
        innerStatement.append(innerStatementBuilder.substring(0, innerStatementBuilder.length() - 2));
        innerStatement.append(" FROM (" + tableName + ") T");
        return innerStatement;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StringBuilder createOuterGroupByStatement(final StatementManipulator sm, final String tableName,
        final List<String> groupByColumnsList, final List<PivotData> pivotDataQueries,
        final List<Pair<String, DBAggregationFunction>> aggValues, final PivotColumnNameGenerator pivotColGenerator) {

        StringBuilder outerGroupByStatement = new StringBuilder();
        StringBuilder outerSelectStatement = createOuterSelectStatement(sm, tableName, groupByColumnsList,
            pivotDataQueries, aggValues, pivotColGenerator);
        if (outerSelectStatement.length() > 2) {
            outerGroupByStatement.append(outerSelectStatement.substring(0, outerSelectStatement.length() - 2));
        }
        return outerGroupByStatement;
    }

}

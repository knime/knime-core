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
 * ---------------------------------------------------------------------
 *
 * History
 *   08.05.2014 (thor): created
 */
package org.knime.core.node.port.database;

import java.sql.SQLException;
import java.sql.Statement;

import org.knime.core.data.StringValue;
import org.knime.core.node.port.database.aggregation.function.AvgDistinctDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.BitAndDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.BitOrDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.CorrDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.CountDistinctDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.CovarPopDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.CovarSampDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.GroupConcatDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.MaxDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.MinDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.RegrAvgXDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.RegrAvgYDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.RegrCountDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.RegrInterceptDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.RegrR2DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.RegrSXXDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.RegrSXYDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.RegrSYYDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.RegrSlopeDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.StdDevPopDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.StdDevSampDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.SumDistinctDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.VarPopDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.VarSampDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.postgresql.ArrayAggDBAggregationFunction;

/**
 * Database utility for PostgreSQL.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class PostgreSQLUtility extends DatabaseUtility {
    private static class PostgreSQLStatementManipulator extends StatementManipulator {
        /**
         * {@inheritDoc}
         */
        @Override
        public void setFetchSize(final Statement statement, final int fetchSize) throws SQLException {
            if (fetchSize >= 0) {
                // fix 2741: postgresql databases ignore fetchsize when
                // AUTOCOMMIT on; setting it to false
                DatabaseConnectionSettings.setAutoCommit(statement.getConnection(), false);
                super.setFetchSize(statement, fetchSize);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String forMetadataOnly(final String sql) {
            return limitRows(sql, 0);
        }
    }

    private static final StatementManipulator MANIPULATOR = new PostgreSQLStatementManipulator();

    /**The unique database identifier.
     * @since 2.11*/
    public static final String DATABASE_IDENTIFIER = "postgresql";

    /**
     *
     */
    public PostgreSQLUtility() {
        super(DATABASE_IDENTIFIER, MANIPULATOR, new ArrayAggDBAggregationFunction.Factory(),
            new AvgDistinctDBAggregationFunction.Factory(), new BitAndDBAggregationFunction.Factory(),
            new BitOrDBAggregationFunction.Factory(), new CountDistinctDBAggregationFunction.Factory(),
            new MaxDBAggregationFunction.Factory(), new MinDBAggregationFunction.Factory(),
            new SumDistinctDBAggregationFunction.Factory(),
            new GroupConcatDBAggregationFunction.Factory("STRING_AGG", StringValue.class),
            new CorrDBAggregationFunction.Factory(), new CovarPopDBAggregationFunction.Factory(),
            new CovarSampDBAggregationFunction.Factory(), new RegrAvgXDBAggregationFunction.Factory(),
            new RegrAvgYDBAggregationFunction.Factory(), new RegrCountDBAggregationFunction.Factory(),
            new RegrInterceptDBAggregationFunction.Factory(), new RegrR2DBAggregationFunction.Factory(),
            new RegrSlopeDBAggregationFunction.Factory(), new RegrSXXDBAggregationFunction.Factory(),
            new RegrSXYDBAggregationFunction.Factory(), new RegrSYYDBAggregationFunction.Factory(),
            new StdDevPopDBAggregationFunction.Factory(), new StdDevSampDBAggregationFunction.Factory(),
            new VarPopDBAggregationFunction.Factory(), new VarSampDBAggregationFunction.Factory());
    }
}

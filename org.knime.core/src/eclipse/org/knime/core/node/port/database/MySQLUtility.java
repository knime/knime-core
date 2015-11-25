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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.aggregation.function.AvgDistinctDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.BitAndDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.BitOrDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.BitXOrDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.CountDistinctDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.GroupConcatDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.MaxDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.MinDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.StdDevPopDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.StdDevSampDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.SumDistinctDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.VarPopDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.VarSampDBAggregationFunction;

/**
 * Database utility for MySQL.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class MySQLUtility extends DatabaseUtility {
    private static class MySQLStatementManipulator extends StatementManipulator {


        /**
         * Constructor of class {@link MySQLStatementManipulator}.
         */
       public MySQLStatementManipulator () {
           super(true);
       }


        /**
         * {@inheritDoc}
         * @deprecated
         */
        @Deprecated
        @Override
        public String quoteColumn(final String colName) {
            Matcher m = SAVE_COLUMN_NAME_PATTERN.matcher(colName);
            if (!m.matches()) {
                return "`" + colName + "`";
            } else {
                // no need to quote
                return colName;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String quoteIdentifier(final String identifier) {
            return quoteColumn(identifier);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setFetchSize(final Statement statement, final int fetchSize) throws SQLException {
            if (fetchSize >= 0) {
                super.setFetchSize(statement, fetchSize);
            } else {
                // fix 2040: MySQL databases read everything into one, big
                // ResultSet leading to an heap space error
                // Integer.MIN_VALUE is an indicator in order to enable
                // streaming results
                statement.setFetchSize(Integer.MIN_VALUE);
                NodeLogger.getLogger(getClass()).info(
                    "Database fetchsize for MySQL database set to " + Integer.MIN_VALUE + ".");
            }
        }

        // pattern that matches all(?) SQL queries for which we must NOT append a LIMIT without wrapping the query first
        private static final Pattern UNSAVE_LIMIT_PATTERN = Pattern.compile(
            "(?i)(?:LIMIT\\s+\\d+|PROCEDURE\\s+\\S+|INTO\\s+\\S+|FOR\\s+UPDATE|LOCK\\s+IN\\s+SHARE\\s+MODE)");

        /**
         * {@inheritDoc}
         */
        @Override
        public String limitRows(final String sql, final long count) {
            Matcher m = UNSAVE_LIMIT_PATTERN.matcher(sql);
            if (m.find()) {
                // wraps the original query
                return super.limitRows(sql, count);
            } else {
                return sql + " LIMIT 0";
            }
        }


        @Override
        public String randomRows(final String sql, final long count) {
            final String tmp =
                    "SELECT * FROM (" + sql + ") " + getTempTableName() + " ORDER BY RAND() LIMIT " + count;
            return tmp;
        }



        /**
         * {@inheritDoc}
         */
        @Override
        public String forMetadataOnly(final String sql) {
            return limitRows(sql, 0);
        }



        @Override
        public String getSamplingStatement(final String sql, final long valueToLimit, final boolean random) {
            if (random) {
                return randomRows(sql, valueToLimit);
            } else {
                return super.limitRows(sql, valueToLimit);
            }
        }
    }

    private static final StatementManipulator MANIPULATOR = new MySQLStatementManipulator();

    /**The unique database identifier.
     * @since 2.11*/
    public static final String DATABASE_IDENTIFIER = "mysql";

    /**
     *
     */
    public MySQLUtility() {
        super(DATABASE_IDENTIFIER, MANIPULATOR, new AvgDistinctDBAggregationFunction.Factory(),
            new BitAndDBAggregationFunction.Factory(), new BitOrDBAggregationFunction.Factory(),
            new BitXOrDBAggregationFunction.Factory(), new CountDistinctDBAggregationFunction.Factory(),
            new GroupConcatDBAggregationFunction.Factory(), new MaxDBAggregationFunction.Factory(),
            new MinDBAggregationFunction.Factory(), new StdDevPopDBAggregationFunction.Factory(),
            new StdDevSampDBAggregationFunction.Factory(), new SumDistinctDBAggregationFunction.Factory(),
            new VarPopDBAggregationFunction.Factory(), new VarSampDBAggregationFunction.Factory());
    }

    @Override
    public boolean supportsRandomSampling() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsCase() {
        return true;
    }
}

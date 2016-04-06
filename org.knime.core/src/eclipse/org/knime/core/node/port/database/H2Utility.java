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
 *   15.03.2016 (thor): created
 */
package org.knime.core.node.port.database;

import org.knime.core.node.port.database.aggregation.function.AvgDistinctDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.BitAndDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.BitOrDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.CountDistinctDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.GroupConcatDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.MaxDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.MinDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.StdDevPopDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.StdDevSampDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.SumDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.VarPopDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.VarSampDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.h2.BoolAndDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.h2.BoolOrDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.h2.SelectivtyDBAggregationFunction;

/**
 * Database utility class for H2.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 3.2
 */
public class H2Utility extends DatabaseUtility {
    private static class H2StatementManipulator extends StatementManipulator {

        /**
         * Constructor.
         */
        public H2StatementManipulator() {
            super(true);
        }

        @Override
        public String randomRows(final String sql, final long count) {
            final String tmp = "SELECT * FROM (" + sql + ") " + getTempTableName() + " ORDER BY RANDOM() LIMIT " + count;
            return limitRows(tmp, count);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String quoteColumn(final String colName) {
            // always quote because H2 converts everything to uppercase
            return "\"" + colName + "\"";
        }
    }

    private static final StatementManipulator MANIPULATOR = new H2StatementManipulator();

    /** The unique database identifier. */
    public static final String DATABASE_IDENTIFIER = "h2";

    /**
     * Constructor.
     */
    public H2Utility() {
        super(DATABASE_IDENTIFIER, MANIPULATOR,  new AvgDistinctDBAggregationFunction.Factory(),
            new BitAndDBAggregationFunction.Factory(), new BitOrDBAggregationFunction.Factory(),
            new BoolAndDBAggregationFunction.Factory(), new BoolOrDBAggregationFunction.Factory(),
            new CountDistinctDBAggregationFunction.Factory(), new GroupConcatDBAggregationFunction.Factory(),
            new MaxDBAggregationFunction.Factory(), new MinDBAggregationFunction.Factory(),
            new SumDBAggregationFunction.Factory(), new SelectivtyDBAggregationFunction.Factory(),
            new StdDevPopDBAggregationFunction.Factory(), new StdDevSampDBAggregationFunction.Factory(),
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

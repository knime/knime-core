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
 *   01.08.2014 (koetter): created
 */
package org.knime.core.node.port.database.aggregation;

import org.knime.core.data.DataType;
import org.knime.core.node.port.database.StatementManipulator;


/**
 * This interface defines a database specific aggregation function such as count. Before implementing you own
 * function have a look at the already existing implementations and reuse them. This allows the user to
 * switch seamlessly between databases that support the same aggregation functions. Otherwise the user needs
 * to adapt the aggregation function after each db switch.
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public interface DBAggregationFunction extends AggregationFunction {

    /**
     * @param originalType Type of the column that will be aggregated
     * @return The type of the aggregated column
     */
    DataType getType(final DataType originalType);

    /**
     * @param manipulator {@link StatementManipulator} for quoting the column name if necessary
     * @param columnName the column to use
     * @param tableName the name of the table the column belongs to
     * @return the sql fragment to use in the sql query e.g. SUM(colName)
     */
    String getSQLFragment(StatementManipulator manipulator, String tableName, String columnName);

    /**
     * @param manipulator {@link StatementManipulator} for quoting the column name if necessary
     * @param subQuery the sub query to use e.g. CASE statement
     * @param tableName the name of the table the column belongs to
     * @return the sql fragment to use in the sql query e.g. SUM(colName)
     * @since 3.1
     */
    String getSQLFragment4SubQuery(StatementManipulator manipulator, String tableName, String subQuery);

    /**
     * @return the name of the function used in the column name
     */
    String getColumnName();
}

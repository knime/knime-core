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
 *   May 7, 2014 ("Patrick Winter"): created
 */
package org.knime.base.node.io.database;

import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;

/**
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
enum DBGroupByAggregationMethod {

    /**
     * Average value.
     */
    AVG(DoubleCell.TYPE),
    /**
     * Number of rows.
     */
    COUNT(IntCell.TYPE),
    /**
     * First value.
     */
    FIRST(null),
    /**
     * Last value.
     */
    LAST(null),
    /**
     * Largest value.
     */
    MAX(null),
    /**
     * Smallest value.
     */
    MIN(null),
    /**
     * Sum.
     */
    SUM(null);

    private DataType m_type;

    /**
     * @param type Type of the aggregation column, null for columns that keep their type
     */
    private DBGroupByAggregationMethod(final DataType type) {
        m_type = type;
    }

    /**
     * @param originalType Type of the column that will be aggregated
     * @return The type of the aggregated column
     */
    DataType getType(final DataType originalType) {
        if (m_type != null) {
            return m_type;
        } else {
            return originalType;
        }
    }

    /**
     * @return Names of all available aggregation methods
     */
    static String[] getAllMethodNames() {
        DBGroupByAggregationMethod[] methods = DBGroupByAggregationMethod.values();
        String[] names = new String[methods.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = methods[i].name();
        }
        return names;
    }

}

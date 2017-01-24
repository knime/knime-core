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
 *   28.08.2014 (koetter): created
 */
package org.knime.core.node.port.database.aggregation.function.oracle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.DBAggregationFunctionFactory;
import org.knime.core.node.port.database.aggregation.function.column.AbstractColumnFunctionSelectDBAggregationFunction;

/**
 *
 * @author Ole Ostergaard, KNIME.com
 * @since 3.3
 */
public final class StatsOneWayAnovaDBAggregationFunction extends AbstractColumnFunctionSelectDBAggregationFunction {

    private static final String ID = "STATS_ONE_WAY_ANOVA";

    private static final String DEFAULT = "SIG";

    private static final List<String> RETURN_VALUES = Collections.unmodifiableList(
        Arrays.asList(DEFAULT, "SUM_SQUARES_BETWEEN", "SUM_SQUARES_WITHIN", "DF_BETWEEN",
            "DF_WITHIN", "MEAN_SQUARES_BETWEEN", "MEAN_SQUARES_WITHIN", "F_RATIO"));

    /**Factory for parent class.*/
    public static final class Factory implements DBAggregationFunctionFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getId() {
            return ID;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DBAggregationFunction createInstance() {
            return new StatsOneWayAnovaDBAggregationFunction();
        }

    }

    /**
     * Constructor.
     */
    private StatsOneWayAnovaDBAggregationFunction() {
        super("Return Value: ", DEFAULT, RETURN_VALUES, "Second column: ", null, DoubleValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getType(final DataType originalType) {
        return DoubleCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLabel() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName() {
        String selectedParameter;
        if (getSelectedParameter().length() < 14){
            selectedParameter = getSelectedParameter();
        } else {
            selectedParameter = getSelectedParameter().substring(0, 14);
        }
        return "OWA" + "_" + selectedParameter + "_" + getSelectedColumnName() ;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCompatible(final DataType type) {
        return type.isCompatible(DataValue.class);
    }

    /**
     * Returns the description for the return value parameters.
     * @return Description for return value parameters
     */
    public String getReturnDescription() {
        return "(SIG: Significance, "
            + "SUM_SQUARES_BETWEEN: Sum of squares between groups, "
            + "SUM_SQUARES_WITHIN: Sum of squares within groups, "
            + "DF_BETWEEN: Degree of freedom between groups, "
            + "DF_WITHIN: Degree of freedom within groups, "
            + "MEAN_SQUARES_BETWEEN: Mean squares between groups, "
            + "MEAN_SQUARES_WITHIN: Mean squares within groups, "
            + "F_RATIO: Ratio of the mean squares between to the mean squares within)";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "The one-way analysis of variance function tests "
            + "differences in means (for groups or variables) for "
            + "statistical significance by comparing two different estimates of variance. "
            + getReturnDescription();
    }
}

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
public final class StatsCrosstabDBAggregationFunction extends AbstractColumnFunctionSelectDBAggregationFunction {

    private static final String ID = "STATS_CROSSTAB";

    private static final String DEFAULT = "CHISQ_SIG";

    private static final List<String> RETURN_VALUES = Collections.unmodifiableList(
        Arrays.asList(DEFAULT, "CHISQ_OBS", "CHISQ_DF", "PHI_COEFFICIENT",
            "CRAMERS_V", "CONT_COEFFICIENT", "COHENS_K"));

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
            return new StatsCrosstabDBAggregationFunction();
        }

    }

    /**
     * Constructor.
     */
    private StatsCrosstabDBAggregationFunction() {
        super("Return value: ", DEFAULT, RETURN_VALUES, "Second column: ", null, DataValue.class);
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
        return "XT" + "_" + getSelectedParameter().subSequence(0, 7) + "_" + getSelectedColumnName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCompatible(final DataType type) {
        return type.isCompatible(DoubleValue.class);
    }

    /**
     * Returns the description for the return value parameters.
     * @return Description for return value parameters
     */
    public String getReturnDescription() {
        return "(CHISQ_OBS: Observed value of chi-squared, "
            + "CHISQ_SIG: Significance of observed chi-squared, "
            + "CHISQ_DF: Degree of freedom for chi-squared, "
            + "PHI-COEFFICIENT: Phi coefficient, "
            + "CRAMERS_V: Cramer's V statistic, "
            + "CONT_COEFFICIENT: Contingency coefficient, "
            + "COHENS_K: Cohen's Kappa)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Crosstabulation is a method used to analyze two nominal variables. " + getReturnDescription();
    }
}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Aug 28, 2019 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.probability;

import java.util.Arrays;

import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataType;
import org.knime.core.node.util.CheckUtils;

/**
 * Factory for {@link ProbabilityDistributionCell}s.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public final class ProbabilityDistributionCellFactory implements DataCellFactory {

    /**
     * The data type for the cells created by this factory.
     */
    public static final DataType TYPE = ProbabilityDistributionCell.TYPE;

    /**
     * Creates a new {@link ProbabilityDistributionCell} from the arguments.
     *
     * @param probabilities the probabilities, must be non-negative and must sum up to 1
     *
     * @return the cell containing the created probability distribution.
     * @throws IllegalArgumentException if one of the arguments is null or not valid
     */
    public static ProbabilityDistributionCell createCell(final double[] probabilities) {
        return createCell(probabilities, 0.0001);
    }

    /**
     * Creates a new {@link ProbabilityDistributionCell} from the arguments.
     *
     * @param probabilities the probabilities, must be non-negative and must sum up to 1 (with respect to the given
     *            epsilon)
     * @param epsilon the imprecision that is the sum of probabilities allow to have
     *
     * @return the cell containing the created probability distribution.
     * @throws IllegalArgumentException if one of the arguments is null or not valid
     */
    public static ProbabilityDistributionCell createCell(final double[] probabilities, final double epsilon) {
        // Check null
        CheckUtils.checkNotNull(probabilities, "The list of probabilities must not be null.");
        // check that precision is positive
        CheckUtils.checkArgument(epsilon >= 0, "The epsilon must not be negative");
        // check that no probability is negative
        CheckUtils.checkArgument(Arrays.stream(probabilities).noneMatch(e -> e < 0d),
            "Probability must not be negative.");
        // check that probabilities sum up to 1
        CheckUtils.checkArgument(sumUpToOne(Arrays.stream(probabilities).sum(), epsilon),
            "The probabilities do not sum up to 1. Consider setting a proper epsilon.");
        return new ProbabilityDistributionCell(probabilities.clone());
    }

    private static boolean sumUpToOne(final double a, final double epsilon) {
        return Math.abs(a - 1.0d) < epsilon;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getDataType() {
        return TYPE;
    }

}

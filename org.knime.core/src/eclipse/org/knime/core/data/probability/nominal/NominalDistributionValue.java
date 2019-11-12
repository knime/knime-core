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
 *   Oct 8, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.probability.nominal;

import java.util.Set;

import javax.swing.Icon;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.node.util.SharedIcons;

/**
 * Special interface that is implemented by {@link DataCell DataCells} that represent probability distributions over
 * nominal values. These distributions share the properties that the probabilities are non-negative and sum up to 1.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public interface NominalDistributionValue extends DataValue {

    /**
     * Meta information on {@link NominalDistributionValue NominalDistributionValues}.
     *
     * @see DataValue#UTILITY
     */
    UtilityFactory UTILITY = new NominalDistributionUtilityFactory();

    /**
     * Returns the probability of {@link DataCell value} in this distribution. Note that unknown values (i.e. values for
     * which {@link NominalDistributionValue#isKnown(String)} returns false) will not result in a failure but instead
     * return a probability of 0.
     *
     * @param value {@link DataCell} to retrieve the probability for
     * @return the probability of {@link DataCell value} in this distribution
     */
    double getProbability(final String value);

    /**
     * Returns the most likely value i.e. the value with the highest probability.<br/>
     * The first value is returned if multiple values have the highest probability (e.g. 50% A 50% B).
     *
     * @return the value with the highest probability
     */
    String getMostLikelyValue();

    /**
     * Returns the maximum probability in this {@link NominalDistributionValue}. <br/>
     * The returned probability is therefore the probability of the value returned by
     * {@link NominalDistributionValue#getMostLikelyValue()}.
     *
     * @return the maximal probability value
     */
    double getMaximalProbability();

    /**
     * @param value the {@link DataCell} to check
     * @return true if {@link DataCell value} is known to this distribution
     */
    boolean isKnown(final String value);

    /**
     * @return the set of values this distribution knows
     */
    Set<String> getKnownValues();

    /** Implementations of the meta information of this value class. */
    class NominalDistributionUtilityFactory extends ExtensibleUtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON = SharedIcons.TYPE_PROBABILITY_DISTRIBUTION.get();

        /** Only subclasses are allowed to instantiate this class. */
        protected NominalDistributionUtilityFactory() {
            super(NominalDistributionValue.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Icon getIcon() {
            return ICON;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "Nominal Probability Distribution";
        }

    }
}

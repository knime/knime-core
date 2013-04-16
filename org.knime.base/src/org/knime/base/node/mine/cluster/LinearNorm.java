/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.cluster;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class LinearNorm {


    private static class Interval {
        private final double m_original;
        private final double m_norm;

        /**
         *
         * @param orig original value
         * @param norm normed value
         */
        public Interval(final double orig, final double norm) {
            m_original = orig;
            m_norm = norm;
        }

        /**
         *
         * @return the original value
         */
        public double getOriginalValue() {
            return m_original;
        }
        /**
         *
         * @return the normalized value
         */
        public double getNormValue() {
            return m_norm;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "orig=" + m_original + " norm=" + m_norm;
        }
    }

    private final String m_fieldName;

    private final List<Interval>m_intervals;

    /**
     *
     * @param fieldName the name of the field
     */
    public LinearNorm(final String fieldName) {
        m_fieldName = fieldName;
        m_intervals = new ArrayList<Interval>();
    }

    /**
     * Represents a LinearNorm PMML element.
     * Adds an pair of values: original value and normed value.
     *
     * @param origValue the original value
     * @param normValue the mapped norm value
     */
    public void addInterval(final double origValue, final double normValue) {
        // check for valid intervals
        if (!m_intervals.isEmpty()) {
            Interval lower = m_intervals.get(m_intervals.size() - 1);
            if (normValue <= lower.m_norm
                    || origValue <= lower.m_original) {
                throw new IllegalArgumentException(
                        "Intervals for LinearNorm must be added "
                        + "in ascending order!");
            }
        }
        m_intervals.add(new Interval(origValue, normValue));
    }

    /**
     *
     * @return the name of the field
     */
    public String getName() {
        return m_fieldName;
    }

    /**
     * Unnormalizes the given values.
     * @param value normalized which should be "unnormalized"
     * @return unnormalized value
     */
    public double unnormalize(final double value) {
        for (int i = 0; i < m_intervals.size() - 1; i++) {
            Interval lower = m_intervals.get(i);
            Interval upper = m_intervals.get(i + 1);
            if (lower.m_norm <= value && value <= upper.m_norm) {
                double y = lower.m_original + ((value - lower.m_norm)
                        * ((upper.m_original - lower.m_original)
                                / (upper.m_norm - lower.m_norm)));
                return y;
            }
        }
        throw new IllegalArgumentException(
                "Value " + value
                + " is out of reported linear normalization!");
    }

}

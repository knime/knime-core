/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.equalsizesampling;

import java.util.BitSet;
import java.util.Random;

import org.knime.base.node.preproc.equalsizesampling.EqualSizeSamplingConfiguration.SamplingMethod;
import org.knime.core.data.DataCell;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class OccurrenceCounter {

    private final DataCell m_categoryCell;
    private int m_count;

    /** @param categoryCell */
    OccurrenceCounter(final DataCell categoryCell) {
        m_categoryCell = categoryCell;
        m_count = 1;
    }

    /** @return the categoryCell */
    DataCell getCategoryCell() {
        return m_categoryCell;
    }

    /** Increment count by one. */
    void incrementCount() {
        m_count += 1;

    }

    /** @return the count */
    int getCount() {
        return m_count;
    }

    public Sampler createSampler(final int count,
            final SamplingMethod sampMethod, final Random random) {
        switch (sampMethod) {
        case Approximate:
            return new RoughSampler(count / (double)m_count, random);
        default:
            return new ExactSampler(count, m_count, random);
        }
    }

    static interface Sampler {
        boolean includeNext();
    }

    private static final class RoughSampler implements Sampler {

        private final Random m_random;
        private final double m_frequency;

        /** @param frequency
         * @param random */
        public RoughSampler(final double frequency, final Random random) {
            m_frequency = frequency;
            m_random = random;
        }

        /** {@inheritDoc} */
        @Override
        public boolean includeNext() {
            return m_random.nextDouble() < m_frequency;
        }
    }

    private static final class ExactSampler extends BitSet implements Sampler {

        private int m_currentIndex;

        /**  */
        public ExactSampler(final int count, final int totalCount,
                final Random random) {
            super(totalCount);
            m_currentIndex = 0;
            init(count, totalCount, random);
        }

        private void init(final int count, final int totalCount,
                final Random random) {
            boolean flag;
            int bitsToFlag;
            if (count < totalCount / 2) {
                // if less than half of all bits need to be set,
                // selectively set random bits
                flag = true;
                bitsToFlag = count;
            } else {
                // if more than half of the bits need to be set,
                // select them all first and selectively unset random bits then
                set(0, totalCount, true);
                flag = false;
                bitsToFlag = totalCount - count;
            }
            for (int i = 0; i < bitsToFlag; i++) {
                while (true) {
                    int index = random.nextInt(totalCount);
                    if (get(index) == flag) { // already set before
                        continue;
                    } else {
                        set(index, flag);
                        break;
                    }
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean includeNext() {
            return get(m_currentIndex++);
        }
    }

}

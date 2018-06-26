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
 *   20.04.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import java.util.Arrays;

import org.apache.commons.math3.util.MathUtils;
import org.knime.base.node.mine.regression.logistic.learner4.data.ClassificationTrainingRow;


/**
 * Simple implementation of a {@link ClassificationTrainingRow} for testing purposes.
 *
 * @author Adrian Nembach, KNIME.com
 */
class MockClassificationTrainingRow implements ClassificationTrainingRow {

    private final double[] m_data;
    private final int[] m_nonZero;
    private final int m_id;
    private final int m_category;

    /**
     *
     */
    public MockClassificationTrainingRow(final double[] features, final int id, final int category) {
        m_id = id;
        m_category = category;
        int[] nonZero = new int[features.length + 1];
        int k = 1;
        for (int i = 1; i < nonZero.length; i++) {
            if (!MathUtils.equals(features[i - 1], 0)) {
                nonZero[k++] = i;
            }
        }
        m_nonZero = Arrays.copyOf(nonZero, k);
        m_data = new double[m_nonZero.length];
        for (int i = 1; i < m_nonZero.length; i++) {
            m_data[i] = features[m_nonZero[i] - 1];
        }
        m_data[0] = 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCategory() {
        return m_category;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return m_id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[id: " + m_id + " cat: " + m_category + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureIterator getFeatureIterator() {
        return new FeatureIter();
    }

    private class FeatureIter implements FeatureIterator {

        private int idx = -1;

        private FeatureIter() { }

        private FeatureIter(final int startIdx) {
            idx = startIdx;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return idx < m_data.length - 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean next() {
            return ++idx < m_data.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getFeatureIndex() {
            return m_nonZero[idx];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getFeatureValue() {
            return m_data[idx];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FeatureIterator spawn() {
            return new FeatureIter(idx - 1);
        }

    }

}

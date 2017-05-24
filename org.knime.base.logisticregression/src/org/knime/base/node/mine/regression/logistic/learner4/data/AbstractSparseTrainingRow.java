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
 *   17.05.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import java.util.Arrays;

/**
 * Abstract implementation of a sparse {@link TrainingRow}.
 * Stores only the non zero values and their indices in the row.
 * This allows for a fast {@link FeatureIterator} but the random access via {@link #getFeature(int)} is slow.
 *
 * @author Adrian Nembach, KNIME.com
 */
abstract class AbstractSparseTrainingRow implements TrainingRow {

    private final float[] m_values;
    private final int[] m_indices;
    private final int m_id;

    protected AbstractSparseTrainingRow(final float[] values, final int[] indices, final int id) {
        m_values = values;
        m_indices = indices;
        m_id = id;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double getFeature(final int idx) {
        int i = Arrays.binarySearch(m_indices, idx);
        if (i < 0) {
            return -1.0;
        }
        return m_values[m_indices[i]];
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
    public int getNextNonZeroIndex(final int startIdx) {
        int nonZeroIdx = Arrays.binarySearch(m_indices, startIdx);
        if (nonZeroIdx >= 0) {
            return m_indices[nonZeroIdx];
        }
        if (-nonZeroIdx == m_indices.length) {
            return -1;
        }
        return m_indices[-nonZeroIdx];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getNonZeroIndices() {
        return m_indices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureIterator getFeatureIterator() {
        return new SparseFeatureIterator();
    }

    private class SparseFeatureIterator implements FeatureIterator {

        private int m_idx = -1;
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_idx < m_values.length - 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean next() {
            return ++m_idx < m_values.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getFeatureIndex() {
            return m_indices[m_idx];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getFeatureValue() {
            return m_values[m_idx];
        }

    }

}

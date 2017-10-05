/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   24.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import java.util.BitSet;

import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow.FeatureIterator;

/**
 * Performs SAG updates in an eager way.
 * Unlike vanilla sgd, sag maintains a stochastic average of all gradients in the training data and uses this
 * average as direction in the gradient descent step.
 *
 * @author Adrian Nembach, KNIME.com
 */
final class EagerSagUpdater <T extends TrainingRow> implements EagerUpdater<T> {

    private double[][] m_gradientSum;
    private double[][] m_gradientMemory;
    private BitSet m_seen;
    private int m_nCovered = 0;
    private int m_nCats;

    private EagerSagUpdater(final int nRows, final int nFets, final int nCats) {
        m_gradientSum = new double[nCats][nFets];
        m_gradientMemory = new double[nCats][nRows];
        m_seen = new BitSet(nRows);
        m_nCats = nCats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final T x, final double[] sig, final WeightMatrix<T> beta, final double stepSize, final int iteration) {
        int id = x.getId();
        if (!m_seen.get(id)) {
            m_seen.set(id);
            m_nCovered++;
        }

        for (FeatureIterator iter = x.getFeatureIterator(); iter.next();) {
            int idx = iter.getFeatureIndex();
            double val = iter.getFeatureValue();
            for (int c = 0; c < m_nCats; c++) {
                double newD = val * (sig[c] - m_gradientMemory[c][id]);
                assert Double.isFinite(newD);
                m_gradientSum[c][idx] += newD;
            }
        }
        for (int c = 0; c < m_nCats; c++) {
            m_gradientMemory[c][id] = sig[c];
        }
        double scale = beta.getScale();
        beta.update((val, c, i) -> performUpdate(val, stepSize, scale, c, i), true);
    }

    private double performUpdate(final double betaValue, final double stepSize, final double scale, final int catIdx, final int fetIdx) {
        if (fetIdx == 0) {
            return betaValue - stepSize * m_gradientSum[catIdx][fetIdx] / m_nCovered;
        }
        return betaValue - (stepSize/ (scale * m_nCovered)) * m_gradientSum[catIdx][fetIdx] ;
    }

    static class EagerSagUpdaterFactory <T extends TrainingRow> implements UpdaterFactory<T, EagerUpdater<T>> {
        private final int m_nRows;
        private final int m_nFets;
        private final int m_nCats;

        /**
         * Creates a factory for EagerSagUpdater objects.
         * For a K class problem <b>nLinModels</b> would be K-1.
         *
         * @param nRows number of rows in the training data
         * @param nFets number of features including the intercept term
         * @param nLinModels number of linear models to train
         *
         */
        public EagerSagUpdaterFactory(final int nRows, final int nFets, final int nLinModels) {
            m_nRows = nRows;
            m_nFets = nFets;
            m_nCats = nLinModels;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public EagerSagUpdater<T> create() {
            return new EagerSagUpdater<>(m_nRows, m_nFets, m_nCats);
        }

    }

}

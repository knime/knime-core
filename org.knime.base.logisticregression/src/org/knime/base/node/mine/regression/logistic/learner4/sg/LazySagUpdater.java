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
 *   29.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import java.util.Arrays;
import java.util.BitSet;

import org.knime.base.node.mine.regression.logistic.learner4.TrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.sg.IndexCache.IndexIterator;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
class LazySagUpdater <T extends TrainingRow> implements LazyUpdater<T> {

    private final double[] m_cummulativeSum;
    private final BitSet m_seen;
    private int m_covered = 0;
    private final double[][] m_gradientSum;
    private final double[][] m_gradientMemory;
    // necessary because the intersect is not regularized
    // and therefore not scaled. But since the intersect dummy feature
    // is always present, we can always store the last stepsize
    private double m_intersectStepSize = 0.0;
    private final int m_nCats;

    /**
     *
     */
    private LazySagUpdater(final int nRows, final int nFets, final int nCats) {
        m_cummulativeSum = new double[nRows];
        m_seen = new BitSet(nRows);
        m_gradientSum = new double[nCats][nFets];
        m_gradientMemory = new double[nCats][nRows];
        m_nCats = nCats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final T x, final double[] sig, final WeightVector<T> beta, final double stepSize,
        final int iteration, final IndexCache indexCache) {
        int idx = x.getId();
        if (!m_seen.get(idx)) {
            m_seen.set(idx);
            m_covered++;
        }

        // update gradient sum
        for (IndexIterator iter = indexCache.getIterator(); iter.hasNext();) {
            int nonZero = iter.next();
            for (int c = 0; c < m_nCats; c++) {
                double newD = x.getFeature(nonZero) * (sig[c] - m_gradientMemory[c][idx]);
                assert Double.isFinite(newD);
                m_gradientSum[c][nonZero] += newD;
            }
        }

        // update gradient memory
        for (int c = 0; c < m_nCats; c++) {
            m_gradientMemory[c][idx] = sig[c];
        }



        double prev = iteration == 0 ? 0 : m_cummulativeSum[iteration - 1];
        double scale = beta.getScale();
        m_cummulativeSum[iteration] = prev + stepSize / (scale * m_covered);
        // the intersect is not scaled!
        assert x.getFeature(0) == 1.0 : "The artificial intercept feature must always be 1!";
        m_intersectStepSize = stepSize / m_covered;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lazyUpdate(final WeightVector<T> beta, final T x, final IndexCache indexCache, final int[] lastVisited, final int iteration) {
        if (iteration > 0) {
            int lastValid = iteration - 1;
            beta.update((val, c, i) -> doLazyUpdate(val, c, i, lastVisited[i], lastValid), true, indexCache);
        }


    }

    private double doLazyUpdate(final double betaVal, final int cat, final int featureIndex, final int lastVisited, final int lastValidIdx) {
        // intersect is handled separately
        if (featureIndex == 0) {
            double ret = betaVal - m_intersectStepSize * m_gradientSum[cat][featureIndex];
            assert Double.isFinite(ret) : "Update results in non finite beta coefficient.";
            return ret;
        }
        if (lastVisited == 0) {
            double ret = betaVal - (m_cummulativeSum[lastValidIdx]) * m_gradientSum[cat][featureIndex];
            assert Double.isFinite(ret) : "Update results in non finite beta coefficient.";
            return ret;
        }
        double ret = betaVal - (m_cummulativeSum[lastValidIdx] - m_cummulativeSum[lastVisited - 1]) * m_gradientSum[cat][featureIndex];
        assert Double.isFinite(ret) : "Update results in non finite beta coefficient.";
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void normalize(final WeightVector<T> beta, final int[] lastVisited, final int iteration) {
        beta.update((val, c, i) -> doLazyUpdate(val, c, i, lastVisited[i], iteration), true);
        m_cummulativeSum[iteration] = 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetJITSystem(final WeightVector<T> beta, final int[] lastVisited) {
        int lastIteration = m_cummulativeSum.length - 1;
        beta.update((val, c, i) -> doLazyUpdate(val, c, i, lastVisited[i], lastIteration), true);
        Arrays.fill(m_cummulativeSum, 0.0);
    }

    static class LazySagUpdaterFactory <T extends TrainingRow> implements UpdaterFactory<T, LazyUpdater<T>> {

        private final int m_nRows;
        private final int m_nFets;
        private final int m_nCats;

        /**
         *
         */
        public LazySagUpdaterFactory(final int nRows, final int nFets, final int nCats) {
            m_nRows = nRows;
            m_nFets = nFets;
            m_nCats = nCats;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LazySagUpdater<T> create() {
            return new LazySagUpdater<>(m_nRows, m_nFets, m_nCats);
        }

    }

}

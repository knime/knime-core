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
 *   30.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
class LazyPriorUpdater extends AbstractPriorUpdater implements LazyRegularizationUpdater {

    private final double[] m_cummulativeSum;

    public LazyPriorUpdater(final Prior prior, final int nRows, final boolean clip) {
        super(prior, nRows, clip);
        m_cummulativeSum = new double[nRows];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final WeightVector<?> beta, final double stepSize, final int iteration) {
        double prev = iteration == 0 ? 0 : m_cummulativeSum[iteration - 1];
        m_cummulativeSum[iteration] = prev + stepSize / getNRows();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lazyUpdate(final WeightVector<?> beta, final TrainingRow row, final int[] lastVisited, final int iteration) {
        if (iteration > 0) {
            double lastCummSum = m_cummulativeSum[iteration - 1];
            if (isClip()) {
                beta.update((val, c, i, f) -> clippedLazyUpdate(val, lastVisited[i], lastCummSum), false, row);
            } else {
                beta.update((val, c, i, f) -> doLazyUpdate(val, lastVisited[i], lastCummSum), false, row);
            }
        }
    }

    private double doLazyUpdate(final double betaValue, final int lastVisited, final double lastCummSum) {
        double prev = lastVisited == 0 ? 0.0 : m_cummulativeSum[lastVisited - 1];
        return betaValue - (lastCummSum - prev) * evaluatePrior(betaValue);
    }

    private double clippedLazyUpdate(final double betaValue, final int lastVisited, final double lastCummSum) {
        double prev = lastVisited == 0 ? 0.0 : m_cummulativeSum[lastVisited - 1];
        return clip(betaValue, lastCummSum - prev);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void resetJITSystem(final WeightVector<?> beta, final int[] lastVisited) {
        double lastCummSum = m_cummulativeSum[getNRows() - 1];
        // intercept is not regularized
        beta.update((val, c, i) -> doLazyUpdate(val, lastVisited[i], lastCummSum), false);

    }


}

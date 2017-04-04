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
 *   24.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sag;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.TrainingRow;

/**
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T> The type of row we are dealing with
 * @param <U> The type of updater to use for updates
 */
abstract class AbstractSGOptimizer <T extends TrainingRow, U extends Updater<T>, R extends RegularizationUpdater> {

    private final UpdaterFactory<T, U> m_updaterFactory;
    private final Loss<T> m_loss;
    private final R m_regUpdater;
    private final LearningRateStrategy<T> m_lrStrategy;
    private final StoppingCriterion<T> m_stoppingCriterion;
    private final TrainingData<T> m_data;

    /**
     *
     */
    public AbstractSGOptimizer(final TrainingData<T> data, final Loss<T> loss, final UpdaterFactory<T, U> updaterFactory,
        final R prior, final LearningRateStrategy<T> learningRateStrategy,
        final StoppingCriterion<T> stoppingCriterion) {
        m_loss = loss;
        m_regUpdater = prior;
        m_lrStrategy = learningRateStrategy;
        m_updaterFactory = updaterFactory;
        m_stoppingCriterion = stoppingCriterion;
        m_data = data;
    }

    public double[][] optimize(final int maxEpoch, final TrainingData<T> data) {

        final int nRows = data.getRowCount();
        final int nFets = data.getFeatureCount() + 1;
        final int nCats = data.getTargetDimension();
        final U updater = m_updaterFactory.create();
        final IndexCache indexCache = createIndexCache(nFets);

        final WeightVector<T> beta = new SimpleWeightVector<>(nFets, nCats, true);

        for (int epoch = 0; epoch < maxEpoch; epoch++) {
            // notify learning rate strategy that a new epoch starts
            m_lrStrategy.startNewEpoch(epoch);
            for (int k = 0; k < nRows; k++) {
                T x = data.getRandomRow();
                indexCache.prepareRow(x);
                prepareIteration(beta, x, updater, m_regUpdater, k, indexCache);
                double[] prediction = beta.predict(x, indexCache);
                double[] sig = m_loss.gradient(x, prediction);
                double stepSize = m_lrStrategy.getCurrentLearningRate(x, prediction, sig);
//                System.out.println("Stepsize: " + stepSize);
                // beta is updated in two steps
                m_regUpdater.update(beta, stepSize, k);
//                updater.update(x, sig, beta, stepSize, k);
                performUpdate(x, updater, sig, beta, stepSize, k, indexCache);
                double scale = beta.getScale();
                if (scale > 1e10 || scale < -1e10 || (scale > 0 && scale < 1e-10) || (scale < 0 && scale > -1e-10)) {
                    normalize(beta, updater, k);
                    beta.normalize();
                }
            }
            postProcessEpoch(beta, updater, m_regUpdater);
            if (m_stoppingCriterion.checkConvergence(beta)) {
                System.out.println("Convergence reached after " + (epoch + 1) + " epochs.");
                break;
            }
        }

        return beta.getWeightVector();
    }

    private RealMatrix calculateCovariateMatrix(final WeightVector<T> beta) {
        final double[][] hessian = m_loss.hessian(m_data, beta);
        RealMatrix observedInformation = MatrixUtils.createRealMatrix(hessian).scalarMultiply(-1);
        return MatrixUtils.inverse(observedInformation);
    }

    protected abstract void normalize(final WeightVector<T> beta, final U updater, final int iteration);

    protected abstract void prepareIteration(final WeightVector<T> beta, final T x, final U updater, final R regUpdater,
        int iteration, final IndexCache indexCache);

    protected abstract void postProcessEpoch(final WeightVector<T> beta, final U updater, final R regUpdater);

    protected abstract IndexCache createIndexCache(final int nFets);

    protected abstract void performUpdate(final T x, final U updater, final double[] gradient, final WeightVector<T> beta,
        final double stepSize, final int iteration, final IndexCache indexCache);

    protected TrainingData<T> getData() {
        return m_data;
    }

}

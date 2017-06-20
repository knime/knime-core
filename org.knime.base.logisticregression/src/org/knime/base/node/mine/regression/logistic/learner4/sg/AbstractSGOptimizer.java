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
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import java.util.Optional;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerResult;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;
import org.knime.core.node.CanceledExecutionException;

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
    private final boolean m_calcCovMatrix;
    private String m_warning = null;

    /**
     *
     */
    public AbstractSGOptimizer(final TrainingData<T> data, final Loss<T> loss, final UpdaterFactory<T, U> updaterFactory,
        final R prior, final LearningRateStrategy<T> learningRateStrategy,
        final StoppingCriterion<T> stoppingCriterion, final boolean calcCovMatrix) {
        m_loss = loss;
        m_regUpdater = prior;
        m_lrStrategy = learningRateStrategy;
        m_updaterFactory = updaterFactory;
        m_stoppingCriterion = stoppingCriterion;
        m_data = data;
        m_calcCovMatrix = calcCovMatrix;
    }

    public LogRegLearnerResult optimize(final int maxEpoch, final TrainingData<T> data, final Progress progress) throws CanceledExecutionException {

        final int nRows = data.getRowCount();
        final int nFets = data.getFeatureCount();
        final int nCats = data.getTargetDimension();
        final U updater = m_updaterFactory.create();
//        final IndexCache indexCache = createIndexCache(nFets);

        final WeightVector<T> beta = new SimpleWeightVector<>(nFets, nCats, true);
        int epoch = 0;
        for (; epoch < maxEpoch; epoch++) {
            // notify learning rate strategy that a new epoch starts
            m_lrStrategy.startNewEpoch(epoch);
            progress.setProgress(((double)epoch) / maxEpoch, "Start epoch " + epoch + " of " + maxEpoch);
            for (int k = 0; k < nRows; k++) {
                progress.checkCanceled();
                T x = data.getRandomRow();
//                indexCache.prepareRow(x);
                prepareIteration(beta, x, updater, m_regUpdater, k/*, indexCache*/);
                double[] prediction = beta.predict(x/*, indexCache*/);
                double[] sig = m_loss.gradient(x, prediction);
                double stepSize = m_lrStrategy.getCurrentLearningRate(x, prediction, sig);
//                System.out.println("Stepsize: " + stepSize);
                // beta is updated in two steps
                m_regUpdater.update(beta, stepSize, k);
//                updater.update(x, sig, beta, stepSize, k);
                performUpdate(x, updater, sig, beta, stepSize, k/*, indexCache*/);
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
        StringBuilder warnBuilder = new StringBuilder();
        if (epoch == maxEpoch) {
            warnBuilder.append("The algorithm did not reach convergence. "
                + "Setting the epoch limit higher might result in a better model.");
        }
        double lossSum = totalLoss(beta);
        RealMatrix betaMat = MatrixUtils.createRealMatrix(beta.getWeightVector());
        RealMatrix covMat = null;
        if (m_calcCovMatrix) {
            try {
                covMat = calculateCovariateMatrix(beta);
            } catch (SingularMatrixException e) {
                if (warnBuilder.length() > 0) {
                    warnBuilder.append("\n");
                }
                warnBuilder.append("The covariance matrix could not be calculated because the"
                    + " observed fisher information matrix was singular.");
                covMat = null;
            }
        }

        return new LogRegLearnerResult(betaMat, covMat, epoch + 1, lossSum);
    }

    private double totalLoss(final WeightVector<T> beta) {
        double lossSum = 0.0;
        for (T x : m_data) {
            double[] prediction = beta.predict(x);
            lossSum += m_loss.evaluate(x, prediction);
        }
        return lossSum;
    }

    private RealMatrix calculateCovariateMatrix(final WeightVector<T> beta) {
        final RealMatrix llHessian = MatrixUtils.createRealMatrix(m_loss.hessian(m_data, beta));
        final RealMatrix priorHessian = m_regUpdater.hessian(beta);
        RealMatrix observedInformation = llHessian.add(priorHessian);
        RealMatrix covMat = new QRDecomposition(observedInformation).getSolver().getInverse().scalarMultiply(-1);
        return covMat;
    }

    protected abstract void normalize(final WeightVector<T> beta, final U updater, final int iteration);

    protected abstract void prepareIteration(final WeightVector<T> beta, final T x, final U updater, final R regUpdater,
        int iteration/*, final IndexCache indexCache*/);

    protected abstract void postProcessEpoch(final WeightVector<T> beta, final U updater, final R regUpdater);

    protected abstract void performUpdate(final T x, final U updater, final double[] gradient, final WeightVector<T> beta,
        final double stepSize, final int iteration/*, final IndexCache indexCache*/);

    protected TrainingData<T> getData() {
        return m_data;
    }

    /**
     * Returns an {@link Optional} that can contain a warning message.
     *
     * @return an {@link Optional} possibly containing a warning message
     */
    Optional<String> getWarning() {
        return Optional.ofNullable(m_warning);
    }

}

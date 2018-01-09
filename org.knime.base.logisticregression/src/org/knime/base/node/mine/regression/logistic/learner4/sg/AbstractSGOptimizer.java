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
 * Abstract implementation for stochastic gradient descent like optimization scheme.
 * Serves as base class for both eager and lazy approaches.
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
     * Creates an AbstractSGOptimizer.
     *
     * @param data training data to learn on
     * @param loss function to minimize
     * @param updaterFactory factory for the updater
     * @param regularizationUpdater performs updates of the regularization term
     * @param learningRateStrategy policy for the learning rate for example a constant learning rate
     * @param stoppingCriterion determines when to stop the training
     * @param calcCovMatrix flag that indicates whether the coefficient covariance matrix should be calculated
     *
     */
    public AbstractSGOptimizer(final TrainingData<T> data, final Loss<T> loss, final UpdaterFactory<T, U> updaterFactory,
        final R regularizationUpdater, final LearningRateStrategy<T> learningRateStrategy,
        final StoppingCriterion<T> stoppingCriterion, final boolean calcCovMatrix) {
        m_loss = loss;
        m_regUpdater = regularizationUpdater;
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

        final WeightMatrix<T> beta = new SimpleWeightMatrix<>(nFets, nCats, true);
        int epoch = 0;
        for (; epoch < maxEpoch; epoch++) {
            // notify learning rate strategy that a new epoch starts
            m_lrStrategy.startNewEpoch(epoch);
            progress.setProgress(((double)epoch) / maxEpoch, "Start epoch " + epoch + " of " + maxEpoch);
            for (int k = 0; k < nRows; k++) {
                progress.checkCanceled();
                T x = data.getRandomRow();
                prepareIteration(beta, x, updater, m_regUpdater, k);
                double[] prediction = beta.predict(x);
                double[] sig = m_loss.gradient(x, prediction);
                double stepSize = m_lrStrategy.getCurrentLearningRate(x, prediction, sig);
                // beta is updated in two steps
                m_regUpdater.update(beta, stepSize, k);
                performUpdate(x, updater, sig, beta, stepSize, k);
                double scale = beta.getScale();
                if (scale > 1e10 || scale < -1e10 || (scale > 0 && scale < 1e-10) || (scale < 0 && scale > -1e-10)) {
                    normalize(beta, updater, k);
                    beta.normalize();
                }
            }
            postProcessEpoch(beta, updater, m_regUpdater);
            if (m_stoppingCriterion.checkConvergence(beta)) {
                break;
            }
        }
        StringBuilder warnBuilder = new StringBuilder();
        if (epoch >= maxEpoch) {
            warnBuilder.append("The algorithm did not reach convergence after the specified number of epochs. "
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

        m_warning = warnBuilder.length() > 0 ? warnBuilder.toString() : null;

        // -lossSum because we minimize the negative loglikelihood but one is usually more interested in the likelihood
        // in a maximum likelihood sense
        return new LogRegLearnerResult(betaMat, covMat, epoch, -lossSum);
    }

    /**
     * Calculates the sum of losses of all rows.
     *
     * @param beta coefficient matrix
     * @return
     */
    private double totalLoss(final WeightMatrix<T> beta) {
        double lossSum = 0.0;
        for (T x : m_data) {
            double[] prediction = beta.predict(x);
            lossSum += m_loss.evaluate(x, prediction);
        }
        return lossSum;
    }

    private RealMatrix calculateCovariateMatrix(final WeightMatrix<T> beta) {
        final RealMatrix llHessian = MatrixUtils.createRealMatrix(m_loss.hessian(m_data, beta));
        final RealMatrix priorHessian = m_regUpdater.hessian(beta);
        RealMatrix observedInformation = llHessian.add(priorHessian);
        RealMatrix covMat = new QRDecomposition(observedInformation).getSolver().getInverse().scalarMultiply(-1);
        return covMat;
    }

    /**
     * Normalize the coefficient matrix <b>beta</b>.
     *
     * @param beta current estimate of the coefficient matrix
     * @param updater the updater used for the current run
     * @param iteration current iteration
     */
    protected abstract void normalize(final WeightMatrix<T> beta, final U updater, final int iteration);

    /**
     * Prepare a new iteration with row <b>x</b>.
     *
     * @param beta current estimate of the coefficient matrix
     * @param x the currently looked at row
     * @param updater the loss updater used for this training run
     * @param regUpdater the regularization updater
     * @param iteration the current iteration
     */
    protected abstract void prepareIteration(final WeightMatrix<T> beta, final T x, final U updater, final R regUpdater,
        int iteration);

    /**
     * Perform any operations necessary to finalize a single epoch.
     *
     * @param beta current estimate of the coefficient matrix
     * @param updater the loss updater used for this training run
     * @param regUpdater the regularization updater
     */
    protected abstract void postProcessEpoch(final WeightMatrix<T> beta, final U updater, final R regUpdater);

    /**
     * Perform the updates of the coefficients in <b>beta</b>.
     * Note that <b>sig</b> is only the partial gradient and needs to be multiplied with the feature values of <b>x</b> to
     * obtain the actual gradient.
     *
     * @param x the currently looked at row
     * @param updater the loss updater used in the current training run
     * @param gradient the partial gradient for all linear models (classes)
     * @param beta the current estimate of the coefficient matrix
     * @param stepSize or learning rate for gradient descent
     * @param iteration the current iteration
     */
    protected abstract void performUpdate(final T x, final U updater, final double[] gradient, final WeightMatrix<T> beta,
        final double stepSize, final int iteration);

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

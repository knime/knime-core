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
 *   10.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import java.util.Optional;

import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearner;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerResult;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.Solver;
import org.knime.base.node.mine.regression.logistic.learner4.data.ClassificationTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.sg.LineSearchLearningRateStrategy.StepSizeType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeProgressMonitor;

/**
 * LogRegLearner implementation that uses the SAG algorithm to find the model.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class SagLogRegLearner implements LogRegLearner {

    private LogRegLearnerSettings m_settings;
    private String m_warning;

    /**
     *
     */
    public SagLogRegLearner(final LogRegLearnerSettings settings) {
        m_settings = settings;
    }

    private LearningRateStrategy<ClassificationTrainingRow> createLearningRateStrategy(
        final LogRegLearnerSettings settings, final boolean isSag,
        final TrainingData<ClassificationTrainingRow> data, final Loss<ClassificationTrainingRow> loss) throws InvalidSettingsException {
        switch (settings.getLearningRateStrategy()) {
            case Annealing:
                if (isSag) {
                    throw new InvalidSettingsException("Stochastic average gradient does not support the annealing"
                        + " learning rate strategy.");
                }
                return new AnnealingLearningRateStrategy<>(
                        settings.getLearningRateDecay(), settings.getInitialLearningRate());
            case Fixed:
                return new FixedLearningRateStrategy<>(settings.getInitialLearningRate());
            case LineSearch:
                if (!isSag) {
                    throw new InvalidSettingsException("Stochastic gradient descent does not support the "
                        + "line search learning rate strategy.");
                }
                double lambda = 1 / settings.getPriorVariance();
                return new LineSearchLearningRateStrategy<>(data, loss, lambda, StepSizeType.Default);
            default:
                throw new InvalidSettingsException("Unknown learning rate strategy \"" + settings.getLearningRateStrategy() + "\".");
        }
    }

    private RegularizationUpdater createRegularizationUpdater(final LogRegLearnerSettings settings,
        final TrainingData<ClassificationTrainingRow> data) throws InvalidSettingsException {
        Prior prior;
        switch (settings.getPrior()) {
            case Gauss:
                prior = new GaussPrior(settings.getPriorVariance());
                break;
            case Laplace:
                prior = new LaplacePrior(settings.getPriorVariance());
                break;
            case Uniform:
                return UniformRegularizationUpdater.INSTANCE;
            default:
                throw new InvalidSettingsException("Unknown prior type \"" + settings.getPrior() + "\".");
        }
        if (settings.isPerformLazy()) {
            return new LazyPriorUpdater(prior, data.getRowCount(), true);
        } else {
            return new EagerPriorUpdater(prior, data.getRowCount(), true);
        }
    }

    private UpdaterFactory<ClassificationTrainingRow, LazyUpdater<ClassificationTrainingRow>> createLazyUpdater(
        final LogRegLearnerSettings settings, final TrainingData<ClassificationTrainingRow> data) {
        assert settings.isPerformLazy() : "This method should only be called if a lazy updater is required.";
        int nRows = data.getRowCount();
        int nFets = data.getFeatureCount();
        int betaDim = data.getTargetDimension();
        switch (settings.getSolver()) {
            case IRLS:
                throw new IllegalStateException("IRLS as solver in SG Framework detected. This indicates a coding error in the settings propagation.");
            case SAG:
                    return new LazySagUpdater.LazySagUpdaterFactory<ClassificationTrainingRow>(nRows, nFets, betaDim);
            case SGD:
                    throw new IllegalStateException("Currently is no lazy implementation for stochastic gradient descent available.");
            default:
                throw new IllegalArgumentException("The solver \"" + settings.getSolver() + "\" is unknown.");
        }
    }

    private UpdaterFactory<ClassificationTrainingRow, EagerUpdater<ClassificationTrainingRow>> createEagerUpdater(
        final LogRegLearnerSettings settings, final TrainingData<ClassificationTrainingRow> data) {
        assert !settings.isPerformLazy() : "This method should only be called if an eager updater is required.";
        int nRows = data.getRowCount();
        int nFets = data.getFeatureCount();
        int betaDim = data.getTargetDimension();
        switch (settings.getSolver()) {
            case IRLS:
                throw new IllegalStateException("IRLS as solver in SG Framework detected. This indicates a coding error in the settings propagation.");
            case SAG:
                return new EagerSagUpdater.EagerSagUpdaterFactory<>(nRows, nFets, betaDim);
            case SGD:
                return new EagerSgdUpdater.EagerSgdUpdaterFactory<>();
            default:
                throw new IllegalArgumentException("The solver \"" + settings.getSolver() + "\" is unknown.");
        }
    }

    private AbstractSGOptimizer createOptimizer(
        final LogRegLearnerSettings settings, final TrainingData<ClassificationTrainingRow> data) throws InvalidSettingsException {
        final Loss<ClassificationTrainingRow> loss = MultinomialLoss.INSTANCE;
        final StoppingCriterion<ClassificationTrainingRow> stoppingCriterion =
                new BetaChangeStoppingCriterion<>(data.getFeatureCount(), data.getTargetDimension(), settings.getEpsilon());
        LearningRateStrategy<ClassificationTrainingRow> lrs = createLearningRateStrategy(settings, settings.getSolver() == Solver.SAG, data, loss);
        RegularizationUpdater regUpdater = createRegularizationUpdater(settings, data);
        if (settings.isPerformLazy()) {
            UpdaterFactory<ClassificationTrainingRow, LazyUpdater<ClassificationTrainingRow>> updaterFactory = createLazyUpdater(settings, data);
            return new LazySGOptimizer<ClassificationTrainingRow, LazyUpdater<ClassificationTrainingRow>, LazyRegularizationUpdater>(
                    data, loss, updaterFactory, (LazyRegularizationUpdater)regUpdater, lrs, stoppingCriterion, m_settings.isCalcCovMatrix());
        } else {
            UpdaterFactory<ClassificationTrainingRow, EagerUpdater<ClassificationTrainingRow>> updaterFactory = createEagerUpdater(settings, data);
            return new EagerSgOptimizer<>(data, loss, updaterFactory, regUpdater, lrs, stoppingCriterion, m_settings.isCalcCovMatrix());

        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogRegLearnerResult learn(final TrainingData<ClassificationTrainingRow> data, final ExecutionMonitor progressMonitor)
        throws CanceledExecutionException, InvalidSettingsException {
        AbstractSGOptimizer sgOpt = createOptimizer(m_settings, data);

        SimpleProgress progMon = new SimpleProgress(progressMonitor.getProgressMonitor());
        LogRegLearnerResult result = sgOpt.optimize(m_settings.getMaxEpoch(), data, progMon);
        Optional<String> warning = sgOpt.getWarning();
        if (warning.isPresent()) {
            m_warning = warning.get();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWarningMessage() {
        return m_warning;
    }

    private class SimpleProgress implements Progress {

        private final NodeProgressMonitor m_progMon;

        /**
         * @param progMon a {@link NodeProgressMonitor} for example a execution context
         *
         */
        public SimpleProgress(final NodeProgressMonitor progMon) {
            m_progMon = progMon;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProgress(final double progress) {
            m_progMon.setProgress(progress);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProgress(final double progress, final String message) {
            m_progMon.setProgress(progress, message);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void checkCanceled() throws CanceledExecutionException {
            m_progMon.checkCanceled();
        }

    }

}

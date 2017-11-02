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

import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;

/**
 * Eager implementation of stochastic gradient descent like optimization schemes for linear models.
 *
 * @author Adrian Nembach, KNIME.com
 */
final class EagerSgOptimizer <T extends TrainingRow, U extends EagerUpdater<T>, R extends RegularizationUpdater> extends AbstractSGOptimizer<T, U, R> {

    /**
     * @param data the training data to learn on
     * @param loss the loss function to minimize
     * @param updaterFactory a factory object for loss updater objects
     * @param regularizationUpdater used to perform regularization updates
     * @param learningRateStrategy scheme for the learning rate or step size
     * @param stoppingCriterion criterion that indicates when to stop training
     * @param calcCovMatrix flag that indicates whether the cofficient covariance matrix should be calculated
     */
    public EagerSgOptimizer(final TrainingData<T> data, final Loss<T> loss, final UpdaterFactory<T, U> updaterFactory, final R regularizationUpdater,
        final LearningRateStrategy<T> learningRateStrategy, final StoppingCriterion<T> stoppingCriterion, final boolean calcCovMatrix) {
        super(data, loss, updaterFactory, regularizationUpdater, learningRateStrategy, stoppingCriterion, calcCovMatrix);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareIteration(final WeightMatrix<T> beta, final T x, final U updater, final R regUpdater, final int iteration) {
        // nothing to prepare

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void postProcessEpoch(final WeightMatrix<T> beta, final U updater, final R regUpdater) {
        // nothing to postprocess
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void performUpdate(final T x, final U updater, final double[] gradient, final WeightMatrix<T> beta, final double stepSize,
        final int iteration) {
        updater.update(x, gradient, beta, stepSize, iteration);
    }




    /**
     * {@inheritDoc}
     */
    @Override
    protected void normalize(final WeightMatrix<T> beta, final U updater, final int iteration) {
        // nothing to do

    }

}

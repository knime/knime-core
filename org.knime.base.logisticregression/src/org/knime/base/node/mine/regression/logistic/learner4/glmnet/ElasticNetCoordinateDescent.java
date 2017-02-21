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
 *   08.02.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.glmnet;

import java.util.BitSet;

/**
 * Solves the elastic net problem:
 * min R_lambda(beta_0,beta) = min ( 1/(2N) sum_i (y_i-beta_0 - x_i^T * beta)^2 + lambda* P_alpha(beta)
 *
 * Where P_alpha(beta) = sum_j=1^p ((1/2)(1 - alpha) beta_j^2 + alpha |beta_j|).
 *
 * alpha specifies the weight of the l1 norm in the elastic net penalty.
 * lambda specifies the impact of the penalty on the objective function.
 *
 * @author Adrian Nembach, KNIME.com
 */
final class ElasticNetCoordinateDescent {

    final private double m_alpha;
    final private UpdateStrategy m_updateStrategy;
    final private FeatureRegularization m_featureRegularization;
    final private TrainingData<?> m_data;

    ElasticNetCoordinateDescent(final TrainingData<?> data, final UpdateStrategy updateStrategy,
        final FeatureRegularization featureRegularization, final double alpha) {
        m_alpha = alpha;
        m_updateStrategy = updateStrategy;
        m_data = data;
        m_featureRegularization = featureRegularization;
    }

    /**
     * Fits a linear regression models to the data for the provided targets.
     * The coefficients are stored in <b>beta</b>.
     * The returned integer is the number of iterations to convergence.
     * @param beta coefficient array
     * @param lambda weight of regularization in objective function
     * @param targets target values.
     * @return number of iterations until convergence
     */
    int fit(final double[] beta, final double lambda, final double[] targets) {
        assert m_data.getFeatureCount() + 1 == beta.length: "beta array does not match feature count.";
        assert m_data.getRowCount() == targets.length : "target array does not match row count.";
//        beta[0] = calculateIntercept(targets);
        m_updateStrategy.initialize(beta, targets);
        ActiveSet activeSet = new MutableActiveSet(beta.length);
        return fitModel(lambda, beta, activeSet);
    }


//    /**
//     * Fits a regression model on the provided data.
//     *
//     * @param lambda Weight of regularization in objective function
//     * @return a 1 x (p+1) matrix of the fitted coefficients
//     */
//    public double[] fit(final double lambda) {
//        final double[] beta = new double[m_data.getRegressorCount() + 1];
//
//        beta[0] = calculateIntercept();
//
//        return fitModel(lambda, beta);
//    }

    /**
     * Fits the linear model and stores the coefficients in <b>beta</b>.
     * @param lambda weight of penalty in objective function
     * @param beta array in which the coefficients are stored
     * @param activeSet active set implementation to use for calculations
     * @return the number of iterations of the outer loop
     */
    private int fitModel(final double lambda, final double[] beta, final ActiveSet activeSet) {
        int iter = 0;
        for (boolean betaChanged = true; betaChanged; iter++) {
            betaChanged = false;
            activeSet.newCycle();
            while (activeSet.hasNextActive()) {
                int i = activeSet.nextActive();
                double betaOld = beta[i];
                beta[i] = m_updateStrategy.update(betaOld, i, m_alpha, lambda * m_featureRegularization.getLambda(i));
                if (ElasticNetUtils.withinEpsilon(beta[i], 0.0)) {
                    activeSet.removeActive(i);
                }
                if (!ElasticNetUtils.withinEpsilon(betaOld, beta[i])) {
                    betaChanged = true;
                }
            }
        }
//        System.out.println("Single step iterations: " + iter);

        return iter;
    }

//    private double calculateIntercept() {
//        double sumY = 0.0;
//        for (TrainingRow row : m_data) {
//            sumY += row.getTarget();
//        }
//        return sumY / m_data.getRowCount();
//    }

    private double calculateIntercept(final double[] targets) {
        double sumY = 0.0;
        for (double y : targets) {
            sumY += y;
        }
        return sumY / targets.length;
    }

    private interface ActiveSet {
        /**
         * @return true if there is another active feature in this cycle
         */
        boolean hasNextActive();
        /**
         * @return the index of the next active feature
         */
        int nextActive();
        /**
         * @param feature the index of the feature that should be active
         */
        void addActive(final int feature);

        /**
         * @param feature the index of the feature that should no longer be active
         */
        void removeActive(final int feature);

        /**
         * Start a new cycle i.e. go back to the beginning of the list of active features
         */
        void newCycle();
    }

    /**
     * An ActiveSet implementation that allows to add and remove features from the active set.
     *
     * @author Adrian Nembach, KNIME.com
     */
    private class MutableActiveSet implements ActiveSet {

        private final BitSet m_active;
        private int m_curr = 0;

        public MutableActiveSet(final int numFeatures) {
            m_active = new BitSet(numFeatures);
            m_active.set(0, numFeatures);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNextActive() {
            return m_curr >= 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int nextActive() {
            int next = m_curr;
            m_curr = m_active.nextSetBit(m_curr + 1);
            return next;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addActive(final int feature) {
            m_active.set(feature, true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void newCycle() {
            m_curr = m_active.nextSetBit(0);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeActive(final int feature) {
            m_active.set(feature, false);
        }

    }

    /**
     * This implementation of ActiveSet always considers all features to be active.
     *
     * @author Adrian Nembach, KNIME.com
     */
    private class AllActiveSet implements ActiveSet {

        final int m_numFeatures;
        int m_curr = 0;

        public AllActiveSet(final int numFeatures) {
            m_numFeatures = numFeatures;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNextActive() {
            return m_curr < m_numFeatures;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int nextActive() {
            return m_curr++;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addActive(final int feature) {
            // do nothing all features are always active
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void newCycle() {
            m_curr = 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeActive(final int feature) {
            // do nothing all features are always active
        }

    }

}

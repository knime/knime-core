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

import org.apache.commons.math3.util.MathUtils;

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
class ElasticNetCoordinateDescent {

    final private double m_alpha;
    final private UpdateStrategy m_updateStrategy;
    final private TrainingData<?> m_data;

    public ElasticNetCoordinateDescent(final TrainingData<?> data, final UpdateStrategy updateStrategy, final double alpha) {
        m_alpha = alpha;
        m_updateStrategy = updateStrategy;
        m_data = data;
    }

    public double[] fit(final double[] beta, final double lambda, final double[] targets) {
        m_updateStrategy.initialize(beta, targets);
        return fitModel(lambda, beta);
    }

    public double[] fit(final double lambda, final double[] targets) {
        if (targets.length != m_data.getRowCount()) {
            throw new IllegalArgumentException("The number of targets ("
                    + targets.length + ") does not match the number of rows ("
                    + m_data.getRowCount() + ").");
        }
        double[] beta = new double[m_data.getFeatureCount() + 1];
        beta[0] = calculateIntercept(targets);
        return fitModel(lambda, beta);
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

    private double[] fitModel(final double lambda, final double[] beta) {
        for (boolean betaChanged = false; betaChanged;) {
            for (int i = 1; i < beta.length; i++) {
                // beta[0] is the intercept term
                double betaOld = beta[i];
                beta[i] = m_updateStrategy.update(betaOld, i - 1, m_alpha, lambda);
                if (!MathUtils.equals(betaOld, beta[i])) {
                    betaChanged = true;
                }
            }
        }

        return beta;
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


}

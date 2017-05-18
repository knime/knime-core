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
 *   17.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import java.util.Arrays;

import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;

/**
 * Performs a line search for the optimal lipschitz constant at every iteration.
 * For more details see the paper "Minimizing Finite Sums with the Stochastic Average Gradient".
 *
 * @author Adrian Nembach, KNIME.com
 */
class LineSearchLearningRateStrategy <T extends TrainingRow> implements LearningRateStrategy<T> {

    private final Loss<T> m_loss;
    private final double[] m_squaredNorms;
    private final double m_lambda;
    private final int m_nFets;
    private final StepSizeType m_stepSizeType;
    private final int m_nRows;
    private final double m_lipschitzMultiplier;

    private double m_lipschitz = 1.0;

    enum StepSizeType {
        /**
         * 1 / (lipschitz + lambda)
         */
        Default,
        /**
         * Works sometimes better than the Default but also sometimes worse. </br>
         * 2 / (lipschitz + (nRows + 1) * lambda)
         */
        StronglyConvex;
    }

    /**
     * @param data the training data
     * @param loss the loss function
     * @param lambda the degree of regularization
     * @param stepSizeType the step size type
     *
     */
    public LineSearchLearningRateStrategy(final TrainingData<T> data, final Loss<T> loss,
        final double lambda, final StepSizeType stepSizeType) {
        m_loss = loss;
        m_squaredNorms = new double[data.getRowCount()];
        // initialize with NaN in order to later recognize if the squared norm
        // has been initialized for the respective row
        Arrays.fill(m_squaredNorms, Double.NaN);
        m_lambda = lambda;
        m_nFets = data.getFeatureCount();
        m_nRows = data.getRowCount();
        m_stepSizeType = stepSizeType;
        m_lipschitzMultiplier = Math.pow(2, -1.0/m_nRows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCurrentLearningRate(final T row, final double[] prediction, final double[] gradient) {
        // check if the squared norm has been calculated for the current row
        final int id = row.getId();
        final double squaredNorm;
        if (Double.isNaN(m_squaredNorms[id])) {
            squaredNorm = calculateSquaredNorm(row);
            m_squaredNorms[id] = squaredNorm;
        } else {
            squaredNorm = m_squaredNorms[id];
        }

        // line search for lipschitz
        double currentLoss = m_loss.evaluate(row, prediction);
        double[] newPred = new double[prediction.length];
        calculateNewPrediction(prediction, newPred, gradient, squaredNorm);
        double newLoss = m_loss.evaluate(row, newPred);

        assert Double.isFinite(currentLoss);
        assert Double.isFinite(newLoss);
        double gg = Double.NEGATIVE_INFINITY;

        // use the max squared gradient among all classes
        // this ensures that step size is small enough yet not too small
        // which is what happens if the squared norm of the gradient matrix is used
        // (we have for each class one gradient vector)
        for (int c = 0; c < gradient.length; c++) {
            double g = gradient[c];
            double ngg = g * g * squaredNorm;
            if (ngg > gg) {
                gg = ngg;
            }
        }

//        System.out.println("loss: " + currentLoss + " new loss: " + newLoss + " gg: " + gg + " lipschitz: " + m_lipschitz );

        while (gg > 1.490116119384765625e-8 && newLoss > currentLoss - gg / (2 * m_lipschitz)) {
            m_lipschitz *= 2;
            for (int i = 0; i < newPred.length; i++) {
                newPred[i] = prediction[i] - squaredNorm * gradient[i] / m_lipschitz;
            }
            newLoss = m_loss.evaluate(row, newPred);
        }

        double stepSize;
        // compute stepsize
        switch (m_stepSizeType) {
            case Default:
                stepSize = 1 / (m_lipschitz + m_lambda);
                break;
            case StronglyConvex:
                stepSize = 2 / (m_lipschitz + (m_nRows + 1) * m_lambda);
                break;
            default:
                throw new IllegalStateException("Unknown StepSizeType: " + m_stepSizeType);
        }

        m_lipschitz *= m_lipschitzMultiplier;
        return stepSize;
    }

    private void calculateNewPrediction(final double[] prediction, final double[] newPrediction, final double[] gradient, final double squaredNorm) {
        for (int i = 0; i < newPrediction.length; i++) {
            newPrediction[i] = prediction[i] - squaredNorm * gradient[i] / m_lipschitz;
        }
    }


    private double calculateSquaredNorm(final T row) {
        double norm = 0.0;
        // row.getFeature(0) returns always a 1 for the intercept term
        for (int i = 1; i < m_nFets; i++) {
            double fet = row.getFeature(i);
            norm += fet * fet;
        }
        return norm;
    }

}

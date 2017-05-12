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
 *   09.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import org.knime.base.node.mine.regression.logistic.learner4.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.TrainingRow;

/**
 * Optimizer based on the stochastic average gradient method.
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T> The type of TrainingRow we are dealing with
 */
public class SagOptimizer <T extends TrainingRow> {

    private final Loss<T> m_loss;
    private final LearningRateStrategy<T> m_lrStrategy;

    /**
     * @param loss function to minimize
     * @param learningRateStrategy provides the learning rates at each step
     *
     */
    public SagOptimizer(final Loss<T> loss, final LearningRateStrategy<T> learningRateStrategy) {
        m_loss = loss;
        m_lrStrategy = learningRateStrategy;
    }

    /**
     * @param data the training data
     * @param loss the loss function
     * @param maxEpoch the maximum number of iterations
     * @param lambda the degree of regularization
     * @return a matrix of weights for a linear model
     */
    public double[][] optimize(final TrainingData<T> data, final int maxEpoch, final double lambda, final boolean fitIntercept) {
        final int nRows = data.getRowCount();
        final int nFets = data.getFeatureCount();
        final int nCats = data.getTargetDimension();
        // initialize
        double[][] g = new double[nCats - 1][nRows];
        double[][] d = new double[nCats - 1][nFets];
        int nCovered = 0;

        WeightVector<T> w = new SimpleWeightVector<>(nFets, nCats, fitIntercept);
        double[][] oldW = new double[nCats - 1][nFets];

        double oldLoss = Double.POSITIVE_INFINITY;

        // iterate over samples
        for (int k = 0; k < maxEpoch; k++) {
//            data.permute();
            for (int r = 0; r < data.getRowCount(); r++) {
                T row = data.getRandomRow();
                double[] prediction = w.predict(row);
                double[] sig = m_loss.gradient(row, prediction);

                int id = row.getId();
                for (int c = 0; c < nCats - 1; c++) {
                    // TODO exploit sparseness
                    for (int i = 0; i < nFets; i++) {
                        double newD = row.getFeature(i) * (sig[c] - g[c][id]);
                        assert Double.isFinite(newD);
                        d[c][i] += newD;
                    }
                    g[c][id] = sig[c];
                }


                if (nCovered < nRows) {
                    nCovered++;
                }

                double alpha = m_lrStrategy.getCurrentLearningRate(row, prediction, sig);
                w.scale(1.0 - alpha * lambda);

                final int finalCovered = nCovered;
                w.update((val, c, i) -> val - alpha * d[c][i] / finalCovered, true);

//                System.out.println("step size: " + alpha);

                w.normalize();

            }

            // after each epoch check how much the weights changed
            double rc = relativeChange(oldW, w);
            System.out.println("Relative change: " + rc);
            double newLoss = sumLoss(data, w);
            System.out.println("Loss: " + newLoss);
            System.out.println("d norm: " + squaredNorm(d));
            if (rc < 1e-5 /*|| oldLoss < newLoss*/) {
                System.out.println("Converged after " + (k+1) + " epochs (Change: " + rc + ").");
                break;
            }
            oldLoss = newLoss;
        }

        // finalize
        w.normalize();

        return w.getWeightVector();
    }

    private double squaredNorm(final double[][] mat) {
        double norm = 0.0;
        for (int i = 0; i < mat.length; i++) {
            for(int j = 0; j < mat[0].length; j++) {
                double e = mat[i][j];
                norm += e * e;
            }
        }
        return norm;
    }

    private double sumLoss(final TrainingData<T> data, final WeightVector<T> w) {
        double l = 0.0;
        for (T row : data) {
            l += m_loss.evaluate(row, w.predict(row));
        }
        return l;
    }

    private double relativeChange(final double[][] oldW, final WeightVector<T> w) {
        double maxChange = 0.0;
        double maxWeight = 0.0;

        double[][] newW = w.getWeightVector();
        assert oldW.length == newW.length : "Number of categories in oldW and newW don't match.";
        for (int i = 0; i < oldW.length; i++) {
            assert oldW[i].length == newW[i].length : "Number of feature weights in oldW and newW don't match.";
            for (int j = 0; j < oldW[i].length; j++) {
                double val = newW[i][j];
                double absVal = Math.abs(val);
                double absDiff = Math.abs(val - oldW[i][j]);
                maxWeight = absVal > maxWeight ? absVal : maxWeight;
                maxChange = absDiff > maxChange ? absDiff : maxChange;
                oldW[i][j] = val;
            }
        }

        return maxChange / maxWeight;
    }

}

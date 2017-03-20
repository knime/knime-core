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
package org.knime.base.node.mine.regression.logistic.learner4.sag;

import java.util.Iterator;

import org.knime.base.node.mine.regression.logistic.learner4.glmnet.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.TrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.sag.LineSearchLearningRateStrategy.StepSizeType;

/**
 * Optimizer based on the stochastic average gradient method.
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T> The type of TrainingRow we are dealing with
 */
public class SagOptimizer <T extends TrainingRow> {


    /**
     * @param data the training data
     * @param loss the loss function
     * @param maxIter the maximum number of iterations
     * @param lambda the degree of regularization
     * @return a matrix of weights for a linear model
     */
    public double[][] optimize(final TrainingData<T> data, final Loss<T> loss, final int maxIter, final double lambda) {
        final int nRows = data.getRowCount();
        final int nFets = data.getFeatureCount() + 1;
        final int nCats = data.getTargetDimension();
        // initialize
        double[][] g = new double[nCats - 1][nRows];
        double[][] d = new double[nCats - 1][nFets];
        int nCovered = 0;

//        LearningRateStrategy<T> learningRateStrategy = new FixedLearningRateStrategy<>(1e-3);
        LearningRateStrategy<T> learningRateStrategy =
                new LineSearchLearningRateStrategy<>(data, loss, lambda, StepSizeType.Default);

        WeightVector<T> w = new ScaledWeightVector<>(nFets, nCats);
        double[][] oldW = new double[nCats - 1][nFets];

        // iterate over samples
        data.permute();
        Iterator<T> iterator = data.iterator();
        for (int k = 0; k < maxIter; k++) {
            T row;
            if (iterator.hasNext()) {
                row = iterator.next();
            } else {
                data.permute();
                iterator = data.iterator();
                row = iterator.next();
            }

            double[] prediction = w.predict(row);
            double[] sig = loss.gradient(row, prediction);

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

            double alpha = learningRateStrategy.getCurrentLearningRate(row, prediction, sig);
            w.scale(alpha, lambda);

            w.update(alpha, d, nCovered);

            w.checkNormalize();

            // after each epoch check how much the weights changed
            if ((k+1) % nRows == 0 && relativeChangeTooSmall(oldW, w)) {
                break;
            }

        }

        // finalize
        w.finalize(d);

        return w.getWeightVector();
    }

    private boolean relativeChangeTooSmall(final double[][] oldW, final WeightVector<T> w) {
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

        return maxChange / maxWeight < 1e-3;
    }

}

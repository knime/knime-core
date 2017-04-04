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
package org.knime.base.node.mine.regression.logistic.learner4.sag;

import org.knime.base.node.mine.regression.logistic.learner4.glmnet.ClassificationTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.TrainingData;

/**
 * The multinomial loss or cross entropy.
 *
 * @author Adrian Nembach, KNIME.com
 */
public enum MultinomialLoss implements Loss<ClassificationTrainingRow> {

    /**
     * The instance of MultinomialLoss.
     */
    INSTANCE;


    /**
     * {@inheritDoc}
     */
    @Override
    public double evaluate(final ClassificationTrainingRow row, final double[] prediction) {
        final double logSumExp = logSumExp(prediction);
        assert Double.isFinite(logSumExp);
        final int cat = row.getCategory();
        return cat == prediction.length ? logSumExp : logSumExp - prediction[row.getCategory()];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] gradient(final ClassificationTrainingRow row, final double[] prediction) {
        double[] gradient = new double[prediction.length];
        final int cat = row.getCategory();
        double logSumExp = logSumExp(prediction);
        for (int i = 0; i < prediction.length; i++) {
            double p = Math.exp(prediction[i] - logSumExp);
            assert Double.isFinite(p) && p <= 1.0 && p >= 0.0 ;
            gradient[i] = cat == i ? p - 1.0 : p;

        }
        return gradient;
    }

    @Override
    public double[][] hessian(final TrainingData<ClassificationTrainingRow> data, final WeightVector<ClassificationTrainingRow> beta) {

        final int nBetaVecs = data.getTargetDimension() - 1;
        final int nFets = data.getFeatureCount() + 1;
        final int matDim = nBetaVecs * nFets;
        double[][] hessian = new double[matDim][matDim];
        for (ClassificationTrainingRow x : data) {
            double[] prediction = beta.predict(x);
            // happens in place!
            transform2Probabilites(x, prediction);
            // hold one weight at a time fixed
            for (int i = 0; i < matDim; i++) {
                // category of the fixed weight
                int iCat = i / nBetaVecs;
                // feature of the fixed weight
                int iFet = i % nFets;
                // value of the feature for current row
                double iFetVal = x.getFeature(iFet);
                for (int cat = 0; cat < nBetaVecs; cat++) {
                    for (int fet = 0; fet < nFets; fet++) {
                        // value of feature for other weight
                        double fetVal = x.getFeature(fet);
                        double h = iFetVal * fetVal * prediction[iCat];
                        if (iCat == cat) {
                            // update if weight applies to same class
                            h *= prediction[cat] * (1.0 - prediction[cat]);
                        } else {
                            // update if weight applies to different class
                            h *= prediction[iCat] * prediction[cat];
                        }
                        // sum second derivatives over all rows
                        hessian[i][cat * nBetaVecs + fet] += h;
                    }
                }
            }
        }


        return hessian;
    }

    private void transform2Probabilites(final ClassificationTrainingRow x, final double[] prediction) {
        double logSumExp = logSumExp(prediction);
        for (int i = 0; i < prediction.length; i++) {
            prediction[i] = Math.exp(prediction[i] - logSumExp);
        }
    }

    private static double logSumExp(final double[] prediction) {
        double max = 1.0;
        for (int i = 0; i < prediction.length; i++) {
            if (prediction[i] > max) {
                max = prediction[i];
            }
        }
        // -max because the reference category always gets prediction 0
        // and hence its contribution is -max (exp(0) == 1)
        double sum = Math.exp(-max);
        for (int i = 0; i < prediction.length; i++) {
            sum += Math.exp(prediction[i] - max);
        }

        return Math.log(sum) + max;
    }

}

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
 *   10.02.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.glmnet;

import org.apache.commons.math3.util.MathUtils;

/**
 * Performs regularized multinomial regression using coordinate descent in combination with iteratively reweighted least squares.
 *
 * Unlike typical multinomial regression we use a symmetric formulation of the respective class problems which is only estimable
 * if regularization is used
 *
 *
 * @author Adrian Nembach, KNIME.com
 */
public class MultinomialRegression extends AbstractLogisticRegression {

    enum ProblemFormulation {
        /**
         * Holds the beta vector for the last category fixed at the zero vector.
         * Used in most multinomial regressions that are not regularized.
         */
        Conventional,
        /**
         * Calculates a beta vector for each category.
         * Not estimable without regularization.
         */
        Symmetric;
    }

    private final ProblemFormulation m_pf;
    private final PathStrategy m_pathStrategy;
    private final double m_alpha;

    MultinomialRegression(final ProblemFormulation problemFormulation, final PathStrategy pathStrategy, final double alpha) {
        m_pf = problemFormulation;
        m_pathStrategy = pathStrategy;
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("Alpha must be in the range [0,1] but was " + alpha +".");
        }
        m_alpha = alpha;
    }

    public double[][] fit(final ClassificationTrainingData data) {

        final int numClasses = extractNumberOfClassesFromData(data);
        if (numClasses < 3) {
            throw new IllegalArgumentException("Data contains only two classes. Use LogisticRegression instead.");
        }
        // in the conventional formulation, the beta for the last class is held fixed and is therefore not estimated
        final int classesConsidered = m_pf == ProblemFormulation.Conventional ? numClasses - 1 : numClasses;
        long rowCount = data.getRowCount();
        if (rowCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Tables of that size are currently not supported.");
        }
        int numRows = (int)rowCount;
        int numFeatures = data.getFeatureCount();
        final MutableWeightingStrategy weights = new MutableWeightingStrategy(new double[numRows], 1);
        final NaiveUpdateStrategy updateStrategy = new NaiveUpdateStrategy(data, weights);
        final ElasticNetCoordinateDescent coordDescent = new ElasticNetCoordinateDescent(data, updateStrategy, m_alpha);
        double[] workingResponses = new double[numRows];

        // numFeatures + 1 because beta[][0] is the interception term
        final double[][] beta = new double[classesConsidered][numFeatures + 1];
        // TODO find out how to correctly determine the intercept term beta[][0]

        final ApproximationPreparator approxPrep = new ApproxPreparator(beta, m_pf);

        while (!m_pathStrategy.pathComplete()) {
            final double lambda = m_pathStrategy.getNextLambda();

            // TODO implement mini framework to handle convergence checking
            double loss = calculateAllClassLossSum(data, beta);
            double oldLoss = 0.0;
            while(!MathUtils.equals(loss, oldLoss)) {
                for (int c = 0; c < numClasses; c++) {
                    updateApproximation(weights, workingResponses, data, beta[c], c, approxPrep);
                    beta[c] = coordDescent.fit(beta[c], lambda, workingResponses);
                }
                oldLoss = loss;
                loss = calculateAllClassLossSum(data, beta);
            }
        }

        return beta;
    }


    /**
     * Returns the unnormalized sum of the log-likelihood.
     * To obtain the log-likelihood, we have to divide by the number of rows in <b>data</b> but
     * since it is currently only used to check for convergence, it is not necessary to perform the division.
     * The unnormalized and normalized log-likelihood have the same extrema locations (just with different values)
     * and a division bears potential for further arithmetic errors.
     *
     * @param data
     * @param beta
     * @return the unnormalized log-likelihood
     */
    private double calculateAllClassLossSum(final ClassificationTrainingData data, final double[][] beta) {

        double loss = 0.0;
        int nc = beta.length;

        for (ClassificationTrainingRow x : data) {
            double instanceLoss = 0.0;
            double normalizer = 0.0;
            for (int c = 0; c < nc; c++) {
                int y = x.getCategory();
                double z = calculateResponse(x, beta[c]);
                if (y == c) {
                    instanceLoss += z;
                }
                normalizer += Math.exp(z);
            }
            // beta contains no vector for the last category if
            // the conventional formulation is used because this beta is zero
            // therefore the z of an x of this class is 0 and the contribution
            // to the normalizer is e^0 = 1
            if (m_pf == ProblemFormulation.Conventional) {
                normalizer += 1;
            }
            instanceLoss -= Math.log(normalizer);
            loss += instanceLoss;
        }

        return loss;
    }

    private static class ApproxPreparator extends AbstractApproximationPreparator {
        final private double[][] m_beta;
        final private double m_denominatorModifier;

        public ApproxPreparator(final double[][] beta, final ProblemFormulation pf) {
            m_beta = beta;
            m_denominatorModifier = pf == ProblemFormulation.Conventional ? 1.0 : 0.0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        double prepareProbability(final TrainingRow x) {
            double denom = 0.0;
            for (int i = 0; i < m_beta.length; i++) {
                denom += Math.exp(calculateResponse(x, m_beta[i]));
            }
            // in the conventional problem formulation
            // the beta vector of the last category is set to zero
            // therefore its contribution to the denominator is 1
            denom += m_denominatorModifier;
            return m_response / denom;
        }
    }


}

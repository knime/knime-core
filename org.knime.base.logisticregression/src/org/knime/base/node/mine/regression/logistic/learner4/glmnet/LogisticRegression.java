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
 *   09.02.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.glmnet;

import org.apache.commons.math3.util.MathUtils;

/**
 * Calculates a (binary) logistic regression model using elastic net regularized coordinate descent.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class LogisticRegression extends AbstractLogisticRegression {

    public double[] fit(final ClassificationTrainingData data, final double alpha, final double lambda) {
        final MutableWeightingStrategy weights = new MutableWeightingStrategy(new double[data.getRowCount()], 0);
        final UpdateStrategy updateStrategy = new NaiveUpdateStrategy(data, weights);
        final ElasticNetCoordinateDescent coordDescent = new ElasticNetCoordinateDescent(data, updateStrategy, alpha);
        final ApproximationPreparator approxPreparator = new ApproxPreparator();
        double[] beta = new double[data.getFeatureCount() + 1];
        // TODO find out how to correctly determine intercept
        final double[] workingResponses = new double[data.getRowCount()];

        for (double ll = calculateSumLogLikelihood(data, beta), llOld = 0.0; MathUtils.equals(ll, llOld);
                llOld = ll, ll = calculateSumLogLikelihood(data, beta)) {
            updateApproximation(weights, workingResponses, data, beta, 0, approxPreparator); // here we always look at the first class
            beta = coordDescent.fit(beta, lambda, workingResponses);
        }
        return beta;
    }

    private static class ApproxPreparator extends AbstractApproximationPreparator {

        /**
         * {@inheritDoc}
         */
        @Override
        double prepareProbability(final TrainingRow x) {
            return 1.0 / (1 + Math.exp(-(m_response)));
        }



    }

}

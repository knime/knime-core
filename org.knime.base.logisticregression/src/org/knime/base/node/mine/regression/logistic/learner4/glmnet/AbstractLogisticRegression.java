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

import java.util.Iterator;


/**
 * Contains methods that are used by both, logistic and multinomial regression.
 *
 * @author Adrian Nembach, KNIME.com
 */
abstract class AbstractLogisticRegression {

    static int extractNumberOfClassesFromData(final ClassificationTrainingData data) {
        return data.getCategoryCount();
    }

    static double calculateResponse(final TrainingRow x, final double[] beta) {
        double response = beta[0];
        for (int i = 1; i < beta.length; i++) {
            response += x.getFeature(i - 1) * beta[i];
        }
        return response;
    }

    static double calculateProbability(final double response) {
        return 1.0 / (1 + Math.exp(-response));
    }

    static double calculateSumLogLikelihood(final ClassificationTrainingData data, final double[] beta) {
        double sumLogLikelihood = 0;
        for (ClassificationTrainingRow x : data) {
            double y = x.getCategory() == 0 ? 1 : 0;
            double z = beta[0];
            for (int i = 1; i < beta.length; i++) {
                z += x.getFeature(i - 1) * beta[i];
            }
            sumLogLikelihood += y * z - Math.log(1 + Math.exp(z));
        }
        return sumLogLikelihood;
    }

    void updateApproximation(final MutableWeightingStrategy weights, final double[] workingResponses,
        final ClassificationTrainingData data, final double[] beta, final int currentClass, final ApproximationPreparator approxPreparator) {
        Iterator<ClassificationTrainingRow> iter = data.iterator();
        for (int rn = 0; iter.hasNext(); rn++) {
            ClassificationTrainingRow row = iter.next();
            approxPreparator.prepare(row, beta, currentClass);
            double p = approxPreparator.getProbability();
            double y = approxPreparator.getTargetIndication();
            double weight;
            if (ElasticNetUtils.abs(1.0 - p) < ElasticNetUtils.epsilon) {
                p = 1.0;
                weight = ElasticNetUtils.epsilon;
            } else if (ElasticNetUtils.abs(p) < ElasticNetUtils.epsilon) {
                p = 0.0;
                weight = ElasticNetUtils.epsilon;
            } else {
                weight = p * (1.0 - p);
            }
            workingResponses[rn] = approxPreparator.getResponse() + (y - p) / weight;
            weights.setWeightFor(rn, weight);
        }
    }

    protected interface ApproximationPreparator {
        void prepare(ClassificationTrainingRow row, double[] beta, int currentClass);

        double getResponse();

        double getProbability();

        double getTargetIndication();
    }

    static abstract class AbstractApproximationPreparator implements ApproximationPreparator {
        double m_response;
        double m_targetIndication;
        double m_probability;

        /**
         *
         */
        public AbstractApproximationPreparator() {
        }

        @Override
        public void prepare(final ClassificationTrainingRow x, final double[] beta, final int currentClass) {
            m_targetIndication = x.getCategory() == currentClass ? 1.0 : 0.0;
            m_response = calculateResponse(x, beta);
            m_probability = prepareProbability(x);
        }

        abstract double prepareProbability(final TrainingRow x);

        /**
         * {@inheritDoc}
         */
        @Override
        public double getResponse() {
            return m_response;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getTargetIndication() {
            return m_targetIndication;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getProbability() {
            return m_probability;
        }

    }

}

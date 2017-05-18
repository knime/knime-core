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
 *   17.02.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.glmnet;

import org.knime.base.node.mine.regression.logistic.learner4.data.ClassificationTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingData;

/**
 * Calculates the minimal lambda for which all coefficients are zero and starts the path there.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class MaxPathStrategy implements PathStrategy {
    private final double[] m_lambdas;
    private int m_curr;
    private double m_maxDot;

    public MaxPathStrategy(final double lambda, final double alpha, final int pathLength,
        final TrainingData<ClassificationTrainingRow> data) {
        double maxDot = AbstractLogisticRegression.maxWorkingResponseDotProduct(data);
        m_maxDot = maxDot;
        double lambdaMax = maxDot / (alpha);
        double lambdaMin = 1e-3 * lambdaMax;
        m_lambdas = new double[pathLength];
        double b = Math.exp((Math.log(lambdaMax) - Math.log(lambdaMin)) / pathLength);
        for (int i = 0; i < pathLength; i++) {
            m_lambdas[i] = lambdaMax / Math.pow(b, i + 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean pathComplete() {
        return m_curr >= m_lambdas.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getNextLambda() {
        return m_lambdas[m_curr++];
    }

}

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

import java.util.Iterator;

import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow.FeatureIterator;

/**
 * Naive update strategy that stores the residual for all rows and updates them if a beta changes.
 *
 * A description can be found in section {@literal 2.1} of the paper "Regularization Paths for Generalized Linear Models
 * via Coordinate Descent" by Friedman et {@literal al.} (2010).
 *
 * @author Adrian Nembach, KNIME.com
 */
class NaiveUpdateStrategy<T extends TrainingRow> implements UpdateStrategy {

    private TrainingData<T> m_data;
    private WeightingStrategy m_weights;
    private double[] m_residuals;
    private double[] m_featureCache;

    public NaiveUpdateStrategy(final TrainingData<T> data, final WeightingStrategy weights) {
        m_data = data;
        m_weights = weights;
        if (m_data.getRowCount() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("The current implementation does not support tables of this size.");
        }
        // If the number of rows is bigger than Integer.MAX_Value I believe we will have other problems
        m_residuals = new double[m_data.getRowCount()];
        m_featureCache = new double[m_residuals.length];
    }

    private static double getFeature(final TrainingRow row, final int featureIndex) {
        for (FeatureIterator iter = row.getFeatureIterator(); iter.next();) {
            if (iter.getFeatureIndex() == featureIndex) {
                return iter.getFeatureValue();
            } else if (iter.getFeatureIndex() > featureIndex) {
                return 0.0;
            }
        }
        // can not be reached
        return 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double update(final double betaOld, final int feature, final double alpha, final double lambda) {
        double residualSum = 0;
        double betaSum = 0;
        double weightSum = 0;
        final Iterator<T> iterator = m_data.iterator();
        for (int rn = 0; iterator.hasNext(); rn++) {
            TrainingRow row = iterator.next();
            double x = getFeature(row, feature);
            m_featureCache[rn] = x;
            double wx = m_weights.getWeightFor(rn) * x;
            residualSum += wx * m_residuals[rn];
            weightSum += wx * x;
        }
        betaSum = betaOld * weightSum;

        double loss = residualSum + betaSum;
        double betaNew = ElasticNetUtils.softThresholding(loss, lambda * alpha) / (weightSum + lambda * (1 - alpha));
//        System.out.println("lambda: " + lambda + " betaNew: " + betaNew);
        if (!ElasticNetUtils.withinEpsilon(betaOld, betaNew)) {
            // beta changed therefore we have to update the residuals
//            System.out.println("Change in beta: " + (betaNew - betaOld));
            updateResiduals(feature, betaNew - betaOld);
        }
        return betaNew;
    }

    private void updateResiduals(final int feature, final double betaDiff) {
        for (int i = 0; i < m_residuals.length; i++) {
            double x = m_featureCache[i];
            m_residuals[i] -= x * betaDiff;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final double[] beta, final double[] targets) {
        Iterator<T> iterator = m_data.iterator();
        int i = 0;
        for (; iterator.hasNext(); i++) {
            TrainingRow x = iterator.next();
            double y = targets[i];
            m_residuals[i] = calculateResidual(x, y, beta);
        }
        assert i == m_residuals.length : "Number of rows returned by iterator (" + i +") does not match number of residuals ("
                + m_residuals.length + ")";

    }

    private double calculateResidual(final TrainingRow x, final double y, final double[] beta) {
        double prediction = ElasticNetUtils.calculateResponse(x, beta);
        return y - prediction;
    }

}

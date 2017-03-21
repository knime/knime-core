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
package org.knime.base.node.mine.regression.logistic.learner4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.linear.MatrixUtils;
import org.knime.base.node.mine.regression.RegressionTrainingData;
import org.knime.base.node.mine.regression.RegressionTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.ClassificationTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.sag.MultinomialLoss;
import org.knime.base.node.mine.regression.logistic.learner4.sag.SagOptimizer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;

/**
 * LogRegLearner implementation that uses the SAG algorithm to find the model.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class SagLogRegLearner implements LogRegLearner {

    /**
     * {@inheritDoc}
     */
    @Override
    public LogRegLearnerResult learn(final RegressionTrainingData data, final ExecutionMonitor progressMonitor)
        throws CanceledExecutionException, InvalidSettingsException {
        final SagOptimizer<ClassificationTrainingRow> sagOpt = new SagOptimizer();
        ClassData classData = new ClassData(data);
        MultinomialLoss loss = MultinomialLoss.INSTANCE;
        double alpha = 1e-3;
        double lambda = 0;
        int maxIter = 50;
        double[][] w = sagOpt.optimize(classData, loss, maxIter, lambda);
        return new LogRegLearnerResult(MatrixUtils.createRealMatrix(w), -1, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWarningMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    private class ClassData implements TrainingData<ClassificationTrainingRow> {

        private final List<ClassificationTrainingRow> m_rows;
        private final int m_catCount;
        private final int m_fetCount;
        private int m_iteratorCalls;

        public ClassData(final RegressionTrainingData data) {
            int nrows = (int)data.getRowCount();
            int nfet = data.getRegressorCount();
            m_fetCount = nfet;
            m_rows = new ArrayList<ClassificationTrainingRow>(nrows);
            int id = 0;
            for (RegressionTrainingRow row : data) {
                m_rows.add(new ClassDataRow(row, id++));
            }
            m_catCount = data.getDomainValues().get(data.getTargetIndex()).size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return m_rows.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getFeatureCount() {
            return m_fetCount;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<ClassificationTrainingRow> iterator() {
//            return new ClassDataRowIterator(m_data.iterator());
//            System.out.println("Iterator called " + (++m_iteratorCalls));
            return m_rows.iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getTargetDimension() {
            return m_catCount;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void permute() {
            Collections.shuffle(m_rows);
        }

    }

    private static class ClassDataRow implements ClassificationTrainingRow {

        private final double[] m_data;
        private final int m_cat;
        private final int m_id;

        public ClassDataRow(final RegressionTrainingRow row, final int id) {
            m_data = row.getParameter().getRow(0);
            m_cat = (int)row.getTarget();
            m_id = id;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public double getFeature(final int idx) {
            return idx == 0.0 ? 1.0 : m_data[idx - 1];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCategory() {
            return m_cat;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int getId() {
            return m_id;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "[id: " + m_id + " cat: " + m_cat + "]";
        }

    }

}

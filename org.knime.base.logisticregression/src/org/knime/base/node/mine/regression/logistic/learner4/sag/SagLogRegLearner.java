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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.util.MathUtils;
import org.knime.base.node.mine.regression.RegressionTrainingData;
import org.knime.base.node.mine.regression.RegressionTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearner;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerResult;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.ClassificationTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.TrainingData;
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
        MultinomialLoss loss = MultinomialLoss.INSTANCE;
        ClassData classData = new ClassData(data);
        double lambda = 0.01;
//        LearningRateStrategy<ClassificationTrainingRow> lrStrategy =
//                new LineSearchLearningRateStrategy<ClassificationTrainingRow>(classData, loss, lambda, StepSizeType.Default);
//        LearningRateStrategy<ClassificationTrainingRow> lrStrategy =
//                new AnnealingLearningRateStrategy<ClassificationTrainingRow>(3, 1e-3);
        LearningRateStrategy<ClassificationTrainingRow> lrStrategy = new FixedLearningRateStrategy<ClassificationTrainingRow>(1e-1);
        final SagOptimizer<ClassificationTrainingRow> sagOpt = new SagOptimizer<>(loss, lrStrategy);
        UpdaterFactory<ClassificationTrainingRow, LazyUpdater<ClassificationTrainingRow>> updaterFactory =
                new LazySagUpdater.LazySagUpdaterFactory<ClassificationTrainingRow>(classData.getRowCount(), classData.getFeatureCount() + 1, classData.getTargetDimension() - 1);
//        UpdaterFactory<ClassificationTrainingRow, EagerUpdater<ClassificationTrainingRow>> updaterFactory =
//                new EagerSagUpdater.EagerSagUpdaterFactory<>(classData.getRowCount(), classData.getFeatureCount() + 1, classData.getTargetDimension() - 1);
//        UpdaterFactory<ClassificationTrainingRow, EagerUpdater<ClassificationTrainingRow>> updaterFactory =
//                EagerSgdUpdater.createFactory();
//        RegularizationPrior prior = new GaussPrior(5);
//        RegularizationPrior prior = UniformPrior.INSTANCE;
//        EagerRegularizationUpdater prior = new EagerRegularizationUpdater(new LaplacePrior(5), classData.getRowCount(), true);
        LazyRegularizationUpdater regUpdater = new LazyPriorUpdater(UniformPrior.INSTANCE, classData.getRowCount(), true);
//        RegularizationUpdater regUpdater = new GaussRegularizationUpdater(1.0/lambda);
//        RegularizationUpdater regUpdater = new EagerPriorUpdater(UniformPrior.INSTANCE, classData.getRowCount(), true);
        StoppingCriterion<ClassificationTrainingRow> stoppingCriterion = new BetaChangeStoppingCriterion<>(classData.getFeatureCount(), classData.getTargetDimension(), 1e-5);
//        EagerSgOptimizer<ClassificationTrainingRow, EagerUpdater<ClassificationTrainingRow>, RegularizationUpdater> sgOpt =
//                new EagerSgOptimizer<>(classData, loss, updaterFactory, regUpdater, lrStrategy, stoppingCriterion);
        LazySGOptimizer<ClassificationTrainingRow, LazyUpdater<ClassificationTrainingRow>, LazyRegularizationUpdater> sgOpt =
                new LazySGOptimizer<>(classData, loss, updaterFactory, regUpdater, lrStrategy, stoppingCriterion);
        int maxIter = 100;
//        double[][] w = sagOpt.optimize(classData, maxIter, lambda, true);
//        double[][] w = sgOpt.optimize(maxIter, classData);
//        return new LogRegLearnerResult(MatrixUtils.createRealMatrix(w), -1, -1);
        return sgOpt.optimize(maxIter, classData);
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

        /**
         * {@inheritDoc}
         */
        @Override
        public ClassificationTrainingRow getRandomRow() {
            int idx = (int)(Math.random() * m_rows.size());
            return m_rows.get(idx);
        }

    }

    private static class ClassDataRow implements ClassificationTrainingRow {

        private final double[] m_data;
        private final int m_cat;
        private final int m_id;
        private final int[] m_nonZero;

        public ClassDataRow(final RegressionTrainingRow row, final int id) {
            m_data = row.getParameter().getRow(0);
            m_cat = (int)row.getTarget();
            m_id = id;
            int[] nonZero = new int[m_data.length + 1];
//            m_nonZero = new int[m_data.length + 1];
            nonZero[0] = 0; // intercept term is always 1
            int k = 1;
            for (int i = 0; i < m_data.length; i++) {
                if (!MathUtils.equals(m_data[i], 0)) {
                    nonZero[k++] = i + 1;
                }
            }
            m_nonZero = Arrays.copyOf(nonZero, k);
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
        /**
         * {@inheritDoc}
         */
        @Override
        public int getNextNonZeroIndex(final int startIdx) {
            if (startIdx == 0) {
                return 0;
            }
            for (int i = startIdx - 1; i < m_data.length; i++) {
                if (!MathUtils.equals(m_data[i], 0.0)) {
                    return i + 1;
                }
            }
            return -1;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int[] getNonZeroIndices() {
            return m_nonZero;
        }

    }

}
